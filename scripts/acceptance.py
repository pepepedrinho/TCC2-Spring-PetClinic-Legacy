"""HTTP acceptance for isolated JVMs or an explicitly selected disposable Compose project."""
import json, os, re, socket, subprocess, time, urllib.request, urllib.error, urllib.parse
from pathlib import Path
from datetime import datetime, timezone
ROOT=Path(__file__).resolve().parents[1]
COMPOSE_PROJECT=os.environ.get('TCC2_COMPOSE_PROJECT')
GATEWAY_PORT=int(os.environ.get('GATEWAY_PORT', '8081')) if COMPOSE_PROJECT else 8081
compose=['docker','compose','-p',COMPOSE_PROJECT] if COMPOSE_PROJECT else None
services={'legacy':'monolith','owners':'owners-service','gateway':'gateway'}
legacy_started=False
JAVA8=os.environ.get('JAVA8_HOME')
JAVA17=os.environ.get('JAVA17_HOME')
if not compose and (not JAVA8 or not JAVA17):
    raise SystemExit('Defina JAVA8_HOME e JAVA17_HOME.')
PROJECTS=ROOT/'projects'
RUN=ROOT/'.work'/('acceptance-'+datetime.now().strftime('%Y%m%d-%H%M%S'))
RUN.mkdir(parents=True)
checks=[]; procs={}; logs=[]
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args): return None
opener=urllib.request.build_opener(NoRedirect)
def req(path,expected=200,data=None,method=None,port=8081):
    headers={}
    if isinstance(data,dict):
        data=urllib.parse.urlencode(data).encode(); headers['Content-Type']='application/x-www-form-urlencoded'
    elif data is not None:
        data=data.encode();headers['Content-Type']='application/json'
    port=GATEWAY_PORT if compose else port
    r=urllib.request.Request(f'http://127.0.0.1:{port}'+path,data=data,headers=headers,method=method)
    try: response=opener.open(r,timeout=10)
    except urllib.error.HTTPError as e: response=e
    body=response.read().decode('utf-8'); status=response.code
    assert status==expected, (path,status,expected,body[:500])
    checks.append({'path':path,'method':r.get_method(),'port':port,'status':status})
    return body,response.headers

def start(name):
    global legacy_started
    if compose:
        subprocess.run([*compose,'start',services[name]],cwd=ROOT,check=True)
        return
    env=os.environ.copy()
    if name=='legacy':
        # Only the first boot of the disposable standalone database runs the scripts.
        env['SPRING_DATASOURCE_INITIALIZE']='false' if legacy_started else 'true'
        legacy_started=True
    env['LEGACY_DB_URL']='jdbc:hsqldb:file:'+str(RUN/'legacy').replace('\\','/')+';shutdown=true;hsqldb.write_delay=false'
    env['OWNERS_DB_URL']='jdbc:h2:file:'+str(RUN/'owners').replace('\\','/')+';DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'
    java=JAVA8 if name=='legacy' else JAVA17
    jar={
        'legacy':PROJECTS/'monolith/target/spring-petclinic-1.5.1.jar',
        'owners':PROJECTS/'owners-service/target/owners-service-1.0.0-SNAPSHOT.jar',
        'gateway':PROJECTS/'gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar',
    }[name]
    log=open(RUN/(name+str(len(logs))+'.log'),'w');logs.append(log)
    procs[name]=subprocess.Popen([str(Path(java)/'bin/java.exe'),'-jar',str(jar)],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=0x08000000)

def stop(name):
    if compose:
        subprocess.run([*compose,'stop',services[name]],cwd=ROOT,check=True)
        return
    p=procs.pop(name,None)
    if p and p.poll() is None: p.terminate();p.wait(timeout=20)

def ready(path,port):
    for _ in range(100):
        try: req(path,port=port);return
        except (OSError,AssertionError): time.sleep(1)
    raise AssertionError(f'{port} did not start; inspect {RUN}')

def has(body,text): assert text in body, text

def redirect(headers, expected_path=None):
    location = urllib.parse.urlsplit(headers['Location'])
    assert not location.netloc or (location.scheme, location.netloc) == ('http', f'127.0.0.1:{GATEWAY_PORT}'), location
    if expected_path:
        assert location.path == expected_path, location
    return location.path
