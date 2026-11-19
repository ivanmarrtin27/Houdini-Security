# Decisiones de diseño — Houdini Security

## DD-1: Estrategia de cifrado de SQLite — **(b) cifrado a nivel de aplicación**

**Decisión:** usar `sqlite-jdbc` (org.xerial) y cifrar con **AES-256-GCM** cada carga útil sensible (y la DEK envuelta en la cabecera). No usar SQLCipher.

**Justificación:**
- Portabilidad: sin drivers nativos ni builds por plataforma (Windows/macOS/Linux).
- Documentable y auditable: el formato de vault está bajo nuestro control (`docs/vault-format.md`).
- Misma API Bouncy Castle / JCE que el resto del producto.
- Maven reproducible en CI sin binarios propietarios.

**Trade-off vs SQLCipher (a):**
| | App-level (b) | SQLCipher (a) |
|--|---------------|---------------|
| Portabilidad | Alta (JAR puro) | Requiere nativos / forks |
| Protección at-rest del fichero DB | Metadatos de esquema/tablas visibles; payloads cifrados | Fichero opaco completo |
| Complejidad ops | Baja | Alta (empaquetado nativo) |
| Cambio master (KEK/DEK) | Solo re-cifrar DEK | Compatible, pero acoplado al driver |

**Mitigación del trade-off:** payloads de entradas cifrados; nombres de carpetas/etiquetas en claro documentados como limitación en el threat model; permisos de fichero 600 en POSIX; wipe de claves al bloquear.

**Preferencia segura por defecto:** (b) — correcta para un MVP offline multiplataforma sin cadena nativa.

## DD-2: Jerarquía KEK / DEK

- Master password → KDF (Argon2id por defecto, PBKDF2 opcional) → **KEK** (256-bit).
- **DEK** aleatoria 256-bit; envoltorio AES-GCM con KEK; verificación del master = éxito del unwrap autenticado (no hash de la master).
- Cambio de master: re-derivar KEK nueva y re-envolver la misma DEK (sin re-cifrar todas las filas).

## DD-3: UI y estética

- AtlantaFX (claro/oscuro) + CSS propio: primario negro, secundario verde.
- i18n ResourceBundle, locale por defecto `es`.
- MVVM + `Task`/`ExecutorService` para KDF y lotes fuera del hilo UI.

## DD-4: Módulos JPMS

- **Omitido en v1:** sqlite-jdbc / Bouncy Castle / AtlantaFX como automatic modules complican `module-info` reproducible.
- Ejecución classpath vía `javafx-maven-plugin` (más portable). Revisitar JPMS en una iteración posterior.
