# Entrevista inicial — Houdini Security

**Estado:** completada y alineada con el brief de producto (2026-11-19).

---

## Bloque A — Producto y usuarios

1. ¿Qué problema concreto resuelve Houdini Security y para quién?
   - **Respuesta:** Caja fuerte digital local/offline para contraseñas, notas secretas y claves API sin nube. Usuario de escritorio que quiere control total de sus secretos.
2. ¿Cuál es el resultado mínimo de valor en la primera versión (MVP)?
   - **Respuesta:** Bóveda cifrada crear/abrir; CRUD de PasswordEntry, SecureNote, ApiKeyEntry; etiquetas/carpetas; búsqueda; favoritos; generador; medidor de fortaleza; auditoría local; clipboard auto-clear; autolock; backup cifrado export/import; import CSV; cambio de master password (KEK/DEK).
3. ¿Qué queda explícitamente fuera de la v1?
   - **Respuesta:** Nube/sync, telemetría, updates automáticos, favicons de red, extensiones de navegador, móvil, 2FA como unlock de bóveda, compartir, autofill del SO.

## Bloque B — Contexto y restricciones

4. ¿Hay requisitos legales, de privacidad o de seguridad innegociables?
   - **Respuesta:** AES-256-GCM; Argon2id + PBKDF2 configurables; KEK/DEK; `char[]`/`byte[]` + wipe; autolock; clipboard clear; cero red; SQL parametrizado; SLF4J mínimo.
5. ¿Hay datos sensibles? ¿Dónde pueden y no pueden guardarse?
   - **Respuesta:** Sí. Solo bóveda local cifrada. Nunca en claro en disco, nunca a red, nunca en logs.
6. ¿Hay integraciones obligatorias?
   - **Respuesta:** No.

## Bloque C — Stack y entrega

7. ¿Qué stack prefieres?
   - **Respuesta:** Java 21+, JavaFX 21+ + AtlantaFX, Bouncy Castle, SQLite jdbc + cifrado app-level, Maven, JUnit 5.
8. ¿Web, móvil, escritorio, CLI, API, o combinación?
   - **Respuesta:** Escritorio (JavaFX).
9. ¿Cómo se desplegará / se probará en local?
   - **Respuesta:** Maven (`mvn test`, `mvn javafx:run`); empaquetado con jpackage (notas en README).

## Bloque D — Calidad y proceso

10. ¿Política de tests y puerta de calidad?
    - **Respuesta:** JUnit 5 para crypto, generador, auditoría, cambio master, integración repo temporal. UI: validación manual.
11. ¿Idioma del código vs. UI y docs?
    - **Respuesta:** Docs/UI en español (ResourceBundle). Código en inglés.
12. ¿Quién aprueba specs/planes?
    - **Respuesta:** El usuario.

---

## Resumen acordado
| Tema | Estado |
|------|--------|
| Producto offline | Definido |
| MVP | Definido (brief) |
| Fuera v1 | Definido |
| Tests | JUnit 5 núcleo + UI manual |
| Idioma | UI/docs ES; código EN |
| Aprobador | Usuario |
| Cifrado SQLite | App-level AES-GCM (ver `docs/design-decisions.md`) |
