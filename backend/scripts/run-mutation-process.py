#!/usr/bin/env python3
"""Bounded Linux subprocess custody for qualification tooling.

Run this adapter as a fresh process. Its subreaper setting and child ownership
are process-local. It never searches for or signals unrelated host processes.
A normal leader exit with live descendants is invalid, even after cleanup.
SIGKILL of this adapter cannot run cleanup; the caller still needs an outer
deadline and must reject a missing result. No test counters are interpreted here.
"""

import argparse
import ctypes
import hashlib
import json
import os
from pathlib import Path
import selectors
import signal
import subprocess
import sys
import time


DEFAULT_MAX_OUTPUT_BYTES = 8 * 1024 * 1024
DEFAULT_CLEANUP_MS = 2000
POLL_SECONDS = 0.01


def process_identity(pid):
    """Read kernel identity and parentage without inspecting command/env data."""
    try:
        fields = Path(f"/proc/{pid}/stat").read_text().rsplit(")", 1)[1].split()
        return {
            "start": int(fields[19]), "parent": int(fields[1]),
            "group": int(fields[2]), "state": fields[0],
        }
    except (FileNotFoundError, ProcessLookupError):
        return None


def direct_children(pid):
    """Include children forked by any thread of an owned Linux process."""
    result = set()
    try:
        tasks = list(Path(f"/proc/{pid}/task").iterdir())
    except (FileNotFoundError, ProcessLookupError):
        return result
    for task in tasks:
        try:
            result.update(int(value) for value in (task / "children").read_text().split())
        except (FileNotFoundError, ProcessLookupError):
            pass
    return result


def enable_subreaper():
    """Refuse to spawn unless Linux can retain/reap orphaned owned children."""
    required = (hasattr(os, "pidfd_open"), hasattr(signal, "pidfd_send_signal"),
                hasattr(os, "waitid"), hasattr(os, "WNOWAIT"))
    if sys.platform != "linux" or not all(required):
        raise RuntimeError("Linux subreaper, pidfd, and waitid support required")
    if not process_identity(os.getpid()) or not Path("/proc/self/task").is_dir():
        raise RuntimeError("Readable Linux /proc child custody required")
    # This live main task cannot disappear before spawn. Read its children
    # interface strictly so unsupported procfs is not mistaken for no children.
    children = Path(f"/proc/self/task/{os.getpid()}/children").read_text().split()
    if children or direct_children(os.getpid()):
        raise RuntimeError("Supervisor must start without pre-existing children")
    probe = os.pidfd_open(os.getpid())
    os.close(probe)
    libc = ctypes.CDLL(None, use_errno=True)
    if libc.prctl(36, ctypes.c_ulong(1), 0, 0, 0) != 0:
        raise OSError(ctypes.get_errno(), "PR_SET_CHILD_SUBREAPER failed")
    enabled = ctypes.c_int()
    if libc.prctl(37, ctypes.byref(enabled), 0, 0, 0) != 0 or enabled.value != 1:
        raise RuntimeError("PR_GET_CHILD_SUBREAPER did not confirm custody")


def close_custody_resources(resources):
    """Attempt every owned resource close and retain all ordinary failures."""
    failures = []
    for name, close in resources:
        try:
            close()
        except Exception as error:
            failures.append(f"{name} {type(error).__name__}: {error}")
    if failures:
        raise RuntimeError("; ".join(failures))


