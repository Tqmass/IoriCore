---
name: ioricore-architecture
description: Ownership Proxy vs Core, dependencias entre módulos Gradle y decisiones de arquitectura de IoriCore. Usar antes de crear una feature, mover código entre módulos, añadir dependencias o cuando haya duda de dónde vive algo.
---

# ioricore-architecture

## Antes de escribir código
1. Buscar si ya existe: `Grep` en `velocity/` (proxy) y `bukkit/` (core) + `.cursor/docs/plan/MODULE-INVENTORY.md`.
2. Identificar el dueño con la tabla. Si no encaja o choca con docs → `OPEN-QUESTIONS.md`, no decidir.
3. Alcance mínimo. Cambio estructural (módulo nuevo, mover paquetes, renombrar API) → plan + OK.

## Ownership (master prompt + PLAN.md)
| PROXY (velocity/bungee) | CORE (bukkit) | COMPARTIDO (common/api) |
|---|---|---|
| routing, maintenance, MOTD lista, queue, find/presence, announcements, donations/stream, staffchat/adminchat, friends, reports/helpop, alts/dupeip, analytics, Discord JDA, staff auth gate, ban-check al entrar, `/lobby` | spawn/setspawn, scoreboard, TAB/nametag, menús, economy, tags/coinshop, staffmode, freeze, chatgames, PictureLogin/join MOTD, bossbar tips, perfiles, ranks, easter eggs, logs, essentials, NMS/visual, enforce staff auth | contratos, DTO, enums, eventos, command framework, utils, hooks (interfaces en `api`), canales/tipos de mensaje |

Punishments: escritura + UI en Core, enforce join en Proxy, misma tabla `ic_punishments`.

## Dependencias permitidas
```
bukkit → common, api, nms-api, nms-v*      velocity/bungee → common, api
common → adventure (sin plataforma)         api → nada (release 8)      nms-v* → nms-api
```
Prohibido: plataforma en `common`/`api`; Bukkit/NMS en proxy; JDA en bukkit; proxy ↔ bukkit directo (solo Redis / plugin messaging / BD).

## Estado real vs objetivo
- `proxy-common` **no existe**: la lógica proxy está duplicada en velocity y bungee (idéntica). Hasta que se apruebe su extracción, cualquier cambio en `velocity/src/main/java/us/iorilabs/proxy/<feature>` se replica **idéntico** en `bungee/…` (ver rule 60).
- `common` usa paquete `us.iorilabs.proxy.*` (objetivo `us.iorilabs.common`, pendiente A14).
- Detalle: `.cursor/docs/ARCHITECTURE.md`. Decisiones: `.cursor/docs/memory/DECISIONS.md`.

## Comunicación entre lados
Redis pub/sub (`ProxyEventType`) para fan-out de red · plugin messaging `ioricore:main` (`PluginMessageType`) para acciones locales autenticadas · BD para estado persistente. Nunca polling SQL.
