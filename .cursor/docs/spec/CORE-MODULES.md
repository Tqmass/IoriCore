# CORE-MODULES — Inventario funcional IoriCore-Bukkit

> Auditoría 2026-11-03 · solo lectura de código. Raíz código: `bukkit/src/main/java/us/iorilabs/bukkit/` (abreviado `B/`), resources `bukkit/src/main/resources/` (abreviado `R/`).
> Intención original: `.cursor/docs/plan/MODULE-INVENTORY.md` (20 módulos), `memory/EXTRAS_BACKLOG.md`, `memory/DESIGN_BRIEF.md`, `memory/DECISIONS.md`, `memory/PROGRESS.md`.
> Cruces: [COMMANDS](COMMANDS.md) · [PERMISSIONS](PERMISSIONS.md) · [CONFIGURATIONS](CONFIGURATIONS.md) · [MENUS](MENUS.md) · [RANKS](RANKS.md) · [STAFFMODE](STAFFMODE.md) · [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) · [INTEGRATIONS](INTEGRATIONS.md) · [FEATURE-STATUS](FEATURE-STATUS.md) · [PROXY-MODULES](PROXY-MODULES.md)

## 0. Evidencia de pruebas

- No hay tests (`bukkit/src/` solo tiene `main/`).
- `IMAGE-INDEX.md` no tiene ninguna captura RESULT-OK de una feature del Core Bukkit; las capturas "propias" son del proxy (`IMAGE-INDEX.md:103-112`). `4a319cbb` ("arranque H2 OK") no dice si es Core o proxy → NO VERIFICADO.
- `PROGRESS.md:3-11` marca como "Cerrado" casi todo el Core, pero solo cuenta como evidencia de prueba "Jar validado ZIP + Lobby/Desktop" (`PROGRESS.md:11`), es decir, que el plugin arranca. **Ningún módulo llega a `IMPLEMENTED_AND_TESTED`.**

## 1. Arranque y registro

| Paso | Dónde | Notas |
|---|---|---|
| `saveResource` de settings, data/easter-eggs, ranks, data/tags, data/coinshop, data/chatgames, commands, permissions, sounds | `B/IoriCorePlugin.java:90-98` | |
| Carga `settings.yml`, `commands.yml`, `permissions.yml` | `:100-102` | `commands()`/`permissions()` **no los lee nadie** (grep sin usos) → los YAML son un STUB (`R/commands.yml:5` "Stub — el wiring llega con el registry") |
| `CoreLanguage.load(SETTINGS.LANGUAGE, legacyMax)` | `:105-107` | idioma **global del servidor** |
| `CoreSqlStorage.open` (H2/MySQL + Mongo soft) · `SoftRedisPublisher.tryOpen` · `VersionMatcher.load` | `:116-118` | |
| FX: `SoundService`, `TitleService`, `ActionBarService` | `:123-126` | |
| `MenuListener` + `MenuApi.enable` | `:128-129` | |
| Hooks soft Vault/LP/Apollo/nLogin/Geyser | `:131-135` | LiteBans se crea dentro de Punishments |
| `ModuleRegistry.register` ×21 + `enableAll()` | `:149-171` | `enableAll` salta los módulos con `enabled()==false`; si una excepción salta, se loguea y se sigue (`B/modules/ModuleRegistry.java:23-32`) |
| `JoinStatusListener` (fuera del registry, sin toggle MODULES) | `:173` | toggle propio `JOIN.STATUS-LINE` |

- `CoreModule.reload()` existe (`B/modules/CoreModule.java:14-17`), pero **no hay comando que la llame**: `/ioricore` (`/icore`) está declarado en `R/plugin.yml:22-24` y no tiene executor → `NOT_IMPLEMENTED` (no hay reload ni help raíz).
- Comandos: `BukkitCommandBridge.register` necesita la entrada en `plugin.yml` y además se apropia de los alias por reflexión en `knownCommands` (`B/commands/BukkitCommandBridge.java:26-68`). Si el módulo está OFF, su comando sigue en `plugin.yml` sin executor → Bukkit muestra la `usage` por defecto (riesgo de UX / mensajes sin i18n).
- `ModuleId` (`api/.../modules/ModuleId.java`) tiene `GEYSER_HOOK`, `LITEBANS_HOOK`, `DONOR_JOIN`, `VAULT_HOOK`, `APOLLO_HOOK` y `NLOGIN_HOOK` sin `CoreModule` asociado.

## 2. Tabla resumen

