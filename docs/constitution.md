# Constitución — Houdini Security

**Estado: APROBADA** (brief de producto del usuario, 2026-11-19)

Principios innegociables. Toda spec, plan y tarea debe cumplirlos.
Producto: gestor de contraseñas y bóveda cifrada **offline** (escritorio).

1. **Simplicidad del stack**: stack permitido = Java 21+, JavaFX 21+ + AtlantaFX, Bouncy Castle, SQLite (jdbc) con cifrado a nivel de aplicación, Maven, JUnit 5. Sin nube, telemetría, sync ni backends remotos. Toda dependencia nueva requiere aprobación explícita.
2. **La spec manda**: nada se implementa si no está en la spec activa; cambios de requisitos primero en `spec.md`. Si falta una decisión, se para y se pregunta (salvo que el brief aprobado ya la cubra).
3. **Separación lógica / interfaz**: capas `crypto` → `storage` → `domain` → `service` → `ui` (MVVM). Criptografía y reglas de negocio fuera de JavaFX. Secretos no se loguean ni se persisten en claro. DI manual.
4. **Política de tests**: la lógica de cifrado, KDF, generador, auditoría, cambio de master password y repositorio de bóveda se cubre con JUnit 5. La UI se valida de forma manual según la spec. Sin evidencia de verificación acordada, no se cierra una tarea de núcleo.
5. **Datos del usuario**: bóveda local; AES-256-GCM (nonce 96-bit, tag 128); KEK/DEK; Argon2id (≥64 MiB, ≥3 iter, parallel 2–4) y/o PBKDF2-HMAC-SHA256 (≥600k); secretos en `char[]`/`byte[]` con limpieza; autolock; limpia portapapeles. Cero red.
6. **Idioma**: documentación e interfaz en español (ResourceBundle). Código (paquetes, clases, APIs) en inglés. Aprobador de specs/planes: el usuario.

## Capabilidades de producto (MVP aprobado)
Crear/abrir bóvedas · CRUD PasswordEntry / SecureNote / ApiKeyEntry · etiquetas/carpetas · búsqueda · favoritos · generador · medidor de fortaleza · auditoría local · clipboard auto-clear · autolock · export/import backup cifrado · import CSV · cambio de master password (re-cifrado solo de DEK).

## Fuera de alcance v1
Nube/sync · telemetría · extensiones de navegador · móvil · 2FA TOTP como autenticación de bóveda · compartir bóvedas · autofill del SO.

## Cómo aprobar cambios
Proponer cambio en constitución/spec y obtener **aprobación explícita** del usuario antes de implementar.
