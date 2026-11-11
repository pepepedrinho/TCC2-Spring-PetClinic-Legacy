# Separação MVC/REST e reorganização dos pacotes

O experimento parte de `eff02afff0f822fdc70359849997815a14ecf0c9`. O estado 00 é a V3 com controladores unificados. O estado 01 separa as interfaces mantendo os pacotes de domínio. O estado 02 também reorganiza os pacotes do monólito. As três revisões são recuperáveis pelas tags `tcc2-v3-pre-separacao-mvc-rest`, `tcc2-exp-controllers-separados` e `tcc2-exp-mvc-rest-modulos`.

## Contratos e estrutura

| Controlador REST | Operações transferidas |
| --- | --- |
| `br.edu.tcc.owners.integration.OwnerRestController` | `search` (GET /api/owners), `get` (GET /api/owners/{id}), `create` (POST /api/owners), `update` (PUT /api/owners/{id}), `delete` (DELETE /api/owners/{id}) |
| `org.springframework.samples.petclinic.integration.PetRestController` | `findPets` (POST /internal/owner-pets), com `!X-PetClinic-Gateway`. Recebe também `formatDate`, `PetSummary` e `VisitSummary`. |

`OwnerLookup` compartilha a consulta de Owner com resposta 404 entre os dois controladores do serviço. No monólito, a montagem do formulário de Pet voltou a exigir `ownerId` porque só atende rotas MVC. Na etapa 02, `Pet.setOwner` passou de `protected` a `public` para manter o vínculo usado por `Owner` e `VisitController` em outros pacotes. O acesso JPA por campos e os nomes das entidades e tabelas foram preservados. O escaneamento parte dos pacotes-raiz das aplicações.

```text
projects/monolith/src/main/java/org/springframework/samples/petclinic/
  integration/  IntegrationErrorHandler, OwnersServiceHttpClient, PetRestController
  owner/        Owner
  pets/         Pet, PetController, PetRepository, PetType, PetTypeFormatter, PetValidator
  visit/        Visit, VisitController, VisitRepository
  model/        BaseEntity, NamedEntity, Person
  system/       CacheConfig, CrashController, WelcomeController
  vet/          Specialty, Vet, VetController, VetRepository, Vets
  PetClinicApplication.java
projects/owners-service/src/main/java/br/edu/tcc/
  integration/         LegacyPetsHttpClient
  owners/              Owner, OwnerController, OwnerLookup, OwnerRepository
  owners/integration/  OwnerRestController
  OwnersApplication.java
```

### Mapeamento dos tipos que mudaram de nome qualificado

| Antes | Depois | Etapa |
| --- | --- | --- |
| `org.springframework.samples.petclinic.owner.PetController$PetSummary` | `org.springframework.samples.petclinic.integration.PetRestController$PetSummary` | 01 |
| `org.springframework.samples.petclinic.owner.PetController$VisitSummary` | `org.springframework.samples.petclinic.integration.PetRestController$VisitSummary` | 01 |
| `org.springframework.samples.petclinic.owner.Pet` | `org.springframework.samples.petclinic.pets.Pet` | 02 |
| `org.springframework.samples.petclinic.owner.PetController` | `org.springframework.samples.petclinic.pets.PetController` | 02 |
| `org.springframework.samples.petclinic.owner.PetRepository` | `org.springframework.samples.petclinic.pets.PetRepository` | 02 |
| `org.springframework.samples.petclinic.owner.PetType` | `org.springframework.samples.petclinic.pets.PetType` | 02 |
| `org.springframework.samples.petclinic.owner.PetTypeFormatter` | `org.springframework.samples.petclinic.pets.PetTypeFormatter` | 02 |
| `org.springframework.samples.petclinic.owner.PetValidator` | `org.springframework.samples.petclinic.pets.PetValidator` | 02 |
| `org.springframework.samples.petclinic.owner.VisitController` | `org.springframework.samples.petclinic.visit.VisitController` | 02 |

Os testes MVC acompanharam os pacotes de seus controladores. `Owner` continua em `owner` no monólito como representação do Owner remoto e associação local com Pets. `br.edu.tcc.owners.Owner` é a entidade persistida pelo serviço.

## Procedimento CK

