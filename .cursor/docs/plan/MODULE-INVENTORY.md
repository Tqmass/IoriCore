# IoriCore — Inventario E2E de módulos (revisión)

**Brand:** RoyalCTW · **Paquetes:** `us.iorilabs.{bukkit,proxy,common,api,abstraction}`  
**i18n:** `messages/{en,es}.yml` + `messages/legacy/{en,es}.yml` · layouts `messages/menus|formats/` (+ legacy formats)  
**Toggles:** `settings.yml` → `MODULES.*` · textos NUNCA hardcode  
**Fuera de esta oleada (FINAL):** Coinshop completo · comandos Discord de sanciones/JDA embeds avanzados

---

## Ownership (quién vive dónde)

| Dominio | Dueño | Notas |
|---------|-------|-------|
| Routing, queue, MOTD proxy, maintenance proxy, friends, alts/dupeip, find/presence, donations/stream, staffchat/adminchat **network**, `/ac` staff request **network** | **PROXY** | Ya IoriProxy |
| Report / HelpOp / Request | **Por modalidad** (plugin modo o Core toggleable) | No duplicar tickets en proxy si el modo los posee |
| Spawn/setspawn, world mgmt, essentials cmds, scoreboard/TAB/nametag, menus, staffmode, freeze, chat format, tags, economy, chatgames, JoinMOTD/PictureLogin, player settings, easter-eggs, ranks UI | **CORE (Bukkit)** | Abstraction/NMS aquí |
| Punishments write + screens | **SHARED-DB** · enforce join PROXY · UI/cmds CORE | Soft-hook LiteBans |
| Staff geo-auth | PROXY gate + CORE enforce | JDA approve = H7 FINAL |
| Discord JDA (único proceso) | **PROXY** | Webhooks siguen; bot puede reenviar |
| Redis bus | PROXY (+ CORE publica eventos) | BD ≠ bus |

---

## Infra transversal (siempre)

| Pieza | Contenido E2E |
|-------|----------------|
| **Command framework** | `CommandParent` / `CommandArgument` (common) + `BukkitCommandBridge` · YAML `commands.yml` + `permissions.yml` |
| **Menu framework** | `Menu`/`MenuItem`/`MenuConfig` · layouts en `messages/menus/{lang}.yml` · textos en messages |
| **Anvil input** | `VersionWrapper.openAnvilInput` · helper `AnvilInput` · 1.8 + 1.21 real |
| **Sounds centralizados** | `SoundService` / `sounds.yml` ids lógicos → XSound/Legacy mapping 1.8–1.21 |
| **Title / ActionBar** | Via `VersionWrapper` (+ Adventure cuando baste) |
| **Language** | Modern + Legacy por protocol ≤ `LEGACY-PROTOCOL-MAX` |
| **Storage** | H2 default · MySQL · **Mongo soft** (toggle) · fail-open bans |
| **Redis** | Bus presencia/punish/discord (proxy) |
| **Logger/Banner** | `BukkitLogger` / `ProxyLogger` RoyalCTW |
| **Prettier + Lombok** | Obligatorio holders `@Getter @Accessors(fluent=true)` |

---

## Módulos CORE — detalle extremo a extremo

### 1. `essentials` (prioridad W1)
- **Cmds (objetivo EssentialsLite + Helium/CoreDub ideas):** heal, feed, fly, god, gamemode/gm, speed, healall, clear/ci, invsee, enderchest/ec, workbench/craft, anvil (gui), enchant, repair, hat, more, nick, realname, near, ping, seen (local stub→proxy data), suicide, kill, burn, extinguish, lightning, time, day/night, weather, sun/rain/thunder, ptime, pweather, vanish/v, teleport/tp/tphere/tpall/tppos, tpa/tpahere/tpaccept/tpdeny/tpcancel, back, top, jump, thru, home/sethome/delhome/homes, warp/setwarp/delwarp/warps, spawn/setspawn, kittycannon (off), socialspy, mail (stub), ignore, msg/r/tell, me, broacast/bc, sudo, powertool, item/i, give, skull, loom/grindstone/smithing (modern only stubs), compass, getpos, world, disposal, loom…
- **Persistencia toggle:** `PERSISTENCE.HOMES|WARPS|BACK|INVENTORY|STATS` on/off por servidor
- **Config:** `settings.yml` ESSENTIALS.* · cooldowns · sounds ids
- **Menus:** opcional warps GUI desde `messages/menus`

### 2. `spawn` / world
- `/spawn` `/setspawn` (perm) · spawn on join opcional
- **WorldManagement:** cancel mob spawn, weather, fire spread, leaf decay, explosions, hunger, fall damage, block break/place (flags YAML por world)
- Listeners finos · async nowhere blocking

