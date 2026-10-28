# TCC II — Strangler Fig V2: Owners

Branch `tcc2-strangler-fig-v2`, derivada do monólito de 2017 (`c36452a2c34443ae26b4ecbba4f149906af14717`). Apenas o cadastro de proprietários foi extraído. Pets, PetTypes, Visits e Vets continuam no legado Java 8/Spring Boot 1.5.4.RELEASE com Thymeleaf e HSQLDB.

| Componente | Tecnologia | Porta | Dados |
| --- | --- | --- | --- |
| Legado | Java 8 / Boot 1.5.4 | 8080 | Pets, tipos, visitas, veterinários — HSQLDB |
| Gateway reutilizado | Java 17 / Boot 3.5.16 / Cloud 2025.0.3 WebFlux | 8081 | sem banco |
| Owners | Java 17 / Boot 3.5.16 | 8082 | somente proprietários — H2 |

O Gateway encaminha `/api/owners` e `/api/owners/**` ao Owners. O restante, inclusive as páginas `/owners/**`, vai ao legado. Acesse [PetClinic pelo Gateway](http://localhost:8081). Não há API Pets no microsserviço.

## Executar no PowerShell

Na raiz do repositório, com JDK 8, JDK 17 e Maven 3.9.11 instalados:

```powershell
# Os parâmetros podem ser alterados para suas instalações.
./scripts/build.ps1 -Offline
# Remova -Offline se precisar baixar dependências.
```

Inicie cada comando abaixo em um terminal separado, todos na raiz:

```powershell
# Terminal 1 — Owners
& 'C:\Program Files\Java\jdk-17\bin\java.exe' -jar owners-service/target/owners-service-1.0.0-SNAPSHOT.jar
# Terminal 2 — legado
& 'C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\java.exe' -jar target/spring-petclinic-1.5.1.jar
# Terminal 3 — Gateway
& 'C:\Program Files\Java\jdk-17\bin\java.exe' -jar gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar
```

A primeira inicialização cria bancos V2 separados em `data/v2/petclinic.*` e `data/owners-v2.mv.db`, mantendo IDs históricos. Reinícios não apagam nem recarregam registros existentes. Os bancos da implementação anterior não são usados. Encerre com Ctrl+C. Não use o perfil MySQL histórico nesta V2.

## Validar e medir

Com as portas 8080–8082 livres (os testes iniciam e encerram suas próprias JVMs):

```powershell
python scripts/acceptance.py
python scripts/test-migration.py
./scripts/collect-ck.ps1 -CkJar 'C:\TCC2\ferramentas\ck\target\ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar' -Java17Home 'C:\Program Files\Java\jdk-17'
```

Os testes HTTP usam bancos novos em `.work/`, verificam formulários, relacionamento Owner–Pet–Visit, persistência após reinício forçado, 503 e Vets independente. `test-migration.py` usa uma base original sintética e verifica os hashes da origem. Python 3 (biblioteca padrão) é suficiente. O Maven Wrapper histórico permanece em 3.3.3; para os módulos Boot 3 use Maven 3.9.11 via script.

## Evidências da entrega

- Legado: **46 passaram, 0 falhas, 0 erros, 1 ignorado** (47 contabilizados pelo Surefire).
- Owners: **6 passaram**; Gateway: **1 passou**. O ignorado é `CrashControllerTests`, já desabilitado na baseline.
- Aceitação HTTP e migração offline: **PASS**, com evidências em `docs/metricas/v2/evidencias/`. Isso não representa cobertura percentual.
- CK efetivamente executado: baseline **24 tipos**, legado **30**, Owners **4**, Gateway **1**; os valores completos e cinco tabelas estão no relatório abaixo.

Leia [a documentação V2](docs/v2/implementacao.md) para fronteiras, mapas de classes, integridade, migração e limitações; [o relatório CK](docs/metricas/v2/relatorio.md) para valores e interpretação. O [README original](docs/historico/README-original-2017.md) foi preservado. Nenhum push remoto faz parte desta entrega.
