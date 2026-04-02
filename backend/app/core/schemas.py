from __future__ import annotations

from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class ModelInfo(BaseModel):
    id: str
    name: str
    path: str
    size_bytes: int
    modified_at: datetime
    hash16mb: str
    suggested_ctx: int | None = None


class ModelListResponse(BaseModel):
    models: list[ModelInfo]


class ProxxModelInfo(BaseModel):
    id: str
    name: str
    owned_by: str | None = None


class ProxxModelListResponse(BaseModel):
    models: list[ProxxModelInfo]


class ServerStartRequest(BaseModel):
    model_path: str = Field(description="Absolute path to a .gguf model")
    port: int | None = None
    ctx_size: int | None = None
    threads: int | None = None
    gpu_layers: int | None = None
    batch_size: int | None = None
    ubatch_size: int | None = None
    flash_attention: bool | None = None
    mmap: bool | None = None
    mlock: bool | None = None
    multi_instance_mode: bool = False
    extra_args: list[str] = Field(default_factory=list)


class ServerStatusResponse(BaseModel):
    running: bool
    pid: int | None = None
    model_path: str | None = None
    host: str
    port: int
    started_at: datetime | None = None
    command: list[str] = Field(default_factory=list)


class ServerActionResponse(BaseModel):
    ok: bool
    message: str
    status: ServerStatusResponse


class WarmupRequest(BaseModel):
    prompt: str = "Hello"
    max_tokens: int = 8


class WarmupResponse(BaseModel):
    ok: bool
    latency_ms: float
    model: str | None = None


class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant", "tool"]
    content: str
    name: str | None = None


class ChatRequest(BaseModel):
    model: str | None = None
    messages: list[ChatMessage]
    system_prompt: str | None = None
    temperature: float | None = 0.7
    top_p: float | None = 1.0
    top_k: int | None = None
    min_p: float | None = None
    repeat_penalty: float | None = None
    presence_penalty: float | None = None
    frequency_penalty: float | None = None
    seed: int | None = None
    max_tokens: int | None = 512
    stop: list[str] | None = None
    stream: bool = True
    metadata: dict[str, Any] = Field(default_factory=dict)


class ChatStartResponse(BaseModel):
    run_id: str
    status: Literal["queued", "running"]


class RunSummary(BaseModel):
    run_id: str
    created_at: datetime
    updated_at: datetime
    status: str
    model: str | None = None
    ttft_ms: float | None = None
    total_time_ms: float | None = None
    input_tokens: int | None = None
    output_tokens: int | None = None
    tokens_per_s: float | None = None
    error: str | None = None


class RunDetail(RunSummary):
    request_messages: list[dict[str, Any]]
    settings: dict[str, Any]
    resources: dict[str, Any]


class RunListResponse(BaseModel):
    runs: list[RunSummary]


class WSStreamMessage(BaseModel):
    channel: Literal["tokens", "stats", "console", "events", "lounge"]
    timestamp: datetime
    payload: dict[str, Any]


class LoungeMessage(BaseModel):
    id: str
    timestamp: datetime
    session_id: str
    alias: str
    text: str


class LoungePostRequest(BaseModel):
    session_id: str
    alias: str | None = None
    text: str


class LoungeListResponse(BaseModel):
    messages: list[LoungeMessage]


class FrontendConfigResponse(BaseModel):
    knoxx_admin_url: str
    knoxx_base_url: str
    knoxx_enabled: bool
    proxx_enabled: bool = False
    proxx_default_model: str = ""
    shibboleth_ui_url: str = ""
    shibboleth_enabled: bool = False


class KnoxxChatRequest(BaseModel):
    message: str
    conversation_id: str | None = None


class KnoxxChatResponse(BaseModel):
    answer: str
    conversation_id: str | None = None


class KnoxxHealthResponse(BaseModel):
    reachable: bool
    configured: bool
    base_url: str
    status_code: int | None = None
    details: dict[str, Any] | None = None


class ProxxHealthResponse(BaseModel):
    reachable: bool
    configured: bool
    base_url: str = ""
    status_code: int | None = None
    model_count: int | None = None
    default_model: str | None = None


class ProxxChatRequest(BaseModel):
    model: str | None = None
    messages: list[ChatMessage]
    system_prompt: str | None = None
    temperature: float | None = 0.7
    top_p: float | None = 1.0
    max_tokens: int | None = 512
    stop: list[str] | None = None
    # RAG options
    rag_enabled: bool = False
    rag_collection: str = "devel_docs"
    rag_limit: int = 5
    rag_threshold: float = 0.6


class ProxxChatResponse(BaseModel):
    answer: str
    model: str | None = None
    rag_context: list[dict[str, Any]] | None = None


class ShibbolethTranscriptItem(BaseModel):
    role: Literal["user", "assistant"]
    content: str
    metadata: dict[str, Any] = Field(default_factory=dict)


class ShibbolethHandoffRequest(BaseModel):
    model: str | None = None
    system_prompt: str | None = None
    provider: str | None = None
    conversation_id: str | None = None
    fake_tools_enabled: bool = False
    items: list[ShibbolethTranscriptItem]


class ShibbolethHandoffResponse(BaseModel):
    ok: bool
    session_id: str
    ui_url: str = ""
    imported_item_count: int = 0
