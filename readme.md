# Spring PetClinic Legacy - Modernização com Strangler Fig

Projeto desenvolvido para o TCC II, com o objetivo de avaliar a modernização incremental de um sistema legado utilizando o padrão arquitetural Strangler Fig.

A aplicação utilizada como referência é o Spring PetClinic de 2017, originalmente desenvolvido com Java 8 e Spring Boot 1.5.4.RELEASE.

A modernização preserva a interface Thymeleaf no monólito e transfere gradualmente as funcionalidades de proprietários (Owners) e animais (Pets) para um microsserviço Java 17.

## 1. Arquitetura

| Componente | Tecnologia | Porta | Responsabilidade |
|---|---|---|---|
| Monólito | Java 8 / Spring Boot 1.5.4 | 8080 | Interface Thymeleaf, visitas e veterinários |
| Gateway | Java 17 / Spring Cloud Gateway | 8081 | Entrada única e roteamento |
| Owners/Pets Service | Java 17 / Spring Boot 3.5.16 | 8082 | API e persistência de Owners, Pets e PetTypes |

O Gateway encaminha as rotas `/api/owners`, `/api/owners/**`, `/api/pets/**` e `/api/pet-types` ao microsserviço.

As demais requisições são encaminhadas ao monólito.

O monólito também realiza chamadas HTTP ao microsserviço para obter dados necessários à composição das páginas e ao cadastro de visitas.

### Responsabilidades

**Microsserviço:**
- Cadastro, consulta e atualização de proprietários.
- Cadastro, consulta e atualização de animais.
- Consulta de tipos de animais.
- Persistência dos dados de Owners e Pets.

**Monólito:**
- Interface web Thymeleaf.
- Cadastro e consulta de visitas.
- Funcionalidades de veterinários.
- Integração com o microsserviço.

## 2. Requisitos

- Git
- JDK 8 para o monólito
- JDK 17 para o Gateway e o microsserviço
- Maven 3 para as aplicações Java 17
- PowerShell para executar os exemplos abaixo

O monólito utiliza o Maven Wrapper incluído no repositório.

## 3. Clonar o repositório

A implementação da modernização está na branch `tcc2-strangler-fig`.

```powershell
git clone -b tcc2-strangler-fig --single-branch https://github.com/pepepedrinho/TCC2-Spring-PetClinic-Legacy.git
cd TCC2-Spring-PetClinic-Legacy
```

## 4. Inicialização dos bancos

Os bancos utilizam armazenamento em arquivo.

A carga histórica deve ser executada somente na primeira inicialização de cada banco.

**Atenção:** os perfis de inicialização não devem ser executados novamente sobre bancos já populados, pois os scripts históricos podem recriar tabelas e apagar dados.

### 4.1. Microsserviço - primeira execução

Abra um terminal com Java 17 e Maven configurados.

Na raiz do repositório:

```powershell
cd owners-pets-service
mvn spring-boot:run "-Dspring-boot.run.profiles=init"
```

A inicialização cria e popula o banco H2 persistente com os dados históricos.

Depois de confirmar que a aplicação iniciou corretamente, encerre com `Ctrl+C`.

O perfil `init` não deve ser utilizado novamente sobre o mesmo banco.

### 4.2. Monólito - primeira execução

Abra outro terminal com Java 8 configurado.

Na raiz do repositório:

```powershell
$repo = (Get-Location).Path
& "$env:JAVA_HOME\bin\java.exe" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$repo" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run "-Drun.profiles=persistente,persistente-init"
```

A inicialização cria e popula o banco HSQLDB persistente.

Depois de confirmar que a aplicação iniciou corretamente, encerre com `Ctrl+C`.

O perfil `persistente-init` não deve ser utilizado novamente sobre o mesmo banco.

## 5. Execução normal

Após a carga inicial, execute as três aplicações em terminais separados.

Todos os comandos devem partir da raiz do repositório, salvo indicação contrária.

