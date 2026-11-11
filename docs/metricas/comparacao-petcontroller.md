# Efeito da incorporação de OwnerPetsController ao PetController

A V3 anterior corresponde ao commit `26fafc5ed169598f09e322bfd992ebb735afd325`, identificado pela tag `tcc2-v3-pre-petcontroller`. A intervenção moveu a leitura em lote de Pets e Visits para o `PetController`. As rotas MVC receberam os caminhos completos e o modelo dos formulários só é preenchido nas rotas com `ownerId`. A chamada interna consulta apenas o repositório de Pets.

A CK `0.7.1-SNAPSHOT`, fonte `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`, foi executada com Java 17.0.12 e `false 0 false`. O coletor existente usou `-V3` para o HEAD confirmado pela tag e `-V3 -WorkingTree` para a intervenção. As raízes Java de produção, o hash do JAR e os hashes dos CSVs estão nas proveniências. A comparação ignora caminho e ordem das linhas. [Coleta anterior](../historico/v3-pre-petcontroller/proveniencia.json) · [Coleta experimental](proveniencia.json).

## População analisada

| Cenário | Tipos CK | Métodos CK |
| --- | ---: | ---: |
| A original | 24 | 91 |
| B anterior | 28 | 95 |
| B experimental | 27 | 93 |
| C anterior | 8 | 32 |
| C experimental | 8 | 32 |

B perdeu um tipo (-3,5714%). A CK ainda emite duas linhas para `PetSummary` e `VisitSummary`, agora aninhadas em `PetController`. A população inclui interfaces e classes internas. A e C reproduziram todos os valores CK de tipos e métodos. Os dois CSVs publicados de C também coincidiram byte a byte com os arquivados.

## Totais por cenário

| Cenário | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: |
| A original | 160 | 118 | 83 | 141 | 6,1588 |
| B anterior | 172 | 130 | 126 | 166 | 6,1066 |
| B experimental | 167 | 129 | 126 | 167 | 5,7763 |
| C anterior | 50 | 40 | 41 | 82 | 1,5111 |
| C experimental | 50 | 40 | 41 | 82 | 1,5111 |
| B experimental − B anterior | -5 | -1 | 0 | +1 | -0,3303 |
| B experimental − B anterior (%) | -2,907 | -0,7692 | 0 | +0,6024 | -5,4089 |
| B anterior − A | +12 | +12 | +43 | +25 | -0,0522 |
| B anterior − A (%) | +7,5 | +10,1695 | +51,8072 | +17,7305 | -0,8474 |
| B experimental − A | +7 | +11 | +43 | +26 | -0,3825 |
| B experimental − A (%) | +4,375 | +9,322 | +51,8072 | +18,4397 | -6,2105 |
| C experimental − C anterior | 0 | 0 | 0 | 0 | 0 |
| C experimental − C anterior (%) | 0 | 0 | 0 | 0 | 0 |

## Médias por tipo

| Cenário | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: |
| A original | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B anterior | 6,1429 | 4,6429 | 4,5 | 5,9286 | 0,2181 |
| B experimental | 6,1852 | 4,7778 | 4,6667 | 6,1852 | 0,2139 |
| C anterior | 6,25 | 5 | 5,125 | 10,25 | 0,1889 |
| C experimental | 6,25 | 5 | 5,125 | 10,25 | 0,1889 |
| B experimental − B anterior | +0,0423 | +0,1349 | +0,1667 | +0,2566 | -0,0042 |
| B experimental − B anterior (%) | +0,6891 | +2,906 | +3,7037 | +4,3284 | -1,9055 |
| B anterior − A | -0,5238 | -0,2738 | +1,0417 | +0,0536 | -0,0385 |
| B anterior − A (%) | -7,8571 | -5,569 | +30,1205 | +0,9119 | -15,012 |
| B experimental − A | -0,4815 | -0,1389 | +1,2083 | +0,3102 | -0,0427 |
| B experimental − A (%) | -7,2222 | -2,8249 | +34,9398 | +5,2797 | -16,6315 |
| C experimental − C anterior | 0 | 0 | 0 | 0 | 0 |
| C experimental − C anterior (%) | 0 | 0 | 0 | 0 | 0 |

