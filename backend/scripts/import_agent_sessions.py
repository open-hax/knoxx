#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sqlite3
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

DEVEL_PATH_RE = re.compile(r"((?:orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.pi)/[A-Za-z0-9._~:/+\-]+)")
URL_RE = re.compile(r"https?://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%\-]+")


@dataclass
class ImportStats:
    sessions_seen: int = 0
    sessions_imported: int = 0
    events_emitted: int = 0
    batches_sent: int = 0


class EventSink:
    def __init__(
        self,
        *,
        base_url: str | None,
        api_key: str | None,
        batch_size: int,
        dry_run: bool,
        verbose: bool,
        request_timeout_seconds: int,
    ) -> None:
        self.base_url = (base_url or "").rstrip("/")
        self.api_key = api_key or ""
        self.batch_size = max(1, batch_size)
        self.dry_run = dry_run
        self.verbose = verbose
        self.request_timeout_seconds = max(10, request_timeout_seconds)
        self.buffer: list[dict[str, Any]] = []
        self.stats = ImportStats()

    def add(self, event: dict[str, Any]) -> None:
        self.buffer.append(event)
        self.stats.events_emitted += 1
        if len(self.buffer) >= self.batch_size:
            self.flush()

    def flush(self) -> None:
        if not self.buffer:
            return
        batch = self.buffer
        self.buffer = []
        if self.dry_run:
            self.stats.batches_sent += 1
            if self.verbose:
                print(f"[dry-run] batch size={len(batch)} first={batch[0]['id']}")
            return
        if not self.base_url or not self.api_key:
            raise SystemExit("OPENPLANNER_BASE_URL and OPENPLANNER_API_KEY are required unless --dry-run is set")
        payload = json.dumps({"events": batch}, ensure_ascii=False).encode("utf-8")
        req = Request(
            f"{self.base_url}/v1/events",
            data=payload,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urlopen(req, timeout=self.request_timeout_seconds) as resp:
                body = resp.read().decode("utf-8", errors="replace")
                self.stats.batches_sent += 1
                if self.verbose:
                    print(f"[upload] batch size={len(batch)} status={resp.status} body={body[:240]}")
        except HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise SystemExit(f"OpenPlanner ingest failed: HTTP {exc.code}: {body[:1200]}") from exc
        except URLError as exc:
            raise SystemExit(f"OpenPlanner ingest failed: {exc}") from exc


def parse_args() -> argparse.Namespace:
    default_backend_dir = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(
        description="Import pi and OpenCode sessions into Knoxx/OpenPlanner knoxx-session memory."
    )
    parser.add_argument("--env-file", default=str(default_backend_dir / ".env"), help="Optional dotenv file to load before resolving env vars.")
    parser.add_argument("--source", choices=["all", "pi", "opencode"], default="all")
    parser.add_argument("--mode", choices=["condensed", "detailed"], default="condensed")
    parser.add_argument("--project", default=None, help="OpenPlanner project/lake name. Defaults to KNOXX_SESSION_PROJECT_NAME or knoxx-session.")
    parser.add_argument("--openplanner-base-url", default=None, help="Defaults to OPENPLANNER_BASE_URL.")
    parser.add_argument("--openplanner-api-key", default=None, help="Defaults to OPENPLANNER_API_KEY.")
    parser.add_argument("--pi-root", default=str(Path.home() / ".pi" / "agent" / "sessions"))
    parser.add_argument("--opencode-db", default=str(Path.home() / ".local" / "share" / "opencode" / "opencode.db"))
    parser.add_argument("--workspace-root", default=str(Path.home() / "devel"))
    parser.add_argument("--batch-size", type=int, default=200)
    parser.add_argument("--request-timeout-seconds", type=int, default=300)
    parser.add_argument("--max-text-chars", type=int, default=8000)
    parser.add_argument("--limit-sessions", type=int, default=0, help="Optional per-source limit after sorting newest-first.")
    parser.add_argument("--session-match", default="", help="Only import sessions whose id/title/path contains this case-insensitive substring.")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    return parser.parse_args()


def load_dotenv(path: str | None) -> None:
    if not path:
        return
    env_path = Path(path)
    if not env_path.exists():
        return
    for raw in env_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if value and len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
            value = value[1:-1]
        os.environ.setdefault(key, value)


def iso_from_value(value: Any) -> str:
    if value is None:
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    if isinstance(value, (int, float)):
        # Session stores are millisecond based.
        if value > 10_000_000_000:
            value = value / 1000.0
        return datetime.fromtimestamp(value, tz=timezone.utc).isoformat().replace("+00:00", "Z")
    text = str(value).strip()
    if not text:
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    if text.isdigit():
        return iso_from_value(int(text))
    if text.endswith("Z"):
        return text
    try:
        return datetime.fromisoformat(text).astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
    except ValueError:
        return text


def json_dumps(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True)


def sha1_text(*parts: Any, size: int = 24) -> str:
    digest = hashlib.sha1("||".join(str(part) for part in parts).encode("utf-8")).hexdigest()
    return digest[:size]


def clip_text(value: Any, max_chars: int) -> str:
    if value is None:
        return ""
    text = value if isinstance(value, str) else json_dumps(value)
    text = text.replace("\x00", "")
    if len(text) <= max_chars:
        return text
    return text[: max_chars - 32] + f"\n\n[truncated {len(text) - max_chars + 32} chars]"


def trim_mention_token(value: str) -> str:
    return re.sub(r"[\s`'\"\)\]\}>:;,.!?]+$", "", re.sub(r"^[\s`'\"\(\[\{<]+", "", value or ""))


def normalize_relative_path(value: str) -> str:
    normalized = (value or "").replace("\\", "/").strip()
    normalized = re.sub(r"^\./+", "", normalized)
    normalized = re.sub(r"^/+", "", normalized)
    normalized = re.sub(r"/+", "/", normalized)
    return normalized


def normalize_devel_path(value: str, workspace_root: str) -> str | None:
    trimmed = trim_mention_token(value)
    workspace_root = workspace_root.rstrip("/")
    if trimmed.startswith("/app/workspace/devel/"):
        trimmed = trimmed[len("/app/workspace/devel/") :]
    elif workspace_root and trimmed.startswith(workspace_root + "/"):
        trimmed = trimmed[len(workspace_root) + 1 :]
    normalized = normalize_relative_path(trimmed)
    if not normalized:
        return None
    if re.match(r"^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.pi)/", normalized):
        return normalized
    return None


def normalize_web_url(value: str) -> str | None:
    raw = trim_mention_token(value)
    if not raw:
        return None
    try:
        from urllib.parse import urlsplit, urlunsplit

        parsed = urlsplit(raw)
        if not parsed.scheme or not parsed.netloc:
            return None
        path = parsed.path or "/"
        return urlunsplit((parsed.scheme, parsed.netloc, path, parsed.query, ""))
    except Exception:
        return None


def extract_urls(text: str) -> list[str]:
    out: list[str] = []
    seen: set[str] = set()
    for match in URL_RE.findall(text or ""):
        normalized = normalize_web_url(match)
        if normalized and normalized not in seen:
            seen.add(normalized)
            out.append(normalized)
    return out


def extract_devel_paths(text: str, workspace_root: str) -> list[str]:
    out: list[str] = []
    seen: set[str] = set()
    for match in DEVEL_PATH_RE.findall(text or ""):
        normalized = normalize_devel_path(match, workspace_root)
        if normalized and normalized not in seen:
            seen.add(normalized)
            out.append(normalized)
    return out


def build_event(
    *,
    project: str,
    session_key: str,
    event_id: str,
    ts: Any,
    kind: str,
    message_key: str,
    text: Any,
    role: str,
    model: str | None,
    tags: list[str],
    extra: dict[str, Any],
    max_text_chars: int,
) -> dict[str, Any]:
    return {
        "schema": "openplanner.event.v1",
        "id": event_id,
        "ts": iso_from_value(ts),
        "source": "knoxx",
        "kind": kind,
        "source_ref": {
            "project": project,
            "session": session_key,
            "message": message_key,
        },
        "text": clip_text(text, max_text_chars),
        "meta": {
            "role": role,
            "author": "user" if role == "user" else "knoxx",
            "model": model,
            "tags": tags,
        },
        "extra": extra,
    }


def session_node_kind(node_type: str) -> str:
    if node_type == "tool_call":
        return "tool_call"
    if node_type == "tool_result":
        return "tool_result"
    if node_type == "reasoning":
        return "reasoning"
    return "message"


def emit_graph_events(
    sink: EventSink,
    *,
    project: str,
    session_key: str,
    node_id: str,
    ts: Any,
    message_key: str,
    node_type: str,
    text: str,
    label: str,
    model: str | None,
    extra: dict[str, Any],
    max_text_chars: int,
    workspace_root: str,
) -> None:
    node_event = build_event(
        project=project,
        session_key=session_key,
        event_id=f"graph.node.{sha1_text(node_id)}",
        ts=ts,
        kind="graph.node",
        message_key=node_id,
        text=text,
        role="system",
        model=model,
        tags=["knoxx", "graph", "node", node_type, extra.get("import_origin", "unknown")],
        extra={
            "lake": project,
            "node_id": node_id,
            "node_type": node_type,
            "node_kind": session_node_kind(node_type),
            "label": label,
            "entity_key": node_id,
            **extra,
        },
        max_text_chars=max_text_chars,
    )
    sink.add(node_event)

    for path in extract_devel_paths(text, workspace_root):
        sink.add(
            build_event(
                project=project,
                session_key=session_key,
                event_id=f"graph.edge.{sha1_text(node_id, 'mentions_devel_path', path)}",
                ts=ts,
                kind="graph.edge",
                message_key=node_id,
                text=f"{node_id} -> devel:file:{path}",
                role="system",
                model=model,
                tags=["knoxx", "graph", "edge", "mentions_devel_path", extra.get("import_origin", "unknown")],
                extra={
                    "lake": project,
                    "edge_id": f"graph.edge.{sha1_text(node_id, 'mentions_devel_path', path)}",
                    "edge_type": "mentions_devel_path",
                    "source_node_id": node_id,
                    "target_node_id": f"devel:file:{path}",
                    "source_lake": project,
                    "target_lake": "devel",
                    "path": path,
                    **extra,
                },
                max_text_chars=max_text_chars,
            )
        )

    for url in extract_urls(text):
        sink.add(
            build_event(
                project=project,
                session_key=session_key,
                event_id=f"graph.edge.{sha1_text(node_id, 'mentions_web_url', url)}",
                ts=ts,
                kind="graph.edge",
                message_key=node_id,
                text=f"{node_id} -> web:url:{url}",
                role="system",
                model=model,
                tags=["knoxx", "graph", "edge", "mentions_web_url", extra.get("import_origin", "unknown")],
                extra={
                    "lake": project,
                    "edge_id": f"graph.edge.{sha1_text(node_id, 'mentions_web_url', url)}",
                    "edge_type": "mentions_web_url",
                    "source_node_id": node_id,
                    "target_node_id": f"web:url:{url}",
                    "source_lake": project,
                    "target_lake": "web",
                    "url": url,
                    **extra,
                },
                max_text_chars=max_text_chars,
            )
        )


def chunk_text(text: str, max_chars: int) -> list[str]:
    text = (text or "").strip()
    if not text:
        return []
    if len(text) <= max_chars:
        return [text]
    chunks: list[str] = []
    start = 0
    while start < len(text):
        end = min(len(text), start + max_chars)
        if end < len(text):
            split = text.rfind("\n\n", start, end)
            if split <= start:
                split = text.rfind("\n", start, end)
            if split <= start:
                split = end
            end = split
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)
        start = max(end, start + 1)
    return chunks


