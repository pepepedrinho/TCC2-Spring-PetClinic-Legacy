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
    r=urllib.request.Request(f'http://localhost:{port}'+path,data=data,headers=headers,method=method)
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
try:
    if not compose:
        for port in (8080,8081,8082):
            with socket.socket() as s:
                assert s.connect_ex(('localhost',port))!=0, f'Port {port} in use; existing process will not be stopped'
        start('owners');start('legacy');start('gateway')
    ready('/api/owners/1',8082);ready('/vets.html',8080);ready('/',8081)
    for path in ('/','/owners','/owners/find','/owners/new','/owners/1','/owners/1/edit','/owners/1/pets/new','/owners/1/pets/1/edit','/owners/1/pets/1/visits/new','/vets.html','/vets.json','/resources/css/petclinic.css'):
        req(path)
    has(req('/owners/1')[0],'Leo');has(req('/owners/6')[0],'Samantha')
    _,h=req('/owners?lastName=Franklin',302);assert h['Location'].endswith('/owners/1');assert ':8080' not in h['Location']
    has(req('/owners?lastName=Davis')[0],'Betty');req('/owners?lastName=Missing')
    req('/api/pets/1',404);req('/api/pet-types',404)
    req('/api/owners/1',409,method='DELETE')
    req('/api/owners/99999',404);req('/owners/99999',404)
    req('/api/owners',400,data='{}')
    req('/owners/new',data={'firstName':'Invalid'})
    owner={'firstName':'Ana','lastName':'V2Acceptance','address':'Rua A','city':'Recife','telephone':'1234567890'}
    _,h=req('/owners/new',302,data=owner);loc=h['Location'];assert ':8080' not in loc
    oid=int(loc.rsplit('/',1)[1]);assert oid>10
    owner['city']='Olinda';req(f'/owners/{oid}/edit',302,data=owner)
    assert json.loads(req(f'/api/owners/{oid}')[0])['city']=='Olinda'
    pet={'name':'V2Pet','type':'cat','birthDate':'2020-01-01'}
    req(f'/owners/{oid}/pets/new',302,data=pet)
    body=req(f'/owners/{oid}')[0];has(body,'V2Pet')
    pid=int(re.search(r'/pets/(\d+)/edit',body).group(1));assert pid>13
    pet['name']='V2PetEdited';req(f'/owners/{oid}/pets/{pid}/edit',302,data=pet)
    req(f'/owners/1/pets/{pid}/edit',404)
    req(f'/owners/1/pets/{pid}/edit',404,data=pet)
    req(f'/owners/1/pets/{pid}/visits/new',404,data={'description':'Wrong owner'})
    req('/owners/99999/pets/new',404,data=pet)
    req(f'/owners/{oid}/pets/new',data=pet) # duplicate name stays on form
    req(f'/owners/{oid}/pets/{pid}/visits/new',302,data={'date':'2026-10-28','description':'V2Visit','petId':'1'})
    has(req(f'/owners/{oid}')[0],'V2Visit')
    assert 'V2Visit' not in req('/owners/1')[0] # forged petId ignored
    has(req(f'/owners/{oid}/pets/{pid}/visits/new')[0],'V2Visit')
    # Crash/restart with the same database files: commits must survive.
    stop('legacy');stop('owners');start('owners');start('legacy')
    ready('/api/owners/1',8082);ready('/vets.html',8080)
    body=req(f'/owners/{oid}')[0]
    for text in ('Olinda','V2PetEdited','V2Visit'): has(body,text)
    req(f'/owners/{oid}/pets/{pid}/edit')
    # Identity allocation and seed initialization must not rewind after restart.
    owner2=json.loads(req('/api/owners',201,data=json.dumps(dict(owner,lastName='AfterRestart')))[0]);assert owner2['id']>oid
    pet['name']='AfterRestart';req(f'/owners/{oid}/pets/new',302,data=pet)
    req(f'/api/owners/{oid}',409,method='DELETE')
    stop('owners')
    started=time.monotonic();req(f'/owners/{oid}',503);assert time.monotonic()-started<8
    req('/owners',503);req(f'/owners/{oid}/pets/new',503,data=pet)
    req('/vets.html');req('/vets.json');req('/')
    start('owners');ready('/api/owners/1',8082);req(f'/owners/{oid}')
    result={'result':'PASS','utc':datetime.now(timezone.utc).isoformat(),'checks':checks,'ownerId':oid,'petId':pid,
            'databases':COMPOSE_PROJECT if compose else str(RUN),
            'restart':'Compose stop/start; database services remain active' if compose else 'abrupt process termination; same file databases'}
except BaseException as e:
    result={'result':'FAIL','error':repr(e),'checks':checks,'logs':str(RUN)}
    raise
finally:
    for name in list(procs):stop(name)
    for log in logs:log.close()
    (RUN/'http-acceptance.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2),flush=True)
