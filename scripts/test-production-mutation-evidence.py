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
LOADER = "target/test/test-ci.cjs"
RUNTIME = ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime"
LOADER_BYTES = ("var SHADOW_IMPORT_PATH = __dirname + '/../../" + RUNTIME + "';\n"
                'SHADOW_IMPORT("sample.test.js");\n'
                'SHADOW_IMPORT("shadow.module.main.append.js");\n').encode()
FIXTURE_BYTES = {LOADER: LOADER_BYTES, RUNTIME + "/goog.base.js": b"var goog = {};\n",
                 RUNTIME + "/goog.base.js.map": b'{"version":3,"file":"goog.base.js"}\n',
                 RUNTIME + "/shadow.module.main.append.js": b"void 0;\n",
                 RUNTIME + "/sample.test.js.map": b'{"version":3}\n'}


def payload(binding, module):
    """Real byte-bearing fixtures, with an unchanged loader and changed runtime."""
    blobs = {**FIXTURE_BYTES, RUNTIME + "/sample.test.js": module}
    files = [{"path": path, "kind": "loader" if path == LOADER else
              "source-map" if path.endswith(".map") else "runtime-js",
              "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
             for path, raw in sorted(blobs.items())]
    return {"schema-version": 1, "source-sha": SHA, "source-binding": binding,
            "loader-path": LOADER, "runtime-root": RUNTIME,
            "imports": ["sample.test.js", "shadow.module.main.append.js"],
            "files": files, "sha256": fixture_digest(files),
            "code-sha256": fixture_digest([entry for entry in files if entry["kind"] != "source-map"])}


def fixture_digest(entries):
    return hashlib.sha256("".join(
        f'{entry["path"]}\t{entry["kind"]}\t{entry["bytes"]}\t{entry["sha256"]}\n'
        for entry in entries).encode()).hexdigest()


def replace_payload_bytes(compiled, path, raw):
    """Keep fixture metadata internally consistent around an actual byte change."""
    item = next(entry for entry in compiled["payload"]["files"] if entry["path"] == path)
    item.update({"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()})
    files = compiled["payload"]["files"]
    compiled["payload"]["sha256"] = fixture_digest(files)
    compiled["payload"]["code-sha256"] = fixture_digest([entry for entry in files if entry["kind"] != "source-map"])
    if path == LOADER:
        compiled["artifact-sha256"] = item["sha256"]


def compile_fixture(binding, module):
    result = evidence("[:test-ci] Build completed. (10 files, 1 compiled, 0 warnings, 0.1s)", 0)
    result.update({"artifact-path": LOADER, "artifact-sha256": hashlib.sha256(LOADER_BYTES).hexdigest(),
                   "payload": payload(binding, module), "source-verified-before?": True,
                   "source-verified-after?": True})
    return result


def test_fixture(output, exit_code, compile_result):
    result = evidence(output, exit_code)
    result.update({"tests": 10, "assertions": 40, "failures": exit_code, "errors": 0, "summaries": 1,
                   "input-payload-sha256": compile_result["payload"]["sha256"],
                   "source-sha": SHA, "source-binding": copy.deepcopy(compile_result["payload"]["source-binding"]),
                   "payload-verified-before?": True, "payload-verified-after?": True,
                   "source-verified-before?": True, "source-verified-after?": True})
    return result


def evidence(output, exit_code):
    return {"exit-code": exit_code, "timed-out?": False, "termination": "exited",
            "cleanup-complete?": True, "capture-truncated?": False, "fatal?": False,
            "output": output, "output-sha256": hashlib.sha256(output.encode()).hexdigest()}


def fixture_batches():
    baseline_compile = compile_fixture({"kind": "baseline"}, b"var answer = true;\n")
    batches = []
    for batch in range(4):
        mutants = []
        for item in range(250):
            fingerprint = f"{batch * 250 + item:064x}"
            binding = {"kind": "mutant", "fingerprint": fingerprint,
                       "path": "src/cljs/sample.cljs", "original-sha256": "c" * 64,
                       "mutated-sha256": "d" * 64}
            compiled = compile_fixture(binding, b"var answer = false;\n")
            mutants.append({"id": f"m{batch * 250 + item:04d}", "fingerprint": fingerprint,
                            "source-binding": binding, "status": "killed", "phase": "test",
                            "compile": compiled, "result": test_fixture(
                                "Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.", 1, compiled)})
        batches.append({"schema_version": 3, "source_sha": SHA, "status": "passed", "baseline": "passed",
                        "baseline_evidence": {"status": "passed", "compile": copy.deepcopy(baseline_compile),
                                              "test": test_fixture("Ran 10 tests containing 40 assertions.\n0 failures, 0 errors.",
                                                                   0, baseline_compile)},
                        "batch_index": batch, "excluded": [], "mutants": mutants})
    return batches


class QualificationEvidenceTests(unittest.TestCase):
    def verify(self, batches, alter_store=None):
        with tempfile.TemporaryDirectory(prefix="knoxx-verifier-") as directory:
            store = Path(directory, "payload-store")
            store.mkdir()
            for raw in [*FIXTURE_BYTES.values(), b"var answer = true;\n", b"var answer = false;\n"]:
                Path(store, hashlib.sha256(raw).hexdigest()).write_bytes(raw)
            if alter_store:
                alter_store(store)
            for index, batch in enumerate(batches):
                Path(directory, f"batch-{index}.json").write_text(json.dumps(batch))
            return verifier.verify(SHA, directory)

    def test_synthetic_complete_disjoint_evidence(self):
        self.assertEqual(1000, self.verify(fixture_batches()))

    def test_optional_debug_maps_match_actual_shadow_layout(self):
        batches = fixture_batches()
        files = batches[0]["baseline_evidence"]["compile"]["payload"]["files"]
        paths = {entry["path"] for entry in files}
        self.assertIn(RUNTIME + "/goog.base.js.map", paths)
        self.assertIn(RUNTIME + "/shadow.module.main.append.js", paths)
        self.assertNotIn(RUNTIME + "/shadow.module.main.append.js.map", paths)
        self.assertEqual(1000, self.verify(batches))

    def test_unchanged_loader_and_changed_imported_code_are_distinguished(self):
        batches = fixture_batches()
        baseline = batches[0]["baseline_evidence"]["compile"]["payload"]
        mutated = batches[0]["mutants"][0]["compile"]["payload"]
        self.assertEqual(baseline["files"][-1], mutated["files"][-1])
        self.assertNotEqual(baseline["code-sha256"], mutated["code-sha256"])
        self.assertEqual(1000, self.verify(batches))

    def test_tampered_imported_runtime_bytes_are_denied(self):
        digest = hashlib.sha256(b"var answer = false;\n").hexdigest()
        with self.assertRaises(ValueError):
            self.verify(fixture_batches(), lambda store: Path(store, digest).write_bytes(b"tampered\n"))

    def test_missing_imported_runtime_bytes_are_denied(self):
        digest = hashlib.sha256(b"var answer = false;\n").hexdigest()
        with self.assertRaises(ValueError):
            self.verify(fixture_batches(), lambda store: Path(store, digest).unlink())

    def test_test_input_and_source_binding_mismatch_are_denied(self):
        for field, value in [("input-payload-sha256", "0" * 64), ("source-sha", "f" * 40),
                             ("source-binding", {"kind": "baseline"})]:
            with self.subTest(field=field):
                batches = fixture_batches(); batches[0]["mutants"][0]["result"][field] = value
                with self.assertRaises(ValueError):
                    self.verify(batches)

    def test_execution_time_custody_changes_are_denied(self):
        for field in ["payload-verified-before?", "payload-verified-after?",
                      "source-verified-before?", "source-verified-after?"]:
            with self.subTest(field=field):
                batches = fixture_batches(); batches[0]["mutants"][0]["result"][field] = False
                with self.assertRaisesRegex(ValueError, "changed during execution"):
                    self.verify(batches)

    def test_compile_source_and_mutant_descriptor_are_bound(self):
        changes = [("source-verified-before?", False), ("source-verified-after?", False)]
        for field, value in changes:
            with self.subTest(field=field):
                batches = fixture_batches(); batches[0]["mutants"][0]["compile"][field] = value
                with self.assertRaisesRegex(ValueError, "Compile source custody"):
                    self.verify(batches)
        for field, value in [("fingerprint", "f" * 64), ("path", "src/cljs/../sample.cljs"),
                             ("original-sha256", "d" * 64)]:
            with self.subTest(field=field):
                batches = fixture_batches(); batches[0]["mutants"][0]["source-binding"][field] = value
                with self.assertRaises(ValueError):
                    self.verify(batches)

    def test_immutable_payload_source_and_inventory_are_bound(self):
        for change in [{"source-sha": "f" * 40}, {"source-binding": {"kind": "baseline"}},
                       {"sha256": "0" * 64}, {"code-sha256": "0" * 64},
                       {"imports": ["../sample.test.js"]}, {"runtime-root": "other"}]:
            with self.subTest(change=change):
                batches = fixture_batches(); batches[0]["mutants"][0]["compile"]["payload"].update(change)
                with self.assertRaises(ValueError):
                    self.verify(batches)
        for alter in [lambda files: files.pop(), lambda files: files.append(copy.deepcopy(files[0])),
                      lambda files: files.reverse()]:
            batches = fixture_batches(); alter(batches[0]["mutants"][0]["compile"]["payload"]["files"])
            with self.assertRaisesRegex(ValueError, "compiled files"):
                self.verify(batches)

    def test_unchanged_executed_code_cannot_be_claimed_as_killed(self):
        batches = fixture_batches()
        mutant = batches[0]["mutants"][0]
        compiled = compile_fixture(mutant["source-binding"], b"var answer = true;\n")
        mutant["compile"] = compiled
        mutant["result"] = test_fixture("Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.", 1, compiled)
        with self.assertRaisesRegex(ValueError, "Unchanged executed code"):
            self.verify(batches)

    def test_maps_alone_do_not_change_executed_code_identity(self):
        batches = fixture_batches(); mutant = batches[0]["mutants"][0]
        compiled = compile_fixture(mutant["source-binding"], b"var answer = true;\n")
        changed_map = b'{"version":3,"sources":["changed.cljs"]}\n'
        replace_payload_bytes(compiled, RUNTIME + "/sample.test.js.map", changed_map)
        mutant["compile"] = compiled
        mutant["result"] = test_fixture("Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.", 1, compiled)
        baseline = batches[0]["baseline_evidence"]["compile"]["payload"]
        self.assertNotEqual(compiled["payload"]["sha256"], baseline["sha256"])
        self.assertEqual(compiled["payload"]["code-sha256"], baseline["code-sha256"])
        with self.assertRaisesRegex(ValueError, "Unchanged executed code"):
            self.verify(batches, lambda store: Path(store, hashlib.sha256(changed_map).hexdigest()).write_bytes(changed_map))

    def test_actual_loader_root_and_import_bytes_are_checked(self):
        for raw, message in [(LOADER_BYTES.replace(b"/../../", b"/../"), "relative runtime root"),
                             (LOADER_BYTES.replace(b'sample.test.js', b'other.test.js'), "loader imports")]:
            with self.subTest(raw=raw):
                batches = fixture_batches(); mutant = batches[0]["mutants"][0]
                replace_payload_bytes(mutant["compile"], LOADER, raw)
                mutant["result"]["input-payload-sha256"] = mutant["compile"]["payload"]["sha256"]
                with self.assertRaisesRegex(ValueError, message):
                    self.verify(batches, lambda store: Path(store, hashlib.sha256(raw).hexdigest()).write_bytes(raw))

    def test_symlink_and_unreferenced_cas_bytes_are_denied(self):
        digest = hashlib.sha256(b"var answer = false;\n").hexdigest()
        def link_blob(store):
            target = store.parent / "owned-runtime-copy"
            target.write_bytes(Path(store, digest).read_bytes())
            Path(store, digest).unlink()
            Path(store, digest).symlink_to(target)
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.verify(fixture_batches(), link_blob)
        def link_store(store):
            target = store.parent / "owned-real-store"
            store.rename(target)
            store.symlink_to(target, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.verify(fixture_batches(), link_store)
        with self.assertRaisesRegex(ValueError, "unexpected or missing blobs"):
            self.verify(fixture_batches(), lambda store: Path(store, "f" * 64).write_bytes(b"unreferenced"))

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
        for change in ({"schema_version": 1}, {"schema_version": 2}, {"baseline_evidence": None}):
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
