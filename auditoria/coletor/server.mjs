import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';

const env = process.env;
const intervalMs = integer('AUDIT_INTERVAL_MS', 15_000, 5_000, 300_000);
const timeoutMs = integer('AUDIT_TIMEOUT_MS', 2_000, 250, 10_000);
const services = [
  { name: 'Spring Cloud Gateway', origin: origin('AUDIT_GATEWAY_URL', 9081), health: '/actuator/health' },
  { name: 'Monólito legado', origin: origin('AUDIT_LEGACY_URL', 9080), health: '/manage/health' },
  { name: 'Owners Service', origin: origin('AUDIT_OWNERS_URL', 9082), health: '/actuator/health' },
];
const gateway = services[0].origin;
const meter = 'spring.cloud.gateway.requests';
const meterPath = '/actuator/metrics/' + meter;

function integer(name, fallback, min, max) {
  const value = Number(env[name] ?? fallback);
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new Error(name + ' fora do intervalo permitido');
  }
  return value;
}

function origin(name, port) {
  const url = new URL(env[name] ?? 'http://127.0.0.1:' + port);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
      url.pathname !== '/' || url.search || url.hash) {
    throw new Error(name + ' deve conter somente a origem HTTP(S)');
  }
  return url.origin;
}

async function request(base, path) {
  try {
    const response = await fetch(base + path, {
      signal: AbortSignal.timeout(timeoutMs),
      redirect: 'error',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok && response.status !== 503) {
      await response.body?.cancel();
      return { state: [401, 403, 404].includes(response.status) ? 'missing' : 'unavailable' };
    }
    return { state: 'ok', status: response.status, data: await response.json() };
  } catch {
    return { state: 'unavailable' };
  }
}

async function health(service) {
  const result = await request(service.origin, service.health);
  let state = 'unknown';
  if (result.state === 'unavailable' || result.status === 503 ||
      ['DOWN', 'OUT_OF_SERVICE'].includes(result.data?.status)) state = 'down';
  if (result.status === 200 && result.data?.status === 'UP') state = 'up';
  return { name: service.name, state };
}

function destination(value) {
  try {
    const uri = new URL(value);
    if (['http:', 'https:'].includes(uri.protocol)) return uri.origin;
    if (uri.protocol === 'no:') return 'Bloqueio local';
  } catch {
    return null;
  }
  return null;
}

async function routes() {
  const result = await request(gateway, '/actuator/gateway/routes');
  if (!Array.isArray(result.data)) return { state: result.state === 'missing' ? 'missing' : 'unavailable', items: [] };
  return {
    state: 'ok',
    items: result.data.map(route => ({
      id: route.route_id,
      paths: route.predicate?.match(/Paths: \[([^\]]*)\]/)?.[1] ?? null,
      destination: destination(route.uri),
    })),
  };
}

function tags(data, key) {
  return data?.availableTags?.find(tag => tag.tag === key)?.values ?? [];
}

function measurement(data, statistic) {
  const value = data?.measurements?.find(entry => entry.statistic === statistic)?.value;
  return Number.isFinite(value) ? value : null;
}

function timer(data) {
  const count = measurement(data, 'COUNT');
  const seconds = measurement(data, 'TOTAL_TIME');
  return { count, meanMs: count > 0 && seconds !== null ? seconds * 1000 / count : null };
}

function filterMeter(tag, value) {
  return request(gateway, meterPath + '?' + new URLSearchParams({ tag: tag + ':' + value }));
}

async function metrics() {
  const result = await request(gateway, meterPath);
  if (result.state === 'missing') {
    const index = await request(gateway, '/actuator/metrics');
    if (index.state === 'unavailable') return { state: 'unavailable' };
    const names = index.data?.names;
    const enabled = Array.isArray(names) && names.includes('spring.cloud.gateway.routes.count');
    return { state: enabled && !names.includes(meter) ? 'empty' : 'missing' };
  }
  const total = timer(result.data);
  if (total.count === null) return { state: 'unavailable' };

  const codes = tags(result.data, 'httpStatusCode').filter(code => /^[1-5]\d\d$/.test(code));
  const [byRoute, byCode] = await Promise.all([
    Promise.all(tags(result.data, 'routeId').map(async id => {
      const { data } = await filterMeter('routeId', id);
      return { id, ...timer(data), codes: data ? tags(data, 'httpStatusCode') : null };
    })),
    Promise.all(codes.map(async code => {
      const { data } = await filterMeter('httpStatusCode', code);
      return { code, count: measurement(data, 'COUNT') };
    })),
  ]);
  return { state: 'ok', ...total, byRoute, byCode };
}

async function collect() {
  const [status, routeData, metricData] = await Promise.all([
    Promise.all(services.map(health)), routes(), metrics(),
  ]);
  return { status, routes: routeData, metrics: metricData, intervalMs, collectedAt: new Date().toISOString() };
}

let latest;
let pending;
function snapshot() {
  if (latest && Date.now() - Date.parse(latest.collectedAt) < intervalMs) return latest;
  pending ??= collect().then(data => (latest = data)).finally(() => { pending = undefined; });
  return pending;
}

const assets = new Map();
for (const [path, file, type] of [
  ['/', 'index.html', 'text/html'], ['/index.html', 'index.html', 'text/html'],
  ['/style.css', 'style.css', 'text/css'], ['/app.js', 'app.js', 'text/javascript'],
]) {
  assets.set(path, { type, body: await readFile(new URL('../painel/' + file, import.meta.url)) });
}

createServer(async (req, res) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'GET') {
    res.writeHead(405, { Allow: 'GET' }).end();
    return;
  }
  if (req.url === '/api/observacao') {
    try {
      const data = await snapshot();
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' }).end(JSON.stringify(data));
    } catch {
      res.writeHead(503).end();
    }
    return;
  }
  const asset = assets.get(req.url);
  if (!asset) {
    res.writeHead(404).end();
    return;
  }
  res.writeHead(200, { 'Content-Type': asset.type + '; charset=utf-8' }).end(asset.body);
}).listen(integer('AUDIT_PORT', 3000, 1, 65535), env.AUDIT_HOST ?? '127.0.0.1');
