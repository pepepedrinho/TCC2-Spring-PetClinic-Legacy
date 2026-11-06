# TCC2 · Revalidação do Strangler Fig V2 — 05/11/2026

Validação da branch `tcc2-strangler-fig-v2` em 5 de novembro de 2026, horário de São Paulo (UTC−03:00). O HEAD foi `dd6c9e5cb9f0ef9c2b5eb9fcc272163fc3e68667` e coincidia com a referência local `origin/tcc2-strangler-fig-v2`. Os builds, os testes e a CK em B, C e D usaram os arquivos da árvore de trabalho, incluindo cinco alterações locais fora do índice. O cenário A foi extraído do commit `c36452a2c34443ae26b4ecbba4f149906af14717`, comum à branch local `1.5.x` e à tag `tcc2-baseline-2017`.

## Estado inicial e isolamento

```text
 M owners-service/src/main/java/br/edu/tcc/owners/Owner.java
 M owners-service/src/main/java/br/edu/tcc/owners/OwnerController.java
 M owners-service/src/main/java/br/edu/tcc/owners/OwnerRepository.java
 M owners-service/src/main/java/br/edu/tcc/owners/OwnersApplication.java
 M src/main/java/org/springframework/samples/petclinic/integration/OwnersClient.java
```

A execução usou cópias isoladas sob `.work/revalidacao-2026-11-05/`. Os cinco arquivos, os demais insumos e as evidências históricas foram preservados. Os hashes dos insumos e dos Java analisados estão na proveniência. O ZIP contém os bytes de todos os fontes Java enviados à CK, separados por cenário. [Proveniência](proveniencia.json) · [Fontes Java](fontes-java-analisadas.zip)

## Ambiente e configuração

Windows 11 x64, Python 3.14.3 e cache Maven local em modo offline. O processo de cada build recebeu seu próprio `JAVA_HOME` e prefixo de `PATH`. O roteiro HTTP escolheu os executáveis Java 8 e 17 ao iniciar as JVMs. A CK rodou no mesmo Java 17 do Owners e Gateway.

| Componente atual | Framework e Java | Configuração |
| --- | --- | --- |
| Monólito Spring PetClinic 1.5.1 | Spring Boot 1.5.4.RELEASE · Temurin 1.8.0_504-b01 | MVC, Pets, Visits e Vets na porta 8080. Perfil production, HSQLDB em arquivo com inicialização própria, JDBC configurável por `LEGACY_DB_URL`, perfil MySQL opcional. Owners via `OWNERS_URL`, padrão `http://localhost:8082`. |
| Owners Service 1.0.0-SNAPSHOT | Spring Boot 3.5.16 · Oracle JDK 17.0.12+8-LTS-286 | API REST de cadastro `/api/owners` na porta 8082. JPA e H2 2.3.232 em arquivo, URL via `OWNERS_DB_URL` e inicialização por `schema.sql`. |
| Gateway 1.0.0-SNAPSHOT | Spring Boot 3.5.16 · Spring Cloud 2025.0.3 · Oracle JDK 17.0.12+8-LTS-286 | WebFlux na porta 8081. Encaminha `/api/owners` e subrotas a 8082 e as demais requisições a 8080. Preserva o cabeçalho Host. |

## Builds e testes

| Componente | Java | Maven | Build | Tests run | Executados | Aprovados | Ignorados | Falhas | Erros |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Monólito legado | Temurin 1.8.0_504 | 3.3.3 · Wrapper | [Sucesso](evidencias/build-legado-wrapper-final.log) | 61 | 60 | 60 | 1 | 0 | 0 |
| Owners Service | Oracle 17.0.12 | 3.9.11 | [Sucesso](evidencias/build-owners-final.log) | 6 | 6 | 6 | 0 | 0 | 0 |
| Gateway | Oracle 17.0.12 | 3.9.11 | [Sucesso](evidencias/build-gateway-final.log) | 1 | 1 | 1 | 0 | 0 | 0 |
| **Total** | — | — | **3 de 3** | 68 | 67 | 67 | 1 | 0 | 0 |

