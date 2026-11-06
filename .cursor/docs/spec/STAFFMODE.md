# STAFFMODE — spec funcional (auditoría 2026-11-03)

> Fuentes: StaffModeX v1.1.5 decompilado (`.cursor/_refs/StaffModeX/`, **comercial ArkFlame → solo referencia funcional, no copiar código**) · IoriCore `bukkit/src/main/java/us/iorilabs/bukkit/modules/staffmode/**` · intención original en `plan/MODULE-INVENTORY.md` §6/§7, `memory/DESIGN_BRIEF.md` "Staff", `memory/DECISIONS.md:8`.
> Rutas SMX relativas a `.cursor/_refs/StaffModeX/src/com/arkflame/staffmodex/`. Rutas IC relativas a `bukkit/src/main/java/us/iorilabs/bukkit/` salvo indicación.
> Ver también: COMMANDS · PERMISSIONS · MENUS · PROXY-MODULES · CORE-MODULES · FEATURE-STATUS.

---

## 1. Qué ofrece StaffModeX (SMX)

### 1.1 Flujo de arranque (`StaffModeX.java#onEnable`)
- Carga `config.yml` + `messages.yml` (`ConfigWrapper`), `ArmorManager`, `RedisManager`, `DatabaseManager` (MySQL opcional), `VeinManager`.
- Listeners siempre registrados: `BlockListeners`, `EntityListeners`, `InventoryListeners`, `PlayerListeners`, `MenuListener`.
- Comandos registrados **condicionalmente** por `*.enabled` en config (examine, freeze, helpop, infractions, ip, report, staffchat, staffmode, warn, vanish); `staffmodex` siempre.
- `StaffActionBarTask` (cada 5 ticks) si `action_bar.enabled`; expansión PAPI `staffmodex` si PlaceholderAPI está presente.
- `onDisable`: descongela a todos, saca de staffmode a todos (restaura inventario), cierra Redis/MySQL.

### 1.2 Hotbar (`hotbar/components/StaffHotbar.java`, `config.yml: items.hotbar`, `messages.yml: hotbar.*`)
Detección de clic (`listeners/PlayerListeners.java`):
- `PlayerInteractEvent` (LEFT/RIGHT, AIR/BLOCK) → llama `onInteract(player)` **y** `onInteract(player, block)`.
- `PlayerInteractEntityEvent` (solo **right-click sobre entidad**) → `onInteract(player)` **y** `onInteract(player, entity)` (excepto Knockback).
- Anti doble disparo: cooldown fijo 100 ms por UUID (`isCooldownOver`).
- Consecuencia: los ítems "de jugador" solo actúan con **click derecho sobre un jugador**; los "de aire" actúan con **cualquier clic** (izq./der., aire o bloque, e incluso al clicar a un jugador).

