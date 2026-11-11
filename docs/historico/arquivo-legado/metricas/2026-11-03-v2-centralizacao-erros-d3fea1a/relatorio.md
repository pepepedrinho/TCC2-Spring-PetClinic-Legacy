# Centralização posterior do tratamento de erros — CK e validação

Intervenção de 03/11/2026 na branch `tcc2-strangler-fig-v2`, sem commit ou push. HEAD de referência: `d3fea1a`. Os snapshots e hashes de fontes identificam os estados B e C não commitados; o HEAD isoladamente não representa esta medição. A iniciativa reduz arquivos de exceção por decisão arquitetural do autor do TCC. Não amplia a extração de Owner.

## Funcionamento anterior e novo

Antes, `OwnersClient.translate` classificava falhas remotas e criava três exceções. As anotações de duas delas definiam 404/400; o advice tratava somente a terceira com 503 e a view `ownersUnavailable`. PetController e VisitController também lançavam a exceção de 404.

Agora, `IntegrationErrorHandler.remoteFailure` classifica apenas falhas encaminhadas explicitamente pela fronteira HTTP: status remoto 404/400 é preservado; outras falhas capturadas tornam-se 503. `notFound` representa vínculo local inválido. `handle` define a resposta: `sendError` com os mesmos motivos anteriores para 404/400, mantendo o despacho de páginas MVC, ou status 503 e a mesma view para indisponibilidade. O cliente mantém HTTP, payloads, encoding e timeouts e delega a classificação; `translate` deixou de existir.

Existe um único tipo interno, `IntegrationErrorHandler.IntegrationFailure`, com status imutável e causa. Ele é necessário para delimitar o advice à integração e transportar a decisão de status desde a fronteira; seu construtor privado impede a criação arbitrária por consumidores. Não é uma reprodução interna das três classes: as decisões agora são compartilhadas nas fábricas e no handler. O advice não captura RuntimeException globalmente. O cliente continua capturando somente `RestClientException | HttpMessageConversionException`; IllegalStateException e erros HTTP não marcados de outras origens propagam para o tratamento original.

São usados mecanismos Java 8/Spring Boot 1.5, sem ResponseStatusException no monólito. Porta e rotas 8081/8080/8082, contratos, modelos, validações, endpoints e timeouts não foram modificados. Owner segue exclusivo do serviço; Pets, Visits e Vets permanecem no legado. Os comentários de 1.5.x foram preservados e os métodos agora alterados ganharam notas breves sobre a decisão posterior.

## Arquivos da intervenção

Modificados:

- `src/main/java/org/springframework/samples/petclinic/integration/IntegrationErrorHandler.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnersClient.java`
- `src/main/java/org/springframework/samples/petclinic/owner/PetController.java`
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- `src/test/java/org/springframework/samples/petclinic/integration/OwnersClientTests.java`
- `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java`
- `docs/v2/estudo-codigo-antes-depois.md`

Excluídos:

- `src/main/java/org/springframework/samples/petclinic/integration/OwnerNotFoundException.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnerValidationException.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnersUnavailableException.java`

Adicionado: `src/test/java/org/springframework/samples/petclinic/integration/IntegrationErrorHandlerTests.java`. Também foi adicionado este diretório de métricas, scripts de reprodução e evidências. Os scripts canônicos anteriores foram preservados. Ajustes de formatação anteriores permanecem no diff geral e não são atribuídos a esta intervenção.

## Testes executados

Maven 3.9.11, `-B -o clean package`, acrescido de `-Dcobertura.skip=true` no legado, conforme o build existente. Execução em cópia isolada dos projetos, usando JDK 8 no monólito e JDK 17 nos demais.

| componente | JDK | executados | aprovados | ignorados | falhas | erros |
| --- | --- | --- | --- | --- | --- | --- |
| monolito | 8 | 61 | 60 | 1 | 0 | 0 |
| owners-service | 17 | 6 | 6 | 0 | 0 | 0 |
| gateway | 17 | 1 | 1 | 0 | 0 | 0 |

Total: **67 aprovados e 1 ignorado**, sem falhas/erros. A referência anterior era 53 aprovados e 1 ignorado; foram acrescentados 14 testes: 11 do advice, 2 do cliente e 1 de pertencimento de Pet em VisitController. Os testes existentes de cliente foram adaptados para verificar status e causa, em vez de três tipos independentes.

