from __future__ import annotations

import json
import asyncio
from pathlib import Path
from time import time
from uuid import uuid4

import httpx
from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.openai_schemas import OpenAIChatCompletionRequest, OpenAIEmbeddingRequest

router = APIRouter(prefix="/v1", tags=["openai"])
_bearer = HTTPBearer(auto_error=False)


def _openai_error(status_code: int, message: str, code: str = "bad_request") -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "error": {
                "message": message,
                "type": "invalid_request_error",
                "param": None,
                "code": code,
            }
        },
    )


async def require_openai_key(
    request: Request,
    creds: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> None:
    expected = request.app.state.settings.model_lab_openai_api_key
    if not expected:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="MODEL_LAB_OPENAI_API_KEY is not configured")
    provided = creds.credentials if creds else ""
    if creds is None or creds.scheme.lower() != "bearer" or provided != expected:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API key")


@router.get("/models", dependencies=[Depends(require_openai_key)])
async def list_models(request: Request) -> dict[str, object]:
    status_obj = request.app.state.llama_manager.status()
    model_id = "local-model"
    if status_obj.model_path:
        model_id = Path(status_obj.model_path).stem
    return {"object": "list", "data": [{"id": model_id, "object": "model"}]}


@router.post("/chat/completions", dependencies=[Depends(require_openai_key)])
async def chat_completions(request: Request, body: OpenAIChatCompletionRequest):
    payload: dict[str, object] = {
        "model": body.model,
        "messages": [m.model_dump(exclude_none=True) for m in body.messages],
        "stream": body.stream,
    }
    if body.temperature is not None:
        payload["temperature"] = body.temperature
    if body.max_tokens is not None:
        payload["max_tokens"] = body.max_tokens
    if body.top_p is not None:
        payload["top_p"] = body.top_p
    if body.stop is not None:
        payload["stop"] = body.stop

    timeout = httpx.Timeout(request.app.state.settings.local_request_timeout_seconds)
    upstream_url = f"{request.app.state.llama_manager.base_url}/v1/chat/completions"

    if body.stream:
        async def stream_response():
            async with httpx.AsyncClient(timeout=timeout) as client:
                async with client.stream("POST", upstream_url, json=payload) as resp:
                    if resp.status_code >= 400:
                        error_text = (await resp.aread()).decode("utf-8", errors="replace")
                        yield f"data: {json.dumps({'error': {'message': error_text or 'Upstream chat failed'}})}\n\n"
                        yield "data: [DONE]\n\n"
                        return
                    async for chunk in resp.aiter_bytes():
                        yield chunk

        return StreamingResponse(stream_response(), media_type="text/event-stream")

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(upstream_url, json=payload)
            resp.raise_for_status()
    except Exception as exc:
        return _openai_error(502, f"Upstream chat request failed: {exc}", code="upstream_error")

    raw = resp.json()
    choices = raw.get("choices") or []
    content = ""
    finish_reason = "stop"
    if choices:
        first = choices[0] or {}
        finish_reason = first.get("finish_reason") or "stop"
        message = first.get("message") or {}
        if isinstance(message, dict):
            content = message.get("content") or message.get("reasoning_content") or ""
        if not content:
            content = first.get("text") or ""

    model_name = raw.get("model") or body.model
    usage = raw.get("usage") or {}
    return {
        "id": raw.get("id") or f"chatcmpl-{uuid4().hex}",
        "object": "chat.completion",
        "created": int(time()),
        "model": model_name,
        "choices": [
            {
                "index": 0,
                "message": {"role": "assistant", "content": content},
                "finish_reason": finish_reason,
            }
        ],
        "usage": {
            "prompt_tokens": usage.get("prompt_tokens", 0),
            "completion_tokens": usage.get("completion_tokens", 0),
            "total_tokens": usage.get("total_tokens", 0),
        },
    }


@router.post("/embeddings", dependencies=[Depends(require_openai_key)])
async def embeddings(request: Request, body: OpenAIEmbeddingRequest):
    settings = request.app.state.settings
    if body.model != settings.embed_model:
        return _openai_error(400, f"Unsupported embedding model '{body.model}', expected '{settings.embed_model}'")

    texts = [body.input] if isinstance(body.input, str) else body.input
    if not texts:
        return _openai_error(400, "'input' must not be empty")

    try:
        if body.knoxx_return_sparse:
            hybrid_vectors = await asyncio.to_thread(request.app.state.embeddings_service.embed_hybrid, texts)
            return {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "embedding": item["embedding"],
                        "sparse": item["sparse"],
                        "index": idx,
                    }
                    for idx, item in enumerate(hybrid_vectors)
                ],
                "model": settings.embed_model,
            }

        vectors = await asyncio.to_thread(request.app.state.embeddings_service.embed, texts)
    except Exception as exc:
        return _openai_error(500, f"Embedding generation failed: {exc}", code="embedding_failed")

    return {
        "object": "list",
        "data": [
            {"object": "embedding", "embedding": vec, "index": idx}
            for idx, vec in enumerate(vectors)
        ],
        "model": settings.embed_model,
    }