class OwnedChildren:
    """Track only descendants of this fresh adapter, using stable pidfds."""

    def __init__(self, leader):
        self.leader = leader
        self.leader_retained = True
        self.entries = {}
        self.remember(leader)

    def remember(self, pid):
        before = process_identity(pid)
        if before is None:
            return
        existing = self.entries.get(pid)
        if existing and existing[0] == before["start"]:
            return
        fd = os.pidfd_open(pid)
        after = process_identity(pid)
        if after is None or before["start"] != after["start"]:
            os.close(fd)
            return
        if existing:
            os.close(existing[1])
        self.entries[pid] = (after["start"], fd)

    def current(self, pid):
        entry = self.entries.get(pid)
        identity = process_identity(pid)
        return identity if entry and identity and entry[0] == identity["start"] else None

    def discover(self):
        pending = [os.getpid()]
        pending.extend(pid for pid in self.entries if self.current(pid))
        visited = set()
        while pending:
            parent = pending.pop()
            if parent in visited:
                continue
            visited.add(parent)
            if parent != os.getpid() and not self.current(parent):
                continue
            for pid in direct_children(parent):
                identity = process_identity(pid)
                if identity and identity["parent"] == parent:
                    try:
                        self.remember(pid)
                    except ProcessLookupError:
                        continue
                    if pid in self.entries:
                        pending.append(pid)

    def reap_nonleader(self):
        # Keep the leader unreaped until group teardown is finished. Its reserved
        # PID prevents the group id from being reused by an unrelated process.
        for pid in list(self.entries):
            current = self.current(pid)
            if pid != self.leader and current and current["parent"] == os.getpid():
                try:
                    reaped, _ = os.waitpid(pid, os.WNOHANG)
                except ChildProcessError:
                    reaped = 0
                if reaped:
                    os.close(self.entries.pop(pid)[1])

    def live_descendants(self):
        return [pid for pid in self.entries if pid != self.leader
                and (current := self.current(pid)) and current["state"] != "Z"]

    def kill_owned(self):
        leader = self.current(self.leader)
        if self.leader_retained and leader and leader["group"] == self.leader:
            try:
                os.killpg(self.leader, signal.SIGKILL)
            except ProcessLookupError:
                pass
        for pid, (_, fd) in list(self.entries.items()):
            if self.current(pid):
                try:
                    signal.pidfd_send_signal(fd, signal.SIGKILL)
                except ProcessLookupError:
                    pass

    def close(self):
        resources = [(f"pidfd {pid}", lambda fd=fd: os.close(fd))
                     for pid, (_, fd) in self.entries.items()]
        self.entries.clear()
        close_custody_resources(resources)


class Capture:
    """Drain a merged pipe without a blocking read or unbounded allocation."""

    def __init__(self, pipe, limit):
        self.pipe = pipe
        self.limit = limit
        self.data = bytearray()
        self.truncated = False
        self.eof = False
        self.selector = selectors.DefaultSelector()
        os.set_blocking(pipe.fileno(), False)
        self.selector.register(pipe, selectors.EVENT_READ)

    def drain(self, wait_seconds):
        if self.eof:
            if wait_seconds:
                time.sleep(wait_seconds)
            return
        for key, _ in self.selector.select(wait_seconds):
            try:
                chunk = os.read(key.fd, 65536)
            except BlockingIOError:
                continue
            if not chunk:
                self.eof = True
                self.selector.unregister(self.pipe)
                continue
            remaining = self.limit - len(self.data)
            self.data.extend(chunk[:remaining])
            if len(chunk) > remaining:
                self.truncated = True

    def close(self):
        close_custody_resources([("selector", self.selector.close),
                                ("output pipe", self.pipe.close)])


def leader_exit(pid):
    """Observe exit without reaping, preserving the private process-group id."""
    return os.waitid(os.P_PID, pid, os.WEXITED | os.WNOHANG | os.WNOWAIT)


def reap_adopted_children(leader, terminate):
    """Reap this fresh adapter's adopted children, even after setup failed.

    These direct child PIDs remain reserved until this single-threaded adapter
    reaps them (SIGCHLD is explicitly default). Their kernel parentage grants
    custody without relying on capture/selector or pidfd allocation succeeding.
    """
    for pid in direct_children(os.getpid()) - {leader}:
        current = process_identity(pid)
        if current and current["parent"] == os.getpid():
            try:
                if terminate:
                    os.kill(pid, signal.SIGKILL)
                os.waitpid(pid, os.WNOHANG)
            except (ChildProcessError, ProcessLookupError):
                pass