A coleta usa CK `0.7.1-SNAPSHOT`, Java 17.0.12, o SHA-256 histórico do JAR e os parâmetros `false 0 false`. O estado 00 reutiliza os CSVs da V3 marcada pela tag. Para 01 e 02 foi executado `./scripts/collect-ck.ps1 -V3 -WorkingTree -Java17Home $env:JAVA17_HOME -CkJar $ckJar`. Os escopos são os fontes Java de produção. Os CSVs mantêm todas as colunas emitidas pela CK, com caminhos relativos e ordem estável. A inclui a referência histórica e teve todos os valores reproduzidos nas duas execuções. [Estado 00](00-v3-original/README.md) · [Estado 01](01-controllers-separados/proveniencia.json) · [Estado 02](../../metricas/proveniencia.json).

Cada média dá o mesmo peso a cada tipo emitido pela CK, incluindo interfaces, tipos internos, records e classes anônimas. Os totais somam CBO, WMC, RFC e LCOM por tipo. LCOM* é reportado como média. Percentuais usam o estado inicial de cada contraste como denominador.

| Cenário e estado | Tipos CK | Métodos CK |
| --- | ---: | ---: |
| A · Original histórico | 24 | 91 |
| B · 00 | 27 | 93 |
| B · 01 | 28 | 94 |
| B · 02 | 28 | 94 |
| B · Δ 01 − 00 | +1 (+3,7037%) | +1 |
| B · Δ 02 − 01 | +0 (0%) | +0 |
| B · Δ 02 − 00 | +1 (+3,7037%) | +1 |
| C · 00 | 8 | 32 |
| C · 01 | 10 | 34 |
| C · 02 | 10 | 34 |
| C · Δ 01 − 00 | +2 (+25%) | +2 |
| C · Δ 02 − 01 | +0 (0%) | +0 |
| C · Δ 02 − 00 | +2 (+25%) | +2 |

## Totais por aplicação

| Cenário e estado | CBO | WMC | RFC | LCOM |
| --- | ---: | ---: | ---: | ---: |
| A · Original histórico | 160 | 118 | 83 | 141 |
| B · 00 V3 | 167 | 129 | 126 | 167 |
| B · 01 Separado | 171 | 129 | 127 | 159 |
| B · 02 Pacotes | 171 | 129 | 127 | 159 |
| B · 01 − 00 | +4 | 0 | +1 | -8 |
| B · 01 − 00 (%) | +2,3952 | 0 | +0,7937 | -4,7904 |
| B · 02 − 01 | 0 | 0 | 0 | 0 |
| B · 02 − 01 (%) | 0 | 0 | 0 | 0 |
| B · 02 − 00 | +4 | 0 | +1 | -8 |
| B · 02 − 00 (%) | +2,3952 | 0 | +0,7937 | -4,7904 |
| C · 00 V3 | 50 | 40 | 41 | 82 |
| C · 01 Separado | 62 | 42 | 46 | 41 |
| C · 02 Pacotes | 62 | 42 | 46 | 41 |
| C · 01 − 00 | +12 | +2 | +5 | -41 |
| C · 01 − 00 (%) | +24 | +5 | +12,1951 | -50 |
| C · 02 − 01 | 0 | 0 | 0 | 0 |
| C · 02 − 01 (%) | 0 | 0 | 0 | 0 |
| C · 02 − 00 | +12 | +2 | +5 | -41 |
| C · 02 − 00 (%) | +24 | +5 | +12,1951 | -50 |

## Médias por tipo

