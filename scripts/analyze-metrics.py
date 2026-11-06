"""Descriptive CK analysis; no quality score or causal claim."""
from pathlib import Path
import csv, json, math, statistics, subprocess, hashlib, sys
ROOT=Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
 raise SystemExit('Informe a pasta da coleta criada por collect-ck.ps1.')
OUT=Path(sys.argv[1]).resolve(strict=True)
if OUT.parent != (ROOT/'.work').resolve() or not OUT.name.startswith('ck-'):
 raise SystemExit('A pasta da coleta deve estar em .work/ck-*.')
M=['cbo','wmc','rfc','lcom','lcom*']
def read(p):
 try: text=p.read_text(encoding='utf-8-sig')
 except UnicodeDecodeError: text=p.read_text(encoding='cp1252')
 return list(csv.DictReader(text.splitlines()))
def save(name,rows):
 with (OUT/name).open('w',encoding='utf-8',newline='') as f:
  w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
def md(rows):
 keys=list(rows[0]);return '\n'.join(['| '+' | '.join(keys)+' |','| '+' | '.join(['---']*len(keys))+' |']+['| '+' | '.join(str(row[k]).replace('|','/') for k in keys)+' |' for row in rows])
def vals(row):return ', '.join(f'{m}={row[m]}' for m in M)
def gitfile(ref,path):return subprocess.check_output(['git','show',ref+':'+path],cwd=ROOT)
base=read(ROOT/'historico/metricas/baseline-original-c36452a/class.csv');recollected=read(ROOT/'historico/metricas/2026-10-28-v2-extracao-5b6a3b1/baseline-recoletada/class.csv')
original={r['class']:r for r in base};fresh={r['class']:r for r in recollected}
assert original.keys()==fresh.keys()
for name,row in original.items():
 for metric in row:
  if metric!='file':assert row[metric]==fresh[name][metric],(name,metric,row[metric],fresh[name][metric])
components={'A baseline':base,'B monolito':read(OUT/'monolito/class.csv'),'C owners':read(OUT/'owners/class.csv'),'Gateway (separado)':read(OUT/'gateway/class.csv')}
summary=[];descriptive=[];totals=[]
for name,rows in components.items():
 summary.append({'conjunto':name,'tipos_CK':len(rows),**{m:round(statistics.mean(float(r[m]) for r in rows),6) for m in M}})
 totals.append({'componente':name,'tipos_CK':len(rows),**{m:sum(float(r[m]) for r in rows) for m in M if m!='lcom*'},'soma_lcom*':'nao interpretada (normalizada)'})
 for m in M:
  numbers=[float(r[m]) for r in rows]
  descriptive.append({'conjunto':name,'metrica':m,'n':len(numbers),'media':statistics.mean(numbers),'mediana':statistics.median(numbers),'min':min(numbers),'max':max(numbers),'desvio_padrao_populacional':statistics.pstdev(numbers)})
current={r['class']:r for r in components['B monolito']};service={r['class']:r for r in components['C owners']}
equivalents=[]
for name,row in original.items():
 short=name.rsplit('.',1)[-1]
 historical_path='src/main/java/'+name.replace('.','/')+'.java'
 current_path=ROOT/'projects/monolith'/historical_path
 if short in ('Owner','OwnerRepository'):status='substituida: representacao MVC' if short=='Owner' else 'substituida: contrato do adaptador de Owners'
 elif name not in current:status='sem equivalente direto'
 else:status='mantida' if gitfile('c36452a2c34443ae26b4ecbba4f149906af14717',historical_path).replace(b'\r\n',b'\n')==current_path.read_bytes().replace(b'\r\n',b'\n') else 'modificada'
 equivalents.append({'classe_historica':name,'situacao':status,'A':vals(row),'B':vals(current[name]) if name in current else 'ausente','limite':'mesmo nome, papel diferente; nao equivalencia de negocio' if short in ('Owner','OwnerRepository') else ''})
extraction=[]
for src,methods,dst,target,limit in [
 ('Owner','address/city/telephone; heranca firstName/lastName/id','Owner','campos, getters/setters, updateFrom','somente cadastro: pets/getPet/addPet permanecem no legado'),
 ('OwnerRepository','findByLastName','OwnerRepository','findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc','responsabilidade de pesquisa; interface derivada diferente'),
 ('OwnerRepository','findById/save','OwnerController','get/create/update','divisao REST + JPA; nao equivalencia um-para-um de classe'),
 ('OwnerController','processCreationForm/processUpdateOwnerForm/processFindForm/showOwner','OwnerController','create/update/search/get','MVC permanece no monolito; somente operacoes autoritativas extraidas'),
 ('Person','firstName/lastName','Owner','campos e acesso','Vet ainda herda Person no legado; classe inteira nao migrou'),
 ('BaseEntity','id/isNew','Owner','id/getId + identidade JPA','base compartilhada permanece; somente identidade de Owner transferida')]:
 a=next(r for r in base if r['class'].endswith('.'+src));c=service['br.edu.tcc.owners.'+dst]
 extraction.append({'origem':src,'metodos_ou_campos':methods,'destino':'br.edu.tcc.owners.'+dst,'equivalente_funcional':target,'A_classe_contexto':vals(a),'C_classe_contexto':vals(c),'limite':limit})
