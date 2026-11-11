# Métricas CK vigentes da V3

B e C foram recoletados no commit de código `f89508d8266c1e1f6c1d9f6cfecea25a9511c6c7`. A usa os CSVs históricos do PetClinic de 2017, revisão `c36452a2c34443ae26b4ecbba4f149906af14717`, verificados contra a recoleta e preservados byte a byte.

Coleta em `2026-11-11T12:11:03.6810028Z` (UTC), com CK `0.7.1-SNAPSHOT`, Java 17.0.12 e parâmetros `false 0 false`. [Proveniência](../historico/2026-11-11-organizacao-final/proveniencia-final.json)

| Cenário | Raiz Java analisada | Métodos CK | CSVs |
| --- | --- | ---: | --- |
| A · Monólito original | `src/main/java` da referência histórica | 91 | [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv) |
| B · Monólito adaptado | `projects/monolith/src/main/java` | 94 | [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv) |
| C · Owners Service | `projects/owners-service/src/main/java` | 32 | [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv) |

| Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A-monolito-original | 24 | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B-monolito-adaptado | 28 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| C-owners-service | 9 | 6,3333 | 4,4444 | 5,1111 | 4,1111 | 0,1512 |

Para reproduzir, configure `JAVA17_HOME` e execute `./scripts/collect-ck.ps1 -V3 -CkJar $ckJar` na revisão medida. O coletor identifica as saídas V3 de produção como D e E, publicadas aqui como B e C.

[Coleta anterior](../historico/2026-11-11-pre-organizacao-final-4732eba/README.md) · [Comparação por etapa](../historico/2026-11-11-organizacao-final/relatorio.md)