| # | Módulo | Toggle `MODULES.*` (default yml / default código) | Clase módulo | ESTADO |
|---|---|---|---|---|
| 1 | Essentials (comandos) | `ESSENTIALS` true/true | `B/modules/essentials/EssentialsModule.java` | PARTIAL |
| 2 | World management | (dentro de ESSENTIALS) | `WorldManagementListener` | IMPLEMENTED_UNTESTED |
| 3 | Chat format | `CHAT-FORMAT` true/true | `chatformat/ChatFormatModule` | PARTIAL → [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) |
| 4 | Ranks | `RANKS` true/true | `B/ranks/RanksModule` | PARTIAL → [RANKS](RANKS.md) |
| 5 | Punishments | `PUNISHMENTS` true/true · `LITEBANS-HOOK` | `B/punishment/PunishmentsModule` | PARTIAL |
| 6 | Staff mode (+freeze) | `STAFF-MODE` true/true | `staffmode/StaffModeModule` | PARTIAL → [STAFFMODE](STAFFMODE.md) |
| 7 | Staff auth enforce | `STAFF-AUTH` false/false | `staffauth/StaffAuthEnforceModule` | PARTIAL |
| 8 | Scoreboard / TAB / nametag | `SCOREBOARD`, `TAB` true/true | `scoreboard/*`, `tab/*` | PARTIAL → [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) |
| 9 | Join MOTD / PictureLogin | `JOIN-MOTD` true/true | `joinmotd/JoinMotdModule` | PARTIAL |
| 9b | Join status (marca staff) | `JOIN.STATUS-LINE` (no MODULES) | `join/JoinStatusListener` | IMPLEMENTED_UNTESTED |
| 10 | Player settings `/settings` | `PLAYER-SETTINGS` true/true | `settings/PlayerSettingsModule` | PARTIAL |
| 11 | Chat logs / cmd logs | `CHAT-LOGS` true/true | `chatlogs/ChatLogsModule` | PARTIAL |
| 12 | Easter eggs | `EASTER-EGGS` false/false | `eastereggs/EasterEggsModule` | PARTIAL |
| 13 | Tags | `TAGS` false/false | `tags/TagsModule` | IMPLEMENTED_UNTESTED |
| 14 | Economy | `ECONOMY` false/false | `economy/EconomyModule` | PARTIAL |
| 15 | Coinshop | `COINSHOP` false/false | `coinshop/CoinshopModule` | PARTIAL |
| 16 | ChatGames | `CHAT-GAMES` false/false | `chatgames/ChatGamesModule` | PARTIAL |
| 17 | Hooks soft | `*-HOOK` true/true | `B/hook/*` | PARTIAL |
| 18 | Skin `/skin` | `SKIN` true/true | `skin/SkinModule` | PARTIAL |
| 19 | IP `/ip` | `IP` true/true | `ip/IpModule` | PARTIAL |
| 20 | Plugin messaging | `PLUGIN-MESSAGING` true/true | `B/messaging/PluginMessagingModule` | PARTIAL |
| 21 | Bossbar tips | `BOSSBAR-TIPS` false/false | `bossbartips/BossbarTipsModule` | IMPLEMENTED_UNTESTED |
| 22 | Gender | `GENDER` false/false | `gender/GenderModule` | STUB |
| 23 | Donor join | `DONOR-JOIN` false (sin lector) | — | NOT_IMPLEMENTED |
| 24 | Profiles / perfiles | — | — | NOT_IMPLEMENTED (no hay código; solo ref visual `bf4de872` en IMAGE-INDEX) |
| 25 | FX (sonidos/títulos/actionbar) | — (siempre) | `B/fx/*` | IMPLEMENTED_UNTESTED |
| 26 | Language modern/legacy | — | `B/language/CoreLanguage` | PARTIAL |
| 27 | Menús (2 frameworks) | — | `B/menu/*`, `B/menu/api/*` | PARTIAL → [MENUS](MENUS.md) |
| 28 | Abstraction / NMS | — | `B/abstraction/*`, `nms/*` | PARTIAL |
| 29 | Storage / Redis soft | `STORAGE.BACKEND`, `REDIS.ENABLED` | `B/storage/*`, `B/redis/*` | PARTIAL |
| 30 | `/ioricore` root + reload | — | — | NOT_IMPLEMENTED |

---

## 3. Essentials (`MODULES.ESSENTIALS`)

**Responsabilidad:** comandos tipo EssentialsLite, spawn, homes, warps, tpa, kits, vanish, ignore, msg/reply, back, nick.
**Registro:** 53 comandos en `EssentialsModule.java:151-205`; listeners `EssentialsListener` y `WorldManagementListener` (`:117-128`). `EssentialsCommand` es la base abstracta (`command/EssentialsCommand.java:13`): `deny()` acepta `ioricore.admin` como bypass (`:33-37`).
**Config:** `settings.yml` `ESSENTIALS.*` (`R/settings.yml:146-151`): `TPA-TIMEOUT-SECONDS`, `MAX-HOMES`, `SPAWN-ON-JOIN`, `SPAWN-ON-DEATH`, `NEAR-RADIUS`. `PERSISTENCE.*` (`:84-93`). Textos en `messages/*.yml` → `ESSENTIALS:` (`R/messages/es.yml:461`).

### 3.1 Subsistemas

| Subsistema | Clases | Persistencia | Comandos | ESTADO | Notas |
|---|---|---|---|---|---|
| Spawn | `spawn/SpawnService.java` | `essentials/spawn.yml` (sin toggle) | spawn, setspawn | IMPLEMENTED_UNTESTED | on-join / on-respawn (`EssentialsListener.java:73-82`) |
| Homes | `home/HomeService.java` | `essentials/homes/{uuid}.yml` · `PERSISTENCE.HOMES` (`:156`) | home, sethome, delhome, homes | IMPLEMENTED_UNTESTED | límite `MAX-HOMES` + `ioricore.essentials.sethome.unlimited` (`SetHomeCommand.java:27-32`); sin límite por rango; I/O síncrona en el hilo principal |
| Warps | `warp/WarpService.java` | `essentials/warps.yml` · `PERSISTENCE.WARPS` | warp (alias warps), setwarp, delwarp (alias deletewarp) | IMPLEMENTED_UNTESTED | sin permiso por warp ni GUI (el inventario la pedía "opcional") |
| TPA | `tpa/TpaService.java`, `TpaRequest` | memoria | tpa, tpahere, tpaccept(tpyes), tpdeny(tpno), tpcancel(tpacancel,tpc) | IMPLEMENTED_UNTESTED | 1 petición entrante por target (sobrescribe), timeout ≥5s (`TpaService.java:17`), sin warmup ni cooldown |
| Back | `EssentialsState.rememberBack` | `essentials/back.yml` · `PERSISTENCE.BACK` | back | IMPLEMENTED_UNTESTED | ⚠ **guarda el YAML entero en cada teleport/muerte en el hilo principal** (`EssentialsState.java:57-61,125-137`) |
| Kits | `kit/KitService.java`, `KitDefinition` | `kits/<kit>.yml` + `kits/cooldowns.yml` · `PERSISTENCE.KITS` | kit(kits), createkit(kitcreate), deletekit(kitdelete) | IMPLEMENTED_UNTESTED | permiso `ioricore.kit.<name>`, cooldown (`KitService.java:59-60`); **sin menú de kits** (la ref de YAML es IoriCTW `kits-es.yml`) |
| Vanish | `vanish/VanishService.java` | memoria | vanish(v) | IMPLEMENTED_UNTESTED | ver: `ioricore.vanish.see`/staff/admin (`:17,102`); ⚠ **StaffMode tiene su propio vanish** (`StaffModeService` `setVanish`) → dos estados distintos |
| Ignore | `ignore/IgnoreService.java` | `essentials/ignore.yml` · `PERSISTENCE.STATS` | ignore | PARTIAL | solo se aplica en msg/reply (`MsgCommand.java:40`, `ReplyCommand.java:47`), **no en el chat público** |
| Msg/Reply | `MsgCommand`, `ReplyCommand`, `EssentialsState.reply` | memoria | msg(tell,w,whisper,message), reply(r) | PARTIAL | ⚠ **no comprueba el mute** (`MuteListener` solo escucha `AsyncPlayerChatEvent`) → un muteado puede usar /msg /r /me; sin socialspy |
| Nick / Realname | `EssentialsState.setNick/applyNick` | `essentials/nicks.yml` · usa `PERSISTENCE.STATS` (`:154`) | nick(nickname), realname | IMPLEMENTED_UNTESTED | cambia displayName y el nombre en la lista (lo recorta a 16) |
| God | `EssentialsState.god` + `onDamage` | memoria | god | IMPLEMENTED_UNTESTED | |