`Tests run` é o total registrado pelo Surefire e inclui o teste ignorado. `CrashControllerTests` está anotado com `@Ignore`. A suíte do legado inclui MVC, persistência HSQLDB em memória, contexto de produção e tratamento de falhas de integração. Owners executou testes de API com Spring Boot, MockMvc e H2 em memória. Gateway verificou a configuração de rotas em contexto Spring Boot com servidor em porta aleatória.

O Wrapper do legado fixa Maven 3.3.3. `mvnw.cmd` falhou com «Não foi possível localizar nem carregar a classe principal de» no caminho com espaços. A invocação direta de `MavenWrapperMain` usou a mesma distribuição do Wrapper e passou na cópia final. Os dois módulos independentes carecem de wrappers próprios e usaram Maven 3.9.11, conforme `scripts/build.ps1`.

Na primeira rodada, o Gateway falhou em `Permission denied: connect` ao abrir loopback, e o H2 da fixture ficou somente leitura na verificação de migração. O Maven 3.9.11 do legado e a primeira tentativa direta do Wrapper chegaram a compartilhar o `target`, o que invalidou os resultados dessa tentativa. O build definitivo foi executado em uma nova cópia, com um único build por diretório e com acesso a loopback. O legado emitiu aviso do Wro4j sobre o mapa de fontes CSS. O Owners emitiu o aviso de `spring.jpa.open-in-view` e logs de validação esperados no teste de entrada inválida. A flag de Cobertura segue o roteiro histórico e a validação não mediu cobertura.

### Integração HTTP e migração

| Verificação | Resultado | Escopo | Evidência |
| --- | --- | --- | --- |
| Aceitação HTTP atual | Aprovada | 57 verificações HTTP | [JSON](evidencias/http-acceptance.json) · [log](evidencias/http.log) |
| Aceitação HTTP de 03/11 + sonda | Aprovada | 59 verificações do roteiro e 4 consultas de observação | [JSON](evidencias/http-centralizacao.json) · [log](evidencias/http-centralizacao.log) |
| Migração com fixture histórica | Aprovada | IDs, vínculos, sequência, proteção do destino e hash da origem | [JSON](evidencias/migracao.json) · [log](evidencias/migracao-final.log) |

Na base de teste do roteiro complementar, prefixo vazio, `%` e `_` retornaram os mesmos 12 cadastros. `zzzz` retornou zero. Os resultados mostram o uso de `%` e `_` como curingas da consulta `LIKE` atual. O roteiro histórico não estabelece um contrato para esses dois caracteres. [IDs retornados](evidencias/consulta-prefixo.json)

A migração usou SQL da baseline, HSQLDB 2.3.5 e H2 2.3.232. Validou a cópia de 11 owners, 14 pets e 5 visits, preservação de IDs e vínculos, ausência da tabela local de Owners no destino, avanço da sequência, recusa de destino preexistente e SHA-256 inalterado dos arquivos de origem. Os testes HTTP usaram bancos descartáveis e encerraram as JVMs que iniciaram. Os roteiros reaproveitaram os arquivos existentes e executaram o reinício abrupto previsto para verificar persistência.

## Comandos e evidências de execução

Comandos principais abaixo. Cada log registra o comando expandido, o diretório de execução, o `JAVA_HOME` e o código de saída. A cópia de trabalho usada nos comandos de build definitivos foi `.work/revalidacao-2026-11-05/execucao-final/`.