A suíte verifica 404 e motivo original, 400 e motivo original, 503/view por indisponibilidade, outro status remoto, timeout, falha de conexão, resposta malformada e falha explícita de conversão, vínculo local inválido e propagação de erros internos e HTTP não pertencentes à integração. Os testes MVC existentes de Owner/Pet/Visit, repositórios e Vets e o teste de rotas do Gateway também foram executados. CrashControllerTests continua ignorado pela configuração anterior; o comportamento interno 500 foi validado por HTTP real em /oups.

O procedimento de aceitação HTTP existente passou com **59 verificações HTTP** (inclui prontidão; não é contagem JUnit). Além dos checks anteriores, foram verificados `/owners/new` com primeiro nome de 31 caracteres rejeitado remotamente como 400, `/oups` como 500 e texto da view de indisponibilidade. Foram exercitados criação/edição/consulta de Owner/Pet/Visit, restrição de vínculo, Vets, rotas e assets pelo Gateway, interrupção e reinicialização das JVMs com os mesmos bancos e permanência dos dados e das sequências. Owners desligado produz 503 e Vets continua acessível.

A migração foi executada em fixture nova de banco histórico, nunca em dados do usuário. Passaram: IDs de Owner/Pet/Visit, relacionamento, ausência de tabela Owners no legado, marcador de seed, continuidade de identidade em 102, recusa de destino existente e hashes idênticos dos arquivos da origem antes/depois. Os procedimentos executados, adaptações de destino, logs de JVM, XMLs Surefire e resultados JSON estão em `evidencias/`.

## Coleta e comparabilidade

CK **0.7.1-SNAPSHOT**, fonte `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`, SHA-256 do JAR `98893efdffd22fb2356560e9ed78b0528e01bbbad032dcd63be3863c73bfc517`, confirmado antes da execução. JDK 17, mesmos argumentos **false 0 false**. A fonte de cada componente é analisada separadamente, sem misturar testes, scripts, fontes geradas ou dependências compiladas. Incluem-se todos os tipos emitidos em class.csv, interfaces e classes internas inclusive; package-info não gera tipo. Não foram excluídos tipos com métricas zero.

- A: fontes do monólito original recuperados por Git de `refs/heads/1.5.x`, commit `c36452a2c34443ae26b4ecbba4f149906af14717`; recoletados nesta execução.
- B: snapshot real da cópia de trabalho imediatamente antes da centralização, com exceções separadas e organização anterior; recoletado, sem presumir igualdade com os antigos CSVs.
- C: snapshot do candidato centralizado compilado/testado; aplicado posteriormente à mesma branch. Hashes arquivados permitem conferir sua identidade com os fontes entregues.
- Owners/Gateway: recoletados na mesma execução. Seus fontes de produção são byte a byte idênticos em B e C.

A recoleta A coincide com todos os campos dos CSVs históricos da baseline, exceto caminhos. As cinco métricas solicitadas de B coincidem com o monólito histórico V2. Foram mantidas também diferenças de LOC/outras colunas nos CSVs brutos; não foram reescritas. Proveniência, comandos, versão Java, hashes por cenário e arquivos ZIP completos dos fontes analisados acompanham esta entrega.

## Médias A × B × C e componentes separados

Média aritmética por tipo CK; `lcom*` é a coluna literal normalizada da ferramenta. A/B/C abaixo referem-se apenas ao monólito. Owners e Gateway não são cenários C adicionais nem parte do denominador das médias do monólito.

| cenario | tipos_CK | cbo | wmc | rfc | lcom | lcom* |
| --- | --- | --- | --- | --- | --- | --- |
| A | 24 | 6.666667 | 4.916667 | 3.458333 | 5.875000 | 0.256618 |
| B | 30 | 6.166667 | 4.866667 | 4.366667 | 5.266667 | 0.227258 |
| C | 28 | 6.571429 | 5.357143 | 4.928571 | 5.750000 | 0.236348 |
| Owners | 4 | 7.250000 | 5.000000 | 2.750000 | 9.000000 | 0.236111 |
| Gateway | 1 | 2.000000 | 1.000000 | 1.000000 | 0.000000 | 0.000000 |

Estatísticas com n, média, mediana, mínimo, máximo e desvio padrão populacional estão em `estatisticas-descritivas.csv`. As diferenças de população exigem cautela ao comparar A com B/C: a extração já existia em B.