### 3.2 Comandos sueltos (todos registrados, ver [COMMANDS](COMMANDS.md))
heal, healall, feed, fly, god, gamemode (gm, gms, gmc, gma, gmsp), speed, kill (suicide), weather (sun, rain, thunder), time (day, night), top, world, tp (teleport), tphere, tppos (teleportpos), back, nick, realname, repair (fix; `.all`), enderchest (ec, echest), invsee, clear (ci, clearinventory), broadcast (bc, bcast), give (i, item), getpos (coords, position), disposal (trash, disposalchest), workbench (craft, wb, craftingtable), hat, near, ping, me (action), skull (head), ignore. Permisos `ioricore.essentials.<cmd>[.others]`.
- `/invsee` abre el inventario real del objetivo, **editable** sin protección (`InvseeCommand.java:35`).
- `/gamemode`: solo `gamemode` y `.others`; **no hay permisos por modo** (EXTRAS_BACKLOG §essentials-lite los pedía).

### 3.3 Comparativa con EssentialsLite (`.cursor/_refs/lists/essentialslite-commands.txt`)
| Lista EL | IoriCore |
|---|---|
| back, spawn, setspawn, fly, gamemode/gms/gmc/gma/gmsp, repair, kit, createkit, deletekit, warp, setwarp, tpa, tpahere, tpaccept, reply, god, heal, kill, weather, feed, enderchest, home, homes, sethome, delhome, time, top, world, tp, tphere, speed | ✔ existen |
| deletewarp · message · nickname · economy · money | ✔ como alias (delwarp / msg / nick / eco / balance) |
| pay | ✔ en Economy (solo si `MODULES.ECONOMY`) |
| helpop | ✘ en el Core; vive en el proxy (ownership `MODULE-INVENTORY.md:15`) |

### 3.4 Lo que pide MODULE-INVENTORY §1 y no existe (`MODULE-INVENTORY.md:44`)
anvil (gui), enchant, more, seen, burn, extinguish, lightning, ptime, pweather, tpall, jump, thru, kittycannon, socialspy, mail, sudo, powertool, compass, loom/grindstone/smithing, menú de warps, persistencia `PERSISTENCE.INVENTORY` (la clave está en `settings.yml:88`, pero ningún código la lee).

**Pendientes:** I/O asíncrona (back, homes, warps, nicks), aplicar mute en msg/r/me, ignore en el chat, unificar vanish con StaffMode, menú de kits/warps, permisos por modo de gamemode, comandos que faltan.

## 4. World management (`WorldManagementListener`)
- Se registra dentro de Essentials (`EssentialsModule.java:126-128`); no tiene toggle propio.
- Flags **globales** `WORLD.DISABLE-*` (`R/settings.yml:175-184`): mob spawn, clima/trueno, explosiones, hambre, daño de caída, fuego (ignite/burn/spread), leaf decay, romper/colocar (bypass `ioricore.world.bypass`) (`WorldManagementListener.java:34-116`).
- Doc vs código: el inventario pedía "flags YAML **por world**" (`MODULE-INVENTORY.md:51`); el código aplica las flags igual a todos los mundos.
- ESTADO: IMPLEMENTED_UNTESTED (sin soporte por mundo).

## 5. Chat format — resumen
`ChatFormatModule` + `ChatFormatListener` + `ChatFormatService`. Construye el mensaje con las partes `tag, rank, player, separator, message` (`ChatFormatService.java:22`) y el YAML `chat/formats/{en,es}.yml` (+ `legacy/`), elegido por el protocolo del emisor (`:176`). Toma el prefijo y sufijo de Vault/LP y el tag del módulo Tags. **No usa los rangos propios de IoriCore** (ningún módulo fuera de `ranks/` importa `RankService`). Grep sin resultados para hover/click y para chat lock. Detalle → [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md).

## 6. Ranks — resumen
`RanksModule` (`B/ranks/RanksModule.java:30-40`) tiene `RankService` (YAML `ranks.yml` o SQL `ic_ranks` según `RANKS.STORAGE`), `/ranks` (`/rank`) y los menús RankList/RankEdit/RankParents. Crea su propio `SoftLuckPermsHook`, distinto del del plugin. Nadie consume los rangos: no hay attachment de permisos (grep `PermissionAttachment` vacío) ni prefijo en el chat o el TAB. `SoftLuckPermsHook.applyPrefix` está vacío (`B/hook/SoftLuckPermsHook.java:41-43`). Detalle → [RANKS](RANKS.md).

