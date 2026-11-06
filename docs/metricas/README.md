# Métricas CK do TCC II

A análise mede a estrutura estática dos fontes Java e permite comparar tipos equivalentes do monólito antes e depois da extração de Owners. A única coleta definitiva está em [atual/](atual/README.md). As campanhas anteriores e os diagnósticos estão em [historico/](historico/README.md).

| Cenário | Escopo | Tipos CK | Métodos CK |
| --- | --- | ---: | ---: |
| A · Monólito original | `src/main/java` de `c36452a` | 24 | 91 |
| B · Monólito adaptado V2 | `projects/monolith/src/main/java` com `Owner.addPet()` simplificado | 28 | 106 |
| C · Owners Service | `projects/owners-service/src/main/java` da V2 | 4 | 20 |

`class.csv` contém uma linha por tipo emitido pela CK, incluindo interfaces e tipos internos. `method.csv` detalha os métodos e construtores emitidos pela ferramenta. A coluna `file` registra o caminho da execução e a coluna `class` identifica o tipo pelo nome totalmente qualificado. A CK não inclui os testes no escopo acima.

O Gateway permanece na arquitetura e na aceitação HTTP. Seu roteamento depende da configuração do framework, e sua pequena população de tipos Java agrega pouca informação à comparação estrutural entre os monólitos e a extração de Owners. As medições anteriores do Gateway estão no histórico.

CBO e RFC descrevem aspectos do acoplamento estático e da resposta de um tipo. WMC descreve complexidade, enquanto LCOM e LCOM* medem aspectos de coesão. São medidas de código que precisam ser interpretadas junto das responsabilidades de cada tipo. Chamadas HTTP, latência, disponibilidade e comportamento após reinício são questões do sistema distribuído e exigem evidências próprias. A variação das métricas CK, por si só, é insuficiente para estabelecer melhoria arquitetural.
