# Coleta CK — centralização posterior de erros

`relatorio.md` apresenta implementação, testes, comparações A/B/C e limitações. Os resultados históricos em `docs/metricas/v2` e `docs/baseline` não foram modificados.

- `A-monolito-original`, `B-monolito-anterior`, `C-monolito-centralizado`, `owners`, `gateway`: CSVs brutos e logs do mesmo JAR CK, parâmetros false 0 false.
- `owners-historico-controle`: recoleta adicional do Owners histórico para explicar a divergência de coesão anterior à centralização; não é um cenário A/B/C.
- `fontes/*.zip`, `fontes-avaliadas.json`, `proveniencia.json`: fontes exatos e rastreabilidade, inclusive de B não commitado.
- `medias-a-b-c-e-componentes.csv`, `variacoes-medias-b-c.csv`, `estatisticas-descritivas.csv`, `somatorios.csv`, `inventario-tipos.csv`, `medias-tipos-equivalentes-b-c.csv`, `classes-equivalentes-b-c.csv`: análise, incluindo tipos adicionados/removidos sem zeros inventados.
- `evidencias/`: builds, Surefire, HTTP, reinicialização, migração e cópias dos procedimentos executados.
- `controles-comparabilidade.json` e `auditoria-preservacao.json`: verificações de fontes e resultados históricos.

Para recalcular tabelas sem escrever no arquivo histórico, copie a campanha para uma nova pasta chamada `v2-centralizacao-erros`. Na raiz do repositório, aponte o analisador para essa cópia:

```powershell
python historico/metricas/2026-11-03-v2-centralizacao-erros-d3fea1a/analisar.py --metrics-dir .work/v2-centralizacao-erros
```

Para nova coleta, extraia os ZIPs de fontes em diretórios de trabalho novos A, B e C. O arquivo A-monolito-original.zip deve ser extraído em A, B-monolito-anterior.zip em B e os três arquivos C-monolito-centralizado.zip, owners.zip e gateway.zip em C. As árvores extraídas preservam src/main/java e os prefixos de módulo.

```powershell
python historico/metricas/2026-11-03-v2-centralizacao-erros-d3fea1a/coletar.py --a <pasta-A> --b <pasta-B> --c <pasta-C> --repo <repositorio-Git> --output <pasta-nova>/v2-centralizacao-erros --ck-jar <mesmo-JAR> --java <JDK17>/bin/java.exe
```

O coletor exige o mesmo SHA-256 do JAR e recusa sobrescrever diretórios já coletados. Use um pai novo mantendo o nome final v2-centralizacao-erros. Defaults do JAR e JDK são os caminhos locais registrados na proveniência; podem ser informados explicitamente. Registre a pasta de execução dos comandos; os caminhos da execução original foram relativos ao workspace deste chat e seus absolutos constam nos CSVs/logs. Para analisar uma nova saída, passe `--metrics-dir <pasta-nova>/v2-centralizacao-erros` ao analisador.

A baseline foi recuperada por Git e a igualdade de todos os campos não relacionados a caminhos foi conferida. B preserva a cópia de trabalho anterior incluindo a organização Java; C identifica o candidato por hashes. O relatório interpretativo não é reescrito pelo analisador: uma nova execução deve ser identificada e interpretada em relatório próprio.

As cópias dos testes em evidencias/procedimentos-executados registram exatamente as adaptações executadas na cópia isolada, inclusive destinos de evidências e Git original usado para SQL. Os scripts canônicos acceptance.py, test-migration.py e collect-ck.ps1 no repositório original continuam intactos. Para repetir aceitação/migração, use outra cópia isolada e ajuste esses caminhos, nunca bancos do usuário.