## 7. Punishments (`MODULES.PUNISHMENTS`, `MODULES.LITEBANS-HOOK`)
- **Clases:** `PunishmentsModule`, `PunishmentService`, `PunishmentSql`, `PunishmentActions`, `MuteListener`, `Punishment`, `enums/PunishmentStatus`; comandos `BanCommand`, `MuteCommand`, `WarnCommand`, `KickCommand` (`PunishmentsModule.java:33-44`).
- **Comandos:** ban (tempban), mute (tempmute), warn, kick · permisos `ioricore.punish.<x>`.
- **Persistencia:** tabla compartida `ic_punishments` + índice (`PunishmentSql.java:19-34`); escritura asíncrona (`PunishmentService.java:215`); caché en memoria de ban/mute.
- **Integración:** si la sanción es un ban, Redis publica la invalidación (`PunishmentService.java:142-148`), lo que necesita `jedis` en runtime (ver §26). LiteBans soft solo en lectura, para isBanned/isMuted (`:61,83`). Kick con pantalla i18n (`PunishmentActions.java:125`).
- **Fail-open:** si la BD falla, se loguea y se devuelve `false` (`:70-73,92-95`).
- **Bugs:**
  - Al aplicar un mute, `muteCache` pasa a `true` y **nunca expira** (`:138-140`, `:79-80`). Un tempmute sigue activo hasta reiniciar.
  - **No hay unban, unmute, history ni notes.** El inventario los pedía (`MODULE-INVENTORY.md:65`).
  - Texto hardcodeado "No reason provided" (`MuteListener.java:30`).
  - `disable()` no desregistra `MuteListener` (`PunishmentsModule.java:48`).
  - El mute no cubre /msg, /r ni /me.
- **ESTADO:** PARTIAL.

## 8. Staff mode / freeze — resumen
`StaffModeModule` (`:30-50`) registra `/staffmode` (staff, sm, mod; args toggle/freeze/help) y `StaffModeListener`, que bloquea move, daño, comida, romper/colocar, drop, inventario e interacción. Hay una task de actionbar para los congelados (`STAFF-MODE.FREEZE-ACTIONBAR-TICKS`). El servicio da hotbar desde messages, vanish propio, freeze, nametag `[Staff]` por Team y glow Apollo. No existe `/freeze` como comando propio; es un argumento de `/staffmode`. Detalle → [STAFFMODE](STAFFMODE.md).

## 9. Staff auth enforce (`MODULES.STAFF-AUTH`, default **false**)
- **Clases:** `StaffAuthEnforceModule`, `StaffAuthEnforceService`, `StaffAuthEnforceListener`.
- **Listener:** join/quit; si el jugador está restringido cancela move, interact, break, place, drop y daño (`StaffAuthEnforceListener.java:24-68`).
- **Flujo:** el proxy envía `STAFF_AUTH_SESSION` por `ioricore:main` → `PluginMessagingBridge` → `acceptSessionFlag(ok)` (`PluginMessagingBridge.java:48-52`). La restricción consiste en ceguera 24h y walk/fly speed 0 (`StaffAuthEnforceService.java:99-108`).
- **Config:** `STAFF-AUTH.STAFF-PERMISSION` (ioriproxy.staff), `GRACE-TICKS`, `ENFORCE-WITHOUT-PROXY` false, `RESTRICT-ON-DENY` false (`R/settings.yml:200-206`).
- **Con los defaults no restringe nunca:**
  - Sin paquete del proxy no hay timer (`:31-33`).
  - Con deny tampoco restringe (`:68`).
  - Además, el módulo viene OFF.
- **Otros riesgos:**
  - `clearRestrict` resetea la velocidad a 0.2/0.1 y pisa un `/speed` previo (`:110-115`).
  - No hay anti-spoof en el backend: el byte `ok` se acepta tal cual.
  - El proxy marca el canal como `handled` (bloquea que el cliente lo falsifique) solo si su `plugin_messaging` está activo (`velocity/.../VelocityPluginMessageListener.java:20,36`).
- Doc vs código: `DESIGN_BRIEF.md:25` pide el título "2FA + ON". Ese título no existe; el feedback son solo mensajes `STAFF-AUTH.RESTRICTED`.
- **ESTADO:** PARTIAL.

## 10. Scoreboard / TAB / nametag — resumen
`ScoreboardModule`: `/scoreboard` (sb, sidebar), listener y task cada `SCOREBOARD.UPDATE-TICKS`. Sidebar con Teams `ic<i>` (`ScoreboardService.java:140-156`), toggle por jugador desde PlayerSettings y llamada Apollo `setNumbersDisabled`. Ese método está vacío: "Stub… Apollo has no dedicated Numbers module API" (`B/hook/SoftApolloHook.java:92-94`). `TabModule`: header/footer cada `TAB.UPDATE-TICKS` vía `VersionWrapper` (`TabService.java:83`). **En 1.12 no hay header/footer**: Bukkit no tiene `setPlayerListHeaderFooter` y el wrapper v1_12_R1 no implementa `sendTabHeaderFooter`. No hay módulo de nametag aparte; solo el prefijo de staff en StaffMode. Detalle → [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md).

## 11. Join MOTD / PictureLogin (`MODULES.JOIN-MOTD`)
- `JoinMotdModule` → `JoinMotdListener` (delay `JOIN-MOTD.DELAY-TICKS`) → `JoinMotdService.send`.
- Elige entre `JOIN-MOTD.LEGACY` y `JOIN-MOTD.MODERN` según el protocolo del jugador (`JoinMotdService.java:26-33`).
- "PictureLogin" **no renderiza ninguna cara**: solo envía las líneas `JOIN-MOTD.PICTURE` con la URL de crafthead (`:36-52`). El `runTaskAsynchronously` está vacío, no hace ningún trabajo async (`:37-40`).
- `GIVE-HEAD` da una cabeza (`:54-66`).
- `{brand}` está hardcodeado como "RoyalCTW" (`:74`).
- Doc vs código: `MODULE-INVENTORY.md:83` y DESIGN_BRIEF piden la cara estilo PictureLogin (MIT, `IMAGE-INDEX` img 08) y líneas premium/bedrock/nLogin. El código solo tiene el placeholder `{bedrock}`.
- **ESTADO:** PARTIAL.

### 11b. Join status line (`JoinStatusListener`)
- Se registra siempre (`IoriCorePlugin.java:173`) y se controla con `JOIN.STATUS-LINE`.
- Envía `JOIN.STAFF-BRAND` a quien tenga `ioricore.admin`, `JOIN.SEE-PERMISSION` o `JOIN.STAFF-PERMISSION`.
- Tipo de cuenta: Bedrock (Geyser), Premium/Cracked (nLogin) u online-mode; si no hay datos, UNKNOWN (`JoinStatusListener.java:57-74`).
- Detrás del proxy, `Bukkit.getOnlineMode()` es false, así que sin nLogin sale siempre "Desconocido". Es lo esperado según PROGRESS.
- **ESTADO:** IMPLEMENTED_UNTESTED.

