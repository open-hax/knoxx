"""Document ingestion API for RAG - chunk, embed, and store in Qdrant."""

from __future__ import annotations

import hashlib
import re
from pathlib import Path
from typing import Any

import httpx
from fastapi import APIRouter, File, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel

router = APIRouter(prefix="/api/rag", tags=["rag"])

DEFAULT_COLLECTION = "devel_docs"
CHUNK_SIZE = 512
CHUNK_OVERLAP = 64


class IngestTextRequest(BaseModel):
    text: str
    source: str = "manual"
    collection: str = DEFAULT_COLLECTION
    chunk_size: int = CHUNK_SIZE
    chunk_overlap: int = CHUNK_OVERLAP


class ChunkItem(BaseModel):
    id: str
    text: str
    source: str
    metadata: dict[str, Any] = {}


class IngestChunksRequest(BaseModel):
    chunks: list[ChunkItem]
    collection: str = DEFAULT_COLLECTION


class IngestPathRequest(BaseModel):
    path: str
    collection: str = DEFAULT_COLLECTION
    chunk_size: int = CHUNK_SIZE
    chunk_overlap: int = CHUNK_OVERLAP


class SearchRequest(BaseModel):
    query: str
    collection: str = DEFAULT_COLLECTION
    limit: int = 10


class CollectionInfo(BaseModel):
    name: str
    points_count: int
    vectors_count: int


def _get_qdrant_url(request: Request) -> str:
    return request.app.state.settings.qdrant_url if hasattr(request.app.state.settings, "qdrant_url") else "http://qdrant:6333"


