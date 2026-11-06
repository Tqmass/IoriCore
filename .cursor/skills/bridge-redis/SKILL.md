---
name: bridge-redis
description: Bridge Redis Core→proxy JDA (CHANNEL-IN/OUT), eventos firmados, cola, idempotencia. Usar en fase 6 StaffReg.
---

# bridge-redis

## Pasos
1. Core publica en `BRIDGE.CHANNEL-IN`; proxy responde en `CHANNEL-OUT`.
2. `SIGN-EVENTS` + secreto `SECRET-ENV`; ignorar no firmados.
3. Cola `QUEUE-MAX` / `DROP-AFTER-SECONDS` si JDA cae.
4. Payload pequeño; idempotencia por event id.
5. Fail closed: sin Redis → staff restringido.

## No hacer
- Segundo JDA en Core.
- Enviar secretos/OTC/password en el payload.
- Bloquear event loop con Redis sync.
