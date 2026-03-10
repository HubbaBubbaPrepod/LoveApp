# ─── migrate-db.ps1 ──────────────────────────────────────────
# Дамп из текущей PostgreSQL → восстановление в Docker-контейнер
# Запускать: .\scripts\migrate-db.ps1 из директории love-app-backend-nest\
# ─────────────────────────────────────────────────────────────

$ErrorActionPreference = "Stop"

# ── Источник (текущая БД) ─────────────────────────────────
$SRC_HOST = if ($env:SRC_HOST) { $env:SRC_HOST } else { "168.222.193.34" }
$SRC_PORT = if ($env:SRC_PORT) { $env:SRC_PORT } else { "5432" }
$SRC_USER = if ($env:SRC_USER) { $env:SRC_USER } else { "spyuser" }
$SRC_DB   = if ($env:SRC_DB)   { $env:SRC_DB }   else { "loveapp_db" }
$SRC_PASS = if ($env:SRC_PASS) { $env:SRC_PASS } else { "0451" }

# ── Назначение (Docker-контейнер) ─────────────────────────
$DST_CONTAINER = "loveapp-postgres"
$DST_USER      = "loveapp"
$DST_DB        = "loveapp"

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$DUMP_FILE = "db_dump_$timestamp.sql"

Write-Host ""
Write-Host "=== Миграция PostgreSQL -> Docker контейнер ===" -ForegroundColor Cyan
Write-Host ""

# ── Шаг 1: Дамп текущей БД ───────────────────────────────
Write-Host "[1/3] Создаю дамп из ${SRC_HOST}:${SRC_PORT}/${SRC_DB} ..." -ForegroundColor Yellow

$env:PGPASSWORD = $SRC_PASS
pg_dump -h $SRC_HOST -p $SRC_PORT -U $SRC_USER -d $SRC_DB `
  --no-owner --no-privileges --clean --if-exists -F p -f $DUMP_FILE
$env:PGPASSWORD = ""

$size = (Get-Item $DUMP_FILE).Length / 1MB
Write-Host "   OK Дамп создан: $DUMP_FILE ($([math]::Round($size, 2)) MB)" -ForegroundColor Green

# ── Шаг 2: Копируем дамп в контейнер ─────────────────────
Write-Host "[2/3] Копирую дамп в контейнер ${DST_CONTAINER} ..." -ForegroundColor Yellow
docker cp $DUMP_FILE "${DST_CONTAINER}:/tmp/${DUMP_FILE}"
Write-Host "   OK Скопирован" -ForegroundColor Green

# ── Шаг 3: Восстанавливаем ───────────────────────────────
Write-Host "[3/3] Восстанавливаю в ${DST_DB} ..." -ForegroundColor Yellow
docker exec $DST_CONTAINER psql -U $DST_USER -d $DST_DB -f "/tmp/$DUMP_FILE"
docker exec $DST_CONTAINER rm -f "/tmp/$DUMP_FILE"

Write-Host ""
Write-Host "=== Миграция завершена! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Проверить:" -ForegroundColor Cyan
Write-Host "  docker exec -it $DST_CONTAINER psql -U $DST_USER -d $DST_DB"
Write-Host "  \dt                         -- список таблиц"
Write-Host "  SELECT count(*) FROM users; -- проверка данных"
Write-Host ""

# Раскомментируй чтобы удалить локальный дамп:
# Remove-Item $DUMP_FILE
