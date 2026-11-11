# TCC2 · Refatoração estrutural das integrações HTTP da V3

> Registro histórico da etapa de padronização das integrações. Consulte a [documentação da V3 final](README.md) para o estado após a unificação.

A refatoração foi feita no worktree `.work/v3`, na branch `tcc2-strangler-fig-v3`. O estado inicial era limpo, em `12b438e45a8f8bfd6383bfecf2ee648834bdaebe`. No worktree de `main`, já havia uma edição em `projects/monolith/src/main/java/org/springframework/samples/petclinic/owner/Owner.java` e o diretório não rastreado `dashboard/`. Ambos foram preservados.

## Estrutura anterior e final

As árvores mostram os pacotes Java de produção e os tipos relevantes para a refatoração.

```text
Monólito · antes
org.springframework.samples.petclinic/
├── PetClinicApplication.java
├── integration/
│   ├── IntegrationErrorHandler.java
│   └── OwnerIntegrationAdapter.java
├── owner/
│   ├── Owner.java
│   ├── OwnerPetsController.java
│   ├── OwnerRepository.java
│   ├── PetController.java
│   └── VisitController.java
├── model/
├── system/
├── vet/
└── visit/

Monólito · depois
org.springframework.samples.petclinic/
├── PetClinicApplication.java
├── integration/
│   ├── IntegrationErrorHandler.java
│   └── OwnersServiceHttpClient.java
├── owner/
│   ├── Owner.java
│   ├── OwnerPetsController.java
│   ├── PetController.java
│   └── VisitController.java
├── model/
├── system/
├── vet/
└── visit/

Owners Service · antes
br.edu.tcc/
└── owners/
    ├── LegacyPetsClient.java
    ├── Owner.java
    ├── OwnerController.java
    ├── OwnerRepository.java
    ├── OwnerWebController.java
    └── OwnersApplication.java

Owners Service · depois
br.edu.tcc/
├── OwnersApplication.java
├── integration/
│   ├── IntegrationErrorHandler.java
│   └── LegacyPetsHttpClient.java
└── owners/
    ├── Owner.java
    ├── OwnerController.java
    ├── OwnerRepository.java
    └── OwnerWebController.java
```

## Classes e dependências

| Classe anterior | Classe final | Arquivos diretamente afetados |
| --- | --- | --- |
| `integration.OwnerIntegrationAdapter` | `integration.OwnersServiceHttpClient` | `PetController.java`, `VisitController.java`, `PetControllerTests.java`, `VisitControllerTests.java`, `ClinicServiceTests.java`, `IntegrationErrorHandlerTests.java` e o teste renomeado `OwnersServiceHttpClientTests.java`. |
| `br.edu.tcc.owners.LegacyPetsClient` | `br.edu.tcc.integration.LegacyPetsHttpClient` | `OwnerWebController.java`, `OwnerWebTests.java` e o novo `br.edu.tcc.integration.IntegrationErrorHandler.java`. |
| `br.edu.tcc.owners.OwnersApplication` | `br.edu.tcc.OwnersApplication` | O mesmo filtro de barra final acompanha a classe. O pacote raiz permite que o Spring Boot descubra os componentes dos dois subpacotes, a entidade e o repositório JPA. |

O `OwnerRepository` remoto foi removido do monólito. `PetController` e `VisitController` injetam `OwnersServiceHttpClient` e chamam `findById(ownerId)` diretamente. O `OwnerRepository` do Owners Service continua sendo a interface Spring Data JPA usada pelos controllers de Owner para acessar o H2.

`OwnerWebController` usa `LegacyPetsHttpClient` para a consulta em lote. Os records públicos aninhados `PetSummary` e `VisitSummary` mantêm o contrato da projeção e ficam acessíveis entre pacotes. A exceção da integração tem visibilidade de pacote. Os clientes mantêm conexão de 1,5 s e leitura de 3 s. Os contratos são `GET /api/owners/{id}` e `POST /internal/owner-pets`.

## Decisão A · Handlers dedicados nas duas aplicações