```powershell
# Inspeção de referências, estado e diferenças
git status --short --branch
git status --porcelain=v1 --untracked-files=all
git rev-parse HEAD 'refs/tags/tcc2-baseline-2017^{}' refs/heads/1.5.x
git diff --name-status c36452a2 -- src/main/java
git archive --format=zip "--output=$work/baseline.zip" c36452a2c34443ae26b4ecbba4f149906af14717 src/main/java

# Legado, a partir da raiz da cópia e com JAVA_HOME apontando para Java 8
cmd.exe /d /c call mvnw.cmd -v
cmd.exe /d /c call mvnw.cmd -o -B clean package -Dcobertura.skip=true
& "$env:JAVA_HOME/bin/java.exe" -classpath "$PWD/.mvn/wrapper/maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain -v
& "$env:JAVA_HOME/bin/java.exe" -classpath "$PWD/.mvn/wrapper/maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain -o -B clean package -Dcobertura.skip=true -Dmaven.gitcommitid.skip=true

# Em owners-service/ e gateway/ da cópia, com JAVA_HOME apontando para Java 17
cmd.exe /d /c call C:\TCC2\ferramentas\apache-maven-3.9.11\bin\mvn.cmd -v
cmd.exe /d /c call C:\TCC2\ferramentas\apache-maven-3.9.11\bin\mvn.cmd -o -B clean package

# Integração a partir da raiz da cópia final
python scripts/acceptance.py
python scripts/migration-isolada.py
python scripts/acceptance-centralizacao-com-sonda.py

# Uma execução por componente, via JDK 17
Get-FileHash -Algorithm SHA256 -LiteralPath $ckJar
& "$java17/bin/java.exe" -jar $ckJar $fontesDoCenario false 0 false $pastaCsvDoCenario
```

`migration-isolada.py` é uma cópia temporária de `scripts/test-migration.py` com `git show` apontando para o repositório original. O roteiro HTTP complementar é uma cópia do procedimento de 03/11 com quatro consultas de observação ao final e `SERVER_ADDRESS=127.0.0.1` no processo de teste. Os scripts mantiveram os destinos históricos relativos à cópia. Os JSONs foram copiados de lá para esta coleta. A flag de execução `maven.gitcommitid.skip` foi passada no legado, cujo log mostra que o plugin antigo ainda executou e escreveu metadados Git no `target` isolado.

## Metodologia CK e rastreabilidade

Foi usado o JAR `C:\TCC2\ferramentas\ck\target\ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar`, CK **0.7.1-SNAPSHOT**, com SHA-256 `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`. O hash é idêntico ao registrado em 28/10 e 03/11. O commit da fonte da CK registrado na proveniência histórica é `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`. Nesta execução, a identidade da ferramenta foi verificada pelo hash do binário.

Os procedimentos de referência são `scripts/collect-ck.ps1`, `docs/metricas/v2/coletar.ps1` e `docs/metricas/v2-centralizacao-erros/coletar.py`. O mesmo comando foi aplicado em quatro pastas de saída novas com `false 0 false` (busca de JARs desativada, partição automática e métricas de variáveis/campos desativadas). Cada escopo contém apenas o `src/main/java` do seu componente. Incluem-se classes, interfaces, classes internas e tipos com zero. Cada média aritmética dá o mesmo peso a cada linha de `class.csv`. As cinco colunas usadas são `cbo`, `wmc`, `rfc`, `lcom` e `lcom*`.

| Cenário | Fonte | Saídas brutas |
| --- | --- | --- |
| A · Monólito original | `src/main/java` extraído do Git na baseline | [class.csv](A-monolito-original/class.csv) · [method.csv](A-monolito-original/method.csv) · [log](A-monolito-original/coleta.txt) |
| B · Monólito adaptado | `src/main/java` da cópia atual | [class.csv](B-monolito-adaptado/class.csv) · [method.csv](B-monolito-adaptado/method.csv) · [log](B-monolito-adaptado/coleta.txt) |
| C · Owners Service | `owners-service/src/main/java` da cópia atual | [class.csv](C-owners-service/class.csv) · [method.csv](C-owners-service/method.csv) · [log](C-owners-service/coleta.txt) |
| D · Gateway | `gateway/src/main/java` da cópia atual | [class.csv](D-gateway/class.csv) · [method.csv](D-gateway/method.csv) · [log](D-gateway/coleta.txt) |

