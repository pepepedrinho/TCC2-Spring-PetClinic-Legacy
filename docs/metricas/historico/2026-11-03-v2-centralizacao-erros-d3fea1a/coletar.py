"""Recollect A/B/C and both services with the historical CK binary; never write v2/."""
from pathlib import Path
import argparse,subprocess,hashlib,json,zipfile,datetime

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--a',required=True,type=Path)
    p.add_argument('--b',required=True,type=Path)
    p.add_argument('--c',required=True,type=Path)
    p.add_argument('--output',required=True,type=Path)
    p.add_argument('--repo',required=True,type=Path)
    p.add_argument('--ck-jar',type=Path,default=Path(r'C:\TCC2\ferramentas\ck\target\ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar'))
    p.add_argument('--java',type=Path,default=Path(r'C:\Program Files\Java\jdk-17\bin\java.exe'))
    a=p.parse_args()
    expected='98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517'
    actual=hashlib.sha256(a.ck_jar.read_bytes()).hexdigest()
    assert actual==expected,'CK binary differs from original V2'
    out=a.output;out.mkdir(parents=True,exist_ok=True)
    assert out.name=='v2-centralizacao-erros','Separate new evidence directory is required'
    sources=[('A-monolito-original',a.a,'src/main/java'),('B-monolito-anterior',a.b,'src/main/java'),('C-monolito-centralizado',a.c,'src/main/java'),('owners',a.c,'owners-service/src/main/java'),('gateway',a.c,'gateway/src/main/java')]
    manifest=[];commands=[]
    snapshotdir=out/'fontes';snapshotdir.mkdir(exist_ok=True)
    for name,root,folder in sources:
        dest=out/name
        assert not dest.exists(),'Do not overwrite previous collection: '+str(dest)
        dest.mkdir()
        snapshot=snapshotdir/(name+'.zip')
        assert not snapshot.exists()
        with zipfile.ZipFile(snapshot,'w',zipfile.ZIP_DEFLATED) as z:
            for f in sorted((root/folder).rglob('*.java')):
                rel=f.relative_to(root).as_posix();z.write(f,rel)
                manifest.append({'scenario':name,'file':rel,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()})
        cmd=[str(a.java),'-jar',str(a.ck_jar),str(root/folder),'false','0','false',str(dest)]
        commands.append(cmd)
        with (dest/'coleta.txt').open('w',encoding='utf-8') as log:
            r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT)
        assert r.returncode==0 and (dest/'class.csv').exists(),name
        print('Collected '+name,flush=True)
    git=lambda *args:subprocess.check_output(['git',*args],cwd=a.repo).decode('utf-8').strip()
    provenance={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'ckVersion':'0.7.1-SNAPSHOT','ckSha256':actual,'ckSourceCommit':'8a1ef916d597d33ad619d748b616bb7dbc3f0c36','parameters':['false','0','false'],'java':subprocess.check_output([str(a.java),'-version'],stderr=subprocess.STDOUT).decode(),'baseline':git('rev-parse','refs/heads/1.5.x'),'head':git('rev-parse','HEAD'),'branch':git('branch','--show-current'),'workingTreeAtCollection':git('status','--short'),'checkpoint':'C is the candidate source snapshot, not yet applied to the original worktree at collection; source SHA-256 and archives identify it exactly. B is the preserved worktree before the intervention, including presentation changes.','commands':commands}
    (out/'proveniencia.json').write_text(json.dumps(provenance,indent=2),encoding='utf-8')
    (out/'fontes-avaliadas.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')

if __name__=='__main__':main()
