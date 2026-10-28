# Resultados CK V2 — extração exclusiva de Owners

A baseline preservada e a recoleta produziram valores identicos para TODOS os campos, exceto caminhos absolutos dos arquivos (24 tipos). Isso sustenta a compatibilidade do JAR/parametros reconstruidos com a coleta historica, cuja branch nao versionou o comando original. Nao prova qual comando foi usado no passado.

Incluidos todos os tipos reportados pelo CK em src/main/java de cada componente (interfaces e classes; package-info nao gera tipo). Excluidos testes, scripts, dependencias compiladas e fontes geradas. Sem excluir DTOs, adaptadores ou classes com metricas zero. Mesma ferramenta e argumentos false 0 false para todos. LCOM* corresponde a coluna literal lcom*. Media aritmetica por tipo; estatisticas populacionais adicionais em CSV. Nao e cobertura de testes.

## Tabela 1 — Metricas gerais

| conjunto | tipos_CK | cbo | wmc | rfc | lcom | lcom* |
| --- | --- | --- | --- | --- | --- | --- |
| A baseline | 24 | 6.666667 | 4.916667 | 3.458333 | 5.875 | 0.256618 |
| B monolito | 30 | 6.166667 | 4.866667 | 4.366667 | 5.266667 | 0.227258 |
| C owners | 4 | 7.25 | 5.0 | 2.75 | 3.5 | 0.21875 |
| Gateway (separado) | 1 | 2.0 | 1.0 | 1.0 | 0.0 | 0.0 |

## Tabela 2 — Classes historicas equivalentes

| classe_historica | situacao | A | B | limite |
| --- | --- | --- | --- | --- |
| org.springframework.samples.petclinic.owner.Pet | modificada | cbo=15, wmc=11, rfc=6, lcom=37, lcom*=0.8 | cbo=15, wmc=12, rfc=6, lcom=45, lcom*=0.8181818 |  |
| org.springframework.samples.petclinic.owner.VisitController | modificada | cbo=14, wmc=6, rfc=6, lcom=6, lcom*=0.6 | cbo=16, wmc=8, rfc=11, lcom=6, lcom*=0.6 |  |
| org.springframework.samples.petclinic.system.CacheConfig | mantida | cbo=7, wmc=2, rfc=3, lcom=1, lcom*=0.0 | cbo=7, wmc=2, rfc=3, lcom=1, lcom*=0.0 |  |
| org.springframework.samples.petclinic.visit.Visit | mantida | cbo=7, wmc=7, rfc=0, lcom=11, lcom*=0.6666667 | cbo=7, wmc=7, rfc=0, lcom=11, lcom*=0.6666667 |  |
| org.springframework.samples.petclinic.model.BaseEntity | mantida | cbo=3, wmc=4, rfc=0, lcom=0, lcom*=0.0 | cbo=3, wmc=4, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.PetClinicApplication | mantida | cbo=2, wmc=1, rfc=1, lcom=0, lcom*=0.0 | cbo=2, wmc=1, rfc=1, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.system.CrashController | mantida | cbo=2, wmc=1, rfc=0, lcom=0, lcom*=0.0 | cbo=2, wmc=1, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.model.NamedEntity | mantida | cbo=4, wmc=3, rfc=1, lcom=1, lcom*=0.33333334 | cbo=4, wmc=3, rfc=1, lcom=1, lcom*=0.33333334 |  |
| org.springframework.samples.petclinic.vet.Vets | mantida | cbo=3, wmc=2, rfc=0, lcom=0, lcom*=0.0 | cbo=3, wmc=2, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.vet.VetRepository | mantida | cbo=4, wmc=1, rfc=0, lcom=0, lcom*=0.0 | cbo=4, wmc=1, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.owner.OwnerRepository | substituida: adaptador HTTP/composicao | cbo=5, wmc=3, rfc=0, lcom=3, lcom*=0.0 | cbo=6, wmc=7, rfc=8, lcom=0, lcom*=0.4 | mesmo nome, papel diferente; nao equivalencia de negocio |
| org.springframework.samples.petclinic.system.WelcomeController | mantida | cbo=2, wmc=1, rfc=0, lcom=0, lcom*=0.0 | cbo=2, wmc=1, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.vet.Specialty | mantida | cbo=3, wmc=0, rfc=0, lcom=0, lcom*=0.0 | cbo=3, wmc=0, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.owner.Owner | substituida: representacao MVC | cbo=13, wmc=19, rfc=15, lcom=58, lcom*=0.78846157 | cbo=9, wmc=19, rfc=16, lcom=58, lcom*=0.78846157 | mesmo nome, papel diferente; nao equivalencia de negocio |
| org.springframework.samples.petclinic.model.Person | mantida | cbo=4, wmc=4, rfc=0, lcom=2, lcom*=0.5 | cbo=4, wmc=4, rfc=0, lcom=2, lcom*=0.5 |  |
| org.springframework.samples.petclinic.owner.PetType | mantida | cbo=3, wmc=0, rfc=0, lcom=0, lcom*=0.0 | cbo=3, wmc=0, rfc=0, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.owner.PetTypeFormatter | mantida | cbo=6, wmc=5, rfc=3, lcom=1, lcom*=0.33333334 | cbo=6, wmc=5, rfc=3, lcom=1, lcom*=0.33333334 |  |
| org.springframework.samples.petclinic.vet.VetController | mantida | cbo=6, wmc=3, rfc=4, lcom=0, lcom*=0.0 | cbo=6, wmc=3, rfc=4, lcom=0, lcom*=0.0 |  |
| org.springframework.samples.petclinic.owner.PetController | modificada | cbo=20, wmc=14, rfc=15, lcom=8, lcom*=0.5925926 | cbo=22, wmc=17, rfc=21, lcom=17, lcom*=0.6333334 |  |
| org.springframework.samples.petclinic.owner.PetValidator | mantida | cbo=5, wmc=6, rfc=7, lcom=1, lcom*=0.5 | cbo=5, wmc=6, rfc=7, lcom=1, lcom*=0.5 |  |
| org.springframework.samples.petclinic.owner.OwnerController | mantida | cbo=15, wmc=14, rfc=17, lcom=0, lcom*=0.44444448 | cbo=15, wmc=14, rfc=17, lcom=0, lcom*=0.44444448 |  |
| org.springframework.samples.petclinic.visit.VisitRepository | mantida | cbo=2, wmc=2, rfc=0, lcom=1, lcom*=0.0 | cbo=2, wmc=2, rfc=0, lcom=1, lcom*=0.0 |  |
| org.springframework.samples.petclinic.owner.PetRepository | modificada | cbo=5, wmc=3, rfc=0, lcom=3, lcom*=0.0 | cbo=5, wmc=4, rfc=0, lcom=6, lcom*=0.0 |  |
| org.springframework.samples.petclinic.vet.Vet | mantida | cbo=10, wmc=6, rfc=5, lcom=8, lcom*=0.6 | cbo=10, wmc=6, rfc=5, lcom=8, lcom*=0.6 |  |

