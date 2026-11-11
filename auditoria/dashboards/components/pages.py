"""Composição das cinco páginas e explicitação da população analisada."""

from dash import html
import pandas as pd

from analysis import (DESCRIPTIONS, METHOD_METRICS, METRIC_LABELS, counterparts, describe,
                      methods_for, overview, pair_classes, percent_share, population, rfc_decomposition)
import charts
from components.ui import PAGES, TYPE_FIELDS, card, graph, grid, number, panel
from data_loader import ADAPTED, EQUIVALENT_TYPES, METRICS, ORIGINAL, OWNERS, SCENARIOS

STATS = {"n": "Tipos", "mean": "Média", "median": "Mediana", "min": "Mínimo", "q1": "Q1",
         "q3": "Q3", "max": "Máximo", "std": "Desvio-padrão populacional"}
SCOPE_LABELS = {"complete": "Todos os tipos", "equivalent": EQUIVALENT_TYPES, "additional": "Tipos adicionais"}
INTERPRETATION = (
    "A CK mede propriedades estáticas do código Java. As mudanças de média descrevem o recorte observado. "
    "A interpretação arquitetural requer considerar a redistribuição de responsabilidades e a comunicação HTTP."
)
SHARES = (
    "As participações usam somas de valores por tipo. CBO e RFC podem contar relações ou invocações "
    "repetidas entre tipos. LCOM e LCOM* têm escalas distintas."
)


def header(page):
    index, title, subtitle = PAGES[page]
    return html.Header([html.P(f"PAINEL {index}", className="eyebrow"), html.H1(title), html.P(subtitle)])


def scope_label(scenario, scope):
    if scenario == "A" and scope == "equivalent":
        return "Classes correspondentes no original"
    return SCOPE_LABELS[scope]


def overview_page(data, enabled):
    means = overview(data)
    rows = means.loc[means["code"].isin(enabled or ())]
    original_count = len(population(data, "A"))
    adapted_count = len(population(data, "B"))
    equivalent_count = len(data.equivalent)
    owners = population(data, "C")
    comparisons = []
    if rows.empty:
        comparisons.append(panel("Métricas da visão geral", html.P(
            "Selecione pelo menos uma métrica no filtro lateral para exibir os gráficos e as tabelas.",
            role="status",
        )))
    else:
        mean_fig, comp_fig = charts.overview_figures(rows)
        metrics = rows["code"].tolist()
        comparisons = [
            panel("Médias dos monólitos", graph("medias", mean_fig),
                  f"Cada métrica tem seu próprio eixo. As classes equivalentes são os {equivalent_count} tipos do "
                  "monólito adaptado com correspondência no monólito original."),
            panel("Efeito da composição", html.Div([
                graph("composicao", comp_fig),
                grid("composicao", rows, {
                    "metric": "Métrica", "before": ORIGINAL, "equivalent": EQUIVALENT_TYPES,
                    "complete": ADAPTED, "delta_equivalent": "Equivalentes Δ", "pct_equivalent": "Equivalentes Δ (%)",
                    "delta_complete": "Adaptado Δ", "pct_complete": "Adaptado Δ (%)",
                }),
            ]), "As diferenças usam a média do monólito original como referência. A análise pareada usa "
                f"{equivalent_count} tipos de cada monólito. A comparação integral usa os "
                f"{original_count} tipos do original e os {adapted_count} do adaptado."),
            panel(OWNERS, grid("owners", pd.DataFrame([owners[metrics].mean()]), {
                m: METRIC_LABELS[m] for m in metrics
            }), f"Médias dos {len(owners)} tipos do microsserviço, analisados separadamente."),
        ]
    provenance = data.provenance
    scenarios = provenance["scenarios"]
    meta = [
        ("Código analisado na V2", scenarios[SCENARIOS["B"][1]].get("revision")),
        ("Código do cenário A", scenarios[SCENARIOS["A"][1]].get("revision")),
        ("Data de coleta (UTC)", provenance.get("collectedUtc")),
        ("Versão CK", provenance.get("ckVersion")),
        ("SHA-256 do JAR CK", provenance.get("ckSha256")),
        ("Parâmetros CK", " ".join(map(str, provenance.get("parameters", [])))),
        ("Integridade", f"{len(data.sources)} CSVs carregados e validados"),
    ]
    return [
        header("overview"),
        html.Div([
            card(ORIGINAL, original_count, "Cenário A"),
            card(ADAPTED, adapted_count, f"Cenário B · {equivalent_count} classes equivalentes"),
            card("Tipos adicionais", len(data.additional), "Recorte do monólito adaptado"),
            card(OWNERS, len(owners), "Cenário analisado separadamente"),
        ], className="kpis"),
        *comparisons,
        panel("Arquitetura e proveniência", html.Div([
            html.P("Na V2, o monólito mantém Pets e Visits e acessa Owners por HTTP. O Gateway encaminha as "
                   "requisições. Seu único tipo Java CK fica fora das comparações porque a medição desse tipo "
                   "tem alcance limitado para descrever a complexidade do roteamento."),
            html.P(INTERPRETATION),
            html.Dl([html.Div([html.Dt(key), html.Dd(value or "—")]) for key, value in meta], className="meta"),
        ])),
    ]


