---
name: staff-registration
description: Alta StaffReg por admin, contraseña temporal Argon2id, cambio forzado, sin logs de secretos. Usar en fase 3 StaffReg.
---

# staff-registration

## Pasos
1. Comando `/staffreg create <nick> <password>` con `STAFFREG.CREATE.PERMISSION` + step-up si `REQUIRE-STEP-UP`.
2. Hash Argon2id (`STAFFREG.PASSWORD.ALGORITHM`); min length desde config.
3. Flag `MUST-CHANGE-ON-FIRST-LOGIN`; forzar cambio en primer `/staff login`.
4. Auditoría: actor, nick, uuid — **nunca** contraseña.
5. Embed `STAFF-CREATED` vía BRIDGE sin secretos.

## No hacer
- Guardar password en YAML/claro.
- Incluir password en logs, embeds o replies Discord.
- Permitir create sin admin autenticado.