def emit_condensed_session(
    sink: EventSink,
    *,
    project: str,
    session_key: str,
    session_label: str,
    ts: Any,
    summary_lines: list[str],
    transcript_lines: list[str],
    model: str | None,
    extra: dict[str, Any],
    max_text_chars: int,
    workspace_root: str,
) -> bool:
    transcript = "\n\n".join(line for line in transcript_lines if line and line.strip()).strip()
    summary = "\n".join(line for line in summary_lines if line and line.strip()).strip()
    emitted = False
    if summary:
        emitted = True
        sink.add(
            build_event(
                project=project,
                session_key=session_key,
                event_id=f"import.summary.{sha1_text(session_key, session_label)}",
                ts=ts,
                kind="knoxx.import.session",
                message_key=f"{session_key}:summary",
                text=summary,
                role="system",
                model=model,
                tags=["knoxx", "import", extra.get("import_origin", "unknown"), "session_summary", "condensed"],
                extra=extra,
                max_text_chars=max_text_chars,
            )
        )
    for idx, chunk in enumerate(chunk_text(transcript, max_text_chars), start=1):
        emitted = True
        message_key = f"{session_key}:transcript:{idx}"
        sink.add(
            build_event(
                project=project,
                session_key=session_key,
                event_id=f"import.transcript.{sha1_text(session_key, idx)}",
                ts=ts,
                kind="knoxx.import.transcript",
                message_key=message_key,
                text=chunk,
                role="system",
                model=model,
                tags=["knoxx", "import", extra.get("import_origin", "unknown"), "transcript", "condensed"],
                extra={**extra, "transcript_chunk_index": idx},
                max_text_chars=max_text_chars,
            )
        )
    return emitted