No monólito, `IntegrationErrorHandler` recebe exclusivamente `IntegrationFailure`. O cliente captura `RestClientException` e `HttpMessageConversionException` na fronteira HTTP. O handler preserva o 400 e o 404 recebidos do Owners Service, com os motivos `Invalid owner data` e `Owner or Pet not found`. Outros erros HTTP remotos, falhas de transporte e respostas inválidas ou incompatíveis se tornam 503. Os fluxos de edição de Pet e criação de Visit também usam o 404 do handler quando o Pet inexiste ou pertence a outro Owner. Esse último caso é uma validação funcional local.

A resolução do monólito usa `sendError` para 400 e 404, permitindo o despacho para a página de erro. Para 503, ela define o status e renderiza `ownersUnavailable` diretamente. Remover o handler sem substituir essa resolução perderia a página de indisponibilidade e os mapeamentos dos dois controllers. O Spring MVC 4.3 resolve exceções anotadas com `@ResponseStatus` via `sendError`. A escolha direta da view de indisponibilidade e o mapeamento centralizado já estão implementados no advice. Erros de binding, como conversão de parâmetros, continuam com o MVC. [Spring MVC 4.3.9](https://docs.spring.io/spring-framework/docs/4.3.9.RELEASE/javadoc-api/org/springframework/web/servlet/mvc/annotation/ResponseStatusExceptionResolver.html).

O Owners Service já resolvia a indisponibilidade do legado com `ResponseStatusException`. O resolver do Spring MVC 6 faz o despacho via `sendError`. Para aplicar a estratégia comum pedida, o novo advice recebe somente `LegacyPetsUnavailableException`, lançada pelo cliente quando o legado falha ou devolve uma projeção incompleta. Ele despacha 503 com o mesmo motivo, `Pets and visits are unavailable`, e delega a página ao fluxo padrão do Boot. O 404 por Owner inexistente continua no MVC ou REST local. A validação de entrada e a resposta a falhas de transporte no Gateway têm seus próprios mecanismos. [Spring MVC 6.2](https://docs.spring.io/spring-framework/docs/6.2.15/javadoc-api/org/springframework/web/servlet/mvc/annotation/ResponseStatusExceptionResolver.html).

A centralização preserva o contrato de erro dos formulários legados e explicita onde o Owners Service converte uma falha de integração em resposta web. No Owners Service, o mecanismo nativo já era suficiente para o caso isolado de 503. O benefício do novo handler é alinhar a organização das duas aplicações, ao custo de dois tipos Java adicionais.

## Validação

| Módulo | Maven e Java | Aprovados | Falhas | Erros | Ignorados |
| --- | --- | ---: | ---: | ---: | ---: |
| Monólito | Maven 3.9.11 no build Docker, Java 8 | 48 | 0 | 0 | 1 |
| Owners Service | Maven 3.9.11 no build Docker, Java 17 | 10 | 0 | 0 | 0 |
| Gateway | Maven 3.9.11 local, Java 17 | 1 | 0 | 0 | 0 |
| Total | | 59 | 0 | 0 | 1 |

Os três módulos foram compilados e empacotados. `CrashControllerTests` já tinha `@Ignore` na V3 anterior. Os testes do monólito verificaram 400, 404, a view de 503, timeout, erro de conexão e resposta malformada. O teste de indisponibilidade do Owners Service agora simula uma falha de transporte no cliente e verifica o status e o motivo da resposta MVC. O contexto encontrou um repositório JPA e os testes web e REST confirmaram seu uso.

A execução final de `python scripts/acceptance.py` aprovou 79 requisições pelo Gateway na porta 18088. O Compose usou o projeto `tcc2-v3-refatoracao-http-20261109` e volumes próprios de HSQLDB e H2. O roteiro verificou pesquisa e detalhe de Owner, cadastro e edição de Owner e Pet, registro de Visit, renderização, redirecionamentos, recursos estáticos, Owner inexistente, vínculo inválido e bloqueio da rota interna. O teste reiniciou os cinco contêineres com os mesmos volumes, confirmou a persistência e simulou a indisponibilidade e a recuperação de cada aplicação. Foram observados os resultados esperados de uma resposta 400, doze 404 e seis 503. [Evidência HTTP](evidencias/http-compose-refatoracao.json).

A tentativa inicial de aceitação usou nomes de projeto divergentes entre a subida e o roteiro, causando conflito de porta no reinício. A repetição usou o mesmo nome explícito nas duas operações. O build local da baseline compilou o monólito e o Owners Service, mas os testes do serviço esbarraram no bloqueio de loopback do sandbox. As execuções finais usaram Docker para os módulos alterados e uma JVM com memória limitada e acesso ao loopback para a suíte do Gateway.

## Métricas CK

A nova coleta usou a CK `0.7.1-SNAPSHOT`, o mesmo SHA-256 do JAR histórico, Java 17.0.12 e os parâmetros `false 0 false`. A comparação cobre os mesmos três diretórios de código Java de produção. Os fontes em `HEAD` coincidem com os da revisão da coleta anterior, `7c1d0e8052c3bfae7c7ea5d0248b953e876a41b1`. A coleta nova leu os fontes refatorados no worktree, com limites de memória na JVM. Os hashes dos fontes e dos CSVs registram o estado medido. [Coleta anterior](metricas/proveniencia.json) · [Nova coleta](metricas-refatoracao/proveniencia.json).

| Módulo | Tipos | Métodos | CBO médio | WMC médio | RFC médio | LCOM médio | LCOM* médio |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Monólito | 29 → 28 | 96 → 95 | 6,0000 → 6,1429 | 4,5172 → 4,6429 | 4,3448 → 4,5000 | 5,7241 → 5,9286 | 0,2106 → 0,2181 |
| Owners Service | 9 → 11 | 33 → 35 | 6,7778 → 5,9091 | 4,5556 → 3,9091 | 5,3333 → 4,5455 | 5,7778 → 4,7273 | 0,1799 → 0,1472 |
| Gateway | 2 → 2 | 2 → 2 | 3,5000 | 1,0000 | 1,5000 | 0,0000 | 0,0000 |

- **Monólito.** O CBO do cliente cai de 9 para 8 com a remoção da dependência da interface. Os cinco indicadores de `PetController` e `VisitController` permanecem iguais. A CK também registra queda de uma unidade em `cboModified` e `fanin` de `Owner` e `VisitRepository`. A remoção de um tipo de valores baixos altera a composição das médias.
- **Owners Service.** Os cinco indicadores dos tipos correspondentes são iguais após associar os nomes qualificados antigos e novos, inclusive os tipos aninhados e anônimo do cliente. As médias caem com a inclusão do advice e da exceção. A visibilidade dos records e do cliente mudou e aparece nos campos de modificadores da CK.
- **Gateway.** Os CSVs normalizados de tipos e métodos reproduzem os resultados anteriores.

- [Monólito · tipos](metricas-refatoracao/B-monolito-adaptado/class.csv) · [métodos](metricas-refatoracao/B-monolito-adaptado/method.csv)
- [Owners Service · tipos](metricas-refatoracao/C-owners-service/class.csv) · [métodos](metricas-refatoracao/C-owners-service/method.csv)
- [Gateway · tipos](metricas-refatoracao/F-gateway-v3/class.csv) · [métodos](metricas-refatoracao/F-gateway-v3/method.csv)

## Conclusão arquitetural

A leitura dos controllers agora distingue o acesso local ao banco da consulta HTTP pelo tipo e pelo nome da dependência. A remoção da interface de um método simplifica a navegação entre os formulários legados e o cliente remoto. A separação entre a classe de inicialização, o domínio Owner e o pacote de integração torna a divisão de responsabilidades visível na árvore de fontes. Essas mudanças sustentam uma melhora de legibilidade. As médias CK são sensíveis à população de tipos e, por si só, não demonstram ganho de qualidade ou desempenho.
