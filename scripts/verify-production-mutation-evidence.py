#!/usr/bin/env python3
"""Require four disjoint batches of completed assertion kills on one source."""
import hashlib
import json
from pathlib import Path
import re
import sys


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


def compile_evidence(result):
    output = completed_evidence(result, 0)
    require(bool(re.search(r"\[:test-ci\] Build completed\. \([0-9]+ files, [0-9]+ compiled, 0 warnings,", output))
            and not re.search(r"\b[1-9][0-9]* warnings", output), "Missing clean test-ci compilation")
    require(bool(re.fullmatch(r"[0-9a-f]{64}", result.get("artifact-sha256", ""))),
            "Missing compiled test entrypoint identity")


def test_evidence(result, killed):
    output = completed_evidence(result, 1 if killed else 0)
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
    seen, indexes, excluded = set(), set(), set()
    for path in files:
        batch = json.loads(path.read_text())
        require(batch.get("schema_version") == 2 and batch.get("source_sha") == sha
                and batch.get("status") == "passed" and batch.get("baseline") == "passed",
                "Invalid, legacy or stale batch")
        baseline = batch.get("baseline_evidence")
        require(isinstance(baseline, dict) and baseline.get("status") == "passed", "Missing green baseline evidence")
        compile_evidence(baseline.get("compile"))
        test_evidence(baseline.get("test"), killed=False)
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
            compile_evidence(mutant.get("compile"))
            test_evidence(mutant.get("result"), killed=True)
            seen.add(key)
    require(not (seen & excluded), "Equivalent exclusion cannot receive kill credit")
    return len(seen)


if __name__ == "__main__":
    source_sha, evidence_directory = sys.argv[1:]
    count = verify(source_sha, evidence_directory)
    print(f"Qualified {source_sha}: {count} distinct completed test kills across four batches")
