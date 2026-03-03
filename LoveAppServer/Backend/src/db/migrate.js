// src/db/migrate.js – Full schema init + incremental migrations
// Safe to run on every startup: CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS.
const pool = require('../config/db');
const logger = require('../config/logger');

const migrations = [
  // ══════════════════════════════════════════════════════════════════════════
  // CORE TABLES — created fresh on a blank database
  // ══════════════════════════════════════════════════════════════════════════

  // ── Users ─────────────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS users (
     id              BIGSERIAL    PRIMARY KEY,
     name            VARCHAR(100) NOT NULL DEFAULT '',
     email           VARCHAR(255) NOT NULL UNIQUE,
     password_hash   TEXT,
     avatar_url      TEXT,
     gender          VARCHAR(10),
     partner_id      INTEGER      REFERENCES users(id),
     partner_code    VARCHAR(8),
     role            VARCHAR(20)  NOT NULL DEFAULT 'user',
     pairing_code    VARCHAR(6),
     pairing_code_expires_at TIMESTAMPTZ,
     created_at      TIMESTAMPTZ  DEFAULT NOW(),
     updated_at      TIMESTAMPTZ  DEFAULT NOW()
   )`,

  // ── Notes ─────────────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS notes (
     id               BIGSERIAL   PRIMARY KEY,
     user_id          INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     title            VARCHAR(500) NOT NULL DEFAULT '',
     content          TEXT        NOT NULL DEFAULT '',
     is_private       BOOLEAN     NOT NULL DEFAULT false,
     tags             TEXT        NOT NULL DEFAULT '',
     image_url        TEXT,
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW(),
     updated_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Wishes ────────────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS wishes (
     id               BIGSERIAL   PRIMARY KEY,
     user_id          INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     title            VARCHAR(500) NOT NULL DEFAULT '',
     description      TEXT        NOT NULL DEFAULT '',
     is_completed     BOOLEAN     NOT NULL DEFAULT false,
     completed_at     TIMESTAMPTZ,
     priority         INTEGER     NOT NULL DEFAULT 0,
     image_url        TEXT,
     category         VARCHAR(100) NOT NULL DEFAULT '',
     due_date         DATE,
     is_private       BOOLEAN     NOT NULL DEFAULT false,
     emoji            TEXT        NOT NULL DEFAULT '',
     image_urls       TEXT        NOT NULL DEFAULT '',
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW(),
     updated_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Mood entries ──────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS mood_entries (
     id               BIGSERIAL   PRIMARY KEY,
     user_id          INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     mood_type        VARCHAR(50) NOT NULL DEFAULT '',
     note             TEXT        NOT NULL DEFAULT '',
     color            VARCHAR(7)  NOT NULL DEFAULT '#FFFFFF',
     date             DATE        NOT NULL DEFAULT CURRENT_DATE,
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Activity logs ─────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS activity_logs (
     id               BIGSERIAL   PRIMARY KEY,
     user_id          INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     title            VARCHAR(500) NOT NULL DEFAULT '',
     description      TEXT        NOT NULL DEFAULT '',
     category         VARCHAR(100) NOT NULL DEFAULT '',
     activity_type    VARCHAR(50) NOT NULL DEFAULT '',
     duration_minutes INTEGER     NOT NULL DEFAULT 0,
     start_time       VARCHAR(5)  NOT NULL DEFAULT '',
     note             TEXT        NOT NULL DEFAULT '',
     event_date       DATE        NOT NULL DEFAULT CURRENT_DATE,
     photo_url        TEXT,
     image_urls       TEXT,
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Menstrual cycles ──────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS menstrual_cycles (
     id                BIGSERIAL   PRIMARY KEY,
     user_id           INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     cycle_start_date  DATE        NOT NULL,
     cycle_duration    INTEGER     NOT NULL DEFAULT 28,
     period_duration   INTEGER     NOT NULL DEFAULT 5,
     symptoms          TEXT        NOT NULL DEFAULT '',
     mood              VARCHAR(50) NOT NULL DEFAULT '',
     notes             TEXT        NOT NULL DEFAULT '',
     last_updated      TIMESTAMPTZ DEFAULT NOW(),
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at        TIMESTAMPTZ,
     created_at        TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Custom calendars ──────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS custom_calendars (
     id               BIGSERIAL    PRIMARY KEY,
     user_id          INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     name             VARCHAR(200) NOT NULL DEFAULT '',
     description      TEXT         NOT NULL DEFAULT '',
     type             VARCHAR(50)  NOT NULL DEFAULT 'custom',
     color_hex        VARCHAR(7)   NOT NULL DEFAULT '#FF6B9D',
     icon             TEXT         NOT NULL DEFAULT '📅',
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Calendar events ───────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS calendar_events (
     id               BIGSERIAL    PRIMARY KEY,
     calendar_id      INTEGER      REFERENCES custom_calendars(id) ON DELETE CASCADE,
     user_id          INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     title            VARCHAR(500) NOT NULL DEFAULT '',
     description      TEXT         NOT NULL DEFAULT '',
     event_date       DATE         NOT NULL DEFAULT CURRENT_DATE,
     color_hex        VARCHAR(7)   NOT NULL DEFAULT '#FF6B9D',
     icon             TEXT         NOT NULL DEFAULT '📅',
     is_all_day       BOOLEAN      NOT NULL DEFAULT true,
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // Keep the old table name as alias (some routes reference it)
  `CREATE TABLE IF NOT EXISTS custom_calendar_events (
     id               BIGSERIAL    PRIMARY KEY,
     calendar_id      INTEGER      REFERENCES custom_calendars(id) ON DELETE CASCADE,
     user_id          INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     title            VARCHAR(500) NOT NULL DEFAULT '',
     description      TEXT         NOT NULL DEFAULT '',
     event_date       DATE         NOT NULL DEFAULT CURRENT_DATE,
     color_hex        VARCHAR(7)   NOT NULL DEFAULT '#FF6B9D',
     is_all_day       BOOLEAN      NOT NULL DEFAULT true,
     server_updated_at TIMESTAMPTZ DEFAULT NOW(),
     deleted_at       TIMESTAMPTZ,
     created_at       TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Relationship info ─────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS relationship_info (
     id                    BIGSERIAL PRIMARY KEY,
     user_id               INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     partner_user_id       INTEGER   REFERENCES users(id),
     relationship_start_date DATE,
     first_kiss_date       DATE,
     anniversary_date      DATE,
     nickname1             VARCHAR(100) NOT NULL DEFAULT '',
     nickname2             VARCHAR(100) NOT NULL DEFAULT '',
     my_birthday           DATE,
     partner_birthday      DATE,
     server_updated_at     TIMESTAMPTZ DEFAULT NOW(),
     created_at            TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── FCM tokens ────────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS fcm_tokens (
     user_id    INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
     fcm_token  TEXT NOT NULL,
     updated_at TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Refresh tokens ────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS refresh_tokens (
     id         BIGSERIAL PRIMARY KEY,
     user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     token      TEXT    NOT NULL UNIQUE,
     expires_at TIMESTAMPTZ NOT NULL,
     created_at TIMESTAMPTZ DEFAULT NOW()
   )`,

  // ── Custom activity types ─────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS custom_activity_types (
     id         BIGSERIAL    PRIMARY KEY,
     user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     name       VARCHAR(100) NOT NULL,
     emoji      TEXT         NOT NULL DEFAULT '✨',
     color_hex  VARCHAR(7)   NOT NULL DEFAULT '#FF6B9D',
     created_at TIMESTAMPTZ  DEFAULT NOW()
   )`,

  // ── Art canvases ──────────────────────────────────────────────────────────
  `CREATE TABLE IF NOT EXISTS art_canvases (
     id            BIGSERIAL    PRIMARY KEY,
     couple_key    VARCHAR(50)  NOT NULL,
     title         VARCHAR(200) NOT NULL DEFAULT 'Без названия',
     created_by    INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     thumbnail_url TEXT,
     updated_at    TIMESTAMPTZ  DEFAULT NOW(),
     created_at    TIMESTAMPTZ  DEFAULT NOW()
   )`,

  // ── Canvas strokes (persisted drawing data) ───────────────────────────────
  `CREATE TABLE IF NOT EXISTS canvas_strokes (
     id           BIGSERIAL    PRIMARY KEY,
     canvas_id    INTEGER      NOT NULL REFERENCES art_canvases(id) ON DELETE CASCADE,
     strokes_json TEXT         NOT NULL DEFAULT '[]',
     updated_at   TIMESTAMPTZ  DEFAULT NOW(),
     UNIQUE (canvas_id)
   )`,

  // ══════════════════════════════════════════════════════════════════════════
  // INCREMENTAL MIGRATIONS — safe to re-run (ADD COLUMN IF NOT EXISTS)
  // ══════════════════════════════════════════════════════════════════════════

  // ── Sync metadata columns ─────────────────────────────────────────────────
  `ALTER TABLE notes              ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE notes              ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE wishes             ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE wishes             ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE mood_entries       ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE mood_entries       ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE activity_logs      ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE activity_logs      ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE menstrual_cycles   ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE menstrual_cycles   ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE custom_calendars   ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE custom_calendars   ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMPTZ`,
  `ALTER TABLE calendar_events    ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE custom_calendar_events ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,
  `ALTER TABLE relationship_info  ADD COLUMN IF NOT EXISTS server_updated_at TIMESTAMPTZ DEFAULT NOW()`,

  // ── Refresh token indexes (table created above) ──────────────────────────
  `CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens(token)`,
  `CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id)`,

  // ── Users extra columns ───────────────────────────────────────────────────
  `ALTER TABLE users ADD COLUMN IF NOT EXISTS role                    VARCHAR(20)  NOT NULL DEFAULT 'user'`,
  `ALTER TABLE users ADD COLUMN IF NOT EXISTS pairing_code            VARCHAR(6)`,
  `ALTER TABLE users ADD COLUMN IF NOT EXISTS pairing_code_expires_at TIMESTAMPTZ`,
  `ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at              TIMESTAMPTZ  DEFAULT NOW()`,

  // ── Wishes extra columns ──────────────────────────────────────────────────
  `ALTER TABLE wishes ADD COLUMN IF NOT EXISTS is_private  BOOLEAN NOT NULL DEFAULT false`,
  `ALTER TABLE wishes ADD COLUMN IF NOT EXISTS emoji       TEXT    NOT NULL DEFAULT ''`,
  `ALTER TABLE wishes ADD COLUMN IF NOT EXISTS image_urls  TEXT    NOT NULL DEFAULT ''`,

  // ── Activity_logs extra columns ───────────────────────────────────────────
  `ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS activity_type     VARCHAR(50) NOT NULL DEFAULT ''`,
  `ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS duration_minutes  INTEGER     NOT NULL DEFAULT 0`,
  `ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS start_time        VARCHAR(5)  NOT NULL DEFAULT ''`,
  `ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS note              TEXT        NOT NULL DEFAULT ''`,

  // ── Relationship_info unique constraint ───────────────────────────────────
  `DO $$ BEGIN
     IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='relationship_info_user_id_key') THEN
       ALTER TABLE relationship_info ADD CONSTRAINT relationship_info_user_id_key UNIQUE (user_id);
     END IF;
   END $$`,
  `ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS partner_user_id INTEGER REFERENCES users(id)`,
  `ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS my_birthday     DATE`,
  `ALTER TABLE relationship_info ADD COLUMN IF NOT EXISTS partner_birthday DATE`,

  // ── Custom activity types extra columns (table created above) ────────────
  `ALTER TABLE custom_activity_types ALTER COLUMN emoji TYPE TEXT`,

  // ── Performance indexes ───────────────────────────────────────────────────
  `CREATE INDEX IF NOT EXISTS idx_notes_user_id          ON notes(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_notes_server_updated   ON notes(server_updated_at)`,
  `CREATE INDEX IF NOT EXISTS idx_wishes_user_id         ON wishes(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_wishes_server_updated  ON wishes(server_updated_at)`,
  `CREATE INDEX IF NOT EXISTS idx_moods_user_id          ON mood_entries(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_moods_date             ON mood_entries(date)`,
  `CREATE INDEX IF NOT EXISTS idx_moods_server_updated   ON mood_entries(server_updated_at)`,
  `CREATE INDEX IF NOT EXISTS idx_activities_user_id     ON activity_logs(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_activities_event_date  ON activity_logs(event_date)`,
  `CREATE INDEX IF NOT EXISTS idx_activities_server_upd  ON activity_logs(server_updated_at)`,
  `CREATE INDEX IF NOT EXISTS idx_cycles_user_id         ON menstrual_cycles(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_cycles_start_date      ON menstrual_cycles(cycle_start_date)`,
  `CREATE INDEX IF NOT EXISTS idx_relationship_user      ON relationship_info(user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_relationship_partner   ON relationship_info(partner_user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_art_canvases_couple    ON art_canvases(couple_key)`,
  `CREATE INDEX IF NOT EXISTS idx_canvas_strokes_canvas  ON canvas_strokes(canvas_id)`,
];

async function runMigrations() {
  let ok = 0; let failed = 0;
  for (const sql of migrations) {
    try {
      await pool.query(sql);
      ok++;
    } catch (err) {
      // non-fatal — some migrations may have already been run or conflict
      logger.warn(`Migration skipped: ${err.message.split('\n')[0]}`);
      failed++;
    }
  }
  logger.info(`Migrations: ${ok} applied, ${failed} skipped`);
}

module.exports = { runMigrations };
