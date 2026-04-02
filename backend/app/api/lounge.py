from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request

from app.core.schemas import LoungeListResponse, LoungePostRequest

router = APIRouter(prefix="/api/lounge", tags=["lounge"])


@router.get("/messages", response_model=LoungeListResponse)
async def list_lounge_messages(request: Request) -> LoungeListResponse:
    lounge = request.app.state.lounge_service
    return LoungeListResponse(messages=lounge.list_messages())


@router.post("/messages")
async def post_lounge_message(body: LoungePostRequest, request: Request) -> dict[str, object]:
    lounge = request.app.state.lounge_service
    event_bus = request.app.state.event_bus

    try:
        msg = lounge.post_message(session_id=body.session_id, text=body.text, alias=body.alias)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    await event_bus.publish("lounge", msg.model_dump(mode="json"))
    return {"ok": True, "message": msg.model_dump(mode="json")}
