from __future__ import annotations

import httpx

from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import (
    ServerActionResponse,
    ServerStartRequest,
    ServerStatusResponse,
    WarmupRequest,
    WarmupResponse,
)

router = APIRouter(prefix="/api/server", tags=["server"])


@router.post("/start", response_model=ServerActionResponse)
async def start_server(body: ServerStartRequest, request: Request) -> ServerActionResponse:
    manager = request.app.state.llama_manager
    try:
        status = await manager.start(body)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return ServerActionResponse(ok=True, message="llama-server started", status=status)


@router.post("/stop", response_model=ServerActionResponse)
async def stop_server(request: Request) -> ServerActionResponse:
    manager = request.app.state.llama_manager
    status = await manager.stop()
    return ServerActionResponse(ok=True, message="llama-server stopped", status=status)


@router.get("/status", response_model=ServerStatusResponse)
async def server_status(request: Request) -> ServerStatusResponse:
    manager = request.app.state.llama_manager
    return manager.status()


@router.get("/health")
async def server_health(request: Request) -> dict[str, object]:
    manager = request.app.state.llama_manager
    status = manager.status()
    if not status.running:
        return {"running": False, "healthy": False}

    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(3.0)) as client:
            response = await client.get(f"{manager.base_url}/health")
            return {"running": True, "healthy": response.status_code < 400, "status_code": response.status_code}
    except Exception:
        return {"running": True, "healthy": False}


@router.post("/warmup", response_model=WarmupResponse)
async def warmup_server(body: WarmupRequest, request: Request) -> WarmupResponse:
    manager = request.app.state.llama_manager
    status = manager.status()
    if not status.running:
        raise HTTPException(status_code=409, detail="llama-server is not running")

    latency_ms, model = await manager.warmup(prompt=body.prompt, max_tokens=body.max_tokens)
    return WarmupResponse(ok=True, latency_ms=latency_ms, model=model)
