# Métricas CK da V3 definitiva

A publicação `tcc2-v3-final-mvc-rest-modulos` usa os fontes de produção da revisão `0409b10060f1b324fa97d26b6e0ea6929b5c1493` para B e C. A usa a referência histórica do PetClinic de 2017, commit `c36452a2c34443ae26b4ecbba4f149906af14717`.

A campanha foi concluída em **11/11/2026 às 07:55 de Brasília** (10:55 UTC), com CK `0.7.1-SNAPSHOT`, Java 17.0.12 e os parâmetros `false 0 false`. Os hashes dos fontes e dos seis CSVs foram verificados na consolidação. Para os fontes, foi considerada a conversão LF/CRLF do checkout Windows. [Proveniência da coleta](../historico/experimento-mvc-rest/02-pacotes-reorganizados/proveniencia.json)

| Cenário | Raiz Java analisada | Tipos CK | Métodos CK | CSVs |
| --- | --- | ---: | ---: | --- |
| A · Monólito original | `src/main/java` da referência histórica | 24 | 91 | [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv) |
| B · Monólito adaptado | `projects/monolith/src/main/java` | 28 | 94 | [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv) |
| C · Owners Service | `projects/owners-service/src/main/java` | 10 | 34 | [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv) |

| Cenário | CBO médio | WMC médio | RFC médio | LCOM médio | LCOM* médio |
| --- | ---: | ---: | ---: | ---: | ---: |
| A | 6,6667 | 4,9167 | 3,4583 | 5,8750 | 0,2566 |
| B | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| C | 6,2000 | 4,2000 | 4,6000 | 4,1000 | 0,1756 |

Para reproduzir a campanha, configure `JAVA17_HOME` e execute `./scripts/collect-ck.ps1 -V3 -CkJar $ckJar` na revisão marcada. O coletor identifica as saídas de produção da V3 como D e E, publicadas aqui como B e C. Compare os valores desconsiderando a coluna de caminho e a ordem das linhas.

[Índice das campanhas anteriores](../historico/README.md) · [Comparação experimental](../historico/experimento-mvc-rest/relatorio.md)
