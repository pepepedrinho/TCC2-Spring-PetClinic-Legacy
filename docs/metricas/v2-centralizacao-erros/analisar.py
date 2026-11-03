"""Recreate descriptive tables from CK CSVs in this new checkpoint directory."""
from pathlib import Path
import argparse,csv,statistics,zipfile

M=['cbo','wmc','rfc','lcom','lcom*']
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--metrics-dir',type=Path,default=Path(__file__).resolve().parent)
    o=parser.parse_args().metrics_dir
    assert o.name=='v2-centralizacao-erros','Use the separate checkpoint directory'
    def read(p):
        try:s=p.read_text(encoding='utf-8-sig')
        except UnicodeDecodeError:s=p.read_text(encoding='cp1252')
        return list(csv.DictReader(s.splitlines()))
    def save(name,rows):
        with (o/name).open('w',encoding='utf-8',newline='') as f:
            writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
    names={'A':'A-monolito-original','B':'B-monolito-anterior','C':'C-monolito-centralizado','Owners':'owners','Gateway':'gateway'}
    data={k:read(o/v/'class.csv') for k,v in names.items()}
    means=[];desc=[];totals=[];counts=[]
    for k,rows in data.items():
        means.append({'cenario':k,'tipos_CK':len(rows),**{m:statistics.mean(float(r[m]) for r in rows) for m in M}})
        totals.append({'cenario':k,'tipos_CK':len(rows),**{m:sum(float(r[m]) for r in rows) for m in M if m!='lcom*'}})
        for m in M:
            vs=[float(r[m]) for r in rows]
            desc.append({'cenario':k,'metrica':m,'n':len(vs),'media':statistics.mean(vs),'mediana':statistics.median(vs),'min':min(vs),'max':max(vs),'desvio_padrao_populacional':statistics.pstdev(vs)})
        with zipfile.ZipFile(o/'fontes'/(names[k]+'.zip')) as z:java_files=sum(n.endswith('.java') for n in z.namelist())
        counts.append({'cenario':k,'arquivos_java':java_files,'tipos_CK':len(rows),'interfaces':sum(r['type']=='interface' for r in rows),'tipos_internos':sum('$' in r['class'] for r in rows)})
    b={r['class']:r for r in data['B']};c={r['class']:r for r in data['C']}
    eq=[]
    for name in sorted(b.keys()|c.keys()):
        row={'classe':name,'situacao':'removida' if name not in c else 'adicionada (interna)' if name not in b else 'equivalente'}
        for m in M:
            row[m+'_B']=b[name][m] if name in b else ''
            row[m+'_C']=c[name][m] if name in c else ''
            row[m+'_delta']=float(c[name][m])-float(b[name][m]) if name in b and name in c else ''
        eq.append(row)
    bs=next(r for r in means if r['cenario']=='B');cs=next(r for r in means if r['cenario']=='C')
    delta=[{'metrica':m,'media_B':bs[m],'media_C':cs[m],'delta':cs[m]-bs[m],'variacao_percentual':100*(cs[m]-bs[m])/bs[m] if bs[m] else 'nao definida'} for m in M]
    paired=[]
    for m in M:
        before=statistics.mean(float(b[k][m]) for k in b.keys()&c.keys());after=statistics.mean(float(c[k][m]) for k in b.keys()&c.keys())
        paired.append({'metrica':m,'tipos_equivalentes':len(b.keys()&c.keys()),'media_B':before,'media_C':after,'delta':after-before})
    for name,rows in [('medias-a-b-c-e-componentes.csv',means),('estatisticas-descritivas.csv',desc),('somatorios.csv',totals),('inventario-tipos.csv',counts),('classes-equivalentes-b-c.csv',eq),('variacoes-medias-b-c.csv',delta),('medias-tipos-equivalentes-b-c.csv',paired)]:save(name,rows)
    print('Tables recreated from raw CSVs; historical outputs and raw CSVs were not modified.')

if __name__=='__main__':main()
