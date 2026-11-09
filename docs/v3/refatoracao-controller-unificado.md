# TCC2 · Unificação dos controllers de Owner na V3

> Auditoria da unificação, com os resultados da coleta que serve de referência para a [V3 final](README.md).

A V3 passou a concentrar as 12 ações REST e MVC de Owner em `br.edu.tcc.owners.OwnerController`. O controller usa `OwnerRepository` para acessar o H2 e `LegacyPetsHttpClient` para buscar Pets e Visits no monólito. O Owners Service voltou ao tratamento nativo do Spring para o 503 da integração. O monólito conserva seu handler dedicado.

## Estado inicial

O trabalho ocorreu no worktree `.work/v3`, na branch `tcc2-strangler-fig-v3`, com `HEAD` em `12b438e45a8f8bfd6383bfecf2ee648834bdaebe`. O estado medido como V3 anterior incluía 27 entradas locais no Git, provenientes da refatoração precedente e de suas evidências. Antes da edição, foram registrados o status por arquivo, os hashes, o diff binário e uma cópia dos arquivos modificados ou novos em `.work/controller-unificado-20261109-133435`. Os hashes dos fontes Java de produção conferiram com a proveniência da coleta anterior. As alterações já existentes em `main` foram preservadas. [Estado inicial na proveniência](metricas-controller-unificado/proveniencia.json) · [Refatoração anterior](refatoracao-integracoes-http.md).

## Estrutura do Owners Service

```text
Antes                               Depois
br.edu.tcc/                         br.edu.tcc/
├── OwnersApplication.java           ├── OwnersApplication.java
├── owners/                         ├── owners/
│   ├── Owner.java                  │   ├── Owner.java
│   ├── OwnerController.java        │   ├── OwnerController.java
│   ├── OwnerRepository.java        │   └── OwnerRepository.java
│   └── OwnerWebController.java     └── integration/
└── integration/                       └── LegacyPetsHttpClient.java
    ├── LegacyPetsHttpClient.java
    └── IntegrationErrorHandler.java
```

`OwnerController` usa `@Controller`. Os métodos REST que retornam `Owner` ou `List<Owner>` usam `@ResponseBody`. A criação REST preserva `ResponseEntity`, JSON, HTTP 201 e `Location`. As ações MVC devolvem as views e os redirecionamentos originais. As duas famílias de endpoints compartilham uma consulta privada por ID com 404. A validação de formulário continua usando `BindingResult` e a lista permitida do binder. A API conserva o `@RequestBody` validado, e as edições mantêm suas fronteiras transacionais. [Spring MVC 6.2 sobre serialização de respostas](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/responsebody.html).

### Rotas preservadas

| Família | Método e caminho | Comportamento |
| --- | --- | --- |
| REST | `GET /api/owners` | Pesquisa por prefixo, com lista JSON. |
| REST | `GET /api/owners/{id}` | Owner em JSON ou 404. |
| REST | `POST /api/owners` | Cadastro com 201, JSON e `Location`. Entrada inválida recebe 400. |
| REST | `PUT /api/owners/{id}` | Edição com retorno JSON. Entrada inválida recebe 400 e ID inexistente recebe 404 com entrada válida. |
| REST | `DELETE /api/owners/{id}` | 409 para Owner existente, preservando referências legadas, e 404 para ID inexistente. |
| MVC | `GET /owners/find` | Formulário de pesquisa. |
| MVC | `GET /owners` | Formulário com mensagem para zero resultados, redirecionamento para um resultado e lista com Pets para vários resultados. |
| MVC | `GET /owners/new` e `POST /owners/new` | Formulário de criação, erros de validação ou redirecionamento ao detalhe após gravação. |
| MVC | `GET /owners/{ownerId}` | Detalhe com Pets e Visits, 404 para Owner inexistente ou 503 se a consulta legada falhar. |
| MVC | `GET /owners/{ownerId}/edit` e `POST /owners/{ownerId}/edit` | Formulário, erros de validação ou redirecionamento ao detalhe após edição. |

A comunicação de leitura do Owners Service segue pelo `POST /internal/owner-pets`. O monólito continua consultando `GET /api/owners/{id}` para os formulários legados. A execução pelo Gateway confirmou as fronteiras de rota, o bloqueio do endpoint interno e o uso de bancos independentes.

### Tratamento de erro

No Owners Service, `LegacyPetsHttpClient` lança `ResponseStatusException` com 503 e o motivo `Pets and visits are unavailable`. A captura abrange transporte, erros HTTP, falha de conversão e resposta nula ou sem algum dos IDs solicitados. Os limites de conexão e leitura continuam em 1.500 ms e 3.000 ms. Saíram `IntegrationErrorHandler` e `LegacyPetsUnavailableException`. A pesquisa com múltiplos resultados e a página de detalhe continuam sendo os fluxos que precisam da integração para renderizar.

