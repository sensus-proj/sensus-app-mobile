"""Regression tests for the performance sample summary."""

import contextlib
import csv
import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from summarize_performance import INPUT_FIELDS, main, read_samples, write_summary


HEADER = ",".join(INPUT_FIELDS)
MAP_WARM = "map,usable,baseline,gama-media,wifi,warm"


class SummarizePerformanceTest(unittest.TestCase):
    def read(self, rows):
        return read_samples(csv.reader(io.StringIO("\n".join([HEADER, *rows]))))

    def report(self, rows):
        output = io.StringIO()
        write_summary(output, self.read(rows))
        return list(csv.DictReader(io.StringIO(output.getvalue())))

    def test_reports_percentiles_and_keeps_conditions_separate(self):
        rows = [f"{MAP_WARM},ok,{duration}" for duration in range(1, 21)]
        rows.append(f"{MAP_WARM.replace('warm', 'cold')},ok,2500")
        cold, warm = self.report(rows)

        self.assertEqual(cold["status"], "insufficient_samples")
        self.assertEqual(cold["p95_ok_ms"], "2500")
        self.assertEqual(warm["p50_ok_ms"], "10")
        self.assertEqual(warm["p95_ok_ms"], "19")
        self.assertEqual(warm["status"], "met")

    def test_errors_and_timeouts_count_against_budget(self):
        successes = [f"{MAP_WARM},ok,2000"] * 19
        [met] = self.report([*successes, f"{MAP_WARM},timeout,"])
        [missed] = self.report([*successes[1:], f"{MAP_WARM},timeout,", f"{MAP_WARM},error,"])

        self.assertEqual(met["status"], "met")
        self.assertEqual(met["within_budget_pct"], "95.00")
        self.assertEqual(missed["status"], "missed")
        self.assertEqual(missed["within_budget_pct"], "90.00")
        self.assertEqual(missed["errors"], "1")
        self.assertEqual(missed["timeouts"], "1")

    def test_no_successes_leaves_percentiles_empty(self):
        [summary] = self.report([f"{MAP_WARM},error,"] * 20)
        self.assertEqual(summary["p50_ok_ms"], "")
        self.assertEqual(summary["p95_ok_ms"], "")
        self.assertEqual(summary["status"], "missed")

    def test_rejects_invalid_rows_and_empty_files(self):
        for row in (
            f"{MAP_WARM},ok,",
            f"{MAP_WARM},ok,-1",
            f"{MAP_WARM},timeout,1000",
            f"{MAP_WARM},unknown,",
            f"{MAP_WARM},ok,1000,extra",
            f"{MAP_WARM.replace('usable', 'unknown')},ok,1000",
            f"{MAP_WARM.replace('wifi', 'unknown')},ok,1000",
            f"{MAP_WARM.replace('baseline', '=1+1')},ok,1000",
        ):
            with self.subTest(row=row), self.assertRaises(ValueError):
                self.read([row])
        with self.assertRaises(ValueError):
            self.read([])
        with self.assertRaises(ValueError):
            read_samples(csv.reader(io.StringIO("bad,header\n")))

    def test_cli_prints_summary_and_rejects_bad_input(self):
        with tempfile.TemporaryDirectory() as directory:
            samples = Path(directory, "samples.csv")
            samples.write_text(f"{HEADER}\n{MAP_WARM},ok,1200\n", encoding="utf-8-sig")
            output = io.StringIO()
            with patch("sys.argv", ["summarize_performance.py", str(samples)]), contextlib.redirect_stdout(output):
                self.assertEqual(main(), 0)
                self.assertIn("insufficient_samples", output.getvalue())
                samples.write_text("bad,header\n", encoding="utf-8")
                with self.assertRaises(SystemExit) as raised, contextlib.redirect_stderr(io.StringIO()):
                    main()
                self.assertEqual(raised.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
