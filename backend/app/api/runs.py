from __future__ import annotations

import io
import json
import zipfile

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse

from app.core.schemas import RunDetail, RunListResponse

router = APIRouter(prefix="/api/runs", tags=["runs"])


@router.get("", response_model=RunListResponse)
async def list_runs(request: Request, limit: int = 100) -> RunListResponse:
    store = request.app.state.run_store
    return RunListResponse(runs=store.list_runs(limit=limit))


@router.get("/{run_id}", response_model=RunDetail)
async def get_run(run_id: str, request: Request) -> RunDetail:
    store = request.app.state.run_store
    run = store.get_run(run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Run not found")
    return run


@router.get("/{run_id}/export")
async def export_run(run_id: str, request: Request) -> StreamingResponse:
    store = request.app.state.run_store
    payload = store.export_run(run_id)
    if payload is None:
        raise HTTPException(status_code=404, detail="Run not found")

    raw = io.BytesIO()
    with zipfile.ZipFile(raw, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("metadata.json", json.dumps(payload.get("run", {}), ensure_ascii=True, indent=2))
        zf.writestr("events.json", json.dumps(payload.get("events", []), ensure_ascii=True, indent=2))
        jsonl = "\n".join(json.dumps(evt, ensure_ascii=True) for evt in payload.get("events", [])) + "\n"
        zf.writestr(f"{run_id}.jsonl", jsonl)
    raw.seek(0)

    headers = {"Content-Disposition": f'attachment; filename="{run_id}.zip"'}
    return StreamingResponse(raw, media_type="application/zip", headers=headers)
