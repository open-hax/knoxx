#!/usr/bin/env python3
"""Require four disjoint batches of completed assertion kills on one source."""
import hashlib
import json
import os
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


def canonical_source_bindings(sha, backend=None):
    """Ask the committed canonical runner to reconstruct identities from source S.

    Python transports and matches this inventory; mutation discovery, fingerprint
    semantics and source rewriting remain in knoxx.mutation-test.
    """
    backend = Path(__file__).resolve().parent.parent / "backend" if backend is None else backend
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


def validate_structural_evidence(sha, directory, *, _backend=None, _reports=None):
    """Validate data/custody only; this API grants no production qualification."""
    require(bool(re.fullmatch(r"[0-9a-f]{40}", sha)), "Immutable commit required")
    files = list(Path(directory).absolute().glob("*.json"))
    require(len(files) == 4, "Exactly four completed batches required")
    canonical = canonical_source_bindings(sha, _backend)
    store = physical_path(Path(directory) / "payload-store", directory=True)
    referenced = set()
    verified = {}  # Explicit invocation-local cache, never reused across stores or runs.
    seen, indexes, excluded = set(), set(), set()
    selections = []
    for path in files:
        batch = json.loads(path.read_bytes() if _reports is None else _reports[path])
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
        batch_excluded = set()
        for entry in batch.get("excluded", []):
            key = entry.get("fingerprint")
            require(isinstance(key, str) and bool(re.fullmatch(r"[0-9a-f]{64}", key))
                    and key not in excluded, "Duplicate or invalid equivalent exclusion")
            candidate = canonical.get(key)
            require(candidate is not None and candidate["ordinal"] % 4 == index,
                    "Equivalent exclusion is not a canonical candidate of this batch")
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
            batch_excluded.add(key)
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
        partition = [key for key, candidate in sorted(canonical.items(), key=lambda item: item[1]["ordinal"])
                     if candidate["ordinal"] % 4 == index and key not in batch_excluded]
        selections.append(([mutant["fingerprint"] for mutant in batch["mutants"]], partition))
    require(not (seen & excluded), "Equivalent exclusion cannot receive kill credit")
    for claimed, partition in selections:
        require(claimed == partition[:len(claimed)],
                "Batch omits or reorders canonical candidates; survivors cannot be skipped")
    stored = list(store.iterdir())
    require(all(digest_shape(path.name, 64) and physical_path(path) for path in stored)
            and {path.name for path in stored} == referenced, "Payload store has unexpected or missing blobs")
    # Rehash each unique blob once at the end: cached bytes may have changed
    # in-place without changing size, inode or a restored modification time.
    for path in stored:
        blob = physical_path(path).read_bytes()
        require(len(blob) == verified[path.name] and hashlib.sha256(blob).hexdigest() == path.name,
                "Verified payload blob changed before final custody check")
    require(canonical_source_bindings(sha, _backend) == canonical,
            "Canonical source bindings changed during evidence verification")
    return len(seen)


KNOXX = "open-hax/knoxx"
SIGNER_PATH = ".github/workflows/deploy-production.yml"
SERVICES_PATH = ".github/workflows/deploy-stack.yml"
HELPER_PREFIXES = ("backend/mutation/", "backend/scripts/", "backend/test/",
                   ".github/actions/knoxx-setup/")
HELPER_FILES = {"scripts/verify-production-mutation-evidence.py", "backend/deps.edn",
                "backend/shadow-cljs.edn", "shared/deps.edn"}
# Explicit portable compiler input for the reviewed Shadow :macros contract.
# Unknown compiler configuration refuses until its input set/hash is reviewed.
# Ordinary .cljc application sources remain selectable S inputs.
MACRO_FILES = {"backend/src/cljs/knoxx/backend/macros.cljc"}
MACRO_CONFIG_SHA256 = "b182a2993c1e818a36dc9e39c011a8dd830cb097e0b624d95986ea3563165dcd"
CONFIG_NAMES = {"package.json", "pnpm-lock.yaml", "pnpm-workspace.yaml", ".npmrc",
                ".pnpmfile.cjs", "pnpmfile.cjs", ".gitignore", ".gitattributes"}
