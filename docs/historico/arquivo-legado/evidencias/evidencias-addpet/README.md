# Evidências de validação de Owner.addPet()

[validacao-original/](validacao-original/) guarda os resumos Surefire e o resultado HTTP da execução de 6 de novembro de 2026, anterior ao commit da refatoração. O monólito teve 61 testes aprovados e um ignorado, incluindo o novo `OwnerTests`. Owners Service teve 6 aprovados e o Gateway, 1. A aceitação registrou 57 verificações HTTP aprovadas.

A [proveniência da CK local](../../metricas/2026-11-06-v2-validacao-addpet-3ccb9e0/proveniencia.json) identifica o estado dos fontes analisados naquela validação. O código e o teste foram incorporados no commit `1d0a3a5`.

## Revalidação da V2 consolidada

Os mesmos fontes de produção e testes foram validados em um clone local isolado. O build usou Java 8 no monólito, Java 17 nos demais módulos e `./scripts/build.ps1 -Offline -SkipGitMetadata`. A opção contorna a falha do plugin legado ao ler metadados Git na worktree e em caminhos com espaços.

| Componente | Aprovados | Ignorados | Falhas | Erros |
| --- | ---: | ---: | ---: | ---: |
| Monólito | 61 | 1 | 0 | 0 |
| Owners Service | 6 | 0 | 0 | 0 |
| Gateway | 1 | 0 | 0 | 0 |

A aceitação passou nas 57 verificações. A migração de teste copiou 11 Owners, 14 Pets e 5 Visits e verificou IDs, vínculos, sequência, integridade da origem e recusa de destino existente. [Surefire](revalidacao/) · [Aceitação HTTP](revalidacao/http-acceptance.json) · [Migração](revalidacao/migration.json)