## Efeito medido de B para C

| metrica | media_B | media_C | delta | variacao_percentual |
| --- | --- | --- | --- | --- |
| cbo | 6.166667 | 6.571429 | 0.404762 | 6.563707 |
| wmc | 4.866667 | 5.357143 | 0.490476 | 10.078278 |
| rfc | 4.366667 | 4.928571 | 0.561905 | 12.868048 |
| lcom | 5.266667 | 5.750000 | 0.483333 | 9.177215 |
| lcom* | 0.227258 | 0.236348 | 0.009090 | 3.999803 |

Três classes de topo foram removidas e uma classe interna foi adicionada: o número de tipos CK cai de 30 para 28, uma redução líquida de 2 (6,67%). No pacote integration, arquivos Java caem de 5 para 2 e tipos de 5 para 3. O número de arquivos .java do monólito cai de 31 para 28, incluindo package-info nas duas contagens. O CK emite uma linha independente para `IntegrationErrorHandler$IntegrationFailure`, que participa de todas as médias de C. O handler externo permanece um tipo; não se omitiu o tipo interno para inflar a redução.

Somatórios observados (não constituem índice global de qualidade):

| cenario | tipos_CK | cbo | wmc | rfc | lcom |
| --- | --- | --- | --- | --- | --- |
| B | 30 | 185.000000 | 146.000000 | 131.000000 | 158.000000 |
| C | 28 | 184.000000 | 150.000000 | 138.000000 | 161.000000 |

O CBO total cai apenas 1, de 185 para 184, mas o CBO médio sobe 6,56% por causa da redistribuição das dependências e da redução do denominador. WMC e RFC totais sobem, de 146 para 150 e de 131 para 138. LCOM total sobe de 158 para 161. Não se soma LCOM* normalizado como índice arquitetural.

Para separar parcialmente o efeito do denominador, os 27 tipos comuns aos dois cenários também foram pareados:

| metrica | tipos_equivalentes | media_B | media_C | delta |
| --- | --- | --- | --- | --- |
| cbo | 27 | 6.777778 | 6.777778 | 0.000000 |
| wmc | 27 | 5.370370 | 5.481481 | 0.111111 |
| rfc | 27 | 4.851852 | 5.074074 | 0.222222 |
| lcom | 27 | 5.851852 | 5.962963 | 0.111111 |
| lcom* | 27 | 0.252509 | 0.245102 | -0.007407 |

O pareamento preserva o tipo pelo nome, não afirma equivalência funcional completa do handler cujo papel mudou. A tabela CSV `classes-equivalentes-b-c.csv` registra todos os tipos da união: métricas B/C e deltas somente onde ambos existem. Tipos removidos são identificados como removidos, sem valores C fabricados; a classe interna adicionada não recebe valor B fictício.

## Classes afetadas, dependências e coesão

| classe | B | C |
| --- | --- | --- |
| integration.IntegrationErrorHandler | cbo=4; wmc=1; rfc=0; lcom=0; lcom*=0.0 | cbo=7; wmc=8; rfc=5; lcom=3; lcom*=0.0 |
| integration.IntegrationErrorHandler$IntegrationFailure | adicionada; sem valor anterior | cbo=1; wmc=2; rfc=1; lcom=0; lcom*=0.0 |
| integration.OwnerNotFoundException | cbo=1; wmc=0; rfc=0; lcom=0; lcom*=0.0 | removida; sem valor atual |
| integration.OwnerValidationException | cbo=1; wmc=0; rfc=0; lcom=0; lcom*=0.0 | removida; sem valor atual |
| integration.OwnersClient | cbo=13; wmc=12; rfc=21; lcom=0; lcom*=0.2 | cbo=8; wmc=8; rfc=20; lcom=0; lcom*=0.0 |
| integration.OwnersUnavailableException | cbo=0; wmc=1; rfc=0; lcom=0; lcom*=0.0 | removida; sem valor atual |
| owner.PetController | cbo=22; wmc=17; rfc=21; lcom=17; lcom*=0.6333334 | cbo=23; wmc=17; rfc=22; lcom=17; lcom*=0.6333334 |
| owner.VisitController | cbo=16; wmc=8; rfc=11; lcom=6; lcom*=0.6 | cbo=17; wmc=8; rfc=12; lcom=6; lcom*=0.6 |

