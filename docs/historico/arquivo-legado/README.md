# Arquivo histórico do TCC II

A [coleta CK oficial](../../metricas/README.md) fica em `docs/metricas/`. Este arquivo reúne materiais de etapas anteriores e registros de validação.

- [Métricas](metricas/README.md) · dez campanhas e diagnósticos com CSVs, proveniência, snapshots e evidências específicas de cada campanha.
- [Documentação](documentacao/) · [implementação da V2](documentacao/implementacao.md), [decisões](documentacao/decisoes.md), [estudo antes/depois](documentacao/estudo-codigo-antes-depois.md), [manutenção de nomes](documentacao/manutencao-nomes-publicacao.md) e [README original](documentacao/README-original-2017.md).
- [Evidências](evidencias/) · [validação de Owner.addPet()](evidencias/evidencias-addpet/README.md) e [validação da manutenção de nomes](evidencias/evidencias-manutencao-nomes/).

## Caminhos e cópias consolidadas

Os caminhos registrados em comandos, manifestos históricos, logs e relatórios de execução descrevem a árvore da época. A organização imediatamente anterior pode ser consultada no commit `955304c`. As referências operacionais e os links relativos apontam para os destinos atuais.

- Na primeira campanha V2, `baseline-recoletada/coleta.txt`, `monolito/coleta.txt`, `owners/coleta.txt` e `gateway/coleta.txt` tinham o mesmo conteúdo. Uma cópia representa os quatro cenários. [Log compartilhado](metricas/2026-10-28-v2-extracao-5b6a3b1/coleta.txt)
- Na campanha do adaptador de Owners, `evidencias/http-centralizacao.json` era idêntico a `evidencias/http-acceptance.json`. O relatório continua registrando os dois roteiros. [JSON preservado](metricas/2026-11-06-v2-owner-adapter-dd6c9e5/evidencias/http-acceptance.json)

Arquivos identificados por manifestos ou pertencentes a suítes distintas mantêm seus nomes e contextos.

## Dashboard experimental

Na worktree de `feat/ck-dashboard`, o carregador lê `docs/metricas/v2-consolidada-2026-11-06/` e espera os cenários A, B, C e D. A campanha histórica correspondente está em [V2 anterior à simplificação de addPet](metricas/2026-11-06-v2-pre-refatoracao-addpet-1e8fa99/). Uma futura integração com a coleta oficial precisa adaptar o carregador e suas verificações para A, B e C e para os hashes por cenário em `proveniencia.json`.