## 12. Player settings `/settings` (`MODULES.PLAYER-SETTINGS`)
- **Clases:** `PlayerSettingsModule`, `PlayerSettingsStore` (`player-settings/{uuid}.yml`, se guarda en quit), `PlayerSettings` (sounds, particles, language, premiumDisplay, scoreboard), menús `SettingsMenu`, `SettingsToggleMenu`, `SettingsLangMenu`.
- `/settings` (alias preferencias). Cambio de contraseña con Anvil → `nLogin.changePassword` (`SettingsMenu.java:131-146`); necesita anvil NMS (§24).
- **Las preferencias no se aplican en ningún sitio, salvo `scoreboard`** (`ScoreboardService.java:86,183`):
  - `sounds`: `SoundService.play` no las consulta (`B/fx/SoundService.java:82-87`).
  - `particles`: no hay sistema de partículas.
  - `premiumDisplay`: nadie la lee.
  - `language`: se guarda (`SettingsLangMenu.java:75`), pero `CoreLanguage` es global y `rawFor`/`listFor` tienen 0 usos.
- Doc vs código: falta el submenú de género (EXTRAS_BACKLOG) y no hay SQL (solo YAML).
- **ESTADO:** PARTIAL.

## 13. Chat logs / command logs (`MODULES.CHAT-LOGS`)
- **Clases:** `ChatLogsModule`, `ChatLogListener` (chat en MONITOR; comandos en MONITOR también con cancelados → ALLOWED/BLOCKED), `ChatLogService` (executor de un solo hilo, I/O fuera del hilo principal).
- **Rutas:** `chat/players/<NAME|UUID>/{chat|commands}-<fecha>.log` con header Player/Total/Generated. Agregados en `chat/messages/`, `messages/donors/`, `messages/staff/`, `blocked-commands/` y `staff-actions/` (`ChatLogService.java:141-146,182-186`).
- **Formato de línea:** `[HH:mm:ss] (server) [rank] name: msg [✓ ALLOWED|! BLOCKED]` (`:153-161`, `:100`). Coincide con `EXTRAS_BACKLOG.md:29`.
- **Rank del log:** sale de permisos (`ioricore.rank.owner`/staff/donor), no de Ranks (`ChatLogListener.java:34-45`).
- **Stubs:**
  - Purga y cap son stubs que solo loguean (`purgeStub`/`capStub`, `:131-137`). `MAX-LINES` y `PURGE-DAYS` no hacen nada.
  - `logStaffAction` no se llama desde ningún sitio, así que `staff-actions/` siempre está vacío.
  - El "Total" del header solo se escribe al crear el fichero.
- Doc vs código: la ruta doc es `logs/players/...` (`EXTRAS_BACKLOG.md:21`) y el código usa `chat/...`. En el código, `BLOCKED` = evento cancelado por otro plugin, no una blacklist propia.
- **ESTADO:** PARTIAL.

## 14. Easter eggs (`MODULES.EASTER-EGGS`, off)
- **Clases:** `EasterEggsModule`, `EasterEggService`, `EasterEggListener` (click en ArmorStand o bloque dentro de `CLICK-RADIUS`), `EasterEgg`.
- **Config:** `settings.yml EASTER-EGGS.*` (`R/settings.yml:228-233`) y `data/easter-eggs.yml` (`EGGS.<id>`: ENABLED, WORLD, X, Y, Z, DISPLAY, TEXTURE, ARMOR-STAND, TITLE, SUBTITLE, REWARD-COMMAND).
- **Funciona:**
  - Title, actionbar de progreso y "ALL-FOUND" (`EasterEggService.java:152-179`).
  - Auto-place de ArmorStands con `AUTO-PLACE` (`:84-104`).
- **Roto / stub:**
  - **La recompensa nunca se entrega**: `claimStub()`, el único que ejecuta `REWARD-COMMAND`, no tiene llamadas (`:181-203`).
  - **El progreso vive solo en memoria** (`found`, `:35`) y se pierde al reiniciar.
  - `TEXTURE` no se usa: la cabeza toma de owner el id del huevo (`:229`).
  - `clearPlaced()` no vacía la lista `placedStands`.
- Doc vs código: `DECISIONS.md:67` pide progreso por jugador, partículas y eventos temporales. No hay partículas ni persistencia.
- **ESTADO:** PARTIAL.

## 15. Tags (`MODULES.TAGS`, off)
- **Clases:** `TagsModule`, `TagService`, `Tag`; `/tag` (tags) con args equip, unequip, list, help.
- **Datos:** `data/tags.yml` → `TAGS.<id>` (DISPLAY, PERMISSION, EMOJI, CHAT). El jugador equipado se guarda en `tags/players/{uuid}.yml` · `PERSISTENCE.TAGS`.
- `canUse`: admin, comprado en Coinshop o permiso (`TagService.java:89-95`). ChatFormat inyecta `{TAG}`/`%tag%`.
- **Incidencias:**
  - El comentario `settings.yml:118` dice `data/tags/players`, pero el código usa `tags/players` (`TagService.java:41`).
  - No hay menú propio de tags (el de compra está en Coinshop) ni SQL.
- **ESTADO:** IMPLEMENTED_UNTESTED.

## 16. Economy (`MODULES.ECONOMY`, off)
- **Clases:** `EconomyModule`, `EconomyService`, `EconomyYamlStore` (`data/economy/{uuid}.yml`), `EconomySql` (`ic_balances`, MERGE en H2 / INSERT…ON DUPLICATE), `EconomyVaultBridge`.
- **Comandos:** balance (money, bal), pay, eco (economy) con give/set/take/help.
- **Config:** `ECONOMY.STORAGE` YAML|SQL, `STARTING-BALANCE`, nombres `PRIMARY/CREDITS/GEMS-NAME` (`R/settings.yml:101-113`) · `PERSISTENCE.ECONOMY`.
- **Comportamiento:**
  - Si Vault tiene un proveedor de Economy, delega en él (`EconomyService.java:47-49`).
  - **IoriCore no se registra como proveedor Vault** (no hay `ServicesManager.register`), así que otros plugins no ven su saldo.
  - Escritura async (`:135`). La **primera lectura es síncrona** y puede ir a SQL en el hilo principal (`:54`).
