"""Adversarial synthetic evidence controls; these never earn campaign credit."""
from collections import Counter
import copy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import yaml

spec = importlib.util.spec_from_file_location(
    "mutation_verifier", Path(__file__).with_name("verify-production-mutation-evidence.py"))
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)
SHA = "a" * 40
SOURCE_MANIFEST = None
LOADER = "target/test/test-ci.cjs"
RUNTIME = ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime"
LOADER_BYTES = ("var SHADOW_IMPORT_PATH = __dirname + '/../../" + RUNTIME + "';\n"
                'SHADOW_IMPORT("sample.test.js");\n'
                'SHADOW_IMPORT("shadow.module.main.append.js");\n').encode()
FIXTURE_BYTES = {LOADER: LOADER_BYTES, RUNTIME + "/goog.base.js": b"var goog = {};\n",
                 RUNTIME + "/goog.base.js.map": b'{"version":3,"file":"goog.base.js"}\n',
                 RUNTIME + "/shadow.module.main.append.js": b"void 0;\n",
                 RUNTIME + "/sample.test.js.map": b'{"version":3}\n'}


# Historical actual runner contract data, not current qualification evidence.
# Source78843174b7fb3cf27a3ad0c8497b74bdb9bf882c, actual-compile-result-v2.json
# SHA2561af951628ddb6aae3aa673ae9983ffeddfc78e168c345e6aec5b76148a6ecbbe.
# Paired with an independent canonical JVM assertion of this exact vector.
RUNNER_DIGEST_VECTOR = [{'path': '.shadow-cljs/builds/test-ci/dev/out/cljs-runtime/borkdude.dynaload.js',
  'kind': 'runtime-js',
  'bytes': 15632,
  'sha256': '01056d679bdde793374719dc7299db423b8301bd163fcc8251b78fdccf673f7c'},
 {'path': '.shadow-cljs/builds/test-ci/dev/out/cljs-runtime/borkdude.dynaload.js.map',
  'kind': 'source-map',
  'bytes': 21877,
  'sha256': 'c99be5cf888716d794dfce5282d343ca10397fd017e2f96ad0559ab0581512f7'},
 {'path': 'target/test/test-ci.cjs',
  'kind': 'loader',
  'bytes': 98437,
  'sha256': '037202de10342bd13a49ba4cc4c09f978287091ff23e974d3753be452a0c11d4'}]
RUNNER_PAYLOAD_DIGEST = "fdea337fc410ac8f655a4c458204beab2e8922d71cdec834d6edc05500fed7a4"
RUNNER_CODE_DIGEST = "5a9a3c92dc82b9c895c2686b3b9c2a0bb88c333bb1e0744205abfcd78fdcbc5e"
# Exact root and first two ordered import lines from actual emitted loader
# SHA256037202de10342bd13a49ba4cc4c09f978287091ff23e974d3753be452a0c11d4.
RUNNER_LOADER_FRAGMENT = (
    b"var SHADOW_IMPORT_PATH = __dirname + '/../../.shadow-cljs/builds/test-ci/dev/out/cljs-runtime';\n"
    b'SHADOW_IMPORT("goog.debug.error.js");\n'
    b'SHADOW_IMPORT("goog.dom.nodetype.js");\n'
)
RUNNER_LOADER_IMPORTS = ['goog.debug.error.js', 'goog.dom.nodetype.js']


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
            binding = copy.deepcopy(SOURCE_MANIFEST["candidates"][item * 4 + batch]["source-binding"])
            fingerprint = binding["fingerprint"]
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


def canonical_candidate_fixture(ordinal):
    """A later canonical identity with self-consistent synthetic kill evidence."""
    binding = copy.deepcopy(SOURCE_MANIFEST["candidates"][ordinal]["source-binding"])
    compiled = compile_fixture(binding, b"var answer = false;\n")
    return {"id": f"canonical-{ordinal}", "fingerprint": binding["fingerprint"],
            "source-binding": binding, "status": "killed", "phase": "test", "compile": compiled,
            "result": test_fixture("Ran 10 tests containing 40 assertions.\n1 failure, 0 errors.", 1, compiled)}


class QualificationEvidenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        """Generate fixture identities with the actual canonical runner, once.

        Only the later transport reads are mocked. Synthetic child outputs and
        payloads never establish campaign credit, even with real source bindings.
        """
        global SHA, SOURCE_MANIFEST
        cls.source_fixture = tempfile.TemporaryDirectory(prefix="knoxx-source-bindings-")
        cls.addClassCleanup(cls.source_fixture.cleanup)
        root = Path(cls.source_fixture.name)
        backend = root / "backend"
        (backend / "mutation/knoxx").mkdir(parents=True)
        (backend / "src/cljs").mkdir(parents=True)
        repository = Path(__file__).resolve().parent.parent
        for relative in ["deps.edn", "mutation/knoxx/mutation_test.clj"]:
            (backend / relative).write_bytes((repository / "backend" / relative).read_bytes())
        (root / ".gitignore").write_text("backend/.cpcache/\n")
        cls.source_bytes = ("(ns sample)\n" + "".join(f"(def v{index} true)\n" for index in range(1004))).encode()
        (backend / "src/cljs/sample.cljs").write_bytes(cls.source_bytes)
        # All Git operations are local to this fresh disposable fixture.
        git_env = {key: value for key, value in os.environ.items() if not key.startswith("GIT_")}
        git_env.update(GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull)
        for args in [["init", "--quiet", "--template="], ["add", "."],
                     ["-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                      "-c", "core.hooksPath=/dev/null", "commit", "--quiet", "-m", "source fixture"]]:
            subprocess.run(["git", *args], cwd=root, env=git_env, check=True, capture_output=True)
        SHA = subprocess.run(["git", "rev-parse", "HEAD"], cwd=root, env=git_env,
                             check=True, capture_output=True, text=True).stdout.strip()
        actual = subprocess.run(["clojure", "-Srepro", "-M:mutation", "--source-bindings", SHA],
                                cwd=backend, check=True, capture_output=True, text=True, timeout=600)
        cls.manifest_stdout = actual.stdout
        SOURCE_MANIFEST = json.loads(actual.stdout)
        if (SOURCE_MANIFEST["source-sha"] != SHA or len(SOURCE_MANIFEST["candidates"]) != 1004
                or (backend / "src/cljs/sample.cljs").read_bytes() != cls.source_bytes):
            raise AssertionError("Actual canonical fixture generation did not preserve source")
        print(json.dumps({"kind": "actual canonical source fixture; synthetic results supply no campaign credit",
                          "source_sha": SHA, "source_bytes_sha256": hashlib.sha256(cls.source_bytes).hexdigest(),
                          "manifest_stdout_sha256": hashlib.sha256(actual.stdout.encode()).hexdigest(),
                          "count": 1004, "first": SOURCE_MANIFEST["candidates"][0],
                          "last": SOURCE_MANIFEST["candidates"][-1]}, sort_keys=True))

    def verify(self, batches, alter_store=None, manifest=None, transport=None, production=False):
        with tempfile.TemporaryDirectory(prefix="knoxx-verifier-") as directory:
            store = Path(directory, "payload-store")
            store.mkdir()
            for raw in [*FIXTURE_BYTES.values(), b"var answer = true;\n", b"var answer = false;\n"]:
                Path(store, hashlib.sha256(raw).hexdigest()).write_bytes(raw)
            if alter_store:
                alter_store(store)
            for index, batch in enumerate(batches):
                Path(directory, f"batch-{index}.json").write_text(json.dumps(batch))
            def captured_manifest(args, **kwargs):
                self.assertEqual(["clojure", "-Srepro", "-M:mutation", "--source-bindings", SHA], args)
                self.assertEqual(Path(verifier.__file__).resolve().parent.parent / "backend", kwargs["cwd"])
                self.assertEqual({"cwd", "check", "capture_output", "text", "timeout"}, set(kwargs))
                self.assertTrue(kwargs["check"] and kwargs["capture_output"] and kwargs["text"])
                self.assertEqual(600, kwargs["timeout"])
                if transport:
                    return transport(args, kwargs)
                return subprocess.CompletedProcess(args, 0, self.manifest_stdout if manifest is None
                                                   else json.dumps(manifest), "")
            with patch.object(subprocess, "run", side_effect=captured_manifest):
                return (verifier.verify if production else verifier.validate_structural_evidence)(SHA, directory)

    def test_synthetic_complete_disjoint_structural_evidence_only(self):
        self.assertEqual(1000, self.verify(fixture_batches()))

    def test_unsigned_synthetic_fixture_cannot_qualify_production(self):
        """The prior production API accepts this fixture; authenticated admission must refuse."""
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(ValueError, "Mandatory authenticated"):
                self.verify(fixture_batches(), production=True)

    def test_relative_evidence_directory_uses_authenticated_absolute_report_keys(self):
        """Real structural parsing uses the frozen reports; authentication is inert."""
        with tempfile.TemporaryDirectory(prefix="knoxx-relative-evidence-") as directory:
            root = Path(directory)
            evidence_root = root / "evidence"
            store = evidence_root / "payload-store"
            store.mkdir(parents=True)
            for raw in [*FIXTURE_BYTES.values(), b"var answer = true;\n", b"var answer = false;\n"]:
                (store / hashlib.sha256(raw).hexdigest()).write_bytes(raw)
            for index, batch in enumerate(fixture_batches()):
                (evidence_root / f"batch-{index}.edn.json").write_text(json.dumps(batch))
            context = {"controller_sha": "c" * 40}  # Explicit mocked authentication only.
            authenticated = []
            def authenticate(path, raw, observed_context):
                self.assertTrue(path.is_absolute())
                self.assertEqual(context, observed_context)
                self.assertEqual(path.read_bytes(), raw)
                authenticated.append(path.name)
            def manifest(args, **kwargs):
                self.assertEqual(["clojure", "-Srepro", "-M:mutation", "--source-bindings", SHA], args)
                self.assertEqual(root / "application/backend", kwargs["cwd"])
                return subprocess.CompletedProcess(args, 0, self.manifest_stdout, "")
            previous_cwd = Path.cwd()
            try:
                os.chdir(root)
                with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                        patch.object(verifier, "trusted_execution_context", return_value=context), \
                        patch.object(verifier, "guard_source_executor") as guards, \
                        patch.object(verifier, "producer_artifacts", return_value=[{"id": 1}]), \
                        patch.object(verifier, "authenticated_report", side_effect=authenticate), \
                        patch.object(subprocess, "run", side_effect=manifest) as canonical:
                    self.assertEqual(1000, verifier.verify(SHA, evidence_root))
                    try:
                        self.assertEqual(1000, verifier.verify(SHA, "evidence"))
                    except KeyError as error:
                        self.fail(f"Relative evidence path missed its authenticated report key: {error}")
                    self.assertEqual(4, guards.call_count)
                    self.assertEqual(4, canonical.call_count)
                    self.assertEqual(verifier.REPORT_NAMES * 2, authenticated)
                    (root / "evidence-link").symlink_to(evidence_root, target_is_directory=True)
                    with self.assertRaisesRegex(ValueError, "symlink"):
                        verifier.verify(SHA, "evidence-link")
                    self.assertEqual(4, canonical.call_count)
                    self.assertEqual(verifier.REPORT_NAMES * 2, authenticated)
            finally:
                os.chdir(previous_cwd)

    def test_fabricated_but_self_consistent_source_bindings_are_denied(self):
        for field, value in [("path", "src/cljs/nonexistent.cljs"), ("original-sha256", "c" * 64),
                             ("mutated-sha256", "d" * 64), ("fingerprint", "f" * 64)]:
            with self.subTest(field=field):
                batches = fixture_batches()
                mutant = batches[0]["mutants"][0]
                for binding in [mutant["source-binding"], mutant["compile"]["payload"]["source-binding"],
                                mutant["result"]["source-binding"]]:
                    binding[field] = value
                if field == "fingerprint":
                    mutant["fingerprint"] = value
                with self.assertRaisesRegex(ValueError, "canonical checked-out source"):
                    self.verify(batches)

    def test_reusing_one_binding_under_new_identity_is_denied(self):
        batches = fixture_batches()
        first, second = batches[0]["mutants"][:2]
        for binding in [second["source-binding"], second["compile"]["payload"]["source-binding"],
                        second["result"]["source-binding"]]:
            binding["mutated-sha256"] = first["source-binding"]["mutated-sha256"]
        with self.assertRaisesRegex(ValueError, "canonical checked-out source"):
            self.verify(batches)

    def test_actual_candidate_in_wrong_batch_is_denied(self):
        batches = fixture_batches()
        batches[0]["mutants"][0], batches[1]["mutants"][0] = batches[1]["mutants"][0], batches[0]["mutants"][0]
        with self.assertRaisesRegex(ValueError, "different canonical batch"):
            self.verify(batches)

    def test_canonical_partition_prefix_cannot_skip_first_candidate(self):
        for selection in ["replace-first", "omit-first-in-order"]:
            with self.subTest(selection=selection):
                batches = fixture_batches()
                later = canonical_candidate_fixture(1001)
                if selection == "replace-first":
                    batches[1]["mutants"][0] = later
                else:
                    batches[1]["mutants"].pop(0)
                    batches[1]["mutants"].append(later)
                self.assertEqual(250, len(batches[1]["mutants"]))
                self.assertEqual(1000, len({mutant["fingerprint"] for batch in batches
                                           for mutant in batch["mutants"]}))
                with self.assertRaisesRegex(ValueError, "Batch omits or reorders canonical candidates"):
                    self.verify(batches)

    def test_canonical_partition_prefix_cannot_reorder_candidates(self):
        batches = fixture_batches()
        mutants = batches[1]["mutants"]
        mutants[0], mutants[1] = mutants[1], mutants[0]
        with self.assertRaisesRegex(ValueError, "Batch omits or reorders canonical candidates"):
            self.verify(batches)

    def test_equivalent_exclusions_must_be_canonical_in_this_batch(self):
        for key in ["f" * 64, SOURCE_MANIFEST["candidates"][1001]["source-binding"]["fingerprint"]]:
            with self.subTest(fingerprint=key):
                batches = fixture_batches()
                self.assertNotIn(key, {mutant["fingerprint"] for batch in batches for mutant in batch["mutants"]})
                batches[0]["excluded"] = [{"fingerprint": key, "relative-path": "a.cljc",
                                           "source-sha256": "c" * 64, "basis-source-sha": "d" * 40,
                                           "manifest-sha256": "e" * 64,
                                           "manifest-path": "backend/mutation/reviewed-equivalents.edn",
                                           "rationale": "Synthetic reviewed equivalent fixture",
                                           "review-reference": "https://github.com/open-hax/knoxx/pull/1"}]
                with self.assertRaisesRegex(ValueError, "Equivalent exclusion is not a canonical candidate of this batch"):
                    self.verify(batches)

    def test_exclusion_beyond_canonical_prefix_receives_zero_credit(self):
        batches = fixture_batches()
        key = SOURCE_MANIFEST["candidates"][1000]["source-binding"]["fingerprint"]
        batches[0]["excluded"] = [{"fingerprint": key, "relative-path": "a.cljc",
                                   "source-sha256": "c" * 64, "basis-source-sha": "d" * 40,
                                   "manifest-sha256": "e" * 64,
                                   "manifest-path": "backend/mutation/reviewed-equivalents.edn",
                                   "rationale": "Synthetic reviewed equivalent fixture",
                                   "review-reference": "https://github.com/open-hax/knoxx/pull/1"}]
        self.assertNotIn(key, {mutant["fingerprint"] for batch in batches for mutant in batch["mutants"]})
        self.assertEqual(1000, self.verify(batches))

    def test_canonical_command_failure_and_source_drift_are_denied(self):
        for error in [OSError("missing canonical runtime"), subprocess.CalledProcessError(1, "clojure"),
                      subprocess.TimeoutExpired("clojure", 600)]:
            with self.subTest(error=type(error).__name__):
                with self.assertRaisesRegex(ValueError, "regeneration failed"):
                    self.verify(fixture_batches(), transport=lambda _args, _kwargs: (_ for _ in ()).throw(error))
        calls = 0
        def changed_source(args, _kwargs):
            nonlocal calls
            calls += 1
            manifest = copy.deepcopy(SOURCE_MANIFEST)
            if calls == 2:
                manifest["candidates"][0]["source-binding"]["mutated-sha256"] = "f" * 64
            return subprocess.CompletedProcess(args, 0, json.dumps(manifest), "")
        with self.assertRaisesRegex(ValueError, "changed during evidence verification"):
            self.verify(fixture_batches(), transport=changed_source)
        self.assertEqual(2, calls)

    def test_canonical_manifest_coordinates_are_not_report_authority(self):
        for alter in [lambda manifest: manifest.update({"source-sha": "f" * 40}),
                      lambda manifest: manifest["candidates"][0].update({"ordinal": 1}),
                      lambda manifest: manifest["candidates"].append(copy.deepcopy(manifest["candidates"][0]))]:
            manifest = copy.deepcopy(SOURCE_MANIFEST)
            alter(manifest)
            with self.assertRaises(ValueError):
                self.verify(fixture_batches(), manifest=manifest)

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
        ]
        for change in changes:
            with self.subTest(change=change):
                batches = fixture_batches()
                batches[0]["mutants"][0]["result"].update(change)
                with self.assertRaises((ValueError, KeyError)):
                    self.verify(batches)

    def test_fatal_marker_in_actual_output_is_denied(self):
        batches = fixture_batches()
        result = batches[0]["mutants"][0]["result"]
        output = result["output"] + "\n[shadow-test-guard] FATAL fixture"
        result.update(evidence(output, 1))
        self.assertFalse(result["fatal?"])  # Exercise the actual output clause.
        with self.assertRaisesRegex(ValueError, "Fatal execution cannot qualify"):
            self.verify(batches)

    def test_unmatched_completed_summary_is_denied(self):
        batches = fixture_batches()
        result = batches[0]["mutants"][0]["result"]
        output = result["output"] + "\n1 failure, 0 errors."
        result.update(evidence(output, 1))
        # Keep the other recorded counters consistent with the forged output,
        # so removing only the summary-count guard would wrongly admit it.
        result["failures"] = 2
        with self.assertRaisesRegex(ValueError, "Missing or unmatched completed summaries"):
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
        with self.assertRaisesRegex(ValueError, "Equivalent exclusion cannot receive kill credit"):
            self.verify(batches)

    def test_explicit_exclusion_receives_zero_evaluated_credit(self):
        batches = fixture_batches()
        removed = batches[0]["mutants"].pop(0)
        replacement = copy.deepcopy(removed)
        actual_binding = copy.deepcopy(SOURCE_MANIFEST["candidates"][1000]["source-binding"])
        fingerprint = actual_binding["fingerprint"]
        replacement.update(id="replacement", fingerprint=fingerprint)
        for binding in [replacement["source-binding"], replacement["compile"]["payload"]["source-binding"],
                        replacement["result"]["source-binding"]]:
            binding.update(actual_binding)
        batches[0]["mutants"].append(replacement)
        batches[0]["excluded"] = [{"fingerprint": removed["fingerprint"], "relative-path": "a.cljc", "source-sha256": "c" * 64,
                                   "basis-source-sha": "d" * 40, "manifest-sha256": "e" * 64,
                                   "manifest-path": "backend/mutation/reviewed-equivalents.edn",
                                   "rationale": "Synthetic reviewed equivalent fixture", "review-reference": "https://github.com/open-hax/knoxx/pull/1"}]
        self.assertEqual(250, len(batches[0]["mutants"]))
        seen = {mutant["fingerprint"] for batch in batches for mutant in batch["mutants"]}
        self.assertNotIn(removed["fingerprint"], seen)
        self.assertEqual(1000, len(seen))
        self.assertEqual(1000, self.verify(batches))

    def test_manifest_formula_matches_current_runner_vector(self):
        self.assertEqual(RUNNER_PAYLOAD_DIGEST, verifier.manifest_digest(RUNNER_DIGEST_VECTOR))
        self.assertEqual(RUNNER_CODE_DIGEST, verifier.manifest_digest(
            [entry for entry in RUNNER_DIGEST_VECTOR if entry["kind"] != "source-map"]))
        self.assertEqual(RUNNER_PAYLOAD_DIGEST, fixture_digest(RUNNER_DIGEST_VECTOR))

    def test_actual_emitted_loader_fragment_matches_layout(self):
        blobs = {LOADER: RUNNER_LOADER_FRAGMENT, RUNTIME + "/goog.base.js": b"var goog = {};\n",
                 RUNTIME + "/" + RUNNER_LOADER_IMPORTS[0]: b"void 0;\n",
                 RUNTIME + "/" + RUNNER_LOADER_IMPORTS[1]: b"void 1;\n"}
        files = [{"path": path, "kind": "loader" if path == LOADER else "runtime-js",
                  "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
                 for path, raw in sorted(blobs.items())]
        fixture = {"schema-version": 1, "source-sha": SHA, "source-binding": {"kind": "baseline"},
                   "loader-path": LOADER, "runtime-root": RUNTIME, "imports": RUNNER_LOADER_IMPORTS,
                   "files": files, "sha256": fixture_digest(files), "code-sha256": fixture_digest(files)}
        with tempfile.TemporaryDirectory(prefix="knoxx-loader-vector-") as directory:
            store = Path(directory)
            for raw in blobs.values():
                (store / hashlib.sha256(raw).hexdigest()).write_bytes(raw)
            referenced = set()
            self.assertEqual(hashlib.sha256(RUNNER_LOADER_FRAGMENT).hexdigest(),
                             verifier.payload_evidence(fixture, SHA, {"kind": "baseline"}, store, referenced))
            self.assertEqual({entry["sha256"] for entry in files}, referenced)

    def test_shared_blobs_are_read_once_then_rechecked_at_end(self):
        original = Path.read_bytes
        reads = Counter()
        def observed(path):
            if path.parent.name == "payload-store":
                reads[path.name] += 1
            return original(path)
        with patch.object(Path, "read_bytes", observed):
            self.assertEqual(1000, self.verify(fixture_batches()))
        expected = {hashlib.sha256(raw).hexdigest(): 2
                    for raw in [*FIXTURE_BYTES.values(), b"var answer = true;\n", b"var answer = false;\n"]}
        expected[hashlib.sha256(LOADER_BYTES).hexdigest()] = 1005  # Parse1004 payloads, then final rehash.
        self.assertEqual(expected, dict(reads))

    def test_each_verify_call_revalidates_its_own_store(self):
        self.assertEqual(1000, self.verify(fixture_batches()))
        digest = hashlib.sha256(b"var answer = false;\n").hexdigest()
        with self.assertRaisesRegex(ValueError, "Compiled payload bytes mismatch"):
            self.verify(fixture_batches(), lambda store: (store / digest).write_bytes(b"tampered\n"))

    def test_cached_blob_size_must_match_each_manifest(self):
        batches = fixture_batches()
        compiled = batches[0]["mutants"][0]["compile"]
        files = compiled["payload"]["files"]
        next(entry for entry in files if entry["path"] == RUNTIME + "/goog.base.js")["bytes"] += 1
        compiled["payload"]["sha256"] = fixture_digest(files)
        compiled["payload"]["code-sha256"] = fixture_digest([entry for entry in files if entry["kind"] != "source-map"])
        batches[0]["mutants"][0]["result"]["input-payload-sha256"] = compiled["payload"]["sha256"]
        with self.assertRaisesRegex(ValueError, "Compiled payload bytes mismatch"):
            self.verify(batches)

    def test_final_recheck_detects_same_inode_size_and_mtime_tamper(self):
        original = Path.read_bytes
        loader_digest = hashlib.sha256(LOADER_BYTES).hexdigest()
        runtime_digest = hashlib.sha256(b"var answer = false;\n").hexdigest()
        loader_reads = 0
        def tamper_after_last_payload(path):
            nonlocal loader_reads
            raw = original(path)
            if path.parent.name == "payload-store" and path.name == loader_digest:
                loader_reads += 1
                if loader_reads == 1004:
                    target = path.parent / runtime_digest
                    before = target.stat()
                    target.write_bytes(b"var answer = FALSE;\n")
                    os.utime(target, ns=(before.st_atime_ns, before.st_mtime_ns))
                    after = target.stat()
                    self.assertEqual((before.st_ino, before.st_size, before.st_mtime_ns),
                                     (after.st_ino, after.st_size, after.st_mtime_ns))
            return raw
        with patch.object(Path, "read_bytes", tamper_after_last_payload):
            with self.assertRaisesRegex(ValueError, "Verified payload blob changed before final custody check"):
                self.verify(fixture_batches())


class ProductionAuthenticationTests(unittest.TestCase):
    """Mocked official adapter characterization, never real cryptographic credit."""

    def fixture(self, reusable=False):
        """Return inert direct/reusable native facts; no real token or transport."""
        caller = "open-hax/services" if reusable else "open-hax/knoxx"
        controller, source = "c" * 40, "d" * 40 if reusable else "c" * 40
        path = ".github/workflows/deploy-stack.yml" if reusable else verifier.SIGNER_PATH
        identity = 92 if reusable else 91
        env = {"GITHUB_ACTIONS": "true", "GITHUB_SERVER_URL": "https://github.com",
               "GITHUB_API_URL": "https://api.github.com", "GITHUB_REF": "refs/heads/main",
               "GITHUB_REPOSITORY": caller, "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2",
               "GITHUB_SHA": source, "GITHUB_EVENT_NAME": "workflow_dispatch",
               "GITHUB_WORKFLOW_REF": f"{caller}/{path}@refs/heads/main", "GH_TOKEN": "inert-fixture"}
        repos = {repository: {"full_name": repository, "id": repo_id, "default_branch": "main",
                              "fork": False, "archived": False}
                 for repository, repo_id in [("open-hax/knoxx", 91), ("open-hax/services", 92)]}
        run = {"id": 123, "run_attempt": 2, "head_sha": source, "head_branch": "main",
               "event": "workflow_dispatch", "repository": repos[caller], "path": path,
               "referenced_workflows": [{"path": f"open-hax/knoxx/{verifier.SIGNER_PATH}@{controller}",
                                         "sha": controller}] if reusable else []}
        context = {"caller": caller, "caller_id": identity, "caller_sha": source, "caller_path": path,
                   "controller_sha": controller, "run_id": 123, "attempt": 2, "ref": "refs/heads/main",
                   "invocation": f"https://github.com/{caller}/actions/runs/123/attempts/2"}
        data = {f"repos/{repository}": value for repository, value in repos.items()}
        data.update({f"repos/{repository}/branches/main": {"name": "main", "protected": True,
                     "commit": {"sha": controller if repository == "open-hax/knoxx" else "d" * 40}}
                     for repository in repos})
        data[f"repos/{caller}/actions/runs/123/attempts/2"] = run
        return env, data, context

    def test_actual_adapter_uses_native_current_direct_and_reusable_coordinates(self):
        """Characterize the real command adapter with controlled native responses."""
        for reusable in (False, True):
            env, data, expected = self.fixture(reusable)
            calls = []
            def response(args, **kwargs):
                self.assertEqual(["gh", "api", "--method", "GET"], args[:4])
                self.assertEqual({"check", "capture_output", "text", "timeout"}, set(kwargs))
                self.assertEqual(120, kwargs["timeout"])
                calls.append(args[-1])
                return subprocess.CompletedProcess(args, 0, json.dumps(data[args[-1]]), "")
            with patch.dict(os.environ, env, clear=True), patch.object(subprocess, "run", side_effect=response):
                self.assertEqual(expected, verifier.trusted_execution_context())
            self.assertEqual(5, len(calls))

    def test_untrusted_or_stale_native_context_never_supplies_authority(self):
        """Deny stale or foreign native caller/controller facts."""
        for change in ("branch-unprotected", "caller-head", "wrong-caller-path", "foreign-caller-id",
                       "wrong-attempt", "missing-called", "duplicate-called", "old-controller"):
            env, data, _ = self.fixture(True)
            run = data["repos/open-hax/services/actions/runs/123/attempts/2"]
            if change == "branch-unprotected":
                data["repos/open-hax/services/branches/main"]["protected"] = False
            elif change == "caller-head":
                run["head_sha"] = "e" * 40
            elif change == "wrong-caller-path":
                run["path"] = ".github/workflows/other.yml"
            elif change == "foreign-caller-id":
                run["repository"] = {"id": 93, "full_name": "open-hax/services"}
            elif change == "wrong-attempt":
                run["run_attempt"] = 1
            elif change == "missing-called":
                run["referenced_workflows"] = []
            elif change == "duplicate-called":
                run["referenced_workflows"] *= 2
            else:
                run["referenced_workflows"][0]["sha"] = "e" * 40
            with self.subTest(change=change), patch.dict(os.environ, env, clear=True), \
                    patch.object(subprocess, "run", side_effect=lambda args, **_:
                                 subprocess.CompletedProcess(args, 0, json.dumps(data[args[-1]]), "")):
                with self.assertRaises(ValueError):
                    verifier.trusted_execution_context()

    def verified_result(self, raw, context):
        """Return an explicitly mocked official certificate and signed subject shape."""
        certificate = {"issuer": "https://token.actions.githubusercontent.com",
                       "buildSignerURI": f'https://github.com/open-hax/knoxx/{verifier.SIGNER_PATH}@{context["controller_sha"]}',
                       "buildSignerDigest": context["controller_sha"],
                       "sourceRepositoryURI": f'https://github.com/{context["caller"]}',
                       "sourceRepositoryIdentifier": str(context["caller_id"]),
                       "sourceRepositoryDigest": context["caller_sha"], "sourceRepositoryRef": context["ref"],
                       "buildConfigURI": f'https://github.com/{context["caller"]}/{context["caller_path"]}@{context["ref"]}',
                       "buildConfigDigest": context["caller_sha"], "runnerEnvironment": "github-hosted",
                       "runInvocationURI": context["invocation"]}
        return [{"verificationResult": {"signature": {"certificate": certificate},
                  "verifiedTimestamps": [{"type": "Tlog"}],
                  "statement": {"predicateType": "https://slsa.dev/provenance/v1",
                                "subject": [{"digest": {"sha256": hashlib.sha256(raw).hexdigest()}}],
                                "predicate": {"untrusted": "not policy authority"}}}}]

    def test_official_verifier_command_and_flat_certificate_contract(self):
        """Require official verification flags and supported flat identity fields."""
        _, _, context = self.fixture(True)
        with tempfile.TemporaryDirectory(prefix="knoxx-auth-contract-") as directory:
            path = Path(directory, "batch-0.edn.json"); raw = b'{"fixture":"inert"}\n'; path.write_bytes(raw)
            expected = ["gh", "attestation", "verify", str(path), "--repo", context["caller"],
                        "--signer-workflow", f"open-hax/knoxx/{verifier.SIGNER_PATH}",
                        "--signer-digest", context["controller_sha"], "--source-digest", context["caller_sha"],
                        "--source-ref", context["ref"], "--deny-self-hosted-runners", "--format", "json"]
            def response(args, **_):
                self.assertEqual(expected, args)
                return subprocess.CompletedProcess(args, 0, json.dumps(self.verified_result(raw, context)), "")
            with patch.object(subprocess, "run", side_effect=response) as transport:
                verifier.authenticated_report(path, raw, context)
            self.assertEqual(1, transport.call_count)

    def test_certificate_identity_subject_and_attempt_adverses_are_denied(self):
        """Reject altered signer, caller, attempt or subject evidence."""
        _, _, context = self.fixture(True)
        with tempfile.TemporaryDirectory(prefix="knoxx-auth-adverse-") as directory:
            path = Path(directory, "batch-0.edn.json"); raw = b'{}\n'; path.write_bytes(raw)
            for field in ("issuer", "buildSignerURI", "buildSignerDigest", "sourceRepositoryURI",
                          "sourceRepositoryIdentifier", "sourceRepositoryDigest", "sourceRepositoryRef",
                          "buildConfigURI", "buildConfigDigest", "runnerEnvironment", "runInvocationURI",
                          "subject", "timestamps", "empty"):
                results = self.verified_result(raw, context)
                verified = results[0]["verificationResult"]
                if field == "subject":
                    verified["statement"]["subject"][0]["digest"]["sha256"] = "e" * 64
                elif field == "timestamps":
                    verified["verifiedTimestamps"] = []
                elif field == "empty":
                    results = []
                else:
                    verified["signature"]["certificate"][field] = "foreign-or-stale"
                with self.subTest(field=field), patch.object(subprocess, "run", return_value=
                        subprocess.CompletedProcess([], 0, json.dumps(results), "")):
                    with self.assertRaisesRegex(ValueError, "No authenticated"):
                        verifier.authenticated_report(path, raw, context)

    def test_invalid_official_execution_and_changed_subject_bytes_are_denied(self):
        """Refuse unavailable verification and report replacement after verification."""
        _, _, context = self.fixture()
        with tempfile.TemporaryDirectory(prefix="knoxx-auth-failure-") as directory:
            path = Path(directory, "batch-0.edn.json"); raw = b'{}\n'; path.write_bytes(raw)
            for error in (OSError("missing official tool"), subprocess.CalledProcessError(1, "gh"),
                          subprocess.TimeoutExpired("gh", 120)):
                with self.subTest(error=type(error).__name__), patch.object(subprocess, "run", side_effect=error):
                    with self.assertRaisesRegex(ValueError, "verification failed"):
                        verifier.authenticated_report(path, raw, context)
            def changed(args, **_):
                path.write_bytes(b'{"changed":true}\n')
                return subprocess.CompletedProcess(args, 0, json.dumps(self.verified_result(raw, context)), "")
            with patch.object(subprocess, "run", side_effect=changed):
                with self.assertRaisesRegex(ValueError, "report bytes changed"):
                    verifier.authenticated_report(path, raw, context)

    def test_fixed_artifact_namespace_requires_completed_current_producers(self):
        """Require complete successful producer jobs and exact native artifact custody."""
        _, _, context = self.fixture(True)
        sha = "a" * 40
        jobs = [{"id": index + 1, "name": name, "status": "completed", "conclusion": "success"}
                for index, name in enumerate(["Knoxx integration-e2e", *[f"Knoxx mutation batch {i}" for i in range(4)]])]
        artifacts = [{"id": index + 10, "name": f"production-mutation-123-2-{sha}-{index}",
                      "expired": False, "size_in_bytes": 123, "digest": "sha256:" + "f" * 64,
                      "workflow_run": {"id": 123, "repository_id": 92, "head_repository_id": 92,
                                       "head_sha": context["caller_sha"], "head_branch": "main"}}
                     for index in range(4)]
        with patch.object(verifier, "native_pages", side_effect=lambda _path, key: jobs if key == "jobs" else artifacts):
            self.assertEqual(artifacts, verifier.producer_artifacts(sha, context))
            for kind in ("failed-job", "missing-job", "duplicate-job", "stale-artifact", "expired-artifact", "foreign-run"):
                changed_jobs, changed_artifacts = copy.deepcopy(jobs), copy.deepcopy(artifacts)
                if kind == "failed-job": changed_jobs[0]["conclusion"] = "failure"
                elif kind == "missing-job": changed_jobs.pop()
                elif kind == "duplicate-job": changed_jobs.append(copy.deepcopy(changed_jobs[0]))
                elif kind == "stale-artifact": changed_artifacts[0]["name"] = f"production-mutation-123-1-{sha}-0"
                elif kind == "expired-artifact": changed_artifacts[0]["expired"] = True
                else: changed_artifacts[0]["workflow_run"]["id"] = 122
                with self.subTest(kind=kind), patch.object(verifier, "native_pages", side_effect=
                        lambda _path, key: changed_jobs if key == "jobs" else changed_artifacts):
                    with self.assertRaises(ValueError): verifier.producer_artifacts(sha, context)


    def test_unsigned_or_malformed_official_results_cannot_supply_identity(self):
        """Absent native fields or an unverified predicate cannot authorize a report."""
        _, _, context = self.fixture()
        with tempfile.TemporaryDirectory(prefix="knoxx-auth-unsupported-") as directory:
            path = Path(directory, "batch-0.edn.json")
            raw = b'{}\n'
            path.write_bytes(raw)
            for results in ([{}], [{"attestation": {"predicate": context}}],
                            [{"verificationResult": {"statement": {"subject": []}}}]):
                with self.subTest(results=results), patch.object(subprocess, "run", return_value=
                        subprocess.CompletedProcess([], 0, json.dumps(results), "")):
                    with self.assertRaisesRegex(ValueError, "No authenticated"):
                        verifier.authenticated_report(path, raw, context)
            for output in ("not-json", "{}", "null"):
                with self.subTest(output=output), patch.object(subprocess, "run", return_value=
                        subprocess.CompletedProcess([], 0, output, "")):
                    with self.assertRaises(ValueError):
                        verifier.authenticated_report(path, raw, context)

    def test_executor_input_equality_and_physical_source_are_mandatory(self):
        """Use controlled committed reads to deny old helpers or changed local bytes."""
        _, _, context = self.fixture()
        with tempfile.TemporaryDirectory(prefix="knoxx-executor-contract-") as directory:
            root = Path(directory)
            helper = root / "scripts/verify-production-mutation-evidence.py"
            helper.parent.mkdir()
            helper.write_bytes(b"trusted controller helper\n")
            application = root / "application"
            application.mkdir()
            selected_helper = application / helper.relative_to(root)
            selected_helper.parent.mkdir()
            selected_helper.write_bytes(helper.read_bytes())
            expected = {"scripts/verify-production-mutation-evidence.py": ("100644", "b" * 40)}
            def committed(checkout, *args):
                if args == ("rev-parse", "HEAD"):
                    return (SHA if checkout == application else context["controller_sha"]).encode() + b"\n"
                if args[:2] == ("merge-base", "--is-ancestor"):
                    return b""
                if args[0] == "show":
                    return b"trusted controller helper\n"
                self.fail(f"Unexpected controlled Git read: {args}")
            with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                    patch.object(verifier, "__file__", str(helper)), \
                    patch.object(verifier, "git_bytes", side_effect=committed), \
                    patch.object(verifier, "executor_entries", return_value=expected):
                self.assertEqual(root, verifier.guard_source_executor(SHA, context, application))
                selected_helper.write_bytes(b"changed source helper\n")
                with self.assertRaisesRegex(ValueError, "application executor input changed"):
                    verifier.guard_source_executor(SHA, context, application)
                selected_helper.write_bytes(helper.read_bytes())
                helper.write_bytes(b"changed controller helper\n")
                with self.assertRaisesRegex(ValueError, "Controller executor input changed"):
                    verifier.guard_source_executor(SHA, context, application)
                helper.write_bytes(b"trusted controller helper\n")
                with patch.object(verifier, "executor_entries", side_effect=[expected, {}]):
                    with self.assertRaisesRegex(ValueError, "executor differs"):
                        verifier.guard_source_executor(SHA, context, application)

    def executor_fixture(self):
        """Committed byte fixtures exercise the real selector; no Git/native reads."""
        contents = {path: b"trusted executor input\n" for path in verifier.HELPER_FILES}
        contents.update({prefix + "fixture.txt": b"tracked helper\n" for prefix in verifier.HELPER_PREFIXES})
        contents["backend/shadow-cljs.edn"] = (Path(__file__).resolve().parent.parent
                                              / "backend/shadow-cljs.edn").read_bytes()
        contents.update({"backend/src/cljs/knoxx/backend/macros.cljc":
                         b"(ns knoxx.backend.macros)\n(defmacro fixture [] true)\n",
                         "shared/src/cljs/fixture/macros.cljc":
                         b"(ns fixture.macros)\n(defmacro fixture [] true)\n",
                         "backend/src/clj/fixture.clj": b"(ns fixture)\n",
                         "backend/src/cljs/ordinary.cljs": b"(ns ordinary)\n",
                         "frontend/ordinary.cljc": b"(ns frontend)\n"})
        return contents

    def executor_tree(self, contents, override=None):
        """Encode inert ls-tree byte rows, preserving modes and blob identities."""
        rows = []
        for path, raw in sorted(contents.items()):
            mode, kind = (override or {}).get(path, ("100644", "blob"))
            blob = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
            rows.append(f"{mode} {kind} {blob}\t{path}".encode() + b"\0")
        return b"".join(rows)

    def test_executor_inventory_includes_portable_jvm_compiler_sources(self):
        contents = self.executor_fixture()
        with patch.object(verifier, "git_bytes", side_effect=lambda root, *args:
                          contents["backend/shadow-cljs.edn"] if args[0] == "show"
                          else self.executor_tree(contents)) as transport:
            entries = verifier.executor_entries(Path("/inert-controller"), "c" * 40)
        self.assertEqual([(Path("/inert-controller"), "ls-tree", "-r", "-z", "c" * 40),
                          (Path("/inert-controller"), "show", "c" * 40 + ":backend/shadow-cljs.edn")],
                         [observed.args for observed in transport.call_args_list])
        self.assertIn("backend/src/cljs/knoxx/backend/macros.cljc", entries)
        self.assertNotIn("shared/src/cljs/fixture/macros.cljc", entries)
        self.assertIn("backend/src/clj/fixture.clj", entries)
        self.assertNotIn("backend/src/cljs/ordinary.cljs", entries)
        self.assertNotIn("frontend/ordinary.cljc", entries)
        self.assertEqual("100644", entries["backend/src/cljs/knoxx/backend/macros.cljc"][0])

    def test_selected_source_cljc_macro_drift_is_refused(self):
        """C/S equality must see actual changed macro blobs, using the real inventory."""
        contents = self.executor_fixture()
        controller, selected = "c" * 40, "b" * 40
        for macro in ("backend/src/cljs/knoxx/backend/macros.cljc",):
            with self.subTest(macro=macro), tempfile.TemporaryDirectory(prefix="knoxx-macro-source-") as directory:
                root = Path(directory)
                for path, raw in contents.items():
                    target = root / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
                changed = {**contents, macro: contents[macro].replace(b"true", b"false")}
                def committed(checkout, *args):
                    self.assertEqual(root, checkout)
                    if args == ("rev-parse", "HEAD"): return controller.encode() + b"\n"
                    if args == ("merge-base", "--is-ancestor", selected, controller): return b""
                    if args[:3] == ("ls-tree", "-r", "-z"):
                        self.assertIn(args[3], (controller, selected))
                        return self.executor_tree(contents if args[3] == controller else changed)
                    if args[0] == "show": return contents[args[1].split(":", 1)[1]]
                    self.fail(f"Unexpected controlled Git read: {args}")
                with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                        patch.object(verifier, "__file__", str(root / "scripts/verify-production-mutation-evidence.py")), \
                        patch.object(verifier, "git_bytes", side_effect=committed):
                    with self.assertRaisesRegex(ValueError, "Selected source executor differs"):
                        verifier.guard_source_executor(selected, {"controller_sha": controller})

    def test_selected_application_cljc_differences_preserve_executor_contract(self):
        """Real C/S inventory allows app changes and still rejects macro/config drift."""
        contents = self.executor_fixture()
        contents.update({"backend/src/cljs/knoxx/backend/domain/fixture.cljc": b"(def app true)\n",
                         "shared/src/cljs/fixture/domain.cljc": b"(def shared true)\n"})
        controller, selected = "c" * 40, "b" * 40
        for changed_path in ("backend/src/cljs/knoxx/backend/domain/fixture.cljc",
                             "shared/src/cljs/fixture/domain.cljc",
                             "backend/src/cljs/knoxx/backend/macros.cljc",
                             "backend/shadow-cljs.edn", "backend/deps.edn"):
            with self.subTest(path=changed_path), tempfile.TemporaryDirectory(prefix="knoxx-app-source-") as directory:
                root = Path(directory)
                application = root / "application"
                changed = {**contents, changed_path: contents[changed_path] + b"; selected difference\n"}
                for checkout, files in ((root, contents), (application, changed)):
                    for path, raw in files.items():
                        target = checkout / path
                        target.parent.mkdir(parents=True, exist_ok=True)
                        target.write_bytes(raw)
                def committed(checkout, *args):
                    self.assertIn(checkout, (root, application))
                    if args == ("rev-parse", "HEAD"):
                        return (selected if checkout == application else controller).encode() + b"\n"
                    if args == ("merge-base", "--is-ancestor", selected, controller): return b""
                    if args[:3] == ("ls-tree", "-r", "-z"):
                        self.assertIn(args[3], (controller, selected))
                        return self.executor_tree(contents if args[3] == controller else changed)
                    if args[0] == "show":
                        commit, relative = args[1].split(":", 1)
                        self.assertIn(commit, (controller, selected))
                        return (contents if commit == controller else changed)[relative]
                    self.fail(f"Unexpected controlled Git read: {args}")
                with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                        patch.object(verifier, "__file__", str(root / "scripts/verify-production-mutation-evidence.py")), \
                        patch.object(verifier, "git_bytes", side_effect=committed):
                    if changed_path.endswith("domain/fixture.cljc") or changed_path.endswith("fixture/domain.cljc"):
                        try:
                            self.assertEqual(root, verifier.guard_source_executor(selected, {"controller_sha": controller}, application))
                        except ValueError as error:
                            self.fail(f"Ordinary selected application CLJC was classified as executor: {error}")
                    else:
                        with self.assertRaisesRegex(ValueError,
                                "Selected source executor differs|Unsupported portable macro configuration"):
                            verifier.guard_source_executor(selected, {"controller_sha": controller}, application)
        missing = {path: raw for path, raw in contents.items()
                   if path != "backend/src/cljs/knoxx/backend/macros.cljc"}
        with patch.object(verifier, "git_bytes", return_value=self.executor_tree(missing)):
            with self.assertRaisesRegex(ValueError, "Incomplete committed executor inventory"):
                verifier.executor_entries(Path("/inert-controller"), controller)
        unknown = {**contents, "backend/src/cljs/fixture/macros.cljc": b"(defmacro new-macro [] true)\n"}
        unknown["backend/shadow-cljs.edn"] = contents["backend/shadow-cljs.edn"].replace(
            b":macros {:knoxx.backend.macros true}",
            b":macros {:knoxx.backend.macros true :fixture.macros true}")
        self.assertNotEqual(contents["backend/shadow-cljs.edn"], unknown["backend/shadow-cljs.edn"])
        # Equal unknown C/S configuration must not silently omit a new macro input.
        with patch.object(verifier, "git_bytes", side_effect=lambda root, *args:
                          unknown["backend/shadow-cljs.edn"] if args[0] == "show"
                          else self.executor_tree(unknown)):
            for commit in (controller, selected):
                with self.subTest(unknown_config=commit), self.assertRaisesRegex(ValueError,
                        "Unsupported portable macro configuration"):
                    verifier.executor_entries(Path("/inert-controller"), commit)

    def test_local_cljc_macro_bytes_are_checked_in_both_checkouts(self):
        """Matching committed blobs cannot hide changed controller/application bytes."""
        contents = self.executor_fixture()
        controller, selected = "c" * 40, "b" * 40
        macro = "backend/src/cljs/knoxx/backend/macros.cljc"
        with tempfile.TemporaryDirectory(prefix="knoxx-macro-physical-") as directory:
            root = Path(directory); application = root / "application"; application.mkdir()
            for checkout in (root, application):
                for path, raw in contents.items():
                    target = checkout / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
            def committed(checkout, *args):
                if args == ("rev-parse", "HEAD"):
                    return (selected if checkout == application else controller).encode() + b"\n"
                if args == ("merge-base", "--is-ancestor", selected, controller): return b""
                if args[:3] == ("ls-tree", "-r", "-z"): return self.executor_tree(contents)
                if args[0] == "show": return contents[args[1].split(":", 1)[1]]
                self.fail(f"Unexpected controlled Git read: {args}")
            with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                    patch.object(verifier, "__file__", str(root / "scripts/verify-production-mutation-evidence.py")), \
                    patch.object(verifier, "git_bytes", side_effect=committed):
                self.assertEqual(root, verifier.guard_source_executor(selected, {"controller_sha": controller}, application))
                for checkout, message in ((root, "Controller executor input changed"),
                                          (application, "application executor input changed")):
                    with self.subTest(checkout=checkout.name):
                        (checkout / macro).write_bytes(contents[macro].replace(b"true", b"false"))
                        with self.assertRaisesRegex(ValueError, message):
                            verifier.guard_source_executor(selected, {"controller_sha": controller}, application)
                        (checkout / macro).write_bytes(contents[macro])
                self.assertEqual(root, verifier.guard_source_executor(selected, {"controller_sha": controller}, application))

    def test_portable_jvm_compiler_source_modes_cannot_bypass_inventory(self):
        contents = self.executor_fixture()
        macro = "backend/src/cljs/knoxx/backend/macros.cljc"
        for mode, kind in (("120000", "blob"), ("160000", "commit")):
            with self.subTest(mode=mode), patch.object(verifier, "git_bytes", return_value=
                    self.executor_tree(contents, {macro: (mode, kind)})):
                with self.assertRaisesRegex(ValueError, "Unsafe executor input mode"):
                    verifier.executor_entries(Path("/inert-controller"), "c" * 40)

    def test_shared_local_library_manifest_is_required_executor_input(self):
        """The local-root deps manifest belongs to the actual executor inventory."""
        contents = self.executor_fixture()
        contents["shared/deps.edn"] = b'{:paths ["src/cljs"]}\n'
        with patch.object(verifier, "git_bytes", side_effect=lambda root, *args:
                          contents["backend/shadow-cljs.edn"] if args[0] == "show"
                          else self.executor_tree(contents)):
            entries = verifier.executor_entries(Path("/inert-controller"), "c" * 40)
        self.assertIn("shared/deps.edn", entries)
        self.assertEqual("100644", entries["shared/deps.edn"][0])
        missing = {path: raw for path, raw in contents.items() if path != "shared/deps.edn"}
        with patch.object(verifier, "git_bytes", return_value=self.executor_tree(missing)):
            with self.assertRaisesRegex(ValueError, "Incomplete committed executor inventory"):
                verifier.executor_entries(Path("/inert-controller"), "c" * 40)

    def test_selected_source_shared_manifest_drift_is_refused(self):
        """Changing the local library paths cannot keep the real C/S inventory equal."""
        contents = self.executor_fixture()
        contents["shared/deps.edn"] = b'{:paths ["src/cljs"]}\n'
        changed = {**contents, "shared/deps.edn": b'{:paths ["unreviewed-source"]}\n'}
        controller, selected = "c" * 40, "b" * 40
        with tempfile.TemporaryDirectory(prefix="knoxx-shared-manifest-source-") as directory:
            root = Path(directory)
            for path, raw in contents.items():
                target = root / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
            def committed(checkout, *args):
                self.assertEqual(root, checkout)
                if args == ("rev-parse", "HEAD"): return controller.encode() + b"\n"
                if args == ("merge-base", "--is-ancestor", selected, controller): return b""
                if args[:3] == ("ls-tree", "-r", "-z"):
                    self.assertIn(args[3], (controller, selected))
                    return self.executor_tree(contents if args[3] == controller else changed)
                if args[0] == "show": return contents[args[1].split(":", 1)[1]]
                self.fail(f"Unexpected controlled Git read: {args}")
            with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                    patch.object(verifier, "__file__", str(root / "scripts/verify-production-mutation-evidence.py")), \
                    patch.object(verifier, "git_bytes", side_effect=committed):
                with self.assertRaisesRegex(ValueError, "Selected source executor differs"):
                    verifier.guard_source_executor(selected, {"controller_sha": controller})

    def test_local_shared_manifest_bytes_are_checked_in_both_checkouts(self):
        """Matching committed blobs still require the actual controller and S bytes."""
        contents = self.executor_fixture()
        contents["shared/deps.edn"] = b'{:paths ["src/cljs"]}\n'
        controller, selected = "c" * 40, "b" * 40
        manifest = "shared/deps.edn"
        with tempfile.TemporaryDirectory(prefix="knoxx-shared-manifest-physical-") as directory:
            root = Path(directory); application = root / "application"; application.mkdir()
            for checkout in (root, application):
                for path, raw in contents.items():
                    target = checkout / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
            def committed(checkout, *args):
                if args == ("rev-parse", "HEAD"):
                    return (selected if checkout == application else controller).encode() + b"\n"
                if args == ("merge-base", "--is-ancestor", selected, controller): return b""
                if args[:3] == ("ls-tree", "-r", "-z"): return self.executor_tree(contents)
                if args[0] == "show": return contents[args[1].split(":", 1)[1]]
                self.fail(f"Unexpected controlled Git read: {args}")
            with patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                    patch.object(verifier, "__file__", str(root / "scripts/verify-production-mutation-evidence.py")), \
                    patch.object(verifier, "git_bytes", side_effect=committed):
                self.assertEqual(root, verifier.guard_source_executor(selected, {"controller_sha": controller}, application))
                for checkout, message in ((root, "Controller executor input changed"),
                                          (application, "application executor input changed")):
                    with self.subTest(checkout=checkout.name):
                        (checkout / manifest).write_bytes(b'{:paths ["unreviewed-source"]}\n')
                        with self.assertRaisesRegex(ValueError, message):
                            verifier.guard_source_executor(selected, {"controller_sha": controller}, application)
                        (checkout / manifest).write_bytes(contents[manifest])
                self.assertEqual(root, verifier.guard_source_executor(selected, {"controller_sha": controller}, application))

    def test_production_uses_signed_snapshot_and_rechecks_native_and_report_custody(self):
        """Control transport calls while preserving the wrapper's custody ordering."""
        _, _, context = self.fixture()
        with tempfile.TemporaryDirectory(prefix="knoxx-auth-wrapper-") as directory:
            root = Path(directory)
            evidence = root / "evidence"
            evidence.mkdir()
            reports = {}
            for index in range(4):
                path = evidence / f"batch-{index}.edn.json"
                raw = json.dumps({"source_sha": SHA, "batch_index": index}).encode()
                path.write_bytes(raw)
                reports[path] = raw
            for adverse in (None, "changed-report", "changed-context", "changed-artifact"):
                events = []
                def authenticated(path, raw, _context):
                    self.assertEqual(reports[path], raw)
                    events.append("authenticated")
                def structural(sha, supplied_directory, **kwargs):
                    self.assertEqual(SHA, sha)
                    self.assertEqual(evidence, supplied_directory)
                    self.assertEqual(root / "application/backend", kwargs["_backend"])
                    self.assertEqual(reports, kwargs["_reports"])
                    self.assertEqual(["authenticated"] * 4, events)
                    events.append("structural")
                    if adverse == "changed-report":
                        next(iter(reports)).write_bytes(b'{"changed":true}')
                    return 1000
                contexts = [context, {**context, "attempt": 3}] if adverse == "changed-context" else [context, context]
                artifacts = [[{"id": 1}], [{"id": 2}]] if adverse == "changed-artifact" else [[{"id": 1}], [{"id": 1}]]
                with self.subTest(adverse=adverse), patch.dict(os.environ, {"GITHUB_WORKSPACE": str(root)}, clear=True), \
                        patch.object(verifier, "trusted_execution_context", side_effect=contexts), \
                        patch.object(verifier, "guard_source_executor") as guards, \
                        patch.object(verifier, "producer_artifacts", side_effect=artifacts), \
                        patch.object(verifier, "authenticated_report", side_effect=authenticated), \
                        patch.object(verifier, "validate_structural_evidence", side_effect=structural):
                    if adverse is None:
                        self.assertEqual(1000, verifier.verify(SHA, evidence))
                        self.assertEqual(2, guards.call_count)
                    else:
                        with self.assertRaises(ValueError):
                            verifier.verify(SHA, evidence)
                for path, raw in reports.items():
                    path.write_bytes(raw)


class RequiredVerifierGateTests(unittest.TestCase):
    def test_required_workflows_execute_equipped_verifier_suite(self):
        """Both actual workflow routes run the suite with local Python/Java/Clojure."""
        repository = Path(__file__).resolve().parent.parent
        for relative, job_name in ((".github/workflows/ci.yml", "knoxx"),
                                   (".github/workflows/eta-mu-review.yml", "production-evidence")):
            with self.subTest(workflow=relative):
                workflow = yaml.load((repository / relative).read_text(), Loader=yaml.BaseLoader)
                self.assertIn(job_name, workflow["jobs"])
                steps = workflow["jobs"][job_name]["steps"]
                gates = [(index, step) for index, step in enumerate(steps)
                         if "scripts/test-production-mutation-evidence.py" in step.get("run", "")]
                self.assertEqual(1, len(gates), "required verifier invocation must exist once")
                index, gate = gates[0]
                self.assertNotIn("continue-on-error", gate)
                before = steps[:index]
                self.assertTrue(any(step.get("uses", "").startswith("actions/setup-java@")
                                    and step.get("with", {}).get("java-version") == "21" for step in before))
                self.assertTrue(any(step.get("uses", "").startswith("DeLaGuardo/setup-clojure@")
                                    and step.get("with", {}).get("cli") == "1.12.5.1654" for step in before))
                command = gate["run"]
                self.assertIn('python3 -m venv "$RUNNER_TEMP/knoxx-production-evidence-python"', command)
                self.assertIn("'PyYAML==6.0.2'", command)
                self.assertIn('"$RUNNER_TEMP/knoxx-production-evidence-python/bin/python" scripts/test-production-mutation-evidence.py', command)
                if job_name == "production-evidence":
                    self.assertEqual("production-evidence", workflow["jobs"]["review"]["needs"])
                    self.assertEqual("false", workflow["jobs"]["review"]["with"]["setup_eta_mu_toolchain"])


if __name__ == "__main__":
    unittest.main()
