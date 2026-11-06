# Coleta CK definitiva · Strangler Fig V2

A campanha de 6 de novembro de 2026 usa a baseline `c36452a` e os fontes Java de produção da V2 no commit `1d0a3a5`. Esse commit incorpora a simplificação de `Owner.addPet()`. A [proveniência](proveniencia.json) registra revisões completas, SHA-256 dos fontes exportados e dos seis CSVs, ambiente e referências para a conferência dos resultados.

A análise mede a estrutura estática dos fontes Java e permite comparar tipos equivalentes do monólito antes e depois da extração de Owners. As campanhas anteriores e os diagnósticos estão no [histórico de métricas](../../historico/metricas/README.md).

| Cenário | Escopo | Tipos CK | Métodos CK | CSVs |
| --- | --- | ---: | ---: | --- |
| A · Monólito original | `src/main/java` de `c36452a` | 24 | 91 | [Tipos](A-monolito-original/class.csv) · [Métodos](A-monolito-original/method.csv) |
| B · Monólito adaptado V2 | `projects/monolith/src/main/java` com `Owner.addPet()` simplificado | 28 | 106 | [Tipos](B-monolito-adaptado/class.csv) · [Métodos](B-monolito-adaptado/method.csv) |
| C · Owners Service | `projects/owners-service/src/main/java` da V2 | 4 | 20 | [Tipos](C-owners-service/class.csv) · [Métodos](C-owners-service/method.csv) |

`class.csv` contém uma linha por tipo emitido pela CK, incluindo interfaces e tipos internos. `method.csv` detalha os métodos e construtores emitidos pela ferramenta. A coluna `file` registra o caminho da execução e a coluna `class` identifica o tipo pelo nome totalmente qualificado. A CK não inclui os testes no escopo acima.

O Gateway permanece na arquitetura e na aceitação HTTP. Seu roteamento depende da configuração do framework, e sua pequena população de tipos Java agrega pouca informação à comparação estrutural entre os monólitos e a extração de Owners. As medições anteriores do Gateway estão no histórico.

CBO e RFC descrevem aspectos do acoplamento estático e da resposta de um tipo. WMC descreve complexidade, enquanto LCOM e LCOM* medem aspectos de coesão. São medidas de código que precisam ser interpretadas junto das responsabilidades de cada tipo. Chamadas HTTP, latência, disponibilidade e comportamento após reinício são questões do sistema distribuído e exigem evidências próprias. A variação das métricas CK, por si só, é insuficiente para estabelecer melhoria arquitetural.

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

[Evidências funcionais](../../historico/evidencias/evidencias-addpet/) · [Coleta anterior à simplificação](../../historico/metricas/2026-11-06-v2-pre-refatoracao-addpet-1e8fa99/relatorio.md)

## Reproduzir

Use PowerShell 7, Git, `tar`, JDK 17 e o JAR com dependências da CK `0.7.1-SNAPSHOT`. O commit de origem registrado para a CK é `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`. O coletor verifica o SHA-256 do JAR, `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`.

Na raiz de um checkout da V2 consolidada, defina `JAVA17_HOME` e `$ckJar` com o caminho do JAR.

```powershell
./scripts/collect-ck.ps1 -CkJar $ckJar
```

O [coletor](../../scripts/collect-ck.ps1) exporta A da baseline e B e C de `HEAD` com `git archive`. Para cada raiz Java de produção, invoca `java -jar <JAR> <fontes> false 0 false <saída>`. Os argumentos desativam a busca de JARs, usam a partição automática e desativam relatórios de métricas de variáveis e campos. Cada execução usa uma nova pasta `.work/ck-*`.

Use os hashes do manifesto para verificar os seis arquivos versionados. Para comparar uma recoleta, compare as linhas como multiconjuntos sem a coluna `file`. O caminho absoluto e a ordem de emissão podem variar entre execuções.
