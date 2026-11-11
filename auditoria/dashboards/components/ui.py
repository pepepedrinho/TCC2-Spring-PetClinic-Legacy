"""Controles, cartões, tabelas e gráficos reutilizados pelas páginas."""

import dash_ag_grid as dag
from dash import dcc, html
import pandas as pd

from analysis import CLASS_METRICS, DESCRIPTIONS, METHOD_METRICS, METRIC_LABELS
from data_loader import METRICS, SCENARIOS

PAGES = {
    "overview": ("01", "Visão geral", "Compare as populações e consulte a proveniência da coleta."),
    "pairs": ("02", "Comparação pareada", "Compare o monólito original com as classes equivalentes do monólito adaptado."),
    "additional": ("03", "Componentes adicionais", "Investigue os tipos introduzidos no monólito V2."),
    "classes": ("04", "Explorador de classes", "Filtre a população e compare os tipos em foco."),
    "methods": ("05", "Explorador de métodos", "Explore as assinaturas e métricas de método registradas pela CK."),
}
TYPE_FIELDS = {
    "class": "Classe", "scenario_label": "Cenário", "type": "Tipo CK",
    **{metric: METRIC_LABELS[metric] for metric in CLASS_METRICS},
    "category": "Categoria", "package": "Pacote Java",
}
PT_GRID = {
    "noRowsToShow": "Nenhum resultado", "filterOoo": "Filtrar…", "contains": "Contém",
    "notContains": "Não contém", "equals": "Igual a", "notEqual": "Diferente de",
    "blank": "Em branco", "notBlank": "Preenchido", "startsWith": "Começa com",
    "endsWith": "Termina com", "lessThan": "Menor que", "greaterThan": "Maior que",
    "lessThanOrEqual": "Menor ou igual", "greaterThanOrEqual": "Maior ou igual",
    "inRange": "No intervalo", "inRangeStart": "De", "inRangeEnd": "Até",
    "page": "Página", "of": "de", "to": "a", "pageSizeSelectorLabel": "Linhas por página",
    "firstPage": "Primeira página", "lastPage": "Última página", "nextPage": "Próxima página",
    "previousPage": "Página anterior", "ariaFilterMenuOpen": "Abrir filtro",
}


def number(value, digits=4, suffix=""):
    if pd.isna(value):
        return "—"
    return f"{value:.{digits}f}".rstrip("0").rstrip(".").replace(".", ",") + suffix if digits else f"{value:.0f}{suffix}"


def options(items):
    return [{"value": key, "label": label} for key, label in items]


def metric_options(keys):
    return [{"value": key, "label": METRIC_LABELS[key], "title": DESCRIPTIONS.get(key, METRIC_LABELS[key])}
            for key in keys]


def dropdown(id_, opts, value=None, **kwargs):
    return dcc.Dropdown(id=id_, options=opts, value=value, searchable=True, **kwargs)


def field(label, control):
    return html.Div([html.Label(label, htmlFor=control.id), control], className="field")


def layout(data):
    visible = data.classes
    unique = visible.drop_duplicates("key").sort_values(["short", "key"])
    names = options((row["key"], f"{row['short']} · {row['package']}") for row in unique.to_dict("records"))
    controls = [
        html.Div([
            field("Cenário", dropdown("scenario", options((k, v[0]) for k, v in SCENARIOS.items()),
                                      "B", clearable=False)),
            field("Recorte", dropdown("scope", [], "complete", clearable=False)),
        ], id="scenario-controls"),
        field("Métrica de tipo", dropdown("metric", metric_options(METRICS), "rfc", clearable=False)),
        html.Div(field("Métrica de método", dropdown("method-metric", metric_options(METHOD_METRICS),
                                                     "wmc", clearable=False)), id="method-controls"),
        field("Pacote Java", dropdown("packages", sorted(visible["package"].unique()), [],
                                      multi=True, placeholder="Todos os pacotes")),
        field("Tipo CK", dropdown("types", sorted(visible["type"].unique()), [],
                                  multi=True, placeholder="Todos os tipos")),
        field("Categoria arquitetural", dropdown("categories", sorted(visible["category"].unique()), [],
                                                 multi=True, placeholder="Todas as categorias")),
        field("Buscar classe", dcc.Input(id="query", type="search", value="", debounce=.25,
                                        placeholder="Nome simples ou qualificado")),
        html.Div([
            field("Valor mínimo", dcc.Input(id="low", type="number", step="any", placeholder="Sem mínimo")),
            field("Valor máximo", dcc.Input(id="high", type="number", step="any", placeholder="Sem máximo")),
        ], className="range"),
        html.Div(field("Ordem por Δ", dropdown("order", options([
            ("increase", "Maior aumento"), ("decrease", "Maior redução"),
        ]), "increase", clearable=False)), id="pair-controls"),
    ]
    return html.Div([
        html.A("Ir para o conteúdo", href="#conteudo", className="skip-link"),
        html.Aside([
            html.Div([html.Span("CK", className="brand-mark"), html.Div([
                html.Strong("Métricas CK"), html.Small("Spring PetClinic · TCC2"),
            ])], className="brand"),
            html.P("ANÁLISE", className="eyebrow"),
            html.Nav(dcc.RadioItems(
                id="page", value="overview", className="navigation",
                options=options((key, html.Span([html.Small(info[0]), info[1]])) for key, info in PAGES.items()),
            ), **{"aria-label": "Páginas da dashboard"}),
            html.Div([html.P("MÉTRICAS DA VISÃO GERAL", className="eyebrow"),
                      dcc.Checklist(id="overview-metrics", options=metric_options(METRICS), value=list(METRICS),
                                    className="metric-checklist")], id="overview-controls"),
            html.Div([html.P("FILTROS DA POPULAÇÃO", className="eyebrow"), *controls], id="population-controls"),
            html.Div([
                html.P("DESTAQUE", className="eyebrow"),
                field("Classes em foco", dropdown("selection", names, [], multi=True, placeholder="Selecionar classes")),
                html.Button("Limpar seleção", id="clear-selection", className="secondary"),
                html.Button("Restaurar filtros e seleção", id="reset", className="secondary"),
            ], className="selection-controls"),
        ], className="sidebar"),
        html.Main(id="conteudo", children=[
            html.Div([html.Span("ESTUDO DE CASO / STRANGLER FIG"),
                      html.Span("V2 CONSOLIDADA", className="badge")], className="topline"),
            html.Div(id="page-content"),
        ], className="main"),
    ], className="shell")