integration=[]
for row in components['B monolito']:
 name=row['class'];short=name.rsplit('.',1)[-1]
 if '.integration.' in name or short=='OwnerRepository':
  purpose={'OwnerRepository':'contrato de acesso a Owners','OwnerIntegrationAdapter':'HTTP e composicao com Pets locais','IntegrationErrorHandler':'classificacao e resposta a falhas de integracao','IntegrationErrorHandler$IntegrationFailure':'marcador de falha remota'}[short]
  integration.append({'classe':name,'papel':purpose,**{m:row[m] for m in M}})
totals.append({'componente':'V2 completa (B+C+Gateway)','tipos_CK':sum(r['tipos_CK'] for r in summary[1:]),**{m:sum(r[m] for r in totals[1:]) for m in M if m!='lcom*'},'soma_lcom*':'nao interpretada (normalizada)'})
for filename,rows in [('tabela-1-gerais.csv',summary),('estatisticas-descritivas.csv',descriptive),('tabela-2-equivalentes.csv',equivalents),('tabela-3-extracao.csv',extraction),('tabela-4-integracao.csv',integration),('tabela-5-arquitetura.csv',totals)]:save(filename,rows)
prod=[]
for directory in ['projects/monolith/src/main/java','projects/owners-service/src/main/java','projects/gateway/src/main/java']:
 for p in sorted((ROOT/directory).rglob('*.java')):prod.append({'file':p.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(OUT/'fontes-avaliadas.json').write_text(json.dumps(prod,indent=2))
report='''# Resultados CK V2 — extração exclusiva de Owners

A baseline preservada e a recoleta historica arquivada produziram valores identicos para TODOS os campos, exceto caminhos absolutos dos arquivos (24 tipos). Isso sustenta a compatibilidade do JAR/parametros reconstruidos com a coleta historica, cuja branch nao versionou o comando original. Nao prova qual comando foi usado no passado.

Incluidos todos os tipos reportados pelo CK em src/main/java de cada componente (interfaces e classes; package-info nao gera tipo). Excluidos testes, scripts, dependencias compiladas e fontes geradas. Sem excluir DTOs, adaptadores ou classes com metricas zero. Mesma ferramenta e argumentos false 0 false para todos. LCOM* corresponde a coluna literal lcom*. Media aritmetica por tipo; estatisticas populacionais adicionais em CSV. Nao e cobertura de testes.

## Tabela 1 — Metricas gerais

'''+md(summary)+'''

## Tabela 2 — Classes historicas equivalentes

'''+md(equivalents)+'''

## Tabela 3 — Extracao funcional

Valores de classe sao contexto, nao metricas dos metodos isolados. As linhas repetem classes quando uma responsabilidade foi repartida; nao somar estas linhas. method.csv preserva a granularidade de metodos. OwnerController MVC continua no legado; o REST nao substitui a renderizacao. OwnerRepository define o contrato e OwnerIntegrationAdapter implementa o acesso remoto e a composicao local.

'''+md(extraction)+'''

## Tabela 4 — Codigo de integracao

Estas classes NAO sao negocio migrado. Owner e representacao MVC adaptada; LegacyDatabaseInitializer e infraestrutura de inicializacao, contabilizada no monolito mas fora desta tabela HTTP.

'''+md(integration)+'''

## Tabela 5 — Arquitetura completa

Somatorios por componente. CBO/RFC somados podem contar referencias repetidas. LCOM* normalizado nao tem soma interpretavel; nao e agregado. Nao se define um indice global de qualidade. A baseline e uma alternativa historica, nao um componente da arquitetura V2.

'''+md(totals)+'''

## Interpretacao

'''
a,b,c=list(summary)[:3]
for m in M:
 av,bv,cv=a[m],b[m],c[m]
 report+=f'- {m}: A={av}, B={bv}, C={cv}. Variacao B/A: {(bv-av)/av*100:.2f}%.\n' if av else f'- {m}: A=0; B={bv}; C={cv}.\n'
report+='''
As medias descrevem populacoes de tamanhos e papeis diferentes. A adaptacao adiciona comunicacao HTTP, composicao e tratamento de falhas, enquanto a logica cadastral passa ao servico. LCOM e LCOM* sao sensiveis a getters/setters e campos: valores altos em representacoes e entidades nao bastam para inferir responsabilidades indevidas. CBO estatico nao mede dependencia operacional, latencia HTTP, consistencia distribuida ou disponibilidade. Os resultados nao demonstram melhoria uniforme de acoplamento, coesao ou complexidade nem causalidade entre Strangler Fig e qualidade global.

Proveniencia: proveniencia.json identifica HEAD no momento da coleta; fontes-avaliadas.json identifica cada arquivo efetivamente analisado, inclusive se havia alteracoes locais. O commit posterior de evidencias nao altera essas fontes. CSVs brutos nao foram normalizados ou reescritos. A baseline original em historico/metricas/baseline-original-c36452a permanece intacta.
'''
(OUT/'relatorio.md').write_text(report,encoding='utf-8')
print(md(summary));print('PASS: baseline original == recoleta historica para todos os valores nao relacionados a caminho')