try:
    if not compose:
        for port in (8080,8081,8082):
            with socket.socket() as s:
                assert s.connect_ex(('127.0.0.1',port))!=0, f'Port {port} in use; existing process will not be stopped'
        start('owners');start('legacy');start('gateway')
    ready('/api/owners/1',8082);ready('/vets.html',8080);ready('/',8081)
    for path in ('/','/owners','/owners/find','/owners/new','/owners/1','/owners/1/edit','/owners/1/pets/new','/owners/1/pets/1/edit','/owners/1/pets/1/visits/new','/vets.html','/vets.json','/resources/css/petclinic.css'):
        req(path)
    has(req('/owners/find')[0], 'action="/owners"')
    has(req('/owners/find/')[0], 'action="/owners"')
    has(req('/owners/')[0], 'George Franklin')
    details = req('/owners/1')[0]
    for fragment in ('George Franklin', 'Leo', '2010-09-07', 'href="/owners/1/edit"',
                     'href="/owners/1/pets/new"', 'href="/owners/1/pets/1/edit"',
                     'href="/owners/1/pets/1/visits/new"', 'href="/resources/css/petclinic.css"'):
        has(details, fragment)
    for fragment in ('Samantha', 'rabies shot', 'spayed'):
        has(req('/owners/6')[0], fragment)
    redirect(req('/owners?lastName=frank', 302)[1], '/owners/1')
    listing = req('/owners?lastName=Davis')[0]
    for fragment in ('Betty Davis', 'Harold Davis', 'Basil', 'href="/owners/2"'):
        has(listing, fragment)
    has(req('/owners?lastName=Missing')[0], 'not found')
    has(req('/owners/new')[0], 'Add Owner')
    has(req('/owners/1/edit')[0], 'Update Owner')
    req('/internal/owner-pets',404,data='[1]')
    req('/%69nternal/owner-pets',404,data='[1]')
    req('/owners/../internal/owner-pets',404,data='[1]')
    req('//internal/owner-pets',404,data='[1]')
    req('/api/pets/1',404);req('/api/pet-types',404)
    req('/api/owners/1',409,method='DELETE')
    req('/api/owners/99999',404);req('/owners/99999',404)
    req('/api/owners',400,data='{}')
    invalid = req('/owners/new',data={'firstName':'Invalid','telephone':'abc'})[0]
    for fragment in ('has-error', 'must not be blank', 'value="abc"'):
        has(invalid, fragment)
    owner={'firstName':'Ana','lastName':'V3Acceptance','address':'Rua A','city':'Recife','telephone':'1234567890'}
    loc = redirect(req('/owners/new',302,data=dict(owner,id=1))[1])
    oid=int(loc.rsplit('/',1)[1]);assert oid>10
    invalid = req(f'/owners/{oid}/edit', data=dict(owner,city=''))[0]
    has(invalid, 'Update Owner')
    has(invalid, 'has-error')
    assert json.loads(req(f'/api/owners/{oid}')[0])['city']=='Recife'
    owner['city']='Olinda'
    redirect(req(f'/owners/{oid}/edit',302,data=dict(owner,id=1))[1], loc)
    assert json.loads(req(f'/api/owners/{oid}')[0])['city']=='Olinda'
    pet={'name':'V3Pet','type':'cat','birthDate':'2020-01-01'}
    redirect(req(f'/owners/{oid}/pets/new',302,data=pet)[1], loc)
    body=req(f'/owners/{oid}')[0];has(body,'V3Pet')
    pid=int(re.search(r'/pets/(\d+)/edit',body).group(1));assert pid>13
    pet['name']='V3PetEdited'
    redirect(req(f'/owners/{oid}/pets/{pid}/edit',302,data=pet)[1], loc)
    req(f'/owners/1/pets/{pid}/edit',404)
    req(f'/owners/1/pets/{pid}/edit',404,data=pet)
    req(f'/owners/1/pets/{pid}/visits/new',404,data={'description':'Wrong owner'})
    req('/owners/99999/pets/new',404,data=pet)
    has(req(f'/owners/{oid}/pets/new',data=pet)[0], 'is already in use')
    visit = {'date':'2026-10-28','description':'V3Visit','petId':'1'}
    redirect(req(f'/owners/{oid}/pets/{pid}/visits/new',302,data=visit)[1], loc)
    has(req(f'/owners/{oid}')[0],'V3Visit')
    assert 'V3Visit' not in req('/owners/1')[0] # forged petId ignored
    has(req(f'/owners/{oid}/pets/{pid}/visits/new')[0],'V3Visit')
    # Restart the isolated deployment and check the same persisted data.
    if compose:
        subprocess.run([*compose,'stop'],cwd=ROOT,check=True)
        subprocess.run([*compose,'up','--no-build','--wait','--wait-timeout','120'],cwd=ROOT,check=True)
    else:
        stop('legacy');stop('owners');start('owners');start('legacy')
    ready('/api/owners/1',8082);ready('/vets.html',8080)
    body=req(f'/owners/{oid}')[0]
    for text in ('Olinda','V3PetEdited','V3Visit'): has(body,text)
    req(f'/owners/{oid}/pets/{pid}/edit')
    # Identity allocation and seed initialization must not rewind after restart.
    owner2=json.loads(req('/api/owners',201,data=json.dumps(dict(owner,lastName='AfterRestart')))[0]);assert owner2['id']>oid
    pet['name']='AfterRestart';req(f'/owners/{oid}/pets/new',302,data=pet)
    req(f'/api/owners/{oid}',409,method='DELETE')
    stop('owners')
    started=time.monotonic();req(f'/owners/{oid}',503);assert time.monotonic()-started<8
    req('/owners',503);req('/api/owners/1',503)
    has(req(f'/owners/{oid}/pets/new',503,data=pet)[0], 'Owners temporariamente indispon')
    req('/vets.html');req('/vets.json');req('/')
    start('owners');ready('/api/owners/1',8082);req(f'/owners/{oid}')
    stop('legacy')
    req(f'/owners/{oid}',503);req('/owners',503)
    req('/owners/find');req('/owners/new');req(f'/owners/{oid}/edit');req(f'/api/owners/{oid}')
    start('legacy');ready('/vets.html',8080)
    has(req(f'/owners/{oid}')[0],'V3Visit')
    result={'result':'PASS','utc':datetime.now(timezone.utc).isoformat(),'checks':checks,'ownerId':oid,'petId':pid,
            'databases':COMPOSE_PROJECT if compose else str(RUN),
            'restart':'Compose stop/up --wait; the same named volumes are mounted' if compose else 'abrupt process termination; same file databases'}
except BaseException as e:
    result={'result':'FAIL','error':repr(e),'checks':checks,'logs':str(RUN)}
    raise
finally:
    for name in list(procs):stop(name)
    for log in logs:log.close()
    (RUN/'http-acceptance.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2),flush=True)
