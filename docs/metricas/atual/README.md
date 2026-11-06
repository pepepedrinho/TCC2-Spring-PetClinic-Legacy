# Coleta CK definitiva · Strangler Fig V2

A campanha de 6 de novembro de 2026 usa a baseline `c36452a` e os fontes Java de produção da V2 no commit `1d0a3a5`. Esse commit incorpora a simplificação de `Owner.addPet()`. A [proveniência](proveniencia.json) registra revisões completas, SHA-256 dos fontes exportados e dos seis CSVs, ambiente e referências para a conferência dos resultados.

- A · Monólito original. [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv)
- B · Monólito adaptado V2. [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv)
- C · Owners Service. [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv)

A recoleta dos três cenários reproduziu todos os campos dos seis CSVs de referência após desconsiderar a coluna `file` e a ordem das linhas. Os conjuntos de fontes também conferiram, admitindo a diferença de finais de linha entre exportação Git e checkout. Os CSVs desta pasta são as saídas brutas da recoleta.

## Identificar os tipos comparáveis

A interseção da coluna `class` entre A e B contém 24 nomes totalmente qualificados. A correspondência é nominal e algumas responsabilidades mudaram, como as de `Owner` e `OwnerRepository`. Os quatro nomes presentes só em B são:

- `org.springframework.samples.petclinic.integration.IntegrationErrorHandler`
- `org.springframework.samples.petclinic.integration.IntegrationErrorHandler$IntegrationFailure`
- `org.springframework.samples.petclinic.integration.OwnerIntegrationAdapter`
- `org.springframework.samples.petclinic.system.LegacyDatabaseInitializer`

O `Owner` em `br.edu.tcc.owners` no cenário C representa o cadastro do microsserviço e tem responsabilidades diferentes das do tipo homônimo no monólito.

## Efeito da simplificação de Owner.addPet()

O método usa diretamente `Set.add()` e mantém a associação no lado de `Pet`. O teste `OwnerTests` cobre Pets novos, persistidos e chamadas repetidas com as mesmas instâncias. Entre B antes e depois da mudança, o RFC caiu de 16 para 15 e o WMC de 19 para 18. Entre as cinco métricas analisadas, CBO, LCOM e LCOM* ficaram estáveis.

| Métrica de `Owner` | A · Original | B · Antes | B · Definitivo | C · Serviço |
| --- | ---: | ---: | ---: | ---: |
| RFC | 15 | 16 | 15 | 0 |
| WMC | 19 | 19 | 18 | 12 |
| CBO | 13 | 9 | 9 | 9 |
| LCOM | 58 | 58 | 58 | 36 |
| LCOM* | 0,78846157 | 0,78846157 | 0,78846157 | 0,77777773 |

[Evidências funcionais](../../v2/evidencias-addpet/) · [Coleta anterior à simplificação](../historico/2026-11-06-v2-pre-refatoracao-addpet-1e8fa99/relatorio.md)

## Reproduzir

Use PowerShell 7, Git, `tar`, JDK 17 e o JAR com dependências da CK `0.7.1-SNAPSHOT`. O commit de origem registrado para a CK é `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`. O coletor verifica o SHA-256 do JAR, `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`.

Na raiz de um checkout da V2 consolidada, defina `JAVA17_HOME` e `$ckJar` com o caminho do JAR.

```powershell
./scripts/collect-ck.ps1 -CkJar $ckJar
```

O [coletor](../../../scripts/collect-ck.ps1) exporta A da baseline e B e C de `HEAD` com `git archive`. Para cada raiz Java de produção, invoca `java -jar <JAR> <fontes> false 0 false <saída>`. Os argumentos desativam a busca de JARs, usam a partição automática e desativam relatórios de métricas de variáveis e campos. Cada execução usa uma nova pasta `.work/ck-*`.

Use os hashes do manifesto para verificar os seis arquivos versionados. Para comparar uma recoleta, compare as linhas como multiconjuntos sem a coluna `file`. O caminho absoluto e a ordem de emissão podem variar entre execuções.