As quatro execuções terminaram com código 0 e `Metrics extracted!!!`. Os logs registram aviso do Log4j sobre appenders. Para A, a comparação como multiconjunto de linhas, excluindo somente a coluna `file`, reproduziu todos os campos de `class.csv` e `method.csv` das três referências, `docs/baseline`, `docs/metricas/v2/baseline-recoletada` e `docs/metricas/v2-centralizacao-erros/A-monolito-original`. Esse controle verifica a comparabilidade empírica com a baseline. O comando exato da coleta mais antiga não consta no histórico.

## Análise crítica e limitações

- B retém os 24 nomes qualificados de A e acrescenta quatro tipos. O CBO médio do universo completo cai de **6,666667 para 6,571429**. Nos mesmos 24 tipos, sobe para **6,791667**. Os quatro tipos novos têm CBO médio **5,25**, o que explica a inversão. `Owner` baixa de 13 para 9 ao mudar de papel. `PetController` e `VisitController` ganham 3 pontos de CBO cada e 7 e 6 de RFC, respectivamente, na validação e na integração com Owners.
- Nos tipos pareados, **LCOM 5,875000 → 6,583333** e **LCOM* 0,256618 → 0,275740**. A inclusão dos tipos novos com LCOM* baixo faz a média global de B cair para 0,236348. `OwnerRepository` ilustra a divergência entre as fórmulas, com LCOM **3 → 0** e LCOM* **0 → 0,4**, ao passar de interface JPA a adaptador com campos e métodos de composição.
- C tem quatro tipos. O LCOM 36 de `Owner` determina a média de LCOM 9,0 do serviço. A classe concentra os campos do cadastro e a identidade, com acessores e `updateFrom`. Comparar seu LCOM 36 com o LCOM 58 de `Owner` em A exige considerar a remoção da associação com Pets e a redistribuição de estado herdado. A implementação da CK depende de acessos a campos declarados e da organização dos métodos. Zeros em interfaces ou classes pequenas têm leitura limitada.
- Entre 03/11 e 05/11, as cinco métricas de todos os tipos de B e D coincidem. Em C, o CBO de `OwnerRepository` sobe de **2 para 5** com a consulta JPQL anotada (`@Query`, `@Param`, `@Transactional`). As outras quatro métricas permanecem iguais. A média de LCOM de C já havia subido de 3,5 para 9,0 entre 28/10 e 03/11. O aumento atual no CBO e a diferença histórica em coesão correspondem a intervalos distintos.
- CBO e RFC descrevem referências a tipos e chamadas estáticas reconhecidas pela CK. WMC pondera os métodos pela complexidade calculada pelo analisador. LCOM usa pares de métodos e LCOM* é a medida normalizada de acesso a atributos desta versão. Resolução de tipos sem JARs, herança, métodos gerados, proxies Spring, JPQL, configuração externa e dependências HTTP limitam a leitura dos números. As rotas do Gateway estão em propriedades. CBO 2 no seu ponto de entrada não quantifica o custo ou o acoplamento das rotas em produção.
- As médias descrevem escopos distintos e não foram combinadas. A comparação pareada reduz o efeito da composição, mas inclui nomes preservados com responsabilidades diferentes. Latência, disponibilidade, cobertura e consistência distribuída exigem evidências próprias. O teste de migração vale para a fixture HSQLDB/H2. O perfil MySQL ficou fora desta execução.

## Arquivos gerados

Inventário da entrega, com caminhos relativos a esta pasta. Os diretórios temporários foram usados para isolar os builds, os bancos e a análise.

