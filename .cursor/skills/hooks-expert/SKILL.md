---
name: hooks-expert
description: Soft-hooks for LuckPerms, PlaceholderAPI, LiteBans and similar. Use when integrating external plugins without hard deps.
---

# hooks-expert

## Rules
- Soft-hook only (reflection / optional class presence).
- No PacketEvents.
- No hard dependency unless user asks.
- Fallback vacío/seguro si el plugin no está.
- Secretos nunca en mensajes de error.
