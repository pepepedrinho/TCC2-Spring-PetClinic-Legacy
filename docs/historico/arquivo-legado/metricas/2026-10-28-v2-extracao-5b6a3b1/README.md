# Artefatos CK e evidências V2

- `baseline-recoletada/`, `monolito/`, `owners/`, `gateway/`: CSVs brutos. As quatro saídas da ferramenta eram idênticas byte a byte e estão reunidas em [coleta.txt](coleta.txt).
- `tabela-1` a `tabela-5`: resultados consolidados; `estatisticas-descritivas.csv`: média, mediana, mínimo, máximo e desvio padrão populacional por componente/métrica.
- `relatorio.md`: cinco tabelas, fronteiras das comparações e interpretação.
- `proveniencia.json`: checkpoint avaliado, argumentos, versão e SHA-256 do CK. JAR local: CK 0.7.1-SNAPSHOT; HEAD da cópia-fonte CK: `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`.
- `fontes-avaliadas.json`: SHA-256 de cada fonte Java medida; os caminhos nos CSVs permanecem brutos, inclusive o checkout temporário da baseline.
- `evidencias/`: XML e texto Surefire, builds, resultado HTTP, migração, verificação final e primeira falha de persistência com write delay padrão.
- `coletar.ps1` e `analisar.py`: entradas locais para os scripts canônicos em `scripts/`.

Para recalcular tabelas de uma execução do coletor histórico no checkout atual, passe a pasta gerada em `.work/`:

```powershell
python historico/metricas/2026-10-28-v2-extracao-5b6a3b1/analisar.py .work/ck-<identificador>
```

Para usar o coletor histórico na árvore atual, exija o mesmo hash do JAR. Ele mede os fontes presentes no checkout, portanto a saída representa a versão executada. Para consultar o procedimento no checkpoint arquivado, use o commit `d3fea1a`:

```powershell
./historico/metricas/2026-10-28-v2-extracao-5b6a3b1/coletar.ps1 -CkJar 'C:\TCC2\ferramentas\ck\target\ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar' -Java17Home 'C:\Program Files\Java\jdk-17'
```

Pré-requisitos: PowerShell 7, Python 3, Git e JDK 17. O CK analisa fontes sem compilar. As métricas A/B/C são coletadas separadamente de `src/main/java`; Gateway é uma quarta população. A nova saída fica em `.work/` e a análise consulta a baseline original `historico/metricas/baseline-original-c36452a/`. Os CSVs da execução local usam a codificação padrão do JAR/Windows; a análise aceita UTF-8 e CP1252 sem reescrevê-los.

A branch anterior não versionou scripts/parâmetros originais; os parâmetros reconstruídos (`false 0 false`) foram validados pela igualdade de TODOS os campos CK da recoleta com a baseline preservada, exceto caminhos. O relatório apresenta esse limite de proveniência explicitamente. `53 passaram + 1 ignorado` são contagens Surefire; 57 verificações HTTP incluem prontidão e não equivalem a cobertura.

A campanha arquivada mantém a observação inicial. A nova execução gera suas próprias evidências. Nenhuma conclusão de melhoria global decorre automaticamente das médias.
