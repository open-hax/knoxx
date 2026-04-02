from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from app.core.schemas import RunDetail, RunSummary


class RunStore:
    def __init__(self, runs_dir: Path, db_path: Path) -> None:
        self._runs_dir = runs_dir
        self._runs_dir.mkdir(parents=True, exist_ok=True)
        self._db_path = db_path
        self._db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    def create_run(
        self,
        run_id: str,
        model: str | None,
        request_messages: list[dict[str, Any]],
        settings: dict[str, Any],
        resources: dict[str, Any],
    ) -> None:
        now = self._utc_now()
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO runs (
                    run_id, created_at, updated_at, status, model,
                    request_messages_json, settings_json, resources_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    run_id,
                    now,
                    now,
                    "running",
                    model,
                    json.dumps(request_messages, ensure_ascii=True),
                    json.dumps(settings, ensure_ascii=True),
                    json.dumps(resources, ensure_ascii=True),
                ),
            )
            conn.commit()
        self.append_event(run_id, "run_started", {"model": model})

    def append_event(self, run_id: str, event_type: str, payload: dict[str, Any]) -> None:
        line = {
            "timestamp": self._utc_now(),
            "type": event_type,
            "payload": payload,
        }
        file_path = self._run_file(run_id)
        with file_path.open("a", encoding="utf-8") as fp:
            fp.write(json.dumps(line, ensure_ascii=True) + "\n")

    def finalize_run(
        self,
        run_id: str,
        status: str,
        ttft_ms: float | None,
        total_time_ms: float | None,
        input_tokens: int | None,
        output_tokens: int | None,
        tokens_per_s: float | None,
        resources: dict[str, Any],
        error: str | None = None,
    ) -> None:
        now = self._utc_now()
        with self._connect() as conn:
            conn.execute(
                """
                UPDATE runs
                SET updated_at = ?, status = ?, ttft_ms = ?, total_time_ms = ?,
                    input_tokens = ?, output_tokens = ?, tokens_per_s = ?,
                    resources_json = ?, error = ?
                WHERE run_id = ?
                """,
                (
                    now,
                    status,
                    ttft_ms,
                    total_time_ms,
                    input_tokens,
                    output_tokens,
                    tokens_per_s,
                    json.dumps(resources, ensure_ascii=True),
                    error,
                    run_id,
                ),
            )
            conn.commit()
        self.append_event(
            run_id,
            "run_finished",
            {
                "status": status,
                "ttft_ms": ttft_ms,
                "total_time_ms": total_time_ms,
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "tokens_per_s": tokens_per_s,
                "error": error,
            },
        )

    def list_runs(self, limit: int = 100) -> list[RunSummary]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT run_id, created_at, updated_at, status, model, ttft_ms, total_time_ms,
                       input_tokens, output_tokens, tokens_per_s, error
                FROM runs
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()
        return [self._row_to_summary(row) for row in rows]

    def get_run(self, run_id: str) -> RunDetail | None:
        with self._connect() as conn:
            row = conn.execute(
                """
                SELECT run_id, created_at, updated_at, status, model, ttft_ms, total_time_ms,
                       input_tokens, output_tokens, tokens_per_s, error,
                       request_messages_json, settings_json, resources_json
                FROM runs
                WHERE run_id = ?
                """,
                (run_id,),
            ).fetchone()
        if not row:
            return None

        return RunDetail(
            run_id=row[0],
            created_at=self._parse_dt(row[1]),
            updated_at=self._parse_dt(row[2]),
            status=row[3],
            model=row[4],
            ttft_ms=row[5],
            total_time_ms=row[6],
            input_tokens=row[7],
            output_tokens=row[8],
            tokens_per_s=row[9],
            error=row[10],
            request_messages=json.loads(row[11] or "[]"),
            settings=json.loads(row[12] or "{}"),
            resources=json.loads(row[13] or "{}"),
        )

    def export_run(self, run_id: str) -> dict[str, Any] | None:
        detail = self.get_run(run_id)
        if detail is None:
            return None

        events: list[dict[str, Any]] = []
        run_file = self._run_file(run_id)
        if run_file.exists():
            with run_file.open("r", encoding="utf-8") as fp:
                for line in fp:
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        events.append(json.loads(line))
                    except json.JSONDecodeError:
                        continue

        return {
            "run": detail.model_dump(mode="json"),
            "events": events,
        }

    def _run_file(self, run_id: str) -> Path:
        return self._runs_dir / f"{run_id}.jsonl"

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self._db_path)

    def _init_db(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS runs (
                    run_id TEXT PRIMARY KEY,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    status TEXT NOT NULL,
                    model TEXT,
                    request_messages_json TEXT NOT NULL,
                    settings_json TEXT NOT NULL,
                    resources_json TEXT NOT NULL,
                    ttft_ms REAL,
                    total_time_ms REAL,
                    input_tokens INTEGER,
                    output_tokens INTEGER,
                    tokens_per_s REAL,
                    error TEXT
                )
                """
            )
            conn.execute("CREATE INDEX IF NOT EXISTS idx_runs_created_at ON runs(created_at DESC)")
            conn.commit()

    @staticmethod
    def _utc_now() -> str:
        return datetime.now(UTC).isoformat()

    @staticmethod
    def _parse_dt(value: str) -> datetime:
        return datetime.fromisoformat(value)

    @staticmethod
    def _row_to_summary(row: tuple[Any, ...]) -> RunSummary:
        return RunSummary(
            run_id=row[0],
            created_at=datetime.fromisoformat(row[1]),
            updated_at=datetime.fromisoformat(row[2]),
            status=row[3],
            model=row[4],
            ttft_ms=row[5],
            total_time_ms=row[6],
            input_tokens=row[7],
            output_tokens=row[8],
            tokens_per_s=row[9],
            error=row[10],
        )
