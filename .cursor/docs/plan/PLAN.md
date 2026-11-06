# PLAN H0 — IoriCore + IoriVelo/IoriBungee (unificación)

**Estado:** H0–H5 ✓ · **W1/W2 en curso** (Essentials Core, FX, chat, hooks, staffmode/settings) · Coinshop + Discord JDA cmds = **FINAL** · Inventario: `MODULE-INVENTORY.md`  
**Repo:** este directorio renombrado a producto `IoriCore` · **Core ref:** `Downloads\IoriCore\IoriCore`  
**Paquetes destino:** `us.iorilabs.{proxy,bukkit,common,api}` · Plugins: `IoriVelo` / `IoriBungee` / `IoriCore-Bukkit`

## Precedencia
1) `.cursor/rules|skills|agents` actuales  2) Darling (`me.vals.darling`)  3) este plan  
Choque: gana el menor nº. Regla vigente: **NMS nunca en proxy** (`30-nms-versiones`).

## Monorepo propuesto (H1)
```
IoriCore/   (nombre producto; carpeta monorepo)
  common/          us.iorilabs.common   # cmds, menus, i18n, yaml, utils, sound/title/actionbar
  api/             us.iorilabs.api      # contratos + DTOs red (opcional si hay API pública)
  proxy-common/    us.iorilabs.proxy    # lógica proxy sin Velocity/Bungee
  velocity/        IoriVelo adapter
  bungee/          IoriBungee adapter
  bukkit/          us.iorilabs.bukkit   # Spigot/Bukkit features
  paper/           (opcional futuro)    # solo APIs Paper-only
  nms/v1_8_R3 … v1_21_R3               # solo abstraction por revisión
```
Migrar código IoriProxy existente → adapters. Estilo Darling + Prettier/Lombok.

## Ownership (anti-duplicado)

| Dominio | Dónde | Notas |
|---------|-------|-------|
| Routing, maintenance, MOTD proxy, queue, find/presence, announcements, donations/stream, staffchat/adminchat, friends pairs, report/helpop tickets, alts/dupeip net, analytics | **PROXY** | Ya en IoriProxy → conservar |
| setspawn/spawn, scoreboard/TAB/nametag, menus, economy, tags/coinshop, staffmode hotbar, freeze local, chatgames, picturelogin MOTD join, bossbar tips, dubilets | **CORE** | NMS/visual aquí |
| Staff auth (register/login) + strip perms + blind/immobile | **CORE enforce** + **PROXY gate** | Hash Argon2/BCrypt; pass nunca en logs |
| Punishments ban/mute/kick/warn/notes + kick screens | **SHARED-DB** write por módulo dueño; enforce join en PROXY | Soft-hook LiteBans opcional |
| Discord JDA (staff↔MC, chat global opcional, /sync OTC, webhooks Visage) | **PROXY (único proceso JDA)** | Backends → Redis events |
| Profile paint (/profile ≈ seen+friends+playtime) | **PROXY data + CORE/PROXY cmd** | Sin gender por ahora |
| /lobby | **PROXY** (vestíbulo config) | Core NO setlobby |
| Redis | bus tiempo-real (ban kick, presence, gbc) | **BD no es bus** |

## Reuse / licencia
- **Darling / IoriCTW / IoriCTWL / CoreDub / HonlyStaff / IoriProxy:** patrones + código propio OK.  
- **Helium / Plasma / BuiltByBit / pDiscord / LiteBans themes:** **solo ideas/colores**; reescribir.  
- **AnvilGUI (WesJD):** API pública OK si licencia MIT compatible.  
- **ChatGames-2.9.1.jar:** estudiar comportamiento; **no** meter decompile en repo (`_decompiled` solo si licencia lo permite).  
- **IoriCore DL:** keep stores/economy skeleton; **discard** report/helpop/find/gbc (dup proxy).

## Abstraction (Core) — no llamarlo “NMS bridge”
Como AnvilGUI: `abstraction` + `VersionWrapper` + `VersionMatcher` + módulos `v1_8_R3`…  
Uso: anvil input, legacy title/bossbar, tab/nametag edge. Preferir Bukkit/Adventure cuando baste.  
Ref: https://github.com/WesJD/AnvilGUI/tree/master/abstraction

## Core JVM
Bytecode **Java 8** → arranca en JVM 8/11/17/21. Proxy = Java del proxy (17+).

## i18n + doble diseño
- Idioma: `messages/{en,es}.yml` (+ legacy `messages/legacy/{en,es}.yml` como Proxy).  
- Diseño: claves `*.MODERN` / `*.LEGACY` o archivo legacy; protocol ≤340 → legacy.  
- Paleta sanciones 1.16+: rojo/blanco/negro; alerts cian/rosa/amarillo (refs `docs/refs/img/12*`).  
- Menús 1.16+: estilo “DUELOS ★” (`06-menu-duelos-style.png`).  
- Scoreboard/TAB 1.16+: HonlyCraft (`01*`). MOTD join 1.8–1.12: PictureLogin + cara (`08*`). Bossbars tips opcional off-by-default (`07*`).

## BD (un esquema MariaDB/MySQL)
`players, friends, profile/seen fields, punishments, notes, reports, tags, tag_owners, discord_links, auth_staff`  
- Un dueño de migraciones (proxy arranque). Pools 3–5/instancia. User BD mínimo privilegio (`auth_staff` solo proxy-auth).  
- Caché + invalidación Redis; bans: fail-open con cache + log (confirmar en H1).

## Bus mensajes
Preferir **Redis pub/sub** para fan-out red (ban, presence, discord bridge). Plugin messaging proxy↔backend **autenticado** solo para acciones locales (staffmode sync, sound). No polling SQL.

## Hitos (stop + OK)
H1 skeleton monorepo+common · H2 NMS · H3 proxy rename+canal · H4 moderation · H5 staff auth/mode · H6 social/profile/pm · H7 Discord JDA · H8 visual · H9 tags+chatgames · H10 prettier/security/QA

## Riesgos
Solape cmds report/helpop/staffchat Core vs Proxy · JDA en cada backend = desastre → un solo nodo · Helium copy-paste · secretos en configs antiguas · Apollo soft-only.

## Preguntas abiertas (necesitan tu OK)
1. ¿Monorepo nuevo `IoriCore/` fuera de `Proxy/`, o renombrar este repo?  
2. ¿Punishments propios o soft-hook LiteBans + UI propia?  
3. ¿Fail-open o fail-closed si cae BD en login ban-check?  
4. ¿Staff auth también obligatorio en proxy antes de backends?  
5. ¿ChatGames: ¿feature Core-only o también network via Redis?

## Refs imagen
`.cursor/docs/refs/img/` — 01 scoreboard, 02 webhook, 05 2FA, 06 menús, 07 bossbars, 08 MOTD, 12 ban screens, 13 sync Discord, 14 PM, **15 ranks**, **16 easter-egg**, **17 chatlogs/player**, **18 cmdlogs/player**.
