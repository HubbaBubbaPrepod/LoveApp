#!/bin/bash
# ─── migrate-db.sh ──────────────────────────────────────────
# Дамп из текущей PostgreSQL → восстановление в Docker-контейнер
# Запускать из директории love-app-backend-nest/
# ─────────────────────────────────────────────────────────────

set -euo pipefail

# ── Источник (текущая БД) ─────────────────────────────────
SRC_HOST="${SRC_HOST:-168.222.193.34}"
SRC_PORT="${SRC_PORT:-5432}"
SRC_USER="${SRC_USER:-spyuser}"
SRC_DB="${SRC_DB:-loveapp_db}"

# ── Назначение (Docker-контейнер) ─────────────────────────
DST_CONTAINER="${DST_CONTAINER:-loveapp-postgres}"
DST_USER="${DST_USER:-spyuser}"
DST_DB="${DST_DB:-loveapp_db}"

DUMP_FILE="db_dump_$(date +%Y%m%d_%H%M%S).sql"

echo "╔══════════════════════════════════════════╗"
echo "║  Миграция PostgreSQL → Docker контейнер  ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── Шаг 1: Дамп текущей БД ───────────────────────────────
echo "[1/3] Создаю дамп из ${SRC_HOST}:${SRC_PORT}/${SRC_DB} ..."
PGPASSWORD="${SRC_PASS:-0451}" pg_dump \
  -h "$SRC_HOST" \
  -p "$SRC_PORT" \
  -U "$SRC_USER" \
  -d "$SRC_DB" \
  --no-owner \
  --no-privileges \
  --clean \
  --if-exists \
  -F p \
  -f "$DUMP_FILE"

echo "   ✓ Дамп создан: $DUMP_FILE ($(du -h "$DUMP_FILE" | cut -f1))"

# ── Шаг 2: Копируем дамп в контейнер ─────────────────────
echo "[2/3] Копирую дамп в контейнер ${DST_CONTAINER} ..."
docker cp "$DUMP_FILE" "${DST_CONTAINER}:/tmp/${DUMP_FILE}"
echo "   ✓ Дамп скопирован"

# ── Шаг 3: Восстанавливаем ───────────────────────────────
echo "[3/3] Восстанавливаю в ${DST_DB} ..."
docker exec -i "$DST_CONTAINER" \
  psql -U "$DST_USER" -d "$DST_DB" -f "/tmp/${DUMP_FILE}"

# Чистим дамп из контейнера
docker exec "$DST_CONTAINER" rm -f "/tmp/${DUMP_FILE}"

echo ""
echo "═══════════════════════════════════════════"
echo "  ✅ Миграция завершена!"
echo ""
echo "  Проверить: docker exec -it ${DST_CONTAINER} psql -U ${DST_USER} -d ${DST_DB}"
echo "  Затем:     \\dt   — список таблиц"
echo "             SELECT count(*) FROM users;"
echo "═══════════════════════════════════════════"

# Удалить локальный дамп (раскомментируй если не нужен)
# rm -f "$DUMP_FILE"
