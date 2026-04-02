from __future__ import annotations

from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

import httpx
from fastapi import APIRouter, HTTPException, Request

from app.api.config import _rewrite_localhost_url
from app.core.schemas import ShibbolethHandoffRequest, ShibbolethHandoffResponse

router = APIRouter(prefix="/api/shibboleth", tags=["shibboleth"])


def _with_query_param(url: str, key: str, value: str) -> str:
    parsed = urlsplit(url)
    query = dict(parse_qsl(parsed.query, keep_blank_values=True))
    query[key] = value
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, urlencode(query), parsed.fragment))


@router.post("/handoff", response_model=ShibbolethHandoffResponse)
async def handoff_to_shibboleth(request: Request, body: ShibbolethHandoffRequest) -> ShibbolethHandoffResponse:
    settings = request.app.state.settings
    if not settings.shibboleth_base_url:
        raise HTTPException(status_code=503, detail="SHIBBOLETH_BASE_URL is not configured")

    payload = {
        "source_app": "knoxx",
        "model": body.model,
        "system_prompt": body.system_prompt,
        "provider": body.provider,
        "conversation_id": body.conversation_id,
        "fake_tools_enabled": body.fake_tools_enabled,
        "items": [item.model_dump(mode="json") for item in body.items],
    }

    timeout = httpx.Timeout(30.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        try:
            response = await client.post(f"{settings.shibboleth_base_url}/api/chat/import", json=payload)
            response.raise_for_status()
            data = response.json()
        except httpx.HTTPStatusError as exc:
            detail = exc.response.text[:500] if exc.response is not None else str(exc)
            raise HTTPException(status_code=502, detail=f"Shibboleth import failed: {detail}") from exc
        except Exception as exc:
            raise HTTPException(status_code=502, detail=f"Shibboleth is unreachable: {exc}") from exc

    session = data.get("session") or {}
    session_id = str(session.get("id") or "").strip()
    if not session_id:
        raise HTTPException(status_code=502, detail="Shibboleth import did not return a session id")

    ui_url = ""
    if settings.shibboleth_ui_url:
        ui_url = _with_query_param(_rewrite_localhost_url(settings.shibboleth_ui_url, request), "session", session_id)

    return ShibbolethHandoffResponse(
        ok=True,
        session_id=session_id,
        ui_url=ui_url,
        imported_item_count=len(body.items),
    )