O handler do monólito foi preservado byte a byte. Ele centraliza a diferenciação de 400, 404 e 503 para a integração de Owner, além de renderizar `ownersUnavailable` para 503. Os testes do monólito verificaram os mapeamentos e a view específica. O cliente do Owners Service precisa apenas converter a falha da projeção para 503, atendido pelo resolver padrão. [Spring MVC 6.2 sobre resolução de status e motivo](https://docs.spring.io/spring-framework/docs/6.2.15/javadoc-api/org/springframework/web/servlet/mvc/annotation/ResponseStatusExceptionResolver.html).

## Arquivos desta etapa

| Operação | Arquivos |
| --- | --- |
| Modificados | `OwnerController.java`, `LegacyPetsHttpClient.java`, `OwnerWebTests.java` e `scripts/collect-ck.ps1`. |
| Excluídos | `OwnerWebController.java` e `IntegrationErrorHandler.java` do Owners Service. A exceção aninhada saiu de `LegacyPetsHttpClient.java`. |
| Criados | Este relatório, `evidencias/http-compose-controller-unificado.json` e a pasta `metricas-controller-unificado/` com CSVs e proveniência. |

A opção `-WorkingTree` do coletor copia os fontes da V3 para um snapshot de análise, incluindo os arquivos locais novos. O modo histórico continua exportando revisões do Git. A busca em fontes e testes do Owners Service ficou sem referências ao controller, handler e exceção removidos. As referências históricas documentam os estados medidos.

## Validação

| Módulo | Execução | Aprovados | Falhas | Erros | Ignorados |
| --- | --- | ---: | ---: | ---: | ---: |
| Monólito | Maven Wrapper 3.3.3, Java 8 | 48 | 0 | 0 | 1 |
| Owners Service | Maven 3.9.11 no Docker, Java 17 | 12 | 0 | 0 | 0 |
| Gateway | Maven 3.9.11, Java 17 | 1 | 0 | 0 | 0 |
| Total | | 61 | 0 | 0 | 1 |

Os três módulos compilaram e foram empacotados. `CrashControllerTests` conserva o `@Ignore` histórico. No Owners Service, a parametrização do teste de 503 acrescentou dois casos aos 59 aprovados anteriormente. Ele cobre transporte, JSON inválido e projeção incompleta, verifica o motivo da resposta e confirma que o formulário de edição e a API continuam acessíveis. As demais verificações Maven foram preservadas. A primeira invocação local do monólito parou na interpretação dos argumentos pelo PowerShell. A execução pelo Wrapper, com argumentos em array, passou.

A suíte `scripts/acceptance.py` aprovou 79 verificações pelo Gateway na porta 18089, no projeto Compose isolado `tcc2-v3-controller-20261109`. O roteiro verificou formulários, pesquisa, listagem e detalhe de Owner, JSON, cadastro e edição, DELETE, Pets, Visits, recursos estáticos e bloqueio da rota interna. As respostas esperadas incluíram um 201, um 400, doze 404, dois 409 e seis 503. A suíte reiniciou os cinco contêineres com os mesmos volumes, confirmou a persistência e exercitou a queda e a recuperação do Owners Service e do monólito. [Evidência HTTP](evidencias/http-compose-controller-unificado.json).

## Coleta e comparação CK

A coleta usou `scripts/collect-ck.ps1 -V3 -WorkingTree`, CK `0.7.1-SNAPSHOT`, Java 17.0.12, o SHA-256 histórico do JAR e os parâmetros `false 0 false`. Foram analisados os fontes Java de produção, com hashes dos snapshots e dos CSVs. Os rótulos A, B e C abaixo descrevem os estados desta comparação. Os diretórios mantêm os identificadores históricos dos cenários.

| Estado | Componente | Tipos | Métodos | CBO médio | WMC médio | RFC médio | LCOM médio | LCOM* médio |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| A · Original | Monólito | 24 | 91 | 6,6667 | 4,9167 | 3,4583 | 5,8750 | 0,2566 |
| B · V3 anterior | Monólito | 28 | 95 | 6,1429 | 4,6429 | 4,5000 | 5,9286 | 0,2181 |
| C · V3 unificada | Monólito | 28 | 95 | 6,1429 | 4,6429 | 4,5000 | 5,9286 | 0,2181 |
| B · V3 anterior | Owners Service | 11 | 35 | 5,9091 | 3,9091 | 4,5455 | 4,7273 | 0,1472 |
| C · V3 unificada | Owners Service | 8 | 32 | 6,2500 | 5,0000 | 5,1250 | 10,2500 | 0,1889 |
| B · V3 anterior | Gateway | 2 | 2 | 3,5000 | 1,0000 | 1,5000 | 0,0000 | 0,0000 |
| C · V3 unificada | Gateway | 2 | 2 | 3,5000 | 1,0000 | 1,5000 | 0,0000 | 0,0000 |

A baseline original tem apenas o monólito. Na passagem de B para C, os CSVs de tipos e métodos do monólito e do Gateway coincidiram, descontando caminhos absolutos e ordem. Os dados anteriores permaneceram em suas pastas. [Proveniência](metricas-controller-unificado/proveniencia.json) · [Resumo em CSV](metricas-controller-unificado/resumo-componentes.csv) · [Comparação por tipo](metricas-controller-unificado/comparacao-tipos-v3.csv).

### Tipos diretamente afetados

| Estado e tipo | Métodos CK | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A · `OwnerController` original do monólito | 9 | 15 | 14 | 17 | 0 | 0,4444 |
| B · `OwnerController` REST | 6 | 16 | 6 | 10 | 0 | 0,1667 |
| B · `OwnerWebController` MVC | 10 | 17 | 15 | 22 | 15 | 0,6750 |
| C · `OwnerController` unificado | 15 | 22 | 20 | 25 | 45 | 0,7333 |
| B e C · `LegacyPetsHttpClient` | 2 | 9 | 5 | 12 | 0 | 0,0000 |
| B e C · `OwnersServiceHttpClient` | 2 | 8 | 5 | 6 | 0 | 0,0000 |
| B · `IntegrationErrorHandler` do Owners Service | 1 | 4 | 1 | 2 | 0 | 0,0000 |
| B · `LegacyPetsUnavailableException` | 1 | 0 | 1 | 0 | 0 | 0,0000 |

O controller unificado reúne cinco ações REST, sete MVC, o binder, a busca privada por ID e um construtor. A passagem de 16 métodos medidos nos dois tipos para 15 no tipo resultante reflete a consolidação dos construtores. A pesquisa MVC concentra as decisões de zero, um e vários resultados e registrou WMC 4 por método, enquanto cada ação REST registrou WMC 1. O WMC 20 do tipo final expressa a concentração desse trabalho. O CBO sobe para 22 em relação a 16 no REST e 17 no MVC. A classe reúne dependências de serialização e resposta HTTP, modelo, binding, JPA e consulta ao legado. O RFC 25 agrega um conjunto de chamadas mais amplo, com operações de persistência e de preparação de views no mesmo tipo.

LCOM e LCOM* aumentam no tipo unificado. O repositório é compartilhado por ações REST e MVC, e o cliente de Pets é usado só por listagem com múltiplos resultados e detalhe. Outras ações trabalham apenas com formulário, validação ou resposta HTTP. Essa distribuição é compatível com menor coesão medida por uso de campos. A comparação é de dois tipos com escopos diferentes para um tipo maior. CBO e RFC dependem dos conjuntos de tipos e chamadas, enquanto LCOM depende de como os métodos usam os campos dentro da classe. Por isso, as métricas individuais foram comparadas separadamente, sem usar a soma dos controllers como equivalente estrutural.

O cálculo de composição abaixo remove apenas as duas linhas de tratamento de erro da tabela de B. Ele mantém os valores dos demais tipos de B e serve para separar o efeito do denominador da mudança interna observada em C.

| Owners Service | Tipos | CBO médio | WMC médio | RFC médio | LCOM médio | LCOM* médio |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| B · Observado | 11 | 5,9091 | 3,9091 | 4,5455 | 4,7273 | 0,1472 |
| B · Excluindo as duas linhas de erro | 9 | 6,7778 | 4,5556 | 5,3333 | 5,7778 | 0,1799 |
| C · Observado | 8 | 6,2500 | 5,0000 | 5,1250 | 10,2500 | 0,1889 |

Retirar os dois tipos pequenos elevaria todas as médias de B. Comparado com esse cálculo intermediário, C tem CBO e RFC médios menores e WMC, LCOM e LCOM* médios maiores. Os cinco indicadores principais dos outros tipos correspondentes permaneceram iguais. A CK registrou uma unidade a menos de `fanin` e `cboModified` em `Owner` e `OwnerRepository`, compatível com a redução de dois controllers consumidores para um. O cliente do microsserviço perdeu a classe interna de exceção, mas conservou seus cinco indicadores principais.

### Limites

Os indicadores descrevem a estrutura Java desta coleta. A análise de classe tem granularidade diferente antes e depois, e a média do componente depende da população. A comparação entre o monólito original e a V3 também inclui a extração de funcionalidades, então o efeito isolado desta etapa é a comparação de B com C. A medição cobre estrutura, enquanto a execução HTTP cobre os cenários exercitados de comportamento.

O coletor também exportou os cenários auxiliares históricos da V2. O cenário `B-monolito-adaptado` da V2 apresentou diferenças de resolução de nomes de tipos, `fanin` e `cboModified` em relação aos CSVs arquivados. Esse cenário auxiliar é distinto da V3 anterior desta comparação. A baseline original reproduziu os CSVs arquivados, e o estado B usou a coleta anterior da V3 validada pelos hashes.

## Conclusão técnica

A unificação reduz o número de pontos de entrada Java para Owner e concentra dependências, decisões e chamadas em uma classe maior. Os clientes e as fronteiras de integração continuam separados. A diferença medida é local à organização do Owners Service, com menor contagem de tipos e maior complexidade e acoplamento no controller individual. Os contratos HTTP exercitados, o acesso ao H2 e a composição com Pets e Visits continuaram funcionando pelo Gateway.