def card(title, value, hint=""):
    return html.Div([html.P(title), html.Strong(value), html.Small(hint)], className="kpi")


def panel(title, content, caption=None, class_name=""):
    return html.Section([html.H2(title), html.P(caption, className="muted") if caption else None, content],
                        className=f"panel {class_name}")


def graph(name, figure, clickable=False):
    ident = {"kind": "class-point", "name": name} if clickable else f"figure-{name}"
    height = figure.layout.height
    figure.update_layout(height=None, autosize=True)
    return html.Div([
        html.Button("Tela cheia", type="button", className="secondary fullscreen-button"),
        html.Div(dcc.Graph(id=ident, figure=figure, responsive=True, config={
            "displaylogo": False, "scrollZoom": False, "modeBarButtonsToRemove": ["lasso2d", "select2d"],
            "toImageButtonOptions": {"format": "png", "filename": f"ck-{name}", "scale": 2},
        }), className="graph-frame", style={"--chart-height": f"{height}px"}),
    ], className="graph-shell")


def grid(name, rows, fields, interactive=False, selection=(), filters=False, help_text=None):
    numeric = set(rows.select_dtypes(include="number").columns)
    defs = []
    for key, label in fields.items():
        wide = key in {"method", "note"}
        column = {
            "field": key, "headerName": label,
            "headerTooltip": DESCRIPTIONS.get(key, label),
            "minWidth": 300 if wide else max(100, min(210, len(label) * 9 + 28)),
            "flex": 3 if wide else 1,
        }
        if key == "class":
            longest = max((len(name.rsplit(".", 1)[-1]) for name in rows[key]), default=0)
            column.update(minWidth=max(170, longest * 7 + 36), flex=2, tooltipField=key,
                          valueFormatter={"function": "formatClassName(params)"})
        elif wide:
            column["tooltipField"] = key
        if key in numeric:
            column["type"] = "numericColumn"
            column["valueFormatter"] = {"function": "formatMetric(params)"}
        if filters:
            column["filter"] = "agNumberColumnFilter" if key in numeric else "agTextColumnFilter"
        defs.append(column)
    fields_in_data = list(fields)
    if interactive:
        fields_in_data = [*fields_in_data, "key"]
    records = rows[fields_in_data].astype(object).where(rows[fields_in_data].notna(), None).to_dict("records")
    if interactive:
        defs.insert(0, {"field": "focused", "headerName": "Foco", "width": 72, "sortable": False})
        for row in records:
            row["focused"] = "✓" if row["key"] in selection else ""
    size = min(12, len(records))
    return html.Div([
        html.Div([
            html.P(help_text or f"{len(records)} registro(s) no recorte.", className="muted"),
            html.Button("Exportar CSV", id={"kind": "export", "name": name}, className="secondary"),
        ], className="table-bar"),
        dag.AgGrid(
            id={"kind": "table", "name": name}, rowData=records, columnDefs=defs,
            className=f"ag-theme-alpine-dark{' interactive-grid' if interactive else ''}",
            style={"height": f"{85 + max(size, 2) * 37}px", "width": "100%"},
            defaultColDef={"sortable": True, "resizable": True},
            getRowId="params.data.key" if interactive else None,
            rowClassRules={"row-focused": "params.data && params.data.focused === '✓'"} if interactive else {},
            dashGridOptions={"pagination": True, "paginationPageSize": 12, "paginationPageSizeSelector": False,
                             "rowHeight": 37, "headerHeight": 40, "animateRows": False,
                             "tooltipShowDelay": 300, "localeText": PT_GRID, "suppressCellFocus": False},
        ),
        dcc.Download(id={"kind": "csv", "name": name}),
    ], className="table-shell")


