---
name: enforcement-events
description: Eventos a cancelar en staff restringido, whitelist /staff*, strip permisos keep rank. Usar fase 8 StaffReg.
---

# enforcement-events

## Cancelar (restringido)
- move, interact, inventario, drop/pickup, daño, chat, teleport
- comandos excepto whitelist `/staff*` (login/register/oauth según diseño)

## Pasos
1. Aplicar en proxy y/o backend según ownership.
2. Strip permission nodes; conservar rank tag display.
3. Timeouts challenge; fail closed si stores caídos.
4. Listar exhaustivo en PR de fase 8.

## No hacer
- Whitelist amplia de comandos.
- Conservar `*` / OP sin auth.
