const unavailable = 'Não disponível';
const number = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
const labels = { up: 'Ativo', down: 'Indisponível', unknown: 'Estado desconhecido ou monitoramento não configurado' };
const el = id => document.getElementById(id);
const format = value => Number.isFinite(value) ? number.format(value) : unavailable;

function append(parent, tag, text = '') {
  const child = document.createElement(tag);
  child.textContent = text;
  parent.append(child);
  return child;
}

function table(id, rows, columns) {
  const body = el(id);
  body.replaceChildren();
  if (!rows.length) {
    append(append(body, 'tr'), 'td', unavailable).colSpan = columns;
    return;
  }
  for (const row of rows) {
    const tr = append(body, 'tr');
    for (const value of row) append(tr, 'td', value ?? unavailable);
  }
}

function render(data) {
  el('services').replaceChildren();
  for (const service of data.status) {
    const item = append(el('services'), 'li', service.name + ' — ');
    append(item, 'strong', labels[service.state]).className = service.state;
  }

  const metrics = data.metrics;
  const empty = metrics.state === 'empty';
  const hasData = metrics.state === 'ok';
  const codes = metrics.byCode ?? [];
  const routes = data.routes.items;
  const routeStats = new Map((metrics.byRoute ?? []).map(route => [route.id, route]));
  const sumCodes = pattern => {
    const matching = codes.filter(entry => pattern.test(entry.code));
    if (empty) return 0;
    if (!hasData || !codes.length || matching.some(entry => entry.count === null)) return null;
    return matching.reduce((sum, entry) => sum + entry.count, 0);
  };

  el('metric-state').textContent = {
    ok: '', empty: 'Sem amostras registradas.', missing: 'Métrica indisponível no Actuator.',
    unavailable: 'Falha ao consultar as métricas do Gateway.',
  }[metrics.state];
  el('totals').replaceChildren();
  for (const [label, value] of [
    ['Total observado', empty ? 0 : metrics.count], ['Sucesso (2xx)', sumCodes(/^2/)],
    ['Redirecionamento (3xx)', sumCodes(/^3/)], ['Erro do cliente (4xx)', sumCodes(/^4/)],
    ['Erro do servidor (5xx)', sumCodes(/^5/)], ['Tempo médio (ms)', metrics.meanMs],
  ]) {
    const item = append(el('totals'), 'div');
    append(item, 'dt', label);
    append(item, 'dd', format(value));
  }

  const destinations = new Map();
  const routeCount = id => routeStats.has(id) ? routeStats.get(id).count : empty || hasData ? 0 : null;
  for (const route of routes) {
    const count = routeCount(route.id);
    const destination = route.destination ?? unavailable;
    const current = destinations.has(destination) ? destinations.get(destination) : 0;
    destinations.set(destination, count === null || current === null ? null : current + count);
  }
  table('destinations', [...destinations].map(([name, count]) => [name, format(count)]), 2);
  table('codes', codes.map(({ code, count }) => [code, format(count)]), 2);
  el('route-state').textContent = {
    ok: routes.length ? '' : 'Nenhuma rota retornada pelo Gateway.',
    missing: 'Consulta de rotas indisponível no Actuator.',
    unavailable: 'Falha ao consultar as rotas do Gateway.',
  }[data.routes.state];
  table('routes', routes.map(route => {
    const count = routeCount(route.id);
    const stats = routeStats.get(route.id);
    return [route.id, route.paths, route.destination, format(count),
      stats?.codes?.join(', ') || (count === 0 ? 'Sem amostras' : unavailable), format(stats?.meanMs)];
  }), 6);
  el('updated').textContent = 'Última coleta ' + new Date(data.collectedAt).toLocaleString('pt-BR');
}

let intervalMs = 15_000;
async function refresh() {
  try {
    const response = await fetch('/api/observacao', { signal: AbortSignal.timeout(30_000) });
    if (!response.ok) throw new Error();
    const data = await response.json();
    render(data);
    intervalMs = data.intervalMs;
  } catch {
    el('updated').textContent = 'Coletor indisponível. A próxima atualização será automática.';
  } finally {
    setTimeout(refresh, intervalMs);
  }
}

refresh();
