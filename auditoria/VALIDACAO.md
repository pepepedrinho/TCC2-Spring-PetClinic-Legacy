# Validação da auditoria

Validação em 11 de novembro de 2026 no worktree `.work/v3`, branch `tcc2-strangler-fig-v3`. A tag anotada `tcc2-v3-pre-auditoria` aponta para `644f9bd61eda40e78c83d8b50df2132acab5a5bf`, o mesmo commit de `tcc2-v3-final`.

## Maven e HTTP

| Procedimento | Resultado |
| --- | --- |
| Maven monólito, Java 8 | 51 aprovados, 1 ignorado pela suíte, 0 falhas |
| Maven Owners Service, Java 17 | 12 aprovados, 0 falhas |
| Maven Gateway, Java 17 | 1 aprovado, 0 falhas |
| `scripts/acceptance.py`, JVMs isoladas com perfil de auditoria | 79 verificações aprovadas |
| `auditoria/coletor/teste_integracao.py`, JVMs isoladas e Chrome headless | Aprovado |
| `docker compose config --quiet`, configuração básica e com réplicas | Aprovado |
| Docker Compose em execução | Pendente. O Docker Desktop encerrou ao iniciar e o daemon Linux não ficou disponível. |

A aceitação HTTP usou arquivos de banco descartáveis e localidade Java inglesa para as mensagens de validação esperadas pelo roteiro. O resultado local está em `.work/acceptance-20261111-105050/http-acceptance.json`, SHA-256 `1d1d1276f2285e460c35aff96b0ceedc02644c7d97c2683c32f5a6e8e96c041d`.

O teste da auditoria confirma a leitura dos recursos estáticos e a execução de `app.js` no Chrome. Exercita a coleta sem amostras, a listagem real de rotas, as contagens e a latência do Gateway após requisições de teste, a falha e recuperação do Owners Service, a interrupção e retomada da coleta após reiniciar o Gateway e a continuidade do PetClinic após parar o coletor. Nas portas de aplicação, as URLs administrativas testadas retornaram HTTP 404. O teste também verificou a ausência de `env` nas interfaces de gerenciamento e a rejeição de `POST /actuator/gateway/refresh` e a restrição do coletor a leitura. A configuração do Compose publica apenas as portas de aplicação do Gateway e da auditoria, ambas em loopback.

## Recoleta CK

Executado `scripts/collect-ck.ps1 -V3 -WorkingTree` com Java 17.0.12, CK `0.7.1-SNAPSHOT` e parâmetros `false 0 false`. O SHA-256 do JAR conferido pelo script é `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`. A recoleta local está em `.work/ck-20261111-104555-297/`. O SHA-256 da sua `proveniencia.json` é `8d07a095596ea45652bbf83c2fb3124c2f44914466601acd6da6c461781e6ef1`.

| Cenário | Tipos | Métodos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| A — Original | 24 | 91 | 6,6667 | 4,9167 | 3,4583 | 5,8750 | 0,2566 |
| B — Adaptado | 28 | 94 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| C — Owners Service | 9 | 32 | 6,3333 | 4,4444 | 5,1111 | 4,1111 | 0,1512 |

Os valores da tabela valem antes e depois da intervenção. Os seis CSVs recoletados diferem em bytes dos publicados porque a CK gravou a coluna `file` com o caminho do snapshot local e a ordem das linhas pode variar. A comparação de todas as colunas e linhas, após normalizar os caminhos e ordenar as linhas, resultou em igualdade para `class.csv` e `method.csv` dos três cenários. Os fontes Java, os scripts de coleta, os CSVs vigentes e os históricos também foram verificados com `git diff` contra a tag da V3. A coleta publicada continua sendo a de [docs/metricas](../docs/metricas/README.md).

## Integridade da importação

Os 14 arquivos do protótipo Python correspondem byte a byte à origem local e aos blobs Git de `auditoria/dashboards/`, conforme verificação SHA-256. A regra de atributos do Git preserva as quebras de linha originais nessa pasta.