| Elemento | Nombre visible | Slot | Material (orden de resolución) + data | Lore | Acción | Clic | Abre menú | Permiso | Estado en IoriCore |
|---|---|---|---|---|---|---|---|---|---|
| phase | `&6Phase` | 0 | COMPASS | "Phase through walls…" | Avanza hasta 5 bloques en la dirección de la mirada y TP al primer hueco de 2 bloques "walkable"; sonido ENDERMAN_TELEPORT | Cualquiera (izq/der, aire/bloque) | No | Ninguno | NOT_IMPLEMENTED (slot 0 en IC = RANDOM-TP con COMPASS) |
| r-teleport | `&aRandom Teleport` | 1 | ENDER_EYE → EYE_OF_ENDER | "Teleport to a random player…" | TP **round-robin** (índice cíclico por staff, no aleatorio) a otros online, incluye staff/vanish; sonido | Cualquiera | No | Ninguno | IMPLEMENTED_UNTESTED (IC: aleatorio real, excluye vanish/staff) |
| vanish | `&bVanish` | 2 | ON: LIME_DYE → INK_SACK:10 · OFF: GRAY_DYE → INK_SACK:8 | "Toggle invisibility…" | Toggle vanish + cambia material/data del ítem | Cualquiera | No | `staffmodex.vanish` (+`vanish.requires_staff`) | IMPLEMENTED_UNTESTED (IC slot 4; ver bug modern §2.4) |
| players | `&3Players` | 3 | PAPER | "Open the GUI…" | Abre lista de jugadores | Cualquiera | **Players** | Ninguno | NOT_IMPLEMENTED |
| stafflist | `&5Staff List` | 4 | SKULL_ITEM (data 0 = skeleton en 1.8) → SKELETON_SKULL | "Display active staff…" | Abre lista de staff (Redis o local) | Cualquiera | **Staff List** | Ninguno | NOT_IMPLEMENTED |
| freeze | `&cFreeze` | 5 | ICE | "Immobilize a player…" | `toggleFreeze(staff)` sobre el objetivo | Right-click jugador | No | `staffmodex.freeze`; requiere estar en staffmode; objetivo con `staffmodex.freeze.bypass` inmune | IMPLEMENTED_UNTESTED (IC slot 1, PACKED_ICE; sin bypass ni requisito staffmode) |
| cps | `&eCPS` | 6 | CLOCK → WATCH | "Monitor clicks per second…" | Cuenta left-clicks del objetivo durante `cps.time` s (10) y reporta media; umbral **>10 hardcodeado** | Right-click jugador | No | Ninguno | NOT_IMPLEMENTED |
| examine | `&dExamine` | 7 | CHEST | "Inspect player inventory and stats…" | Abre Examine | Right-click jugador | **Examine** | `staffmodex.examine` (al abrir) | PARTIAL (IC slot 2 INSPECT: solo abre inventario vivo del objetivo, sin menú de stats) |
| follow | `&9Follow` | 8 | LEAD → LEASH | "Mount the player…" | `target.setPassenger(staff)` (monta al staff encima) | Right-click jugador | No | Ninguno | NOT_IMPLEMENTED |
| knockback | `&6Knockback` | 8 (**colisiona con follow**) · `enabled: false` | BLAZE_ROD + Knockback I | "Hit the player to check his knockback." | Único ítem que permite pegar estando en staffmode (`EntityListeners#onEntityDamageByEntity`) | Left-click (golpe) jugador | No | Ninguno | NOT_IMPLEMENTED |
| — | — | — | — | — | — | — | — | — | IC extra: STAFF-CHAT (slot 6, BOOK) · EXIT (slot 8, REDSTONE) — no existen en SMX |

### 1.3 Menús (`modernlib/menus/Menu.java`: `rows*9`, `InventoryHolder`; clicks cancelados en `MenuListener`; filler `items.fill` = STAINED_GLASS_PANE:7 → GRAY_STAINED_GLASS_PANE)

| Menú | Título | Tamaño | Slots / elementos | Acciones | Navegación | Condiciones / permisos |
|---|---|---|---|---|---|---|
| Players (`PlayersHotbarItem`) | `hotbar.players.menu_title` | 4 filas | 0–26: cabeza por jugador online (máx. 27, sin uno mismo), lore loc/vida/comida/XP/nivel/gamemode/**IP**/UUID + PAPI · 27: ARROW "Close" | Left = TP · Right = abrir Examine | Close | Sin paginación; sin permiso |
| Staff List (`StaffListHotbarItem`) | `hotbar.stafflist.menu_title` | 3 filas | 0–17: staff (Redis `ONLINE_STAFF`, o local con perm `staffmodex.staffmode`) · aquí = PlayerItem (TP/examine) · otro server = cabeza + server · 18: ARROW close | Left TP / Right examine (solo locales) | Close | Carga async; **bug**: con Redis desactivado `getOnline()` devuelve set vacío (no `null`) → la lista local nunca se usa (`RedisManager#getOnline`) |
| Examine (`menus/ExaminePlayerMenu.java`) | `&6Examining <name>` (**hardcoded**) | 3 filas | 9 Food (BREAD) · 10 Connection (COMPASS, IP/puerto) · 11 GameMode (GRASS) · 12 Infractions (PAPER, si warning/report) · 13 Location (MAP) · 14 Notes (PAPER) · 15 Freeze (ICE, si freeze) · 16 Warn (PAPER, si warning) · 17 Inventory (CHEST) | Food: heal+feed (`staffmodex.heal`) · Location: TP (`staffmodex.teleport`) · Notes: escribir nota por chat · Freeze: toggle · Warn: motivo por chat · Inventory: submenú | → Infractions / Inventory | Abrir: `staffmodex.examine` |
| Inventory view (`PlayerInventoryItem`) | `<name>'s inventory` | 5 filas | 0–35 inventario · 36 ARROW back · 39 offhand (**bug**: lee la mano del staff, no del objetivo) · 40 efectos (WATER_BUCKET) · 41–44 armadura (vacío = pane roja data 14) | Solo lectura | Back → Examine | — |
| Infractions (`menus/InfractionsMenu.java`) | `menus.infractions.title` | 3 filas | 0 Warnings (REDSTONE) · 1 Reports (BOOK) · 26 Back (si viene de otro menú) | Abre visor por tipo | Back | `staffmodex.infractions` |
| Infractions viewer (`InfractionsViewerMenu`) | `messages.infractions-menu-title` | 3 filas | 0..n más reciente primero (motivo/reportador/fecha) · 26 Back | — | Back | Sin paginación (overflow > 26 NO VERIFICADO) |
| Chest preview (`utils/Inventories.java`) | Nombre del bloque | Tamaño del contenedor | Copia del contenido | Abrir cofres **sin animación/ruido** (copia) | — | right-click bloque `InventoryHolder` en staffmode + `staffmodex.chestpreview` |