## Tabela 3 — Extracao funcional

Valores de classe sao contexto, nao metricas dos metodos isolados. As linhas repetem classes quando uma responsabilidade foi repartida; nao somar estas linhas. method.csv preserva a granularidade de metodos. OwnerController MVC continua no legado; o REST nao substitui a renderizacao. OwnerRepository legado agora e integracao, apesar do nome historico.

| origem | metodos_ou_campos | destino | equivalente_funcional | A_classe_contexto | C_classe_contexto | limite |
| --- | --- | --- | --- | --- | --- | --- |
| Owner | address/city/telephone; heranca firstName/lastName/id | br.edu.tcc.owners.Owner | campos, getters/setters, updateFrom | cbo=13, wmc=19, rfc=15, lcom=58, lcom*=0.78846157 | cbo=9, wmc=12, rfc=0, lcom=14, lcom*=0.7083333 | somente cadastro: pets/getPet/addPet permanecem no legado |
| OwnerRepository | findByLastName | br.edu.tcc.owners.OwnerRepository | findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc | cbo=5, wmc=3, rfc=0, lcom=3, lcom*=0.0 | cbo=2, wmc=1, rfc=0, lcom=0, lcom*=0.0 | responsabilidade de pesquisa; interface derivada diferente |
| OwnerRepository | findById/save | br.edu.tcc.owners.OwnerController | get/create/update | cbo=5, wmc=3, rfc=0, lcom=3, lcom*=0.0 | cbo=16, wmc=6, rfc=10, lcom=0, lcom*=0.16666667 | divisao REST + JPA; nao equivalencia um-para-um de classe |
| OwnerController | processCreationForm/processUpdateOwnerForm/processFindForm/showOwner | br.edu.tcc.owners.OwnerController | create/update/search/get | cbo=15, wmc=14, rfc=17, lcom=0, lcom*=0.44444448 | cbo=16, wmc=6, rfc=10, lcom=0, lcom*=0.16666667 | MVC permanece no monolito; somente operacoes autoritativas extraidas |
| Person | firstName/lastName | br.edu.tcc.owners.Owner | campos e acesso | cbo=4, wmc=4, rfc=0, lcom=2, lcom*=0.5 | cbo=9, wmc=12, rfc=0, lcom=14, lcom*=0.7083333 | Vet ainda herda Person no legado; classe inteira nao migrou |
| BaseEntity | id/isNew | br.edu.tcc.owners.Owner | id/getId + identidade JPA | cbo=3, wmc=4, rfc=0, lcom=0, lcom*=0.0 | cbo=9, wmc=12, rfc=0, lcom=14, lcom*=0.7083333 | base compartilhada permanece; somente identidade de Owner transferida |