| Cenário e estado | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: |
| A · Original histórico | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B · 00 V3 | 6,1852 | 4,7778 | 4,6667 | 6,1852 | 0,2139 |
| B · 01 Separado | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| B · 02 Pacotes | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| B · 01 − 00 | -0,078 | -0,1706 | -0,131 | -0,5066 | +0,0027 |
| B · 01 − 00 (%) | -1,2618 | -3,5714 | -2,8061 | -8,1908 | +1,2624 |
| B · 02 − 01 | 0 | 0 | 0 | 0 | 0 |
| B · 02 − 01 (%) | 0 | 0 | 0 | 0 | 0 |
| B · 02 − 00 | -0,078 | -0,1706 | -0,131 | -0,5066 | +0,0027 |
| B · 02 − 00 (%) | -1,2618 | -3,5714 | -2,8061 | -8,1908 | +1,2624 |
| C · 00 V3 | 6,25 | 5 | 5,125 | 10,25 | 0,1889 |
| C · 01 Separado | 6,2 | 4,2 | 4,6 | 4,1 | 0,1756 |
| C · 02 Pacotes | 6,2 | 4,2 | 4,6 | 4,1 | 0,1756 |
| C · 01 − 00 | -0,05 | -0,8 | -0,525 | -6,15 | -0,0133 |
| C · 01 − 00 (%) | -0,8 | -16 | -10,2439 | -60 | -7,0588 |
| C · 02 − 01 | 0 | 0 | 0 | 0 | 0 |
| C · 02 − 01 (%) | 0 | 0 | 0 | 0 | 0 |
| C · 02 − 00 | -0,05 | -0,8 | -0,525 | -6,15 | -0,0133 |
| C · 02 − 00 (%) | -0,8 | -16 | -10,2439 | -60 | -7,0588 |

## Correspondências com o monólito histórico A

Cada célula numérica usa CBO / WMC / RFC / LCOM / LCOM*. A correspondência combina nome, pacote e responsabilidade. Para OwnerController, compara-se a responsabilidade MVC histórica com a contraparte MVC no serviço. A coluna 00 usa o controlador unificado. OwnerRepository corresponde funcionalmente ao repositório de cadastro do serviço. As duas representações atuais de Owner aparecem em linhas distintas.

