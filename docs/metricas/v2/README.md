# Artefatos CK e evidências V2

- `baseline-recoletada/`, `monolito/`, `owners/`, `gateway/`: CSVs brutos e saída da ferramenta.
- `tabela-1` a `tabela-5`: resultados consolidados; `estatisticas-descritivas.csv`: média, mediana, mínimo, máximo e desvio padrão populacional por componente/métrica.
- `relatorio.md`: cinco tabelas, fronteiras das comparações e interpretação.
- `proveniencia.json`: checkpoint avaliado, argumentos, versão e SHA-256 do CK. JAR local: CK 0.7.1-SNAPSHOT; HEAD da cópia-fonte CK: `8a1ef916d597d33ad619d748b616bb7dbc3f0c36`.
- `fontes-avaliadas.json`: SHA-256 de cada fonte Java medida; os caminhos nos CSVs permanecem brutos, inclusive o checkout temporário da baseline.
- `evidencias/`: XML e texto Surefire, builds, resultado HTTP, migração, verificação final e primeira falha de persistência com write delay padrão.
- `coletar.ps1` e `analisar.py`: entradas locais para os scripts canônicos em `scripts/`.

Na raiz, para repetir a análise dos CSVs existentes:

```powershell
python docs/metricas/v2/analisar.py
```

Para repetir a coleta, exigindo o mesmo hash do JAR:

```powershell
./docs/metricas/v2/coletar.ps1 -CkJar 'C:\TCC2\ferramentas\ck\target\ck-0.7.1-SNAPSHOT-jar-with-dependencies.jar' -Java17Home 'C:\Program Files\Java\jdk-17'
```

Pré-requisitos: PowerShell 7, Python 3, Git e JDK 17. O CK analisa fontes sem compilar. As métricas A/B/C são coletadas separadamente de `src/main/java`; Gateway é uma quarta população. O script arquiva exclusivamente as fontes históricas em `.work/` e não faz checkout nem altera a baseline original `docs/baseline/`. Os CSVs da execução local usam a codificação padrão do JAR/Windows; a análise aceita UTF-8 e CP1252 sem reescrevê-los.

A branch anterior não versionou scripts/parâmetros originais; os parâmetros reconstruídos (`false 0 false`) foram validados pela igualdade de TODOS os campos CK da recoleta com a baseline preservada, exceto caminhos. O relatório apresenta esse limite de proveniência explicitamente. `53 passaram + 1 ignorado` são contagens Surefire; 57 verificações HTTP incluem prontidão e não equivalem a cobertura.

Reexecutar coleta/testes atualiza evidências V2; preserve o commit publicado desta entrega para manter a observação inicial. Nenhuma conclusão de melhoria global decorre automaticamente das médias.