### 1.4 Comandos (`commands/*.java`, `plugin.yml`)
| Comando | Alias | Permiso (código) | Notas |
|---|---|---|---|
| `/staffmode` | staff, mod | `staffmodex.staffmode` | Toggle |
| `/vanish` | v | `staffmodex.vanish` | `vanish.requires_staff` opcional |
| `/freeze <p>` | ss | `staffmodex.freeze` | Requiere estar en staffmode |
| `/examine <p>` | invsee | `staffmodex.examine` | Abre Examine |
| `/staffchat [msg]` | sc | `staffmodex.staffchat` | Sin args = toggle; con args = envía |
| `/helpop <msg>` | — | `staffmodex.helpop` · recibe `staffmodex.helpop.receive` · bypass `staffmodex.bypass.cooldown` | Cooldown `helpop.cooldown` |
| `/report <p> <motivo>` | — | `staffmodex.report` · recibe `staffmodex.report.receive` | Persistido como infracción |
| `/warn <p> <motivo>` | — | `staffmodex.warn` (código `"staffmodex."+name`) | **Inconsistente**: `permissions.yml` y `WarnPlayerItem` usan `staffmodex.warning` |
| `/infractions <p>` | warnings | `staffmodex.infractions` (en menú) | |
| `/ip <p>` | iplog | `staffmodex.ip` | IP última conocida (memoria/yml/MySQL) |
| `/staffmodex reload` | — | `staffmodex.admin` | |

Permisos no documentados en `permissions.yml` pero usados: `staffmodex.helpop`, `.helpop.receive`, `.bypass.cooldown`, `.report`, `.warn`, `.armor.<set>`.

### 1.5 Protecciones / listeners
| Evento | Staff en staffmode | Congelado |
|---|---|---|
| BlockBreak | Cancelado | Cancelado + msg |
| BlockPlace | Cancelado salvo `staffmodex.build` | Cancelado + msg |
| EntityDamage (recibe) | Cancelado | Cancelado |
| EntityDamageByEntity (causa) | Cancelado salvo Knockback item | Cancelado |
| EntityTarget (mobs) | Cancelado | Cancelado |
| InventoryClick / Drag | Cancelado | Cancelado + msg |
| Drop / Pickup | Cancelado | Cancelado |
| Interact | Right-click cancelado (chest preview) | Cancelado + msg |
| Command | Bloquea `staffmode.blocked_commands` | **Todos** los comandos bloqueados |
| Chat | Staffchat si toggled | Chat aislado: congelado ↔ staff que congeló (+ resto de staff) |
| Move | — | Solo rotación (from con yaw/pitch de to) |
| Death | keepInventory | keepInventory |
| FoodLevel | Cancelado | — |
| Quit | Sale de staffmode | Aviso al staff + `freeze.commands.disconnect` (clave no presente en config por defecto) |
| Join | Oculta vanish existentes; restaura inventario pendiente de `inventories.yml`; alerta join staff (`staffmodex.join`/`view_joins`); re-entra a staffmode si Redis `STAFFMODE` lo contiene | — |