```text
A-monolito-original/class.csv
A-monolito-original/coleta.txt
A-monolito-original/method.csv
B-monolito-adaptado/class.csv
B-monolito-adaptado/coleta.txt
B-monolito-adaptado/method.csv
C-owners-service/class.csv
C-owners-service/coleta.txt
C-owners-service/method.csv
D-gateway/class.csv
D-gateway/coleta.txt
D-gateway/method.csv
evidencias/build-gateway-final.log
evidencias/build-gateway.log
evidencias/build-legado-alternativo.log
evidencias/build-legado-wrapper-direto.log
evidencias/build-legado-wrapper-final.log
evidencias/build-legado.log
evidencias/build-owners-final.log
evidencias/build-owners.log
evidencias/consulta-prefixo.json
evidencias/http-acceptance.json
evidencias/http-centralizacao.json
evidencias/http-centralizacao.log
evidencias/http.log
evidencias/java-17-gateway.txt
evidencias/java-17-owners.txt
evidencias/java-8-legado.txt
evidencias/maven-gateway.txt
evidencias/maven-legado-alternativo.txt
evidencias/maven-legado-wrapper-direto.txt
evidencias/maven-legado.txt
evidencias/maven-owners.txt
evidencias/migracao-final.log
evidencias/migracao.json
evidencias/migracao.log
evidencias/servicos-http-centralizacao/gateway2.log
evidencias/servicos-http-centralizacao/legacy1.log
evidencias/servicos-http-centralizacao/legacy4.log
evidencias/servicos-http-centralizacao/owners0.log
evidencias/servicos-http-centralizacao/owners3.log
evidencias/servicos-http-centralizacao/owners5.log
fontes-java-analisadas.zip
proveniencia.json
relatorio.md
```

## Resumo para análise com ChatGPT

Revalidação em 05/11/2026 da branch `tcc2-strangler-fig-v2`, HEAD `dd6c9e5` mais as cinco alterações locais. Baseline PetClinic 1.5.x no commit `c36452a`. A, B, C e D designam, nesta coleta, monólito original, monólito adaptado, Owners Service e Gateway. CK 0.7.1-SNAPSHOT com o mesmo hash e parâmetros `false 0 false` das coletas recentes. Os CSVs da baseline são iguais aos três históricos após excluir os caminhos.

| Componente | Java | Maven | Build | Tests run | Executados | Aprovados | Ignorados | Falhas | Erros |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Monólito legado | Temurin 1.8.0_504 | 3.3.3 · Wrapper | [Sucesso](evidencias/build-legado-wrapper-final.log) | 61 | 60 | 60 | 1 | 0 | 0 |
| Owners Service | Oracle 17.0.12 | 3.9.11 | [Sucesso](evidencias/build-owners-final.log) | 6 | 6 | 6 | 0 | 0 | 0 |
| Gateway | Oracle 17.0.12 | 3.9.11 | [Sucesso](evidencias/build-gateway-final.log) | 1 | 1 | 1 | 0 | 0 | 0 |
| **Total** | — | — | **3 de 3** | 68 | 67 | 67 | 1 | 0 | 0 |

| Verificação | Resultado | Escopo | Evidência |
| --- | --- | --- | --- |
| Aceitação HTTP atual | Aprovada | 57 verificações HTTP | [JSON](evidencias/http-acceptance.json) · [log](evidencias/http.log) |
| Aceitação HTTP de 03/11 + sonda | Aprovada | 59 verificações do roteiro e 4 consultas de observação | [JSON](evidencias/http-centralizacao.json) · [log](evidencias/http-centralizacao.log) |
| Migração com fixture histórica | Aprovada | IDs, vínculos, sequência, proteção do destino e hash da origem | [JSON](evidencias/migracao.json) · [log](evidencias/migracao-final.log) |

Na base de teste do roteiro complementar, prefixo vazio, `%` e `_` retornaram os mesmos 12 cadastros. `zzzz` retornou zero. Os resultados mostram o uso de `%` e `_` como curingas da consulta `LIKE` atual. O roteiro histórico não estabelece um contrato para esses dois caracteres. [IDs retornados](evidencias/consulta-prefixo.json)

- Os tipos novos fazem o CBO e os dois LCOM médios de B caírem no universo completo, embora subam no pareamento de 24 tipos. O RFC e o WMC sobem nos dois recortes.
- `PetController` e `VisitController` ganham referências e chamadas. O adaptador `OwnerRepository` do legado muda de papel e tem LCOM e LCOM* em direções diferentes.
- `Owner` em C reúne o cadastro, com escopo diferente do `Owner` em A. Em C, a diferença frente à coleta de 03/11 nas cinco métricas se concentra no CBO de `OwnerRepository`. O Gateway é majoritariamente configuração.
- Médias por componente e por subgrupo têm universos diferentes. As métricas são estáticas e uma queda de LCOM por si só não demonstra ganho arquitetural.