def scope_banner(base, visible, selection, caption):
    focused = len(set(selection) & set(visible["key"]))
    outside = len(selection) - focused
    message = f"{focused} em foco neste recorte"
    if outside:
        message += f" · {outside} fora do recorte"
    return html.Div([
        html.Div([html.Strong(f"{len(visible)} / {len(base)} tipos"), html.P(caption)]),
        html.Span(message, className="focus-badge", role="status"),
    ], className="scope")


def stats_cards(values, metric):
    stats = describe(values)
    return html.Div([
        card("Média", number(stats["mean"]), METRIC_LABELS[metric]),
        card("Mediana", number(stats["median"]), f"Q1 {number(stats['q1'])} · Q3 {number(stats['q3'])}"),
        card("Mínimo / máximo", f"{number(stats['min'])} / {number(stats['max'])}", "Valores observados"),
        card("Desvio-padrão", number(stats["std"]), f"Populacional · {stats['n']} tipo(s)"),
    ], className="kpis")


def pairs_page(data, base, visible, metric, selection, order):
    pairs = pair_classes(data, visible, metric, order)
    comparison = pd.DataFrame([{"scenario": label, **describe(pairs[field])}
                               for field, label in (("before", ORIGINAL), ("after", EQUIVALENT_TYPES))])
    note_rows = pairs.loc[pairs["note"].notna()]
    return [
        header("pairs"),
        scope_banner(base, pairs, selection, "Pares por nome qualificado. O tipo CK e o intervalo usam o monólito original."),
        html.Div([
            panel(f"{METRIC_LABELS[metric]} · Diferenças por tipo",
                  graph("diferencas", charts.differences(pairs, metric, selection), clickable=True),
                  "Clique na barra ou no ponto em zero para alternar o destaque."),
            panel(f"{METRIC_LABELS[metric]} · Original × adaptado",
                  graph("dispersao", charts.paired_scatter(pairs, metric, selection), clickable=True),
                  "Acima de Y = X há aumento. Pontos com os mesmos valores podem se sobrepor."),
        ], className="pair-layout"),
        panel("Valores e variações dos pares", grid("pares", pairs, {
            "class": TYPE_FIELDS["class"], "type": "Tipo original", "type_b": "Tipo adaptado",
            "before": ORIGINAL, "after": EQUIVALENT_TYPES,
            "delta": "Δ adaptado − original", "percent": "Δ (%)", "note": "Mudança de responsabilidade",
        }, interactive=True, selection=selection,
            help_text=f"{len(pairs)} pares · Clique nas linhas para comparar em foco."),
              "A variação percentual usa o valor do monólito original. A tabela exibe — quando esse valor é zero."),
        panel("Estatística do recorte pareado",
              grid("estatistica-pares", comparison, {"scenario": "Cenário", **STATS})),
        panel("Responsabilidades e interpretação", html.Div([
            html.P(INTERPRETATION),
            html.Ul([html.Li([html.Strong(f"{row['short']} · "), row["note"]])
                     for row in note_rows.to_dict("records")]) if len(note_rows) else None,
        ])),
    ]


def additional_page(data, base, visible, metric, selection):
    d = rfc_decomposition(data)
    focused = visible.loc[visible["key"].isin(selection)].copy()
    detailed = visible.assign(
        pct_additional=visible["rfc"].map(lambda r: percent_share(r, d["additional"])),
        pct_growth=visible["rfc"].map(lambda r: percent_share(r, d["growth"])),
        pct_adapted=visible["rfc"].map(lambda r: percent_share(r, d["adapted"])),
    )
    contribution = pd.DataFrame([
        {"group": "Variação nas classes equivalentes", "rfc": d["equivalent_growth"], "pct": d["equivalent_pct"]},
        {"group": "Tipos adicionais", "rfc": d["additional"], "pct": d["additional_pct"]},
    ])
    caption = "Tipos do monólito adaptado sem nome qualificado correspondente no monólito original."
    return [
        header("additional"), scope_banner(base, visible, selection, caption),
        panel(f"{METRIC_LABELS[metric]} · Tipos adicionais",
              graph("adicionais", charts.class_bars(visible, metric, selection), clickable=True),
              DESCRIPTIONS.get(metric)),
        panel("Contribuição individual no RFC", grid("contribuicao", detailed, {
            "class": TYPE_FIELDS["class"], "category": "Categoria", "rfc": "RFC",
            "pct_additional": "% dos adicionais", "pct_growth": "% da diferença total", "pct_adapted": "% do adaptado",
        }, interactive=True, selection=selection),
              "Os denominadores usam a coleta completa. Os filtros restringem as linhas exibidas."),
        panel("Decomposição do RFC", html.Div([
            html.Div([
                card(ORIGINAL, d["original"], "Soma por tipo"),
                card(EQUIVALENT_TYPES, d["equivalent"], "Recorte do monólito adaptado"),
                card("Tipos adicionais", d["additional"], "Recorte do monólito adaptado"),
                card(ADAPTED, d["adapted"], f"Δ de {d['growth']:+d} sobre o original"),
            ], className="kpis"),
            graph("decomposicao-rfc", charts.rfc_waterfall(d)),
            grid("crescimento-rfc", contribution, {"group": "Componente da diferença", "rfc": "Δ soma RFC",
                                                   "pct": "% da diferença total"}),
            html.P(f"Em foco no recorte: soma RFC {int(focused['rfc'].sum())}, "
                   f"{number(percent_share(focused['rfc'].sum(), d['growth']), 2, '%')} da diferença total."
                   if len(focused) else "Selecione tipos para calcular sua contribuição conjunta."),
        ]), "Identidade aritmética da coleta integral baseada na soma dos RFCs por tipo. "
            "Chamadas únicas do sistema e efeitos causais exigem outras medidas."),
    ]


