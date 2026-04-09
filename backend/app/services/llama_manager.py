"""Stub LlamaServerManager - no local inference, Proxx only."""

from __future__ import annotations

from typing import Any, AsyncIterator

from app.core.config import Settings
from app.services.event_bus import EventBus


class LlamaServerManager:
    def __init__(self, settings: Settings, event_bus: EventBus) -> None:
        self._settings = settings
        self._event_bus = event_bus

    @property
    def is_running(self) -> bool:
        return False

    @property
    def current_model(self) -> str | None:
        return None

    @property
    def is_healthy(self) -> bool:
        return False

    async def start(self, model_path: str, **kwargs: Any) -> None:
        pass

    async def stop(self) -> None:
        pass

    async def warmup(self, prompt: str, max_tokens: int = 16) -> dict[str, Any]:
        return {"ok": False, "error": "local inference disabled"}

    async def chat_completion_stream(
        self, messages: list[dict], **kwargs: Any
    ) -> AsyncIterator[str]:
        return
        yield ""
