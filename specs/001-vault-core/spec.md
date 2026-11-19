# Spec 001 — Vault Core (MVP)

Estado: aprobada

## Contexto y objetivo
Entregar el MVP completo de Houdini Security: bóveda offline cifrada con CRUD de secretos, generador, auditoría, autolock, clipboard y backup, según brief aprobado.

## Usuarios
Persona que gestiona contraseñas, notas y API keys en un solo dispositivo sin nube.

## Historias de usuario
- HU-1. Como usuario, quiero crear y abrir una bóveda con master password para guardar secretos con seguridad.
- HU-2. Como usuario, quiero CRUD de contraseñas, notas y API keys con carpetas, etiquetas, favoritos y búsqueda.
- HU-3. Como usuario, quiero generar contraseñas/passphrases seguras y ver su fortaleza.
- HU-4. Como usuario, quiero una auditoría local accionable (débiles, repetidas, antiguas, API keys por caducar).
- HU-5. Como usuario, quiero autolock, limpia de portapapeles y cambio de master password sin re-cifrar todas las entradas.
- HU-6. Como usuario, quiero exportar/importar backup cifrado e importar CSV con advertencia.

## Requisitos funcionales
- RF-1: CUANDO el usuario crea una bóveda con master password válida, EL SISTEMA genera DEK, envuelve con KEK derivada y persiste cabecera + DB.
- RF-2: CUANDO el usuario abre una bóveda con master correcta, EL SISTEMA desenvuelve la DEK y desbloquea; SI la master es incorrecta, ENTONCES rechaza con error tipado y backoff.
- RF-3: EL SISTEMA permite CRUD de PasswordEntry, SecureNote y ApiKeyEntry con carpetas, etiquetas, favoritos y búsqueda en memoria tras desbloqueo.
- RF-4: EL SISTEMA ofrece generador (longitud, clases de caracteres, excluir ambiguos, passphrase con wordlist) usando SecureRandom.
- RF-5: EL SISTEMA calcula fortaleza (entropía + patrones) y auditoría local (débiles, repetidas, antiguas >N días, API keys por caducar) con score y lista.
- RF-6: MIENTRAS la app tiene un secreto en el portapapeles, EL SISTEMA lo limpia tras N segundos (default 20) solo si el contenido sigue siendo el de la app.
- RF-7: CUANDO hay inactividad configurada o lock manual, EL SISTEMA bloquea, limpia DEK/vistas/portapapeles de la app.
- RF-8: EL SISTEMA exporta/importa backup cifrado e importa CSV mostrando advertencia de plaintext.
- RF-9: CUANDO el usuario cambia la master password, EL SISTEMA re-envuelve la DEK con la nueva KEK (sin re-cifrar filas).
- RF-10: EL SISTEMA opera 100% offline (sin telemetría, updates ni favicons de red).

## Requisitos no funcionales
- RNF-1: AES-256-GCM nonce 96-bit único, tag 128; Argon2id ≥64MiB ≥3 iter parallel 2–4; PBKDF2 ≥600k; salt ≥16.
- RNF-2: Secretos en char[]/byte[]; wipe en finally; sin secretos en logs/toString.
- RNF-3: UI en español; código en inglés; tema negro/verde profesional.
- RNF-4: Capas crypto/storage/domain/service/ui; excepciones VaultLockedException, InvalidMasterPasswordException, CorruptedVaultException.

## Casos límite
- Master incorrecta repetida → backoff.
- Ciphertext manipulado → CorruptedVaultException / fallo autenticación.
- Lock con diálogo de generador abierto → cerrar/limpiar estado sensible.
- CSV mal formado → error claro sin escribir parcial corrupto.

## Fuera de alcance
Nube, sync, extensiones, móvil, 2FA unlock, autofill SO.

## Criterios de finalización
- `mvn test` / `mvn compile` OK.
- Docs de formato, decisiones y threat model presentes.
- Flujos MVP usables en UI.

## Dudas abiertas
Ninguna bloqueante (brief aprobado).