- **Solo existe una moneda.** CREDITS y GEMS son solo nombres (`:111-123`), sin saldo. Doc: `settings.yml:100` "PRIMARY = Royales · CREDITS · GEMS".
- `EconomyService.unload` no tiene llamadas: la caché crece sin límite.
- **ESTADO:** PARTIAL.

## 17. Coinshop (`MODULES.COINSHOP`, off · requiere ECONOMY + TAGS)
- **Clases:** `CoinshopModule`, `CoinshopService`, `CoinshopOffer`, enums `CoinshopOfferType{TAG,COSMETIC}` y `PurchaseResult`; menús `CoinshopMenu`, `CoinshopCategoryMenu`, `CoinshopConfirmMenu` (`menus/{modern,legacy}/coinshop*.yml`).
- `/coinshop` (shop, tienda).
- **Datos:** ofertas en `data/coinshop.yml` (`OFFERS.<id>` TYPE, TARGET, PRICE, MATERIAL, DATA). Lo comprado va a `coinshop/players/{uuid}.yml`.
- **Solo funcionan las ofertas TAG**: `load()` salta las que no son TAG (`CoinshopService.java:63-64`). El flujo es has → withdraw → añadir a owned → save (`:121-144`).
- Doc vs código: `MODULE-INVENTORY.md:103` pide "buy tags/ranks cosmetics". Ranks y cosméticos no están.
- **ESTADO:** PARTIAL.

## 18. ChatGames (`MODULES.CHAT-GAMES`, off)
- **Clases:** `ChatGamesModule`, `ChatGamesService`, `ChatGamesListener` (AsyncChat HIGH → cancela la respuesta correcta y la resuelve en el hilo principal), `ChatGameQuestion`, `command/ChatGamesCommand`.
- **Comandos:** `/chatgames` (cg, chatgame); `start`, `skip|end` con `ioricore.chatgames.admin` (`ChatGamesCommand.java:42,54`).
- **Config** `data/chatgames.yml`: `INTERVAL-SECONDS` (≥30), `TIMEOUT-SECONDS` (≥10), `MIN-PLAYERS`, `WINNER-COOLDOWN-SECONDS`, `REWARD.MONEY`, `REWARD.COMMANDS[]` y `QUESTIONS[]` {QUESTION, ANSWERS[]} (`ChatGamesService.java:38-70`).
- **Mecánica:**
  - Pregunta aleatoria, broadcast `CHAT-GAMES.START/WIN/TIMEOUT`.
  - Recompensa: depósito en Economy más comandos de consola con `%player%`/`<player>` (`:166-180`).
- **ESTADO:** PARTIAL (solo trivia).

### 18.1 Comparativa con la referencia `.cursor/_refs/ChatGames/` (CriticalRed 2.9.1 · licencia comercial/desconocida → **solo referencia funcional, no copiar**, `_refs/README.md:14`)
| Aspecto | Referencia (`com/criticalred/chatgames`, `resources/config.yml`, `configs-zip/`) | IoriCore |
|---|---|---|
| Tipos de juego | `GameType`: REACTION, UNSCRAMBLE, TRIVIA, MATH (EASY/HARD/BOTH), ALGEBRA (`enums/GameType.java`, `enums/MathDifficulty.java`, `tasks/*Game.java`) | Solo trivia (pregunta/respuestas fijas) |
| Generadores | `maths/EquationGenerator`, `AlgebraEquationGenerator`, `WordHandler` + `words.txt`/`words.yml` | ✘ (las "matemáticas" son preguntas fijas en YAML) |
| Temporización | `game-duration`, `game-delay.min/max` aleatorio, `min-players` (`config.yml:76-85`) | intervalo fijo + timeout + min players |
| Ámbito | `world-whitelist`, `allow-cancelled-chat-wins`, `show-answer-progress` (`config.yml:87-117`) | ✘ (global; ignora el chat ya cancelado) |
| Recompensas | por juego: `broadcast`, `commands`, `chance-commands` con %; `rewards.yml` con acciones `[playercmd] [consolecmd] [playermsg] [broadcast]`, `N%~`, `has:perm` y premios por puesto (`configs-zip/rewards.yml:1-30`) | dinero fijo + lista de comandos de consola, sin chance, sin permisos, sin premio por tipo |
| Sonidos | `game-sound`, `win-sound` | ✘ (`SoundService` no se usa aquí) |
| Puntos / ranking | `ScoreHandler`, `RecordHandler` (mejor tiempo), `players.yml`/MySQL, `gui/ScoreboardGUI` + `scoreboard-gui.yml` | ✘ (no hay stats ni top) |
| Hologramas / PAPI | `HologramHandler` (Decent/Fancy), `PlaceholderHandler` | ✘ |
| Discord | `DiscordWebhookHandler` (start/win/timeout) | ✘ (en IoriCore sería el proxy JDA) |
| Comandos admin | enable, disable, reload, forcegame <tipo>, stop, checkgame, soundtest, scoreboard, update, hologram (`resources/plugin.yml:15-40`, `commands/ChatCommand.java`) | start, skip/end · **sin reload** |
| Placeholders | `%player% %answer% %question% %time% %time_s%` | `{player} {answer} {question} {money}` (sin tiempo) |
| Cooldown ganador | — | ✔ `WINNER-COOLDOWN-SECONDS` (exclusivo de IoriCore) |

**Lo que falta en IoriCore, priorizado:** tipos reaction/unscramble/math/algebra con generadores · delay aleatorio · reload · recompensas por tipo con chance/permiso · sonidos · puntos y top (SQL `ic_*`) · whitelist de mundos · tiempo de respuesta.