### 1.6 Estados
- **Staffmode on** (`managers/StaffModeManager#addStaff`): guarda location/gamemode/staffchat → vanish si `vanish.on_staff_mode` → staffchat si `staffchat.on_staff_mode` → guarda inventario (disco) → clear → heal → gamemode `gamemode.mode` → hotbar → fly → efectos (`staffmode.effects`: NIGHT_VISION, JUMP 1, SPEED 1 con alias) → armadura de cuero por permiso (`armors.sets`).
- **Off**: inversa; restaura location (`restore_location`), gamemode, staffchat, visibilidad.
- **Vanish** (`player/VanishPlayer.java`): `hidePlayer` a todos salvo `staffmodex.vanish.bypass`; `staffmodex.vanish.force` oculta incluso a bypass; hook PremiumVanish opcional; oculta join/quit message.
- **Freeze** (`player/FreezablePlayer.java`, `FreezeStatus.java`): mensaje + title + actionbar, ceguera, casco PACKED_ICE (guarda el original), sonidos; countdown `freeze.time` min **solo visual** (`isCountdownFinished` no se usa: no hay auto-ban).
- **Persistencia**: inventario en `inventories.yml` (disco, sobrevive crash; incluye vida/comida/fly) · infracciones `infractions/<uuid>.yml` o MySQL `staffmodex_infractions` · IPs `ips/<uuid>.yml` o `staffmodex_ips` · notas: en memoria (NO VERIFICADO persistencia).
- **Extras**: action bar staff (vanish ✔/✖, staffchat, TPS) y freeze countdown · diamond finder (alerta xray por vetas, `diamond-finder.min-streak`) · CPS test · notas por chat · PAPI `%staffmodex_enabled|freeze_countdown|freeze_time|frozen_player|playercount%` (`freeze_*` NPE si no congelado).

### 1.7 Compatibilidad de versiones
- Un solo jar `api-version: 1.13`; materiales por **lista de nombres** (primer existente, `modernlib/utils/Materials.java`), data por `short damage`; sonidos por alias (`Sounds.play(..., "ENTITY_ENDERMAN_TELEPORT","ENDERMAN_TELEPORT")`); efectos por alias; OFF_HAND vía `valueOf` en try/catch.
- IoriCore ya decidió lo contrario: **dos sistemas independientes** `menus/modern/` y `menus/legacy/` (brief 2026-11-03) + `util/LegacyMaterials.of(...)` como fallback.

### 1.8 Redis (`managers/RedisManager.java`)
| Clave / canal | Uso |
|---|---|
| `ONLINE_STAFF` (set) · `ONLINE_STATUS_<name>` (contador) · `CONNECTED_SERVER_<name>` | Presencia de staff en red (staff list con servidor) |
| `STAFFMODE` (set de nombres) | Staffmode **persistente entre servidores**: al entrar a otro server se reactiva |
| pub/sub `staff_chat` (`sender\nmsg`) | Staffchat cross-server |
- No sincroniza vanish ni freeze. Claves por **nombre** (no UUID).
- Bug: exige `redis.ip` + `redis.port` aunque haya `uri` → con el config por defecto nunca conecta.
- ⚠ El `config.yml` de referencia trae una URI Redis con credenciales embebidas: no reutilizar.

---

## 2. Qué tiene IoriCore (estado real)

### 2.1 Conexión
- `StaffModeModule` registrado en `IoriCorePlugin.java:155`; flag `MODULES.STAFF-MODE` (`settings.yml:47`, default true) → `StaffModeModule.java:28`.
- `enable()`: `MenuConfig` (carga `menus/{modern|legacy}/staff-mode.yml`, `MenuConfig.java:25-53`, detección legacy por `GRAY_STAINED_GLASS_PANE`), `/staffmode` vía `BukkitCommandBridge`, `StaffModeListener`, tarea actionbar freeze (`STAFF-MODE.FREEZE-ACTIONBAR-TICKS`, min 5).
- Sin evidencia RESULT-OK en `IMAGE-INDEX.md` → nada es `IMPLEMENTED_AND_TESTED`.

