# Discord ownership (OVERRIDE 2026-11-03)

## Decisión
- **JDA en Core Bukkit** (`bukkit/.../discord/**`): bots, slash `/sync`, DMs staff-auth, embeds, multi-bot / multi-canal por backend.
- **Proxy**: webhooks (`webhooks/`) + eventos Redis hacia Core. El bot JDA actual en `velocity|bungee/discord/*` se mantiene temporalmente hasta migrar staff-auth DM; **no ampliar** features JDA en proxy.

## Config
- YAML UPPERCASE estilo IoriCTW (`discord.yml` en Core).
- Mensajes en `messages/{en,es}.yml` + legacy.
- Ref integración: Snowfall / `_refs` + `INTEGRATIONS.md`.

## Flujo staff-auth (objetivo)
1. Proxy detecta cambio IP/geo → publica Redis `STAFF_AUTH_REQUEST`.
2. Core bot DM al Discord vinculado (botones Approve/Deny).
3. Core responde Redis → proxy levanta restricción / aplica deny.
4. Canal Discord = fallback admin, nunca auto-approve del mismo uuid.

## `/sync`
- In-game Core o Proxy emite OTC → Discord slash en **bot Core**.
- Tabla `ic_discord_links` (migración `core` o `shared`).
