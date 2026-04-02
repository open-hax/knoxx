from __future__ import annotations

from typing import Any

import httpx

from app.core.config import Settings


class EmbeddingsService:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._model: Any | None = None
        self._st_model: Any | None = None

    def _ensure_model(self) -> Any:
        if self._model is not None:
            return self._model

        try:
            from FlagEmbedding import BGEM3FlagModel
        except ImportError as exc:
            raise RuntimeError(
                "FlagEmbedding is not installed. Install with: pip install FlagEmbedding sentence-transformers"
            ) from exc

        self._model = BGEM3FlagModel("BAAI/bge-m3")
        return self._model

    def _ensure_sentence_transformer(self) -> Any:
        if self._st_model is not None:
            return self._st_model

        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise RuntimeError(
                "sentence-transformers is not installed. Install with: pip install sentence-transformers"
            ) from exc

        self._st_model = SentenceTransformer("BAAI/bge-m3")
        return self._st_model

    def _fallback_sparse(self, text: str) -> dict[str, list[float] | list[int]]:
        weights: dict[int, float] = {}
        tokens = [t for t in "".join(c.lower() if c.isalnum() else " " for c in text).split() if t]
        for token in tokens[:4096]:
            h = 2166136261
            for ch in token:
                h ^= ord(ch)
                h = (h * 16777619) & 0xFFFFFFFF
            idx = int(h % 250000)
            weights[idx] = weights.get(idx, 0.0) + 1.0

        items = sorted((i, float(v)) for i, v in weights.items())
        return {
            "indices": [i for i, _ in items],
            "values": [v for _, v in items],
        }

    def _embed_proxx(self, texts: list[str]) -> list[list[float]]:
        """Generate embeddings using Proxx /api/embed endpoint (Ollama-backed)."""
        proxx_url = self._settings.proxx_base_url
        auth_token = self._settings.proxx_auth_token
        
        if not proxx_url or not auth_token:
            raise RuntimeError("PROXX_BASE_URL and PROXX_AUTH_TOKEN must be configured for proxx embed mode")

        model = self._settings.proxx_embed_model
        vectors: list[list[float]] = []

        timeout = httpx.Timeout(60.0)
        
        with httpx.Client(timeout=timeout) as client:
            # Proxx supports batch embeddings via /api/embed
            resp = client.post(
                f"{proxx_url}/api/embed",
                json={"model": model, "input": texts},
                headers={
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {auth_token}",
                },
            )
            resp.raise_for_status()
            data = resp.json()
            embeddings = data.get("embeddings", [])
            
            for emb in embeddings:
                vectors.append([float(v) for v in emb])

        return vectors

    def embed(self, texts: list[str]) -> list[list[float]]:
        mode = self._settings.embed_mode.lower()

        if mode == "proxx":
            return self._embed_proxx(texts)

        if mode != "local":
            raise RuntimeError(f"Unsupported EMBED_MODE: {mode}")

        dense_rows: list[Any]
        try:
            model = self._ensure_model()
            try:
                encoded = model.encode(
                    texts,
                    return_dense=True,
                    return_sparse=False,
                    return_colbert=False,
                )
            except TypeError:
                encoded = model.encode(
                    texts,
                    return_dense=True,
                    return_sparse=False,
                )

            dense = encoded.get("dense_vecs") if isinstance(encoded, dict) else encoded
            if dense is None:
                raise RuntimeError("Embedding model returned no dense vectors")
            if hasattr(dense, "tolist"):
                dense = dense.tolist()
            dense_rows = list(dense)
        except Exception:
            # Robust fallback: sentence-transformers dense embeddings only
            st_model = self._ensure_sentence_transformer()
            dense = st_model.encode(texts, normalize_embeddings=True)
            if hasattr(dense, "tolist"):
                dense = dense.tolist()
            dense_rows = list(dense)

        vectors: list[list[float]] = []
        for vec in dense_rows:
            values = [float(v) for v in vec]
            if len(values) != self._settings.embed_dim:
                raise RuntimeError(
                    f"Embedding dimension mismatch: got {len(values)}, expected {self._settings.embed_dim}"
                )
            vectors.append(values)

        return vectors

    def embed_hybrid(self, texts: list[str]) -> list[dict[str, object]]:
        mode = self._settings.embed_mode.lower()

        if mode == "proxx":
            # Proxx/Ollama only supports dense embeddings
            dense_vectors = self._embed_proxx(texts)
            output: list[dict[str, object]] = []
            for idx, emb in enumerate(dense_vectors):
                sparse = self._fallback_sparse(texts[idx])
                output.append({
                    "embedding": emb,
                    "sparse": sparse,
                })
            return output

        if mode != "local":
            raise RuntimeError(f"Unsupported EMBED_MODE: {mode}")

        dense_rows: list[Any]
        lexical_rows: list[Any]
        try:
            model = self._ensure_model()
            try:
                encoded = model.encode(
                    texts,
                    return_dense=True,
                    return_sparse=True,
                    return_colbert=False,
                )
            except TypeError:
                encoded = model.encode(
                    texts,
                    return_dense=True,
                    return_sparse=True,
                )

            dense = encoded.get("dense_vecs") if isinstance(encoded, dict) else encoded
            lexical_weights = encoded.get("lexical_weights") if isinstance(encoded, dict) else None

            if dense is None:
                raise RuntimeError("Embedding model returned no dense vectors")

            if hasattr(dense, "tolist"):
                dense = dense.tolist()
            dense_rows = list(dense)

            if lexical_weights is None:
                lexical_weights = [{} for _ in texts]
            lexical_rows = list(lexical_weights)
        except Exception:
            st_model = self._ensure_sentence_transformer()
            dense = st_model.encode(texts, normalize_embeddings=True)
            if hasattr(dense, "tolist"):
                dense = dense.tolist()
            dense_rows = list(dense)
            lexical_rows = [self._fallback_sparse(t) for t in texts]

        output: list[dict[str, object]] = []
        for idx, vec in enumerate(dense_rows):
            values = [float(v) for v in vec]
            if len(values) != self._settings.embed_dim:
                raise RuntimeError(
                    f"Embedding dimension mismatch: got {len(values)}, expected {self._settings.embed_dim}"
                )

            lex = lexical_rows[idx] if idx < len(lexical_rows) else {}
            sparse_pairs: list[tuple[int, float]] = []
            if isinstance(lex, dict):
                for k, v in lex.items():
                    try:
                        sparse_pairs.append((int(k), float(v)))
                    except Exception:
                        continue

            sparse_pairs = [(i, w) for i, w in sparse_pairs if w > 0]
            sparse_pairs.sort(key=lambda p: p[0])
            if sparse_pairs:
                sparse = {
                    "indices": [i for i, _ in sparse_pairs],
                    "values": [w for _, w in sparse_pairs],
                }
            else:
                sparse = self._fallback_sparse(texts[idx])

            output.append({
                "embedding": values,
                "sparse": sparse,
            })

        return output
