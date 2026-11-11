# Dashboard CK do Spring PetClinic

Aplicação local de leitura para explorar a coleta CK consolidada da migração Strangler Fig no TCC2. A interface usa Python 3.11+, Plotly Dash, Plotly, Dash AG Grid Community, pandas e NumPy. Os controles e as descrições estão em português brasileiro.

## Executar no Windows 11

A partir da raiz do repositório, no PowerShell:

```powershell
cd dashboard
py -3.11 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe app.py
```

Abra [http://127.0.0.1:8050](http://127.0.0.1:8050) no navegador. Para encerrar, use **Ctrl+C** no terminal em que o servidor está rodando.

Se a porta 8050 estiver ocupada, execute `.\.venv\Scripts\python.exe app.py --port 8051` e abra [http://127.0.0.1:8051](http://127.0.0.1:8051). Se o comando `py` estiver indisponível, confira `python --version` e use `python -m venv .venv` com Python 3.11 ou superior. Os comandos acima funcionam sem ativar o ambiente virtual. O servidor escuta somente na interface de loopback e carrega seus recursos localmente.

## Páginas e interações

| Página | Análise |
| --- | --- |
| **Visão geral** | Quantidades, médias por métrica em eixos independentes, efeito da composição, médias do Microsserviço Owners em separado e proveniência. O filtro lateral atualiza os gráficos e as tabelas. Sem métricas selecionadas, o painel orienta como restaurar a análise. |
| **Comparação pareada** | Barras de Δ por tipo, dispersão com referência Y = X, tabela de diferenças e estatística descritiva dos mesmos pares. Alterne CBO, WMC, RFC, LCOM e LCOM*. |
| **Componentes adicionais** | Valores dos tipos introduzidos em B, classificação, gráfico em cascata da soma de RFC e contribuições para a diferença total. |
| **Explorador de classes** | Tabela ordenável, estatísticas, boxplot e histograma. Em foco, exibe as cinco métricas, as contrapartes existentes e a posição e participação da métrica selecionada no cenário completo. |
| **Explorador de métodos** | Tabela de assinaturas e ranking de até 15 métodos das classes em foco. A métrica de método tem controle próprio. |

**Filtrar** muda a população e recalcula as visualizações do recorte. Use cenário, recorte, pacote, tipo CK, categoria, busca literal pelo nome e limites inclusivos para a métrica de tipo. Várias opções do mesmo campo funcionam como alternativas. Campos distintos são combinados. A página A × B fixa o recorte pareado e usa a métrica e o tipo de A para aplicar o intervalo e o filtro de tipo. A página de adicionais fixa B adicional. As referências integrais na Visão geral e na decomposição de RFC têm população fixa.

**Destacar** conserva a população. Clique em uma linha de tipo, em uma barra ou em um ponto selecionável, ou use **Classes em foco**. Repita o clique para retirar o destaque. A seleção atravessa páginas e A/B por nome qualificado. O contador indica quantas seleções estão fora do recorte. Para ver métodos, mantenha as classes em foco e escolha o cenário em que deseja investigá-las. **Limpar seleção** mantém os filtros. **Restaurar filtros e seleção** volta ao cenário B completo e à métrica RFC.

**Exportar CSV** usa as linhas disponíveis na tabela, incluindo filtros de coluna da tabela de métodos. Esses filtros refinam a tabela e sua exportação. O ranking mantém as classes em foco como referência. Use o ícone de câmera na barra do Plotly para salvar PNG e **Tela cheia** para ampliar um gráfico, conforme o navegador.

## Dados e metodologia

- A dashboard lê a [coleta oficial em docs/metricas](../docs/metricas/README.md), resolvida a partir da localização de `data_loader.py`. Em uma worktree que ainda não contenha a coleta atual, defina `CK_METRICS_DIR` com o caminho absoluto para `docs/metricas` de uma worktree atualizada antes de iniciar o servidor. Um caminho relativo nessa variável é interpretado a partir da raiz da worktree da dashboard.
- Na inicialização, verifica o esquema e as colunas numéricas dos seis CSVs, a unicidade das identidades e os totais e SHA-256 de cada cenário registrados em `proveniencia.json`. Os caminhos históricos da coluna `file` são descartados. Métodos são associados pelo cenário e nome qualificado, com assinatura CK e linha para distinguir registros.
- A coleta Docker tem 24 tipos e 91 métodos em A, 27 tipos e 104 métodos em B e 4 tipos e 20 métodos em C. A e B têm 24 tipos equivalentes por nome qualificado, e três são exclusivos de B. Owners é analisado separadamente. O Gateway aparece como contexto da arquitetura.
- [classification.json](classification.json) documenta a classificação manual dos tipos de integração e as mudanças de responsabilidade de tipos pareados. A detecção de tipos adicionais usa apenas os CSVs. Um adicional sem classificação explícita recebe a categoria “Adicional sem classificação”.
- As médias são aritméticas. O desvio-padrão é populacional e os quartis usam interpolação linear. Δ é B − A. Δ% é `100 × (B − A) / A` e fica indefinido quando A vale zero. Na visão geral, a variação percentual é calculada sobre as **médias**. A decomposição de RFC usa **somas por tipo**, e os percentuais de contribuição para o aumento usam a diferença entre as somas de B e A.
- Mudanças de média descrevem as populações analisadas. Os papéis de `Owner` e `OwnerRepository`, entre outros, mudam na V2. O acoplamento estático da CK e as dependências operacionais por HTTP têm escopos diferentes. Somas de RFC ou CBO podem contar relações repetidas. As métricas de método têm definições próprias e seus totais podem divergir das métricas de tipo.

### Limites de visualização

Pontos de dispersão podem se sobrepor quando os pares têm os mesmos valores. Use a tabela ou as barras para selecionar um nome específico. No boxplot, o deslocamento vertical dos pontos serve para separar empates. O valor da métrica permanece no eixo horizontal. As exportações de imagem são em PNG. A interface usa tema escuro.

Com `responsive=True`, o Dash remove as dimensões da figura Plotly e aplica `height: 100%` no elemento do gráfico. A regra anterior tentava definir a altura nesse mesmo elemento via CSS. O pai ficava sem altura definida, o que permitia que o SVG ultrapassasse o espaço do painel ao trocar o número de métricas. Agora o contêiner reserva a altura no fluxo, proporcional ao número de linhas de subgráficos. O Dash ajusta a figura ao contêiner, inclusive em tela cheia. Os painéis e as tabelas ocupam blocos separados. Tabelas largas mantêm a rolagem horizontal dentro do AG Grid.

## Estrutura e testes

| Arquivos | Responsabilidade |
| --- | --- |
| `app.py` | Servidor local, recortes ativos e callbacks. |
| `data_loader.py`, `analysis.py`, `classification.json` | Carregamento, validação, cálculos e classificação. |
| `charts.py`, `components/` | Figuras, páginas e componentes reutilizáveis. |
| `assets/` | Tema, formatação de tabela e tela cheia. |
| `tests/` | Integridade da coleta, cálculos, filtros, seleção, consistência visual e endpoints Dash. |
| `requirements.txt`, `.gitignore` | Dependências e exclusão do ambiente virtual e dos caches. |

Execute a suíte na pasta `dashboard`:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

