from __future__ import annotations

import asyncio
from typing import Any

import httpx
from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import (
    ProxxChatRequest,
    ProxxChatResponse,
    ProxxHealthResponse,
    ProxxModelInfo,
    ProxxModelListResponse,
)

router = APIRouter(prefix="/api/proxx", tags=["proxx"])


def _configured(settings) -> bool:
    return bool(settings.proxx_base_url and settings.proxx_auth_token)


def _headers(settings) -> dict[str, str]:
    return {
        "Authorization": f"Bearer {settings.proxx_auth_token}",
        "Content-Type": "application/json",
    }


def _extract_content(data: dict[str, Any]) -> str:
    choices = data.get("choices") or []
    if not choices:
        return ""

    first = choices[0] or {}
    message = first.get("message") or {}
    if isinstance(message, dict):
        content = message.get("content")
        if isinstance(content, str) and content:
            return content
        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, dict) and item.get("type") == "text":
                    text = item.get("text")
                    if isinstance(text, str) and text:
                        parts.append(text)
            if parts:
                return "".join(parts)
        reasoning = message.get("reasoning") or message.get("reasoning_content")
        if isinstance(reasoning, str) and reasoning:
            return reasoning

    text = first.get("text")
    return text if isinstance(text, str) else ""


async def _rag_search(
    query: str,
    collection: str,
    limit: int,
    threshold: float,
    embeddings_service,
    qdrant_url: str,
) -> list[dict[str, Any]]:
    """Search RAG collection and return results above threshold."""
    # Generate query embedding
    try:
        vectors = await asyncio.to_thread(embeddings_service.embed, [query])
        query_vector = vectors[0]
    except Exception:
        return []

    # Search Qdrant
    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            resp = await client.post(
                f"{qdrant_url}/collections/{collection}/points/search",
                json={
                    "vector": query_vector,
                    "limit": limit,
                    "with_payload": True,
                },
            )
            if resp.status_code >= 400:
                return []
            results = resp.json().get("result", [])
        except Exception:
            return []

    # Filter by threshold
    filtered = []
    for r in results:
        score = r.get("score", 0)
        if score >= threshold:
            payload = r.get("payload", {})
            filtered.append({
                "score": score,
                "text": payload.get("text", ""),
                "source": payload.get("source", ""),
            })
    
    return filtered


def _format_rag_context(results: list[dict[str, Any]]) -> str:
    """Format RAG results into context string."""
    if not results:
        return ""
    
    lines = ["Relevant context from knowledge base:"]
    for i, r in enumerate(results, 1):
        source = r.get("source", "unknown")
        text = r.get("text", "")
        # Truncate long texts
        if len(text) > 500:
            text = text[:500] + "..."
        lines.append(f"\n[{i}] Source: {source}")
        lines.append(f"Score: {r.get('score', 0):.2f}")
        lines.append(f"Content: {text}")
    
    return "\n".join(lines)


@router.get("/health", response_model=ProxxHealthResponse)
async def proxx_health(request: Request) -> ProxxHealthResponse:
    settings = request.app.state.settings
    configured = _configured(settings)
    if not configured:
        return ProxxHealthResponse(
            reachable=False,
            configured=False,
            base_url=settings.proxx_base_url or "",
            default_model=settings.proxx_default_model,
        )

    timeout = httpx.Timeout(5.0)
    status_code: int | None = None
    model_count: int | None = None

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            health_resp = await client.get(f"{settings.proxx_base_url}/health")
            status_code = health_resp.status_code
            models_resp = await client.get(f"{settings.proxx_base_url}/v1/models", headers=_headers(settings))
            models_resp.raise_for_status()
            models_payload = models_resp.json()
            model_count = len(models_payload.get("data") or [])
            reachable = health_resp.status_code < 500
    except Exception:
        reachable = False

    return ProxxHealthResponse(
        reachable=reachable,
        configured=True,
        base_url=settings.proxx_base_url or "",
        status_code=status_code,
        model_count=model_count,
        default_model=settings.proxx_default_model,
    )


@router.get("/models", response_model=ProxxModelListResponse)
async def list_proxx_models(request: Request) -> ProxxModelListResponse:
    settings = request.app.state.settings
    if not _configured(settings):
        raise HTTPException(status_code=503, detail="Proxx is not configured")

    timeout = httpx.Timeout(15.0)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.get(f"{settings.proxx_base_url}/v1/models", headers=_headers(settings))
            resp.raise_for_status()
            payload = resp.json()
    except httpx.HTTPStatusError as exc:
        detail = exc.response.text[:500] if exc.response is not None else str(exc)
        raise HTTPException(status_code=502, detail=f"Proxx model list failed: {detail}") from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Proxx is unreachable: {exc}") from exc

    items = payload.get("data") or []
    models = [
        ProxxModelInfo(
            id=str(item.get("id", "")),
            name=str(item.get("id", "")),
            owned_by=item.get("owned_by"),
        )
        for item in items
        if item.get("id")
    ]
    return ProxxModelListResponse(models=models)


@router.post("/chat", response_model=ProxxChatResponse)
async def proxx_chat(request: Request, body: ProxxChatRequest) -> ProxxChatResponse:
    settings = request.app.state.settings
    if not _configured(settings):
        raise HTTPException(status_code=503, detail="Proxx is not configured")

    messages = [msg.model_dump(exclude_none=True) for msg in body.messages]
    
    # RAG context retrieval
    rag_results: list[dict[str, Any]] = []
    rag_context_str = ""
    
    if body.rag_enabled:
        # Get the last user message as the query
        last_user_msg = None
        for msg in reversed(messages):
            if msg.get("role") == "user":
                last_user_msg = msg.get("content", "")
                break
        
        if last_user_msg:
            rag_results = await _rag_search(
                query=last_user_msg,
                collection=body.rag_collection,
                limit=body.rag_limit,
                threshold=body.rag_threshold,
                embeddings_service=request.app.state.embeddings_service,
                qdrant_url=settings.qdrant_url,
            )
            
            if rag_results:
                rag_context_str = _format_rag_context(rag_results)
    
    # Build system prompt with RAG context
    system_prompt = body.system_prompt or ""
    if rag_context_str:
        if system_prompt:
            system_prompt = f"{system_prompt}\n\n{rag_context_str}"
        else:
            system_prompt = f"You have access to relevant context from a knowledge base. Use this context to inform your response.\n\n{rag_context_str}"
    
    if system_prompt and not any(message.get("role") == "system" for message in messages):
        messages.insert(0, {"role": "system", "content": system_prompt})

    payload: dict[str, Any] = {
        "model": body.model or settings.proxx_default_model,
        "messages": messages,
        "temperature": body.temperature,
        "top_p": body.top_p,
        "max_tokens": body.max_tokens,
        "stop": body.stop,
        "stream": False,
    }
    payload = {key: value for key, value in payload.items() if value is not None}

    timeout = httpx.Timeout(request.app.state.settings.local_request_timeout_seconds)
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(
                f"{settings.proxx_base_url}/v1/chat/completions",
                json=payload,
                headers=_headers(settings),
            )
            resp.raise_for_status()
            data = resp.json()
    except httpx.HTTPStatusError as exc:
        detail = exc.response.text[:500] if exc.response is not None else str(exc)
        raise HTTPException(status_code=502, detail=f"Proxx chat failed: {detail}") from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Proxx is unreachable: {exc}") from exc

    return ProxxChatResponse(
        answer=_extract_content(data),
        model=data.get("model") or payload.get("model"),
        rag_context=rag_results if body.rag_enabled else None,
    )
