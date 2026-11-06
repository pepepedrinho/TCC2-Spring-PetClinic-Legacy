# Experimento Docker Compose · Strangler Fig V2

A execução básica usa cinco contêineres. O Gateway é o único serviço publicado no host, por padrão em `http://localhost:8081`.

| Serviço | Runtime e acesso interno | Dados |
| --- | --- | --- |
| `gateway` | Java 17, HTTP em `gateway:8081` | Sem banco |
| `monolith` | Java 8, HTTP em `monolith:8080` | JDBC para `legacy-db:9001/petclinic` |
| `owners-service` | Java 17, HTTP em `owners-service:8082` | JDBC para `owners-db:9092/./owners` |
| `legacy-db` | HSQLDB 2.3.5 em modo servidor HSQL | Volume nomeado `legacy-data` |
| `owners-db` | H2 2.3.232 em modo servidor TCP | Volume nomeado `owners-data` |

Manter os dois SGBDs preserva os drivers, o dialeto, os arquivos gerados pela migração e os mapeamentos JPA. A variante com bancos embarcados em volumes também preservaria o dialeto, mas um arquivo H2 aberto diretamente por duas JVMs não serve para as réplicas deste experimento. Cada servidor de banco abre seu próprio arquivo e atende aos clientes pela rede. MySQL e PostgreSQL exigiriam adaptar dialeto, migração e validação e ficaram fora do experimento.

## Iniciar, reiniciar e recriar

Use Docker com contêineres Linux e o plugin Compose. Execute na raiz da worktree experimental.

```sh
docker compose -p tcc2-v2 up --build --wait
docker compose -p tcc2-v2 ps
docker compose -p tcc2-v2 logs -f
```

Acesse [PetClinic pelo Gateway](http://localhost:8081). `GATEWAY_PORT` altera a porta local. As imagens de base e a do HAProxy estão fixadas por digest. As aplicações executam testes Maven no build e separam o JDK de compilação da imagem com JRE. Os healthchecks fazem consultas JDBC aos bancos ou requisições HTTP às aplicações. O Compose aguarda a prontidão inicial dos serviços dependentes.

É possível parar, iniciar ou reiniciar cada componente individualmente. Por exemplo:

```sh
docker compose -p tcc2-v2 stop owners-service
docker compose -p tcc2-v2 start owners-service
docker compose -p tcc2-v2 restart monolith
docker compose -p tcc2-v2 restart gateway
docker compose -p tcc2-v2 up --force-recreate --wait
docker compose -p tcc2-v2 down
docker compose -p tcc2-v2 up --wait
```

Use o mesmo nome de projeto para reabrir os mesmos volumes. Para uma instalação limpa, escolha um nome novo. `down -v` remove os volumes do projeto e apaga os bancos armazenados neles. Use-o apenas se os volumes forem descartáveis e você tiver identificado o projeto correto.

## Provisionamento e recuperação

A entrada dos dois contêineres de banco prepara os arquivos antes de abrir a porta TCP. Em um volume vazio, executa o SQL canônico do componente, verifica o esquema esperado e grava `.provisioned-v1` após a validação. No HSQLDB, aplica `SET FILES WRITE DELAY 0` antes de abrir o servidor em cada partida, inclusive com um banco existente. Em um volume marcado, verifica o esquema e pula a carga de dados. O Hibernate continua com `ddl-auto=none`. O monólito e as réplicas de Owners no Compose não executam carga inicial.

Se houver arquivos de banco sem marcador, o serviço falha antes da carga e preserva os arquivos. Isso inclui uma interrupção entre a criação dos dados e a gravação do marcador. Examine os logs e preserve uma cópia do volume para recuperação. A checagem detecta tabelas e colunas ausentes e a presença indevida de Owners no legado. Ela não é uma auditoria geral de integridade nem recupera automaticamente um provisionamento parcial.

Os bancos só ficam nas redes privadas de dados. O HSQLDB usa o usuário `SA` sem senha da fixture histórica, e o H2 aceita `OWNERS_DB_PASSWORD` para um volume novo. Mantenha a mesma senha nas próximas partidas. Os protocolos internos não usam TLS e as credenciais padrão são para laboratório local. O isolamento depende do controle de acesso ao host e ao Docker. Nenhuma porta de banco é publicada.

## Importar a migração offline

Use o [script de migração existente](scripts/migrate-owners.ps1) com o banco de origem encerrado e um diretório de destino novo. O migrador abre a origem em modo somente leitura, mantém IDs e vínculos e escreve `migration-complete.txt` no destino. A importação abaixo abre os arquivos copiados com as mesmas versões de banco do migrador.

Com um projeto novo e antes de executar `up`, faça a importação para os dois volumes. Em PowerShell, use:

```powershell
$destino = (Resolve-Path 'C:/caminho/para/destino-da-migracao').Path
docker compose -p tcc2-migrado build
docker compose -p tcc2-migrado run --rm -T --no-deps -v "${destino}:/import:ro" legacy-db import
docker compose -p tcc2-migrado run --rm -T --no-deps -v "${destino}:/import:ro" owners-db import
docker compose -p tcc2-migrado up --wait
```

Confirme o sucesso dos dois imports antes de iniciar. A origem montada é somente leitura, a importação recusa volumes já ocupados e a carga de demonstração fica desabilitada nos bancos importados. Use a senha vazia para os arquivos H2 produzidos pelo migrador atual.

## Duas réplicas de Owners

Use o complemento no mesmo projeto se quiser reaproveitar os volumes, ou escolha outro projeto e outra porta para testar em paralelo.

```sh
docker compose -p tcc2-v2 -f compose.yaml -f compose.replicas.yaml up --wait
docker compose -p tcc2-v2 -f compose.yaml -f compose.replicas.yaml ps
replica=$(docker compose -p tcc2-v2 -f compose.yaml -f compose.replicas.yaml ps -q owners-service | head -n 1)
docker stop "$replica"
docker start "$replica"
```

O complemento acrescenta uma segunda réplica e um HAProxy, chegando a sete contêineres. O Gateway e o monólito passam a usar o HAProxy. Ele resolve as réplicas via DNS interno, verifica a prontidão via HTTP e distribui requisições em round-robin. O cabeçalho `X-Owners-Replica` identifica o slot que respondeu à chamada REST. O balanceador não repete requisições de escrita.

As réplicas compartilham o mesmo H2 via TCP. Há um servidor de banco, um volume de Owners e um balanceador. A perda de uma réplica pode causar falhas transitórias até a detecção pelo healthcheck. A perda do banco interrompe o acesso a Owners. Este cenário não replica o H2, não resolve concorrência de edições simultâneas além das garantias do banco e não demonstra alta disponibilidade do sistema completo.

## Verificação funcional

A aceitação padrão pode usar um projeto Compose básico **descartável**, recém-inicializado. O teste cria e altera registros e para serviços desse projeto.

```powershell
$env:GATEWAY_PORT = '18081'
$env:TCC2_COMPOSE_PROJECT = 'tcc2-teste-compose'
docker compose -p $env:TCC2_COMPOSE_PROJECT up --build --wait
python scripts/acceptance.py
```

O roteiro acessa as rotas pelo Gateway e reinicia os serviços de aplicação, mantendo os bancos ativos. O arquivo `.work/acceptance-*/http-acceptance.json` registra as verificações. A fixture offline pode ser criada e validada por `python scripts/test-migration.py` com Java 17 e dependências JDBC no cache Maven. Os builds Docker executam as suítes Java dos três projetos.
