# DECISIONS

## 2026-11-04 — Bukkit config wave (partial)
- **`/skin` y `/ip`**: solo Discord (JDA); Core los deja off (`MODULES.SKIN/IP`, `commands.yml`). No duplicar en lobby.
- **Join brand `[S]`**: prefijo aqua **staff-only** (`JOIN.STATUS-LINE` + permiso staff); no broadcast global a jugadores.
- **Vanish**: debe ocultar al vanished en tab, nametag, `/near`, joins y visibilidad Bukkit (Essentials + StaffMode).
- **Freeze**: persiste tras reconnect (Redis/sync + snapshot staffmode); no liberar solo por quit sin regla config.
- **YAML split**: defs pesadas en `data/*.yml`; `database.yml` / `redis.yml` scaffolds Bukkit; toggles en `settings.yml` `MODULES` + `PERSISTENCE`.

## 2026-11-03 — Cierre gaps estructurales (OK usuario)
- **No** extraer `proxy-common`: la lógica compartida vive en `common/`; duplicar velocity↔bungee **está bien**.
- **No** renombrar permisos/canal `ioriproxy.*` → es legado aceptado; no migrar.
- NMS **26.x**: módulo único `nms/v26_R1` (Mojang mappings, CraftBukkit unversioned) para 26.1/26.2/26.3; compile JDK 25 + jars en `Spigot Versions`. Embebido en `META-INF/ioricore-nms/v26_R1.jar` (Shadow/ASM no soporta classfile 69; `VersionMatcher` lo carga con `URLClassLoader`).

## 2026-11-04 — Staffreg + Discord JDA (Core)
- `/staffreg` exige `MODULES.DISCORD` + bot ready; genera OTC via `DiscordLinkService` → Discord `/staffreg` o `/sync`.
- Login Approve/Deny por MD solo si JDA Core escucha Redis `STAFF_AUTH_REQUEST` (proxy publica; proxy JDA gated off).
- `presets.yml` = easter eggs; `join-effects.yml` = join effects (no mezclar).
- Nametag `[Staff Mode]` solo con Apollo disponible.

## 2026-11-03 — Deploy diferido
- **No desplegar jars** (Proxy/Lobby/Desktop) hasta cerrar fases R4–R9 + integraciones. Solo compile/shadowJar local para verificar. Deploy = paso final único. *(Superado: deploy oleada final hecho la misma noche.)*

## 2026-11-03 — OVERRIDE usuario (post Claude R1–R2)
Corrige decisiones previas del mismo día y del agente Claude. Fuente de verdad = este bloque.

### Ownership Proxy vs Core
- **Proxy (`velocity/` + `bungee/`)**: se queda como IoriProxy — friends, network, Redis, MySQL/H2/**Mongo soft**, staffchat proxy, MOTD proxy, webhooks Discord, presencia, report/helpop de red. **No** meter features de juego (staffmode hotbar, ranks, menús lobby, JoinEffects, chat format Alonso).
- **Core (`bukkit/`)**: StaffModeX `/staff`, Helium ranks/tags/coinshop, punishments, JoinMOTD/PictureLogin, Darling scoreboard/TAB/nametag, menús YAML modern+legacy, chat formats Alonso-style, sounds, soft-hooks LP/LiteBans/Apollo, JoinEffects, disguise si Helium/Darling lo tienen, **Discord JDA** (bots/canales multi).
- Comparar siempre refs en `.cursor/_refs` (Helium, Darling, IoriCTW, StaffModeX, EssentialsLite, CoreDub, ChatGames, PictureLogin, JoinEffects, IoriLib) y llevar lo mejor.

### Storage
- **MongoDB VUELVE** como opción soft/alternativa (Claude la retiró sin OK). Proxy: `DATABASE: H2|MYSQL|MONGODB|NONE`. Core: `STORAGE.BACKEND: h2|mysql|mongo` + `STORAGE.MONGO.*` soft (compileOnly / jar externo). SQL (MySQL/H2) sigue siendo el camino principal para esquema `ic_*` compartido.
- MySQL/MariaDB principal de producción; H2 fallback local.

### Discord
- **JDA vive en Core Bukkit** (varios bots / canales por backend). Proxy: webhooks + bridge Redis; staff-auth DM se migrará a Core cuando el bot Bukkit esté listo. Snowfall / refs GitHub = integración JDA.
- Config YAML estilo IoriCTW (UPPERCASE, separadores `# ***` / `# ────`).

### Legacy / NMS / i18n visual
- Corte **LEGACY = 1.12.2 (protocol 340) HARDCODED** — no configurable en YAML. Quitar `LEGACY-MAX-VERSION` como knob (dejar comentario histórico OK).
- NMS / abstracción Core: objetivo compat **1.8.9 → 26.3** (antes 1.21.11); mapear wrappers faltantes progresivamente.
- Menús: ficheros independientes `menus/modern/<id>.yml` + `menus/legacy/<id>.yml`; textos i18n; diseño editable estilo IoriCTW + ejemplos HonlyCraft (chatcolor, gamemode, smallcaps fsymbols).
- Chat formats: modern (hex/RGB/MiniMessage) y legacy (`&` codes) en `chat/formats/` separados, sin transformaciones raras.
- JoinMOTD 1.8.9 + PictureLogin modern; diseños HonlyCraft en `docs/refs`.
- Monedas UI: Royales / Créditos / Gemas. Brand network: **RoyalCTW**.

### Extras backlog confirmados
- JoinEffects (wool volcano, rocket, fireworks…) módulo Core — ref `.cursor/_refs/JoinEffects`.
- Permisos editables (`permissions.yml` / `commands.yml`).
- Sonidos personalizables (`sounds.yml`).

## 2026-11-03 — Usuario (prompt auditoría funcional) — PARCIALMENTE SUPERSEDIDO
- **Java 17 bytecode en todo**, incluidos servidores 1.8.9. Java 8 se estudiará más adelante (cierra A4).
- **Menús: dos implementaciones independientes** Modern (1.16+) y Legacy (1.8.9).
- **No crear `proxy-common` automáticamente**; preferir `common/` si no mete deps incorrectas (A3 abierta).
- ~~MongoDB retirada~~ → **ANULADO** por OVERRIDE arriba (Mongo soft restaurado).
- Referencias: Helium ranks · StaffModeX staffmode · IoriCTW menús · ChatGames · CoreDub staffreg · PictureLogin MOTD · Darling scoreboard/TAB · JoinEffects.

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

## Abiertas
- Branding plugin id Velocity `ioriproxy` → `iorivelo` (H3).
- Migrar más servicios a holder+Sql split.
