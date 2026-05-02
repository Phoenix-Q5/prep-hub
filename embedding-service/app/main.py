"""
Embedding microservice — GPU-accelerated text embeddings via bge-small-en-v1.5.

Endpoints:
  POST /embed         - embed one or more texts, returns vectors (384 dims)
  POST /similarity    - compute cosine similarity between two texts
  GET  /health        - health check with model metadata
"""

import os
import time
import logging
from contextlib import asynccontextmanager

import numpy as np
import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

logger = logging.getLogger("embedding-service")

MODEL_NAME = os.getenv("MODEL_NAME", "BAAI/bge-small-en-v1.5")
DEVICE = os.getenv("DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
MAX_BATCH = int(os.getenv("MAX_BATCH", "128"))

model: SentenceTransformer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    logger.info(f"Loading model {MODEL_NAME} on {DEVICE}...")
    start = time.time()
    model = SentenceTransformer(MODEL_NAME, device=DEVICE)
    elapsed = time.time() - start
    dim = model.get_sentence_embedding_dimension()
    logger.info(f"Model loaded in {elapsed:.1f}s — {dim} dims, device={DEVICE}")
    yield
    model = None


app = FastAPI(title="PrepHub Embedding Service", lifespan=lifespan)


# ---------- schemas ----------

class EmbedRequest(BaseModel):
    texts: list[str] = Field(..., min_length=1, max_length=MAX_BATCH)
    normalize: bool = True


class EmbedResponse(BaseModel):
    embeddings: list[list[float]]
    dimension: int
    model: str
    device: str
    elapsed_ms: float


class SimilarityRequest(BaseModel):
    text_a: str
    text_b: str


class SimilarityResponse(BaseModel):
    similarity: float
    model: str


# ---------- endpoints ----------

@app.post("/embed", response_model=EmbedResponse)
async def embed(req: EmbedRequest):
    if model is None:
        raise HTTPException(503, "Model not loaded")

    start = time.time()
    vecs = model.encode(
        req.texts,
        normalize_embeddings=req.normalize,
        convert_to_numpy=True,
        show_progress_bar=False,
        batch_size=min(len(req.texts), 64),
    )
    elapsed_ms = (time.time() - start) * 1000

    return EmbedResponse(
        embeddings=vecs.tolist(),
        dimension=vecs.shape[1],
        model=MODEL_NAME,
        device=DEVICE,
        elapsed_ms=round(elapsed_ms, 2),
    )


@app.post("/similarity", response_model=SimilarityResponse)
async def similarity(req: SimilarityRequest):
    if model is None:
        raise HTTPException(503, "Model not loaded")

    vecs = model.encode(
        [req.text_a, req.text_b],
        normalize_embeddings=True,
        convert_to_numpy=True,
    )
    sim = float(np.dot(vecs[0], vecs[1]))
    return SimilarityResponse(similarity=round(sim, 6), model=MODEL_NAME)


@app.get("/health")
async def health():
    if model is None:
        raise HTTPException(503, "Model not loaded")
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "device": DEVICE,
        "dimension": model.get_sentence_embedding_dimension(),
        "gpu_available": torch.cuda.is_available(),
        "gpu_name": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
    }
