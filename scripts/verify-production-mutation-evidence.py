#!/usr/bin/env python3
"""Require four disjoint batches of completed assertion kills on one source."""
import hashlib
import json
from pathlib import Path
import re
import stat
import subprocess
import sys

LOADER = "target/test/test-ci.cjs"
RUNTIME = ".shadow-cljs/builds/test-ci/dev/out/cljs-runtime"
JS_NAME = r"[A-Za-z0-9_$][A-Za-z0-9_.$-]*\.js"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def completed_evidence(result, exit_code):
    """Verify captured execution independently of runner status labels."""
    require(isinstance(result, dict), "Missing execution evidence")
    require(type(result.get("exit-code")) is int and result["exit-code"] == exit_code,
            "Abnormal execution exit")
    require(result.get("timed-out?") is False and result.get("termination") == "exited"
            and result.get("cleanup-complete?") is True and result.get("capture-truncated?") is False,
            "Incomplete execution or capture")
    output = result.get("output")
    require(isinstance(output, str), "Missing captured output")
    require(result.get("fatal?") is False and "[shadow-test-guard] FATAL" not in output,
            "Fatal execution cannot qualify")
    require(hashlib.sha256(output.encode("utf-8")).hexdigest() == result.get("output-sha256"),
            "Captured output hash mismatch")
    return output


def digest_shape(value, length):
    return isinstance(value, str) and bool(re.fullmatch(r"[0-9a-f]{" + str(length) + "}", value))


def physical_path(path, directory=False):
    """CAS custody must not follow a file or ancestor symlink."""
    absolute = path.absolute()
    for ancestor in [*reversed(absolute.parents), absolute]:
        require(not ancestor.is_symlink(), "Payload custody contains a symlink")
    require(absolute.exists(), "Missing payload custody path")
    mode = absolute.stat().st_mode
    require(stat.S_ISDIR(mode) if directory else stat.S_ISREG(mode), "Invalid payload custody file type")
    return absolute


def source_binding(binding, fingerprint=None):
    require(isinstance(binding, dict), "Missing immutable source binding")
    if fingerprint is None:
        require(binding == {"kind": "baseline"}, "Invalid baseline source binding")
        return
    require(digest_shape(fingerprint, 64)
            and set(binding) == {"kind", "fingerprint", "path", "original-sha256", "mutated-sha256"}
            and binding["kind"] == "mutant" and binding["fingerprint"] == fingerprint,
            "Invalid mutant source binding")
    path = binding.get("path")
    require(isinstance(path, str) and path.startswith("src/cljs/")
            and path.endswith((".cljs", ".cljc"))
            and all(part not in ("", ".", "..") for part in path.split("/"))
            and "\\" not in path and "\x00" not in path, "Unsafe bound source path")
    require(digest_shape(binding.get("original-sha256"), 64)
            and digest_shape(binding.get("mutated-sha256"), 64)
            and binding["original-sha256"] != binding["mutated-sha256"], "Invalid bound source hashes")


def canonical_source_bindings(sha):
    """Ask the committed canonical runner to reconstruct identities from source S.

    Python transports and matches this inventory; mutation discovery, fingerprint
    semantics and source rewriting remain in knoxx.mutation-test.
    """
    backend = Path(__file__).resolve().parent.parent / "backend"
    try:
        result = subprocess.run(["clojure", "-Srepro", "-M:mutation", "--source-bindings", sha],
                                cwd=backend, check=True, capture_output=True, text=True, timeout=600)
        manifest = json.loads(result.stdout)
    except (OSError, subprocess.SubprocessError, json.JSONDecodeError) as error:
        raise ValueError("Canonical immutable source binding regeneration failed") from error
    require(isinstance(manifest, dict) and set(manifest) == {"schema-version", "source-sha", "candidates"}
            and type(manifest["schema-version"]) is int and manifest["schema-version"] == 1
            and manifest["source-sha"] == sha and isinstance(manifest["candidates"], list),
            "Invalid canonical source binding manifest")
    bindings = {}
    for ordinal, candidate in enumerate(manifest["candidates"]):
        require(isinstance(candidate, dict) and set(candidate) == {"ordinal", "source-binding"}
                and type(candidate["ordinal"]) is int and candidate["ordinal"] == ordinal,
                "Invalid canonical source candidate ordinal")
        binding = candidate["source-binding"]
        fingerprint = binding.get("fingerprint") if isinstance(binding, dict) else None
        source_binding(binding, fingerprint)
        require(fingerprint is not None and fingerprint not in bindings,
                "Duplicate canonical source mutation identity")
        bindings[fingerprint] = candidate
    return bindings


