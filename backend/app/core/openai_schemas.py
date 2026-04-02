from __future__ import annotations

from typing import Literal

from pydantic import BaseModel


class OpenAIChatMessage(BaseModel):
    role: Literal["system", "user", "assistant", "tool"]
    content: str


class OpenAIChatCompletionRequest(BaseModel):
    model: str
    messages: list[OpenAIChatMessage]
    temperature: float | None = None
    max_tokens: int | None = None
    top_p: float | None = None
    stream: bool = False
    stop: str | list[str] | None = None


class OpenAIEmbeddingRequest(BaseModel):
    model: str
    input: str | list[str]
    knoxx_return_sparse: bool = False
