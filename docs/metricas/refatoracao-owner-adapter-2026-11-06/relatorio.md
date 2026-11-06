# Refatoração de OwnerRepository no Strangler Fig V2

## Alterações

- `OwnerRepository` voltou a ser uma interface Java com `findByLastName`, `findById` e `save`. Os controllers continuam a receber essa interface por injeção. A interface tem um único bean de implementação, `OwnerIntegrationAdapter`, registrado com `@Service`.
- `OwnerIntegrationAdapter` incorporou o `OwnersClient` e a composição de Owners com Pets locais. O adaptador mantém `RestTemplate`, `owners.base-url`, `/api/owners` e os timeouts de 1500 ms para conexão e 3000 ms para leitura. As consultas associam Pets pelo ID do Owner. O POST atualiza o ID do objeto recebido e o PUT envia os campos de cadastro para o ID existente. O `IntegrationErrorHandler` continua a classificar as falhas remotas.
- `OwnersClient.java` foi removido. `OwnersClientTests` passou a `OwnerIntegrationAdapterTests`. `IntegrationErrorHandlerTests` passou a exercitar o adaptador e `ClinicServiceTests` usa o adaptador com HTTP simulado e repositório local real. Owners continuam no microsserviço e Pets e Visits continuam no monólito. O README de entrada foi atualizado para descrever o contrato e seu adaptador.

Os três blocos históricos da branch `1.5.x` foram preservados. Os comentários da V2 no contrato apontam para o adaptador, e o comentário auxiliar de composição acompanha o método. A remoção manual em `OwnersClient` foi respeitada na migração. Os hashes dos quatro fontes locais do Owners Service e dos 45 arquivos da coleta anterior conferem com os registrados antes da edição. A branch permaneceu em `tcc2-strangler-fig-v2`, no HEAD `dd6c9e5cb9f0ef9c2b5eb9fcc272163fc3e68667`. Nenhuma alteração foi preparada para commit.

## Validação

| Componente | JDK | Maven | Build | Aprovados | Ignorados | Falhas | Erros |
|---|---|---|---|---:|---:|---:|---:|
| Monólito | Temurin 8u504 | Wrapper 3.3.3 | Sucesso | 60 | 1 | 0 | 0 |
| Owners Service | Oracle 17.0.12 | 3.9.11 | Sucesso | 6 | 0 | 0 | 0 |
| Gateway | Oracle 17.0.12 | 3.9.11 | Sucesso | 1 | 0 | 0 | 0 |

Os builds usaram `-o -B clean package` em uma cópia isolada dos fontes atuais, incluindo as edições locais. Para o monólito, foi invocado diretamente o `MavenWrapperMain`, com as flags históricas `-Dcobertura.skip=true` e `-Dmaven.gitcommitid.skip=true`. A primeira chamada precisou ser refeita com as propriedades `-D` entre aspas para o PowerShell. O build do monólito emitiu um erro do utilitário MSYS `echo` durante a execução, mas concluiu com sucesso, gerou o JAR e executou a suíte. O teste ignorado foi `CrashControllerTests`, conforme a configuração já existente.

As primeiras tentativas do Gateway e do Owners Service no sandbox encontraram restrições de loopback do Netty e do pipe local de anexação do agente Mockito, respectivamente. A primeira verificação de migração encontrou o H2 em modo somente leitura. As novas execuções passaram com acesso local liberado, usando o mesmo código e os mesmos testes.

A aceitação HTTP do repositório passou com 57 verificações. O roteiro complementar preservado no estudo de centralização de erros passou com 57. Ambos usaram os JARs recém-compilados, o Gateway e bancos HSQLDB e H2 descartáveis. Exercitaram consulta por ID e por sobrenome, criação e edição de Owner, composição e edição de Pets, inclusão de Visit e persistência após reinício. As verificações incluíram Owner inexistente (404), dados inválidos (400), exclusão não suportada (409) e indisponibilidade do Owners Service (503). O roteiro complementar também verificou o HTML da página de indisponibilidade e o 400 propagado pelo formulário do monólito. Os testes MVC verificaram os motivos de 400 e 404, a view `ownersUnavailable` para 503 e falhas de conversão, timeout e conexão.

A fixture de migração copiou 11 Owners, 14 Pets e 5 Visits. O teste confirmou IDs, vínculos, sequência do novo cadastro, ausência da tabela local de Owners no destino, recusa de destino existente e integridade da origem por SHA-256.

Evidências locais da validação em [.work/refatoracao-owner-adapter-2026-11-06](../../../.work/refatoracao-owner-adapter-2026-11-06/), com logs de build e JSONs da aceitação. O JSON da migração está sob `build/docs/metricas/v2/evidencias/migration.json` nessa pasta.

## Coleta CK