### Médias por componente

| Cenário | Tipos CK | Classes de topo | Interfaces | Classes internas | Linhas em method.csv | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A · Monólito original | 24 | 20 | 4 | 0 | 91 | 6.666667 | 4.916667 | 3.458333 | 5.875000 | 0.256618 |
| B · Monólito adaptado | 28 | 24 | 3 | 1 | 107 | 6.571429 | 5.357143 | 4.928571 | 5.750000 | 0.236348 |
| C · Owners Service | 4 | 3 | 1 | 0 | 20 | 8.000000 | 5.000000 | 2.750000 | 9.000000 | 0.236111 |
| D · Gateway | 1 | 1 | 0 | 0 | 1 | 2.000000 | 1.000000 | 1.000000 | 0.000000 | 0.000000 |

### A × B com composição controlada

| Conjunto | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| A · Universo completo | 24 | 6.666667 | 4.916667 | 3.458333 | 5.875000 | 0.256618 |
| B · Universo completo | 28 | 6.571429 | 5.357143 | 4.928571 | 5.750000 | 0.236348 |
| Δ B − A · Universos completos | 4 | -0.095238 | 0.440476 | 1.470238 | -0.125000 | -0.020270 |
| A · Tipos pareados por nome | 24 | 6.666667 | 4.916667 | 3.458333 | 5.875000 | 0.256618 |
| B · Tipos pareados por nome | 24 | 6.791667 | 5.375000 | 4.375000 | 6.583333 | 0.275740 |
| Δ B − A · Pareados | 0 | 0.125000 | 0.458333 | 0.916667 | 0.708333 | 0.019122 |
| A · Pareados com arquivo modificado | 9 | 11.000000 | 8.666667 | 7.333333 | 12.888889 | 0.395426 |
| B · Pareados com arquivo modificado | 9 | 11.333333 | 9.888889 | 9.777778 | 14.777778 | 0.446417 |
| Δ B − A · Arquivo modificado | 0 | 0.333333 | 1.222222 | 2.444444 | 1.888889 | 0.050991 |

### Todos os tipos pareados em A × B

Os nomes abaixo são relativos a `org.springframework.samples.petclinic`. O pareamento usa o nome qualificado. A indicação de fonte alterada ignora apenas diferenças de fim de linha. Owner e OwnerRepository preservam os nomes com mudanças de papel.

