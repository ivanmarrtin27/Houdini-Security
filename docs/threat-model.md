# Modelo de amenazas — Houdini Security

## Qué protege

- Confidencialidad de secretos en disco frente a lectura del fichero `.db` sin master password (payloads AES-GCM).
- Autenticidad/integridad de payloads y DEK (tag GCM); manipulación → fallo de autenticación.
- Resistencia a fuerza bruta offline de la master mediante Argon2id/PBKDF2 costosos.
- Exposición accidental vía portapapeles (auto-clear si el contenido sigue siendo el de la app).
- Exposición por inactividad (autolock + wipe de DEK/vistas).
- Logs: no deben contener secretos (política de código + SLF4J mínimo).

## Qué NO protege

- Atacante con master password o que observe al usuario teclearla.
- Malware con privilegios que lea memoria del proceso, hooks de teclado, o dump de RAM/swap/hibernación (Java GC puede copiar arrays; ver `docs/limitations.md`).
- Análisis forense avanzado del fichero SQLite: esquema, conteo de filas, nombres de carpetas/etiquetas en claro.
- Dispositivo ya comprometido (rootkit, debugger adjunto).
- Pérdida de la master password (no hay recuperación en la nube).
- Import CSV en claro mientras el fichero CSV exista en disco.
- Amenazas de red (el producto no usa red; no mitiga MITM de otros programas).

## Actores considerados

| Actor | Mitigación principal |
|-------|----------------------|
| Ladrón de disco / backup sin clave | Cifrado app-level + KDF |
| Compañero de piso con acceso breve | Autolock + clipboard clear |
| Malware local privilegiado | Fuera de alcance realista |
| Usuario que pierde la clave | Fuera de alcance (diseño) |
