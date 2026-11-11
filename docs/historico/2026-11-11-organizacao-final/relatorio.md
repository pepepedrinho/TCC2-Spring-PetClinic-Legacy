# Organização final de pacotes e remoção de OwnerLookup

A V3 anterior está na tag `tcc2-v3-pre-organizacao-final` (`4732ebacce7193689808011f6e593b3b6f78c9b0`). A primeira etapa (`faea26d185418e5108534c66ea419b355cb13451`) reorganizou os pacotes e manteve `OwnerLookup`. A segunda (`f89508d8266c1e1f6c1d9f6cfecea25a9511c6c7`) removeu o componente compartilhado. A recoleta intermediária permite atribuir as diferenças às etapas.

## Organização e contrato

| Tipo antes | Tipo após reorganização |
| --- | --- |
| `org.springframework.samples.petclinic.owner.Owner` | `org.springframework.samples.petclinic.model.Owner` |
| `br.edu.tcc.OwnersApplication` | `br.edu.tcc.owners.OwnersApplication` |
| `br.edu.tcc.owners.integration.OwnerRestController` | `br.edu.tcc.integration.OwnerRestController` |

A entrada do Owners Service passou a `br.edu.tcc.owners`. A varredura de componentes cobre `owners` e `integration`. Entidade JPA e repositório do serviço permanecem em `owners`, o pacote da aplicação. O Owner do monólito continua sendo o modelo dos dados remotos e da associação local com Pets.

Cada controlador do serviço consulta `OwnerRepository.findById` e converte ausência em HTTP 404 no próprio controlador. A exclusão de Owner existente continua retornando 409. A validação, as transações existentes e as rotas de Pets e Visits foram preservadas.

## Resultados CK

CK `0.7.1-SNAPSHOT`, Java 17.0.12 e parâmetros `false 0 false`. As médias incluem todos os tipos emitidos pela CK. A usa a coleta histórica e foi conferido nas duas execuções. Os percentuais usam o estado inicial de cada contraste. A nomenclatura completa foi mapeada antes da comparação individual.
[Antes](../2026-11-11-pre-organizacao-final-4732eba/README.md) · [Somente pacotes](somente-pacotes/README.md) · [Coleta final daquela intervenção](../2026-11-11-pre-ownersapplication-root-9152c1c/README.md) · [Proveniência final](proveniencia-final.json)

### Tipos e médias

| Estado | Tipos | CBO | WMC | RFC | LCOM | LCOM* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A · Referência | 24 | 6,6667 | 4,9167 | 3,4583 | 5,875 | 0,2566 |
| B · Anterior | 28 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| B · Pacotes | 28 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| B · Final | 28 | 6,1071 | 4,6071 | 4,5357 | 5,6786 | 0,2166 |
| B · Δ Pacotes | 0 | 0 | 0 | 0 | 0 | 0 |
| B · Δ Pacotes (%) | 0 | 0 | 0 | 0 | 0 | 0 |
| B · Δ Remoção | 0 | 0 | 0 | 0 | 0 | 0 |
| B · Δ Remoção (%) | 0 | 0 | 0 | 0 | 0 | 0 |
| B · Δ Total | 0 | 0 | 0 | 0 | 0 | 0 |
| B · Δ Total (%) | 0 | 0 | 0 | 0 | 0 | 0 |
| C · Anterior | 10 | 6,2 | 4,2 | 4,6 | 4,1 | 0,1756 |
| C · Pacotes | 10 | 6,2 | 4,2 | 4,6 | 4,1 | 0,1756 |
| C · Final | 9 | 6,3333 | 4,4444 | 5,1111 | 4,1111 | 0,1512 |
| C · Δ Pacotes | 0 | 0 | 0 | 0 | 0 | 0 |
| C · Δ Pacotes (%) | 0 | 0 | 0 | 0 | 0 | 0 |
| C · Δ Remoção | -1 | +0,1333 | +0,2444 | +0,5111 | +0,0111 | -0,0243 |
| C · Δ Remoção (%) | -10 | +2,1505 | +5,8201 | +11,1111 | +0,271 | -13,8537 |
| C · Δ Total | -1 | +0,1333 | +0,2444 | +0,5111 | +0,0111 | -0,0243 |
| C · Δ Total (%) | -10 | +2,1505 | +5,8201 | +11,1111 | +0,271 | -13,8537 |

