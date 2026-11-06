# TCC II — Spring PetClinic Strangler Fig V2

Este projeto aplica o padrão Strangler Fig ao Spring PetClinic legado para migrar o cadastro de proprietários em etapas. O microsserviço Owners é a fonte de cadastro de Owner. O monólito continua servindo a interface web e mantendo Pets, tipos de Pet, visitas e veterinários. O Gateway dá acesso aos dois componentes por uma entrada comum.

| Projeto | Papel | JDK | Porta |
| --- | --- | --- | ---: |
| [Monólito](projects/monolith/) | Spring Boot 1.5, Thymeleaf e HSQLDB. O adaptador consulta Owners por HTTP e compõe os Pets locais pelo `ownerId`. | 8 | 8080 |
| [Owners Service](projects/owners-service/) | API REST de proprietários em Spring Boot 3, com banco H2 próprio. | 17 | 8082 |
| [Gateway](projects/gateway/) | Spring Cloud Gateway. Encaminha `/api/owners` e seus subcaminhos para Owners e os demais caminhos para o monólito. | 17 | 8081 |

## Compilar e testar

Na raiz do repositório, em PowerShell 7, defina `JAVA8_HOME` e `JAVA17_HOME`. Configure `MAVEN_HOME` ou adicione o Maven 3.9.11 ao `PATH`. O monólito usa o Wrapper 3.3.3. Python 3 é necessário para a aceitação e a migração de teste.

```powershell
./scripts/build.ps1
python scripts/acceptance.py
python scripts/test-migration.py
```

O build compila, executa os testes Maven e empacota os três JARs. Use `-Offline` com o cache Maven completo. Se o plugin legado falhar ao resolver metadados Git em caminhos com espaços ou em uma worktree, use `./scripts/build.ps1 -SkipGitMetadata`. A aceitação usa as portas 8080, 8081 e 8082 livres. A aceitação e a migração criam bancos e evidências descartáveis em `.work/`.

## Executar a aplicação

Depois do build, inicie cada componente em um terminal separado, sempre na raiz:

```powershell
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/owners-service/target/owners-service-1.0.0-SNAPSHOT.jar
& "$env:JAVA8_HOME/bin/java.exe" -jar projects/monolith/target/spring-petclinic-1.5.1.jar
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar
```

Abra [PetClinic no Gateway](http://localhost:8081). Os bancos persistem sob `data/`. As variáveis `LEGACY_DB_URL` e `OWNERS_DB_URL` permitem usar bancos isolados.

## Código, medições e origem

- [Scripts de build, aceitação, migração e coleta](scripts/).
- [Implementação e decisões da V2](docs/v2/) e [evidências de validação de Owner.addPet()](docs/v2/evidencias-addpet/).
- [Metodologia CK](docs/metricas/README.md), [coleta definitiva A/B/C](docs/metricas/atual/README.md) e [coletas históricas](docs/metricas/historico/README.md).

A baseline é a branch `1.5.x`, no commit `c36452a2c34443ae26b4ecbba4f149906af14717`, também identificado pela tag `tcc2-baseline-2017`. O código de origem é do projeto Spring PetClinic, distribuído sob a Apache License 2.0. [Projeto Spring PetClinic](https://github.com/spring-projects/spring-petclinic) · [README de 2017 e créditos](docs/historico/README-original-2017.md) · [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