def manifest_digest(files):
    rows = "".join(f'{entry["path"]}\t{entry["kind"]}\t{entry["bytes"]}\t{entry["sha256"]}\n'
                   for entry in files)
    return hashlib.sha256(rows.encode("utf-8")).hexdigest()


def payload_evidence(payload, sha, binding, store, referenced, verified=None):
    """Recompute the fixed Shadow test-ci payload identity from preserved bytes."""
    verified = {} if verified is None else verified
    require(isinstance(payload, dict) and type(payload.get("schema-version")) is int
            and payload["schema-version"] == 1
            and payload.get("source-sha") == sha and payload.get("source-binding") == binding,
            "Missing or stale compiled payload source binding")
    require(payload.get("loader-path") == LOADER and payload.get("runtime-root") == RUNTIME,
            "Unsupported compiled payload layout")
    imports = payload.get("imports")
    require(isinstance(imports, list) and imports and all(isinstance(name, str)
            and re.fullmatch(JS_NAME, name) for name in imports)
            and len(imports) == len(set(imports)) and "goog.base.js" not in imports,
            "Invalid compiled import inventory")
    expected = {LOADER: "loader", RUNTIME + "/goog.base.js": "runtime-js"}
    expected.update({RUNTIME + "/" + name: "runtime-js" for name in imports})
    files = payload.get("files")
    require(isinstance(files, list) and all(isinstance(entry, dict) for entry in files),
            "Missing compiled file inventory")
    paths = [entry.get("path") for entry in files]
    require(all(isinstance(path, str) for path in paths) and set(expected).issubset(paths),
            "Missing required compiled files")
    for path in paths:
        if path not in expected:
            require(path.startswith(RUNTIME + "/") and path.endswith(".js.map")
                    and expected.get(path[:-4]) == "runtime-js", "Unexpected compiled debug map")
            expected[path] = "source-map"
    require([entry.get("path") for entry in files] == sorted(expected),
            "Missing, additional, duplicate or unordered compiled files")
    loader = None
    loader_hash = None
    for entry in files:
        require(set(entry) == {"path", "kind", "bytes", "sha256"}
                and entry["kind"] == expected[entry["path"]]
                and type(entry["bytes"]) is int and entry["bytes"] > 0
                and digest_shape(entry["sha256"], 64), "Malformed compiled file identity")
        path = physical_path(store / entry["sha256"])
        if entry["sha256"] not in verified or entry["kind"] == "loader":
            blob = path.read_bytes()
            require(len(blob) == entry["bytes"] and hashlib.sha256(blob).hexdigest() == entry["sha256"],
                    "Compiled payload bytes mismatch")
            verified[entry["sha256"]] = len(blob)
        require(verified[entry["sha256"]] == entry["bytes"], "Compiled payload bytes mismatch")
        referenced.add(entry["sha256"])
        if entry["kind"] == "loader":
            try:
                loader = blob.decode("utf-8")
            except UnicodeDecodeError as error:
                raise ValueError("Invalid compiled loader encoding") from error
            loader_hash = entry["sha256"]
    root_line = "var SHADOW_IMPORT_PATH = __dirname + '/../../" + RUNTIME + "';"
    lines = loader.splitlines()
    require([line for line in lines if line.startswith("var SHADOW_IMPORT_PATH")] == [root_line],
            "Invalid compiled loader relative runtime root")
    import_lines = [line for line in lines if line.lstrip().startswith("SHADOW_IMPORT(")]
    matches = [re.fullmatch(r'SHADOW_IMPORT\("(' + JS_NAME + r')"\);', line) for line in import_lines]
    require(all(matches) and [match.group(1) for match in matches] == imports,
            "Compiled loader imports differ from preserved inventory")
    require(manifest_digest(files) == payload.get("sha256")
            and manifest_digest([entry for entry in files if entry["kind"] != "source-map"])
            == payload.get("code-sha256"), "Compiled payload aggregate identity mismatch")
    return loader_hash


