# MEMORY — IoriCore (canónico corto)

Actualizado 2026-11-07 (loop autónomo + repository-auditor).

## Ownership
- **Proxy** = IoriVelo + IoriBungee: friends, Redis, SQL/Mongo, staffchat, MOTD, webhooks, JDA (único).
- **Core** = IoriCore-Bukkit: staffmode, ranks, menús, visual, JoinMOTD, essentials, NMS, punish UI/write.

## Proceso agente
- Regla `85-autonomous-dev-loop` · anti-dup `agents/repository-auditor.md` · higiene `post-task-hygiene`.
- 2026-11-07: anti-dup P0 + StaffAuth legacy eliminado; Bukkit sin JDA (BRIDGE); deploy IoriVelo.

## Estado
- R1–R9 + oleadas P0/P1. Deploy: IoriVelo → Proxy; IoriCore-Bukkit → Lobby+Desktop.
- Legacy UI **1.12.2 / 340**.

## Gaps abiertos
P0: vanish/freeze/[S]/skin-ip/chatgame-economy. P1: menús IoriCTW, staffreg, storage/, punish menu.  
Docs drift IoriProxy→IoriVelo (OPEN-QUESTIONS A2). **WONTFIX:** proxy-common · rename ioriproxy.*.

## Refs
`.cursor/INDEX.md` · `_refs/*` · `docs/plan/PHASE-STATUS.md`
