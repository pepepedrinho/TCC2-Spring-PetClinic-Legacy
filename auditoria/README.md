# Auditoria da coexistência Strangler Fig

O painel permite ao avaliador acompanhar a disponibilidade do Gateway, do monólito legado e do Owners Service, consultar o roteamento efetivo e observar o tráfego que passa pelo Gateway. O PetClinic continua acessível pela interface original.

- `dashboards/` preserva o protótipo Python anterior, com código, recursos estáticos, classificação, dependências e testes. Os 14 arquivos úteis foram conferidos por SHA-256 após a cópia. A origem local permanece na raiz do worktree principal. O ambiente virtual e os caches Python ficam na origem.
- `painel/` contém `index.html`, `style.css` e `app.js`. O painel usa HTML, CSS e JavaScript nativos.
- `coletor/server.mjs` usa Node.js 22 sem dependências npm. Serve os três arquivos estáticos e a leitura agregada `GET /api/observacao` na mesma origem.
- `coletor/teste_integracao.py` exercita o painel contra JVMs e bancos descartáveis. `Dockerfile` empacota o coletor e o painel.

## Fontes dos dados

| Componente | Versão | Porta administrativa padrão | Consultas do coletor |
| --- | --- | ---: | --- |
| Gateway | Boot/Actuator 3.5.16, Cloud 2025.0.3, Gateway WebFlux 4.3.5 | 9081 | `/actuator/health`, `/actuator/gateway/routes`, `/actuator/metrics`, `/actuator/metrics/spring.cloud.gateway.requests` |
| Monólito | Boot/Actuator 1.5.4.RELEASE, Java 8 | 9080 | `/manage/health` |
| Owners Service | Boot/Actuator 3.5.16, Java 17 | 9082 | `/actuator/health` |

Antes da intervenção, somente o monólito tinha Actuator. O prefixo `/manage` estava na porta de aplicação, com segurança de gerenciamento desabilitada. O perfil `auditoria` agora habilita somente sua saúde HTTP em porta separada. Gateway e Owners Service recebem o starter de Actuator, e a interface HTTP de gerenciamento dos três módulos fica desativada fora do perfil.