### 3. `chat` (formats)
- `messages/formats/{lang}.yml` + `legacy/formats`
- Parts: tag emoji style `&8(&f&ki&r&c&l❤&f&ki&r&8)` + rank + name + » + message (ref img 21)
- Hover/click · Vault/LP prefix · protocol pick
- Soft PAPI placeholders when hooked

### 4. `ranks` (Helium-style UI)
- YAML + SQL GLOBAL\|SERVER · RankList + **RankEdit** (name, weight, prefix/suffix, color, bold/italic, default/staff/admin/op/visible) · submenus clickeables
- Editor anvil para strings · soft LP sync later

### 5. `punishments`
- ban/tempban/mute/tempmute/warn/kick/notes · SQL `ic_punishments`
- **RED theme** mensajes (LiteBans Froggy inspo, rewrite)
- Soft LiteBans mirror opcional · Proxy BanCheck fail-open

### 6. `staffmode`
- Toggle staffmode · hotbar items YAML menus · vanish sync · freeze · inspect
- Nametag `[Staff]` · Lunar glow 1.8.9+ soft Apollo hook
- Disable numbers Lunar toggle

### 7. `staffauth` (enforce)
- Blind/immobile hasta `STAFF_AUTH_SESSION` · ya skeleton

### 8. `scoreboard` / `tab` / `nametag`
- Toggle MODULES.SCOREBOARD · boards YAML modern/legacy
- Apollo Lunar options soft

### 9. `joinmotd` / `picturelogin`
- 1.8–1.12: text MOTD join center/separators
- 1.16+: PictureLogin-style face + decorations (refs img 08)
- Geyser bedrock / premium / nLogin status lines

### 10. `player-settings` (`/settings`)
- Menú + submenús: sounds, particles, language, premium display, change password (Anvil → nLogin API soft)
- Persist PlayerData toggles

### 11. `chatlogs`
- Per-player chat + command logs · ya

### 12. `eastereggs`
- settings timings · eggs YAML · titles messages · ya

### 13. `tags` (W2+, shop FINAL)
- Equip tag · chat part · storage · coinshop link **FINAL**

### 14. `economy` (stub→real mid)
- Vault soft · balance/pay/eco admin · persistence toggle

### 15. `coinshop` **FINAL**
- Menús IoriCTW-style · buy tags/ranks cosmetics

### 16. `chatgames` 
- Core-only · toggle · later polish

### 17. `hooks` soft
- LuckPerms · LiteBans · Vault · Geyser · Apollo · nLogin · PAPI (si hace falta)

### 18. `skin` (`/skin`)
- Discord-like modes: Face, Head, Body Front, Body 3D, Full · bukkit cmd + optional Discord later
- Usa API pública skins (Mojang/CraftHead) async

### 19. `ip` (`/ip`)
- Staff: IP, country/geo stub, alts count, bedrock?, nLogin?, first/last seen

### 20. `messaging` plugin
- `ioricore:main` anti-spoof · handshake · staff auth session

---

## Módulos PROXY — resumen

| Módulo | Estado objetivo |
|--------|-----------------|
| Maintenance / MOTD / Server list | Existente |
| Friends / playtime / seen network | Existente + profile paint |
| Alts / blacklist | Existente |
| Staff chat / admin chat / `/ac` | Network |
| Report/HelpOp | Solo si no modalidad; else bus |
| BanCheck join | ✓ skeleton |
| StaffAuth gate + webhook | ✓ · JDA FINAL |
| Discord JDA bot | **FINAL**: ban/mute/… cmds, welcome/boost/store embeds, puede emitir lo mismo que webhooks |
| Redis bus | eventos join/quit/start/stop/punish |

---

## Oleadas de implementación

1. **W1 (ahora):** refs EssentialsLite · Sound/Title/AB · essentials cmds core · spawn/world · chat format 1.8 style · hooks Vault/Apollo/nLogin · punish RED theme  
2. **W2:** staffmode · joinmotd/picturelogin · scoreboard · /settings menus · /skin · /ip · persist toggles  
3. **W3:** ranks editor Helium-deep · tags base · economy Vault · Mongo option  
4. **FINAL:** coinshop · Discord JDA full  

---

## Refs locales

| Fuente | Uso |
|--------|-----|
| `_refs/EssentialsLite/` | Lista cmds + patrones (reescritura propia) |
| Helium zip / SRC | Ideas ranks/menus — **no copy** |
| LiteBans Froggy zip | Colores RED theme — rewrite messages |
| OpenPhoenix / FrozedLib / Sponsor | Solo si aporta patrón limpio |
| apollo-bukkit jar | Soft-hook API pública |
| nLogin docs | Soft-hook password/premium |
| imgs 19–21 | skin modes · rank editor · chat tag |
