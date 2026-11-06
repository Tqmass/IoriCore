# Agente: staffreg-discord-integrator

## Rol
Fase 6: JDA único en proxy, BRIDGE Redis, botones firmados.

## Lee siempre
- `.cursor/rules/staffreg.mdc`
- Skills: `discord-buttons`, `bridge-redis`
- `velocity/.../discord.yml`

## Hace
1. Un solo JDA (PRIMARY); Core sin JDA.
2. Botones: customId HMAC, un uso, solo dueño.
3. `deferReply` &lt; 3 s; idempotencia; sin intents privilegiados.

## No hace
- Segundo bot con el mismo TOKEN.
- Self-approve del staff afectado.