| Tipo em A | Contraparte em 02 | A | 00 | 01 | 02 |
| --- | --- | --- | --- | --- | --- |
| `PetClinicApplication` | `org.springframework.samples.petclinic.PetClinicApplication` | 2 / 1 / 1 / 0 / 0 | 2 / 1 / 1 / 0 / 0 | 2 / 1 / 1 / 0 / 0 | 2 / 1 / 1 / 0 / 0 |
| `model.BaseEntity` | `org.springframework.samples.petclinic.model.BaseEntity` | 3 / 4 / 0 / 0 / 0 | 3 / 4 / 0 / 0 / 0 | 3 / 4 / 0 / 0 / 0 | 3 / 4 / 0 / 0 / 0 |
| `model.NamedEntity` | `org.springframework.samples.petclinic.model.NamedEntity` | 4 / 3 / 1 / 1 / 0,3333 | 4 / 3 / 1 / 1 / 0,3333 | 4 / 3 / 1 / 1 / 0,3333 | 4 / 3 / 1 / 1 / 0,3333 |
| `model.Person` | `org.springframework.samples.petclinic.model.Person` | 4 / 4 / 0 / 2 / 0,5 | 4 / 4 / 0 / 2 / 0,5 | 4 / 4 / 0 / 2 / 0,5 | 4 / 4 / 0 / 2 / 0,5 |
| `owner.Owner` | `org.springframework.samples.petclinic.owner.Owner` | 13 / 19 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 |
| `owner.Owner` (cadastro persistido) | `br.edu.tcc.owners.Owner` | 13 / 19 / 15 / 58 / 0,7885 | 9 / 12 / 0 / 36 / 0,7778 | 9 / 12 / 0 / 36 / 0,7778 | 9 / 12 / 0 / 36 / 0,7778 |
| `owner.OwnerController` | `br.edu.tcc.owners.OwnerController` | 15 / 14 / 17 / 0 / 0,4444 | 22 / 20 / 25 / 45 / 0,7333 | 15 / 14 / 20 / 4 / 0,6444 | 15 / 14 / 20 / 4 / 0,6444 |
| `owner.OwnerRepository` | `br.edu.tcc.owners.OwnerRepository` | 5 / 3 / 0 / 3 / 0 | 5 / 1 / 0 / 0 / 0 | 5 / 1 / 0 / 0 / 0 | 5 / 1 / 0 / 0 / 0 |
| `owner.Pet` | `org.springframework.samples.petclinic.pets.Pet` | 15 / 11 / 6 / 37 / 0,8 | 15 / 12 / 6 / 45 / 0,8182 | 15 / 12 / 6 / 45 / 0,8182 | 15 / 12 / 6 / 45 / 0,8182 |
| `owner.PetController` | `org.springframework.samples.petclinic.pets.PetController` | 20 / 14 / 15 / 8 / 0,5926 | 24 / 21 / 44 / 19 / 0,6364 | 21 / 16 / 25 / 10 / 0,5926 | 21 / 16 / 25 / 10 / 0,5926 |
| `owner.PetRepository` | `org.springframework.samples.petclinic.pets.PetRepository` | 5 / 3 / 0 / 3 / 0 | 6 / 5 / 0 / 10 / 0 | 6 / 5 / 0 / 10 / 0 | 6 / 5 / 0 / 10 / 0 |
| `owner.PetType` | `org.springframework.samples.petclinic.pets.PetType` | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 |
| `owner.PetTypeFormatter` | `org.springframework.samples.petclinic.pets.PetTypeFormatter` | 6 / 5 / 3 / 1 / 0,3333 | 6 / 5 / 3 / 1 / 0,3333 | 6 / 5 / 3 / 1 / 0,3333 | 6 / 5 / 3 / 1 / 0,3333 |
| `owner.PetValidator` | `org.springframework.samples.petclinic.pets.PetValidator` | 5 / 6 / 7 / 1 / 0,5 | 5 / 6 / 7 / 1 / 0,5 | 5 / 6 / 7 / 1 / 0,5 | 5 / 6 / 7 / 1 / 0,5 |
| `owner.VisitController` | `org.springframework.samples.petclinic.visit.VisitController` | 14 / 6 / 6 / 6 / 0,6 | 17 / 8 / 12 / 6 / 0,6 | 17 / 8 / 12 / 6 / 0,6 | 17 / 8 / 12 / 6 / 0,6 |
| `system.CacheConfig` | `org.springframework.samples.petclinic.system.CacheConfig` | 7 / 2 / 3 / 1 / 0 | 7 / 2 / 3 / 1 / 0 | 7 / 2 / 3 / 1 / 0 | 7 / 2 / 3 / 1 / 0 |
| `system.CrashController` | `org.springframework.samples.petclinic.system.CrashController` | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 |
| `system.WelcomeController` | `org.springframework.samples.petclinic.system.WelcomeController` | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 |
| `vet.Specialty` | `org.springframework.samples.petclinic.vet.Specialty` | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 |
| `vet.Vet` | `org.springframework.samples.petclinic.vet.Vet` | 10 / 6 / 5 / 8 / 0,6 | 10 / 6 / 5 / 8 / 0,6 | 10 / 6 / 5 / 8 / 0,6 | 10 / 6 / 5 / 8 / 0,6 |
| `vet.VetController` | `org.springframework.samples.petclinic.vet.VetController` | 6 / 3 / 4 / 0 / 0 | 6 / 3 / 4 / 0 / 0 | 6 / 3 / 4 / 0 / 0 | 6 / 3 / 4 / 0 / 0 |
| `vet.VetRepository` | `org.springframework.samples.petclinic.vet.VetRepository` | 4 / 1 / 0 / 0 / 0 | 4 / 1 / 0 / 0 / 0 | 4 / 1 / 0 / 0 / 0 | 4 / 1 / 0 / 0 / 0 |
| `vet.Vets` | `org.springframework.samples.petclinic.vet.Vets` | 3 / 2 / 0 / 0 / 0 | 3 / 2 / 0 / 0 / 0 | 3 / 2 / 0 / 0 / 0 | 3 / 2 / 0 / 0 / 0 |
| `visit.Visit` | `org.springframework.samples.petclinic.visit.Visit` | 7 / 7 / 0 / 11 / 0,6667 | 7 / 7 / 0 / 11 / 0,6667 | 7 / 7 / 0 / 11 / 0,6667 | 7 / 7 / 0 / 11 / 0,6667 |
| `visit.VisitRepository` | `org.springframework.samples.petclinic.visit.VisitRepository` | 2 / 2 / 0 / 1 / 0 | 2 / 2 / 0 / 1 / 0 | 2 / 2 / 0 / 1 / 0 | 2 / 2 / 0 / 1 / 0 |

## Controladores REST e componente compartilhado

