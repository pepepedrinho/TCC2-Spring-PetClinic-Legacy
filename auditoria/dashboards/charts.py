"""Figuras Plotly e identidade dos pontos selecionáveis."""

from math import ceil

import numpy as np
import plotly.graph_objects as go
from plotly.subplots import make_subplots

from analysis import METRIC_LABELS
from data_loader import ADAPTED, EQUIVALENT_TYPES, ORIGINAL

A, B, EQUIVALENT = "#62a6ff", "#38c9b3", "#ba9bfa"
HIGHLIGHT, INCREASE, DECREASE, NEUTRAL = "#ffc666", "#65a6ff", "#c29afa", "#8197b2"
BG, INK, GRID = "#151f2e", "#e7edf7", "#2b3a50"


def style(fig, height=345, **layout):
    fig.update_layout(
        template="plotly_dark", paper_bgcolor=BG, plot_bgcolor=BG,
        font=dict(family="Segoe UI, sans-serif", size=13, color=INK),
        margin=dict(l=30, r=22, t=30, b=40), height=height,
        legend=dict(orientation="h", yanchor="bottom", y=1.03, x=0),
        hoverlabel=dict(namelength=-1), separators=",.",
        **layout,
    )
    fig.update_xaxes(gridcolor=GRID, zerolinecolor="#5f7188", automargin=True)
    fig.update_yaxes(gridcolor=GRID, zerolinecolor="#5f7188", automargin=True)
    return fig


def empty(message="Nenhum tipo no recorte.", height=260):
    fig = style(go.Figure(), height)
    fig.add_annotation(text=message, x=.5, y=.5, xref="paper", yref="paper", showarrow=False)
    return fig.update_xaxes(visible=False).update_yaxes(visible=False)


def overview_figures(rows):
    if rows.empty:
        return empty("Selecione uma métrica."), empty("Selecione uma métrica.")
    groups = [("before", ORIGINAL, A), ("complete", ADAPTED, B),
              ("equivalent", EQUIVALENT_TYPES, EQUIVALENT)]
    columns = min(len(rows), 3)
    levels = ceil(len(rows) / columns)
    means = make_subplots(rows=levels, cols=columns, subplot_titles=rows["metric"].tolist())
    for i, item in enumerate(rows.to_dict("records")):
        r, c = i // columns + 1, i % columns + 1
        for field, label, color in groups:
            means.add_bar(x=[label], y=[item[field]], name=label, marker_color=color,
                          showlegend=i == 0, legendgroup=field,
                          hovertemplate=f"{item['metric']} · %{{y:.6g}}<extra>{label}</extra>",
                          row=r, col=c)
    means.update_xaxes(showticklabels=False)
    means.update_yaxes(rangemode="tozero")
    style(means, 220 * levels, bargap=.35)
    means.update_layout(legend=dict(yanchor="top", y=-.05))
    changes = go.Figure([
        go.Bar(x=rows["metric"].tolist(), y=rows[field].tolist(), name=label, marker_color=color,
               hovertemplate=f"%{{x}} · %{{y:+.2f}}%<br>Em relação ao {ORIGINAL.lower()}"
                             "<extra>%{fullData.name}</extra>")
        for field, label, color in [
            ("pct_equivalent", EQUIVALENT_TYPES, EQUIVALENT), ("pct_complete", ADAPTED, B),
        ]
    ])
    changes.update_yaxes(title="Variação da média", ticksuffix="%")
    style(changes, barmode="group")
    changes.update_layout(legend=dict(yanchor="top", y=-.12))
    return means, changes


def custom_types(rows):
    return rows[["key", "class"]].to_numpy().tolist()


