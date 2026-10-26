# Planejamento da extração Owners/Pets

## Baseline
- Sistema: Spring PetClinic monolítico
- Commit original: c36452a2c34443ae26b4ecbba4f149906af14717
- Data do commit: 03/11/2017
- Java: 8
- Spring Boot: 1.5.4.RELEASE
- Ferramenta de métricas: CK
- Baseline quantitativo: docs/baseline/

## Justificativa da escolha

O domínio Owners/Pets foi selecionado por apresentar classes com
valores elevados de acoplamento (CBO) e dependências funcionais
com o domínio Visits.

PetController apresentou CBO 20, enquanto Owner apresentou
WMC 19 e LCOM 58 na análise inicial.

A escolha não pressupõe melhoria automática dessas métricas
após a migração.

## Fronteira arquitetural proposta

Owners/Pets Service:
- Owner
- Pet
- PetType
- Controllers e repositórios relacionados
- Validações e formatações necessárias

Monólito remanescente:
- Visit e VisitRepository
- VisitController
- Vet e funcionalidades relacionadas
- Composição inicial das páginas Thymeleaf

## Persistência proposta

O serviço Owners/Pets será responsável pelas tabelas owners,
pets e types.

O monólito continuará responsável por visits, vets,
specialties e vet_specialties.

O relacionamento visits.pet_id deixará de ser uma chave
estrangeira entre bancos e precisará ser validado por contrato
de integração entre serviços.

## Integração proposta

O Spring Cloud Gateway será utilizado como ponto de entrada.

O monólito consumirá uma API interna do serviço Owners/Pets
para obter informações de proprietários e animais necessárias
ao domínio Visits e à composição das páginas.

## Pontos que exigem validação

- Preservação dos formulários e redirecionamentos
- Compatibilidade dos objetos utilizados pelo Thymeleaf
- Consulta e cadastro de visitas
- Comportamento diante de indisponibilidade do serviço
- Integridade referencial entre bancos separados
- Comparação das métricas CK antes e depois da extração
- Identificação das novas dependências HTTP
