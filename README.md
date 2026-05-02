# PrepHub — Interview Preparation Platform

A self-hosted, full-stack interview prep platform with real-time search-as-you-type, semantic search, and hybrid search powered by vector embeddings.

## Stack

- **Frontend**: React 18, TypeScript, Vite, TailwindCSS, shadcn/ui, TanStack Query, Zustand, React Router v6
- **Backend**: Spring Boot 3.3, Java 21, Spring Security + JWT, Spring Data JPA
- **Database**: PostgreSQL 16 (with `pg_trgm`, `btree_gin`, `pgvector`, `pg_stat_statements`)
- **Search**: Elasticsearch 8 (edge n-gram tokenizer for type-ahead) + pgvector (semantic/hybrid)
- **Embeddings**: Python FastAPI microservice — `BAAI/bge-small-en-v1.5` via `sentence-transformers` (384 dims, GPU-accelerated)
- **Cache**: Redis 7
- **Storage**: MinIO (S3-compatible)
- **Observability**: Prometheus + Grafana + Loki + Promtail + Jaeger + cAdvisor + Node/PG/Redis/ES exporters
- **Migrations**: Flyway

## Hardware Profile

Tuned for: 24-core dual CPU, 104GB RAM, 12TB disk. See `docker-compose.yml` for resource allocations.

## Quick Start

```bash
# 1. Bring up infrastructure
cp .env.example .env
docker compose up -d

# 2. Wait for everything to be healthy
docker compose ps

# 3. Run the backend
cd backend
./mvnw spring-boot:run

# 4. Run the frontend (dev mode)
cd frontend
npm install
npm run dev

# 5. Run the embedding service
cd embedding-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- Backend: `http://localhost:8080` — Swagger UI at `/swagger-ui.html`
- Frontend: `http://localhost:5173` (Vite dev server, proxies `/api` to the backend)
- Embedding service: `http://localhost:8000`

A bootstrap admin is created on first start: `admin` / `admin12345` (change in production via `app.bootstrap.*` properties).

## Service Endpoints

| Service           | URL                                    | Notes                             |
|-------------------|----------------------------------------|-----------------------------------|
| API               | http://localhost:8080                  | Spring Boot                       |
| Swagger UI        | http://localhost:8080/swagger-ui.html  | OpenAPI                           |
| Frontend          | http://localhost:5173                  | Vite dev server                   |
| Embedding Service | http://localhost:8000                  | FastAPI, bge-small-en-v1.5        |
| Postgres          | localhost:5432                         | user/pw in `.env`                 |
| Elasticsearch     | http://localhost:9200                  | Single-node                       |
| Redis             | localhost:6379                         | LRU eviction, 3GB cap             |
| MinIO API         | http://localhost:9000                  | S3-compatible                     |
| MinIO Console     | http://localhost:9001                  | Web UI                            |
| Prometheus        | http://localhost:9090                  | Metrics                           |
| Grafana           | http://localhost:3001                  | admin/admin                       |
| Loki              | http://localhost:3100                  | Log aggregation                   |
| Jaeger UI         | http://localhost:16686                 | Distributed tracing               |

## Key API Endpoints

### Public
- `GET  /api/topics` — list topics (cached)
- `GET  /api/questions?topicId=...&page=0&size=20` — list questions
- `GET  /api/questions/hot?limit=10` — hot questions (cached)
- `GET  /api/questions/{id}` — question detail
- `GET  /api/search/typeahead?q=java&topicId=...&limit=10` — instant search (Elasticsearch)
- `GET  /api/search/semantic?q=...&limit=10` — semantic search (pgvector cosine similarity)
- `GET  /api/search/hybrid?q=...&limit=10` — hybrid search (typeahead + semantic, merged)
- `GET  /api/users/{username}` — public profile

### Authenticated
- `POST /api/auth/register` — sign up
- `POST /api/auth/login` — get tokens
- `POST /api/auth/refresh` — refresh access token
- `GET  /api/users/me` — your profile
- `PATCH /api/users/me` — update bio/display name
- `POST /api/users/me/avatar` — upload avatar (multipart)
- `POST /api/questions` — submit a question
- `POST /api/questions/{id}/likes` — like
- `DELETE /api/questions/{id}/likes` — unlike
- `POST /api/suggestions` — submit a suggestion (new question / topic / edit)
- `GET  /api/suggestions/me` — your suggestions

### Admin
- `POST /api/admin/topics` — create topic
- `DELETE /api/admin/topics/{id}` — delete topic
- `GET  /api/admin/suggestions/pending` — review queue
- `POST /api/admin/suggestions/{id}/review` — approve or reject

## Search

### Search-as-you-Type (Elasticsearch)
The frontend debounces input (~150ms) and calls `/api/search/typeahead?q=...`. The Elasticsearch index uses an `edge_ngram` analyzer (2–20 chars):
- Typing `j` → no match (below 2-char minimum, intentional to reduce noise)
- Typing `ja` → matches "java", "javascript", "jaeger"
- Typo tolerance via `fuzziness: AUTO`

