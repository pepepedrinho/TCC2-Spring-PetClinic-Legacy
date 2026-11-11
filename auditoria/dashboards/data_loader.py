"""Leitura da coleta consolidada e verificação dos artefatos CK."""

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
import json
import os
from pathlib import Path
import re

import numpy as np
import pandas as pd

HERE = Path(__file__).resolve().parent
DATA_DIR = HERE.parent / "docs" / "metricas"
METRICS = ("cbo", "wmc", "rfc", "lcom", "lcom*")
ORIGINAL = "Monólito original"
ADAPTED = "Monólito adaptado"
EQUIVALENT_TYPES = "Classes equivalentes"
OWNERS = "Microsserviço Owners"

# Rótulo e diretório canônico. Os totais e hashes vêm do manifesto.
SCENARIOS = {
    "A": (ORIGINAL, "A-monolito-original"),
    "B": (ADAPTED, "B-monolito-adaptado"),
    "C": (OWNERS, "C-owners-service"),
}
CLASS_COLUMNS = {"file", "class", "type", *METRICS}
METHOD_COLUMNS = {
    "file", "class", "method", "constructor", "line", "hasJavaDoc",
    "wmc", "rfc", "cbo", "loc", "parametersQty", "methodsInvokedQty",
    "comparisonsQty", "loopQty", "tryCatchQty",
}


@dataclass(frozen=True)
class Collection:
    classes: pd.DataFrame
    methods: pd.DataFrame
    equivalent: frozenset[str]
    additional: frozenset[str]
    provenance: dict
    sources: dict[str, Path]
    notes: dict[str, str]


def read_csv(path, required, expected_hash, text_fields, nullable=()):
    raw = path.read_bytes()
    if expected_hash and sha256(raw).hexdigest() != expected_hash:
        raise ValueError(f"SHA-256 divergente da proveniência em {path.parent.name}/{path.name}.")
    # A coluna histórica file contém caracteres Windows-1252 nesta coleta.
    try:
        frame = pd.read_csv(BytesIO(raw), encoding="utf-8-sig")
    except UnicodeDecodeError:
        frame = pd.read_csv(BytesIO(raw), encoding="cp1252")
    missing = required.difference(frame.columns)
    if missing:
        raise ValueError(f"{path.name}: colunas ausentes {sorted(missing)}.")
    for name in frame.columns.difference(text_fields):
        try:
            values = pd.to_numeric(frame[name], errors="raise")
            finite = np.isfinite(values.dropna()).all()
            if not finite or (name not in nullable and values.isna().any()):
                raise ValueError("valor ausente ou infinito")
        except (TypeError, ValueError) as exc:
            raise ValueError(f"{path.parent.name}/{path.name}: coluna numérica inválida {name}.") from exc
        frame[name] = values
    return frame.drop(columns="file")


def load_collection(root=None):
    if root is None:
        configured = os.environ.get("CK_METRICS_DIR")
        root = HERE.parent / configured if configured else DATA_DIR
    root = Path(root).resolve()
    provenance_path = root / "proveniencia.json"
    if not provenance_path.is_file():
        raise FileNotFoundError(
            f"Coleta oficial ausente em {provenance_path}. "
            "Use CK_METRICS_DIR para apontar para docs/metricas da worktree atualizada."
        )
    provenance = json.loads(provenance_path.read_text(encoding="utf-8-sig"))
    config = json.loads((HERE / "classification.json").read_text(encoding="utf-8-sig"))
    classes, methods, sources = {}, {}, {}
    for code, (_, canonical) in SCENARIOS.items():
        directory = root / canonical
        scenario = provenance.get("scenarios", {}).get(canonical, {})
        totals = (scenario.get("types"), scenario.get("methods"))
        if any(type(count) is not int or count <= 0 for count in totals):
            raise ValueError(f"{canonical}: totais de tipos e métodos ausentes ou inválidos na proveniência.")
        hashes = scenario.get("outputSha256", {})
        for name in ("class.csv", "method.csv"):
            expected_hash = hashes.get(name, "")
            if not isinstance(expected_hash, str) or not re.fullmatch(r"[0-9a-f]{64}", expected_hash):
                raise ValueError(f"SHA-256 ausente ou inválido na proveniência em {canonical}/{name}.")
            path = directory / name
            if not path.is_file():
                raise FileNotFoundError(f"CSV obrigatório ausente: {path}")
            sources[f"{canonical}/{name}"] = path
        cs = read_csv(directory / "class.csv", CLASS_COLUMNS, hashes["class.csv"],
                      {"file", "class", "type"}, nullable=("tcc", "lcc"))
        ms = read_csv(directory / "method.csv", METHOD_COLUMNS, hashes["method.csv"],
                      {"file", "class", "method", "constructor", "hasJavaDoc"})
        if (len(cs), len(ms)) != totals:
            raise ValueError(f"{canonical}: esperados {totals[0]} tipos e {totals[1]} métodos.")
        if cs[["class", "type"]].isna().any().any() or cs["class"].duplicated().any():
            raise ValueError(f"{canonical}: identidade de tipo vazia ou duplicada.")
        identity = ["class", "method", "line"]
        if ms[identity].isna().any().any() or ms.duplicated(identity).any():
            raise ValueError(f"{canonical}: identidade de método vazia ou duplicada.")
        if not set(ms["class"]).issubset(cs["class"]):
            raise ValueError(f"{canonical}: há métodos sem tipo correspondente.")
        classes[code] = cs.assign(scenario=code)
        methods[code] = ms.assign(scenario=code)

    original, adapted = (frozenset(classes[code]["class"]) for code in ("A", "B"))
    equivalent, additional = original & adapted, adapted - original
    if equivalent != original:
        raise ValueError("Pareamento por nome qualificado: há tipos do cenário A ausentes em B.")
    class_data = pd.concat(classes.values(), ignore_index=True)
    class_data["package"] = class_data["class"].str.rpartition(".")[0]
    class_data["short"] = class_data["class"].str.rsplit(".", n=1).str[-1]
    class_data["scenario_label"] = class_data["scenario"].map({k: v[0] for k, v in SCENARIOS.items()})
    class_data["category"] = class_data["scenario"].map({
        "A": "Legado equivalente", "B": "Legado equivalente",
        "C": OWNERS,
    })
    mask = class_data["scenario"].eq("B") & class_data["class"].isin(additional)
    class_data.loc[mask, "category"] = (
        class_data.loc[mask, "class"].map(config["additional_categories"]).fillna("Adicional sem classificação")
    )
    # A e B compartilham a seleção pelo mesmo nome. Serviços têm identidade própria.
    class_data["key"] = class_data["scenario"].replace({"A": "AB", "B": "AB"}) + "|" + class_data["class"]
    return Collection(class_data, pd.concat(methods.values(), ignore_index=True),
                      equivalent, additional, provenance, sources, config["responsibility_notes"])
