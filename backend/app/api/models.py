from __future__ import annotations

from fastapi import APIRouter, Request

from app.core.schemas import ModelListResponse

router = APIRouter(prefix="/api", tags=["models"])


@router.get("/models", response_model=ModelListResponse)
async def list_models(request: Request) -> ModelListResponse:
    models = request.app.state.model_discovery.scan()
    return ModelListResponse(models=models)
