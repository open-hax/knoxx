from __future__ import annotations

import httpx
from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import (
    KnoxxChatRequest,
    KnoxxChatResponse,
    KnoxxHealthResponse,
)

router = APIRouter(prefix="/api/knoxx", tags=["knoxx"])


@router.get("/health", response_model=KnoxxHealthResponse)
async def knoxx_health(request: Request) -> KnoxxHealthResponse:
    settings = request.app.state.settings
    configured = bool(settings.knoxx_api_key)
    if not configured:
        return KnoxxHealthResponse(
            reachable=False,
            configured=False,
            base_url=settings.knoxx_base_url,
        )

    status_code: int | None = None
    details: dict | None = None
    reachable = False

    timeout = httpx.Timeout(5.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        try:
            resp = await client.get(f"{settings.knoxx_base_url}/health")
            status_code = resp.status_code
            details = (
                resp.json()
                if resp.headers.get("content-type", "").startswith("application/json")
                else None
            )
            reachable = resp.status_code < 500
        except Exception:
            reachable = False

    return KnoxxHealthResponse(
        reachable=reachable,
        configured=True,
        base_url=settings.knoxx_base_url,
        status_code=status_code,
        details=details,
    )


@router.post("/chat", response_model=KnoxxChatResponse)
async def knoxx_chat(
    request: Request, body: KnoxxChatRequest
) -> KnoxxChatResponse:
    return await _proxy_chat(request, body, direct=False)


@router.post("/direct", response_model=KnoxxChatResponse)
async def knoxx_direct(
    request: Request, body: KnoxxChatRequest
) -> KnoxxChatResponse:
    return await _proxy_chat(request, body, direct=True)


async def _proxy_chat(
    request: Request, body: KnoxxChatRequest, direct: bool
) -> KnoxxChatResponse:
    settings = request.app.state.settings
    if not settings.knoxx_api_key:
        raise HTTPException(status_code=503, detail="KNOXX_API_KEY is not configured")

    path = "/api/llm/chat" if direct else "/api/chat"
    payload = {
        "message": body.message,
        "conversationId": body.conversation_id,
    }
    timeout = httpx.Timeout(settings.local_request_timeout_seconds)

    async with httpx.AsyncClient(timeout=timeout) as client:
        try:
            resp = await client.post(
                f"{settings.knoxx_base_url}{path}",
                json=payload,
                headers={
                    "x-api-key": settings.knoxx_api_key,
                    "Content-Type": "application/json",
                },
            )
            resp.raise_for_status()
            data = resp.json()
        except httpx.HTTPStatusError as exc:
            detail = exc.response.text[:500] if exc.response is not None else str(exc)
            raise HTTPException(
                status_code=502, detail=f"Knoxx request failed: {detail}"
            ) from exc
        except Exception as exc:
            raise HTTPException(
                status_code=502, detail=f"Knoxx is unreachable: {exc}"
            ) from exc

    return KnoxxChatResponse(
        answer=str(data.get("answer", "")),
        conversation_id=data.get("conversationId"),
        message_parts=data.get("message_parts") or data.get("messageParts"),
    )


from fastapi.responses import StreamingResponse


@router.api_route(
    "/proxy/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"]
)
async def knoxx_proxy(path: str, request: Request):
    settings = request.app.state.settings
    if not settings.knoxx_api_key:
        raise HTTPException(status_code=503, detail="KNOXX_API_KEY is not configured")

    target_url = f"{settings.knoxx_base_url}/api/{path}"
    headers = dict(request.headers)
    headers.pop("host", None)
    headers["x-api-key"] = settings.knoxx_api_key

    # Ensure content-length is dropped if it's chunked, otherwise httpx complains.
    # Actually, httpx is fine with content=request.stream()

    timeout = httpx.Timeout(60.0)
    client = httpx.AsyncClient(timeout=timeout)

    req = client.build_request(
        request.method,
        target_url,
        headers=headers,
        params=request.query_params,
        content=request.stream(),
    )

    try:
        resp = await client.send(req, stream=True)

        # Function to ensure we close the client after streaming
        async def stream_wrapper():
            async for chunk in resp.aiter_raw():
                yield chunk
            await client.aclose()

        return StreamingResponse(
            stream_wrapper(),
            status_code=resp.status_code,
            headers={
                k: v
                for k, v in resp.headers.items()
                if k.lower()
                not in ("transfer-encoding", "content-encoding", "content-length")
            },
        )
    except httpx.RequestError as exc:
        await client.aclose()
        raise HTTPException(status_code=502, detail=f"Proxy request failed: {exc}")
