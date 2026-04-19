#!/bin/bash
# Daily PostgreSQL dump to dumps/, remove older than 7 days.
set -e
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"
DUMP_DIR="$DEPLOY_DIR/dumps"
RETENTION_DAYS=7
CONTAINER="${CONTAINER_NAME:-devops-lesson-db}"
DB_NAME="${POSTGRES_DB:-devops_lesson}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"

mkdir -p "$DUMP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DUMP_FILE="$DUMP_DIR/devops_lesson_$TIMESTAMP.sql"

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  docker exec "$CONTAINER" pg_dump -U "$POSTGRES_USER" -d "$DB_NAME" --no-owner --no-acl > "$DUMP_FILE"
  echo "Dump created: $DUMP_FILE"
else
  echo "DB container not found: $CONTAINER" >&2
  exit 1
fi

find "$DUMP_DIR" -maxdepth 1 -name "devops_lesson_*.sql" -type f -mtime +$RETENTION_DAYS -delete
echo "Old dumps (older than $RETENTION_DAYS days) removed."
