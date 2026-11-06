# TCC2 · Validação e métricas da Strangler Fig V2 consolidada

## Implementação avaliada

A coleta usa os fontes do commit `1e8fa995390adeded74d965478b75240821ce098`, cujo pai é o checkpoint `0f122b9d1a7bd165cd2cf101f417fdeee11d7ee0`. O monólito Java 8 persiste Pets e Visits em HSQLDB. O Owners Service Java 17 persiste Owners em H2. O Gateway separa a API de Owners das rotas do monólito. `OwnerRepository` é o contrato dos controllers e `OwnerIntegrationAdapter` o implementa por HTTP. A consulta remota compõe o Owner com Pets locais e sincroniza `Pet.ownerId`.

As restrições de binding restauradas aceitam `name`, `birthDate` e `type` para Pet e `date` e `description` para Visit. A edição de Pet usa o ID da URL após validar o vínculo. As duas restrições corrigem inconsistências de binding e preservam a lógica funcional do legado. A sobrescrita de Pet foi reproduzida no monólito original. No caso de Visit, o original bloqueava apenas `id` e expunha o setter de `petId`. A gravação no Pet errado foi reproduzida por HTTP na V2 quando esse binder foi restaurado. As restrições protegem a integridade da gravação independentemente da comunicação Strangler Fig.

## Validação funcional

| Componente | Java | Maven | Aprovados | Ignorados | Falhas | Erros |
| --- | --- | --- | --- | --- | --- | --- |
| monolito-v2 | 8u504 | 3.3.3 | 60 | 1 | 0 | 0 |
| owners-service | 17.0.12 | 3.9.11 | 6 | 0 | 0 | 0 |
| gateway | 17.0.12 | 3.9.11 | 1 | 0 | 0 | 0 |

O total repetiu o checkpoint, com 67 aprovados e um ignorado. Os builds usaram o cache Maven local em modo offline. [Resumo dos builds](evidencias/builds.json)

O roteiro integral de `scripts/acceptance.py` manteve suas 57 verificações e recebeu oito requisições adicionais em um executor temporário. As JVMs rodaram com os JARs compilados, loopback e bancos descartáveis. O Gateway respondeu em 8081, o monólito em 8080 e o Owners Service em 8082. Foram verificados consulta de Owners por ID e sobrenome, cadastro e edição de Owners e Pets, composição, criação e consulta de Visits, persistência após reinício, respostas 400, 404, 409 e 503 e indisponibilidade e recuperação do Owners Service. [65 verificações HTTP](evidencias/http-acceptance.json)

A auditoria JDBC foi executada após o reinício, em uma cópia da base HSQLDB da aceitação.

| Requisição | Resultado persistido |
| --- | --- |
| Criar `ForgedPetId` no Owner 11 com `id=1`, tipo `cat` e data `2020-01-01` | Novo Pet no Owner 11. O Pet 1, Leo, manteve seus dados e o Owner 1 |
| Criar `V2PetEdited` no Owner 11 com `id=1`, repetindo um nome existente | Formulário reapresentado. Uma única linha chamada `V2PetEdited` |
| Editar o Pet 14 do Owner 11 com `id=1` no corpo | A linha 14 recebeu a edição e manteve o Owner 11 |
| Criar `V2Visit` em `/owners/11/pets/14/visits/new` com `petId=1` e data `2026-10-28` | Uma única Visit, ligada ao Pet 14 |

O formulário de edição renderizou o ID oculto e o cadastro com ID oculto vazio passou. A página do Owner remoto mostrou cada Pet do cenário uma vez. A prova direta de `Owner.addPet()` associou um Pet existente e um novo e verificou que chamadas repetidas com as mesmas instâncias não duplicam entradas. [Auditoria JDBC e composição](evidencias/auditoria.json)