Cached in Redis for 60s per `(q, topicId)` pair.

### Semantic Search (pgvector)
`/api/search/semantic` embeds the query via the embedding service, then runs a nearest-neighbor cosine similarity search against the `question_embeddings` table (pgvector). Questions are auto-embedded asynchronously on create/update using `BAAI/bge-small-en-v1.5` (384 dims).

### Hybrid Search
`/api/search/hybrid` merges typeahead and semantic results, deduplicates by question ID, and ranks by combined score.

## Embedding Service

A lightweight FastAPI microservice (`embedding-service/`) that wraps `sentence-transformers`.

**Endpoints:**

| Method | Path          | Description                                      |
|--------|---------------|--------------------------------------------------|
| POST   | `/embed`      | Embed one or more texts, returns float vectors   |
| POST   | `/similarity` | Cosine similarity between two texts              |
| GET    | `/health`     | Health check with model + device metadata        |

- Model: `BAAI/bge-small-en-v1.5` (configurable via `MODEL_NAME` env var)
- Device: auto-detects CUDA, falls back to CPU
- Max batch size: 128 (configurable via `MAX_BATCH` env var)

## Frontend Pages

| Route              | Component            | Access     |
|--------------------|----------------------|------------|
| `/`                | HomePage             | Public     |
| `/login`           | LoginPage            | Public     |
| `/register`        | RegisterPage         | Public     |
| `/questions/:id`   | QuestionPage         | Public     |
| `/profile/:username` | ProfilePage        | Public     |
| `/ask`             | CreateQuestionPage   | Auth only  |
| `/suggestions`     | SuggestionsPage      | Auth only  |
| `/admin`           | AdminPage            | Admin only |

State management: Zustand (`useAuthStore`). Server state: TanStack Query with 30s stale time.

## Hot Topics

`GET /api/questions/hot` is cached in Redis for 5 minutes. Cache is evicted on any write (create/like/update/delete).

## Observability

- **Metrics**: Spring exposes `/actuator/prometheus`. Prometheus scrapes app + all exporters.
- **Logs**: Promtail tails Docker container logs into Loki. Query in Grafana → Explore → Loki.
- **Traces**: Spring sends OTLP traces to Jaeger. Each request has a trace ID embedded in logs.
- **Dashboards**: Drop pre-built Grafana JSON into `docker/grafana/dashboards/`.

Recommended Grafana dashboards (import by ID):
- 4701 (JVM Micrometer)
- 9628 (PostgreSQL)
- 11835 (Redis)
- 14191 (Elasticsearch)
- 13639 (Loki Logs)

## Project Structure

```
prephub/
├── docker-compose.yml         # All infrastructure
├── .env.example               # Copy to .env and edit
├── docker/                    # Service configs
│   ├── postgres/init.sql
│   ├── redis/redis.conf
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/
│   ├── loki/local-config.yaml
│   └── promtail/config.yml
├── embedding-service/         # Python FastAPI embedding microservice
│   ├── app/main.py            # FastAPI app (embed, similarity, health)
│   ├── requirements.txt       # fastapi, uvicorn, sentence-transformers, torch
│   └── Dockerfile
├── frontend/                  # React + Vite SPA
│   ├── src/
│   │   ├── api/               # Axios client + typed endpoint functions
│   │   ├── components/        # Header, QuestionCard, SearchBar, sidebars, shadcn/ui
│   │   ├── hooks/             # useDebounce
│   │   ├── pages/             # HomePage, QuestionPage, LoginPage, RegisterPage, …
│   │   ├── store/             # Zustand auth store
│   │   └── types/             # Shared TypeScript types
│   ├── package.json
│   └── vite.config.ts
└── backend/
    ├── pom.xml
    └── src/main/
        ├── java/com/prephub/
        │   ├── PrepHubApplication.java
        │   ├── config/        # Properties, CORS, Redis, MinIO, ES index init
        │   ├── security/      # JWT, filter, auth controller, security config
        │   ├── common/        # Auditable, exceptions, enums, error handler
        │   ├── user/          # User, Portfolio
        │   ├── topic/
        │   ├── question/
        │   ├── answer/
        │   ├── engagement/    # Likes
        │   ├── suggestion/
        │   ├── search/        # Elasticsearch document, indexer, search service
        │   ├── embedding/     # EmbeddingClient, EmbeddingService, pgvector repo
        │   ├── storage/       # MinIO storage service
        │   └── bootstrap/     # Admin seeder
        └── resources/
            ├── application.yml
            ├── banner.txt
            ├── elasticsearch/questions-settings.json
            └── db/migration/  # Flyway migrations (V1–V3 incl. pgvector)
```
