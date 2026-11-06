# TCC II — Strangler Fig V2: extração exclusiva de Owner

A branch `tcc2-strangler-fig-v2` mantém o Spring PetClinic legado e extrai somente o cadastro de Owner. Pets, PetTypes, Visits e Vets permanecem no monólito. A baseline original é a branch `1.5.x`, commit `c36452a2c34443ae26b4ecbba4f149906af14717`.

| Componente ativo | Plataforma | Porta | Persistência |
| --- | --- | --- | --- |
| Monólito | Java 8 / Spring Boot 1.5.4.RELEASE / Thymeleaf | 8080 | Pets, tipos, visitas e veterinários — HSQLDB |
| Gateway | Java 17 / Boot 3.5.16 / Spring Cloud 2025.0.3 WebFlux | 8081 | Sem banco |
| owners-service | Java 17 / Boot 3.5.16 | 8082 | Exclusivamente Owner — H2 |

O Gateway encaminha `/api/owners` e `/api/owners/**` para Owners e os demais caminhos, inclusive os formulários `/owners/**`, para o monólito. Acesse [a aplicação pelo Gateway](http://localhost:8081). O monólito usa a interface `OwnerRepository`. O `OwnerIntegrationAdapter` consulta o serviço e compõe os Pets locais usando `ownerId`.

O `IntegrationErrorHandler` centraliza classificação e respostas dos erros de integração: 404 para Owner inexistente ou vínculo inválido de Pet, 400 para dados rejeitados e 503 com `ownersUnavailable` para indisponibilidade, conexão, timeout ou conversão remota. Um único tipo interno delimita esse tratamento; erros internos não relacionados seguem o tratamento original.

## Projetos Maven

| POM | JDK | Maven |
| --- | --- | --- |
| `projects/monolith/pom.xml` | 8 | Wrapper 3.3.3 |
| `projects/owners-service/pom.xml` | 17 | 3.9.11 |
| `projects/gateway/pom.xml` | 17 | 3.9.11 |

Os três projetos são independentes. O repositório não tem POM agregador na raiz.

## Abrir os três projetos no IntelliJ IDEA

1. Abra a **pasta raiz do repositório** em **File → Open**. No painel **Maven**, confira se os três projetos da tabela aparecem. Se faltar algum, use **Link Maven Projects** (ou **Add Maven Project**, conforme a versão) e selecione o POM na mesma janela. [Projetos Maven no IntelliJ](https://www.jetbrains.com/help/idea/delegate-build-and-run-actions-to-maven.html)
2. Em **File → Project Structure → Modules → Dependencies**, selecione o **Module SDK** de cada projeto conforme a tabela. Em **Sources**, confira os níveis de linguagem 8 e 17. [SDK por módulo](https://www.jetbrains.com/help/idea/configure-modules.html)
3. Em **Run → Edit Configurations**, crie uma configuração **Maven** para cada POM. Selecione o diretório do respectivo projeto, o Maven da tabela e o **JRE** em **Java Options**. Use `clean package -Dcobertura.skip=true` para o monólito e `clean package` para os outros dois. Assim, o Maven que compila o monólito é executado de fato com Java 8. [Configuração Maven](https://www.jetbrains.com/help/idea/run-debug-configuration-maven.html)
4. Para iniciar uma aplicação pela IDE, selecione o módulo e o JRE correspondentes na configuração **Application** ou **Spring Boot**. Defina **Working directory** como `$PROJECT_DIR$` (a raiz do repositório), para que os caminhos relativos dos bancos continuem apontando para `data/`. [Configuração Application](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html)

Para pesquisar somente Java de produção, crie um escopo local **Java de produção** em **Settings → Appearance & Behavior → Scopes**. Selecione cada uma das pastas abaixo e clique em **Include Recursively**. Em **Find in Files**, use o escopo criado e a máscara `*.java`. A seleção vale apenas para a busca. [Escopos no IntelliJ](https://www.jetbrains.com/help/idea/configuring-scopes-and-file-colors.html)

- `projects/monolith/src/main/java`
- `projects/owners-service/src/main/java`
- `projects/gateway/src/main/java`

## Compilar e executar os testes

No PowerShell, defina `JAVA8_HOME` e `JAVA17_HOME` com as pastas dos JDKs. Use `MAVEN_HOME` ou coloque o Maven 3.9.11 no `PATH`. Execute na raiz:

```powershell
./scripts/build.ps1
python scripts/acceptance.py
python scripts/test-migration.py
```

O build usa o Wrapper original do monólito e o Maven 3.9.11 para os módulos Boot 3. No Windows, o script invoca o JAR do Wrapper diretamente para contornar as aspas do `mvnw.cmd` em caminhos com espaços. Acrescente `-Offline` ao build quando o cache Maven estiver completo. Também é possível informar as instalações com `-Java8Home`, `-Java17Home` e `-MavenHome`.

A aceitação HTTP precisa das portas locais 8080, 8081 e 8082 livres e usa os JARs do build. Ela grava bancos descartáveis, logs e `http-acceptance.json` em `.work/acceptance-*`. A migração de teste usa uma fixture descartável e grava `migration.json` em `.work/migration-*`.

## Iniciar os três componentes

Depois do build, use três terminais na raiz do repositório, com as mesmas variáveis dos JDKs:

```powershell
# Terminal 1 — Owners
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/owners-service/target/owners-service-1.0.0-SNAPSHOT.jar
```

```powershell
# Terminal 2 — monólito
& "$env:JAVA8_HOME/bin/java.exe" -jar projects/monolith/target/spring-petclinic-1.5.1.jar
```

```powershell
# Terminal 3 — Gateway
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar
```

A primeira inicialização cria bancos separados em `data/v2/petclinic.*` e `data/owners-v2.mv.db`, relativos à raiz. Reinícios mantêm os registros e as sequências. `LEGACY_DB_URL` e `OWNERS_DB_URL` permitem caminhos isolados. Encerre com Ctrl+C. O perfil MySQL e o docker-compose históricos não fazem parte desta execução V2.

## Testes e rastreabilidade acadêmica

A validação da centralização registrou 60 testes aprovados e 1 ignorado no monólito, 6 aprovados em Owners e 1 no Gateway, além de 59 checks HTTP com reinicialização e migração em fixture. As execuções posteriores desta manutenção estão registradas na [nota de manutenção](docs/v2/manutencao-nomes-publicacao.md). Checks HTTP incluem prontidão e não representam cobertura percentual.

- [Estudo do código antes/depois](docs/v2/estudo-codigo-antes-depois.md): trechos reais de 1.5.x, V2 e intervenção posterior de erros.
- [Implementação V2 original](docs/v2/implementacao.md): fronteiras, integridade, migração e limitações daquele checkpoint.
- [CK e evidências da primeira V2](docs/metricas/v2/relatorio.md).
- [CK e evidências da centralização posterior](docs/metricas/v2-centralizacao-erros/relatorio.md): A/B/C, snapshots e limitações da ferramenta.
- [Procedimento de reprodução em nova pasta](docs/metricas/v2-centralizacao-erros/README.md).
- [README original de 2017](docs/historico/README-original-2017.md).

As métricas são vinculadas aos fontes e hashes do checkpoint medido. Para uma nova coleta, defina `JAVA17_HOME` e aponte `$ckJar` para o JAR `ck-0.7.1-SNAPSHOT` com o SHA-256 histórico:

```powershell
./scripts/collect-ck.ps1 -CkJar $ckJar
```

O script coleta somente `src/main/java` dos três projetos. A análise usa o CSV histórico como referência e grava seus resultados junto à coleta em uma nova pasta `.work/ck-*`.

## Implementação histórica removida da árvore atual

`owners-pets-service` pertence à implementação anterior, que extraía Owners e Pets juntos, e não integra esta V2. Seus fontes permanecem no histórico Git e na branch `tcc2-strangler-fig`; o diretório não é necessário para compilar, testar, executar ou medir os componentes atuais. A árvore ativa usa somente `owners-service`, Gateway e monólito. Documentos e evidências históricos continuam preservados.

Arquivos de build e configuração local de IDE permanecem ignorados. Logs acadêmicos dentro de `docs/metricas` são intencionalmente versionáveis, assim como CSVs, snapshots de fontes e proveniência.