A coleta C examinou somente `src/main/java`. Foi executada com Java 17 e o mesmo JAR `ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar` das coletas anteriores. O SHA-256 conferido antes da execução foi `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`. Os parâmetros históricos foram `false 0 false`. Os resultados brutos estão em [class.csv](class.csv) e [method.csv](method.csv), e o comando está no [log da coleta](coleta.txt).

A corresponde ao monólito original em `c36452a2c34443ae26b4ecbba4f149906af14717`. B corresponde ao monólito adaptado em `dd6c9e5` com as edições locais anteriores a esta refatoração. Os SHA-256 dos 28 fontes Java antes da mudança conferem com os registrados para B. Os CSVs de A e B conferem com os hashes históricos. [Proveniência anterior](../revalidacao-2026-11-05-dd6c9e5/proveniencia.json) · [A](../revalidacao-2026-11-05-dd6c9e5/A-monolito-original/class.csv) · [B](../revalidacao-2026-11-05-dd6c9e5/B-monolito-adaptado/class.csv)

### Monólito completo

As médias usam todas as linhas de tipo do `class.csv` de cada cenário, incluindo interfaces e tipos internos.

| Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| A · original | 24 | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B · adaptado antes | 28 | 6,5714 | 5,3571 | 4,9286 | 5,75 | 0,2363 |
| C · adaptado depois | 28 | 6,5 | 5,3214 | 4,7857 | 5,8571 | 0,234 |

### Contrato e componentes de integração

| Cenário | Tipo | Categoria CK | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A · original | `OwnerRepository` | interface | 5 | 3 | 0 | 3 | 0 |
| B · adaptado antes | `OwnerRepository` | class | 6 | 7 | 8 | 0 | 0,4 |
| C · adaptado depois | `OwnerRepository` | interface | 1 | 3 | 0 | 3 | 0 |
| B · adaptado antes | `OwnersClient` | class | 8 | 8 | 20 | 0 | 0 |
| C · adaptado depois | `OwnerIntegrationAdapter` | class | 11 | 11 | 24 | 0 | 0,3333 |
| B · adaptado antes | `IntegrationErrorHandler` | class | 7 | 8 | 5 | 3 | 0 |
| C · adaptado depois | `IntegrationErrorHandler` | class | 7 | 8 | 5 | 3 | 0 |
| B · adaptado antes | `IntegrationErrorHandler$IntegrationFailure` | innerclass | 1 | 2 | 1 | 0 | 0 |
| C · adaptado depois | `IntegrationErrorHandler$IntegrationFailure` | innerclass | 1 | 2 | 1 | 0 | 0 |

A CK emitiu linhas separadas para `OwnerRepository` e `OwnerIntegrationAdapter` em C. O total ficou em 28 tipos, pois a interface ocupa o lugar da classe de mesmo nome e o adaptador ocupa o lugar do cliente removido. O RFC de `OwnerRepository` passou de 8 para 0. O adaptador tem RFC 24 e incorpora a chamada HTTP, a composição com Pets e o preenchimento do ID remoto. A queda do RFC da interface decorre da transferência de comportamento para o adaptador. RFC por tipo registra chamadas reconhecidas pela CK e sua soma não representa chamadas distintas do sistema. O limite HTTP e a dependência em tempo de execução do Owners Service continuam presentes.

As medidas de coesão devem ser lidas junto com as responsabilidades e os campos de cada tipo. O LCOM da interface reflete o cálculo da CK para três métodos sem estado. A comparação com A é histórica, pois seu contrato herdava de Spring Data e operava sobre a persistência local.

### Tipos preservados

Fora da fronteira de integração e de `OwnerRepository`, os 24 tipos remanescentes de B para C tiveram valores idênticos nas cinco métricas, tipo a tipo. Isso inclui controllers, entidades e repositórios de Pets e Visits. Os dois tipos do handler também conservaram os cinco valores. A tabela separa os tipos preservados em B e C do subconjunto com mesmo nome nos três cenários, que permite comparar a referência original com a mesma seleção de tipos.

| Conjunto | Cenário | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Preservados em B e C | B · adaptado antes | 24 | 6,75 | 5,2083 | 4,3333 | 6,5833 | 0,2591 |
| Preservados em B e C | C · adaptado depois | 24 | 6,75 | 5,2083 | 4,3333 | 6,5833 | 0,2591 |
| Presentes em A, B e C | A · original | 23 | 6,7391 | 5 | 3,6087 | 6 | 0,2678 |
| Presentes em A, B e C | B · adaptado antes | 23 | 6,8261 | 5,3043 | 4,2174 | 6,8696 | 0,2703 |
| Presentes em A, B e C | C · adaptado depois | 23 | 6,8261 | 5,3043 | 4,2174 | 6,8696 | 0,2703 |

### Integridade dos resultados

- `class.csv` SHA-256 `1728bfb36a97ddb96449708f744f7f7206ac37d390f2df7b9d8908ec03839896`
- `method.csv` SHA-256 `e251fc23b9410ae9d622fd190fbc6427f0739a23c771974219b3370d2491adbc`
