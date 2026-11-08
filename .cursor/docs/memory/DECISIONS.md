# DECISIONS

## 2026-11-01 — Layout YAML Bukkit (i18n-first)
- **Menus:** `messages/menus/{en,es}.yml` (no `menus.yml` raíz).
- **Formats:** `messages/formats/{en,es}.yml` + `messages/legacy/formats/{en,es}.yml` (protocol ≤340).
- **Easter eggs:** settings en `settings.yml` → `EASTER-EGGS:`; defs en `easter-eggs.yml` (coords/reward); textos UI en messages.
- **Ranks:** `ranks.yml` + futuro SQL scope GLOBAL|SERVER (`RANKS.STORAGE`).
- **Meta:** `commands.yml` + `permissions.yml`.
- Boot: `BukkitLogger.banner` (RoyalCTW). Discord JDA solo en **proxy**.

## 2026-11-01 — Módulo Bukkit (no /core/)
- Backend Spigot/Bukkit vive en **`bukkit/`** · packages `us.iorilabs.bukkit.*`.
- **`paper/`** solo si hace falta API Paper-only (futuro). NMS sigue en `nms/v*`.
- Artifact: `IoriCore-Bukkit-<ver>.jar`. Semver en `build.gradle.kts` `version` (sin SNAPSHOT en Desktop).
- `package.json` / lock = **solo Prettier** (`ioricore-tooling`), no forma parte del plugin.

## 2026-11-01 — Abstraction (no “NMS bridge”)
- Nombre canónico como AnvilGUI: paquete **`abstraction`**, interfaz **`VersionWrapper`**, loader **`VersionMatcher`**.
- Módulos por revisión: `nms/v1_8_R3` … (igual que WesJD). Ref: https://github.com/WesJD/AnvilGUI/tree/master/abstraction
- NMS **solo Core**; proxy nunca.

## 2026-11-01 — Core JVM multi
- Core bytecode **17** (alineado con `:common` / Adventure / switch). Corre en JVM **17 / 21**.
- API compile: Spigot 1.8.8 local (multi-versión runtime). Java 8/11 server JVM: pendiente split o downgrade common.
- Proxy: Java latest del proxy (17+).

## 2026-11-01 — Core commands = CommandParent
- Core usa el mismo framework `CommandParent`/`CommandArgument`/`CommandSource` (common).
- Bridge Bukkit: `BukkitCommandBridge` + `BukkitCommandSource`.
- Mensajes solo en `messages/{en,es}.yml` — cero strings hardcodeados al jugador.

## 2026-11-01 — Rank core / menús / eggs / logs (ideas Helium)
- **Ranks:** menú lista + tooltip (id, prefix/suffix, priority, parents, staff/media, perms) · click edit. Soft-hook LP. **No** copiar Helium/Plasma.
- **Menús:** fillers + personalización máxima estilo **IoriCTW** (nuestros decoradores), no skin Helium.
- **Easter eggs:** regalos/heads clicables, title/particles, progreso por jugador, eventos temporales.
- **Logs:** por jugador (`chatlogs` / `cmdlogs`) con header Player/Total/Generated + bloques por fecha + server-id + rank + ALLOWED/BLOCKED. Además de `/logs/` agregados.
- Refs img: `15-rank-founder-menu`, `16-easter-egg-gift`, `17-chatlogs-per-player`, `18-cmdlogs-per-player`.

## 2026-11-01 — H2 NMS modules naming
- Gradle sustituía `:nms:api` → `:api`. Proyectos `:nms-api`, `:nms-v1_8_R3`, …
- `compileOnly` Spigot 1.8.8 local para title packets.
- Anvil: wrappers por revisión (abstraction).

## 2026-11-01 — H1b packages
- Proxy/common: `me.tqmass.dev.proxy` → `us.iorilabs.proxy`.
- Clases `IoriProxy*` conservadas; artifacts `IoriVelo`/`IoriBungee`.

## 2026-11-01 — H1 Core toolchain (superseded parcialmente)
- Antes Core Java 21 por Paper 1.21.4. **Reemplazado** por release 8 + API Spigot antigua (ver “Core JVM multi”).

## 2026-11-01 — H0 LOCK (OK usuario)
- Renombrar repo → monorepo `IoriCore` (`IoriVelo` / `IoriBungee` / `IoriCore-Bukkit`).
- Punishments propios + soft-hook LiteBans.
- Ranks ideas Helium + LP soft-hook.
- Staff auth proxy solo cambio IP/geo; Discord DM Approve.
- Ban-check: **fail-open con caché**.
- ChatGames solo Core, activable.
- Gender OFF default.
- Geyser soft-hook; formats.yml; `/settings`; donor join Core; logs; H2 fallback.
- Extras: `EXTRAS_BACKLOG.md`.

## 2026-11-01 — H0 unificación
- NMS solo Core; JDA un proceso (proxy); Redis=bus; BD esquema único.

## 2026-11-07 — Autonomous development loop
- Orquestador = agente principal vía regla always-apply `85-autonomous-dev-loop`.
- Anti-duplicación: agente **solo lectura** `repository-auditor` (antes/después de implementar + hook `stop`).
- No sistema paralelo: se reutilizan architecture/qa/java/security/config/staffreg existentes.
- Higiene: `post-task-hygiene` exige pase anti-dup; jar deploy canónico `IoriVelo-*` / `IoriCore-Bukkit-*`.
- Sustituye: flujo ad-hoc “implementar y listo” sin auditoría de dups.

## 2026-11-07 — Anti-dup ownership (P0/P1)
- Staff join: **StaffReg** canónico; StaffAuth legacy solo si StaffReg ausente.
- Core `/staffreg create` → proxy-only; JDA Core siempre BRIDGE-only.
- Discord punish: Redis `persist` → Core escribe `ic_punishments`; proxy no INSERT.
- DDL `ic_discord_links` / sync codes → `SchemaCatalog.shared()`; proxy/core mig 3–4 noop.
- StaffChat Core: local + subscribe; no publish network.
- Bungee: `BungeeStaffRegListener` (parity Velocity).

## 2026-11-07 — StaffAuth legacy OUT + JDA solo proxy
- Eliminado paquete `proxy/staffauth`, `/staffauth`, `StaffAuthConfig`, botones legacy StaffAuth en Discord proxy.
- Auth staff = **solo StaffReg** (módulo `staff-auth` en settings sigue como toggle del feature).
- Bukkit: sin dep JDA; `DiscordBotService` BRIDGE-only; DM/embeds vía Redis → proxy.
- Conservado: `StaffAuthEnforce*` (Core), canal Redis `STAFF_AUTH_SESSION`, plantillas `staff-auth`.

## Abiertas
- Branding plugin id Velocity `ioriproxy` → `iorivelo` (H3).
- Migrar más servicios a holder+Sql split.