def compile_evidence(result, sha, binding, store, referenced, verified=None):
    require(digest_shape(sha, 40), "Immutable compile source required")
    source_binding(binding, binding.get("fingerprint") if isinstance(binding, dict)
                   and binding.get("kind") == "mutant" else None)
    output = completed_evidence(result, 0)
    require(bool(re.search(r"\[:test-ci\] Build completed\. \([0-9]+ files, [0-9]+ compiled, 0 warnings,", output))
            and not re.search(r"\b[1-9][0-9]* warnings", output), "Missing clean test-ci compilation")
    require(result.get("source-verified-before?") is True and result.get("source-verified-after?") is True,
            "Compile source custody was not preserved")
    payload = result.get("payload")
    loader_hash = payload_evidence(payload, sha, binding, store, referenced, verified)
    require(result.get("artifact-path") == LOADER and result.get("artifact-sha256") == loader_hash,
            "Compiled entrypoint identity mismatch")
    return payload


def test_evidence(result, killed, payload):
    output = completed_evidence(result, 1 if killed else 0)
    require(result.get("source-sha") == payload["source-sha"]
            and result.get("source-binding") == payload["source-binding"]
            and result.get("input-payload-sha256") == payload["sha256"],
            "Test input differs from compiled source or payload")
    require(all(result.get(key) is True for key in ["payload-verified-before?", "payload-verified-after?",
                                                   "source-verified-before?", "source-verified-after?"]),
            "Test source or payload changed during execution")
    totals = re.findall(r"Ran (\d+) tests? containing (\d+) assertions?\.", output)
    outcomes = re.findall(r"\b(\d+) failures?,\s*(\d+) errors?\.", output)
    require(bool(totals) and len(totals) == len(outcomes), "Missing or unmatched completed summaries")
    require(all(int(tests) > 0 and int(assertions) > 0 for tests, assertions in totals), "Empty test execution")
    observed = {"tests": sum(int(tests) for tests, _ in totals),
                "assertions": sum(int(assertions) for _, assertions in totals),
                "failures": sum(int(failures) for failures, _ in outcomes),
                "errors": sum(int(errors) for _, errors in outcomes), "summaries": len(totals)}
    require(all(type(result.get(key)) is int and result[key] == value for key, value in observed.items()),
            "Counters do not match captured completed summaries")
    red = observed["failures"] > 0 or observed["errors"] > 0
    require(red == killed, "Kill requires completed red assertions; baseline requires green")


