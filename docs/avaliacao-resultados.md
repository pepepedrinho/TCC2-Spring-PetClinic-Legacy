# Avaliação dos resultados da migração incremental

## 1. Objetivo

Avaliar os efeitos da aplicação do padrão Strangler Fig ao Spring PetClinic legado, considerando a migração incremental dos domínios Owners e Pets para um microsserviço.

A avaliação contempla métricas estáticas de código, preservação das funcionalidades, persistência dos dados e comportamento diante da indisponibilidade do microsserviço.

## 2. Metodologia

A versão de referência corresponde ao commit c36452a2c34443ae26b4ecbba4f149906af14717, de 03/11/2017, baseado em Java 8 e Spring Boot 1.5.4.RELEASE.

As métricas foram coletadas com a ferramenta CK 0.7.1, utilizando os arquivos Java de produção de cada aplicação.

Foram avaliados três conjuntos:

- Baseline: monólito original de 2017.
- Monólito refatorado: aplicação Java 8 após a integração com o microsserviço.
- Microsserviço Owners/Pets: aplicação Java 17 responsável pelos dados e operações de proprietários e animais.

Os resultados completos estão disponíveis em `docs/baseline/` e `docs/metricas/`.

## 3. Resultados quantitativos

| Indicador | Baseline | Monólito refatorado | Microsserviço |
|---|---:|---:|---:|
| Classes | 24 | 32 | 16 |
| CBO médio | 6,67 | 6,34 | 5,06 |
| WMC médio | 4,92 | 5,88 | 3,69 |
| RFC médio | 3,46 | 4,72 | 5,38 |
| LCOM médio | 5,88 | 9,19 | 7,44 |
| LCOM* médio | 0,26 | 0,32 | 0,26 |

A arquitetura refatorada possui 48 classes identificadas pelo CK, distribuídas entre o monólito e o microsserviço. Esse aumento não representa, por si só, uma duplicação da complexidade: parte das classes foi introduzida para estabelecer as fronteiras e os contratos de integração.

Em relação à baseline, o CBO médio do monólito diminuiu aproximadamente 4,9%, enquanto WMC, RFC, LCOM e LCOM* apresentaram aumentos.

Esses resultados não demonstram uma melhoria uniforme da qualidade interna do código.

## 4. Comparação das classes originais

Todas as 24 classes identificadas na baseline foram encontradas na análise do monólito refatorado.

Considerando CBO, WMC, RFC e LCOM, 20 classes mantiveram os mesmos valores e quatro apresentaram alterações:

| Classe | CBO antes/depois | WMC antes/depois | RFC antes/depois | LCOM antes/depois |
|---|---|---|---|---|
| VisitController | 14 / 17 | 6 / 7 | 6 / 10 | 6 / 6 |
| PetTypeFormatter | 6 / 9 | 5 / 8 | 3 / 9 | 1 / 4 |
| PetController | 20 / 22 | 14 / 17 | 15 / 19 | 8 / 12 |
| OwnerController | 15 / 18 | 14 / 14 | 17 / 17 | 0 / 8 |

As alterações se concentram nas classes que participam da integração com Owners/Pets ou da composição das páginas legadas.

## 5. Interpretação das métricas

O CBO médio do monólito apresentou uma pequena redução, mas as métricas WMC, RFC e LCOM aumentaram.

No microsserviço, os valores médios de CBO e WMC foram inferiores aos da baseline, enquanto RFC e LCOM foram superiores.

A elevação de LCOM exige interpretação cuidadosa. Classes de domínio e objetos de transferência de dados com vários atributos e métodos de acesso podem apresentar valores elevados nessa métrica sem necessariamente acumular responsabilidades de negócio distintas.

Entre os maiores valores de LCOM observados estão OwnerResponse (77) e PetResponse (54), no monólito refatorado, e Owner (66), no microsserviço.

A comparação evidencia mudanças na distribuição da estrutura do código, mas não autoriza concluir que a qualidade global aumentou ou diminuiu exclusivamente a partir das médias.

## 6. Resultados funcionais

A migração foi executada incrementalmente, preservando a interface Thymeleaf no monólito.

Foram verificadas as seguintes funcionalidades:

- Consulta e manutenção de proprietários por meio do microsserviço.
- Consulta e manutenção de animais por meio do microsserviço.
- Associação dos animais aos respectivos proprietários.
- Cadastro e consulta de visitas mantidos no monólito.
- Preservação do acesso às funcionalidades de veterinários.
- Acesso integrado por meio do Spring Cloud Gateway na porta 8081.

Na validação automatizada mais recente do monólito, foram executados 51 testes, com zero falhas, zero erros e um teste ignorado.

Essa contagem corresponde à suíte Maven do monólito, não a uma contagem consolidada de testes de todos os componentes.

## 7. Persistência dos dados

O microsserviço utiliza um banco H2 persistente para os dados de Owners e Pets.

O monólito utiliza HSQLDB persistente para os dados legados, incluindo Visits.

Foi realizada uma validação manual com o cadastro de uma nova visita para um animal criado no microsserviço. Após reiniciar o monólito com o perfil persistente, a visita permaneceu disponível.

A inicialização histórica do HSQLDB utiliza um perfil separado, destinado exclusivamente à primeira carga do banco.

## 8. Indisponibilidade do microsserviço

A indisponibilidade do microsserviço Owners/Pets foi simulada interrompendo sua execução.

Durante a falha:

- O acesso à página de proprietário retornou HTTP 503.
- Uma mensagem amigável foi apresentada ao usuário.
- A funcionalidade de veterinários permaneceu acessível.

Um teste automatizado adicional validou o tratamento HTTP 503.

O resultado demonstra isolamento funcional parcial: as funcionalidades não dependentes de Owners/Pets continuaram operacionais, enquanto as dependentes da API ficaram temporariamente indisponíveis.

## 9. Limitações da avaliação

As métricas CK representam propriedades estáticas das classes Java. Elas não medem diretamente o acoplamento HTTP, a latência das chamadas entre serviços, a disponibilidade operacional ou o custo de manutenção distribuída.

A comparação das médias também é influenciada pela diferença na quantidade e na natureza das classes presentes em cada versão.

A permanência de classes de domínio no monólito, necessárias à compatibilidade com a interface e com o código legado, também deve ser considerada ao interpretar a redistribuição de responsabilidades.

Os testes funcionais de persistência e indisponibilidade descritos foram realizados no ambiente local. Não constituem uma avaliação de carga, desempenho ou disponibilidade em produção.

## 10. Conclusão

A aplicação do Strangler Fig permitiu introduzir um Gateway e extrair incrementalmente as operações de Owners e Pets para um microsserviço Java 17, preservando a execução do monólito Java 8 e suas funcionalidades remanescentes.

Os testes e verificações manuais demonstraram o funcionamento da integração, a persistência dos dados e um tratamento controlado da indisponibilidade do microsserviço.

Entretanto, as métricas CK apresentaram resultados mistos: houve pequena redução do CBO médio no monólito, acompanhada de aumentos em outras métricas.

Portanto, o principal resultado observado é a viabilidade da modernização incremental e da coexistência entre tecnologias, e não uma redução generalizada da complexidade interna do código.