def _chunk_text(text: str, chunk_size: int, overlap: int) -> list[str]:
    """Split text into overlapping chunks by sentences/paragraphs."""
    # Split by paragraphs first
    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    
    chunks: list[str] = []
    current_chunk = ""
    
    for para in paragraphs:
        # If paragraph alone exceeds chunk size, split by sentences
        if len(para) > chunk_size:
            sentences = re.split(r"(?<=[.!?])\s+", para)
            for sent in sentences:
                if len(current_chunk) + len(sent) + 1 > chunk_size and current_chunk:
                    chunks.append(current_chunk.strip())
                    # Keep overlap from end of current chunk
                    if overlap > 0:
                        words = current_chunk.split()
                        overlap_text = " ".join(words[-overlap//4:]) if len(words) > overlap//4 else ""
                        current_chunk = overlap_text + " " + sent if overlap_text else sent
                    else:
                        current_chunk = sent
                else:
                    current_chunk = current_chunk + " " + sent if current_chunk else sent
        else:
            if len(current_chunk) + len(para) + 2 > chunk_size and current_chunk:
                chunks.append(current_chunk.strip())
                if overlap > 0:
                    words = current_chunk.split()
                    overlap_text = " ".join(words[-overlap//4:]) if len(words) > overlap//4 else ""
                    current_chunk = overlap_text + "\n\n" + para if overlap_text else para
                else:
                    current_chunk = para
            else:
                current_chunk = current_chunk + "\n\n" + para if current_chunk else para
    
    if current_chunk.strip():
        chunks.append(current_chunk.strip())
    
    return chunks


def _make_id(text: str, source: str, idx: int) -> str:
    """Generate a deterministic UUID for a chunk."""
    import uuid
    h = hashlib.sha256(f"{source}:{idx}:{text[:100]}".encode()).hexdigest()
    # Convert to UUID format (first 32 hex chars)
    return str(uuid.UUID(h[:32]))


async def _ensure_collection(qdrant_url: str, collection: str, dim: int) -> None:
    """Create collection if it doesn't exist."""
    async with httpx.AsyncClient(timeout=10.0) as client:
        # Check if collection exists
        resp = await client.get(f"{qdrant_url}/collections/{collection}")
        if resp.status_code == 200:
            return
        
        # Create collection
        await client.put(
            f"{qdrant_url}/collections/{collection}",
            json={
                "vectors": {
                    "size": dim,
                    "distance": "Cosine",
                },
            },
        )


@router.post("/ingest/text")
async def ingest_text(request: Request, body: IngestTextRequest) -> dict[str, Any]:
    """Ingest text into Qdrant collection."""
    settings = request.app.state.settings
    qdrant_url = _get_qdrant_url(request)
    embeddings_service = request.app.state.embeddings_service
    
    # Chunk the text
    chunks = _chunk_text(body.text, body.chunk_size, body.chunk_overlap)
    if not chunks:
        return {"status": "no_content", "chunks": 0}
    
    # Ensure collection exists
    await _ensure_collection(qdrant_url, body.collection, settings.embed_dim)
    
    # Generate embeddings
    try:
        vectors = await asyncio.to_thread(embeddings_service.embed, chunks)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Embedding failed: {exc}")
    
    # Build points
    points = []
    for idx, (chunk, vector) in enumerate(zip(chunks, vectors)):
        point_id = _make_id(chunk, body.source, idx)
        points.append({
            "id": point_id,
            "vector": vector,
            "payload": {
                "text": chunk,
                "source": body.source,
                "chunk_index": idx,
                "total_chunks": len(chunks),
            },
        })
    
    # Upsert to Qdrant
    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.put(
            f"{qdrant_url}/collections/{body.collection}/points",
            json={"points": points},
        )
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"Qdrant error: {resp.text}")
    
    return {
        "status": "success",
        "chunks": len(chunks),
        "collection": body.collection,
        "source": body.source,
    }


@router.post("/ingest/chunks")
async def ingest_chunks(request: Request, body: IngestChunksRequest) -> dict[str, Any]:
    """Ingest pre-chunked content into Qdrant collection."""
    settings = request.app.state.settings
    qdrant_url = _get_qdrant_url(request)
    embeddings_service = request.app.state.embeddings_service
    
    if not body.chunks:
        return {"status": "no_content", "chunks": 0}
    
    # Ensure collection exists
    await _ensure_collection(qdrant_url, body.collection, settings.embed_dim)
    
    # Extract texts for embedding
    texts = [c.text for c in body.chunks]
    
    # Generate embeddings
    try:
        vectors = await asyncio.to_thread(embeddings_service.embed, texts)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Embedding failed: {exc}")
    
    # Build points
    points = []
    for chunk, vector in zip(body.chunks, vectors):
        points.append({
            "id": chunk.id,
            "vector": vector,
            "payload": {
                "text": chunk.text,
                "source": chunk.source,
                **chunk.metadata,
            },
        })
    
    # Upsert to Qdrant
    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.put(
            f"{qdrant_url}/collections/{body.collection}/points",
            json={"points": points},
        )
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"Qdrant error: {resp.text}")
    
    return {
        "status": "success",
        "chunks": len(body.chunks),
        "collection": body.collection,
    }


@router.post("/ingest/file")
async def ingest_file(
    request: Request,
    file: UploadFile = File(...),
    collection: str = DEFAULT_COLLECTION,
    chunk_size: int = CHUNK_SIZE,
    chunk_overlap: int = CHUNK_OVERLAP,
) -> dict[str, Any]:
    """Ingest an uploaded file into Qdrant collection."""
    content = await file.read()
    
    # Try to decode as text
    try:
        text = content.decode("utf-8")
    except UnicodeDecodeError:
        raise HTTPException(status_code=400, detail="File must be UTF-8 text")
    
    return await ingest_text(
        request,
        IngestTextRequest(
            text=text,
            source=file.filename or "upload",
            collection=collection,
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
        ),
    )


@router.post("/search")
async def search(request: Request, body: SearchRequest) -> dict[str, Any]:
    """Search for similar documents in Qdrant collection."""
    settings = request.app.state.settings
    qdrant_url = _get_qdrant_url(request)
    embeddings_service = request.app.state.embeddings_service
    
    # Generate query embedding
    try:
        vectors = await asyncio.to_thread(embeddings_service.embed, [body.query])
        query_vector = vectors[0]
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Embedding failed: {exc}")
    
    # Search Qdrant
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.post(
            f"{qdrant_url}/collections/{body.collection}/points/search",
            json={
                "vector": query_vector,
                "limit": body.limit,
                "with_payload": True,
            },
        )
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"Qdrant error: {resp.text}")
        
        results = resp.json().get("result", [])
    
    return {
        "query": body.query,
        "collection": body.collection,
        "results": [
            {
                "id": r.get("id"),
                "score": r.get("score"),
                "payload": r.get("payload", {}),
            }
            for r in results
        ],
    }


@router.get("/collections")
async def list_collections(request: Request) -> list[CollectionInfo]:
    """List all Qdrant collections."""
    qdrant_url = _get_qdrant_url(request)
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.get(f"{qdrant_url}/collections")
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"Qdrant error: {resp.text}")
        
        collections = resp.json().get("result", {}).get("collections", [])
        
        result = []
        for c in collections:
            name = c.get("name", "")
            # Get collection info
            info_resp = await client.get(f"{qdrant_url}/collections/{name}")
            if info_resp.status_code == 200:
                info = info_resp.json().get("result", {})
                result.append(CollectionInfo(
                    name=name,
                    points_count=info.get("points_count", 0),
                    vectors_count=info.get("vectors_count", 0),
                ))
        
        return result


@router.delete("/collections/{collection}")
async def delete_collection(request: Request, collection: str) -> dict[str, str]:
    """Delete a Qdrant collection."""
    qdrant_url = _get_qdrant_url(request)
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.delete(f"{qdrant_url}/collections/{collection}")
        if resp.status_code >= 400:
            raise HTTPException(status_code=502, detail=f"Qdrant error: {resp.text}")
    
    return {"status": "deleted", "collection": collection}


# Import asyncio at module level
import asyncio