### 2.2 Funcionalidades
| Funcionalidad | Estado | Evidencia / notas |
|---|---|---|
| `/staffmode` (alias staff, sm, mod) toggle/help | IMPLEMENTED_UNTESTED | `command/StaffModeCommand.java:25-52`, `plugin.yml:42-45`, `commands.yml:50-55` |
| `/staff freeze <p>` | IMPLEMENTED_UNTESTED | `StaffModeCommand.java:54-74`; **no** exige estar en staffmode ni respeta bypass |
| Guardar/restaurar inventario + armadura | PARTIAL | Solo **memoria** (`StaffModeService.java:46-50,88-89,118-123`) → crash = pérdida. No guarda location, vida, comida, XP, efectos |
| Gamemode | PARTIAL | Guarda y restaura (`:90,125-126`) pero no fija uno al entrar |
| Flight / night vision / invulnerable | IMPLEMENTED_UNTESTED | `:402-433`, settings `STAFF-MODE.FLIGHT/NIGHT-VISION/INVULNERABLE` (`settings.yml:190-194`) |
| Vanish de staffmode | IMPLEMENTED_UNTESTED | `:141-170`; ve vanish `ioricore.vanish.see`/`ioricore.staff`/`ioricore.admin` (`:466-472`) |
| Nametag `[Staff Mode]` (displayName, tablist, team `icStaff`) | IMPLEMENTED_UNTESTED | `:343-400`, `messages/es.yml:306` |
| Glow Lunar + disable numbers (Apollo soft) | IMPLEMENTED_UNTESTED | `:445-464`, `hook/SoftApolloHook.java`; fallback raro: si no hay Apollo y NIGHT-VISION=false igual aplica night vision (`:456-459`) |
| Freeze (estado + efectos SLOW 6/JUMP 128 + actionbar) | PARTIAL | `:172-207`; movimiento: solo bloquea cambio de **bloque** (`StaffModeListener.java:53-64`); sin bloqueo de comandos, sin chat aislado, sin title, sin aviso/acción al desconectar (`cleanup` lo descongela en silencio, `:236-249`), sin persistencia ni cross-server |
| Hotbar RANDOM-TP / FREEZE / INSPECT / VANISH / STAFF-CHAT / EXIT | IMPLEMENTED_UNTESTED (STAFF-CHAT = PARTIAL: solo imprime tip) | `StaffModeService.java:268-316`, `StaffModeListener.java:143-228`, `menus/*/staff-mode.yml` |
| INSPECT | PARTIAL | `staff.openInventory(target.getInventory())` (`StaffModeListener.java:168`), sólo lectura por cancelación de click; sin stats/armadura dedicada |
| Protecciones staff: drop, pickup, inv click/drag, daño recibido, hambre | IMPLEMENTED_UNTESTED | `StaffModeListener.java:66-141` |
| Protecciones staff: block break/place, interact, daño **causado**, mob target | NOT_IMPLEMENTED | Solo se cancelan para congelados (`:100-108`, `:77-83`) → el staff invisible puede romper/colocar/pegar |
| Protecciones congelado: break/place/drop/pickup/inv/daño/hambre/right-click | IMPLEMENTED_UNTESTED | idem; left-click no se cancela (`:175`) |
| Mensajes es/en/legacy | IMPLEMENTED_UNTESTED | `messages/es.yml:285-340`, `en.yml`, `legacy/es.yml` |
| Permisos `ioricore.staffmode.{freeze,vanish,inspect,randomtp}` | IMPLEMENTED_UNTESTED | `plugin.yml:289-306`, `permissions.yml:25-29` |
| Players menu, staff list menu, examine menu, CPS, follow, phase, knockback, chest preview silencioso, notas, diamond alert, blocked commands, armaduras, efectos configurables, action bar staff, restore location | NOT_IMPLEMENTED | Sin código |
| Persistencia staffmode entre servidores / presencia | NOT_IMPLEMENTED | `PluginMessageType` solo tiene `HANDSHAKE_PING/PONG, STAFF_AUTH_SESSION` (`api/.../messaging/PluginMessageType.java`) |

### 2.3 Módulos relacionados
| Módulo | Estado | Relación |
|---|---|---|
| Essentials `/vanish` (`modules/essentials/vanish/VanishService.java`) | IMPLEMENTED_UNTESTED | **Segundo estado de vanish**; sync unidireccional Essentials→StaffMode (`markVanished`, `:89-96`); tablist " " vs prefijo staff |
| Essentials `/invsee` (`modules/essentials/command/InvseeCommand.java`) | existe (ver CORE-MODULES) | Solapa con INSPECT |
| StaffAuth enforce (`modules/staffauth/*`) | ver CORE-MODULES / PROXY-MODULES | Bloquea move/interact/break/place/drop/daño hasta `STAFF_AUTH_SESSION` (`StaffAuthEnforceListener.java:33-70`); default `MODULES.STAFF-AUTH: false` (`settings.yml:57`); el staffmode **no** comprueba si el staff está autenticado |
| Punishments (`punishment/*`: ban/mute/warn/kick, `PunishmentsModule` `IoriCorePlugin.java:162`) | ver CORE-MODULES | Sin integración con staffmode/freeze (sin "ban desde freeze", sin infractions en inspect) |
| Proxy staffchat `/sc` | registrado (`velocity/.../IoriProxy.java:603-605`) | El ítem STAFF-CHAT solo remite a `/sc` |
| Proxy `/staff` list (`velocity/.../commands/StaffCommand.java`) | STUB (código sin registrar: no hay `new StaffCommand` en `IoriProxy`) | Si se registra, **interceptará `/staff`** antes que el alias Bukkit `staff` |
| Proxy helpop / report (`velocity/.../helpop`, `report`) | ver PROXY-MODULES | SMX los tiene en Bukkit; aquí son proxy → no reproducir en Core |
| Core `/ip` (`modules/ip`) | ver CORE-MODULES | Equivale a `/ip` de SMX |

