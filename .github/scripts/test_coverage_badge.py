"""Regression checks for report-derived coverage; no network or repository writes."""

import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location("badge", Path(__file__).with_name("coverage-badge.py"))
badge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(badge)


class CoverageBadgeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.previous = Path.cwd()
        os.chdir(self.temp.name)
        self.addCleanup(self.temp.cleanup)
        self.addCleanup(os.chdir, self.previous)

    def reports(self, backend_covered=960, backend_total=1000, frontend_covered=100, frontend_total=100):
        backend = Path("Backend/target/site/jacoco/jacoco.xml")
        frontend = Path("web/coverage/coverage-summary.json")
        backend.parent.mkdir(parents=True, exist_ok=True)
        frontend.parent.mkdir(parents=True, exist_ok=True)
        backend.write_text(f'<report><counter type="LINE" covered="{backend_covered}" missed="{backend_total-backend_covered}"/></report>')
        frontend.write_text(json.dumps({"total": {"lines": {"covered": frontend_covered, "total": frontend_total}}}))

    def generate(self):
        with contextlib.redirect_stdout(io.StringIO()):
            badge.generate()
        return json.loads(Path("coverage-badge/report.json").read_text())

    def test_uses_weighted_real_counts_not_mean_or_threshold(self):
        self.reports()
        result = self.generate()
        self.assertEqual(result["message"], "96.36%")
        self.assertEqual((result["covered"], result["total"]), (1060, 1100))
        self.assertEqual(result["modules"]["frontend"]["percent"], 100)

    def test_aggregate_cannot_hide_a_failed_module(self):
        self.reports(backend_covered=95, backend_total=100, frontend_covered=10000, frontend_total=10000)
        with self.assertRaises(ValueError):
            self.generate()
        self.assertFalse(Path("coverage-badge/coverage.json").exists())

    def test_missing_report_does_not_create_a_fake_badge(self):
        with self.assertRaises(FileNotFoundError):
            self.generate()
        self.assertFalse(Path("coverage-badge/coverage.json").exists())

    def test_invalid_counts_are_rejected(self):
        for covered, total in ((0, 0), (-1, 100), (101, 100)):
            with self.subTest(covered=covered, total=total):
                self.reports(frontend_covered=covered, frontend_total=total)
                with self.assertRaises(ValueError):
                    self.generate()


if __name__ == "__main__":
    unittest.main()