| Tipo | 01 · CBO / WMC / RFC / LCOM / LCOM* | 02 · CBO / WMC / RFC / LCOM / LCOM* |
| --- | --- | --- |
| `org.springframework.samples.petclinic.integration.PetRestController` | 7 / 5 / 20 / 1 / 0,3333 | 7 / 5 / 20 / 1 / 0,3333 |
| `br.edu.tcc.owners.integration.OwnerRestController` | 15 / 6 / 8 / 0 / 0,3333 | 15 / 6 / 8 / 0 / 0,3333 |
| `br.edu.tcc.owners.OwnerLookup` | 4 / 2 / 2 / 0 / 0 | 4 / 2 / 2 / 0 / 0 |
| `org.springframework.samples.petclinic.integration.PetRestController$PetSummary` | 5 / 1 / 10 / 0 / 0 | 5 / 1 / 10 / 0 / 0 |
| `org.springframework.samples.petclinic.integration.PetRestController$VisitSummary` | 2 / 1 / 3 / 0 / 0 | 2 / 1 / 3 / 0 / 0 |

### Conjuntos funcionais MVC e REST

As células exibem WMC / RFC. A soma de RFC pode contar a mesma invocação em classes diferentes. Os valores do conjunto descrevem a distribuição do código entre seus controladores.

| Domínio | Estado | MVC ou unificado | REST | Soma MVC e REST |
| --- | --- | ---: | ---: | ---: |
| Pet | 00 | 21 / 44 | — | — |
| Pet | 01 | 16 / 25 | 5 / 20 | 21 / 45 |
| Pet | 02 | 16 / 25 | 5 / 20 | 21 / 45 |
| Owner | 00 | 20 / 25 | — | — |
| Owner | 01 | 14 / 20 | 6 / 8 | 20 / 28 |
| Owner | 02 | 14 / 20 | 6 / 8 | 20 / 28 |

## Efeito observado de cada etapa

Em 00 → 01, B ganhou `PetRestController`. Os dois DTOs internos passaram do controlador MVC para o REST e continuaram como tipos separados. C ganhou `OwnerRestController` e `OwnerLookup`. Os dois controladores MVC concentraram menos WMC e RFC individualmente, e as somas dos pares de controladores e os agregados por aplicação mudaram. Em Owner, o componente de busca compartilhada também contribui com WMC 2 e RFC 2 ao total da aplicação.

Em 01 → 02, sete tipos de produção mudaram de pacote e permaneceram na população medida. O serviço reproduziu os dois CSVs byte a byte.

Todos os tipos de B preservaram as cinco métricas após aplicar o mapeamento de pacotes. A CK registrou a mudança de visibilidade de `Pet.setOwner` em suas colunas auxiliares.

Os indicadores descrevem estrutura estática. Os testes medem a preservação do comportamento nas condições executadas.

## Validação

| Estado | Monólito aprovados / ignorados | Owners Service aprovados | Gateway aprovados | HTTP aprovadas |
| --- | ---: | ---: | ---: | ---: |
| 00 · Histórico | 51 / 1 | 12 | 1 | 79 |
| 01 | 51 / 1 | 12 | 1 | 79 |
| 02 | 51 / 1 | 12 | 1 | 79 |

As duas etapas executaram Maven offline com Java 8 no monólito e Java 17 no Owners Service e Gateway, com zero falhas e erros. `CrashControllerTests` permaneceu ignorado. A aceitação executou três JVMs locais com bancos descartáveis e mensagens de validação em inglês. Cobre os formulários, operações REST, comunicação entre serviços, reinício, indisponibilidade e bloqueio de rotas internas. O modo Docker Compose ficou fora dessas execuções. [Validação 01](01-controllers-separados/validacao.json) · [Validação 02](02-pacotes-reorganizados/validacao.json).

## Integração futura

No worktree da V3, confirme branch, estado limpo e HEAD em `tcc2-v3-pre-separacao-mvc-rest`. Se o HEAD continuar nesse ancestral, um avanço rápido conserva os commits testados. Se a V3 tiver avançado, reavalie a integração antes de aplicá-la.

Para integrar só a etapa 01, execute `git merge --ff-only tcc2-exp-controllers-separados`. A coleta da etapa 01 fica no histórico do experimento. Para integrar ambas, execute `git merge --ff-only tcc2-exp-mvc-rest-modulos`. Nesse caso, a coleta de 02 passa a constar em `docs/metricas/`.