### Totais de CBO, WMC e RFC

| Estado | CBO | WMC | RFC |
| --- | ---: | ---: | ---: |
| A · Referência | 160 | 118 | 83 |
| B · Anterior | 171 | 129 | 127 |
| B · Pacotes | 171 | 129 | 127 |
| B · Final | 171 | 129 | 127 |
| B · Δ Pacotes | 0 | 0 | 0 |
| B · Δ Pacotes (%) | 0 | 0 | 0 |
| B · Δ Remoção | 0 | 0 | 0 |
| B · Δ Remoção (%) | 0 | 0 | 0 |
| B · Δ Total | 0 | 0 | 0 |
| B · Δ Total (%) | 0 | 0 | 0 |
| C · Anterior | 62 | 42 | 46 |
| C · Pacotes | 62 | 42 | 46 |
| C · Final | 57 | 40 | 46 |
| C · Δ Pacotes | 0 | 0 | 0 |
| C · Δ Pacotes (%) | 0 | 0 | 0 |
| C · Δ Remoção | -5 | -2 | 0 |
| C · Δ Remoção (%) | -8,0645 | -4,7619 | 0 |
| C · Δ Total | -5 | -2 | 0 |
| C · Δ Total (%) | -8,0645 | -4,7619 | 0 |

## Efeito por tipo

A ordem dos valores é CBO / WMC / RFC / LCOM / LCOM*.

| Tipo após mover pacotes | Antes | Pacotes | Final |
| --- | --- | --- | --- |
| `org.springframework.samples.petclinic.model.Owner` | 9 / 18 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 | 9 / 18 / 15 / 58 / 0,7885 |
| `br.edu.tcc.owners.OwnersApplication` | 4 / 2 / 4 / 1 / 0 | 4 / 2 / 4 / 1 / 0 | 4 / 2 / 4 / 1 / 0 |
| `br.edu.tcc.owners.OwnerController` | 15 / 14 / 20 / 4 / 0,6444 | 15 / 14 / 20 / 4 / 0,6444 | 15 / 14 / 21 / 0 / 0,5833 |
| `br.edu.tcc.owners.OwnerLookup` | 4 / 2 / 2 / 0 / 0 | 4 / 2 / 2 / 0 / 0 | Removido |
| `br.edu.tcc.integration.OwnerRestController` | 15 / 6 / 8 / 0 / 0,3333 | 15 / 6 / 8 / 0 / 0,3333 | 14 / 6 / 9 / 0 / 0 |

A etapa de pacotes preservou as cinco métricas por tipo após a correspondência pelos nomes qualificados.

Na segunda etapa, a remoção de `OwnerLookup` retirou **4 / 2 / 2** dos totais CBO / WMC / RFC de C. Os tipos remanescentes contribuíram com uma variação de **-1 / 0 / +2** na mesma ordem. O denominador das médias também diminuiu de 10 para 9 tipos.

## Validação

| Etapa | Monólito aprovados / ignorados | Owners Service | Gateway | HTTP |
| --- | ---: | ---: | ---: | ---: |
| Pacotes | 51 / 1 | 12 | 1 | 79 |
| Final | 51 / 1 | 12 | 1 | 79 |

Maven offline com Java 8 e 17, zero falhas e erros nas duas etapas. O teste `CrashControllerTests` permaneceu ignorado. A aceitação usou JVMs locais isoladas com bancos descartáveis e verificou o Gateway, persistência após reinício, rotas públicas e internas e as integrações. A execução em Docker Compose ficou fora desta validação. [Etapa de pacotes](somente-pacotes/validacao.json) · [Etapa final](validacao-final.json)
