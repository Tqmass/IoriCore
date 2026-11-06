---
name: fails-and-stepup
description: 3 fallos password = ban + Unban Discord; step-up siempre activo; fallback PASSWORD. Usar fase 7 StaffReg.
---

# fails-and-stepup

## Pasos
1. Contar fallos en `WINDOW-SECONDS`; a `MAX-FAILS` → BAN `BAN-DURATION`, revoke sessions, alerta SECURITY.
2. Botón Unban solo roles `COMMANDS.ROLE-IDS`, nunca el afectado.
3. STEP-UP siempre: TTL, cmds (`op`,`lp`,`stop`,…) y nodos `*` / `minecraft.command.op`.
4. Sin Discord → `STAFFREG.UNLINKED.STEP-UP-FALLBACK: PASSWORD`.
5. `ENFORCE-OP-AND-WILDCARD`: retirar OP/`*` hasta auth.

## No hacer
- Flag para desactivar step-up.
- Dejar OP/`*` a staff no autenticado.