REPORT_NAMES = [f"batch-{index}.edn.json" for index in range(4)]


def official_json(args, paginated=False):
    """Invoke the real official transport; diagnostics never include token bytes."""
    try:
        result = subprocess.run(["gh", *args], check=True, capture_output=True,
                                text=True, timeout=120)
        require(len(result.stdout.encode("utf-8")) <= 16 * 1024 * 1024,
                "Official response exceeds bounded capture")
        value = json.loads(result.stdout)
    except (OSError, subprocess.SubprocessError, UnicodeError, json.JSONDecodeError) as error:
        raise ValueError("Official native or cryptographic verification failed") from error
    require(isinstance(value, list if paginated else dict), "Invalid official response shape")
    return value


def native(path):
    """Read one authenticated native JSON object through the official CLI."""
    return official_json(["api", "--method", "GET", "-H", "Accept: application/vnd.github+json",
                          "-H", "X-GitHub-Api-Version: 2022-11-28", path])


def native_pages(path, key):
    """Require complete count-checked native pagination and unique identities."""
    pages = official_json(["api", "--method", "GET", "--paginate", "--slurp",
                           "-H", "Accept: application/vnd.github+json",
                           "-H", "X-GitHub-Api-Version: 2022-11-28", path], paginated=True)
    require(pages and all(isinstance(page, dict) and isinstance(page.get(key), list)
                         and type(page.get("total_count")) is int for page in pages),
            "Incomplete native pagination")
    entries = [entry for page in pages for entry in page[key]]
    require(all(page["total_count"] == len(entries) for page in pages),
            "Native pagination count mismatch")
    require(all(isinstance(entry, dict) and type(entry.get("id")) is int and entry["id"] > 0
                for entry in entries) and len({entry["id"] for entry in entries}) == len(entries),
            "Duplicate or malformed native identity")
    return entries


def protected_main(repository):
    """Protected current-main identity comes from fresh native facts, never a report."""
    repo = native(f"repos/{repository}")
    branch = native(f"repos/{repository}/branches/main")
    require(repo.get("full_name") == repository and type(repo.get("id")) is int
            and repo["id"] > 0 and repo.get("default_branch") == "main"
            and repo.get("fork") is False and repo.get("archived") is False
            and branch.get("name") == "main" and branch.get("protected") is True,
            "Trusted protected main is unavailable")
    sha = branch.get("commit", {}).get("sha")
    require(digest_shape(sha, 40), "Invalid protected main commit")
    return repo["id"], sha