O Gateway expõe o endpoint `gateway` com `access=read-only` e usa o filtro de métricas nativo. O coletor lê `route_id`, o predicado de caminho e a origem do destino retornados pelo Gateway. A rota de bloqueio interno aparece como bloqueio local. As consultas ao temporizador usam as dimensões `routeId` e `httpStatusCode`. A latência média em milissegundos é calculada a partir de `TOTAL_TIME` em segundos dividido por `COUNT`. O painel agrega as contagens das rotas por destino configurado. No Compose de réplicas, esse destino é o HAProxy de Owners, e a saúde de Owners é a de uma instância resolvida pelo DNS do Compose. [API do Gateway 4.3](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html) · [Filtro de métricas](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/global-filters.html) · [Actuator 1.5.4](https://docs.spring.io/spring-boot/docs/1.5.4.RELEASE/reference/pdf/spring-boot-reference.pdf)

O temporizador passa a existir após a primeira amostra. Quando o índice contém o medidor de rotas e o temporizador ainda está ausente, o painel exibe zero requisições e média indisponível. Falhas de consulta e endpoints não configurados têm mensagens próprias. Os totais acumulam desde a partida da JVM. Como as consultas às diferentes dimensões ocorrem em paralelo enquanto há tráfego, somas parciais podem divergir momentaneamente do total. Requisições sem código HTTP numérico podem contribuir apenas para o total.

A saúde indica a resposta real do Actuator. O coletor retorna **Ativo** para `200 UP`, **Indisponível** em caso de falha de comunicação ou saúde negativa, e **Estado desconhecido ou monitoramento não configurado** para endpoints ausentes ou respostas sem um estado reconhecido.

## Executar com Docker Compose

A partir da raiz do worktree V3:

```powershell
docker compose -p tcc2-v3 up --build --wait
```

Abra [Auditoria](http://127.0.0.1:3000) e [PetClinic](http://127.0.0.1:8081). Para usar outra porta no host, defina `$env:AUDIT_PORT = '3001'` antes do comando. O painel e o coletor usam o mesmo contêiner e podem ser interrompidos com `docker compose -p tcc2-v3 stop auditoria`. Os serviços da aplicação continuam funcionando.

As portas administrativas ficam acessíveis pela rede `app` do Compose e sem publicação no host. O Gateway e a auditoria publicam suas portas de aplicação em loopback. A rede de aplicação deve conter apenas contêineres confiáveis, pois os listeners administrativos internos usam HTTP sem autenticação. O coletor aceita apenas os caminhos de leitura definidos e consulta exclusivamente as origens informadas pelo operador. As respostas omitem os detalhes de saúde, filtros, metadados e credenciais. Os listeners das aplicações usam portas distintas e o Gateway mantém as regras de roteamento existentes.

## Executar sem Docker

Instale Node.js 22, Java 8, Java 17 e Maven. Na raiz do worktree, defina `JAVA8_HOME` e `JAVA17_HOME` e execute `./scripts/build.ps1 -SkipGitMetadata`. Se `mvn.cmd` estiver fora do PATH, passe `-MavenHome`. Use um terminal para cada serviço, com as variáveis Java disponíveis em cada um:

```powershell
# Monólito. Para criar uma base local nova, defina LEGACY_DB_URL para
# um arquivo descartável e SPRING_DATASOURCE_INITIALIZE=true somente na primeira execução.
& "$env:JAVA8_HOME/bin/java.exe" -jar projects/monolith/target/spring-petclinic-1.5.1.jar --spring.profiles.active=production,auditoria

# Owners Service
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/owners-service/target/owners-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=auditoria

# Gateway
& "$env:JAVA17_HOME/bin/java.exe" -jar projects/gateway/target/petclinic-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=auditoria

# Painel e coletor
node auditoria/coletor/server.mjs
```

Os listeners administrativos e o coletor escutam em `127.0.0.1` por padrão. Use `AUDIT_GATEWAY_MANAGEMENT_PORT`, `AUDIT_LEGACY_MANAGEMENT_PORT` e `AUDIT_OWNERS_MANAGEMENT_PORT` nas respectivas JVMs para alterar portas. Passe as origens correspondentes ao coletor por `AUDIT_GATEWAY_URL`, `AUDIT_LEGACY_URL` e `AUDIT_OWNERS_URL`, no formato `http://host:porta`. A porta local do coletor usa `AUDIT_PORT`. Em Docker essa variável configura a porta publicada, enquanto o contêiner escuta em 3000.

`AUDIT_INTERVAL_MS` define a atualização do painel e a duração do cache compartilhado, com padrão de 15.000 ms e faixa de 5.000 a 300.000 ms. `AUDIT_TIMEOUT_MS` controla o timeout por consulta no coletor, com padrão de 2.000 ms e faixa de 250 a 10.000 ms. No Compose, o intervalo pode ser passado pelo ambiente do host. O cache une consultas concorrentes e o coletor só consulta as JVMs quando há acesso ao painel ou à API.

## Verificar o tráfego

```powershell
curl.exe -i http://127.0.0.1:8081/
curl.exe -i http://127.0.0.1:8081/api/owners/1
curl.exe -i http://127.0.0.1:8081/api/owners/9999999
curl.exe http://127.0.0.1:3000/api/observacao
```

Após o intervalo de atualização, compare as contagens nas rotas `petclinic-monolith` e `owners-service` e os códigos HTTP das respostas. Em JVMs locais, compare também com `curl.exe http://127.0.0.1:9081/actuator/metrics/spring.cloud.gateway.requests`. O coletor consulta somente endpoints de gerenciamento. Os healthchecks preexistentes do Compose incluem uma chamada a `/` no Gateway e contribuem para o total do monólito, mesmo sem navegação manual.

Chamadas diretas entre os serviços, como a consulta de Pets e Visits do Owners Service ao monólito, ficam fora do temporizador do Gateway. Também ficam fora desta etapa os percentis, as séries históricas e a identificação de réplicas individuais. A observação de chamadas internas pode ser avaliada futuramente com instrumentação fora dos fontes Java analisados.

## Validação e isolamento CK

```powershell
# Aceitação com JVMs locais e bases isoladas. As mensagens esperadas estão em inglês.
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US'
$env:SPRING_PROFILES_INCLUDE = 'auditoria'
python scripts/acceptance.py
Remove-Item Env:SPRING_PROFILES_INCLUDE, Env:JAVA_TOOL_OPTIONS

# Opcional para também executar a interface em Chrome ou Chromium headless
$env:AUDIT_BROWSER = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
python auditoria/coletor/teste_integracao.py

# Recoleta com o JAR CK já utilizado pelo TCC
./scripts/collect-ck.ps1 -V3 -WorkingTree -CkJar $ckJar
```

As saídas de produção V3 D e E correspondem aos cenários publicados B e C. Compare os CSVs por conteúdo após tornar os caminhos da coluna `file` relativos ao repositório, uniformizar o separador e ordenar as linhas. A CK inclui o caminho do snapshot no CSV bruto e a ordem pode variar. Consulte a [coleta vigente](../docs/metricas/README.md) e o [registro da validação](VALIDACAO.md).
