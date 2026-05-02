#!/bin/bash
set -e
cd "$(dirname "$0")/.."
[ -f .env ] || cp .env.example .env
docker compose up -d
echo ""
echo "Waiting for services to be healthy..."
sleep 5
docker compose ps
echo ""
echo "Run backend with: cd backend && ./mvnw spring-boot:run"