## Tabela 4 — Codigo de integracao

Estas classes NAO sao negocio migrado. Owner e representacao MVC adaptada; LegacyDatabaseInitializer e infraestrutura de inicializacao, contabilizada no monolito mas fora desta tabela HTTP.

| classe | papel | cbo | wmc | rfc | lcom | lcom* |
| --- | --- | --- | --- | --- | --- | --- |
| org.springframework.samples.petclinic.integration.OwnerValidationException | 400 remoto | 1 | 0 | 0 | 0 | 0.0 |
| org.springframework.samples.petclinic.integration.OwnersClient | HTTP, contrato de cadastro, encoding, timeouts | 13 | 12 | 21 | 0 | 0.2 |
| org.springframework.samples.petclinic.integration.IntegrationErrorHandler | HTTP 503 e view de indisponibilidade | 4 | 1 | 0 | 0 | 0.0 |
| org.springframework.samples.petclinic.integration.OwnersUnavailableException | falha de rede/servidor/contrato | 0 | 1 | 0 | 0 | 0.0 |
| org.springframework.samples.petclinic.owner.OwnerRepository | adaptacao HTTP e composicao com Pets locais | 6 | 7 | 8 | 0 | 0.4 |
| org.springframework.samples.petclinic.integration.OwnerNotFoundException | 404 e vinculo incorreto | 1 | 0 | 0 | 0 | 0.0 |

## Tabela 5 — Arquitetura completa

Somatorios por componente. CBO/RFC somados podem contar referencias repetidas. LCOM* normalizado nao tem soma interpretavel; nao e agregado. Nao se define um indice global de qualidade. A baseline e uma alternativa historica, nao um componente da arquitetura V2.

| componente | tipos_CK | cbo | wmc | rfc | lcom | soma_lcom* |
| --- | --- | --- | --- | --- | --- | --- |
| A baseline | 24 | 160.0 | 118.0 | 83.0 | 141.0 | nao interpretada (normalizada) |
| B monolito | 30 | 185.0 | 146.0 | 131.0 | 158.0 | nao interpretada (normalizada) |
| C owners | 4 | 29.0 | 20.0 | 11.0 | 14.0 | nao interpretada (normalizada) |
| Gateway (separado) | 1 | 2.0 | 1.0 | 1.0 | 0.0 | nao interpretada (normalizada) |
| V2 completa (B+C+Gateway) | 35 | 216.0 | 167.0 | 143.0 | 172.0 | nao interpretada (normalizada) |

## Interpretacao

- cbo: A=6.666667, B=6.166667, C=7.25. Variacao B/A: -7.50%.
- wmc: A=4.916667, B=4.866667, C=5.0. Variacao B/A: -1.02%.
- rfc: A=3.458333, B=4.366667, C=2.75. Variacao B/A: 26.27%.
- lcom: A=5.875, B=5.266667, C=3.5. Variacao B/A: -10.35%.
- lcom*: A=0.256618, B=0.227258, C=0.21875. Variacao B/A: -11.44%.

As medias descrevem populacoes de tamanhos e papeis diferentes. A adaptacao adiciona comunicacao HTTP, composicao e tratamento de falhas, enquanto a logica cadastral passa ao servico. LCOM e LCOM* sao sensiveis a getters/setters e campos: valores altos em representacoes e entidades nao bastam para inferir responsabilidades indevidas. CBO estatico nao mede dependencia operacional, latencia HTTP, consistencia distribuida ou disponibilidade. Os resultados nao demonstram melhoria uniforme de acoplamento, coesao ou complexidade nem causalidade entre Strangler Fig e qualidade global.

Proveniencia: proveniencia.json identifica HEAD no momento da coleta; fontes-avaliadas.json identifica cada arquivo efetivamente analisado, inclusive se havia alteracoes locais. O commit posterior de evidencias nao altera essas fontes. CSVs brutos nao foram normalizados ou reescritos. A baseline original em docs/baseline permanece intacta.
