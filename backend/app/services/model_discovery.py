from __future__ import annotations

import hashlib
import re
from datetime import UTC, datetime
from pathlib import Path

from app.core.schemas import ModelInfo


class ModelDiscoveryService:
    def __init__(self, models_dir: Path) -> None:
        self._models_dir = models_dir

    def scan(self) -> list[ModelInfo]:
        self._models_dir.mkdir(parents=True, exist_ok=True)
        model_paths = sorted(self._models_dir.rglob("*.gguf"), key=lambda p: p.stat().st_mtime, reverse=True)
        return [self._to_model_info(path) for path in model_paths if path.is_file()]

    def _to_model_info(self, path: Path) -> ModelInfo:
        stats = path.stat()
        return ModelInfo(
            id=path.stem,
            name=path.name,
            path=str(path.resolve()),
            size_bytes=stats.st_size,
            modified_at=datetime.fromtimestamp(stats.st_mtime, tz=UTC),
            hash16mb=self._sha256_16mb(path),
            suggested_ctx=self._guess_ctx(path.name),
        )

    @staticmethod
    def _guess_ctx(filename: str) -> int | None:
        match = re.search(r"(\d+(?:\.\d+)?)B", filename, re.IGNORECASE)
        if not match:
            return None
        try:
            size_b = float(match.group(1))
        except ValueError:
            return None
        if size_b <= 2:
            return 8192
        if size_b <= 9:
            return 8192
        return 4096

    @staticmethod
    def _sha256_16mb(path: Path) -> str:
        hasher = hashlib.sha256()
        max_bytes = 16 * 1024 * 1024
        chunk_size = 1024 * 1024
        read_total = 0

        with path.open("rb") as fp:
            while read_total < max_bytes:
                block = fp.read(min(chunk_size, max_bytes - read_total))
                if not block:
                    break
                hasher.update(block)
                read_total += len(block)

        return hasher.hexdigest()
