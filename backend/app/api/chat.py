from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import ChatRequest, ChatStartResponse

router = APIRouter(prefix="/api", tags=["chat"])


@router.post("/chat", response_model=ChatStartResponse)
async def create_chat_run(body: ChatRequest, request: Request) -> ChatStartResponse:
    status = request.app.state.llama_manager.status()
    if not status.running:
        raise HTTPException(status_code=409, detail="llama-server is not running")

    run_id = await request.app.state.chat_service.enqueue_chat(body)
    return ChatStartResponse(run_id=run_id, status="queued")
