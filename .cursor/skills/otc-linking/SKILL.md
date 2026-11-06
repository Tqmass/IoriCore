---
name: otc-linking
description: OTC Discord ≥10 alfanum, hash, TTL, canje atómico, ADMIN-ASSIGN con confirmación del dueño. Usar en fase 4 StaffReg.
---

# otc-linking

## Pasos
1. Generar OTC ≥10 sin caracteres ambiguos; guardar **hash**; TTL `SYNC.CODE-TTL-SECONDS`.
2. Canje `/sync` atómico (un uso); rate-limit `REDEEM-COOLDOWN-SECONDS`.
3. `ONE-DISCORD-PER-ACCOUNT`: rechazar si Discord ya ligado a otra cuenta.
4. `ADMIN-ASSIGN`: ID/@usuario → MD `LINK-CONFIRM` → botón `CONFIRM-LINK` (TTL).
5. Aviso `UNLINKED` si entra sin vínculo; password login si `ALLOW-PASSWORD-LOGIN`.

## No hacer
- Loguear OTC en claro.
- Reutilizar código usado/caducado.
- Vincular sin confirmación del dueño si `REQUIRE-OWNER-CONFIRM`.
