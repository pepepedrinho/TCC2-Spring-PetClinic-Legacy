"""Verificações do servidor Dash e da consistência da interação cruzada."""

import json
import unittest

from dash._utils import stringify_id

from app import DEFAULTS, create_app, render_page
from components.ui import TYPE_FIELDS, grid
from data_loader import ADAPTED, EQUIVALENT_TYPES, METRICS, ORIGINAL, load_collection


def find(components, id_):
    for component in components if isinstance(components, list) else [components]:
        if getattr(component, "id", None) == id_:
            return component
        children = getattr(component, "children", [])
        found = find(children, id_) if children is not None else None
        if found is not None:
            return found
    return None


class AppTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = load_collection()
        cls.app = create_app(cls.data)
        cls.client = cls.app.server.test_client()

    def render(self, page, **overrides):
        values = {**DEFAULTS, **overrides}
        return render_page(
            self.data, page, values["scenario"], values.get("scope", "complete"), values["metric"],
            values["method-metric"], values["packages"], values["types"], values["query"],
            values["categories"], values["low"], values["high"], values["order"],
            values["overview-metrics"], values.get("selection", []),
        )

    def callback(self, output, inputs, changed, state=None, outputs=None):
        id_, property_ = output.rsplit(".", 1)
        response = self.client.post("/_dash-update-component", json={
            "output": output, "outputs": outputs if outputs is not None else {"id": id_, "property": property_},
            "inputs": inputs, "state": state or [], "changedPropIds": [changed],
        })
        self.assertEqual(response.status_code, 200, response.get_data(as_text=True)[:500])
        return response.get_json()["response"]

    def test_server_and_render_callback(self):
        for route in ("/", "/_dash-layout", "/_dash-dependencies", "/assets/dashboard.css"):
            with self.client.get(route) as response:
                self.assertEqual(response.status_code, 200)
        values = {"page": "overview", **DEFAULTS, "scope": "complete", "selection": []}
        inputs = [{"id": item["id"], "property": item["property"], "value": values[item["id"]]}
                  for item in self.app.callback_map["page-content.children"]["inputs"]]
        result = self.callback("page-content.children", inputs, "page.value")
        self.assertIn("Visão geral", json.dumps(result, ensure_ascii=False))

    def test_pair_table_and_charts_use_same_names(self):
        layout = self.render("pairs", query="Controller", low=6, high=15,
                             packages=["org.springframework.samples.petclinic.owner"])
        table = find(layout, {"kind": "table", "name": "pares"})
        bar = find(layout, {"kind": "class-point", "name": "diferencas"})
        scatter = find(layout, {"kind": "class-point", "name": "dispersao"})
        keys = {row["key"] for row in table.rowData}
        self.assertEqual(len(keys), 2)
        self.assertEqual(keys, {point[0] for point in bar.figure.data[0].customdata})
        self.assertEqual(keys, {point[0] for trace in scatter.figure.data[1:] for point in trace.customdata})

    def test_class_table_and_boxplot_use_same_scope_and_focus(self):
        key = "AB|org.springframework.samples.petclinic.integration.OwnerIntegrationAdapter"
        layout = self.render("classes", scope="additional", query="Adapter", selection=[key])
        table = find(layout, {"kind": "table", "name": "classes"})
        box = find(layout, {"kind": "class-point", "name": "boxplot"})
        self.assertEqual([row["key"] for row in table.rowData], [key])
        self.assertEqual(table.rowData[0]["focused"], "✓")
        self.assertEqual([trace.customdata[0][0] for trace in box.figure.data[1:]], [key])
        related = find(layout, {"kind": "table", "name": "contrapartes"})
        self.assertEqual([row["scenario_label"] for row in related.rowData], ["Monólito adaptado"])

    def test_class_column_formats_only_display_and_retains_distinct_identities(self):
        rows = self.data.classes.loc[self.data.classes["short"].eq("Owner")
                                     & self.data.classes["scenario"].isin(["B", "C"])]
        table = find(grid("owners", rows, TYPE_FIELDS, interactive=True),
                     {"kind": "table", "name": "owners"})
        column = next(col for col in table.columnDefs if col["field"] == "class")
        self.assertEqual(column["headerName"], "Classe")
        self.assertEqual(column["valueFormatter"], {"function": "formatClassName(params)"})
        self.assertEqual(column["tooltipField"], "class")
        self.assertEqual(table.getRowId, "params.data.key")
        self.assertEqual(len({row["key"] for row in table.rowData}), 2)
        self.assertEqual({row["class"] for row in table.rowData},
                         {"org.springframework.samples.petclinic.owner.Owner", "br.edu.tcc.owners.Owner"})

    def test_overview_metric_counts_keep_figures_and_tables_in_sync(self):
        for count in (5, 4, 3, 2, 1, 0, 3, 5):
            with self.subTest(count=count):
                metrics = METRICS[:count]
                layout = self.render("overview", **{"overview-metrics": list(metrics)})
                means = find(layout, "figure-medias")
                changes = find(layout, "figure-composicao")
                table = find(layout, {"kind": "table", "name": "composicao"})
                owners = find(layout, {"kind": "table", "name": "owners"})
                if not count:
                    self.assertTrue(all(item is None for item in (means, changes, table, owners)))
                    self.assertIn("Selecione pelo menos uma métrica", str(layout))
                    continue
                labels = [m.upper() for m in metrics]
                self.assertEqual([a.text for a in means.figure.layout.annotations], labels)
                self.assertEqual([trace.name for trace in means.figure.data[:3]],
                                 [ORIGINAL, ADAPTED, EQUIVALENT_TYPES])
                self.assertTrue(all(list(trace.x) == labels for trace in changes.figure.data))
                self.assertEqual([row["metric"] for row in table.rowData], labels)
                self.assertEqual(list(owners.rowData[0]), list(metrics))
                self.assertIsNone(means.figure.layout.height)
                self.assertTrue(means.responsive)

    def test_method_analysis_requires_visible_selection(self):
        self.assertIsNone(find(self.render("methods"), {"kind": "table", "name": "metodos"}))
        key = "AB|org.springframework.samples.petclinic.integration.OwnerIntegrationAdapter"
        layout = self.render("methods", selection=[key])
        rows = find(layout, {"kind": "table", "name": "metodos"}).rowData
        self.assertGreater(len(rows), 0)
        self.assertEqual({row["class"] for row in rows}, {key.split("|")[1]})
        self.assertIsNone(find(self.render("methods", selection=[key], scenario="C"),
                               {"kind": "table", "name": "metodos"}))

    def test_clicks_and_clear_update_shared_selection(self):
        key = "AB|org.springframework.samples.petclinic.owner.PetController"
        chart = {"kind": "class-point", "name": "dispersao"}
        grid = {"kind": "table", "name": "classes"}
        state = [{"id": "selection", "property": "value", "value": []}]
        inputs = [
            [{"id": chart, "property": "clickData", "value": {"points": [{"customdata": [key]}]}}],
            [{"id": grid, "property": "cellClicked", "value": {"rowId": key}}],
            {"id": "clear-selection", "property": "n_clicks", "value": None},
            {"id": "reset", "property": "n_clicks", "value": None},
        ]
        for changed, expected in [
            (f"{stringify_id(chart)}.clickData", [key]),
            (f"{stringify_id(grid)}.cellClicked", []),
            (f"{stringify_id(grid)}.cellClicked", [key]),
            ("clear-selection.n_clicks", []),
        ]:
            with self.subTest(changed=changed):
                result = self.callback("selection.value", inputs, changed, state)
                self.assertEqual(result["selection"]["value"], expected)
                state[0]["value"] = expected

    def test_rendering_a_table_without_a_click_keeps_selection(self):
        grid = {"kind": "table", "name": "composicao"}
        response = self.client.post("/_dash-update-component", json={
            "output": "selection.value", "outputs": {"id": "selection", "property": "value"},
            "inputs": [
                [], [{"id": grid, "property": "cellClicked"}],
                {"id": "clear-selection", "property": "n_clicks", "value": None},
                {"id": "reset", "property": "n_clicks", "value": None},
            ],
            "state": [{"id": "selection", "property": "value", "value": []}],
            "changedPropIds": [f"{stringify_id(grid)}.cellClicked"],
        })
        self.assertEqual(response.status_code, 204)

    def test_gateway_is_absent_from_controls_and_primary_figures(self):
        scenario = find(self.app.layout, "scenario")
        self.assertEqual([o["value"] for o in scenario.options], ["A", "B", "C"])
        self.assertTrue(all(not o["value"].startswith("D|") for o in find(self.app.layout, "selection").options))
        self.assertIsNotNone(find(self.render("overview"), "figure-medias"))

    def test_additional_and_empty_recuts_render(self):
        visible = self.render("additional", categories=["Integração"])
        table = find(visible, {"kind": "table", "name": "contribuicao"})
        self.assertEqual(len(table.rowData), 3)
        figure = find(visible, "figure-decomposicao-rfc").figure
        self.assertEqual(list(figure.data[0].y), [83, 13, 30, 0])
        empty = self.render("classes", query="SemCorrespondencia")
        self.assertEqual(find(empty, {"kind": "table", "name": "classes"}).rowData, [])

