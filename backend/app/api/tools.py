from __future__ import annotations

import asyncio
import smtplib
from email.message import EmailMessage
from pathlib import Path

from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import (
    EmailSendRequest,
    EmailSendResponse,
    ToolBashRequest,
    ToolBashResponse,
    ToolCatalogResponse,
    ToolDefinition,
    ToolEditRequest,
    ToolEditResponse,
    ToolReadRequest,
    ToolReadResponse,
    ToolWriteRequest,
    ToolWriteResponse,
)

router = APIRouter(prefix="/api/tools", tags=["tools"])

ROLE_TOOLS: dict[str, list[tuple[str, str, str]]] = {
    "executive": [
        ("read", "Read", "Read files and retrieved context"),
        ("write", "Write", "Create new markdown drafts and artifacts"),
        ("edit", "Edit", "Revise existing documents and drafts"),
        ("bash", "Shell", "Run controlled shell commands"),
        ("canvas", "Canvas", "Open long-form markdown drafting canvas"),
        ("email.send", "Email", "Send drafts through configured email account"),
        ("discord.publish", "Discord", "Publish updates to Discord"),
        ("bluesky.publish", "Bluesky", "Publish updates to Bluesky"),
    ],
    "principal_architect": [
        ("read", "Read", "Read files and retrieved context"),
        ("write", "Write", "Create new markdown drafts and artifacts"),
        ("edit", "Edit", "Revise existing documents and drafts"),
        ("bash", "Shell", "Run controlled shell commands"),
        ("canvas", "Canvas", "Open long-form markdown drafting canvas"),
    ],
    "junior_dev": [
        ("read", "Read", "Read files and retrieved context"),
        ("write", "Write", "Create new markdown drafts and notes"),
        ("canvas", "Canvas", "Open long-form markdown drafting canvas"),
    ],
}


def _normalize_role(role: str) -> str:
    return role if role in ROLE_TOOLS else "junior_dev"


def _tool_catalog(role: str, email_enabled: bool) -> ToolCatalogResponse:
    normalized_role = _normalize_role(role)
    tools = [
        ToolDefinition(
            id=tool_id,
            label=label,
            description=description,
            enabled=email_enabled if tool_id == "email.send" else True,
        )
        for tool_id, label, description in ROLE_TOOLS[normalized_role]
    ]
    return ToolCatalogResponse(
        role=normalized_role, tools=tools, email_enabled=email_enabled
    )


def _ensure_role_can_use(role: str, tool_id: str) -> str:
    normalized_role = _normalize_role(role)
    allowed_tools = {
        current_tool_id
        for current_tool_id, _label, _description in ROLE_TOOLS[normalized_role]
    }
    if tool_id not in allowed_tools:
        raise HTTPException(
            status_code=403,
            detail=f"Role '{normalized_role}' cannot use tool '{tool_id}'",
        )
    return normalized_role


def _ensure_role_can_send(role: str) -> str:
    return _ensure_role_can_use(role, "email.send")


def _workspace_root(request: Request) -> Path:
    return request.app.state.settings.workspace_root.resolve()


def _resolve_workspace_path(request: Request, raw_path: str) -> Path:
    workspace_root = _workspace_root(request)
    candidate = Path(raw_path)
    resolved = (
        candidate.resolve()
        if candidate.is_absolute()
        else (workspace_root / candidate).resolve()
    )
    if not resolved.is_relative_to(workspace_root):
        raise HTTPException(status_code=403, detail="Path escapes workspace root")
    return resolved


def _safe_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError as exc:
        raise HTTPException(
            status_code=400, detail=f"File is not UTF-8 text: {path}"
        ) from exc


def _clip(text: str, limit: int = 20000) -> tuple[str, bool]:
    if len(text) <= limit:
        return text, False
    return text[:limit], True


@router.get("/catalog", response_model=ToolCatalogResponse)
async def tools_catalog(
    request: Request, role: str | None = None
) -> ToolCatalogResponse:
    settings = request.app.state.settings
    requested_role = role or settings.knoxx_default_role
    return _tool_catalog(
        requested_role, bool(settings.gmail_app_email and settings.gmail_app_password)
    )


