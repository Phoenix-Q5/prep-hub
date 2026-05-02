# PrepHub — Interview Preparation Platform

A self-hosted, full-stack interview prep platform with real-time search-as-you-type.

## Stack

- **Backend**: Spring Boot 3.3, Java 21, Spring Security + JWT, Spring Data JPA
- **Database**: PostgreSQL 16 (with `pg_trgm`, `btree_gin`, `pg_stat_statements`)
- **Search**: Elasticsearch 8 (edge n-gram tokenizer for type-ahead)
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
```

Backend lives at `http://localhost:8080`. Swagger UI at `/swagger-ui.html`.

A bootstrap admin is created on first start: `admin` / `admin12345` (change in production via `app.bootstrap.*` properties).

## Service Endpoints

| Service        | URL                          | Notes                             |
|----------------|------------------------------|-----------------------------------|
| API            | http://localhost:8080        | Spring Boot                       |
| Swagger UI     | http://localhost:8080/swagger-ui.html | OpenAPI                  |
| Postgres       | localhost:5432               | user/pw in `.env`                 |
| Elasticsearch  | http://localhost:9200        | Single-node                       |
| Redis          | localhost:6379               | LRU eviction, 3GB cap             |
| MinIO API      | http://localhost:9000        | S3-compatible                     |
| MinIO Console  | http://localhost:9001        | Web UI                            |
| Prometheus     | http://localhost:9090        | Metrics                           |
| Grafana        | http://localhost:3001        | admin/admin                       |
| Loki           | http://localhost:3100        | Log aggregation                   |
| Jaeger UI      | http://localhost:16686       | Distributed tracing               |

## Key API Endpoints

### Public
- `GET  /api/topics` — list topics (cached)
- `GET  /api/questions?topicId=...&page=0&size=20` — list questions
- `GET  /api/questions/hot?limit=10` — hot questions (cached)
- `GET  /api/questions/{id}` — question detail
- `GET  /api/search/typeahead?q=java&topicId=...&limit=10` — instant search
- `GET  /api/users/{username}` — public profile

### Authenticated
- `POST /api/auth/register` — sign up
- `POST /api/auth/login` — get tokens
- `POST /api/auth/refresh` — refresh access token
- `GET  /api/users/me` — your profile
- `PATCH /api/users/me` — update bio/display name
- `POST /api/users/me/avatar` — upload avatar (multipart)
- `POST /api/questions` — submit a question (becomes a suggestion if you want — adapt to your workflow)
- `POST /api/questions/{id}/likes` — like
- `DELETE /api/questions/{id}/likes` — unlike
- `POST /api/suggestions` — submit a suggestion (new question / topic / edit)
- `GET  /api/suggestions/me` — your suggestions

### Admin
- `POST /api/admin/topics` — create topic
- `DELETE /api/admin/topics/{id}` — delete topic
- `GET  /api/admin/suggestions/pending` — review queue
- `POST /api/admin/suggestions/{id}/review` — approve or reject

## Search-as-you-Type

The frontend should debounce input (~150ms) and call `/api/search/typeahead?q=...`. The Elasticsearch index uses an `edge_ngram` analyzer (2-20 chars), so:
- Typing `j` → no match (below 2-char minimum, intentional to reduce noise)
- Typing `ja` → matches "java", "javascript", "jaeger"
- Typing `jav` → narrows further
- Typo tolerance: `jav` matches `Java` exactly; `javv` matches via `fuzziness: AUTO`

Cached in Redis for 60s per (q, topicId) pair.

## Hot Topics

`GET /api/questions/hot` is cached in Redis for 5 minutes. Cache is evicted on any write (create/like/update/delete) so it stays reasonably fresh while protecting the DB.

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
        │   ├── storage/       # MinIO storage service
        │   └── bootstrap/     # Admin seeder
        └── resources/
            ├── application.yml
            ├── elasticsearch/questions-settings.json
            └── db/migration/  # Flyway migrations
```

## Next Steps

1. **Build the frontend** — React + Vite + TypeScript + Tailwind. The three-column layout from the architecture diagram.
2. **Add the `AnswerService` + controller** — currently entities and repo exist; service is left as a small exercise.
3. **Wire SSE or WebSocket** — for live counter updates on questions when others like/comment.
4. **Add semantic search later** — your 12GB GPU can run a small embedding model (e.g., `bge-small-en`) and store vectors in Postgres via `pgvector`. Then your search becomes hybrid: keyword + semantic.
5. **Scheduled re-indexing job** — for resilience if ES gets out of sync.
