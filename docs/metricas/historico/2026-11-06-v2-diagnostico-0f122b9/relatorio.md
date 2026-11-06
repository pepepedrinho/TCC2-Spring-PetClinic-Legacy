# TCC2 · Diagnóstico de validação e métricas da Strangler Fig V2

## Implementação avaliada

A coleta diagnóstica cobre a árvore local sobre o checkpoint `0f122b9d1a7bd165cd2cf101f417fdeee11d7ee0`, com cinco fontes Java modificados. A aceitação obrigatória falhou, de modo que este material permanece em `.work/` para revisão e a coleta definitiva fica pendente. O patch e a proveniência registram o estado avaliado. [Patch dos fontes](fontes-v2.patch) O monólito Java 8 continua a persistir Pets e Visits em HSQLDB. O Owners Service Java 17 persiste o cadastro de Owners em H2. O Gateway mantém a divisão de rotas entre a API de Owners e o monólito. `OwnerRepository` é a interface consumida pelos controllers, com implementação HTTP em `OwnerIntegrationAdapter`. A consulta remota compõe o Owner com Pets locais e mantém a associação em memória, sincronizando `Pet.ownerId`.

A revisão desde o checkpoint abrange `OwnerIntegrationAdapter`, `Owner`, `OwnerRepository`, `Pet` e `PetController`. A única correção feita na validação foi remover o `throw` duplicado em `PetController.checkOwnership()`. `VisitController` foi revisado sem mudança. A composição de Visit com Pet e Owner continua presente, mas o binder herdado do checkpoint bloqueia apenas `id` e aceita `petId`. O binder de Pet foi mantido conforme a revisão manual, sem `setAllowedFields`, e a edição continua a gravar pelo `petId` da URL.

## Validação funcional

| Componente | Java | Maven | Aprovados | Ignorados | Falhas | Erros |
| --- | --- | --- | --- | --- | --- | --- |
| monolito-v2 | 8u504 | 3.3.3 | 60 | 1 | 0 | 0 |
| owners-service | 17.0.12 | 3.9.11 | 6 | 0 | 0 | 0 |
| gateway | 17.0.12 | 3.9.11 | 1 | 0 | 0 | 0 |

Os resultados repetem o checkpoint de 67 testes aprovados e um ignorado. Os builds foram executados com o cache Maven local em modo offline. [Resumo dos builds](evidencias/builds.json)

O roteiro obrigatório, com sondas adicionais de edição, parou na verificação da Visit adulterada. Um POST para o Pet 14 enviou `petId=1` e recebeu 302. O log HSQLDB registrou a Visit no Pet 1. A falha antecede o reinício previsto no roteiro. `loadPetWithVisit()` verifica a URL e preenche o vínculo, mas o binder posterior pode sobrescrevê-lo pelo setter público de `Visit.petId`. A lista permitida `date, description` em `dd6c9e5` foi substituída pela regra de `1.5.x`, que bloqueia somente `id`, antes do checkpoint `0f122b9`. O original também tinha o setter público. A falha HTTP foi reproduzida nesta etapa na V2. [Aceitação obrigatória](evidencias/aceitacao-falha.json) · [SQL persistido](evidencias/vinculo-visit.sql.txt)

O diagnóstico HTTP separado completou 65 verificações com os mesmos JARs e novas bases descartáveis. Usou descrições distintas para comprovar o vínculo incorreto da Visit adulterada e testar a Visit legítima sem `petId` no corpo. O Gateway respondeu em 8081 e os processos diretos em 8080 e 8082. Passaram os fluxos legítimos de consulta, cadastro e edição de Owners e Pets, a criação e consulta da Visit legítima, a persistência após reinício, os cenários 400, 404, 409 e 503 e a recuperação do Owners Service. Esse resultado tem escopo diagnóstico e a aceitação obrigatória continua reprovada. [Verificações HTTP do diagnóstico](evidencias/http-acceptance.json)

O formulário renderizou o ID oculto, e a edição com um ID divergente preservou o Pet da URL e o Pet do outro Owner. O cadastro com o ID oculto vazio criou um novo Pet. A página do Owner remoto mostrou cada Pet do cenário uma vez. Uma prova direta de `Owner.addPet()` confirmou que um Pet existente e um novo são associados e que a reapresentação da mesma instância não duplica a entrada. Essa lógica permite a composição de Pets existentes. O teste não mede deduplicação por ID entre instâncias distintas. [Verificações dirigidas](evidencias/diagnostico-http.txt) · [Auditoria](evidencias/auditoria.txt)

