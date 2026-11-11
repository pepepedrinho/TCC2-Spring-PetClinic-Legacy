"""Cálculos e recortes da dashboard, independentes da interface."""

import numpy as np
import pandas as pd

from data_loader import METRICS

METRIC_LABELS = {
    "cbo": "CBO", "wmc": "WMC", "rfc": "RFC", "lcom": "LCOM", "lcom*": "LCOM*",
    "loc": "LOC", "fanin": "Fan-in", "fanout": "Fan-out", "dit": "DIT", "noc": "NOC",
    "methodsInvokedQty": "Invocações", "parametersQty": "Parâmetros",
    "comparisonsQty": "Comparações", "loopQty": "Laços", "tryCatchQty": "Blocos try/catch",
    "maxNestedBlocksQty": "Aninhamento máximo", "returnsQty": "Retornos",
}
DESCRIPTIONS = {
    "cbo": "Acoplamento entre objetos identificado estaticamente pela CK.",
    "wmc": "Complexidade ponderada dos métodos. No nível de método, a CK registra sua complexidade.",
    "rfc": "Resposta potencial do tipo ou do método conforme as invocações identificadas pela CK.",
    "lcom": "Falta de coesão em métodos, na escala original da métrica.",
    "lcom*": "Falta de coesão na variante normalizada registrada separadamente pela CK.",
    "loc": "Linhas de código reportadas pela CK.",
}
CLASS_METRICS = (*METRICS, "loc", "fanin", "fanout", "dit", "noc")
METHOD_METRICS = (
    "wmc", "rfc", "cbo", "loc", "methodsInvokedQty", "parametersQty", "comparisonsQty",
    "loopQty", "tryCatchQty", "maxNestedBlocksQty", "returnsQty",
)


def percent_change(before, after):
    """Variação relativa à origem. Zero ou ausência implica percentual indefinido."""
    if isinstance(before, pd.Series):
        return (after - before).div(before.where(before.ne(0))).mul(100)
    return (after - before) / before * 100 if pd.notna(before) and before != 0 else np.nan


def percent_share(part, total):
    return part / total * 100 if total else np.nan


def population(data, scenario, scope="complete"):
    rows = data.classes.loc[data.classes["scenario"].eq(scenario)]
    if scenario in ("A", "B") and scope == "equivalent":
        return rows.loc[rows["class"].isin(data.equivalent)]
    if scenario == "B" and scope == "additional":
        return rows.loc[rows["class"].isin(data.additional)]
    return rows


def filter_classes(rows, metric, packages=(), types=(), query="", categories=(), low=None, high=None):
    mask = rows["class"].str.contains((query or "").strip(), case=False, regex=False)
    for field, allowed in (("package", packages), ("type", types), ("category", categories)):
        if allowed:
            mask &= rows[field].isin(allowed)
    if low is not None:
        mask &= rows[metric].ge(low)
    if high is not None:
        mask &= rows[metric].le(high)
    return rows.loc[mask]


def pair_classes(data, original, metric, order="increase"):
    """Recebe o recorte de A. B acompanha os mesmos nomes, inclusive no filtro numérico."""
    original = original.loc[original["class"].isin(data.equivalent)]
    adapted = population(data, "B", "equivalent")[["class", metric, "type"]]
    fields = ["class", "type", "short", "package", "category", "key", metric]
    rows = original[fields].rename(columns={metric: "before"}).merge(
        adapted.rename(columns={metric: "after", "type": "type_b"}),
        on="class", validate="one_to_one",
    )
    rows["delta"] = rows["after"] - rows["before"]
    rows["percent"] = percent_change(rows["before"], rows["after"])
    rows["note"] = rows["class"].map(data.notes)
    return rows.sort_values(["delta", "class"], ascending=[order == "decrease", True])


def overview(data):
    a = population(data, "A")[list(METRICS)].mean()
    b = population(data, "B")[list(METRICS)].mean()
    paired = population(data, "B", "equivalent")[list(METRICS)].mean()
    return pd.DataFrame({
        "metric": [METRIC_LABELS[k] for k in METRICS],
        "before": a, "equivalent": paired, "complete": b,
        "delta_equivalent": paired - a, "delta_complete": b - a,
        "pct_equivalent": percent_change(a, paired), "pct_complete": percent_change(a, b),
    }).rename_axis("code").reset_index()


def describe(values):
    values = values.dropna()
    return {
        "n": len(values), "mean": values.mean(), "median": values.median(),
        "min": values.min(), "max": values.max(), "std": values.std(ddof=0),
        "q1": values.quantile(.25), "q3": values.quantile(.75),
    }


def rfc_decomposition(data):
    a = int(population(data, "A")["rfc"].sum())
    b = int(population(data, "B")["rfc"].sum())
    equivalent = int(population(data, "B", "equivalent")["rfc"].sum())
    additional = int(population(data, "B", "additional")["rfc"].sum())
    growth = b - a
    return {
        "original": a, "equivalent": equivalent, "additional": additional, "adapted": b,
        "growth": growth, "equivalent_growth": equivalent - a,
        "equivalent_pct": percent_share(equivalent - a, growth),
        "additional_pct": percent_share(additional, growth),
    }


def counterparts(data, keys):
    """Mantém somente observações existentes para cada identidade selecionada."""
    return data.classes.loc[data.classes["key"].isin(keys)].sort_values(["key", "scenario"])


def methods_for(data, rows):
    return data.methods.merge(rows[["scenario", "class", "short"]], on=["scenario", "class"],
                              how="inner", validate="many_to_one")


def selection_after_click(selection, key, visible_keys):
    if key not in visible_keys:
        return selection
    if key in selection:
        return [item for item in selection if item != key]
    return [*selection, key]
