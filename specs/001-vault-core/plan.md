# Plan 001 — Vault Core

## Arquitectura
- `com.houdinisecurity.crypto` — AES-GCM, KDF, SecretHolder, VaultCrypto
- `com.houdinisecurity.domain` — entradas, carpetas, tags, excepciones
- `com.houdinisecurity.storage` — cabecera, SQLite repo, backup
- `com.houdinisecurity.service` — vault, generator, strength, audit, clipboard, autolock, import/export
- `com.houdinisecurity.ui` — MVVM JavaFX + AtlantaFX

## Decisiones
- Cifrado app-level (DD-1): cubre RF-1..3, RNF-1.
- KEK/DEK: RF-9.
- Búsqueda en memoria tras unlock: RF-3 + seguridad de payloads.

## Tests
Round-trip crypto, bad password, GCM tamper, nonce uniqueness, generator, audit, master change, repo temporal.