def selected_detail(data, focused, metric, scenario):
    if focused.empty:
        return panel("Comparação em foco", html.P("Clique em uma linha ou ponto para investigar um tipo."))
    related = counterparts(data, focused["key"])
    total = population(data, scenario)
    ranking = total.set_index("key")[metric].rank(method="min", ascending=False)
    details = focused.copy()
    details["rank"] = details["key"].map(ranking)
    details["share"] = details[metric].map(lambda value: percent_share(value, total[metric].sum()))
    detail_fields = {"class": TYPE_FIELDS["class"], "category": "Categoria", metric: METRIC_LABELS[metric],
                     "rank": "Posição", "share": "% da soma"}
    return panel("Comparação em foco", html.Div([
        grid("contrapartes", related, {k: TYPE_FIELDS[k] for k in ["class", "scenario_label", "category", *METRICS]}),
        html.Ul([html.Li([html.Strong(f"{r['short']} · "), data.notes[r["class"]]])
                 for r in focused.to_dict("records") if r["class"] in data.notes]),
        html.H3(f"Posição no cenário completo · {len(total)} tipos"),
        grid("posicao", details, detail_fields),
        html.P(SHARES, className="muted"),
        html.P("Use o Explorador de métodos para investigar as assinaturas das classes em foco."),
    ]), "Cada linha corresponde a uma observação existente. As contrapartes A/B usam o nome Java qualificado.")


def classes_page(data, base, visible, metric, selection, scenario, scope):
    focused = visible.loc[visible["key"].isin(selection)]
    box, hist = charts.distributions(visible, metric, selection)
    return [
        header("classes"), scope_banner(base, visible, selection, f"{SCENARIOS[scenario][0]} · {scope_label(scenario, scope)}"),
        stats_cards(visible[metric], metric),
        panel("Tipos CK", grid("classes", visible, TYPE_FIELDS, interactive=True, selection=selection,
                              help_text=f"{len(visible)} tipos · Clique em várias linhas para compará-las."),
              "Os controles laterais filtram a tabela e as distribuições. Os cabeçalhos ordenam os valores."),
        selected_detail(data, focused, metric, scenario),
        html.Div([
            panel(f"{METRIC_LABELS[metric]} · Boxplot", graph("boxplot", box, clickable=True),
                  "Clique em um ponto para localizar o tipo no restante da página."),
            panel(f"{METRIC_LABELS[metric]} · Histograma", graph("histograma", hist),
                  "As faixas mostram a frequência. A cor âmbar indica os tipos em foco."),
        ], className="split"),
    ]


def methods_page(data, base, visible, selection, scenario, scope, metric):
    focused = visible.loc[visible["key"].isin(selection)]
    intro = [
        header("methods"), scope_banner(base, visible, selection, f"{SCENARIOS[scenario][0]} · {scope_label(scenario, scope)}"),
    ]
    if focused.empty:
        return [*intro, panel("Escolha as classes a investigar",
                              html.P("Adicione uma classe do recorte em Classes em foco ou selecione uma linha "
                                     "no Explorador de classes."))]
    rows = methods_for(data, focused)
    fields = {"class": TYPE_FIELDS["class"], "method": "Assinatura CK", "line": "Linha", "constructor": "Construtor",
              **{m: METRIC_LABELS[m] for m in METHOD_METRICS}}
    return [*intro,
            panel("Métodos das classes em foco", grid("metodos", rows, fields, filters=True),
                  f"{len(rows)} métodos de {len(focused)} tipo(s) em foco. A identificação combina classe, assinatura "
                  "e linha. As métricas de método têm definição própria e sua soma pode divergir da métrica de tipo."),
            panel(f"{METRIC_LABELS[metric]} · Até 15 métodos com maior valor",
                  graph("ranking-metodos", charts.method_ranking(rows, metric)),
                  "O ranking usa as classes em foco do cenário e do recorte indicados acima.")]