def trusted_execution_context():
    """Admit only current protected Knoxx dispatch or the protected Services route.

    Native called-workflow SHA establishes reusable C; github.workflow_sha is not
    substituted for it. The official certificate will independently bind this
    context. These fresh reads do not accept a supplied authorization flag.
    """
    env = os.environ
    require(env.get("GITHUB_ACTIONS") == "true"
            and env.get("GITHUB_SERVER_URL") == "https://github.com"
            and env.get("GITHUB_API_URL") == "https://api.github.com"
            and env.get("GITHUB_REF") == "refs/heads/main"
            and isinstance(env.get("GH_TOKEN"), str) and bool(env["GH_TOKEN"].strip()),
            "Mandatory authenticated production workflow context is missing")
    caller = env.get("GITHUB_REPOSITORY")
    require(caller in (KNOXX, "open-hax/services"), "Unsupported production caller")
    run_id, attempt = env.get("GITHUB_RUN_ID", ""), env.get("GITHUB_RUN_ATTEMPT", "")
    require(bool(re.fullmatch(r"[1-9][0-9]*", run_id))
            and bool(re.fullmatch(r"[1-9][0-9]*", attempt)), "Invalid native run coordinates")
    caller_id, caller_sha = protected_main(caller)
    _, controller_sha = protected_main(KNOXX)
    require(env.get("GITHUB_SHA") == caller_sha, "Caller is no longer current protected main")
    run = native(f"repos/{caller}/actions/runs/{run_id}/attempts/{attempt}")
    caller_path = SIGNER_PATH if caller == KNOXX else SERVICES_PATH
    event = env.get("GITHUB_EVENT_NAME")
    require(event in (("workflow_dispatch",) if caller == KNOXX
                     else ("workflow_dispatch", "pull_request_target")), "Unsupported production event")
    workflow_ref = f"{caller}/{caller_path}@refs/heads/main"
    require(env.get("GITHUB_WORKFLOW_REF") == workflow_ref
            and type(run.get("id")) is int and run["id"] == int(run_id)
            and type(run.get("run_attempt")) is int and run["run_attempt"] == int(attempt)
            and run.get("head_sha") == caller_sha and run.get("head_branch") == "main"
            and run.get("event") == event and run.get("repository", {}).get("id") == caller_id
            and run.get("repository", {}).get("full_name") == caller
            and isinstance(run.get("path"), str) and run["path"].split("@", 1)[0] == caller_path,
            "Native caller workflow binding mismatch")
    if caller == KNOXX:
        require(controller_sha == caller_sha, "Direct controller revision mismatch")
    else:
        refs = run.get("referenced_workflows")
        require(isinstance(refs, list), "Missing native reusable-workflow references")
        matches = [ref for ref in refs if isinstance(ref, dict)
                   and isinstance(ref.get("path"), str)
                   and ref["path"].split("@", 1)[0] == f"{KNOXX}/{SIGNER_PATH}"]
        require(len(matches) == 1 and matches[0].get("sha") == controller_sha,
                "Called controller is not current protected Knoxx main")
        require(matches[0]["path"] in (f"{KNOXX}/{SIGNER_PATH}@{controller_sha}",
                                      f"{KNOXX}/{SIGNER_PATH}@main")
                and matches[0].get("ref", "refs/heads/main") == "refs/heads/main",
                "Unsupported native reusable-workflow reference")
    return {"caller": caller, "caller_id": caller_id, "caller_sha": caller_sha,
            "caller_path": caller_path, "controller_sha": controller_sha,
            "run_id": int(run_id), "attempt": int(attempt), "ref": "refs/heads/main",
            "invocation": f"https://github.com/{caller}/actions/runs/{run_id}/attempts/{attempt}"}


def git_bytes(root, *args):
    """Read committed Git objects with inherited Git overrides and hooks disabled."""
    env = {key: value for key, value in os.environ.items() if not key.startswith("GIT_")}
    env.update(GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull, GIT_TERMINAL_PROMPT="0")
    try:
        return subprocess.run(["git", "-c", "core.hooksPath=/dev/null", "-c", "core.fsmonitor=false",
                               *args], cwd=root, env=env, check=True,
                              capture_output=True, timeout=120).stdout
    except (OSError, subprocess.SubprocessError) as error:
        raise ValueError("Trusted executor committed source read failed") from error


def executor_entries(root, commit):
    """Exact committed transitive executor/config inventory, not mutation identity law.

    Bind the explicit configured portable macro input and inherited JVM/helper
    inputs. Ordinary .cljc application code may differ between selected S and C.
    """
    entries = {}
    for raw in git_bytes(root, "ls-tree", "-r", "-z", commit).split(b"\0"):
        if not raw:
            continue
        metadata, path_raw = raw.split(b"\t", 1)
        path = path_raw.decode("utf-8")
        if (path in HELPER_FILES or path.startswith(HELPER_PREFIXES)
                or Path(path).name in CONFIG_NAMES
                or path in MACRO_FILES
                or (path.startswith(("backend/", "shared/")) and path.endswith(".clj"))):
            mode, kind, blob = metadata.decode("ascii").split()
            require(mode in ("100644", "100755") and kind == "blob", "Unsafe executor input mode")
            entries[path] = (mode, blob)
    require((HELPER_FILES | MACRO_FILES).issubset(entries)
            and all(any(path.startswith(prefix) for path in entries) for prefix in HELPER_PREFIXES),
            "Incomplete committed executor inventory")
    require(hashlib.sha256(git_bytes(root, "show", f"{commit}:backend/shadow-cljs.edn")).hexdigest()
            == MACRO_CONFIG_SHA256, "Unsupported portable macro configuration")
    return entries


