# Métricas CK vigentes do experimento

| Cenário | Escopo | CSVs |
| --- | --- | --- |
| A · Monólito original | `src/main/java` de `c36452a` | [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv) |
| B · Monólito adaptado | `projects/monolith/src/main/java` | [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv) |
| C · Owners Service | `projects/owners-service/src/main/java` | [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv) |

B e C correspondem ao experimento com controladores separados e pacotes reorganizados. A [proveniência](proveniencia.json) registra os escopos, parâmetros e hashes. O [histórico do experimento](../historico/experimento-mvc-rest/README.md) preserva as configurações anteriores e a comparação.
