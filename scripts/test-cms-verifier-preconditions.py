#!/usr/bin/env python3
"""CMS verifiers must fail before testing an unidentified checkout."""
from contextlib import contextmanager
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class CmsVerifierPreconditionsTest(unittest.TestCase):
    @contextmanager
    def verifier_fixture(self, script_name, git_body='printf "%s\\n" 0123456789abcdef0123456789abcdef01234567\n',
                         checkout=None, symlink_entry=False, git_root_body=None):
        with tempfile.TemporaryDirectory(prefix="knoxx-cms-preconditions-") as temporary:
            temporary_root = Path(temporary)
            fixture = temporary_root / "source checkout"
            if checkout:
                real_git = shutil.which("git")
                self.assertIsNotNone(real_git, "Git is required for actual checkout fixtures")

                def git(*arguments, directory):
                    return subprocess.run(
                        [real_git, "-C", str(directory), "-c", "core.hooksPath=/dev/null",
                         "-c", "commit.gpgsign=false", "-c", "user.name=CMS fixture",
                         "-c", "user.email=cms-fixture@example.invalid", *arguments],
                        check=True, capture_output=True, text=True, timeout=10,
                    )

                repository = fixture if checkout == "own" else temporary_root / "enclosing checkout"
                repository.mkdir()
                git("init", "--quiet", directory=repository)
                git("commit", "--quiet", "--allow-empty", "-m", "Isolated verifier fixture",
                    directory=repository)
                if checkout == "enclosing":
                    fixture = repository / "copied source"
                elif checkout == "worktree":
                    git("worktree", "add", "--quiet", "--detach", str(fixture), "HEAD",
                        directory=repository)
                    self.assertTrue((fixture / ".git").is_file(), "linked-worktree control was not created")
                else:
                    self.assertEqual(checkout, "own")
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
                "git": 'case "$*" in\n*--show-toplevel)\n' + (
                    git_root_body if git_root_body is not None else
                    'printf "%s\\n" "$KNOXX_VERIFIER_GIT_ROOT"\n') + ';;\n*)\n' + git_body + ';;\nesac\n',
                "mktemp": 'printf "fixture allocation called\\n" > "$KNOXX_VERIFIER_MKTEMP_MARKER"\nexec "$KNOXX_VERIFIER_REAL_MKTEMP" "$@"\n',
                "node": 'printf "helper called\\n" > "$KNOXX_VERIFIER_NODE_MARKER"\nexit 7\n',
                "pnpm": 'printf "pnpm called\\n" > "$KNOXX_VERIFIER_PNPM_MARKER"\nexit 99\n',
                "clojure": "exit 0\n",
            }
            if checkout:
                del stubs["git"]
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
                "KNOXX_VERIFIER_GIT_ROOT": str(fixture),
                "KNOXX_VERIFIER_MKTEMP_MARKER": str(fixture / "mktemp-started"),
                "KNOXX_VERIFIER_REAL_MKTEMP": shutil.which("mktemp"),
                "TMPDIR": str(fixture),
            })
            if symlink_entry:
                alias = temporary_root / "linked source path"
                alias.symlink_to(fixture, target_is_directory=True)
                script = alias / "scripts" / script_name
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
                        self.assertFalse((node_marker.parent / "mktemp-started").exists(),
                                         "fixture allocated without a verified checkout:\n" + detail)
                        self.assertNotIn("Verifying checkout", result.stdout)
                        self.assertNotIn("PASS ", result.stdout)

    def test_cms_history_preserves_helper_failure(self):
        self.assert_helper_failure_stops_verifier("verify-cms-history.sh")

    def test_cms_source_locale_review_preserves_helper_failure(self):
        self.assert_helper_failure_stops_verifier("verify-cms-source-locale-review.sh")

    def test_enclosing_checkout_cannot_supply_an_unrelated_commit(self):
        for script_name in ("verify-cms-history.sh", "verify-cms-source-locale-review.sh"):
            with self.subTest(script=script_name):
                with self.verifier_fixture(script_name, checkout="enclosing") as (result, node_marker, pnpm_marker):
                    detail = result.stdout + result.stderr
                    self.assertNotEqual(0, result.returncode, detail)
                    self.assertFalse(node_marker.exists(), "helper started under an enclosing identity:\n" + detail)
                    self.assertFalse(pnpm_marker.exists(), "compiler started under an enclosing identity:\n" + detail)
                    self.assertFalse((node_marker.parent / "mktemp-started").exists(),
                                     "fixture allocated under an enclosing identity:\n" + detail)
                    self.assertNotIn("Verifying checkout", result.stdout)
                    self.assertNotIn("PASS ", result.stdout)

    def test_own_checkouts_and_linked_worktrees_admit_physical_source_paths(self):
        for script_name in ("verify-cms-history.sh", "verify-cms-source-locale-review.sh"):
            for checkout in ("own", "worktree"):
                for symlink_entry in (False, True):
                    with self.subTest(script=script_name, checkout=checkout, symlink=symlink_entry):
                        with self.verifier_fixture(script_name, checkout=checkout, symlink_entry=symlink_entry) as (result, node_marker, pnpm_marker):
                            detail = result.stdout + result.stderr
                            self.assertEqual(7, result.returncode, detail)
                            self.assertTrue(node_marker.is_file(), "own checkout did not reach the helper sentinel:\n" + detail)
                            self.assertFalse(pnpm_marker.exists(), "compiler started after helper failure:\n" + detail)
                            self.assertIn("Verifying checkout " + str(node_marker.parent.resolve()) + " at ", result.stdout)
                            self.assertRegex(result.stdout, r" at [0-9a-f]{40}\n")
                            self.assertNotIn("PASS ", result.stdout)

    def test_unresolved_git_root_stops_before_fixture_helper_or_compile(self):
        for script_name in ("verify-cms-history.sh", "verify-cms-source-locale-review.sh"):
            for root_body in ('printf "git root unavailable\\n" >&2\nexit 23\n', "exit 0\n"):
                with self.subTest(script=script_name, root=root_body):
                    with self.verifier_fixture(script_name, git_root_body=root_body) as (result, node_marker, pnpm_marker):
                        detail = result.stdout + result.stderr
                        self.assertNotEqual(0, result.returncode, detail)
                        self.assertFalse((node_marker.parent / "mktemp-started").exists(), detail)
                        self.assertFalse(node_marker.exists(), detail)
                        self.assertFalse(pnpm_marker.exists(), detail)
                        self.assertNotIn("Verifying checkout", result.stdout)
                        self.assertNotIn("PASS ", result.stdout)


if __name__ == "__main__":
    unittest.main()