OwnersClient perdeu a classificação e as dependências diretas das três exceções e de HttpStatus/HttpStatusCodeException; passou a depender do handler e do marcador de retorno. Seu CBO caiu 13→8, WMC 12→8 e RFC 21→20. Isso mede simplificação local do cliente.

IntegrationErrorHandler recebeu a classificação HTTP, decisão de respostas, HttpServletResponse, ModelAndView e construção do marcador. Seu CBO subiu 4→7, WMC 1→8 e RFC 0→5. PetController e VisitController agora chamam a fábrica notFound; CK contabiliza essas chamadas e a referência ao tipo de retorno, aumentando CBO e RFC em 1 em cada controller. A nova classe interna tem CBO 1, WMC 2 e RFC 1. Portanto, parte da complexidade foi concentrada e as dependências de chamadores mudaram; eliminar arquivos não elimina automaticamente acoplamento.

LCOM* do cliente caiu 0,2→0. LCOM do handler foi de 0 para 3, mas o handler é sem estado e sua LCOM* continua zero. Na implementação deste CK, LCOM compara pares de métodos e acessos a campos, e LCOMNormalized devolve zero quando não há atributos. Zero nessa situação não prova coesão perfeita; três métodos estáticos sem campos também não demonstram isoladamente falha de responsabilidade. As médias de LCOM e LCOM* do monólito aumentaram, logo a centralização não mostra melhoria global uniforme de coesão.

## Diferença de Owners em relação à coleta histórica

Embora os fontes de Owners sejam idênticos entre B e C, o novo LCOM médio de Owners é 9, contra 3,5 no resultado histórico V2; LCOM* médio é 0,236111, contra 0,218750. A diferença está em Owner: LCOM 14→36 e LCOM* 0,7083333→0,77777773. CBO, WMC e RFC permanecem iguais.

Foi executado um controle adicional em `owners-historico-controle/`, com o fonte original do checkpoint `5b6a3b173153d5be8f4625e96ab627aa5f3bd4e6`, mesma ferramenta e parâmetros. Todos os campos, exceto caminhos, reproduziram a coleta histórica de Owners. Naquele fonte, campos e acessores eram intercalados; no snapshot B eles já estavam agrupados pela intervenção anterior. A inspeção dos visitantes LCOM/LCOMNormalized mostra que registram campos e atribuem acessos ao último método durante a travessia, tornando os resultados sensíveis à ordem das declarações nesse caso. Essa é uma limitação observada da medição, não evidência de mudança funcional de Owners nem efeito da centralização de erros. Os fontes dos visitantes usados para essa verificação estão em evidencias/ck-implementacao.

O controle é adicional e não substitui nem altera os cenários A/B/C. Gateway reproduziu todos os campos da coleta histórica, exceto caminhos. Não foram ajustados valores nem excluídos resultados divergentes.

## Preservação, limites e reprodução

`docs/metricas/v2`, docs/baseline e docs/historico permanecem intactos, incluindo hashes, CSVs e evidências. Nenhum resultado novo foi gravado neles. A inicialização de bancos reais, as configurações e os scripts CK anteriores não foram modificados. A auditoria de hashes após aplicação é registrada separadamente em `auditoria-preservacao.json`.

Os resultados descrevem este checkpoint e esta ferramenta; CBO não mede dependência operacional, latência ou disponibilidade. A/B/C não controlam todos os efeitos da extração e da apresentação anterior. Timeout e conversão foram exercitados com mocks, enquanto indisponibilidade/conexão recusada, binding remoto 400, 404, 500, persistência/restart, negócios e roteamento também foram exercitados no HTTP real. A migração validada é de uma fixture histórica, não de um banco particular do usuário. Os testes não provam ausência absoluta de regressões, mas não foram observadas regressões nos cenários executados.

Não houve nova extração, mudança de negócio ou contratos. A redução de arquivos foi alcançada, com simplificação local do cliente e maior concentração de decisões no handler, acompanhada de aumento das médias solicitadas do monólito. Não se conclui melhoria automática de qualidade.

Os scripts `coletar.py` e `analisar.py`, snapshots de fontes e README descrevem como repetir em um diretório novo, sem sobrescrever as coletas anteriores. As cópias dos procedimentos HTTP/migração redirecionam evidências apenas para esta pasta e recuperam SQL do Git original; os scripts canônicos permanecem preservados.