def guard_source_executor(sha, context, application=None):
    """C owns verification; S may differ only when its execution inputs equal C."""
    require(digest_shape(sha, 40), "Immutable selected source required")
    root = physical_path(Path(os.environ.get("GITHUB_WORKSPACE", "")), directory=True)
    require(Path(__file__).absolute() == root / "scripts/verify-production-mutation-evidence.py"
            and git_bytes(root, "rev-parse", "HEAD").strip().decode("ascii") == context["controller_sha"],
            "Verifier is not in the authenticated controller checkout")
    git_bytes(root, "merge-base", "--is-ancestor", sha, context["controller_sha"])
    expected = executor_entries(root, context["controller_sha"])
    require(executor_entries(root, sha) == expected, "Selected source executor differs from trusted controller")
    for relative in expected:
        require(physical_path(root / relative).read_bytes()
                == git_bytes(root, "show", f'{context["controller_sha"]}:{relative}'),
                "Controller executor input changed after checkout")
    if application is not None:
        app = physical_path(application, directory=True)
        require(app == root / "application"
                and git_bytes(app, "rev-parse", "HEAD").strip().decode("ascii") == sha,
                "Selected application checkout is not exact S")
        for relative in expected:
            require(physical_path(app / relative).read_bytes()
                    == git_bytes(root, "show", f'{sha}:{relative}'),
                    "Selected application executor input changed after checkout")
    return root


def producer_artifacts(sha, context):
    """Select fixed current-attempt artifacts only after real producer jobs succeed."""
    prefix = f'production-mutation-{context["run_id"]}-{context["attempt"]}-{sha}-'
    jobs = native_pages(f'repos/{context["caller"]}/actions/runs/{context["run_id"]}'
                        f'/attempts/{context["attempt"]}/jobs?per_page=100', "jobs")
    for name in ["Knoxx integration-e2e", *[f"Knoxx mutation batch {index}" for index in range(4)]]:
        matches = [job for job in jobs if job.get("name", "").split(" / ")[-1] == name]
        require(len(matches) == 1 and matches[0].get("status") == "completed"
                and matches[0].get("conclusion") == "success",
                "Required native producer did not complete successfully")
    artifacts = native_pages(f'repos/{context["caller"]}/actions/runs/{context["run_id"]}'
                             '/artifacts?per_page=100', "artifacts")
    selected = [artifact for artifact in artifacts if artifact.get("name", "").startswith(prefix)]
    expected = [prefix + str(index) for index in range(4)]
    require(len(selected) == 4 and sorted(artifact["name"] for artifact in selected) == expected,
            "Missing, additional or ambiguous current-attempt producer artifacts")
    for artifact in selected:
        run = artifact.get("workflow_run", {})
        require(artifact.get("expired") is False and type(artifact.get("size_in_bytes")) is int
                and artifact["size_in_bytes"] > 0
                and bool(re.fullmatch(r"sha256:[0-9a-f]{64}", artifact.get("digest", "")))
                and run.get("id") == context["run_id"] and run.get("repository_id") == context["caller_id"]
                and run.get("head_repository_id") == context["caller_id"]
                and run.get("head_sha") == context["caller_sha"] and run.get("head_branch") == "main",
                "Producer artifact native source custody mismatch")
    return sorted(selected, key=lambda item: item["name"])


def report_bytes(sha, directory):
    """Freeze exact report bytes; only these bytes enter the later structural reader."""
    root = physical_path(Path(directory), directory=True)
    paths = sorted(root.glob("*.json"))
    require([path.name for path in paths] == REPORT_NAMES, "Exactly four fixed producer reports required")
    reports = {}
    for index, path in enumerate(paths):
        raw = physical_path(path).read_bytes()
        value = json.loads(raw)
        require(isinstance(value, dict) and value.get("source_sha") == sha
                and type(value.get("batch_index")) is int and value["batch_index"] == index,
                "Signed report selected source or batch mismatch")
        reports[path] = raw
    return reports