O roteiro `scripts/test-migration.py` migrou 11 Owners, 14 Pets e 5 Visits. A auditoria comparou todas as colunas de Owners e das seis tabelas do legado após reabrir os destinos. A prova de sequência persistiu o ID 102, o destino existente foi recusado e os SHA-256 da origem permaneceram iguais. [Migração](evidencias/migration.json) · [Auditoria após reabertura](evidencias/auditoria.json)

## Método da coleta CK

CK `0.7.1-SNAPSHOT`, Java 17 e argumentos `false 0 false`. O SHA-256 do JAR é `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`. A foi extraído por `git archive` de `c36452a2c34443ae26b4ecbba4f149906af14717` e cada cenário inclui apenas a sua raiz de produção `src/main/java`. As médias são aritméticas e incluem toda linha de tipo emitida pela CK, inclusive interfaces e tipos internos. Os valores percentuais usam A como denominador. Os hashes das entradas e saídas, os comandos, as revisões e o estado da árvore durante a coleta estão na proveniência. [Proveniência](proveniencia.json)

## A × B · Monólitos completos

| Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| A · Monólito original | 24 | 6,666667 | 4,916667 | 3,458333 | 5,875 | 0,256618 |
| B · Monólito V2 atual | 28 | 6,5 | 5,321429 | 4,785714 | 5,857143 | 0,233967 |
| Owners Service | 4 | 8 | 5 | 2,75 | 9 | 0,236111 |
| Gateway | 1 | 2 | 1 | 1 | 0 | 0 |

| Métrica | A | B | Δ B − A | Δ % sobre A |
| --- | --- | --- | --- | --- |
| CBO | 6,666667 | 6,5 | -0,166667 | -2,5% |
| WMC | 4,916667 | 5,321429 | +0,404762 | +8,23% |
| RFC | 3,458333 | 4,785714 | +1,327381 | +38,38% |
| LCOM | 5,875 | 5,857143 | -0,017857 | -0,3% |
| LCOM* | 0,256618 | 0,233967 | -0,022651 | -8,83% |

A e B têm populações diferentes. As médias descrevem os tipos de cada cenário. CBO e RFC descrevem relações reconhecidas estaticamente pela CK no código Java, enquanto a comunicação HTTP e a dependência operacional entre processos têm alcance distinto. LCOM e LCOM* dependem do estado e dos métodos declarados em cada tipo, com leitura limitada em interfaces e pontos de entrada. As médias não medem acoplamento arquitetural, disponibilidade ou latência do sistema distribuído.

## A × B · Tipos com o mesmo nome qualificado

A comparação pareada usa os mesmos 24 nomes qualificados nos dois monólitos. Tipos como `Owner`, `OwnerRepository` e `Pet` têm nomes correspondentes e papéis parcialmente diferentes após a extração.

| Métrica | A | B | Δ B − A | Δ % sobre A |
| --- | --- | --- | --- | --- |
| CBO | 6,666667 | 6,583333 | -0,083333 | -1,25% |
| WMC | 4,916667 | 5,208333 | +0,291667 | +5,93% |
| RFC | 3,458333 | 4,041667 | +0,583333 | +16,87% |
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
| owner.PetController | 20 → 23 | 14 → 17 | 15 → 22 | 8 → 17 | 0,592593 → 0,633333 |
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

A referência é a coleta diagnóstica de 6 de novembro, identificada na proveniência. A, Owners Service e Gateway repetiram todos os campos de `class.csv` e `method.csv` após desconsiderar o caminho. O monólito V2 manteve 28 tipos. [Comparação por tipo](comparacao-diagnostica.csv)

| Tipo | Métrica | Anterior | Atual | Δ |
| --- | --- | --- | --- | --- |
| owner.PetController | RFC | 21 | 22 | +1 |

O RFC de `PetController` acompanha a volta da chamada `setAllowedFields` ao binder de Pets. A troca de `setDisallowedFields` por `setAllowedFields` em `VisitController` preservou as cinco métricas desse tipo.

Comentários, formatação e a ordem de métodos também podem mudar campos de localização ou outras colunas brutas da CK. A tabela destaca CBO, WMC, RFC, LCOM e LCOM*.
