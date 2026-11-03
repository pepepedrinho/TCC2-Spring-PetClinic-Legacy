"""Reproducible migration test against a new baseline fixture, never user databases."""
import hashlib,json,os,subprocess
from pathlib import Path
from datetime import datetime,timezone
root=Path(__file__).resolve().parents[1]
run=root/'.work'/('migration-'+datetime.now().strftime('%Y%m%d-%H%M%S'));run.mkdir(parents=True)
java=str(Path(os.environ.get('JAVA17_HOME',r'C:\Program Files\Java\jdk-17'))/'bin/java.exe')
repo=Path.home()/'.m2/repository'
cp=str(repo/'org/hsqldb/hsqldb/2.3.5/hsqldb-2.3.5.jar')+';'+str(repo/'com/h2database/h2/2.3.232/h2-2.3.232.jar')
for file in ('schema.sql','data.sql'):
    content=subprocess.check_output(['git','show','c36452a2c34443ae26b4ecbba4f149906af14717:src/main/resources/db/hsqldb/'+file],cwd='C:\\Users\\Theki\\OneDrive\\Área de Trabalho\\spring-petclinic-legado')
    (run/file).write_bytes(content)
fixture=run/'Fixture.java'
fixture.write_text('''import java.sql.*;import java.nio.file.*;
class Fixture {
 public static void main(String[] args) throws Exception {
  Path dir=Path.of(args[0]);
  if(args.length==1) {
   try(Connection c=DriverManager.getConnection("jdbc:hsqldb:file:"+dir.resolve("source"),"SA","")) {
    for(String file:new String[]{"schema.sql","data.sql"})
     for(String sql:Files.readString(dir.resolve(file)).split(";")) if(!sql.isBlank()) c.createStatement().execute(sql);
    c.createStatement().execute("INSERT INTO owners VALUES(101,'Ana','Migrated','Rua','Olinda','1234567890')");
    c.createStatement().execute("INSERT INTO pets VALUES(101,'MigratedPet','2020-01-01',1,101)");
    c.createStatement().execute("INSERT INTO visits VALUES(101,101,'2026-10-28','MigratedVisit')");
    c.createStatement().execute("SHUTDOWN");
   }
  } else {
   try(Connection l=DriverManager.getConnection("jdbc:hsqldb:file:"+dir.resolve("target/petclinic")+";shutdown=true","SA","");
       Connection o=DriverManager.getConnection("jdbc:h2:file:"+dir.resolve("target/owners"),"sa","")) {
    ResultSet r=o.createStatement().executeQuery("SELECT city FROM owners WHERE id=101");
    if(!r.next()||!r.getString(1).equals("Olinda")) throw new AssertionError("Owner missing");
    r=l.createStatement().executeQuery("SELECT p.owner_id,v.description FROM pets p JOIN visits v ON v.pet_id=p.id WHERE p.id=101");
    if(!r.next()||r.getInt(1)!=101||!r.getString(2).equals("MigratedVisit")) throw new AssertionError("Relationship missing");
    r=l.createStatement().executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='OWNERS'");r.next();
    if(r.getInt(1)!=0) throw new AssertionError("Local owners table remains");
    r=o.createStatement().executeQuery("SELECT COUNT(*) FROM seed_history");r.next();if(r.getInt(1)!=1)throw new AssertionError("Missing marker");
    o.createStatement().execute("INSERT INTO owners(first_name,last_name,address,city,telephone) VALUES('Next','Owner','R','C','123')");
    r=o.createStatement().executeQuery("SELECT MAX(id) FROM owners");r.next();if(r.getInt(1)!=102)throw new AssertionError("Sequence rewound");
    l.createStatement().execute("SHUTDOWN");
   }
   System.out.println("PASS: IDs, Owner-Pet-Visit, absence of local Owners, identity sequence");
  }
 }
}
''')
def call(file,*args):
 return subprocess.run([java,'--class-path',cp,str(file),*map(str,args)],cwd=root,capture_output=True,text=True)
def ok(result):
 assert result.returncode==0,result.stdout+result.stderr
 return result.stdout
outputs=[ok(call(fixture,run))]
def hashes(): return {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in run.glob('source.*')}
before=hashes()
outputs.append(ok(call(root/'scripts/MigrateOwners.java',run/'source',run/'target',root)))
after=hashes();assert before==after,(before,after)
outputs.append(ok(call(fixture,run,'verify')))
retry=call(root/'scripts/MigrateOwners.java',run/'source',run/'target',root)
assert retry.returncode!=0 and 'Target must not exist' in retry.stderr
outputs.append('PASS: existing target refused; source files unchanged (SHA-256)')
out=root/'docs/metricas/v2-centralizacao-erros/evidencias/migration.json'
out.write_text(json.dumps({'result':'PASS','utc':datetime.now(timezone.utc).isoformat(),'output':outputs,'sourceHashes':before},indent=2))
print('\n'.join(outputs))
