"""Servidor local e callbacks Dash da dashboard CK."""

import argparse
from pathlib import Path

from dash import ALL, MATCH, Dash, Input, Output, State, ctx, dcc, html, no_update
from dash.exceptions import PreventUpdate
import pandas as pd

from analysis import filter_classes, population, selection_after_click
from components import pages, ui
from data_loader import METRICS, load_collection

SCOPES = {
    "A": ("complete", "equivalent"),
    "B": ("complete", "equivalent", "additional"),
    "C": ("complete",),
}
DEFAULTS = {
    "scenario": "B", "metric": "rfc", "method-metric": "wmc",
    "packages": [], "types": [], "query": "", "categories": [], "low": None, "high": None,
    "order": "increase", "overview-metrics": list(METRICS),
}


def render_page(data, page, scenario, scope, metric, method_metric,
                packages, types, query, categories, low, high, order, enabled, selection):
    if page == "overview":
        return pages.overview_page(data, enabled)
    scope = scope if scope in SCOPES[scenario] else "complete"
    if page == "pairs":
        scenario, scope = "A", "equivalent"
    elif page == "additional":
        scenario, scope = "B", "additional"
    base = population(data, scenario, scope)
    visible = filter_classes(base, metric, packages, types, query, categories, low, high)
    selection = selection or []
    if page == "pairs":
        content = pages.pairs_page(data, base, visible, metric, selection, order)
    elif page == "additional":
        content = pages.additional_page(data, base, visible, metric, selection)
    elif page == "classes":
        content = pages.classes_page(data, base, visible, metric, selection, scenario, scope)
    else:
        content = pages.methods_page(data, base, visible, selection, scenario, scope, method_metric)
    if low is not None and high is not None and low > high:
        content.insert(2, html.P("O valor mínimo deve ser menor ou igual ao máximo.",
                                className="range-error", role="alert"))
    return content


def create_app(data=None):
    data = load_collection() if data is None else data
    keys = set(data.classes["key"])
    app = Dash(
        __name__, title="Métricas CK · Spring PetClinic", update_title=None,
        assets_folder=str(Path(__file__).resolve().parent / "assets"),
        suppress_callback_exceptions=True,
        meta_tags=[{"name": "viewport", "content": "width=device-width, initial-scale=1"}],
    )
    app.index_string = app.index_string.replace("<html>", '<html lang="pt-BR">')
    app.layout = ui.layout(data)

    @app.callback(
        Output("page-content", "children"),
        Input("page", "value"), Input("scenario", "value"), Input("scope", "value"),
        Input("metric", "value"), Input("method-metric", "value"), Input("packages", "value"),
        Input("types", "value"), Input("query", "value"), Input("categories", "value"),
        Input("low", "value"), Input("high", "value"), Input("order", "value"),
        Input("overview-metrics", "value"), Input("selection", "value"),
    )
    def update_page(*controls):
        return render_page(data, *controls)

    @app.callback(
        Output("overview-controls", "hidden"), Output("population-controls", "hidden"),
        Output("scenario-controls", "hidden"), Output("method-controls", "hidden"),
        Output("pair-controls", "hidden"), Input("page", "value"),
    )
    def show_controls(page):
        return page != "overview", page == "overview", page in {"pairs", "additional"}, page != "methods", page != "pairs"

    @app.callback(
        Output("scope", "options"), Output("scope", "value"),
        Input("scenario", "value"), Input("reset", "n_clicks"), State("scope", "value"),
    )
    def update_scope(scenario, _reset, scope):
        available = SCOPES[scenario]
        selected = "complete" if ctx.triggered_id == "reset" or scope not in available else scope
        return ui.options((key, pages.scope_label(scenario, key)) for key in available), selected

    @app.callback(
        [Output(control, "value") for control in DEFAULTS],
        Input("reset", "n_clicks"), prevent_initial_call=True,
    )
    def reset_filters(_clicks):
        return list(DEFAULTS.values())

    @app.callback(
        Output("selection", "value"),
        Input({"kind": "class-point", "name": ALL}, "clickData"),
        Input({"kind": "table", "name": ALL}, "cellClicked"),
        Input("clear-selection", "n_clicks"), Input("reset", "n_clicks"),
        State("selection", "value"), prevent_initial_call=True,
    )
    def update_selection(_points, _cells, _clear, _reset, selection):
        trigger = ctx.triggered_id
        if trigger in ("clear-selection", "reset"):
            return []
        if not isinstance(trigger, dict):
            raise PreventUpdate
        source = 0 if trigger["kind"] == "class-point" else 1
        event = next((entry.get("value") for entry in ctx.inputs_list[source] if entry["id"] == trigger), None)
        if not event:
            raise PreventUpdate
        if source == 0:
            custom = event["points"][0].get("customdata")
            key = custom[0] if custom else None
        else:
            key = event.get("rowId")
        current = selection or []
        new = selection_after_click(current, key, keys)
        return new if new != current else no_update

    @app.callback(
        Output({"kind": "csv", "name": MATCH}, "data"),
        Input({"kind": "export", "name": MATCH}, "n_clicks"),
        State({"kind": "table", "name": MATCH}, "virtualRowData"),
        State({"kind": "table", "name": MATCH}, "rowData"),
        State({"kind": "table", "name": MATCH}, "columnDefs"),
        prevent_initial_call=True,
    )
    def export_csv(clicks, filtered, rows, columns):
        if not clicks:
            raise PreventUpdate
        labels = {c["field"]: c["headerName"] for c in columns if c["field"] != "focused"}
        if "class" in labels:
            labels["class"] = "Nome Java qualificado"
        table = pd.DataFrame(filtered if filtered is not None else rows, columns=list(labels)).rename(columns=labels)
        return dcc.send_string("\ufeff" + table.to_csv(index=False), f"ck-{ctx.triggered_id['name']}.csv")

    return app


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Inicia a dashboard CK localmente.")
    parser.add_argument("--port", type=int, default=8050, help="Porta HTTP local (padrão: 8050).")
    args = parser.parse_args()
    create_app().run(host="127.0.0.1", port=args.port, debug=False)