## 19. Bossbar tips (`MODULES.BOSSBAR-TIPS`, off)
- `BossbarTipsService`: rota `BOSSBAR-TIPS.LINES` (messages) cada `INTERVAL-SECONDS` y la oculta tras `STAY-SECONDS` (`BossbarTipsService.java:30-63`).
- BossBar por reflexión si la API del **servidor** la tiene. Si no (1.8), manda un title a todos (`:61-67`); en 1.8 es intrusivo cada 60s.
- Usa el idioma global y un solo color (AQUA/SOLID hardcodeado, `:76-77`).
- Doc: DESIGN_BRIEF pide "sin bossbar (o off)" en legacy; el fallback a title contradice ese "off".
- **ESTADO:** IMPLEMENTED_UNTESTED.

## 20. Gender (`MODULES.GENDER`, off)
`GenderModule.enable()` solo loguea "stub" (`GenderModule.java:28-30`). No tiene datos, menú ni placeholders. **STUB.**

## 21. Donor join (`MODULES.DONOR-JOIN`)
Está en `settings.yml:50` y en `ModuleId.DONOR_JOIN`, pero ningún código lo lee. Se pedía en `EXTRAS_BACKLOG.md:12` ("Donor join broadcast + sound"). En `IMAGE-INDEX` existe la captura `69b624e6` "-> NEW DONOR", pero es del proxy (donations). **NOT_IMPLEMENTED** en el Core.

## 22. Skin `/skin` (`MODULES.SKIN`)
- `SkinService`, `SkinCommand`, `enums/SkinMode` (face, head, body, body3d, full → URLs crafthead/minotar) (`SkinMode.java`).
- Prueba la URL de forma async y después, en el hilo principal, envía `SKIN.RESULT` con la URL y **da un item gratis**: cabeza (face/head) o un mapa **vacío** con lore (`SkinService.java:37-95`). No se renderiza ninguna imagen.
- Doc: "Discord-like modes … optional Discord later" (`MODULE-INVENTORY.md:112`).
- Riesgo: cualquiera con `ioricore.skin` puede generar items sin límite (no hay cooldown).
- **ESTADO:** PARTIAL.

## 23. IP `/ip` (`MODULES.IP`)
- `IpCommand` (`ioricore.ip`): uuid, IP (`getAddress()`), bedrock (Geyser) y server (`IpCommand.java:31-50`). Solo funciona con jugadores online.
- Doc vs código: `MODULE-INVENTORY.md:116` pide país/geo, número de alts, nLogin y first/last seen. Nada de eso está.
- Detrás del proxy, la IP depende de forwarding (BungeeCord/Velocity modern) → NO VERIFICADO.
- **ESTADO:** PARTIAL.

## 24. Abstraction / NMS
Loader: `VersionMatcher` toma el sufijo del paquete CraftBukkit. Si no tiene `v`, mapea por `getBukkitVersion`: 1.8→v1_8_R3, 1.12→v1_12_R1, 1.16→v1_16_R3, 1.20→v1_20_R3, 1.21→v1_21_R1 (`B/abstraction/VersionMatcher.java:22-34`). Si la clase no existe usa `NoopVersionWrapper`. Todo se envuelve en `FxAwareVersionWrapper`, que intenta primero Bukkit nativo (`BukkitNativeFx`: sendTitle, `spigot().sendMessage(ACTION_BAR)`, `setPlayerListHeaderFooter`) y si falla delega en NMS (`FxAwareVersionWrapper.java:35-58`).

| Revisión | Ficheros | Anvil input | Title | ActionBar | Tab header/footer | Real / esqueleto |
|---|---|---|---|---|---|---|
| `v1_8_R3` | `nms/v1_8_R3/.../VersionWrapper_v1_8_R3.java` (297 l.) | ✔ `supportsAnvilInput`, open/close + listener (`:49-101,214-288`) | ✔ packets (`:119`) | ✔ (`:144`) | ✔ (`:155`) | **REAL** |
| `v1_12_R1` | 49 l. | ✘ warning una vez | stub (lo resuelve Bukkit nativo) | stub (Bukkit nativo) | ✘ (sin método) → **sin TAB header en 1.12** | ESQUELETO |
| `v1_16_R3` | 49 l. (idéntico a 1.20 salvo el nombre) | ✘ | stub → nativo | stub → nativo | stub → nativo ✔ | ESQUELETO |
| `v1_20_R3` | 49 l. | ✘ | stub → nativo | stub → nativo | stub → nativo ✔ | ESQUELETO |
| `v1_21_R1` | 233 l., Java 21, NMS con mappings Spigot | ✔ (`:48-116`) | ✔ `player.sendTitle` (`:118-122`) | (default → nativo) | (default → nativo) | **REAL** (anvil + title) |

- Por tanto, el anvil (cambio de contraseña en `/settings`, editor de rangos) **solo funciona en 1.8.8 y 1.21/1.21.1**. En 1.12, 1.16 y 1.20 `AnvilInput.open` devuelve false (`B/menu/anvil/AnvilInput.java:20-26`).
- **Versiones que acaban en Noop:**
  - Spigot 1.21.3+ (paquete `v1_21_R2`/`R3`).
  - 1.9–1.11, 1.13–1.15 y 1.17–1.19.
  - Paper 1.20.5+ sin relocación cae por el fallback en v1_21_R1/v1_20_R3; que funcione en runtime con mappings Mojang es NO VERIFICADO.
- La compilación depende de jars locales con ruta absoluta (`nms/v1_21_R1/build.gradle.kts`: `C:/Users/TqmassPC/Documents/BuildTools/...`), no reproducible en otra máquina.
- v1_21_R1 sale con bytecode 21. Es compatible con la decisión "Java 17 bytecode en todo" solo porque se carga únicamente en 1.21 (`DECISIONS.md:29`); conviene dejarlo documentado.
- **ESTADO:** PARTIAL.