### Terminal 1 - Microsserviço (Java 17)

```powershell
cd owners-pets-service
mvn spring-boot:run
```

### Terminal 2 - Monólito (Java 8)

```powershell
$repo = (Get-Location).Path
& "$env:JAVA_HOME\bin\java.exe" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$repo" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run "-Drun.profiles=persistente"
```

### Terminal 3 - Gateway (Java 17)

```powershell
cd gateway
mvn spring-boot:run
```

Após iniciar as três aplicações, acesse:

http://localhost:8081

A porta 8081 é o ponto de entrada principal da arquitetura.

## 6. Persistência

O microsserviço utiliza H2 persistente:

`owners-pets-service/data/ownerspets`

O monólito utiliza HSQLDB persistente:

`data/petclinic-legado`

Esses diretórios são ignorados pelo Git.

O banco do microsserviço contém os dados de proprietários e animais.

O monólito continua responsável pelos dados de visitas e veterinários.

A referência de uma visita ao animal é realizada por identificador e validada pela integração entre as aplicações, sem chave estrangeira entre bancos separados.

## 7. Testes automatizados

### Monólito - Java 8

Na raiz do repositório:

```powershell
$repo = (Get-Location).Path
& "$env:JAVA_HOME\bin\java.exe" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$repo" org.apache.maven.wrapper.MavenWrapperMain test
```

Na última validação, foram executados 51 testes, com zero falhas, zero erros e um teste ignorado.

### Microsserviço - Java 17

```powershell
cd owners-pets-service
mvn test
```

### Gateway - Java 17

```powershell
cd gateway
mvn test
```

A quantidade de 51 testes refere-se à suíte do monólito, e não à soma de todos os componentes.

## 8. Comportamento diante de falhas

A indisponibilidade do microsserviço Owners/Pets foi testada interrompendo sua execução.

Durante a falha:
- As páginas dependentes do microsserviço retornaram HTTP 503.
- Uma mensagem amigável foi exibida.
- As funcionalidades de veterinários permaneceram acessíveis pelo Gateway.

O tratamento HTTP 503 também possui teste automatizado.

## 9. Avaliação com métricas CK

A ferramenta CK 0.7.1 foi utilizada para comparar o código original e a arquitetura refatorada.

| Indicador | Baseline 2017 | Monólito refatorado | Microsserviço |
|---|---:|---:|---:|
| Classes | 24 | 32 | 16 |
| CBO médio | 6,67 | 6,34 | 5,06 |
| WMC médio | 4,92 | 5,88 | 3,69 |
| RFC médio | 3,46 | 4,72 | 5,38 |
| LCOM médio | 5,88 | 9,19 | 7,44 |
| LCOM* médio | 0,26 | 0,32 | 0,26 |

A comparação identificou resultados mistos.

O principal resultado observado foi a viabilidade da modernização incremental, sem uma redução generalizada das métricas de qualidade interna.

As limitações das métricas e a interpretação dos resultados estão detalhadas na documentação.

## 10. Documentação

- [Avaliação dos resultados](docs/avaliacao-resultados.md)
- [Planejamento da extração](docs/arquitetura/planejamento-extracao.md)
- [Etapa E1 - Gateway](docs/arquitetura/etapa-e1-gateway.md)
- [Baseline histórica](docs/baseline/)
- [Métricas da arquitetura refatorada](docs/metricas/)
- [README original de 2017](docs/historico/README-original-2017.md)

## 11. Origem e licença

Este trabalho utiliza como base o projeto open source Spring PetClinic:

https://github.com/spring-projects/spring-petclinic

A versão histórica utilizada corresponde ao commit:

`c36452a2c34443ae26b4ecbba4f149906af14717`

Data: 03/11/2017.

O projeto original é disponibilizado sob a licença Apache 2.0. Consulte o arquivo de licença presente no repositório.

O README histórico foi preservado em `docs/historico/README-original-2017.md`.