### 2.4 Bugs / riesgos detectados
1. Staff en staffmode (vanish + invulnerable) **puede romper/colocar bloques, pulsar botones/placas y pegar** (no se cancela). Alto.
2. Inventario solo en memoria: crash o kill del proceso = inventario del staff perdido (SMX usa disco).
3. Modern: `refreshVanishItem` usa data 10 sobre `GRAY_DYE` (`StaffModeService.java:321-338`) → el ítem no cambia a LIME_DYE en 1.13+; solo cambia el nombre.
4. 1.9+: `PlayerInteractEvent`/`PlayerInteractEntityEvent` pueden disparar por mano principal y offhand; IC no filtra `getHand()` ni tiene cooldown → posible doble toggle (freeze/vanish). NO VERIFICADO en servidor.
5. `disable()` siempre quita vanish (`StaffModeService.java:111`) aunque el staff estuviera en `/vanish` de Essentials antes; y `setVanish` de staffmode no actualiza `VanishService` → estados divergentes.
6. Freeze evadible: desconectarse lo limpia sin aviso; no hay bypass para staff superior; un staff puede congelar a otro staff.
7. `isHotbar` acepta un ítem sin display name si coincide el material (`StaffModeListener.java:234`).
8. Conflicto latente `/staff` (alias Bukkit) vs `StaffCommand` del proxy.

---

## 3. Qué queremos reproducir

Leyenda: ✅ claramente útil · ❓ dudosa (pregunta al usuario) · ❌ no reproducir.

| Funcionalidad | Valoración | Motivo / nota |
|---|---|---|
| Protecciones completas del staff (break/place con perm build, interact, daño causado, mob target, death keepInventory) | ✅ | Corrige bug §2.4-1 |
| Persistencia de inventario a disco/BD (+ location, vida, comida, XP, efectos) | ✅ | Seguridad de datos |
| Restore location al salir | ✅ | Configurable (`RESTORE-LOCATION`) |
| Gamemode configurable al entrar | ✅ | SMX `gamemode.mode` |
| Freeze completo: bloqueo de comandos (whitelist), chat aislado staff↔congelado, title, casco hielo/ceguera opcional, aviso al staff si desconecta + acción configurable | ✅ | Núcleo de moderación SS |
| Freeze bypass (`ioricore.staffmode.freeze.bypass`) y requisito de estar en staffmode | ✅ | |
| Examine menu (stats + inventario + armadura + efectos + location TP + freeze) | ✅ | Unifica INSPECT/invsee; dos diseños modern/legacy |
| Players menu (lista + TP/examine) | ✅ | Con paginación (SMX no tiene) |
| Staff list en hotbar con servidor | ✅ | Requiere proxy (presencia), ver §4 |
| Blocked commands en staffmode | ✅ | |
| Vanish item con cambio de material ON/OFF por versión | ✅ | Corrige bug §2.4-3 |
| Fuente única de vanish (Essentials + staffmode) | ✅ | Corrige §2.4-5 |
| Cooldown anti doble clic + filtro de mano | ✅ | §2.4-4 |
| Action bar staff (vanish/staffchat/TPS) | ❓ | ¿Choca con actionbars de otros módulos (bossbar tips, freeze)? |
| Efectos configurables (speed/jump) | ❓ | Hoy solo night vision fija |
| Armadura de cuero por rango | ❓ | Estético; ¿se quiere con Lunar glow ya presente? |
| Phase (atravesar paredes) | ❓ | Útil en survival/factions; poco en CTW |
| Follow (montar sobre jugador) | ❓ | Puede ser detectable por el jugador |
| CPS test | ❓ | ¿Hay anticheat externo que ya lo haga? |
| Knockback stick | ❓ | Requiere permitir daño desde staff |
| Chest preview silencioso | ❓ | Útil; implica abrir copia (sin animación) |
| Diamond finder / xray alert | ❓ | Irrelevante en CTW; útil en survival |
| Notas de staff por jugador | ❓ | ¿Ya cubierto por punishments `notes` (MODULE-INVENTORY §5)? |
| Join alerts de staff | ❓ | ¿Proxy ya lo hace (StaffAlertService)? |
| Staffmode persistente entre servidores | ❓ | Ver §4 |
| Warn/report/infractions/helpop/staffchat dentro de staffmode (Bukkit) | ❌ | Ya son proxy o punishments (ownership `MODULE-INVENTORY.md:14-17`) |
| Hook PremiumVanish | ❌ | No se usa en la red (NO VERIFICADO) |
| Random TP round-robin | ❌ | IC ya tiene aleatorio; ❓ si se prefiere cíclico |

