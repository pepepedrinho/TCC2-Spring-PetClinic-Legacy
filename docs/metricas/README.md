# Métricas CK da implementação vigente

| Cenário | Fontes | CSVs |
| --- | --- | --- |
| A · Monólito original | `src/main/java` de `c36452a` | [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv) |
| B · Monólito adaptado | `projects/monolith/src/main/java` | [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv) |
| C · Owners Service | `projects/owners-service/src/main/java` | [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv) |

A campanha vigente mede a V3 após a integração de `OwnerPetsController` ao `PetController`. A [comparação experimental](comparacao-petcontroller.md) apresenta médias, totais e variações por tipo. A [proveniência](proveniencia.json) registra o binário, os parâmetros e os hashes. As [coletas e evidências anteriores](../historico/README.md) preservam o contexto de cada etapa.

Antes de publicar uma coleta, arquive a campanha vigente em `docs/historico/` com a revisão e a proveniência correspondentes. Preserve A como referência e publique os novos B e C com os mesmos nomes de cenário.

Na revisão a medir, execute `./scripts/collect-ck.ps1 -V3 -CkJar $ckJar` com `JAVA17_HOME` definido. Para medir a árvore de trabalho, acrescente `-WorkingTree`. A saída do coletor identifica a V3 do monólito e do serviço como D e E, respectivamente. Use essas saídas para publicar B e C. A configuração e as raízes de fontes constam na proveniência.