| Tipo | Estado | CBO A → B | WMC A → B | RFC A → B | LCOM A → B | LCOM* A → B |
| --- | --- | --- | --- | --- | --- | --- |
| `owner.Owner` | Entidade JPA em A, representação MVC em B | 13 → 9 | 19 → 19 | 15 → 16 | 58 → 58 | 0.78846157 → 0.78846157 |
| `owner.OwnerController` | Mantém MVC e chama o adaptador; métricas estáveis | 15 → 15 | 14 → 14 | 17 → 17 | 0 → 0 | 0.44444448 → 0.44444448 |
| `owner.OwnerRepository` | Interface JPA em A, adaptador HTTP e composição com Pets em B | 5 → 6 | 3 → 7 | 0 → 8 | 3 → 0 | 0.0 → 0.4 |
| `owner.Pet` | Associação JPA a Owner passa a usar ownerId | 15 → 15 | 11 → 12 | 6 → 6 | 37 → 45 | 0.8 → 0.8181818 |
| `owner.PetController` | Valida Owner remoto e vínculo local | 20 → 23 | 14 → 17 | 15 → 22 | 8 → 17 | 0.5925926 → 0.6333334 |
| `owner.PetRepository` | Inclui consulta por ownerId | 5 → 5 | 3 → 4 | 0 → 0 | 3 → 6 | 0.0 → 0.0 |
| `owner.PetTypeFormatter` | Ajustes de nomes, métricas estáveis | 6 → 6 | 5 → 5 | 3 → 3 | 1 → 1 | 0.33333334 → 0.33333334 |
| `owner.VisitController` | Valida o vínculo Owner–Pet e delega tratamento de falhas | 14 → 17 | 6 → 8 | 6 → 12 | 6 → 6 | 0.6 → 0.6 |
| `vet.VetController` | Ajustes de nomes, métricas estáveis | 6 → 6 | 3 → 3 | 4 → 4 | 0 → 0 | 0.0 → 0.0 |
| `PetClinicApplication` | Fonte idêntica | 2 → 2 | 1 → 1 | 1 → 1 | 0 → 0 | 0.0 → 0.0 |
| `model.BaseEntity` | Fonte idêntica | 3 → 3 | 4 → 4 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `model.NamedEntity` | Fonte idêntica | 4 → 4 | 3 → 3 | 1 → 1 | 1 → 1 | 0.33333334 → 0.33333334 |
| `model.Person` | Fonte idêntica | 4 → 4 | 4 → 4 | 0 → 0 | 2 → 2 | 0.5 → 0.5 |
| `owner.PetType` | Fonte idêntica | 3 → 3 | 0 → 0 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `owner.PetValidator` | Fonte idêntica | 5 → 5 | 6 → 6 | 7 → 7 | 1 → 1 | 0.5 → 0.5 |
| `system.CacheConfig` | Fonte idêntica | 7 → 7 | 2 → 2 | 3 → 3 | 1 → 1 | 0.0 → 0.0 |
| `system.CrashController` | Fonte idêntica | 2 → 2 | 1 → 1 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `system.WelcomeController` | Fonte idêntica | 2 → 2 | 1 → 1 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `vet.Specialty` | Fonte idêntica | 3 → 3 | 0 → 0 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `vet.Vet` | Fonte idêntica | 10 → 10 | 6 → 6 | 5 → 5 | 8 → 8 | 0.6 → 0.6 |
| `vet.VetRepository` | Fonte idêntica | 4 → 4 | 1 → 1 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `vet.Vets` | Fonte idêntica | 3 → 3 | 2 → 2 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
| `visit.Visit` | Fonte idêntica | 7 → 7 | 7 → 7 | 0 → 0 | 11 → 11 | 0.6666667 → 0.6666667 |
| `visit.VisitRepository` | Fonte idêntica | 2 → 2 | 2 → 2 | 0 → 0 | 1 → 1 | 0.0 → 0.0 |

### Tipos novos em B

| Tipo | Papel | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| `integration.IntegrationErrorHandler` | Classifica falhas e define resposta HTTP/MVC | 7 | 8 | 5 | 3 | 0.0 |
| `integration.IntegrationErrorHandler$IntegrationFailure` | Classe interna que carrega status e causa | 1 | 2 | 1 | 0 | 0.0 |
| `integration.OwnersClient` | Cliente HTTP, contrato de cadastro e delegação de falhas | 8 | 8 | 20 | 0 | 0.0 |
| `system.LegacyDatabaseInitializer` | Infraestrutura de inicialização do banco do legado | 5 | 3 | 7 | 0 | 0.0 |
| Média · integração HTTP (3) |  | 5.333333 | 6.000000 | 8.666667 | 1.000000 | 0.000000 |
| Média · todos os novos (4) |  | 5.250000 | 5.250000 | 8.250000 | 0.750000 | 0.000000 |

### Owners Service (C)

Tipos do pacote `br.edu.tcc.owners`.

| Tipo | Classificação | Papel | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `Owner` | Domínio migrado/adaptado | Cadastro antes distribuído entre Owner, Person e BaseEntity. Pets ficam no legado. | 9 | 12 | 0 | 36 | 0.77777773 |
| `OwnerController` | Novo · API REST | Expõe cadastro e busca. O controller MVC continua no legado. | 16 | 6 | 10 | 0 | 0.16666667 |
| `OwnerRepository` | Persistência de domínio adaptada | Porta JPA do cadastro. Consulta JPQL por prefixo e sem distinção de caixa. | 5 | 1 | 0 | 0 | 0.0 |
| `OwnersApplication` | Novo · inicialização | Ponto de entrada do serviço independente. | 2 | 1 | 1 | 0 | 0.0 |

