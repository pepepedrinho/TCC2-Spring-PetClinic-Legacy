# TCC II — Strangler Fig V2: extração exclusiva de Owner

A branch `tcc2-strangler-fig-v2` mantém o Spring PetClinic legado e extrai somente o cadastro de Owner. Pets, PetTypes, Visits e Vets permanecem no monólito. A baseline original é a branch `1.5.x`, commit `c36452a2c34443ae26b4ecbba4f149906af14717`.

| Componente ativo | Plataforma | Porta | Persistência |
| --- | --- | --- | --- |
| Monólito | Java 8 / Spring Boot 1.5.4.RELEASE / Thymeleaf | 8080 | Pets, tipos, visitas e veterinários — HSQLDB |
| Gateway | Java 17 / Boot 3.5.16 / Spring Cloud 2025.0.3 WebFlux | 8081 | Sem banco |
| owners-service | Java 17 / Boot 3.5.16 | 8082 | Exclusivamente Owner — H2 |

O Gateway encaminha `/api/owners` e `/api/owners/**` para Owners e os demais caminhos, inclusive os formulários `/owners/**`, para o monólito. Acesse [a aplicação pelo Gateway](http://localhost:8081). O monólito consulta o serviço pelo `OwnersClient` e compõe os Pets locais usando `ownerId`.

O `IntegrationErrorHandler` centraliza classificação e respostas dos erros de integração: 404 para Owner inexistente ou vínculo inválido de Pet, 400 para dados rejeitados e 503 com `ownersUnavailable` para indisponibilidade, conexão, timeout ou conversão remota. Um único tipo interno delimita esse tratamento; erros internos não relacionados seguem o tratamento original.

## Compilar e executar os testes

Pré-requisitos: PowerShell, JDK 8, JDK 17 e Maven 3.9.11. Na raiz:

```powershell
./scripts/build.ps1 -MavenHome 'C:\TCC2\ferramentas\apache-maven-3.9.11' `
    -Java8Home 'C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot' `
    -Java17Home 'C:\Program Files\Java\jdk-17'
```

O script executa `clean package` com todas as suítes dos três projetos, usando o JDK de cada módulo. Acrescente `-Offline` quando as dependências já estiverem disponíveis. Ajuste os caminhos à sua instalação. O Maven Wrapper histórico é 3.3.3; para os módulos Boot 3, use Maven 3.9.11 pelo script.

## Iniciar os três componentes

Depois do build, use três terminais, todos na raiz do repositório:

```powershell
# Terminal 1 — Owners
& 'C:\Program Files\Java\jdk-17\bin\java.exe' -jar owners-service/target/owners-service-1.0.0-SNAPSHOT.jar
```

```powershell
# Terminal 2 — monólito
& 'C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\java.exe' -jar target/spring-petclinic-1.5.1.jar
```

```powershell
# Terminal 3 — Gateway
& 'C:\Program Files\Java\jdk-17\bin\java.exe' -jar gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar
```

A primeira inicialização cria bancos separados em `data/v2/petclinic.*` e `data/owners-v2.mv.db`. Reinícios mantêm os registros e as sequências; bancos existentes não são reinicializados. `LEGACY_DB_URL` e `OWNERS_DB_URL` permitem caminhos isolados. Encerre com Ctrl+C. O perfil MySQL e o docker-compose históricos não fazem parte desta execução V2.

## Testes e rastreabilidade acadêmica

A validação da centralização registrou 60 testes aprovados e 1 ignorado no monólito, 6 aprovados em Owners e 1 no Gateway, além de 59 checks HTTP com reinicialização e migração em fixture. As execuções posteriores desta manutenção estão registradas na [nota de manutenção](docs/v2/manutencao-nomes-publicacao.md). Checks HTTP incluem prontidão e não representam cobertura percentual.

- [Estudo do código antes/depois](docs/v2/estudo-codigo-antes-depois.md): trechos reais de 1.5.x, V2 e intervenção posterior de erros.
- [Implementação V2 original](docs/v2/implementacao.md): fronteiras, integridade, migração e limitações daquele checkpoint.
- [CK e evidências da primeira V2](docs/metricas/v2/relatorio.md).
- [CK e evidências da centralização posterior](docs/metricas/v2-centralizacao-erros/relatorio.md): A/B/C, snapshots e limitações da ferramenta.
- [Procedimento de reprodução em nova pasta](docs/metricas/v2-centralizacao-erros/README.md).
- [README original de 2017](docs/historico/README-original-2017.md).

As métricas são vinculadas aos fontes e hashes do checkpoint medido. A padronização posterior de identificadores não recalcula nem substitui CK e não autoriza atribuir automaticamente esses valores ao fonte atual. Não execute os coletores históricos sobre suas saídas preservadas. Para nova medição, use uma pasta separada e o procedimento documentado.

`scripts/acceptance.py` e `scripts/test-migration.py` preservam os procedimentos HTTP/migração da primeira V2 e seus destinos históricos de evidências. Para repeti-los sem sobrescrever resultados acadêmicos, execute cópias isoladas com os destinos redirecionados, conforme as cópias registradas em `docs/metricas/v2-centralizacao-erros/evidencias/procedimentos-executados/`.

## Implementação histórica removida da árvore atual

`owners-pets-service` pertence à implementação anterior, que extraía Owners e Pets juntos, e não integra esta V2. Seus fontes permanecem no histórico Git e na branch `tcc2-strangler-fig`; o diretório não é necessário para compilar, testar, executar ou medir os componentes atuais. A árvore ativa usa somente `owners-service`, Gateway e monólito. Documentos e evidências históricos continuam preservados.

Arquivos de build e configuração local de IDE permanecem ignorados. Logs acadêmicos dentro de `docs/metricas` são intencionalmente versionáveis, assim como CSVs, snapshots de fontes e proveniência.
