"""Disposable Linux subprocess controls for the qualification supervisor."""

import ctypes
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch


SUPERVISOR = Path(os.environ.get(
    "MUTATION_PROCESS_SUPERVISOR",
    str(Path(__file__).resolve().parents[2] / "scripts/run-mutation-process.py"),
))


def identity(pid):
    """Read only a PID's kernel identity; a reused PID is not an owned child."""
    try:
        fields = Path(f"/proc/{pid}/stat").read_text().rsplit(")", 1)[1].split()
        return int(fields[19])
    except (FileNotFoundError, ProcessLookupError):
        return None


def own_descendants():
    """Collect the test process's descendants for its outer failure guard."""
    pending = [os.getpid()]
    found = {}
    while pending:
        parent = pending.pop()
        try:
            tasks = list(Path(f"/proc/{parent}/task").iterdir())
        except FileNotFoundError:
            continue
        for task in tasks:
            try:
                children = (task / "children").read_text().split()
            except FileNotFoundError:
                continue
            for value in children:
                pid = int(value)
                start = identity(pid)
                if start is not None and pid not in found:
                    found[pid] = start
                    pending.append(pid)
    return found


def outer_cleanup():
    """Kill/reap only this isolated test process's children, with a deadline."""
    deadline = time.monotonic() + 2
    while time.monotonic() < deadline:
        children = own_descendants()
        for pid, start in children.items():
            try:
                fd = os.pidfd_open(pid)
                try:
                    if identity(pid) == start:
                        signal.pidfd_send_signal(fd, signal.SIGKILL)
                finally:
                    os.close(fd)
            except ProcessLookupError:
                pass
        while True:
            try:
                pid, _ = os.waitpid(-1, os.WNOHANG)
            except ChildProcessError:
                break
            if not pid:
                break
        if not own_descendants():
            return
        time.sleep(0.01)
    raise AssertionError("Outer test guard could not reap its owned children")


PIPE_HOLDER = """
import json, os, pathlib, subprocess, sys, time
marker, mode = sys.argv[1:]
child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'],
                         start_new_session=(mode == 'escaped'))
def identity(pid):
    fields = pathlib.Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()
    return [pid, int(fields[19])]
pathlib.Path(marker).write_text(json.dumps([identity(os.getpid()), identity(child.pid)]))
print('owned pipe-holder started', flush=True)
if mode == 'timeout':
    time.sleep(30)
"""