| Conjunto | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| Domínio e persistência adaptados | 2 | 7.000000 | 6.500000 | 0.000000 | 18.000000 | 0.388889 |
| REST e inicialização novos | 2 | 9.000000 | 3.500000 | 5.500000 | 0.000000 | 0.083333 |

### Origem funcional em A e destino em C

O pareamento indica origem de responsabilidade. Os valores cobrem cada tipo inteiro e se repetem quando uma responsabilidade foi repartida. As diferenças de escopo inviabilizam uma média pareada neste quadro.

| A → C | Vínculo funcional | CBO A → C | WMC A → C | RFC A → C | LCOM A → C | LCOM* A → C |
| --- | --- | --- | --- | --- | --- | --- |
| `owner.Owner` → `Owner` | Campos de cadastro e identidade. A relação com Pets fica em B. | 13 → 9 | 19 → 12 | 15 → 0 | 58 → 36 | 0.78846157 → 0.77777773 |
| `model.Person` → `Owner` | First/last name. Person segue sendo usada em B. | 4 → 9 | 4 → 12 | 0 → 0 | 2 → 36 | 0.5 → 0.77777773 |
| `model.BaseEntity` → `Owner` | Identidade de Owner. A classe base segue em B. | 3 → 9 | 4 → 12 | 0 → 0 | 0 → 36 | 0.0 → 0.77777773 |
| `owner.OwnerRepository` → `OwnerRepository` | Busca e persistência do cadastro sem join com Pets. | 5 → 5 | 3 → 1 | 0 → 0 | 3 → 0 | 0.0 → 0.0 |
| `owner.OwnerController` → `OwnerController` | Operações de cadastro reaparecem como API. A renderização MVC fica em B. | 15 → 16 | 14 → 6 | 17 → 10 | 0 → 0 | 0.44444448 → 0.16666667 |

### Gateway (D)

| Tipo | Papel | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| `br.edu.tcc.gateway.GatewayApplication` | Inicializa Spring Cloud Gateway | 2 | 1 | 1 | 0 | 0.0 |

Com um único tipo, os valores da classe coincidem com a média de D. As duas rotas são declaradas em `application.properties`.

### Histórico por componente

| Conjunto | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| B · 28/10 · V2 | 30 | 6.166667 | 4.866667 | 4.366667 | 5.266667 | 0.227258 |
| B · 03/11 · Centralização | 28 | 6.571429 | 5.357143 | 4.928571 | 5.750000 | 0.236348 |
| B · 05/11 · Atual | 28 | 6.571429 | 5.357143 | 4.928571 | 5.750000 | 0.236348 |
| C · 28/10 · V2 | 4 | 7.250000 | 5.000000 | 2.750000 | 3.500000 | 0.218750 |
| C · 03/11 · Centralização | 4 | 7.250000 | 5.000000 | 2.750000 | 9.000000 | 0.236111 |
| C · 05/11 · Atual | 4 | 8.000000 | 5.000000 | 2.750000 | 9.000000 | 0.236111 |
| D · 28/10 · V2 | 1 | 2.000000 | 1.000000 | 1.000000 | 0.000000 | 0.000000 |
| D · 03/11 · Centralização | 1 | 2.000000 | 1.000000 | 1.000000 | 0.000000 | 0.000000 |
| D · 05/11 · Atual | 1 | 2.000000 | 1.000000 | 1.000000 | 0.000000 | 0.000000 |

O monólito centralizado da coleta de 03/11 tinha a letra C naquele experimento. Aqui ele é o antecedente de B. As linhas com composição diferente são descritivas. O quadro abaixo registra as mudanças nas cinco métricas entre 03/11 e 05/11.

| Tipo | CBO anterior → atual | WMC anterior → atual | RFC anterior → atual | LCOM anterior → atual | LCOM* anterior → atual |
| --- | --- | --- | --- | --- | --- |
| C · OwnerRepository | 2 → 5 | 1 → 1 | 0 → 0 | 0 → 0 | 0.0 → 0.0 |