## 25. FX (sonidos, títulos, actionbar)
- `SoundService` lee `sounds.yml` → `SOUNDS.<KEY>` (`R/sounds.yml:8`), con resolución enum/string y compatibilidad legacy (`SoundService.java:33-118`). Claves lógicas en `SoundKey`: UI_CLICK, TELEPORT, SUCCESS, ERROR, STAFF_ALERT, LEVEL_UP, PAGE_TURN, DENY, CONFIRM, NOTIFICATION.
- `TitleService` y `ActionBarService` delegan en `VersionWrapper` (`TitleService.java:26-39`).
- No respetan la preferencia `sounds` de `/settings`. La intención era XSound; no se ha verificado si se usa XSound (el código es propio).
- **ESTADO:** IMPLEMENTED_UNTESTED.

## 26. Hooks soft (`MODULES.*-HOOK`)
| Hook | Clase | Implementado | Falta / stub |
|---|---|---|---|
| Vault | `SoftVaultHook` | prefix/suffix de Chat (`:31,44-49`) | — (Economy usa su propio `EconomyVaultBridge`) |
| LuckPerms | `SoftLuckPermsHook` | prefix/suffix meta | `applyPrefix` vacío (`:41-43`); Ranks crea una 2ª instancia |
| Apollo (Lunar) | `SoftApolloHook` | glow override/reset (`:53-89`) | `setNumbersDisabled` vacío (`:92-94`) |
| nLogin | `SoftNLoginHook` | isAuthenticated, isPremium, changePassword | — |
| Geyser | `SoftGeyserHook` | isBedrock | — |
| LiteBans | `SoftLiteBansHook` | isBanned, isMuted, activeBanReason | no escribe / no espeja; solo lo usa Punishments |
| PlaceholderAPI | — | ✘ | lo pedían `MODULE-INVENTORY.md:58,109` → NOT_IMPLEMENTED |
| Redis (publish) | `B/redis/SoftRedisPublisher` | `publishPunishmentInvalidate` por reflexión de jedis | `jedis` es `compileOnly` (`bukkit/build.gradle.kts:31`) y no se incluye en el jar → noop si el servidor no lo aporta |
| Mongo | `B/storage/SoftMongoStorage` | `tryOpen` | `mongodb-driver-sync` `compileOnly` (`:30`); `softMongo()` no lo usa ningún módulo. Retirada limpia: solo se toca `CoreSqlStorage.java:20-63` (decisión `DECISIONS.md:32`) |

Detalle de integraciones → [INTEGRATIONS](INTEGRATIONS.md).

## 27. Plugin messaging (`MODULES.PLUGIN-MESSAGING`)
- `PluginMessagingBridge`: canal `ioricore:main` (`api/.../PluginChannels.java:6`).
  - `HANDSHAKE_PING` → `PONG`.
  - `STAFF_AUTH_SESSION` → StaffAuth (`PluginMessagingBridge.java:37-53`).
- El backend no tiene secreto, HMAC ni nonce. Doc: "anti-spoof · handshake" (`MODULE-INVENTORY.md:119`). El único anti-spoof es que el proxy marca el canal como handled.
- **ESTADO:** PARTIAL.

## 28. Language (`CoreLanguage`)
- Carga `messages/<lang>.yml` + `messages/legacy/<lang>.yml` (si falta, cae a `en`) y `chat/formats` (`CoreLanguage.java:44-63`).
- `raw/list(key, ph, protocol)` elige el bag legacy si `protocol ≤ legacyProtocolMax`.
- ⚠ `rawFor`/`listFor` tienen **0 usos**. Fuera de ChatFormat, JoinMotd, Scoreboard y Tab (los únicos que llaman a `ProtocolProbe.protocol`), todos los mensajes salen del bag **modern** (`raw(key)` = `Integer.MAX_VALUE`, `:65-71`). Las claves legacy de messages solo se usan en esos 4 módulos.
- Idioma por jugador: no hay (ver §12).
- Las 4 variantes tienen 21 secciones raíz cada una (grep `^[A-Z-]*:`), así que hay paridad estructural.
- **ESTADO:** PARTIAL.

## 29. Menús — resumen
- Hay dos frameworks.
- **Viejo:** `B/menu/` (`Menu`, `MenuItem`, `MenuConfig`, `MenuFiller`, `MenuListener`). Lo usan Coinshop, Settings y Ranks; StaffMode usa `MenuConfig`.
- **Nuevo:** `B/menu/api/` (`MenuApi`, `Button`, `SimpleMenu`, `ConfirmationMenu`…). Solo se habilita en `IoriCorePlugin:129`; ningún módulo lo usa.
- `MenuConfig` elige `menus/modern|legacy/*.yml` según el **material del servidor** (`MenuConfig.java:43-62`), no según el protocolo del jugador. Esto choca con la decisión de "dos sistemas independientes" (`DECISIONS.md:30`).
- Detalle → [MENUS](MENUS.md).

## 30. Storage
- `CoreSqlStorage`: Hikari, H2 `file:…;MODE=MySQL` o MySQL (`com.mysql:mysql-connector-j`, no MariaDB driver) (`CoreSqlStorage.java:76-108`).
- Tablas Core: `ic_punishments`, `ic_balances` y `ic_ranks` (este último según RANKS).
- Todo lo demás es YAML por fichero: homes, warps, back, nicks, ignore, kits, tags, coinshop, settings, economy (modo YAML).
- La clave `STORAGE.MYSQL.PASSWORD` está en texto plano en `settings.yml:23`.

---

## 31. Pendientes transversales (ordenados)
1. `/ioricore reload|help` + usar `commands.yml`/`permissions.yml`, o eliminarlos.
2. Mute en /msg, /r y /me; caducidad de la caché del mute; unban/unmute/history.
3. Easter eggs: llamar a la entrega de la recompensa y persistir el progreso.
4. Aplicar las preferencias de `/settings` (sonido, idioma por jugador, premium) y usar `rawFor` en los mensajes.
5. Unificar el vanish de Essentials y StaffMode.
6. Anvil en 1.12/1.16/1.20 y el mapeo `v1_21_R2+`; TAB header en 1.12.
7. Sacar del hilo principal la I/O de back, homes, warps y nicks y la primera lectura de economy.
8. ChatGames: tipos de juego y recompensas (§18.1); Coinshop: ranks y cosméticos; Economy: monedas CREDITS/GEMS y proveedor Vault.
9. Donor join, Gender y PlaceholderAPI: hay que decidir si se implementan o se retiran.
