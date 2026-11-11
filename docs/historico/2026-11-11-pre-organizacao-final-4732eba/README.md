# V3 antes da organização final

Coleta de 11/11/2026 às 07:55 de Brasília (10:55 UTC), arquivada do commit `4732ebacce7193689808011f6e593b3b6f78c9b0` e da tag `tcc2-v3-pre-organizacao-final`. Os seis CSVs são cópias byte a byte dos arquivos publicados nessa revisão.

B e C medem a V3 com MVC e REST separados, domínios de Pets e Visits reorganizados e `OwnerLookup`. A representa o monólito histórico. A coleta usou CK `0.7.1-SNAPSHOT` e `false 0 false`. [Proveniência original](../experimento-mvc-rest/02-pacotes-reorganizados/proveniencia.json)

| Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A-monolito-original | 24 | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B-monolito-adaptado | 28 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| C-owners-service | 10 | 6,2 | 4,2 | 4,6 | 4,1 | 0,1756 |
