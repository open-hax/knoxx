#!/usr/bin/env python3
"""CMS verifiers must fail before testing an unidentified checkout."""
from contextlib import contextmanager
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class CmsVerifierPreconditionsTest(unittest.TestCase):
    @contextmanager
    def verifier_fixture(self, script_name, git_body='printf "%s\\n" 0123456789abcdef0123456789abcdef01234567\n'):
        with tempfile.TemporaryDirectory(prefix="knoxx-cms-preconditions-") as temporary:
            fixture = Path(temporary)
            for directory in (
                "scripts",
                "bin",
                "backend/node_modules",
                "backend/src/cljs/knoxx/backend/infra",
                "backend/test/cljs/knoxx/backend",
            ):
                (fixture / directory).mkdir(parents=True, exist_ok=True)
            (fixture / "backend/src/cljs/knoxx/backend/infra/cms_store.cljs").touch()
            (fixture / "backend/test/cljs/knoxx/backend/cms_source_locale_review_test.cljs").touch()
            # Execute the checked-in script unchanged, with its checkout and
            # preconditions rooted in an isolated fixture through BASH_SOURCE.
            script = fixture / "scripts" / script_name
            script.symlink_to(ROOT / "scripts" / script_name)
            stubs = {
                "git": git_body,
                "node": 'printf "helper called\\n" > "$KNOXX_VERIFIER_NODE_MARKER"\nexit 7\n',
                "pnpm": 'printf "pnpm called\\n" > "$KNOXX_VERIFIER_PNPM_MARKER"\nexit 99\n',
                "clojure": "exit 0\n",
            }
            for name, body in stubs.items():
                executable = fixture / "bin" / name
                executable.write_text("#!/bin/sh\n" + body)
                executable.chmod(0o755)
            node_marker = fixture / "node-started"
            pnpm_marker = fixture / "pnpm-started"
            environment = os.environ.copy()
            environment.update({
                "PATH": str(fixture / "bin") + os.pathsep + environment.get("PATH", os.defpath),
                "KNOXX_VERIFIER_NODE_MARKER": str(node_marker),
                "KNOXX_VERIFIER_PNPM_MARKER": str(pnpm_marker),
                "TMPDIR": str(fixture),
            })
            result = subprocess.run(
                ["bash", str(script)],
                cwd=fixture,
                env=environment,
                capture_output=True,
                text=True,
                timeout=10,
            )
            yield result, node_marker, pnpm_marker

    def assert_helper_failure_stops_verifier(self, script_name):
        with self.verifier_fixture(script_name) as (result, node_marker, pnpm_marker):
            detail = result.stdout + result.stderr
            self.assertTrue(node_marker.is_file(), "Node helper was not reached:\n" + detail)
            self.assertEqual(7, result.returncode, detail)
            self.assertFalse(pnpm_marker.exists(), "pnpm started after the helper failed:\n" + detail)
            self.assertIn("at 0123456789abcdef0123456789abcdef01234567", result.stdout)
            self.assertNotIn("PASS ", result.stdout)

    def test_unverified_git_identity_stops_both_verifiers_before_helper_or_compile(self):
        for script_name in ("verify-cms-history.sh", "verify-cms-source-locale-review.sh"):
            for git_body in ('printf "git rejected checkout\\n" >&2\nexit 23\n',
                             "exit 0\n", 'printf "not-a-full-commit\\n"\n'):
                with self.subTest(script=script_name, git=git_body):
                    with self.verifier_fixture(script_name, git_body) as (result, node_marker, pnpm_marker):
                        detail = result.stdout + result.stderr
                        self.assertNotEqual(0, result.returncode, detail)
                        self.assertFalse(node_marker.exists(), "helper started without a verified checkout:\n" + detail)
                        self.assertFalse(pnpm_marker.exists(), "compiler started without a verified checkout:\n" + detail)
                        self.assertNotIn("Verifying checkout", result.stdout)
                        self.assertNotIn("PASS ", result.stdout)

    def test_cms_history_preserves_helper_failure(self):
        self.assert_helper_failure_stops_verifier("verify-cms-history.sh")

    def test_cms_source_locale_review_preserves_helper_failure(self):
        self.assert_helper_failure_stops_verifier("verify-cms-source-locale-review.sh")


if __name__ == "__main__":
    unittest.main()
