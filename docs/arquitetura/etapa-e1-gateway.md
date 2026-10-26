# E1 — Coexistência com Spring Cloud Gateway

## Objetivo

Introduzir um ponto de entrada único para o PetClinic legado,
sem modificar suas regras de negócio ou seu banco de dados.

## Componentes

- Monólito: Java 8, Spring Boot 1.5.4, porta 8080.
- Gateway: Java 17, Spring Boot 3.5.16, Spring Cloud 2025.0.3, porta 8081.
- Roteamento: todas as requisições encaminhadas ao monólito.

## Implementação

O Gateway foi criado como projeto Maven independente na pasta
gateway/, preservando o pom.xml e o código-fonte do monólito.

A rota Path=/** encaminha as requisições para localhost:8080.

## Problema identificado

Na primeira execução, a navegação de proprietários redirecionava
o navegador diretamente para localhost:8080, contornando o Gateway.

Foi configurado o filtro PreserveHostHeader para preservar o
Host original recebido pelo Gateway.

Após a correção, a navegação manual realizada permaneceu
na porta 8081.

## Resultado parcial

- Gateway compilado com sucesso.
- Monólito e Gateway executados simultaneamente.
- Acesso pelo Gateway confirmado.
- Navegação manual sem saída da porta 8081 após a correção.

Ainda são necessários testes funcionais completos e automatizados
antes de considerar a coexistência integralmente validada.

## Próxima etapa

Introduzir o microsserviço Owners/Pets e migrar gradualmente
as responsabilidades definidas no planejamento arquitetural.
