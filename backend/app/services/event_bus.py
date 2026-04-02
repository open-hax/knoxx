from __future__ import annotations

import asyncio
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from app.core.schemas import WSStreamMessage

CHANNELS = {"tokens", "stats", "console", "events", "lounge"}


@dataclass(slots=True)
class Subscription:
    id: str
    channels: set[str]
    queue: asyncio.Queue[WSStreamMessage]


class EventBus:
    def __init__(self, queue_size: int = 1000) -> None:
        self._queue_size = queue_size
        self._subs: dict[str, Subscription] = {}
        self._lock = asyncio.Lock()

    async def subscribe(self, channels: set[str] | None = None) -> Subscription:
        if not channels:
            channels = set(CHANNELS)
        invalid = channels - CHANNELS
        if invalid:
            raise ValueError(f"Invalid channels: {sorted(invalid)}")

        sub = Subscription(id=uuid4().hex, channels=channels, queue=asyncio.Queue(maxsize=self._queue_size))
        async with self._lock:
            self._subs[sub.id] = sub
        return sub

    async def unsubscribe(self, sub_id: str) -> None:
        async with self._lock:
            self._subs.pop(sub_id, None)

    def active_subscriber_count(self) -> int:
        return len(self._subs)

    async def publish(self, channel: str, payload: dict[str, Any]) -> None:
        if channel not in CHANNELS:
            raise ValueError(f"Unknown channel '{channel}'")

        message = WSStreamMessage(channel=channel, timestamp=datetime.now(UTC), payload=payload)
        async with self._lock:
            subscribers = [s for s in self._subs.values() if channel in s.channels]

        for sub in subscribers:
            if sub.queue.full():
                try:
                    sub.queue.get_nowait()
                except asyncio.QueueEmpty:
                    pass
            try:
                sub.queue.put_nowait(message)
            except asyncio.QueueFull:
                continue