## Médias dos 22 tipos equivalentes por nome qualificado

| Cenário | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: |
| A original | 6,3636 | 4,5909 | 3 | 6,2727 | 0,2597 |
| B anterior | 6,5 | 4,9091 | 3,6818 | 7,3636 | 0,2624 |
| B experimental | 6,5455 | 5,0909 | 4,5909 | 7,4545 | 0,2626 |
| B experimental − B anterior | +0,0455 | +0,1818 | +0,9091 | +0,0909 | +0,0001 |
| B experimental − B anterior (%) | +0,6993 | +3,7037 | +24,6914 | +1,2346 | +0,0525 |

## Tipos equivalentes individualmente

Cada célula segue a ordem CBO / WMC / RFC / LCOM / LCOM*. As duas últimas colunas dão a diferença da versão experimental em relação a A e a B anterior.

| Tipo | A original | B anterior | B experimental | Δ vs. A | Δ vs. B anterior |
| --- | --- | --- | --- | --- | --- |
| `BaseEntity` | 3 / 4 / 0 / 0 / 0 | 3 / 4 / 0 / 0 / 0 | 3 / 4 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `CacheConfig` | 7 / 2 / 3 / 1 / 0 | 7 / 2 / 3 / 1 / 0 | 7 / 2 / 3 / 1 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `CrashController` | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `NamedEntity` | 4 / 3 / 1 / 1 / 0,3333 | 4 / 3 / 1 / 1 / 0,3333 | 4 / 3 / 1 / 1 / 0,3333 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Owner` | 13 / 19 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 | -4 / -1 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Person` | 4 / 4 / 0 / 2 / 0,5 | 4 / 4 / 0 / 2 / 0,5 | 4 / 4 / 0 / 2 / 0,5 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Pet` | 15 / 11 / 6 / 37 / 0,8 | 15 / 12 / 6 / 45 / 0,8182 | 15 / 12 / 6 / 45 / 0,8182 | 0 / +1 / 0 / +8 / +0,0182 | 0 / 0 / 0 / 0 / 0 |
| `PetClinicApplication` | 2 / 1 / 1 / 0 / 0 | 2 / 1 / 1 / 0 / 0 | 2 / 1 / 1 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `PetController` | 20 / 14 / 15 / 8 / 0,5926 | 23 / 17 / 24 / 17 / 0,6333 | 24 / 21 / 44 / 19 / 0,6364 | +4 / +7 / +29 / +11 / +0,0438 | +1 / +4 / +20 / +2 / +0,003 |
| `PetRepository` | 5 / 3 / 0 / 3 / 0 | 6 / 5 / 0 / 10 / 0 | 6 / 5 / 0 / 10 / 0 | +1 / +2 / 0 / +7 / 0 | 0 / 0 / 0 / 0 / 0 |
| `PetType` | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `PetTypeFormatter` | 6 / 5 / 3 / 1 / 0,3333 | 6 / 5 / 3 / 1 / 0,3333 | 6 / 5 / 3 / 1 / 0,3333 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `PetValidator` | 5 / 6 / 7 / 1 / 0,5 | 5 / 6 / 7 / 1 / 0,5 | 5 / 6 / 7 / 1 / 0,5 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Specialty` | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 3 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Vet` | 10 / 6 / 5 / 8 / 0,6 | 10 / 6 / 5 / 8 / 0,6 | 10 / 6 / 5 / 8 / 0,6 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `VetController` | 6 / 3 / 4 / 0 / 0 | 6 / 3 / 4 / 0 / 0 | 6 / 3 / 4 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `VetRepository` | 4 / 1 / 0 / 0 / 0 | 4 / 1 / 0 / 0 / 0 | 4 / 1 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Vets` | 3 / 2 / 0 / 0 / 0 | 3 / 2 / 0 / 0 / 0 | 3 / 2 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `Visit` | 7 / 7 / 0 / 11 / 0,6667 | 7 / 7 / 0 / 11 / 0,6667 | 7 / 7 / 0 / 11 / 0,6667 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `VisitController` | 14 / 6 / 6 / 6 / 0,6 | 17 / 8 / 12 / 6 / 0,6 | 17 / 8 / 12 / 6 / 0,6 | +3 / +2 / +6 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `VisitRepository` | 2 / 2 / 0 / 1 / 0 | 2 / 2 / 0 / 1 / 0 | 2 / 2 / 0 / 1 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |
| `WelcomeController` | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 2 / 1 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 / 0 |

