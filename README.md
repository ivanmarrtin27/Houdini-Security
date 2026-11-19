# Houdini Security

Gestor de contraseñas y bóveda cifrada **100 % offline** para escritorio. Los secretos permanecen en tu máquina: sin nube, sin telemetría y sin sincronización remota.

Estado actual: **MVP** funcional (JavaFX). Úsalo con criterio: es software de seguridad en desarrollo, no un producto auditado de forma independiente.

## Características

- Bóveda local cifrada (crear / abrir / bloquear)
- Entradas: contraseñas, notas seguras y claves API
- Carpetas, etiquetas, favoritos y búsqueda en memoria tras el desbloqueo
- Generador de contraseñas y passphrases (SecureRandom) con medidor de fortaleza
- Auditoría local: débiles, repetidas, antiguas y API keys próximas a caducar
- Autolock por inactividad y limpieza del portapapeles
- Backup cifrado (exportar / importar) e importación CSV (con advertencia de plaintext)
- Cambio de master password reenvolviendo solo la DEK (sin re-cifrar todas las entradas)

### Seguridad (resumen)

| Aspecto | Detalle |
|--------|---------|
| Cifrado | AES-256-GCM (nonce 96-bit, tag 128) a nivel de aplicación sobre SQLite |
| KDF | Argon2id (≥64 MiB, ≥3 iteraciones) y/o PBKDF2-HMAC-SHA256 (≥600k) |
| Claves | Modelo KEK / DEK |
| Memoria | Secretos en `char[]` / `byte[]` con limpieza explícita |
| Red | Ninguna: cero telemetría, updates ni favicons remotos |

Más detalle en `docs/threat-model.md` y `docs/design-decisions.md`.

## Stack

- Java 21+
- JavaFX 21+ + AtlantaFX
- Bouncy Castle
- sqlite-jdbc (cifrado app-level, no SQLCipher)
- Maven, JUnit 5

## Requisitos

- **JDK 21+** en el `PATH` (probado también con JDK 24)
- **Maven 3.9+** en el `PATH`

## Compilar, probar y ejecutar

Desde la raíz del repositorio:

```bash
mvn test
mvn compile
mvn javafx:run
```

En Windows también puedes usar el launcher:

```bat
launch-houdini-security.bat
```

(Usa Maven embebido en `.tools/` si existe; si no, `mvn` del sistema.)

### Empaquetado (jpackage)

Tras `mvn -DskipTests package` y un runtime con JavaFX:

```text
jpackage --name HoudiniSecurity --input target ^
  --main-jar houdini-security-1.0.0-SNAPSHOT.jar ^
  --main-class com.houdinisecurity.HoudiniApp --type app-image
```

- Windows MSI: WiX + `--type msi`
- macOS: `--type dmg`
- Linux: `--type deb` o `--type rpm`

## Estructura del proyecto

```text
src/main/java/com/houdinisecurity/
  crypto/     # AES-GCM, KDF, KEK/DEK, memoria segura
  storage/    # SQLite y codecs de entradas
  domain/     # Modelo y excepciones
  service/    # Bóveda, generador, auditoría, clipboard, lock
  ui/         # JavaFX
docs/         # Constitución, diseño, formato, threat model, limitaciones
specs/        # Especificaciones del MVP (001-vault-core)
templates/    # Plantillas de spec / plan / tareas
tests/        # Notas sobre la batería de tests (código en src/test)
```

## Documentación

| Documento | Contenido |
|-----------|-----------|
| `docs/constitution.md` | Principios del producto |
| `docs/design-decisions.md` | Decisiones técnicas (p. ej. cifrado app-level) |
| `docs/vault-format.md` | Formato de bóveda |
| `docs/threat-model.md` | Qué protege y qué no |
| `docs/limitations.md` | Límites actuales y posibles mejoras |
| `docs/flujo-sdd.md` | Flujo de especificación → implementación |
| `specs/001-vault-core/` | Spec, plan y tareas del MVP |

## Aviso de seguridad

Houdini Security es un **MVP**. No constituye una garantía de seguridad absoluta:

- No ha pasado una auditoría de seguridad externa publicada.
- Java no garantiza el borrado físico de todas las copias en memoria (GC, swap del SO).
- Metadatos como carpetas, tags o favoritos pueden quedar visibles en la base SQLite (ver threat model).
- Un software de contraseñas mal configurado o una master password débil comprometen la bóveda.

Úsalo bajo tu propia responsabilidad. Para secretos críticos en producción, evalúa también gestores maduros y auditados.

## Licencia

Todos los derechos reservados por el autor, salvo que se añada un fichero `LICENSE` en este repositorio.