def verify(sha, directory):
    require(bool(re.fullmatch(r"[0-9a-f]{40}", sha)), "Immutable commit required")
    files = list(Path(directory).glob("*.json"))
    require(len(files) == 4, "Exactly four completed batches required")
    canonical = canonical_source_bindings(sha)
    store = physical_path(Path(directory) / "payload-store", directory=True)
    referenced = set()
    verified = {}  # Explicit invocation-local cache, never reused across stores or runs.
    seen, indexes, excluded = set(), set(), set()
    for path in files:
        batch = json.loads(path.read_text())
        require(batch.get("schema_version") == 3 and batch.get("source_sha") == sha
                and batch.get("status") == "passed" and batch.get("baseline") == "passed",
                "Invalid, legacy or stale batch")
        baseline = batch.get("baseline_evidence")
        require(isinstance(baseline, dict) and baseline.get("status") == "passed", "Missing green baseline evidence")
        baseline_binding = {"kind": "baseline"}
        baseline_payload = compile_evidence(baseline.get("compile"), sha, baseline_binding, store, referenced, verified)
        test_evidence(baseline.get("test"), killed=False, payload=baseline_payload)
        index = batch["batch_index"]
        require(type(index) is int and index in range(4) and index not in indexes, "Invalid batch index")
        indexes.add(index)
        require(isinstance(batch.get("mutants"), list) and len(batch["mutants"]) >= 250,
                "Insufficient mutations in batch")
        for entry in batch.get("excluded", []):
            key = entry.get("fingerprint")
            require(isinstance(key, str) and bool(re.fullmatch(r"[0-9a-f]{64}", key))
                    and key not in excluded, "Duplicate or invalid equivalent exclusion")
            require(bool(re.fullmatch(r"[0-9a-f]{64}", entry.get("source-sha256", "")))
                    and bool(re.fullmatch(r"[0-9a-f]{40}", entry.get("basis-source-sha", "")))
                    and bool(re.fullmatch(r"[0-9a-f]{64}", entry.get("manifest-sha256", "")))
                    and isinstance(entry.get("manifest-path"), str) and entry["manifest-path"]
                    and isinstance(entry.get("relative-path"), str) and entry["relative-path"]
                    and isinstance(entry.get("rationale"), str) and entry["rationale"].strip()
                    and bool(re.fullmatch(r"https://github\.com/open-hax/knoxx/pull/[1-9][0-9]*(?:#[A-Za-z0-9_-]+)?",
                                          entry.get("review-reference", ""))),
                    "Equivalent exclusion lacks explicit source/review provenance")
            excluded.add(key)
        for mutant in batch["mutants"]:
            key = mutant["fingerprint"]
            require(isinstance(key, str) and bool(re.fullmatch(r"[0-9a-f]{64}", key)) and key not in seen,
                    "Duplicate or invalid mutation")
            require(mutant["status"] == "killed" and mutant["phase"] == "test", "Mutation was not test-killed")
            binding = mutant.get("source-binding")
            source_binding(binding, key)
            candidate = canonical.get(key)
            require(candidate is not None and candidate["source-binding"] == binding,
                    "Mutation binding does not match canonical checked-out source")
            require(candidate["ordinal"] % 4 == index, "Mutation belongs to a different canonical batch")
            payload = compile_evidence(mutant.get("compile"), sha, binding, store, referenced, verified)
            require(payload["code-sha256"] != baseline_payload["code-sha256"],
                    "Unchanged executed code cannot qualify as a kill")
            test_evidence(mutant.get("result"), killed=True, payload=payload)
            seen.add(key)
    require(not (seen & excluded), "Equivalent exclusion cannot receive kill credit")
    stored = list(store.iterdir())
    require(all(digest_shape(path.name, 64) and physical_path(path) for path in stored)
            and {path.name for path in stored} == referenced, "Payload store has unexpected or missing blobs")
    # Rehash each unique blob once at the end: cached bytes may have changed
    # in-place without changing size, inode or a restored modification time.
    for path in stored:
        blob = physical_path(path).read_bytes()
        require(len(blob) == verified[path.name] and hashlib.sha256(blob).hexdigest() == path.name,
                "Verified payload blob changed before final custody check")
    require(canonical_source_bindings(sha) == canonical,
            "Canonical source bindings changed during evidence verification")
    return len(seen)


if __name__ == "__main__":
    source_sha, evidence_directory = sys.argv[1:]
    count = verify(source_sha, evidence_directory)
    print(f"Qualified {source_sha}: {count} distinct completed test kills across four batches")
