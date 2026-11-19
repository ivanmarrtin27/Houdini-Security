# Limitaciones y mejoras futuras

## Limitaciones actuales

- **GC / swap:** Java no garantiza borrado físico de todas las copias de `char[]`/`byte[]` (GC, String accidentales, swap del SO). Se mitiga con `SecretHolder`, `Arrays.fill` en `finally` y evitar `String` para secretos; no es garantía absoluta.
- **Metadatos SQLite en claro:** carpetas, tags, timestamps, favoritos y tipos son visibles en el `.db` (ver threat model).
- **Sin SQLCipher:** el fichero DB no es opaco completo.
- **Wordlist passphrase:** lista embebida limitada (inglesa corta); no es diceware auditada completa.
- **i18n:** solo español en v1 (estructura lista para más locales).
- **JPMS:** algunas dependencias son automatic modules; empaquetado jpackage puede requerir ajustes por plataforma.
- **Import CSV:** el plaintext en disco es responsabilidad del usuario.
- **Sin red:** tampoco hay comprobación de filtraciones/HaveIBeenPwned.

## Mejoras futuras (fuera de MVP)

- SQLCipher opcional o contenedor opaco completo.
- Locales adicionales; wordlist Diceware grande multiidioma.
- Argon2 calibrado al hardware en primer arranque.
- Compresión/streaming de backups grandes.
- Integridad de cabecera con firma adicional.
- Modo portable USB documentado.
- Auditoría de reutilización cross-vault (N/A offline multi-vault).