def differences(rows, metric, selection):
    if rows.empty:
        return empty("Nenhum par no recorte.")
    colors = [HIGHLIGHT if key in selection else INCREASE if delta > 0 else DECREASE if delta < 0 else NEUTRAL
              for key, delta in zip(rows["key"], rows["delta"])]
    fig = go.Figure(go.Bar(
        x=rows["delta"].tolist(), y=rows["class"].tolist(), orientation="h",
        marker_color=colors, customdata=rows[["key", "class", "before", "after"]].to_numpy().tolist(),
        hovertemplate=f"%{{customdata[1]}}<br>{ORIGINAL}: %{{customdata[2]:.6g}}"
                      f"<br>{EQUIVALENT_TYPES}: %{{customdata[3]:.6g}}"
                      "<br>Δ %{x:+.6g}<extra></extra>",
    ))
    # Pontos em Δ = 0 permitem selecionar também os pares com barra de comprimento zero.
    same = rows.loc[rows["delta"].eq(0)]
    if not same.empty:
        fig.add_scatter(x=same["delta"].tolist(), y=same["class"].tolist(), mode="markers",
                        marker=dict(size=8, color=[HIGHLIGHT if k in selection else NEUTRAL for k in same["key"]]),
                        customdata=custom_types(same), showlegend=False,
                        hovertemplate="%{customdata[1]}<br>Δ = 0<extra></extra>")
    fig.update_yaxes(autorange="reversed", categoryorder="array", categoryarray=rows["class"].tolist(),
                     tickvals=rows["class"].tolist(), ticktext=rows["short"].tolist())
    fig.update_xaxes(title=f"Δ {METRIC_LABELS[metric]} (adaptado − original)")
    return style(fig, max(285, 65 + len(rows) * 24), showlegend=False)


def paired_scatter(rows, metric, selection):
    if rows.empty:
        return empty("Nenhum par no recorte.")
    lower = min(rows["before"].min(), rows["after"].min())
    upper = max(rows["before"].max(), rows["after"].max())
    padding = max((upper - lower) * .08, .1)
    bounds = [lower - padding, upper + padding]
    fig = go.Figure(go.Scatter(x=bounds, y=bounds, mode="lines", name="Y = X",
                               line=dict(color=NEUTRAL, dash="dash"), hoverinfo="skip"))
    unselected = rows.loc[~rows["key"].isin(selection)]
    for label, subset, color in (
        ("Aumento", unselected.loc[unselected["delta"].gt(0)], INCREASE),
        ("Redução", unselected.loc[unselected["delta"].lt(0)], DECREASE),
        ("Sem variação", unselected.loc[unselected["delta"].eq(0)], NEUTRAL),
        ("Selecionados", rows.loc[rows["key"].isin(selection)], HIGHLIGHT),
    ):
        if not subset.empty:
            fig.add_scatter(x=subset["before"].tolist(), y=subset["after"].tolist(), name=label,
                            mode="markers", customdata=custom_types(subset),
                            marker=dict(color=color, size=10, line=dict(width=1, color=BG)),
                            hovertemplate=f"%{{customdata[1]}}<br>{ORIGINAL}: %{{x:.6g}}"
                                          f"<br>{EQUIVALENT_TYPES}: %{{y:.6g}}<extra></extra>")
    fig.update_xaxes(title=ORIGINAL, range=bounds)
    fig.update_yaxes(title=EQUIVALENT_TYPES, range=bounds, scaleanchor="x", scaleratio=1)
    return style(fig, 390)


def class_bars(rows, metric, selection):
    if rows.empty:
        return empty()
    rows = rows.sort_values([metric, "class"], ascending=[False, True])
    colors = [HIGHLIGHT if key in selection else B for key in rows["key"]]
    fig = go.Figure(go.Bar(
        x=rows[metric].tolist(), y=rows["class"].tolist(), orientation="h", marker_color=colors,
        customdata=custom_types(rows), hovertemplate="%{customdata[1]}<br>%{x:.6g}<extra></extra>",
    ))
    zeros = rows.loc[rows[metric].eq(0)]
    if not zeros.empty:
        fig.add_scatter(x=zeros[metric].tolist(), y=zeros["class"].tolist(), mode="markers",
                        marker=dict(size=8, color=[HIGHLIGHT if k in selection else B for k in zeros["key"]]),
                        customdata=custom_types(zeros), hovertemplate="%{customdata[1]}<br>0<extra></extra>")
    fig.update_yaxes(autorange="reversed", categoryorder="array", categoryarray=rows["class"].tolist(),
                     tickvals=rows["class"].tolist(), ticktext=rows["short"].tolist())
    fig.update_xaxes(title=METRIC_LABELS[metric], rangemode="tozero")
    return style(fig, max(270, 85 + len(rows) * 24), showlegend=False)


