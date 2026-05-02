#!/bin/bash
# DANGER: wipes all data
cd "$(dirname "$0")/.."
docker compose down -v
echo "All volumes removed."
