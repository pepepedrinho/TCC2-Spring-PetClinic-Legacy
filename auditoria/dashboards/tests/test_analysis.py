import hashlib
import json
import os
from pathlib import Path
import shutil
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd

from analysis import (counterparts, describe, filter_classes, methods_for, overview,
                      pair_classes, percent_change, population, rfc_decomposition, selection_after_click)
from data_loader import load_collection, read_csv


class CollectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = load_collection()

    def test_three_official_scenarios(self):
        self.assertEqual(self.data.classes.groupby("scenario").size().to_dict(),
                         {"A": 24, "B": 27, "C": 4})
        self.assertEqual(self.data.methods.groupby("scenario").size().to_dict(),
                         {"A": 91, "B": 104, "C": 20})
        self.assertNotIn("LegacyDatabaseInitializer", set(population(self.data, "B")["short"]))

    def test_pairs_by_qualified_name(self):
        a = population(self.data, "A")
        b = population(self.data, "B", "equivalent")
        self.assertEqual(set(a["class"]), set(b["class"]))
        self.assertEqual(len(pair_classes(self.data, a, "rfc")), 24)

    def test_additional_types(self):
        rows = population(self.data, "B", "additional")
        self.assertEqual(set(rows["short"]), {"OwnerIntegrationAdapter",
                                             "IntegrationErrorHandler", "IntegrationErrorHandler$IntegrationFailure"})
        self.assertEqual(set(rows["category"]), {"Integração"})

    def test_five_means(self):
        expected = [
            [160 / 24, 158 / 24, 177 / 27],
            [118 / 24, 124 / 24, 145 / 27],
            [83 / 24, 96 / 24, 126 / 27],
            [141 / 24, 161 / 24, 164 / 27],
            [.25661800125, .25907311, .2426328878],
        ]
        np.testing.assert_allclose(overview(self.data)[["before", "equivalent", "complete"]], expected, rtol=1e-6)
        self.assertAlmostEqual(population(self.data, "C")["rfc"].mean(), 2.75)

    def test_percentages_and_zero(self):
        means = overview(self.data).set_index("code")
        self.assertAlmostEqual(means.at["rfc", "pct_equivalent"], 13 / 83 * 100)
        self.assertAlmostEqual(means.at["rfc", "pct_complete"], ((126 / 27) / (83 / 24) - 1) * 100)
        self.assertTrue(np.isnan(percent_change(0, 0)))
        self.assertTrue(np.isnan(percent_change(0, 5)))
        np.testing.assert_allclose(percent_change(pd.Series([0, 5, 2]), pd.Series([1, 0, 3])),
                                   [np.nan, -100, 50], equal_nan=True)

    def test_rfc_decomposition(self):
        d = rfc_decomposition(self.data)
        self.assertEqual([d[key] for key in ["original", "equivalent", "additional", "adapted", "growth"]],
                         [83, 96, 30, 126, 43])
        self.assertAlmostEqual(d["additional_pct"], 30 / 43 * 100)
        self.assertAlmostEqual(d["equivalent_pct"] + d["additional_pct"], 100)

    def test_filters_keep_same_population_when_paired(self):
        a = population(self.data, "A")
        scope = filter_classes(a, "rfc", packages=["org.springframework.samples.petclinic.owner"],
                               types=["class"], query="Controller", low=6, high=15)
        result = pair_classes(self.data, scope, "rfc")
        self.assertEqual(result["short"].tolist(), ["PetController", "VisitController"])
        self.assertEqual(result["delta"].tolist(), [7, 6])
        self.assertEqual(result["after"].tolist(), [22, 12])

    def test_empty_selection_and_scope(self):
        rows = counterparts(self.data, [])
        self.assertTrue(rows.empty)
        self.assertTrue(methods_for(self.data, rows).empty)
        self.assertEqual(describe(rows["rfc"])["n"], 0)
        self.assertTrue(np.isnan(describe(rows["rfc"])["mean"]))

    def test_absent_observations_are_not_fabricated(self):
        b = population(self.data, "B")
        keys = b.loc[b["short"].isin(["PetController", "OwnerIntegrationAdapter"]), "key"]
        result = counterparts(self.data, keys)
        self.assertEqual(result.groupby("short")["scenario"].apply(list).to_dict(),
                         {"OwnerIntegrationAdapter": ["B"], "PetController": ["A", "B"]})
        methods = methods_for(self.data, result)
        self.assertFalse(methods.duplicated(["scenario", "class", "method", "line"]).any())

    def test_selection_toggles_visible_identities(self):
        self.assertEqual(selection_after_click([], "x", {"x"}), ["x"])
        self.assertEqual(selection_after_click(["x"], "y", {"x"}), ["x"])
        self.assertEqual(selection_after_click(["x"], "x", {"x"}), [])

    def test_hashes_match_original_data_after_reads(self):
        load_collection()
        self.assertEqual(len(self.data.sources), 6)
        for canonical, file in self.data.sources.items():
            scenario, name = canonical.split("/")
            hashes = self.data.provenance["scenarios"][scenario]["outputSha256"]
            self.assertEqual(hashlib.sha256(file.read_bytes()).hexdigest(), hashes[name])

    def test_override_and_per_scenario_hash_validation(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            for relative, source in self.data.sources.items():
                dest = root / relative
                dest.parent.mkdir(exist_ok=True)
                shutil.copyfile(source, dest)
            manifest = root / "proveniencia.json"
            manifest.write_text(json.dumps(self.data.provenance), encoding="utf-8")
            with patch.dict(os.environ, {"CK_METRICS_DIR": str(root)}):
                self.assertTrue(all(file.is_relative_to(root.resolve()) for file in load_collection().sources.values()))
                provenance = json.loads(manifest.read_text(encoding="utf-8"))
                provenance["scenarios"]["A-monolito-original"]["outputSha256"]["class.csv"] = "0" * 64
                manifest.write_text(json.dumps(provenance), encoding="utf-8")
                with self.assertRaisesRegex(ValueError, "SHA-256 divergente"):
                    load_collection()

    def test_bad_schema_data_and_hash(self):
        with TemporaryDirectory() as tmp:
            path = Path(tmp) / "class.csv"
            path.write_text("file,cbo\nignored,wrong\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                read_csv(path, {"file", "cbo"}, "invalid hash", {"file"})
            with self.assertRaisesRegex(ValueError, "ausentes"):
                read_csv(path, {"file", "cbo", "wmc"}, None, {"file"})
            with self.assertRaisesRegex(ValueError, "inválida cbo"):
                read_csv(path, {"file", "cbo"}, None, {"file"})