@router.post("/email/send", response_model=EmailSendResponse)
async def send_email(request: Request, body: EmailSendRequest) -> EmailSendResponse:
    settings = request.app.state.settings
    normalized_role = _ensure_role_can_send(body.role)

    if not settings.gmail_app_email or not settings.gmail_app_password:
        raise HTTPException(
            status_code=503, detail="Gmail app credentials are not configured"
        )
    if not body.to:
        raise HTTPException(
            status_code=400, detail="At least one recipient is required"
        )

    message = EmailMessage()
    message["From"] = settings.gmail_app_email
    message["To"] = ", ".join(body.to)
    if body.cc:
        message["Cc"] = ", ".join(body.cc)
    message["Subject"] = body.subject
    message.set_content(body.markdown)

    all_recipients = [*body.to, *body.cc, *body.bcc]

    try:
        with smtplib.SMTP("smtp.gmail.com", 587, timeout=30) as server:
            server.starttls()
            server.login(settings.gmail_app_email, settings.gmail_app_password)
            server.send_message(
                message, from_addr=settings.gmail_app_email, to_addrs=all_recipients
            )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"Email send failed: {exc}"
        ) from exc

    return EmailSendResponse(
        ok=True, role=normalized_role, sent_to=all_recipients, subject=body.subject
    )


@router.post("/read", response_model=ToolReadResponse)
async def tool_read(request: Request, body: ToolReadRequest) -> ToolReadResponse:
    normalized_role = _ensure_role_can_use(body.role, "read")
    path = _resolve_workspace_path(request, body.path)
    if not path.exists():
        raise HTTPException(status_code=404, detail=f"Path not found: {body.path}")

    if path.is_dir():
        entries = sorted(
            child.name + ("/" if child.is_dir() else "") for child in path.iterdir()
        )
        content, truncated = _clip("\n".join(entries))
        return ToolReadResponse(
            ok=True,
            role=normalized_role,
            path=str(path),
            content=content,
            truncated=truncated,
        )

    lines = _safe_text(path).splitlines()
    start = max(body.offset - 1, 0)
    stop = start + max(body.limit, 1)
    numbered = [
        f"{index + 1}: {line}"
        for index, line in enumerate(lines[start:stop], start=start)
    ]
    content, truncated = _clip("\n".join(numbered))
    return ToolReadResponse(
        ok=True,
        role=normalized_role,
        path=str(path),
        content=content,
        truncated=truncated or stop < len(lines),
    )


@router.post("/write", response_model=ToolWriteResponse)
async def tool_write(request: Request, body: ToolWriteRequest) -> ToolWriteResponse:
    normalized_role = _ensure_role_can_use(body.role, "write")
    path = _resolve_workspace_path(request, body.path)
    if path.exists() and not body.overwrite:
        raise HTTPException(
            status_code=409, detail=f"File exists and overwrite is false: {body.path}"
        )
    if body.create_parents:
        path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body.content, encoding="utf-8")
    return ToolWriteResponse(
        ok=True,
        role=normalized_role,
        path=str(path),
        bytes_written=len(body.content.encode("utf-8")),
    )


@router.post("/edit", response_model=ToolEditResponse)
async def tool_edit(request: Request, body: ToolEditRequest) -> ToolEditResponse:
    normalized_role = _ensure_role_can_use(body.role, "edit")
    path = _resolve_workspace_path(request, body.path)
    if not path.exists() or path.is_dir():
        raise HTTPException(status_code=404, detail=f"File not found: {body.path}")
    current = _safe_text(path)
    if body.old_string not in current:
        raise HTTPException(status_code=409, detail="old_string not found in file")
    replacements = current.count(body.old_string) if body.replace_all else 1
    updated = (
        current.replace(body.old_string, body.new_string)
        if body.replace_all
        else current.replace(body.old_string, body.new_string, 1)
    )
    path.write_text(updated, encoding="utf-8")
    return ToolEditResponse(
        ok=True, role=normalized_role, path=str(path), replacements=replacements
    )


@router.post("/bash", response_model=ToolBashResponse)
async def tool_bash(request: Request, body: ToolBashRequest) -> ToolBashResponse:
    normalized_role = _ensure_role_can_use(body.role, "bash")
    workspace_root = _workspace_root(request)
    workdir = (
        _resolve_workspace_path(request, body.workdir)
        if body.workdir
        else workspace_root
    )
    timeout_seconds = min(max(body.timeout_ms, 1000), 300000) / 1000

    process = await asyncio.create_subprocess_shell(
        body.command,
        cwd=str(workdir),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    try:
        stdout_bytes, stderr_bytes = await asyncio.wait_for(
            process.communicate(), timeout=timeout_seconds
        )
    except asyncio.TimeoutError as exc:
        process.kill()
        await process.wait()
        raise HTTPException(
            status_code=408, detail=f"Command timed out after {timeout_seconds:.0f}s"
        ) from exc

    stdout, _ = _clip(stdout_bytes.decode("utf-8", errors="replace"), 24000)
    stderr, _ = _clip(stderr_bytes.decode("utf-8", errors="replace"), 12000)
    return ToolBashResponse(
        ok=process.returncode == 0,
        role=normalized_role,
        command=body.command,
        exit_code=process.returncode or 0,
        stdout=stdout,
        stderr=stderr,
    )
