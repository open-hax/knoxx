from __future__ import annotations

from collections import deque
from datetime import UTC, datetime
from threading import Lock
from uuid import uuid4

from app.core.schemas import LoungeMessage


class LoungeService:
    def __init__(self, max_messages: int = 300) -> None:
        self._messages: deque[LoungeMessage] = deque(maxlen=max_messages)
        self._lock = Lock()

    def list_messages(self) -> list[LoungeMessage]:
        with self._lock:
            return list(self._messages)

    def post_message(self, session_id: str, text: str, alias: str | None = None) -> LoungeMessage:
        clean_text = text.strip()
        if not clean_text:
            raise ValueError("Message cannot be empty")

        name = (alias or "").strip() or f"user-{session_id[:6]}"
        msg = LoungeMessage(
            id=uuid4().hex,
            timestamp=datetime.now(UTC),
            session_id=session_id,
            alias=name,
            text=clean_text,
        )

        with self._lock:
            self._messages.append(msg)
        return msg
