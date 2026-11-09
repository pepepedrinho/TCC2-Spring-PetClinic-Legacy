# V3 final · Arquitetura, validação e métricas

A V3 migra a responsabilidade Owner para um serviço separado. O Gateway encaminha a API REST `/api/owners` e as páginas de pesquisa, detalhe, criação e edição de Owner ao Owners Service. Rotas aninhadas de Pets e Visits, Vets e recursos estáticos seguem para o monólito. O [README principal](../../README.md) mostra o diagrama. O [guia Docker](../../DOCKER.md) descreve a execução e o provisionamento.

## Componentes e integrações

| Fluxo | Implementação |
| --- | --- |
| Cadastro de Owner | `br.edu.tcc.OwnersApplication` descobre `br.edu.tcc.owners.OwnerController`, que reúne as ações MVC e REST. `OwnerRepository` acessa o H2 por JPA. |
| Páginas compostas de Owner | `OwnerController` usa `br.edu.tcc.integration.LegacyPetsHttpClient` para consultar Pets e Visits em lote por `POST /internal/owner-pets`. O Gateway bloqueia o prefixo interno. A resposta é agrupada por ID de Owner. |
| Formulários legados | `PetController` e `VisitController` usam `org.springframework.samples.petclinic.integration.OwnersServiceHttpClient` para consultar `GET /api/owners/{id}`. Pets e Visits continuam no HSQLDB, assim como Vets. |

Cada requisição que precisa de dados remotos faz uma consulta em uma direção. A aplicação chamada responde com seus próprios dados. O fluxo de escrita de Owner é local ao Owners Service e a API conserva o 409 ao tentar excluir um Owner existente, preservando referências legadas.

O `IntegrationErrorHandler` do monólito centraliza os erros 400, 404 e 503 dos fluxos legados e renderiza `ownersUnavailable` na indisponibilidade. No Owners Service, `LegacyPetsHttpClient` lança `ResponseStatusException` com 503 e o motivo `Pets and visits are unavailable` quando a leitura do legado falha. As páginas de detalhe e de múltiplos resultados dependem dessa leitura. O formulário de Owner e a API REST têm acesso local. Se o Owners Service ficar indisponível, o Gateway responde 503 nas suas rotas e os formulários legados que o consultam também recebem 503.

## Validação de finalização

| Módulo | Aprovados | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Monólito | 48 | 0 | 0 | 1 |
| Owners Service | 12 | 0 | 0 | 0 |
| Gateway | 1 | 0 | 0 | 0 |

Os três módulos foram compilados e empacotados com `scripts/build.ps1`. O teste ignorado é o `CrashControllerTests`, já marcado dessa forma nas etapas anteriores. [Evidência Maven](evidencias/maven-v3-final.json).

A execução de `scripts/acceptance.py` aprovou 79 verificações pelo Gateway em um projeto Compose isolado, na porta 18090. O roteiro verificou REST, Thymeleaf, redirecionamentos, gravações de Owner, Pet e Visit, bloqueio da API interna, persistência após reinício e indisponibilidade nas duas direções. Entre os resultados esperados houve 1 resposta 400, 12 respostas 404, 2 respostas 409 e 6 respostas 503. O projeto temporário foi encerrado com os volumes preservados. [Evidência HTTP](evidencias/http-compose-v3-final.json).

## Métricas CK da versão final

A coleta usa CK `0.7.1-SNAPSHOT`, Java 17.0.12 e `false 0 false`. Os hashes dos arquivos Java do worktree de validação coincidiram com os da coleta da V3 unificada. A proveniência preserva o estado medido, o SHA-256 do JAR e o hash dos CSVs. [Proveniência da V3 unificada](metricas-controller-unificado/proveniencia.json).

| Componente | Tipos CK | Métodos CK | CBO médio | WMC médio | RFC médio | LCOM médio | LCOM* médio |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Monólito original | 24 | 91 | 6,6667 | 4,9167 | 3,4583 | 5,8750 | 0,2566 |
| Monólito remanescente V3 | 28 | 95 | 6,1429 | 4,6429 | 4,5000 | 5,9286 | 0,2181 |
| Owners Service V3 | 8 | 32 | 6,2500 | 5,0000 | 5,1250 | 10,2500 | 0,1889 |
| Gateway V3 | 2 | 2 | 3,5000 | 1,0000 | 1,5000 | 0,0000 | 0,0000 |

As médias usam os tipos de cada componente, incluindo tipos internos e interfaces emitidos pela CK. A granularidade da classe mudou com a unificação de MVC e REST. O controller unificado concentra 15 métodos, com CBO 22, WMC 20 e RFC 25. A retirada do handler e da exceção do Owners Service também muda a população de tipos. A análise se restringe à estrutura Java e exclui templates, configuração declarativa e latência. [Resumo por componente](metricas-controller-unificado/resumo-componentes.csv) · [Comparação por tipo](metricas-controller-unificado/comparacao-tipos-v3.csv) · [CSVs da coleta final](metricas-controller-unificado/).

Para repetir a coleta a partir do código commitado, use PowerShell 7, configure `JAVA17_HOME` e aponte `$ckJar` para o JAR registrado na proveniência:

```powershell
./scripts/collect-ck.ps1 -V3 -CkJar $ckJar
```

O coletor salva a saída em um novo diretório sob `.work/`. O modo `-WorkingTree` permite medir fontes locais antes do commit. A comparação dos CSVs desconsidera a coluna de caminho e a ordem das linhas.

## Histórico do experimento

- [V3 inicial e migração da interface Owner](relatorio.md). Inclui os primeiros resultados da V3, a migração offline e a validação com duas réplicas.
- [Padronização dos clientes HTTP](refatoracao-integracoes-http.md). Registra a etapa intermediária com handlers dedicados nas duas aplicações.
- [Unificação dos controllers](refatoracao-controller-unificado.md). Registra a auditoria que produziu a coleta da V3 unificada e compara as métricas individuais.
- [Metodologia e coleta da V2](../metricas/README.md) · [Arquivo histórico](../../historico/README.md).
