# MEMORY — IoriCore (canónico corto)

Actualizado 2026-11-03 noche (oleada final post-auditoría).

## Ownership
- **Proxy** = IoriProxy (V+B): friends, network PM (off default), Redis, SQL/Mongo, staffchat, MOTD, **webhooks**. JDA bot proxy **off** (`DISCORD-BOT: false`).
- **Core** = Bukkit: staffmode, ranks, menús YAML, visual, JoinMOTD, JoinEffects, ChatGames, **Discord JDA**, punishments.

## Estado
- R1–R9 código + oleada P0/P1 + NMS 1.12/16/20 + Redis freeze/vanish.
- Nota ~**8.2/10**. Deploy: IoriVelo → Proxy; IoriCore-Bukkit → Lobby+Desktop.
- Legacy UI **1.12.2 / 340 hardcoded**.

## Gaps abiertos
P0 feedback 2026-11-04: vanish/freeze/[S]/skin-ip/chatgame-economy. Pendiente P1: menús IoriCTW, staffreg, storage/, punish menu, visual.  
**WONTFIX:** proxy-common · rename ioriproxy.*.

## Refs
`.cursor/_refs/*` · `docs/refs/design/` · `docs/plan/PHASE-STATUS.md`