---

## 4. Integración

### 4.1 Ownership
| Pieza | Dueño | Coordinación proxy |
|---|---|---|
| Hotbar, menús, protecciones, freeze local, vanish local, inventario | CORE (Bukkit) | — |
| Staffchat | PROXY (`/sc`) | Ítem STAFF-CHAT podría hacer toggle vía plugin message (nuevo tipo) en vez de tip |
| Staff list (quién, rango, servidor, ¿staffmode/vanish?) | PROXY tiene presencia | Registrar `StaffCommand` del proxy; para el menú Core: petición/respuesta por plugin messaging o Redis |
| Vanish visible en red (ocultar de `/glist`, `/find`, TAB global, staff list) | PROXY lee estado | Core publica `VANISH_STATE` |
| Staffmode entre servidores | Core aplica, proxy recuerda | Core publica `STAFFMODE_STATE`; al conectar, proxy/Redis lo reenvía |
| Freeze al cambiar de servidor / desconectar | PROXY puede bloquear `/server` y avisar | Core publica `FREEZE_STATE`; proxy bloquea switch y ejecuta acción de desconexión |
| Staff auth | PROXY gate + CORE enforce | Staffmode debería negar toggle si `StaffAuthEnforceService.isRestricted` |
- Mensajes nuevos de plugin messaging requieren anti-spoof (ver skill security / PROXY-MODULES). PROPUESTA, nada implementado.

### 4.2 Permisos (PROPUESTA; ver PERMISSIONS)
`ioricore.staffmode` (toggle) · `.freeze` · `.freeze.bypass` · `.vanish` · `.inspect` · `.randomtp` · `.players` · `.stafflist` · `.build` · `.chestpreview` · `.teleport` · `.heal` · ❓ `.phase` `.follow` `.cps` `.knockback`. Ver vanish: unificar `ioricore.vanish.see` (hoy también `ioricore.staff`/`ioricore.admin`, `StaffModeService.java:466-472`).

### 4.3 Ranks / punishments / menús / comandos
- Ranks: prefijo nametag `[Staff Mode]` hoy fijo en messages; ❓ usar prefijo del rango (ver RANKS).
- Punishments: Examine → botones "Ban/Mute/Warn" que abran flujo de punishments (ver CORE-MODULES); freeze → acción "ban al desconectar" configurable.
- Menús: Examine, Players, Staff List deben existir como **dos YAML independientes** `menus/modern/*.yml` y `menus/legacy/*.yml` (ver MENUS).
- Comandos: mantener `/staffmode` (staff, sm, mod) — ❓ quitar alias `staff` si el proxy registra `/staff`; ❓ añadir `/freeze` (alias `ss`) top-level (ver COMMANDS).

---

## 5. Propuesta de config (PROPUESTA — no implementado)

`settings.yml`:
```yaml
# ────────────────────────────
# STAFF-MODE
# ────────────────────────────
# ~ PROPUESTA. Comportamiento del modo staff (textos en messages/{lang}.yml)
STAFF-MODE:
  GAMEMODE: SURVIVAL          # ~ NONE = no cambiar
  FLIGHT: true
  INVULNERABLE: true
  RESTORE-LOCATION: true
  VANISH-ON-ENTER: true
  INVENTORY-STORAGE: FILE     # ~ FILE (data/staffmode-inventories.yml) | SQL
  EFFECTS:
    - NIGHT_VISION:0
  BLOCKED-COMMANDS:
    - /sethome
  REQUIRE-STAFF-AUTH: true    # ~ niega toggle si staffauth restringe
  CLICK-COOLDOWN-MS: 150

# ────────────────────────────
# FREEZE
# ────────────────────────────
FREEZE:
  REQUIRE-STAFF-MODE: true
  ALLOWED-COMMANDS:
    - /msg
    - /r
  BLIND: true
  ICE-HELMET: true
  ACTIONBAR-TICKS: 20
  ON-DISCONNECT:              # ~ ejecutados por consola; <player> <staff>
    - "ban <player> 30d Desconexión durante revisión"
```

