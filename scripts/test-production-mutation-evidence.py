"""Adversarial synthetic evidence controls; these never earn campaign credit."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "mutation_verifier", Path(__file__).with_name("verify-production-mutation-evidence.py"))
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)
SHA = "a" * 40


def evidence(output, exit_code):
    return {"exit-code": exit_code, "timed-out?": False, "termination": "exited",
            "cleanup-complete?": True, "capture-truncated?": False, "fatal?": False,
            "output": output, "output-sha256": hashlib.sha256(output.encode()).hexdigest()}


def fixture_batches():
    compile_result = evidence("[:test-ci] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)", 0)
    compile_result["artifact-sha256"] = "b" * 64
    green = evidence("Ran 10 tests containing 40 assertions.\n0 failures, 0 errors.", 0)
    green.update({"tests": 10, "assertions": 40, "failures": 0, "errors": 0, "summaries": 1})
    red = evidence("Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.", 1)
    red.update({"tests": 10, "assertions": 40, "failures": 1, "errors": 0, "summaries": 1})
    return [{"schema_version": 2, "source_sha": SHA, "status": "passed", "baseline": "passed",
             "baseline_evidence": {"status": "passed", "compile": copy.deepcopy(compile_result), "test": copy.deepcopy(green)},
             "batch_index": batch, "excluded": [],
             "mutants": [{"id": f"m{batch * 250 + item:04d}", "fingerprint": f"{batch * 250 + item:064x}",
                          "status": "killed", "phase": "test", "compile": copy.deepcopy(compile_result),
                          "result": copy.deepcopy(red)} for item in range(250)]} for batch in range(4)]


class QualificationEvidenceTests(unittest.TestCase):
    def verify(self, batches):
        with tempfile.TemporaryDirectory(prefix="knoxx-verifier-") as directory:
            for index, batch in enumerate(batches):
                Path(directory, f"batch-{index}.json").write_text(json.dumps(batch))
            return verifier.verify(SHA, directory)

    def test_synthetic_complete_disjoint_evidence(self):
        self.assertEqual(1000, self.verify(fixture_batches()))

    def test_invalid_execution_and_forged_counters_are_denied(self):
        changes = [
            {"exit-code": 137}, {"termination": "signal"}, {"cleanup-complete?": False},
            {"capture-truncated?": True}, {"fatal?": True}, {"output-sha256": "0" * 64},
            {"tests": 0}, {"failures": 0}, {"summaries": 2},
            {"output": "Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.\n[shadow-test-guard] FATAL fixture"},
            {"output": "Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.\n1 failure, 0 errors."},
        ]
        for change in changes:
            with self.subTest(change=change):
                batches = fixture_batches()
                batches[0]["mutants"][0]["result"].update(change)
                with self.assertRaises((ValueError, KeyError)):
                    self.verify(batches)

    def test_missing_baseline_and_legacy_schema_are_denied(self):
        for change in ({"schema_version": 1}, {"baseline_evidence": None}):
            with self.subTest(change=change):
                batches = fixture_batches(); batches[0].update(change)
                with self.assertRaises((ValueError, KeyError, TypeError)):
                    self.verify(batches)

    def test_equivalent_records_cannot_receive_kill_credit(self):
        batches = fixture_batches()
        batches[0]["excluded"] = [{"fingerprint": batches[0]["mutants"][0]["fingerprint"],
                                   "relative-path": "a.cljc", "source-sha256": "c" * 64,
                                   "basis-source-sha": "d" * 40, "manifest-sha256": "e" * 64,
                                   "manifest-path": "backend/mutation/reviewed-equivalents.edn",
                                   "rationale": "Reviewed observational equivalence",
                                   "review-reference": "https://github.com/open-hax/knoxx/pull/1"}]
        with self.assertRaises(ValueError):
            self.verify(batches)

    def test_explicit_exclusion_receives_zero_evaluated_credit(self):
        batches = fixture_batches()
        batches[0]["excluded"] = [{"fingerprint": "f" * 64, "relative-path": "a.cljc", "source-sha256": "c" * 64,
                                   "basis-source-sha": "d" * 40, "manifest-sha256": "e" * 64,
                                   "manifest-path": "backend/mutation/reviewed-equivalents.edn",
                                   "rationale": "Synthetic reviewed equivalent fixture", "review-reference": "https://github.com/open-hax/knoxx/pull/1"}]
        self.assertEqual(1000, self.verify(batches))
        batches[0]["mutants"].pop()
        with self.assertRaises(ValueError):
            self.verify(batches)


if __name__ == "__main__":
    unittest.main()
