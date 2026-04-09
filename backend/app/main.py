from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from app.api.chat import router as chat_router
from app.api.config import router as config_router
from app.api.lounge import router as lounge_router
from app.api.models import router as models_router
from app.api.openai import router as openai_router
from app.api.proxx import router as proxx_router
from app.api.rag import router as rag_router
from app.api.runs import router as runs_router
from app.api.knoxx import router as knoxx_router
from app.api.shibboleth import router as shibboleth_router
from app.api.tools import router as tools_router
from app.core.config import get_settings
from app.services.chat_service import ChatService
from app.services.event_bus import CHANNELS, EventBus
from app.services.llama_manager import LlamaServerManager
from app.services.embeddings_service import EmbeddingsService
from app.services.metrics import MetricsSampler
from app.services.model_discovery import ModelDiscoveryService
from app.services.lounge import LoungeService
from app.services.run_store import RunStore


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    event_bus = EventBus()
    model_discovery = ModelDiscoveryService(settings.models_dir)
    llama_manager = LlamaServerManager(settings, event_bus)
    run_store = RunStore(settings.runs_dir, settings.runs_db_path)
    metrics_sampler = MetricsSampler(settings, event_bus, llama_manager)
    lounge_service = LoungeService()
    embeddings_service = EmbeddingsService(settings)
    chat_service = ChatService(
        settings, event_bus, llama_manager, run_store, metrics_sampler
    )
    metrics_sampler.set_activity_providers(
        active_clients_provider=event_bus.active_subscriber_count,
        active_runs_provider=chat_service.active_run_count,
    )

    app.state.settings = settings
    app.state.event_bus = event_bus
    app.state.model_discovery = model_discovery
    app.state.llama_manager = llama_manager
    app.state.run_store = run_store
    app.state.metrics_sampler = metrics_sampler
    app.state.chat_service = chat_service
    app.state.lounge_service = lounge_service
    app.state.embeddings_service = embeddings_service

    await metrics_sampler.start()
    yield
    await metrics_sampler.stop()
    await llama_manager.stop()


app = FastAPI(title="Knoxx Backend", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(models_router)
app.include_router(chat_router)
app.include_router(runs_router)
app.include_router(lounge_router)
app.include_router(config_router)
app.include_router(knoxx_router)
app.include_router(proxx_router)
app.include_router(shibboleth_router)
app.include_router(tools_router)
app.include_router(rag_router)
app.include_router(openai_router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.websocket("/ws/stream")
async def stream_socket(websocket: WebSocket, channels: str | None = None) -> None:
    selected = set(CHANNELS)
    if channels:
        selected = {c.strip() for c in channels.split(",") if c.strip()}

    await websocket.accept()
    session_id = websocket.query_params.get("session_id", "")
    try:
        sub = await websocket.app.state.event_bus.subscribe(selected)
    except ValueError as exc:
        await websocket.send_json({"error": str(exc)})
        await websocket.close(code=1008)
        return

    try:
        while True:
            message = await sub.queue.get()
            if message.channel in {"tokens", "events"}:
                target_session = str(message.payload.get("session_id", ""))
                if target_session and session_id and target_session != session_id:
                    continue
            await websocket.send_json(message.model_dump(mode="json"))
    except WebSocketDisconnect:
        pass
    finally:
        await websocket.app.state.event_bus.unsubscribe(sub.id)