def should_keep_session(match_text: str, needle: str) -> bool:
    if not needle:
        return True
    return needle.lower() in match_text.lower()


def iter_pi_session_files(root: Path) -> list[Path]:
    if not root.exists():
        return []
    files = [path for path in root.rglob("*.jsonl") if path.is_file()]
    files.sort(key=lambda p: p.stat().st_mtime, reverse=True)
    return files


def join_text_parts(parts: Iterable[dict[str, Any]], *, field: str = "text") -> str:
    values: list[str] = []
    for part in parts:
        if part.get("type") == "text":
            text = part.get(field)
            if text:
                values.append(str(text))
    return "\n\n".join(values).strip()


def import_pi_sessions(args: argparse.Namespace, sink: EventSink, project: str) -> None:
    pi_root = Path(args.pi_root)
    files = iter_pi_session_files(pi_root)
    if args.limit_sessions > 0:
        files = files[: args.limit_sessions]

    for path in files:
        sink.stats.sessions_seen += 1
        try:
            lines = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
        except Exception as exc:
            print(f"[warn] skipping pi session {path}: {exc}", file=sys.stderr)
            continue
        if not lines:
            continue
        header = lines[0]
        session_id = str(header.get("id") or path.stem)
        cwd = str(header.get("cwd") or "")
        session_key = f"pi:{session_id}"
        title = path.name
        match_text = " ".join([session_key, cwd, title, str(path)])
        if not should_keep_session(match_text, args.session_match):
            continue

        common_extra = {
            "import_origin": "pi",
            "original_session_id": session_id,
            "source_file": str(path),
            "cwd": cwd,
        }

        if args.mode == "condensed":
            summary_lines = [
                "Imported pi session",
                f"Session: {session_key}",
                f"Original session id: {session_id}",
                f"CWD: {cwd or '(unknown)'}",
                f"Source file: {path}",
            ]
            transcript_lines: list[str] = []
            for idx, entry in enumerate(lines[1:], start=1):
                entry_type = entry.get("type")
                message = entry.get("message") or {}
                if entry_type == "message":
                    role = message.get("role")
                    content = message.get("content") or []
                    if role in {"user", "assistant", "toolResult"}:
                        body = join_text_parts(content)
                        if body:
                            label = role
                            if role == "toolResult":
                                label = f"toolResult:{message.get('toolName') or 'tool'}"
                            transcript_lines.append(f"[{label}]\n{body}")
                    if role == "assistant":
                        for part in content:
                            part_type = part.get("type")
                            if part_type == "thinking":
                                thinking_text = str(part.get("thinking") or "").strip()
                                if thinking_text:
                                    transcript_lines.append(f"[reasoning]\n{thinking_text}")
                            elif part_type == "toolCall":
                                tool_name = part.get("name") or "tool"
                                arguments = clip_text(part.get("arguments"), min(args.max_text_chars, 4000))
                                transcript_lines.append(f"[toolCall:{tool_name}]\n{arguments}")
                elif entry_type == "custom_message" and entry.get("display", True):
                    custom_text = str(entry.get("content") or "").strip()
                    if custom_text:
                        transcript_lines.append(f"[note:{entry.get('customType') or 'custom'}]\n{custom_text}")
            imported_any = emit_condensed_session(
                sink,
                project=project,
                session_key=session_key,
                session_label=path.name,
                ts=header.get("timestamp") or path.stat().st_mtime,
                summary_lines=summary_lines,
                transcript_lines=transcript_lines,
                model=None,
                extra=common_extra,
                max_text_chars=args.max_text_chars,
                workspace_root=args.workspace_root,
            )
            if imported_any:
                sink.stats.sessions_imported += 1
                if args.verbose:
                    print(f"[pi] imported session={session_key} file={path}")
            continue

        imported_any = False
        for idx, entry in enumerate(lines[1:], start=1):
            entry_type = entry.get("type")
            ts = entry.get("timestamp") or header.get("timestamp")

            if entry_type == "message":
                message = entry.get("message") or {}
                role = message.get("role")
                content = message.get("content") or []
                model = message.get("model")
                base_extra = {**common_extra, "entry_index": idx, "message_id": entry.get("id")}

                if role == "user":
                    text = join_text_parts(content)
                    if text:
                        imported_any = True
                        message_key = entry.get("id") or f"pi:{session_id}:msg:{idx}"
                        event_id = f"import.pi.message.{sha1_text(session_id, message_key, 'user')}"
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=event_id,
                                ts=ts,
                                kind="knoxx.message",
                                message_key=message_key,
                                text=text,
                                role="user",
                                model=model,
                                tags=["knoxx", "import", "pi", "message", "user"],
                                extra=base_extra,
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        emit_graph_events(
                            sink,
                            project=project,
                            session_key=session_key,
                            node_id=f"{project}:import:pi:{session_id}:user:{idx}",
                            ts=ts,
                            message_key=message_key,
                            node_type="user_message",
                            text=text,
                            label="Imported pi user message",
                            model=model,
                            extra=base_extra,
                            max_text_chars=args.max_text_chars,
                            workspace_root=args.workspace_root,
                        )
                elif role == "assistant":
                    text_parts = [part for part in content if part.get("type") == "text"]
                    assistant_text = join_text_parts(text_parts)
                    if assistant_text:
                        imported_any = True
                        message_key = entry.get("id") or f"pi:{session_id}:assistant:{idx}"
                        event_id = f"import.pi.message.{sha1_text(session_id, message_key, 'assistant')}"
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=event_id,
                                ts=ts,
                                kind="knoxx.message",
                                message_key=message_key,
                                text=assistant_text,
                                role="assistant",
                                model=model,
                                tags=["knoxx", "import", "pi", "message", "assistant"],
                                extra=base_extra,
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        emit_graph_events(
                            sink,
                            project=project,
                            session_key=session_key,
                            node_id=f"{project}:import:pi:{session_id}:assistant:{idx}",
                            ts=ts,
                            message_key=message_key,
                            node_type="assistant_message",
                            text=assistant_text,
                            label="Imported pi assistant message",
                            model=model,
                            extra=base_extra,
                            max_text_chars=args.max_text_chars,
                            workspace_root=args.workspace_root,
                        )

                    for part_index, part in enumerate(content):
                        part_type = part.get("type")
                        if part_type == "thinking":
                            thinking_text = str(part.get("thinking") or "").strip()
                            if not thinking_text:
                                continue
                            imported_any = True
                            message_key = f"{entry.get('id') or idx}:thinking:{part_index}"
                            sink.add(
                                build_event(
                                    project=project,
                                    session_key=session_key,
                                    event_id=f"import.pi.reasoning.{sha1_text(session_id, message_key)}",
                                    ts=ts,
                                    kind="knoxx.reasoning",
                                    message_key=message_key,
                                    text=thinking_text,
                                    role="system",
                                    model=model,
                                    tags=["knoxx", "import", "pi", "reasoning"],
                                    extra=base_extra,
                                    max_text_chars=args.max_text_chars,
                                )
                            )
                            emit_graph_events(
                                sink,
                                project=project,
                                session_key=session_key,
                                node_id=f"{project}:import:pi:{session_id}:reasoning:{idx}:{part_index}",
                                ts=ts,
                                message_key=message_key,
                                node_type="reasoning",
                                text=thinking_text,
                                label="Imported pi reasoning",
                                model=model,
                                extra=base_extra,
                                max_text_chars=args.max_text_chars,
                                workspace_root=args.workspace_root,
                            )
                        elif part_type == "toolCall":
                            imported_any = True
                            tool_name = str(part.get("name") or "tool")
                            tool_call_id = str(part.get("id") or f"{entry.get('id') or idx}:tool:{part_index}")
                            arguments = part.get("arguments")
                            tool_text = f"Tool call: {tool_name}\nArguments:\n{clip_text(arguments, args.max_text_chars)}"
                            tool_extra = {**base_extra, "tool_name": tool_name, "tool_call_id": tool_call_id, "arguments": arguments}
                            sink.add(
                                build_event(
                                    project=project,
                                    session_key=session_key,
                                    event_id=f"import.pi.tool.{sha1_text(session_id, tool_call_id, 'call')}",
                                    ts=ts,
                                    kind="knoxx.tool_receipt",
                                    message_key=tool_call_id,
                                    text=tool_text,
                                    role="system",
                                    model=model,
                                    tags=["knoxx", "import", "pi", "tool", "call", tool_name],
                                    extra=tool_extra,
                                    max_text_chars=args.max_text_chars,
                                )
                            )
                            emit_graph_events(
                                sink,
                                project=project,
                                session_key=session_key,
                                node_id=f"{project}:import:pi:{session_id}:tool-call:{tool_call_id}",
                                ts=ts,
                                message_key=tool_call_id,
                                node_type="tool_call",
                                text=tool_text,
                                label=f"Imported pi tool call · {tool_name}",
                                model=model,
                                extra=tool_extra,
                                max_text_chars=args.max_text_chars,
                                workspace_root=args.workspace_root,
                            )
                elif role == "toolResult":
                    tool_name = str(message.get("toolName") or "tool")
                    tool_call_id = str(message.get("toolCallId") or f"{entry.get('id') or idx}:tool-result")
                    text = join_text_parts(content)
                    if text:
                        imported_any = True
                        tool_extra = {**base_extra, "tool_name": tool_name, "tool_call_id": tool_call_id}
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=f"import.pi.tool.{sha1_text(session_id, tool_call_id, 'result')}",
                                ts=ts,
                                kind="knoxx.tool_receipt",
                                message_key=tool_call_id,
                                text=f"Tool result: {tool_name}\nOutput:\n{text}",
                                role="system",
                                model=model,
                                tags=["knoxx", "import", "pi", "tool", "result", tool_name],
                                extra=tool_extra,
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        emit_graph_events(
                            sink,
                            project=project,
                            session_key=session_key,
                            node_id=f"{project}:import:pi:{session_id}:tool-result:{tool_call_id}",
                            ts=ts,
                            message_key=tool_call_id,
                            node_type="tool_result",
                            text=text,
                            label=f"Imported pi tool result · {tool_name}",
                            model=model,
                            extra=tool_extra,
                            max_text_chars=args.max_text_chars,
                            workspace_root=args.workspace_root,
                        )
            elif entry_type == "custom_message":
                if not entry.get("display", True):
                    continue
                custom_text = str(entry.get("content") or "").strip()
                if custom_text:
                    imported_any = True
                    sink.add(
                        build_event(
                            project=project,
                            session_key=session_key,
                            event_id=f"import.pi.note.{sha1_text(session_id, idx, entry.get('customType') or 'note')}",
                            ts=ts,
                            kind="knoxx.import.note",
                            message_key=str(entry.get("id") or f"pi:{session_id}:custom:{idx}"),
                            text=custom_text,
                            role="system",
                            model=None,
                            tags=["knoxx", "import", "pi", "note", str(entry.get("customType") or "custom")],
                            extra={**common_extra, "entry_index": idx, "custom_type": entry.get("customType")},
                            max_text_chars=args.max_text_chars,
                        )
                    )

        if imported_any:
            sink.stats.sessions_imported += 1
            if args.verbose:
                print(f"[pi] imported session={session_key} file={path}")