`menus/modern/staff-mode.yml` (legacy idéntico en estructura, materiales legacy):
```yaml
# *************************************
# * IoriCore | Menu STAFF-MODE        *
# *************************************
# ~ Modern (1.16+). Hotbar de staff. Textos → messages/{lang}.yml (STAFF-MODE.HOTBAR.<KEY>)

# ────────────────────────────
# HOTBAR
# ────────────────────────────
# ~ ACTION: RANDOM-TP | FREEZE | EXAMINE | VANISH | PLAYERS | STAFF-LIST | STAFF-CHAT | PHASE | FOLLOW | CPS | EXIT
# ~ CLICK: RIGHT | LEFT | ANY · TARGET: PLAYER | AIR | ANY
HOTBAR:
  RANDOM-TP:
    Enabled: true
    Slot: 0
    Material: 'COMPASS'
    Data: 0
    Action: RANDOM-TP
    Click: RIGHT
    Target: AIR
    Permission: 'ioricore.staffmode.randomtp'
  FREEZE:
    Enabled: true
    Slot: 1
    Material: 'PACKED_ICE'
    Data: 0
    Action: FREEZE
    Click: RIGHT
    Target: PLAYER
    Permission: 'ioricore.staffmode.freeze'
  EXAMINE:
    Enabled: true
    Slot: 2
    Material: 'CHEST'
    Data: 0
    Action: EXAMINE
    Click: RIGHT
    Target: PLAYER
    Permission: 'ioricore.staffmode.inspect'
  PLAYERS:
    Enabled: true
    Slot: 3
    Material: 'PAPER'
    Data: 0
    Action: PLAYERS
    Click: RIGHT
    Target: AIR
    Permission: 'ioricore.staffmode.players'
  VANISH:
    Enabled: true
    Slot: 4
    Material-On: 'LIME_DYE'
    Data-On: 0
    Material-Off: 'GRAY_DYE'
    Data-Off: 0
    Action: VANISH
    Click: RIGHT
    Target: ANY
    Permission: 'ioricore.staffmode.vanish'
  STAFF-LIST:
    Enabled: true
    Slot: 5
    Material: 'PLAYER_HEAD'
    Data: 0
    Action: STAFF-LIST
    Click: RIGHT
    Target: AIR
    Permission: 'ioricore.staffmode.stafflist'
  STAFF-CHAT:
    Enabled: true
    Slot: 6
    Material: 'BOOK'
    Data: 0
    Action: STAFF-CHAT
    Click: RIGHT
    Target: AIR
    Permission: 'ioricore.staffchat'   # ~ ❓ permiso real del proxy
  EXIT:
    Enabled: true
    Slot: 8
    Material: 'REDSTONE'
    Data: 0
    Action: EXIT
    Click: RIGHT
    Target: ANY
```
Legacy (diferencias): `VANISH` → `Material-On: 'INK_SACK'`, `Data-On: 10`, `Material-Off: 'INK_SACK'`, `Data-Off: 8` · `STAFF-LIST` → `Material: 'SKULL_ITEM'`, `Data: 3`.
Nuevos menús PROPUESTA: `menus/{modern,legacy}/staff-examine.yml`, `staff-players.yml`, `staff-list.yml` (formato MENUS: `TITLE`, `ROWS`, `FILLER`, `CONTENT-SLOTS`, `ITEMS.<KEY>`).

---

## 6. Pendientes
- [ ] Decidir funcionalidades ❓ de §3.
- [ ] Corregir protecciones de staff (§2.4-1) e inventario a disco (§2.4-2) — prioridad.
- [ ] Unificar vanish Essentials/StaffMode (una sola fuente de verdad).
- [ ] Definir mensajes de plugin messaging/Redis para staffmode/vanish/freeze (ver PROXY-MODULES) y registrar `StaffCommand` del proxy.
- [ ] Resolver conflicto alias `/staff`.
- [ ] Verificar en servidor 1.8.9 y 1.20+ el doble evento por mano (§2.4-4).
- [ ] Prueba real con capturas → IMAGE-INDEX (ninguna feature está `IMPLEMENTED_AND_TESTED`).