## Tipos adicionais em relação ao original

Cada célula segue a ordem CBO / WMC / RFC / LCOM / LCOM*. Os dois tipos internos foram pareados pela função que exercem no contrato, apesar da mudança do nome qualificado.

| Tipo | B anterior | B experimental |
| --- | --- | --- |
| `IntegrationErrorHandler` | 7 / 8 / 5 / 3 / 0 | 7 / 8 / 5 / 3 / 0 |
| `IntegrationFailure` | 1 / 2 / 1 / 0 / 0 | 1 / 2 / 1 / 0 / 0 |
| `OwnerPetsController` | 6 / 5 / 20 / 1 / 0,3333 | — |
| `OwnersServiceHttpClient` | 8 / 5 / 6 / 0 / 0 | 8 / 5 / 6 / 0 / 0 |
| `PetSummary` | 5 / 1 / 10 / 0 / 0 | 5 / 1 / 10 / 0 / 0 |
| `VisitSummary` | 2 / 1 / 3 / 0 / 0 | 2 / 1 / 3 / 0 / 0 |

Totais dos tipos adicionais em B anterior foram 29 / 22 / 45 / 4 / 0,3333. Em B experimental foram 23 / 17 / 25 / 3 / 0.

## Interpretação

O `PetController` passou de WMC 17 para 21 e de RFC 24 para 44. Foi o único tipo comum a A e B que mudou em alguma das cinco métricas entre as duas versões de B. A consolidação do carregamento dos atributos MVC também entra nessa medição, pois o endpoint interno precisa funcionar sem carregar o formulário nem consultar o Owners Service.

No monólito, o WMC total passou de 130 para 129 e o RFC total ficou em 126. Antes da incorporação, os dois controladores somavam WMC 22 e RFC 44. O controlador resultante tem WMC 21 e RFC 44. Entre os 22 tipos equivalentes, o ganho de 4 pontos de WMC e 20 de RFC no `PetController` elevou as médias em 0,1818 e 0,9091, respectivamente. A remoção do tipo também alterou o denominador das médias gerais.

## Testes Maven

| Módulo | Aprovados | Ignorados | Falhas | Erros |
| --- | ---: | ---: | ---: | ---: |
| Monólito | 51 | 1 | 0 | 0 |
| Owners Service | 12 | 0 | 0 | 0 |
| Gateway | 1 | 0 | 0 | 0 |

O monólito executou o Maven Wrapper com Java 8, via `scripts/build.ps1 -Offline -SkipGitMetadata`. Os demais módulos executaram `mvn -B -o clean package` com Java 17 e acesso ao loopback após o bloqueio do sandbox na primeira tentativa do Owners Service. O teste já ignorado é `CrashControllerTests`.

Os três testes de regressão em `PetControllerTests` verificam a resposta JSON de múltiplos Owners em uma consulta, com ordenação, desempate por ID, datas, Owner sem Pets e Pets sem Visits. Também cobrem lote vazio sem consulta e rejeição da chamada com o cabeçalho do Gateway.

## Aceitação HTTP

O roteiro `python scripts/acceptance.py` completou 79 verificações em JVMs locais isoladas, com bancos descartáveis e o Gateway nas rotas públicas. A execução cobriu buscas, cadastro e edição de Owners, leitura, criação e edição de Pets, criação de Visits, bloqueio da API interna, persistência após reinício e indisponibilidade nas duas direções.

O Docker Engine estava indisponível. Foi usado o modo local já oferecido pelo roteiro, com `JAVA_TOOL_OPTIONS=-Duser.language=en -Duser.country=US` para reproduzir as mensagens em inglês verificadas pelo teste. O relatório JSON da execução fica em `.work/acceptance-20261110-213929/http-acceptance.json` no worktree V3.
