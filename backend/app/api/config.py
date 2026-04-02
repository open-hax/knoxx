from __future__ import annotations

from urllib.parse import urlsplit, urlunsplit

from fastapi import APIRouter, Request

from app.core.schemas import FrontendConfigResponse

router = APIRouter(prefix="/api", tags=["config"])


def _request_hostname(request: Request) -> str | None:
    forwarded_host = request.headers.get("x-forwarded-host")
    raw_host = (forwarded_host or request.headers.get("host") or "").split(",")[0].strip()
    if not raw_host:
        return request.url.hostname
    parsed = urlsplit(f"http://{raw_host}")
    return parsed.hostname or request.url.hostname


def _request_scheme(request: Request) -> str:
    forwarded_proto = request.headers.get("x-forwarded-proto")
    if forwarded_proto:
        return forwarded_proto.split(",")[0].strip()
    return request.url.scheme or "http"


def _rewrite_localhost_url(url: str, request: Request) -> str:
    try:
        parsed = urlsplit(url)
    except Exception:
        return url

    if parsed.scheme not in {"http", "https"}:
        return url

    local_hosts = {"localhost", "127.0.0.1", "::1"}
    if parsed.hostname not in local_hosts:
        return url

    req_host = _request_hostname(request)
    if not req_host:
        return url

    netloc = req_host
    if parsed.port is not None:
        netloc = f"{req_host}:{parsed.port}"

    scheme = _request_scheme(request)
    return urlunsplit((scheme, netloc, parsed.path, parsed.query, parsed.fragment))


@router.get("/config", response_model=FrontendConfigResponse)
async def frontend_config(request: Request) -> FrontendConfigResponse:
    settings = request.app.state.settings
    knoxx_admin_url = _rewrite_localhost_url(settings.knoxx_admin_url, request)
    knoxx_base_url = _rewrite_localhost_url(settings.knoxx_base_url, request)
    shibboleth_ui_url = _rewrite_localhost_url(settings.shibboleth_ui_url, request) if settings.shibboleth_ui_url else ""
    return FrontendConfigResponse(
        knoxx_admin_url=knoxx_admin_url,
        knoxx_base_url=knoxx_base_url,
        knoxx_enabled=bool(settings.knoxx_api_key),
        proxx_enabled=bool(settings.proxx_base_url and settings.proxx_auth_token),
        proxx_default_model=settings.proxx_default_model,
        shibboleth_ui_url=shibboleth_ui_url,
        shibboleth_enabled=bool(settings.shibboleth_base_url and settings.shibboleth_ui_url),
    )