def result_record(termination, output=b"", exit_code=None, timed_out=False,
                  truncated=False, cleanup_complete=True, limit=DEFAULT_MAX_OUTPUT_BYTES,
                  diagnostic=""):
    return {
        "exit-code": exit_code, "timed-out?": timed_out,
        "termination": termination, "output": output.decode("utf-8", errors="replace"),
        "capture-truncated?": truncated, "cleanup-complete?": cleanup_complete,
        "output-sha256": hashlib.sha256(output).hexdigest(),
        "captured-output-bytes": len(output), "max-output-bytes": limit,
        "diagnostic": diagnostic[:1000],
    }


def supervise(command, timeout_ms, max_output_bytes=DEFAULT_MAX_OUTPUT_BYTES,
              cleanup_ms=DEFAULT_CLEANUP_MS):
    """Execute one command and return bounded, inspectable process evidence."""
    process = owned = capture = None
    termination = "setup-error"
    diagnostic = ""
    cancelled = []
    handlers = {}
    cleanup_complete = True
    timed_out = False
    observed_exit_code = None

    def observe_leader_exit():
        nonlocal observed_exit_code
        observed = leader_exit(process.pid)
        if observed:
            observed_exit_code = (observed.si_status if observed.si_code == os.CLD_EXITED
                                  else -observed.si_status)
        return observed

    def finalization_failure(stage, error):
        nonlocal termination, diagnostic, cleanup_complete
        termination = "cleanup-error" if process else "setup-error"
        cleanup_complete = False
        diagnostic = f"{diagnostic}; {stage} {type(error).__name__}: {error}".lstrip("; ")

    try:
        if not command or min(timeout_ms, max_output_bytes, cleanup_ms) <= 0:
            raise ValueError("Command and positive timeout/output/cleanup limits required")
        enable_subreaper()
        for sig in (signal.SIGINT, signal.SIGTERM):
            handlers[sig] = signal.signal(sig, lambda received, _frame: cancelled.append(received))
        # An inherited SIGCHLD=SIG_IGN would auto-reap the leader, destroying
        # the PID reservation used to make group signalling safe.
        handlers[signal.SIGCHLD] = signal.signal(signal.SIGCHLD, signal.SIG_DFL)
        process = subprocess.Popen(command, stdin=subprocess.DEVNULL,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        owned = OwnedChildren(process.pid)
        capture = Capture(process.stdout, max_output_bytes)
        deadline = time.monotonic() + timeout_ms / 1000
        exit_observed_at = None
        while True:
            owned.discover()
            owned.reap_nonleader()
            capture.drain(POLL_SECONDS)
            observed_exit = observe_leader_exit()
            now = time.monotonic()
            if cancelled:
                termination = "cancelled"
                break
            if capture.truncated:
                termination = "output-limit"
                break
            if observed_exit:
                if observed_exit.si_code != os.CLD_EXITED:
                    termination = "signal"
                    break
                if owned.live_descendants():
                    termination = "cleanup-error"
                    diagnostic = "Leader exited with live owned descendants"
                    break
                exit_observed_at = exit_observed_at or now
                if capture.eof:
                    termination = "exited"
                    break
                if now - exit_observed_at >= 0.1:
                    termination = "cleanup-error"
                    diagnostic = "Output pipe stayed open after leader exit"
                    break
            elif now >= deadline:
                termination = "timeout"
                timed_out = True
                break
    except Exception as error:
        termination = "cleanup-error" if process else "setup-error"
        diagnostic = f"{type(error).__name__}: {error}"
    finally:
        if process:
            cleanup_complete = False
            try:
                cleanup_deadline = time.monotonic() + cleanup_ms / 1000
                # Popen established a private session, and its leader is still
                # unreaped. Group cleanup cannot hit a reused/shared group id.
                # Adoption then brings even setsid/double-fork escapes directly
                # under this subreaper; kill/reap them until custody is empty.
                if not capture:
                    process.stdout.close()
                while time.monotonic() < cleanup_deadline:
                    if owned:
                        owned.discover()
                    children = direct_children(os.getpid()) - {process.pid}
                    if termination == "exited" and (
                            children or (owned and owned.live_descendants())):
                        termination = "cleanup-error"
                        diagnostic = "Leader exited with late owned descendants"
                    if termination != "exited":
                        # Stable pidfds also reach descendants that escaped the
                        # private group but have not yet been adopted.
                        if owned:
                            owned.kill_owned()
                        elif process.returncode is None:
                            try:
                                os.killpg(process.pid, signal.SIGKILL)
                            except ProcessLookupError:
                                pass
                        reap_adopted_children(process.pid, True)
                    else:
                        reap_adopted_children(process.pid, False)
                    if capture:
                        capture.drain(POLL_SECONDS)
                    else:
                        time.sleep(POLL_SECONDS)
                    if capture and capture.truncated and termination == "exited":
                        termination = "output-limit"
                    exited = process.returncode is not None or observe_leader_exit()
                    children = direct_children(os.getpid()) - {process.pid}
                    if exited and not children and (not capture or capture.eof):
                        if process.returncode is None:
                            process.wait(timeout=0)
                            if owned:
                                owned.leader_retained = False
                        if not direct_children(os.getpid()):
                            cleanup_complete = True
                            break
                if not cleanup_complete:
                    termination = "cleanup-error"
                    diagnostic = (diagnostic + "; cleanup deadline exceeded").lstrip("; ")
                    process.poll()
                    if owned and process.returncode is not None:
                        owned.leader_retained = False
            except Exception as error:
                termination = "cleanup-error"
                diagnostic = f"{diagnostic}; cleanup {type(error).__name__}: {error}".lstrip("; ")
            finally:
                finalizers = []
                if capture:
                    finalizers.append(("capture close", capture.close))
                elif process.stdout:
                    finalizers.append(("output pipe close", process.stdout.close))
                if owned:
                    finalizers.append(("owned pidfds close", owned.close))
                for stage, finalize in finalizers:
                    try:
                        finalize()
                    except Exception as error:
                        finalization_failure(stage, error)
        for sig, handler in handlers.items():
            try:
                signal.signal(sig, handler)
            except Exception as error:
                finalization_failure(f"restore signal {sig}", error)
    output = bytes(capture.data) if capture else b""
    try:
        output.decode("utf-8")
    except UnicodeDecodeError:
        termination = "cleanup-error"
        diagnostic = (diagnostic + "; Captured output is not losslessly representable as UTF-8 text").lstrip("; ")
    exit_code = (process.returncode if process and process.returncode is not None
                 else observed_exit_code)
    return result_record(termination, output, exit_code,
                         timed_out, capture.truncated if capture else bool(process),
                         cleanup_complete, max_output_bytes, diagnostic)


class JsonArgumentParser(argparse.ArgumentParser):
    """Keep argument/setup failures on the same single-JSON stdout protocol."""

    def error(self, message):
        raise ValueError(message)


def main(argv=None):
    parser = JsonArgumentParser(add_help=False)
    parser.add_argument("--timeout-ms", required=True, type=int)
    parser.add_argument("--max-output-bytes", type=int, default=DEFAULT_MAX_OUTPUT_BYTES)
    parser.add_argument("--cleanup-ms", type=int, default=DEFAULT_CLEANUP_MS)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    try:
        args = parser.parse_args(argv)
        command = args.command[1:] if args.command[:1] == ["--"] else args.command
    except (ValueError, argparse.ArgumentError) as error:
        result = result_record("setup-error", diagnostic=f"{type(error).__name__}: {error}")
    else:
        result = supervise(command, args.timeout_ms, args.max_output_bytes, args.cleanup_ms)
    print(json.dumps(result, ensure_ascii=True, separators=(",", ":")))


if __name__ == "__main__":
    main()
