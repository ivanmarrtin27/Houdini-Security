# Formato de bóveda Houdini — versión 1

## Ficheros

Para una bóveda en ruta `path/to/name.houdini`:

| Fichero | Rol |
|---------|-----|
| `name.houdini` | Cabecera binaria (magic, KDF, salt, DEK envuelta) |
| `name.houdini.db` | SQLite (metadatos + ciphertext por entrada) |

Escritura atómica: escribir a `*.tmp` y `Files.move(ATOMIC_MOVE|REPLACE_EXISTING)`. Antes de migraciones o cambio de master: copia `*.bak`.

## Cabecera (`*.houdini`) — little-endian

| Offset | Tamaño | Campo |
|--------|--------|-------|
| 0 | 4 | Magic `HOUD` (0x48 0x4F 0x55 0x44) |
| 4 | 2 | Versión formato `uint16` = 1 |
| 6 | 1 | KDF id: `1`=Argon2id, `2`=PBKDF2-HMAC-SHA256 |
| 7 | 1 | Reservado (0) |
| 8 | 4 | memoryKiB (Argon2) o 0 |
| 12 | 4 | iterations |
| 16 | 4 | parallelism (Argon2) o 0 |
| 20 | 2 | saltLen |
| 22 | saltLen | salt (≥16 bytes) |
| … | 12 | nonce GCM del envoltorio DEK |
| … | 32+16 | ciphertext DEK (32) + tag (16) |

AAD del envoltorio DEK: `magic || version` (6 bytes).

## Tablas SQLite

```sql
CREATE TABLE schema_meta (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE folders (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  parent_id TEXT
);

CREATE TABLE tags (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE entries (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,          -- PASSWORD | NOTE | API_KEY
  folder_id TEXT,
  favorite INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,  -- epoch millis
  updated_at INTEGER NOT NULL,
  ciphertext BLOB NOT NULL      -- nonce(12) || ct || tag(16)
);

CREATE TABLE entry_tags (
  entry_id TEXT NOT NULL,
  tag_id TEXT NOT NULL,
  PRIMARY KEY (entry_id, tag_id)
);

CREATE TABLE app_settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
```

`schema_meta` incluye `format_version=1`.

## Payload cifrado (JSON UTF-8, luego AES-256-GCM con DEK)

AAD por registro: `entryId UTF-8 || "|" || formatVersion`.

**PASSWORD:** `site`, `username`, `password`, `url`, `notes`, `recoveryCodes` (opcional; ausente = vacío; códigos de respaldo 2FA, secreto)  
**NOTE:** `title`, `body`  
**API_KEY:** `service`, `apiKey`, `apiSecret`, `expiresAt` (epoch o null)

## Backup cifrado

Export = cabecera + dump SQLite empaquetados en contenedor HOUD-BACKUP v1 (magic `HBAK`) con la misma DEK envuelta (o re-envoltorio con passphrase de backup). Import valida magic/versión y restaura ficheros.
