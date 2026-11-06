"""Regenera os cinco gráficos da coleta oficial CK com Pandas e Matplotlib."""

from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.patches import Patch
from matplotlib.ticker import FuncFormatter, MaxNLocator
import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
METRICS_DIR = ROOT / "docs/metricas"
ARCHIVE = ROOT / "historico/metricas/2026-11-06-v2-pre-docker-0ad652a"
OUTPUT = METRICS_DIR / "graficos"

METRICS = ["cbo", "wmc", "rfc", "lcom", "lcom*"]
CLASS_METRICS = ["cbo", "wmc", "rfc", "lcom*"]
BLUE, TEAL, GOLD = "#2457A6", "#087F79", "#A65315"
BEFORE, AFTER = "#64748B", TEAL
DOWN, UP = "#2457A6", "#B34821"
INK, MUTED, GRID = "#132238", "#485569", "#E2E8F0"

plt.rcParams.update({
    "font.family": "DejaVu Sans",
    "font.size": 11,
    "text.color": INK,
    "axes.labelcolor": MUTED,
    "xtick.color": MUTED,
    "ytick.color": MUTED,
    "figure.facecolor": "white",
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.spines.left": False,
    "axes.spines.bottom": False,
})


def read_types(path):
    data = pd.read_csv(path, encoding="cp1252", usecols=["class", *METRICS])
    types = data.set_index("class").astype(float)
    if types.empty or not types.index.is_unique or types.isna().any().any():
        raise ValueError(f"Coleta incompleta: {path}")
    return types


def number(value, digits):
    return f"{value:.{digits}f}".replace(".", ",")


def title(fig, heading, detail):
    fig.suptitle(heading, x=0.07, y=0.98, ha="left", fontsize=22, fontweight="bold")
    fig.text(0.07, 0.915, detail, color=MUTED, fontsize=11)


def save(fig, filename):
    fig.savefig(OUTPUT / filename, dpi=240)
    plt.close(fig)


def small_multiples(data, colors, heading, detail, filename, note, digits):
    rows = 2
    columns = (len(data.columns) + rows - 1) // rows
    fig, axes = plt.subplots(rows, columns, figsize=(13, 7.5), squeeze=False)
    fig.subplots_adjust(left=0.08, right=0.97, bottom=0.13, top=0.84, hspace=0.65, wspace=0.32)
    title(fig, heading, detail)
    for ax, key in zip(axes.flat, data.columns):
        values = data[key]
        bars = ax.bar(range(len(values)), values, width=0.55, color=colors)
        ax.set_title(key.upper(), loc="left", fontweight="bold")
        ax.set_xticks(range(len(values)), values.index, fontsize=9)
        ax.set_ylim(0, max(values.max() * 1.28, 0.01))
        ax.yaxis.set_major_locator(MaxNLocator(nbins=4, integer=digits[key] == 0))
        ax.yaxis.set_major_formatter(FuncFormatter(lambda value, _, precision=min(2, digits[key]): number(value, precision)))
        ax.grid(axis="y", color=GRID)
        ax.set_axisbelow(True)
        ax.bar_label(bars, labels=[number(v, digits[key]) for v in values], padding=4, fontsize=10)
    for ax in list(axes.flat)[len(data.columns):]:
        ax.set_visible(False)
    fig.text(0.07, 0.045, note, color=MUTED, fontsize=10)
    save(fig, filename)