A migração preservou as 11 linhas de Owners, os IDs e todos os registros das seis tabelas que permanecem no legado, incluindo 14 Pets e 5 Visits. A auditoria comparou todas as colunas dessas linhas após reabrir os destinos. A prova de sequência persistiu o ID 102, o destino existente foi recusado e os SHA-256 dos arquivos da origem permaneceram iguais. [Migração](evidencias/migration.json) · [Auditoria após reabertura](evidencias/auditoria.txt)

O binder de criação de Pets reproduz a aceitação do campo `id` do baseline de 2017. A investigação anterior confirmou que um ID adulterado na criação permite sobrescrita e troca de Owner. Esse comportamento herdado foi preservado por decisão metodológica. A verificação de edição desta etapa tem escopo separado.

## Método da coleta CK

CK `0.7.1-SNAPSHOT`, Java 17 e argumentos `false 0 false`. O SHA-256 do JAR é `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`. A foi extraído por `git archive` de `c36452a2c34443ae26b4ecbba4f149906af14717` e cada cenário inclui apenas a sua raiz de produção `src/main/java`. As médias são aritméticas e incluem toda linha de tipo emitida pela CK, inclusive interfaces e tipos internos. Os valores percentuais usam A como denominador. Os hashes das entradas e saídas, os comandos, as revisões e o estado da árvore durante a coleta estão na proveniência. [Proveniência](proveniencia.json)

## A × B · Monólitos completos

| Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| A · Monólito original | 24 | 6,666667 | 4,916667 | 3,458333 | 5,875 | 0,256618 |
| B · Monólito V2 atual | 28 | 6,5 | 5,321429 | 4,75 | 5,857143 | 0,233967 |
| Owners Service | 4 | 8 | 5 | 2,75 | 9 | 0,236111 |
| Gateway | 1 | 2 | 1 | 1 | 0 | 0 |

| Métrica | A | B | Δ B − A | Δ % sobre A |
| --- | --- | --- | --- | --- |
| CBO | 6,666667 | 6,5 | -0,166667 | -2,5% |
| WMC | 4,916667 | 5,321429 | +0,404762 | +8,23% |
| RFC | 3,458333 | 4,75 | +1,291667 | +37,35% |
| LCOM | 5,875 | 5,857143 | -0,017857 | -0,3% |
| LCOM* | 0,256618 | 0,233967 | -0,022651 | -8,83% |

A e B têm populações diferentes. As médias descrevem os tipos de cada cenário. CBO e RFC descrevem relações reconhecidas estaticamente pela CK no código Java, enquanto a comunicação HTTP e a dependência operacional entre processos têm alcance distinto. LCOM e LCOM* dependem do estado e dos métodos declarados em cada tipo, com leitura limitada em interfaces e pontos de entrada. As médias não medem acoplamento arquitetural, disponibilidade ou latência do sistema distribuído.

## A × B · Tipos com o mesmo nome qualificado

A comparação pareada usa os mesmos 24 nomes qualificados nos dois monólitos. Tipos como `Owner`, `OwnerRepository` e `Pet` têm nomes correspondentes e papéis parcialmente diferentes após a extração.

| Métrica | A | B | Δ B − A | Δ % sobre A |
| --- | --- | --- | --- | --- |
| CBO | 6,666667 | 6,583333 | -0,083333 | -1,25% |
| WMC | 4,916667 | 5,208333 | +0,291667 | +5,93% |
| RFC | 3,458333 | 4 | +0,541667 | +15,66% |
| LCOM | 5,875 | 6,708333 | +0,833333 | +14,18% |
| LCOM* | 0,256618 | 0,259073 | +0,002455 | +0,96% |

Cada célula apresenta A → B.