@unittest.skipUnless(sys.platform == "linux", "Linux qualification adapter")
class MutationProcessTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # An outer subreaper makes even a deliberately broken predecessor safe:
        # children orphaned by the guard remain ours to kill and reap.
        libc = ctypes.CDLL(None, use_errno=True)
        if libc.prctl(36, 1, 0, 0, 0) != 0:
            raise OSError(ctypes.get_errno(), "test subreaper setup failed")

    def tearDown(self):
        outer_cleanup()

    def run_command(self, command, timeout_ms=350, max_output_bytes=None,
                    send_signal=None, adapter_setup=None):
        args = [sys.executable, str(SUPERVISOR), "--timeout-ms", str(timeout_ms)]
        if max_output_bytes is not None:
            args += ["--max-output-bytes", str(max_output_bytes)]
        args += ["--", *command]
        if adapter_setup:
            args = [sys.executable, "-c", adapter_setup, str(SUPERVISOR), *args[2:]]
        start = time.monotonic()
        process = subprocess.Popen(args, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, start_new_session=True)
        try:
            if send_signal:
                # The marker makes cancellation occur after the owned command
                # has spawned, rather than racing the supervisor's setup.
                marker, sig = send_signal
                deadline = time.monotonic() + 1
                while not marker.exists() and time.monotonic() < deadline:
                    time.sleep(0.01)
                self.assertTrue(marker.exists(), "owned child never became ready")
                process.send_signal(sig)
            try:
                stdout, stderr = process.communicate(timeout=3)
            except subprocess.TimeoutExpired:
                outer_cleanup()
                self.fail("Supervisor exceeded outer 3-second guard; pipe stayed open")
            self.assertEqual(process.returncode, 0, stderr.decode())
            self.assertEqual(stderr, b"", "outer result must be JSON only")
            result = json.loads(stdout)
            self.assertLess(time.monotonic() - start, 2.7)
            self.assertTrue(result["cleanup-complete?"])
            self.assertEqual(own_descendants(), {},
                             "supervisor left owned live/zombie children")
            return result
        finally:
            outer_cleanup()
            process.stdout.close()
            process.stderr.close()

    def pipe_holder(self, mode, cancellation=None):
        with tempfile.TemporaryDirectory(prefix="knoxx-mutation-process-") as directory:
            marker = Path(directory) / "owned-pids.json"
            result = self.run_command(
                [sys.executable, "-c", PIPE_HOLDER, str(marker), mode],
                send_signal=(marker, cancellation) if cancellation else None,
            )
            self.assertTrue(marker.exists())
            for pid, start in json.loads(marker.read_text()):
                self.assertNotEqual(identity(pid), start,
                                    f"owned live/zombie PID {pid} remains")
            self.assertEqual(own_descendants(), {}, "owned process tree remains")
            return result

    def test_timeout_reaps_pipe_holder(self):
        result = self.pipe_holder("timeout")
        self.assertEqual(result["termination"], "timeout")
        self.assertTrue(result["timed-out?"])
        self.assertEqual(result["exit-code"], -signal.SIGKILL)
        self.assertIn("owned pipe-holder started", result["output"])

    def test_exited_parent_reaps_pipe_holder(self):
        result = self.pipe_holder("parent-exit")
        self.assertEqual(result["termination"], "cleanup-error")
        self.assertFalse(result["timed-out?"])
        self.assertEqual(result["exit-code"], 0)

    def test_reaps_group_escaped_grandchild(self):
        result = self.pipe_holder("escaped")
        self.assertEqual(result["termination"], "cleanup-error")
        self.assertFalse(result["timed-out?"])

    def test_tracked_pidfds_clean_without_group_signal_delivery(self):
        # Force group delivery to miss the owned tree. Stable pidfds must still
        # terminate descendants before the retained leader can be reaped.
        wrapper = """
import importlib.util, sys
spec = importlib.util.spec_from_file_location('mutation_process', sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
module.os.killpg = lambda _group, _signal: None
module.main(sys.argv[2:])
"""
        result = self.run_command(
            [sys.executable, "-c", "import time; time.sleep(30)"],
            adapter_setup=wrapper,
        )
        self.assertEqual(result["termination"], "timeout")
        self.assertTrue(result["timed-out?"])
        self.assertEqual(result["exit-code"], -signal.SIGKILL)

    def test_cancellation_cleans_owned_tree(self):
        result = self.pipe_holder("timeout", cancellation=signal.SIGTERM)
        self.assertEqual(result["termination"], "cancelled")
        self.assertFalse(result["timed-out?"])

    def test_normal_exit_and_merged_output(self):
        result = self.run_command([
            sys.executable, "-c",
            "import os; os.write(1,b'first\\n'); os.write(2,b'second\\n')",
        ])
        self.assertEqual(result["termination"], "exited")
        self.assertEqual(result["exit-code"], 0)
        self.assertFalse(result["timed-out?"])
        self.assertFalse(result["capture-truncated?"])
        self.assertEqual(result["output"], "first\nsecond\n")
        self.assertEqual(result["captured-output-bytes"], 13)
        self.assertEqual(result["output-sha256"],
                         hashlib.sha256(b"first\nsecond\n").hexdigest())

    def test_completed_red_exit_is_preserved(self):
        result = self.run_command([
            sys.executable, "-c",
            "print('Ran 1 tests containing 1 assertions.'); "
            "print('1 failures, 0 errors.'); raise SystemExit(1)",
        ])
        self.assertEqual(result["termination"], "exited")
        self.assertEqual(result["exit-code"], 1)

    def test_signal_exit_is_invalid(self):
        result = self.run_command([
            sys.executable, "-c",
            "import os,signal; os.kill(os.getpid(),signal.SIGTERM)",
        ])
        self.assertEqual(result["termination"], "signal")
        self.assertEqual(result["exit-code"], -signal.SIGTERM)

    def test_invalid_utf8_cannot_claim_lossless_normal_capture(self):
        result = self.run_command([
            sys.executable, "-c", "import os; os.write(1,b'\\xff')",
        ])
        self.assertEqual(result["termination"], "cleanup-error")
        self.assertEqual(result["output-sha256"], hashlib.sha256(b"\xff").hexdigest())

    def test_inherited_ignored_sigchld_does_not_lose_child_custody(self):
        previous = signal.signal(signal.SIGCHLD, signal.SIG_IGN)
        try:
            # Exec the adapter from a wrapper with SIG_IGN; only the adapter
            # inherits that disposition, while unittest retains its child PID.
            wrapper = """
import os, signal, sys
signal.signal(signal.SIGCHLD, signal.SIG_IGN)
os.execv(sys.executable, [sys.executable, *sys.argv[1:]])
"""
            # The unittest parent must retain its wrapper PID to wait safely.
            signal.signal(signal.SIGCHLD, signal.SIG_DFL)
            args = [sys.executable, "-c", wrapper, str(SUPERVISOR),
                    "--timeout-ms", "350", "--", sys.executable, "-c", "print('done')"]
            result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    timeout=3)
            self.assertEqual(result.returncode, 0)
            self.assertEqual(result.stderr, b"")
            record = json.loads(result.stdout)
            self.assertEqual(record["termination"], "exited")
            self.assertEqual(record["exit-code"], 0)
            self.assertTrue(record["cleanup-complete?"])
        finally:
            signal.signal(signal.SIGCHLD, previous)

    def test_output_limit_is_bounded_and_invalid(self):
        result = self.run_command([
            sys.executable, "-c", "import os; os.write(1,b'x'*1000000)",
        ], max_output_bytes=128)
        self.assertEqual(result["termination"], "output-limit")
        self.assertTrue(result["capture-truncated?"])
        self.assertEqual(result["captured-output-bytes"], 128)
        self.assertEqual(result["output"], "x" * 128)

    def test_missing_executable_fails_closed(self):
        result = self.run_command(["/nonexistent/knoxx-mutation-fixture"])
        self.assertEqual(result["termination"], "setup-error")
        self.assertIsNone(result["exit-code"])

    def test_postspawn_capture_setup_failure_still_reaps_owned_child(self):
        wrapper = """
import importlib.util, sys
spec = importlib.util.spec_from_file_location('mutation_process', sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
def denied_capture(*args):
    raise OSError('fixture selector/capture setup denied')
module.Capture = denied_capture
module.main(sys.argv[2:])
"""
        result = self.run_command([sys.executable, "-c", "import time; time.sleep(30)"],
                                  adapter_setup=wrapper)
        self.assertEqual(result["termination"], "cleanup-error")
        self.assertTrue(result["cleanup-complete?"])

    def test_platform_and_prctl_setup_fail_closed_without_spawn(self):
        spec = importlib.util.spec_from_file_location("mutation_process", SUPERVISOR)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        with patch.object(module.sys, "platform", "unsupported"), \
                patch.object(module.subprocess, "Popen") as spawn:
            result = module.supervise(["must-not-spawn"], 100)
            self.assertEqual(result["termination"], "setup-error")
            spawn.assert_not_called()
        with patch.object(module.ctypes, "CDLL", side_effect=OSError("prctl denied")), \
                patch.object(module.subprocess, "Popen") as spawn:
            result = module.supervise(["must-not-spawn"], 100)
            self.assertEqual(result["termination"], "setup-error")
            spawn.assert_not_called()
        with patch.object(module, "process_identity", return_value=None), \
                patch.object(module.subprocess, "Popen") as spawn:
            result = module.supervise(["must-not-spawn"], 100)
            self.assertEqual(result["termination"], "setup-error")
            spawn.assert_not_called()
        with patch.object(module.os, "pidfd_open", side_effect=OSError("pidfd denied")), \
                patch.object(module.subprocess, "Popen") as spawn:
            result = module.supervise(["must-not-spawn"], 100)
            self.assertEqual(result["termination"], "setup-error")
            spawn.assert_not_called()


if __name__ == "__main__":
    unittest.main()