def equivalent_changes(original, adapted):
    common = original.index.intersection(adapted.index)
    difference = adapted.loc[common, CLASS_METRICS] - original.loc[common, CLASS_METRICS]
    changed = difference.loc[difference.abs().gt(1e-9).any(axis=1)].copy()
    changed = changed.sort_index(key=lambda names: names.str.rsplit(".", n=1).str[-1])
    labels = changed.index.str.rsplit(".", n=1).str[-1]
    if labels.has_duplicates:
        labels = changed.index

    height = max(6.4, 2.3 + 0.39 * len(changed))
    fig, axes = plt.subplots(1, 4, sharey=True, figsize=(15, height))
    fig.subplots_adjust(left=0.19, right=0.975, bottom=0.15, top=0.83, wspace=0.25)
    title(
        fig,
        "Onde as classes equivalentes mudaram",
        f"Diferença B − A · {len(changed)} de {len(common)} tipos comuns mudaram em pelo menos um indicador exibido.",
    )
    for ax, key in zip(axes, CLASS_METRICS):
        values = changed[key]
        limit = max(values.abs().max(), 0.01)
        bars = ax.barh(range(len(values)), values, color=[UP if v > 0 else DOWN for v in values], height=0.61)
        ax.set_title(key.upper(), loc="left", fontweight="bold")
        ax.set_xlim(-1.38 * limit, 1.38 * limit)
        ax.axvline(0, color="#8B9AAD", lw=1)
        ax.xaxis.set_major_locator(MaxNLocator(nbins=4, integer=key != "lcom*"))
        ax.xaxis.set_major_formatter(FuncFormatter(lambda value, _, precision=2 if key == "lcom*" else 0: number(value, precision)))
        ax.tick_params(axis="y", length=0)
        ax.grid(axis="x", color=GRID)
        ax.set_axisbelow(True)
        ax.bar_label(
            bars,
            labels=[number(v, 3 if key == "lcom*" else 0) if abs(v) > 1e-9 else "" for v in values],
            padding=3,
            fontsize=8,
        )
    axes[0].set_yticks(range(len(changed)), labels)
    axes[0].invert_yaxis()
    fig.legend(
        handles=[Patch(color=DOWN, label="Redução"), Patch(color=UP, label="Aumento")],
        loc="lower left", bbox_to_anchor=(0.18, 0.035), ncol=2, frameon=False,
    )
    save(fig, "02-classes-equivalentes.png")


def main():
    original = read_types(METRICS_DIR / "A-monolito-original/class.csv")
    adapted = read_types(METRICS_DIR / "B-monolito-adaptado/class.csv")
    owners = read_types(METRICS_DIR / "C-owners-service/class.csv")
    before = read_types(ARCHIVE / "B-monolito-adaptado/class.csv")
    OUTPUT.mkdir(parents=True, exist_ok=True)

    mean_digits = {key: 3 for key in METRICS}
    counts_digits = {key: (3 if key == "lcom*" else 0) for key in CLASS_METRICS}
    means = pd.DataFrame([original.mean(), adapted.mean()], index=["A · Original", "B · Adaptado"])
    small_multiples(
        means, [BLUE, TEAL], "Médias CK dos monólitos",
        f"A · Original ({len(original)} tipos)   |   B · Adaptado ({len(adapted)} tipos)",
        "01-medias-monolitos.png", "Cada painel tem escala própria. LCOM* é exibido em escala separada.", mean_digits,
    )

    equivalent_changes(original, adapted)

    added = adapted.loc[adapted.index.difference(original.index), CLASS_METRICS]
    aliases = {
        "OwnerIntegrationAdapter": "OwnerIntegration\nAdapter",
        "IntegrationErrorHandler": "IntegrationError\nHandler",
        "IntegrationErrorHandler$IntegrationFailure": "IntegrationFailure\n(classe interna)",
    }
    added.index = [aliases.get(name.rsplit(".", 1)[-1], name) for name in added.index]
    small_multiples(
        added, [BLUE, TEAL, GOLD], "Tipos adicionais do monólito V2",
        f"{len(added)} tipos exclusivos de B após a transferência do provisionamento para o Docker.",
        "03-tipos-adicionais.png", "O handler e sua classe interna são tipos distintos na CK.", counts_digits,
    )

    owner_type = "org.springframework.samples.petclinic.owner.Owner"
    owner_rows = pd.DataFrame(
        [original.loc[owner_type], adapted.loc[owner_type], owners.loc["br.edu.tcc.owners.Owner"]],
        index=["A · Original", "B · Adaptado", "C · Serviço"],
    )[CLASS_METRICS]
    small_multiples(
        owner_rows, [BLUE, TEAL, GOLD], "Owner nos três cenários",
        "A e B são o mesmo tipo nominal no monólito. C tem responsabilidades distintas.",
        "04-owner.png", "O Owner do microsserviço não é um equivalente direto dos tipos A e B.", counts_digits,
    )

    shared = adapted.index.intersection(before.index)
    if len(shared) != len(adapted) or not adapted.loc[shared].equals(before.loc[shared]):
        raise ValueError("A comparação exige que todos os tipos restantes mantenham suas métricas.")
    population = pd.DataFrame([before.mean(), adapted.mean()], index=["V2 anterior", "V2 Docker"])
    population.insert(0, "Tipos CK", [len(before), len(adapted)])
    small_multiples(
        population, [BEFORE, AFTER], "Efeito da retirada do inicializador",
        "Cenário B antes e depois de transferir o provisionamento do banco para a infraestrutura.",
        "05-efeito-inicializador.png",
        "As métricas dos tipos restantes são idênticas. As médias mudam pela composição da população.",
        {**mean_digits, "Tipos CK": 0},
    )


if __name__ == "__main__":
    main()