| Tipo | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- |
| PetClinicApplication | 2 → 2 | 1 → 1 | 1 → 1 | 0 → 0 | 0 → 0 |
| model.BaseEntity | 3 → 3 | 4 → 4 | 0 → 0 | 0 → 0 | 0 → 0 |
| model.NamedEntity | 4 → 4 | 3 → 3 | 1 → 1 | 1 → 1 | 0,333333 → 0,333333 |
| model.Person | 4 → 4 | 4 → 4 | 0 → 0 | 2 → 2 | 0,5 → 0,5 |
| owner.Owner | 13 → 9 | 19 → 19 | 15 → 16 | 58 → 58 | 0,788462 → 0,788462 |
| owner.OwnerController | 15 → 15 | 14 → 14 | 17 → 17 | 0 → 0 | 0,444444 → 0,444444 |
| owner.OwnerRepository | 5 → 1 | 3 → 3 | 0 → 0 | 3 → 3 | 0 → 0 |
| owner.Pet | 15 → 15 | 11 → 12 | 6 → 6 | 37 → 45 | 0,8 → 0,818182 |
| owner.PetController | 20 → 23 | 14 → 17 | 15 → 21 | 8 → 17 | 0,592593 → 0,633333 |
| owner.PetRepository | 5 → 5 | 3 → 4 | 0 → 0 | 3 → 6 | 0 → 0 |
| owner.PetType | 3 → 3 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| owner.PetTypeFormatter | 6 → 6 | 5 → 5 | 3 → 3 | 1 → 1 | 0,333333 → 0,333333 |
| owner.PetValidator | 5 → 5 | 6 → 6 | 7 → 7 | 1 → 1 | 0,5 → 0,5 |
| owner.VisitController | 14 → 17 | 6 → 8 | 6 → 12 | 6 → 6 | 0,6 → 0,6 |
| system.CacheConfig | 7 → 7 | 2 → 2 | 3 → 3 | 1 → 1 | 0 → 0 |
| system.CrashController | 2 → 2 | 1 → 1 | 0 → 0 | 0 → 0 | 0 → 0 |
| system.WelcomeController | 2 → 2 | 1 → 1 | 0 → 0 | 0 → 0 | 0 → 0 |
| vet.Specialty | 3 → 3 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| vet.Vet | 10 → 10 | 6 → 6 | 5 → 5 | 8 → 8 | 0,6 → 0,6 |
| vet.VetController | 6 → 6 | 3 → 3 | 4 → 4 | 0 → 0 | 0 → 0 |
| vet.VetRepository | 4 → 4 | 1 → 1 | 0 → 0 | 0 → 0 | 0 → 0 |
| vet.Vets | 3 → 3 | 2 → 2 | 0 → 0 | 0 → 0 | 0 → 0 |
| visit.Visit | 7 → 7 | 7 → 7 | 0 → 0 | 11 → 11 | 0,666667 → 0,666667 |
| visit.VisitRepository | 2 → 2 | 2 → 2 | 0 → 0 | 1 → 1 | 0 → 0 |

## Tipos de integração introduzidos

| Tipo | Categoria CK | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| integration.IntegrationErrorHandler | class | 7 | 8 | 5 | 3 | 0 |
| integration.IntegrationErrorHandler$IntegrationFailure | innerclass | 1 | 2 | 1 | 0 | 0 |
| integration.OwnerIntegrationAdapter | class | 11 | 11 | 24 | 0 | 0,333333 |
| system.LegacyDatabaseInitializer | class | 5 | 3 | 7 | 0 | 0 |

`OwnerIntegrationAdapter` reúne HTTP e composição. `IntegrationErrorHandler` e seu tipo interno classificam falhas e integram as respostas MVC. `LegacyDatabaseInitializer` prepara o banco legado e fica fora da fronteira HTTP. `OwnerRepository` aparece na comparação pareada como contrato do adaptador.

## Owners Service e Gateway

| Tipo | Categoria CK | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| br.edu.tcc.owners.Owner | class | 9 | 12 | 0 | 36 | 0,777778 |
| br.edu.tcc.owners.OwnerController | class | 16 | 6 | 10 | 0 | 0,166667 |
| br.edu.tcc.owners.OwnerRepository | interface | 5 | 1 | 0 | 0 | 0 |
| br.edu.tcc.owners.OwnersApplication | class | 2 | 1 | 1 | 0 | 0 |
| br.edu.tcc.gateway.GatewayApplication | class | 2 | 1 | 1 | 0 | 0 |

O domínio extraído é o cadastro de `br.edu.tcc.owners.Owner`. Seu repositório e controller dão acesso persistente e HTTP, e `OwnersApplication` inicia o serviço. O Gateway tem um tipo de produção no CSV. A configuração de rotas em propriedades fica fora da métrica de classe. As médias dos dois módulos aparecem no quadro de componentes.

## Comparação com a coleta anterior

A referência anterior do monólito é a coleta da refatoração do adaptador. Para A, Owners Service e Gateway, os CSVs arquivados da revalidação de 5 de novembro correspondem à coleta de controle após a reorganização de diretórios. A, Owners Service e Gateway repetiram todos os campos de `class.csv` e `method.csv` após desconsiderar o caminho. O monólito V2 manteve 28 tipos.

| Tipo | Métrica | Anterior | Atual | Δ |
| --- | --- | --- | --- | --- |
| owner.PetController | RFC | 22 | 21 | -1 |

O RFC de `PetController` acompanha a retirada da chamada `setAllowedFields` do binder de Pets. A validação e a atribuição de `petId` na edição continuam presentes.

Comentários, formatação e a ordem de métodos também podem mudar campos de localização ou outras colunas brutas da CK. A tabela destaca CBO, WMC, RFC, LCOM e LCOM*.
