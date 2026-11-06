---
name: discord-buttons
description: Embeds + botones StaffReg firmados (HMAC customId), validación discord_id, fallback canal. Usar en fase 6.
---

# discord-buttons

## Pasos
1. customId = acción + token + HMAC (`BUTTONS.SIGN-CUSTOM-ID`).
2. Un solo uso; `DISABLE-AFTER-USE`; TTL = reto.
3. Solo Discord vinculado (`ONLY-OWNER-CAN-PRESS`); canal fallback: admin ≠ afectado.
4. Acciones: Approve, Deny, No fui yo, Confirmar vínculo, Unban.
5. Responder &lt; 3 s (`deferReply`); editar mensaje tras uso.

## No hacer
- Self-approve.
- Botones sin firma/TTL.
- Intents privilegiados innecesarios.