def authenticated_report(path, raw, context):
    """Official cryptography first, then certificate-only C/G/run identity checks."""
    results = official_json(["attestation", "verify", str(path), "--repo", context["caller"],
                             "--signer-workflow", f"{KNOXX}/{SIGNER_PATH}",
                             "--signer-digest", context["controller_sha"],
                             "--source-digest", context["caller_sha"], "--source-ref", context["ref"],
                             "--deny-self-hosted-runners", "--format", "json"], paginated=True)
    digest = hashlib.sha256(raw).hexdigest()
    for result in results:
        if not isinstance(result, dict):
            continue
        verified = result.get("verificationResult", {})
        certificate = verified.get("signature", {}).get("certificate", {})
        statement = verified.get("statement", {})
        signer_uri = certificate.get("buildSignerURI")
        if (certificate.get("issuer") == "https://token.actions.githubusercontent.com"
                and signer_uri in (f"https://github.com/{KNOXX}/{SIGNER_PATH}@refs/heads/main",
                                   f'https://github.com/{KNOXX}/{SIGNER_PATH}@{context["controller_sha"]}')
                and certificate.get("buildSignerDigest") == context["controller_sha"]
                and certificate.get("sourceRepositoryURI") == f'https://github.com/{context["caller"]}'
                and certificate.get("sourceRepositoryIdentifier") == str(context["caller_id"])
                and certificate.get("sourceRepositoryDigest") == context["caller_sha"]
                and certificate.get("sourceRepositoryRef") == context["ref"]
                and certificate.get("buildConfigURI")
                == f'https://github.com/{context["caller"]}/{context["caller_path"]}@{context["ref"]}'
                and certificate.get("buildConfigDigest") == context["caller_sha"]
                and certificate.get("runnerEnvironment") == "github-hosted"
                and certificate.get("runInvocationURI") == context["invocation"]
                and isinstance(verified.get("verifiedTimestamps"), list) and verified["verifiedTimestamps"]
                and statement.get("predicateType") == "https://slsa.dev/provenance/v1"
                and any(isinstance(subject, dict) and subject.get("digest", {}).get("sha256") == digest
                        for subject in statement.get("subject", []))):
            require(physical_path(path).read_bytes() == raw, "Authenticated report bytes changed")
            return
    raise ValueError("No authenticated current-controller/current-caller/current-attempt report")


def verify(sha, directory):
    """Production qualification requires real native context and official authentication."""
    context = trusted_execution_context()
    application = Path(os.environ["GITHUB_WORKSPACE"]) / "application"
    guard_source_executor(sha, context, application)
    artifacts = producer_artifacts(sha, context)
    reports = report_bytes(sha, directory)
    for path, raw in reports.items():
        authenticated_report(path, raw, context)
    count = validate_structural_evidence(sha, directory, _backend=application / "backend", _reports=reports)
    require(all(physical_path(path).read_bytes() == raw for path, raw in reports.items()),
            "Authenticated reports changed during structural verification")
    guard_source_executor(sha, context, application)
    require(trusted_execution_context() == context and producer_artifacts(sha, context) == artifacts,
            "Native execution context or producer custody changed during verification")
    return count


def executor_main(args):
    """Small workflow adapter surface; neither preparer grants mutation credit."""
    if len(args) == 2 and args[0] == "--prepare-source":
        sha = args[1]
        context = trusted_execution_context()
        guard_source_executor(sha, context)
        print(json.dumps({"source_sha": sha, "controller_sha": context["controller_sha"]}))
    elif len(args) == 2 and args[0] == "--producer-artifact-ids":
        context = trusted_execution_context()
        guard_source_executor(args[1], context)
        print(",".join(str(item["id"]) for item in producer_artifacts(args[1], context)))
    elif len(args) == 3 and args[0] == "--attest-subjects":
        sha, directory = args[1:]
        context = trusted_execution_context()
        guard_source_executor(sha, context)
        producer_artifacts(sha, context)
        reports = report_bytes(sha, directory)
        print("\n".join(str(path) for path in reports))
    else:
        require(len(args) == 2, "Expected immutable source and evidence directory")
        sha, directory = args
        count = verify(sha, directory)
        print(f"Qualified {sha}: {count} distinct completed test kills across four batches")


if __name__ == "__main__":
    executor_main(sys.argv[1:])