def rfc_waterfall(result):
    fig = go.Figure(go.Waterfall(
        x=[ORIGINAL, f"Δ das {EQUIVALENT_TYPES.lower()}", "Tipos adicionais", ADAPTED],
        y=[result["original"], result["equivalent_growth"], result["additional"], 0],
        measure=["absolute", "relative", "relative", "total"], textposition="outside",
        text=[str(result["original"]), f'{result["equivalent_growth"]:+d}',
              f'{result["additional"]:+d}', str(result["adapted"])],
        increasing=dict(marker=dict(color=B)), decreasing=dict(marker=dict(color=DECREASE)),
        totals=dict(marker=dict(color=A)), connector=dict(line=dict(color=NEUTRAL)),
        hovertemplate="%{x}<br>%{text}<extra></extra>",
    ))
    fig.update_xaxes(tickvals=list(fig.data[0].x),
                     ticktext=["Monólito<br>original", "Δ das classes<br>equivalentes",
                               "Tipos<br>adicionais", "Monólito<br>adaptado"])
    fig.update_yaxes(title="Soma do RFC por tipo",
                     range=[min(0, result["equivalent"]), 1.17 * max(result["original"], result["adapted"])])
    return style(fig, 305, showlegend=False)


def distributions(rows, metric, selection, value_field=None):
    field = value_field or metric
    label = METRIC_LABELS[metric] if not value_field else f"{METRIC_LABELS[metric]} em {field}"
    rows = rows.dropna(subset=[field])
    if rows.empty:
        return empty(), empty()
    box = go.Figure(go.Box(x=rows[field].tolist(), y0=0, orientation="h", boxpoints=False,
                           quartilemethod="linear", name="População", marker_color=B))
    # Espalhamento vertical determinístico apenas para expor pontos com o mesmo valor.
    sorted_rows = rows.sort_values([field, "class"])
    for selected, group in sorted_rows.groupby(sorted_rows["key"].isin(selection), sort=True):
        offsets = group.groupby(field).cumcount() - (group.groupby(field)[field].transform("size") - 1) / 2
        box.add_scatter(
            x=group[field].tolist(), y=(offsets * .09 + (.48 if selected else .37)).tolist(),
            mode="markers", marker=dict(color=HIGHLIGHT if selected else A, size=8, opacity=.85),
            customdata=custom_types(group), hovertemplate="%{customdata[1]}<br>%{x:.6g}<extra></extra>",
        )
    box.update_yaxes(visible=False)
    box.update_xaxes(title=label)
    style(box, 235, showlegend=False)
    hist = go.Figure()
    if rows[field].nunique() == 1:
        edges = np.array([rows[field].min() - .5, rows[field].max() + .5])
    else:
        edges = np.histogram_bin_edges(rows[field], bins="sturges")
    for selected, group in rows.groupby(rows["key"].isin(selection), sort=True):
        hist.add_histogram(
            x=group[field].tolist(), name="Selecionados" if selected else "Demais tipos",
            xbins=dict(start=float(edges[0]), end=float(edges[-1]), size=float(edges[1] - edges[0])),
            bingroup="types", marker_color=HIGHLIGHT if selected else B,
            hovertemplate="Faixa %{x}<br>%{y} tipo(s)<extra>%{fullData.name}</extra>",
        )
    hist.update_xaxes(title=label)
    hist.update_yaxes(title="Tipos", dtick=1)
    return box, style(hist, 280, barmode="stack")


def method_ranking(methods, metric):
    if methods.empty:
        return empty("Nenhum método no recorte.")
    top = methods.sort_values([metric, "class", "line"], ascending=[False, True, True]).head(15)
    names = (top["short"] + " · " + top["method"] + " · L" + top["line"].astype(str)).tolist()
    fig = go.Figure(go.Bar(x=top[metric].tolist(), y=names, orientation="h", marker_color=B,
                           customdata=top[["class", "method", "line"]].to_numpy().tolist(),
                           hovertemplate="%{customdata[0]}<br>%{customdata[1]} · linha %{customdata[2]}"
                                         "<br>%{x:.6g}<extra></extra>"))
    fig.update_yaxes(autorange="reversed")
    fig.update_xaxes(title=METRIC_LABELS[metric], rangemode="tozero")
    return style(fig, max(285, 80 + 25 * len(top)))