def parse_json_text(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return value
    if not value:
        return {}
    try:
        return json.loads(value)
    except Exception:
        return {}


def fetch_opencode_sessions(db_path: Path) -> list[sqlite3.Row]:
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.cursor()
        cur.execute(
            """
            select
              s.id as session_id,
              s.title,
              s.directory,
              s.time_created,
              s.time_updated,
              p.worktree,
              p.name as project_name,
              w.directory as workspace_dir,
              w.name as workspace_name
            from session s
            left join project p on p.id = s.project_id
            left join workspace w on w.id = s.workspace_id
            order by s.time_updated desc
            """
        )
        return cur.fetchall()
    finally:
        conn.close()


def import_opencode_sessions(args: argparse.Namespace, sink: EventSink, project: str) -> None:
    db_path = Path(args.opencode_db)
    if not db_path.exists():
        print(f"[warn] opencode db not found: {db_path}", file=sys.stderr)
        return

    sessions = fetch_opencode_sessions(db_path)
    if args.limit_sessions > 0:
        sessions = sessions[: args.limit_sessions]

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    try:
        for session_row in sessions:
            sink.stats.sessions_seen += 1
            session_id = str(session_row["session_id"])
            session_key = f"opencode:{session_id}"
            title = str(session_row["title"] or "")
            directory = str(session_row["directory"] or "")
            worktree = str(session_row["worktree"] or "")
            workspace_dir = str(session_row["workspace_dir"] or "")
            match_text = " ".join([session_key, title, directory, worktree, workspace_dir])
            if not should_keep_session(match_text, args.session_match):
                continue

            common_extra = {
                "import_origin": "opencode",
                "original_session_id": session_id,
                "title": title,
                "directory": directory,
                "worktree": worktree,
                "workspace_dir": workspace_dir,
                "workspace_name": session_row["workspace_name"],
                "project_name": session_row["project_name"],
            }

            cur = conn.cursor()
            cur.execute(
                "select id, time_created, data from message where session_id = ? order by time_created asc",
                (session_id,),
            )
            message_rows = cur.fetchall()
            cur.execute(
                "select id, message_id, time_created, data from part where session_id = ? order by time_created asc, id asc",
                (session_id,),
            )
            parts_by_message: dict[str, list[dict[str, Any]]] = defaultdict(list)
            for part_row in cur.fetchall():
                parts_by_message[str(part_row["message_id"])].append(
                    {
                        "id": str(part_row["id"]),
                        "time_created": part_row["time_created"],
                        "data": parse_json_text(part_row["data"]),
                    }
                )

            if args.mode == "condensed":
                summary_lines = [
                    "Imported OpenCode session",
                    f"Session: {session_key}",
                    f"Original session id: {session_id}",
                    f"Title: {title or '(untitled)'}",
                    f"Directory: {directory or '(unknown)'}",
                    f"Worktree: {worktree or '(unknown)'}",
                    f"Workspace: {workspace_dir or '(unknown)'}",
                    f"Messages: {len(message_rows)}",
                    f"Parts: {sum(len(v) for v in parts_by_message.values())}",
                ]
                transcript_lines: list[str] = []
                model = None
                for message_row in message_rows:
                    message_id = str(message_row["id"])
                    message_data = parse_json_text(message_row["data"])
                    role = str(message_data.get("role") or "system")
                    provider = message_data.get("providerID")
                    model_id = message_data.get("modelID")
                    if provider or model_id:
                        model = "/".join(part for part in [str(provider or "").strip(), str(model_id or "").strip()] if part)
                    parts = parts_by_message.get(message_id, [])
                    for part_row in parts:
                        part = part_row["data"]
                        part_type = str(part.get("type") or "")
                        if part_type == "text":
                            text = str(part.get("text") or "").strip()
                            if text:
                                transcript_lines.append(f"[{role}]\n{text}")
                        elif part_type == "reasoning":
                            text = str(part.get("text") or "").strip()
                            if text:
                                transcript_lines.append(f"[reasoning]\n{text}")
                        elif part_type == "tool":
                            tool_name = str(part.get("tool") or "tool")
                            state = part.get("state") or {}
                            input_text = clip_text(state.get("input"), min(args.max_text_chars, 2500))
                            output_text = clip_text(state.get("output"), min(args.max_text_chars, 2500))
                            block = [f"[tool:{tool_name}]"]
                            if input_text:
                                block.append(f"Input:\n{input_text}")
                            if output_text:
                                block.append(f"Output:\n{output_text}")
                            transcript_lines.append("\n".join(block))
                imported_any = emit_condensed_session(
                    sink,
                    project=project,
                    session_key=session_key,
                    session_label=title or session_id,
                    ts=session_row["time_created"],
                    summary_lines=summary_lines,
                    transcript_lines=transcript_lines,
                    model=model,
                    extra=common_extra,
                    max_text_chars=args.max_text_chars,
                    workspace_root=args.workspace_root,
                )
                if imported_any:
                    sink.stats.sessions_imported += 1
                    if args.verbose:
                        print(f"[opencode] imported session={session_key} title={title or '(untitled)'}")
                continue

            imported_any = False
            for message_row in message_rows:
                message_id = str(message_row["id"])
                message_data = parse_json_text(message_row["data"])
                role = str(message_data.get("role") or "system")
                model = None
                provider = message_data.get("providerID")
                model_id = message_data.get("modelID")
                if provider or model_id:
                    model = "/".join(part for part in [str(provider or "").strip(), str(model_id or "").strip()] if part)
                base_extra = {
                    **common_extra,
                    "message_id": message_id,
                    "mode": message_data.get("mode"),
                    "agent": message_data.get("agent"),
                    "variant": message_data.get("variant"),
                    "cwd": (message_data.get("path") or {}).get("cwd"),
                    "root": (message_data.get("path") or {}).get("root"),
                }
                parts = parts_by_message.get(message_id, [])
                for part_index, part_row in enumerate(parts):
                    part = part_row["data"]
                    part_type = str(part.get("type") or "")
                    ts = part_row["time_created"] or message_row["time_created"]
                    part_key = f"{message_id}:{part_index}:{part_type or 'part'}"

                    if part_type == "text":
                        text = str(part.get("text") or "").strip()
                        if not text:
                            continue
                        imported_any = True
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=f"import.opencode.message.{sha1_text(session_id, part_row['id'])}",
                                ts=ts,
                                kind="knoxx.message",
                                message_key=part_key,
                                text=text,
                                role=role if role in {"user", "assistant", "system"} else "system",
                                model=model,
                                tags=["knoxx", "import", "opencode", "message", role],
                                extra={**base_extra, "part_id": part_row["id"], "part_type": part_type},
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        node_type = "assistant_message" if role == "assistant" else "user_message"
                        if role not in {"assistant", "user"}:
                            node_type = "reasoning" if role == "system" else "assistant_message"
                        emit_graph_events(
                            sink,
                            project=project,
                            session_key=session_key,
                            node_id=f"{project}:import:opencode:{session_id}:{node_type}:{sha1_text(part_row['id'])}",
                            ts=ts,
                            message_key=part_key,
                            node_type=node_type,
                            text=text,
                            label=f"Imported OpenCode {role} text",
                            model=model,
                            extra={**base_extra, "part_id": part_row["id"], "part_type": part_type},
                            max_text_chars=args.max_text_chars,
                            workspace_root=args.workspace_root,
                        )
                    elif part_type == "reasoning":
                        text = str(part.get("text") or "").strip()
                        if not text:
                            continue
                        imported_any = True
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=f"import.opencode.reasoning.{sha1_text(session_id, part_row['id'])}",
                                ts=ts,
                                kind="knoxx.reasoning",
                                message_key=part_key,
                                text=text,
                                role="system",
                                model=model,
                                tags=["knoxx", "import", "opencode", "reasoning"],
                                extra={**base_extra, "part_id": part_row["id"], "part_type": part_type},
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        emit_graph_events(
                            sink,
                            project=project,
                            session_key=session_key,
                            node_id=f"{project}:import:opencode:{session_id}:reasoning:{sha1_text(part_row['id'])}",
                            ts=ts,
                            message_key=part_key,
                            node_type="reasoning",
                            text=text,
                            label="Imported OpenCode reasoning",
                            model=model,
                            extra={**base_extra, "part_id": part_row["id"], "part_type": part_type},
                            max_text_chars=args.max_text_chars,
                            workspace_root=args.workspace_root,
                        )
                    elif part_type == "tool":
                        tool_name = str(part.get("tool") or "tool")
                        call_id = str(part.get("callID") or f"{message_id}:tool:{part_index}")
                        state = part.get("state") or {}
                        input_text = clip_text(state.get("input"), args.max_text_chars)
                        output_text = clip_text(state.get("output"), args.max_text_chars)
                        summary = [f"Tool: {tool_name}"]
                        status = state.get("status")
                        if status:
                            summary.append(f"Status: {status}")
                        if input_text:
                            summary.append(f"Input:\n{input_text}")
                        if output_text:
                            summary.append(f"Output:\n{output_text}")
                        summary_text = "\n".join(summary)
                        imported_any = True
                        tool_extra = {
                            **base_extra,
                            "part_id": part_row["id"],
                            "part_type": part_type,
                            "tool_name": tool_name,
                            "tool_call_id": call_id,
                            "status": status,
                        }
                        sink.add(
                            build_event(
                                project=project,
                                session_key=session_key,
                                event_id=f"import.opencode.tool.{sha1_text(session_id, part_row['id'])}",
                                ts=ts,
                                kind="knoxx.tool_receipt",
                                message_key=call_id,
                                text=summary_text,
                                role="system",
                                model=model,
                                tags=["knoxx", "import", "opencode", "tool", tool_name],
                                extra=tool_extra,
                                max_text_chars=args.max_text_chars,
                            )
                        )
                        if input_text:
                            emit_graph_events(
                                sink,
                                project=project,
                                session_key=session_key,
                                node_id=f"{project}:import:opencode:{session_id}:tool-call:{sha1_text(call_id, part_row['id'])}",
                                ts=ts,
                                message_key=call_id,
                                node_type="tool_call",
                                text=input_text,
                                label=f"Imported OpenCode tool call · {tool_name}",
                                model=model,
                                extra=tool_extra,
                                max_text_chars=args.max_text_chars,
                                workspace_root=args.workspace_root,
                            )
                        if output_text:
                            emit_graph_events(
                                sink,
                                project=project,
                                session_key=session_key,
                                node_id=f"{project}:import:opencode:{session_id}:tool-result:{sha1_text(call_id, part_row['id'])}",
                                ts=ts,
                                message_key=call_id,
                                node_type="tool_result",
                                text=output_text,
                                label=f"Imported OpenCode tool result · {tool_name}",
                                model=model,
                                extra=tool_extra,
                                max_text_chars=args.max_text_chars,
                                workspace_root=args.workspace_root,
                            )

            if imported_any:
                sink.stats.sessions_imported += 1
                if args.verbose:
                    print(f"[opencode] imported session={session_key} title={title or '(untitled)'}")
    finally:
        conn.close()


def main() -> int:
    args = parse_args()
    load_dotenv(args.env_file)

    project = args.project or os.environ.get("KNOXX_SESSION_PROJECT_NAME") or "knoxx-session"
    base_url = args.openplanner_base_url or os.environ.get("OPENPLANNER_BASE_URL")
    api_key = args.openplanner_api_key or os.environ.get("OPENPLANNER_API_KEY")

    sink = EventSink(
        base_url=base_url,
        api_key=api_key,
        batch_size=args.batch_size,
        dry_run=args.dry_run,
        verbose=args.verbose,
        request_timeout_seconds=args.request_timeout_seconds,
    )

    print(
        json.dumps(
            {
                "project": project,
                "source": args.source,
                "dry_run": args.dry_run,
                "pi_root": args.pi_root,
                "opencode_db": args.opencode_db,
                "workspace_root": args.workspace_root,
                "session_match": args.session_match or None,
                "limit_sessions": args.limit_sessions or None,
                "mode": args.mode,
            },
            ensure_ascii=False,
        )
    )

    if args.source in {"all", "pi"}:
        before = ImportStats(**sink.stats.__dict__)
        import_pi_sessions(args, sink, project)
        sink.flush()
        after = sink.stats
        print(
            f"[pi] sessions_seen={after.sessions_seen - before.sessions_seen} "
            f"sessions_imported={after.sessions_imported - before.sessions_imported} "
            f"events={after.events_emitted - before.events_emitted} "
            f"batches={after.batches_sent - before.batches_sent}"
        )

    if args.source in {"all", "opencode"}:
        before = ImportStats(**sink.stats.__dict__)
        import_opencode_sessions(args, sink, project)
        sink.flush()
        after = sink.stats
        print(
            f"[opencode] sessions_seen={after.sessions_seen - before.sessions_seen} "
            f"sessions_imported={after.sessions_imported - before.sessions_imported} "
            f"events={after.events_emitted - before.events_emitted} "
            f"batches={after.batches_sent - before.batches_sent}"
        )

    sink.flush()
    print(
        f"[total] sessions_seen={sink.stats.sessions_seen} sessions_imported={sink.stats.sessions_imported} "
        f"events={sink.stats.events_emitted} batches={sink.stats.batches_sent}"
    )
    if not args.dry_run and sink.stats.events_emitted == 0:
        print("[warn] no events emitted", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
