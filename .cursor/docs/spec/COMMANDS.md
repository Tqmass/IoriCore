# COMMANDS — catálogo de comandos IoriCore (auditoría 2026-11-03)

> Fuente del **estado real** = código. Intención original = `.cursor/docs/*.md`. Permisos en detalle → [PERMISSIONS.md](PERMISSIONS.md). Módulos → [PROXY-MODULES.md](PROXY-MODULES.md) / [CORE-MODULES.md](CORE-MODULES.md). Estado global → [FEATURE-STATUS.md](FEATURE-STATUS.md).

## 0. Cómo se registran

| Capa | Mecanismo | Evidencia |
|------|-----------|-----------|
| Común | `ProxyCommand` (interfaz) · `CommandParent` (subcomandos + `defaultArgument`, permiso por argumento) · `CommandArgument` | `common/src/main/java/us/iorilabs/proxy/commands/{ProxyCommand,CommandParent,CommandArgument,CommandRegistry}.java` |
| Proxy | `IoriProxy.registerCommands()` → `CommandRegistry.registerConfigured(cmd, commands.yml ALIASES)` → `platform.registerCommands()` | `velocity/.../proxy/IoriProxy.java:542-544,576-677` (idéntico en `bungee/`) |
| Velocity | `BrigadierCommand` con `.requires(perm)` (consola siempre pasa) + greedy `args`; aliases = código ∪ YAML | `velocity/.../velocity/VelocityPlatform.java:136-162`, `velocity/.../velocity/command/VelocityBrigadierCommand.java:19-77` |
| Bungee | `net.md_5.bungee.api.plugin.Command(name, permission, aliases)` + check propio | `bungee/.../bungee/BungeePlatform.java:132-158`, `bungee/.../bungee/command/BungeeProxyCommand.java:17-51` |
| Core (Bukkit) | Cada módulo llama `BukkitCommandBridge.register(plugin, cmd)` → `plugin.getCommand(name)` (debe existir en `plugin.yml`) + `setPermission` + `setAliases` + inyección reflectiva en `knownCommands` (pisa vanilla/Essentials) | `bukkit/.../bukkit/commands/BukkitCommandBridge.java:26-67` |
| Core YAML | `commands.yml`/`permissions.yml` se copian y cargan (`IoriCorePlugin.java:96-102`) pero **ningún código los lee** (cabecera "Stub", `bukkit/src/main/resources/commands.yml:5`). Aliases/permisos Core = hardcode en cada clase. | grep `commands()`/`permissions()` sin usos |

**Diferencias Velocity vs Bungee (proxy)** — mismas clases de comando (`us.iorilabs.proxy.commands.*` idénticas, `diff -rq` vacío):
- Velocity: sin permiso el nodo Brigadier queda **oculto** (`requires`); comportamiento conocido de Velocity 3.x: el comando se reenvía al backend → p. ej. `/ping` cae en el `/ping` de Core (NO VERIFICADO en servidor).
- Bungee: sin permiso, BungeeCord responde con su propio `no_permission` antes de `execute` (el `general.no_permission` de IoriProxy solo sale si el permiso cambia en caliente) (NO VERIFICADO en servidor).
- Ambos: la consola salta el permiso raíz (`VelocityBrigadierCommand.java:69-70`, `BungeeProxyCommand.java:45`).

Leyenda: Consola/Jugador = puede ejecutarlo. "arg" = consola solo con objetivo explícito. ESTADO según brief. `IMPLEMENTED_AND_TESTED (histórico)` = salida propia capturada en `IMAGE-INDEX.md:103-112` (capturas anteriores al refactor de paquete; revalidar).

---

## 1. Proxy (IoriVelo / IoriBungee)

Ruta: `velocity/src/main/java/us/iorilabs/proxy/commands/<Clase>.java` (idéntica en `bungee/`). Aliases: **Y** = `commands.yml`, **C** = hardcode en `aliases()` de la clase (siempre se registran aunque se borren del YAML). Permisos = valor por defecto de `permissions.yml` (resuelto por `CommandsConfig.permission(key)` → `PermissionsConfig`, fallback `ioriproxy.<key>`).

### 1.1 Núcleo (siempre registrado)

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/ioriproxy` (`ROOT.NAME`) | `ROOT.ALIASES` (vacío) | `[help [pág] \| info \| reload]` | `ioriproxy.reload` (todo el comando, no solo reload) | sí | sí | Ayuda paginada, versión/plataforma, recarga configs (`RootCommand.java:19-59`) | IMPLEMENTED_AND_TESTED (histórico, help `2325b427`… IMAGE-INDEX:104) |
| `/redistest` | — | — | `ioriproxy.redistest` | sí | sí | Publica evento `REDIS_TEST` (`RedisTestCommand.java:32`); debug registrado en prod | IMPLEMENTED_UNTESTED |
| `/glist` | gcount (Y+C) | — | `ioriproxy.glist` | sí | sí | Online total y por servidor (`GlistCommand.java`) | IMPLEMENTED_UNTESTED |
| `/announce` | announcer (Y+C), titleannounce (C) | `[servidor\|all\|*] <título> \| <subtítulo>` | `ioriproxy.announce` | sí | sí | Title/subtitle a servidor o red (`AnnounceCommand.java:36-73`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:107) |
| `/alert` | broadcast, bc (Y+C) | `<mensaje>` | `ioriproxy.alert` | sí | sí | Broadcast de red (`AlertCommand.java`) | IMPLEMENTED_UNTESTED |
| `/seen` | inspect, info (Y+C) | `<jugador>` | `ioriproxy.seen` (+ `.admin/.ipaddress/.country` para campos) | sí | sí | Ficha de jugador; IP/país según permiso (`SeenCommand.java:41-54`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |

### 1.2 Routing — flag `MODULES.SEND` / `WHEREIS` / `LOBBY`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/send` | — | `<servidor>` · `<jugador> <servidor>` | `ioriproxy.send` · otros `ioriproxy.send.other` | arg | sí | Mueve a sí mismo / a otro + alerta staff (`SendCommand.java:35-121`) | IMPLEMENTED_UNTESTED |
| `/goto` | — | `<jugador>` | `ioriproxy.goto` | no | sí | Ir al servidor del jugador (cross-proxy vía presencia) (`GotoCommand.java:32-75`) | IMPLEMENTED_UNTESTED |
| `/servers` | server (Y+C) | — (ignora args) | `ioriproxy.servers` | sí | sí | Lista servidores online/offline (`ServersCommand.java:33-60`). ⚠ alias `/server` pisa el built-in y `/server <x>` ya no conecta | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |
| `/find` | whereis (Y+C) | `<jugador>` | `ioriproxy.find` | sí | sí | Servidor + proxy del jugador (`FindCommand.java`) | IMPLEMENTED_UNTESTED |
| `/hub` | lobby (Y+C), vestibulo (Y) | — | `ioriproxy.hub` | no | sí | `router.pickLobby` + send (`HubCommand.java:33-40`) | IMPLEMENTED_UNTESTED |

### 1.3 Maintenance / Blacklist — flags `MAINTENANCE`, `BLACKLIST`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/maintenance` | mantenimiento (Y) | `on [dur] \| on countdown <t> [dur] \| off \| list\|status \| help \| reload \| whitelist add\|remove\|list [j] \| <servidor> [countdown <t>] [dur]` | `ioriproxy.maintenance` (subcomandos sin permiso propio) | sí | sí | Global/por servidor, countdown, whitelist (`MaintenanceCommand.java:16-41`, `argument/MaintenanceOps.java:124-202`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:108; bug "Whitelist - X" `ee4bc9f6`) |
| `/blacklist` | — | `<jugador> [-s] [motivo]` | `ioriproxy.blacklist` | sí | sí | Añade blacklist (`BlacklistCommand.java:38-66`) | IMPLEMENTED_UNTESTED |
| `/unblacklist` | — | `<jugador> [-s] [motivo]` | `ioriproxy.blacklist` (mismo nodo) | sí | sí | Quita blacklist (misma clase, `unblacklist=true`) | IMPLEMENTED_UNTESTED |

### 1.4 Staff — flags `STAFFCHAT`, `ADMINCHAT`, `STAFF-AUTH`, `ANALYTICS`, `ALTS`, `TRANSFER`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/staffchat` | sc (Y+C) | `[mensaje]` (vacío = toggle) | `ioriproxy.staffchat.send` (recibir `.receive`) | no | sí | Canal staff de red (`StaffChatCommand.java:31-44`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:110) |
| `/adminchat` | ac (Y+C) | `[mensaje]` | **ninguno** en raíz (`permission()` = `""`, `AdminChatCommand.java:30-33`); admin = `ioriproxy.adminchat.send`, no-admin = `ioriproxy.request.send` | no | sí | Admin: toggle/enviar; resto: "solicitar admin" (`AdminChatCommand.java:37-70`) | IMPLEMENTED_UNTESTED |
| `/staffauth` | staff-auth, sauth (Y) | `approve <token>` | `ioriproxy.staffauth.approve` (raíz y arg) | sí | sí | Aprueba login staff tras cambio IP/geo (`StaffAuthCommand.java`, `argument/StaffAuthApproveArgument.java`) | IMPLEMENTED_UNTESTED |
| `/analytics` | — | `[versions [pág]]` | `ioriproxy.analytics` | sí | sí | Online/peak/joins/RAM + por servidor; `versions` = clientes/brands (`AnalyticsCommand.java:39-235`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |
| `/alts` | alt, accounts (Y+C), multicuenta (C) | `<jugador>` | `ioriproxy.alts` | sí | sí | Multicuentas por IP (`AltsCommand.java`) | IMPLEMENTED_UNTESTED |
| `/transfer` | migrate (Y+C), accounttransfer (C) | `<origen> <destino>` | `ioriproxy.transfer` | sí | sí | Migra amigos/playtime/whitelist/IPs (`TransferCommand.java`) | IMPLEMENTED_UNTESTED |

### 1.5 Moderación jugador — flags `REPORT`, `HELPOP`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/report` | reportar (Y) | `<jugador> <motivo...>` · `toggle title` | **ninguno** en raíz (`ReportCommand.java:30-33`); interno `ioriproxy.report.send`; toggle `ioriproxy.report.receive` | no | sí | Crea reporte de red + title a staff (`ReportCommand.java:37-97`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:106) |
| `/reports` | reportes (Y) | `[list] \| resolve <id>` | `ioriproxy.report.receive` · resolve `ioriproxy.report.resolve` | sí | sí | Lista pendientes / resuelve (`ReportsCommand.java:17-57`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:106) |
| `/request` | ayuda (Y) | `<mensaje>` · `reply <id> <msg>` · `move <id>` | `ioriproxy.request.send` · reply `ioriproxy.request.reply` · move `.move` o `.reply` | no | sí | HelpOp de red con [REPLY]/[MOVE] (`RequestCommand.java:18-149`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:106) |

### 1.6 Contenido — flags `STREAM`, `DONATIONS`, `GGWAVE`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/stream` | directo (Y) | `<url>` | `ioriproxy.stream` | no | sí | Anuncia directo (`StreamCommand.java`, key `stream`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:107) |
| `/youtube` | video (Y) | `<url>` | `ioriproxy.youtube` | no | sí | Misma clase, key `youtube` (`IoriProxy.java:613`) | IMPLEMENTED_UNTESTED |
| `/donation` | donacion (Y) | `test <jugador> <paquete> <precio>` | `ioriproxy.donations` | sí | sí | Solo simula donación (no hay otros subcomandos) (`DonationCommand.java:36-42`) | PARTIAL |
| `/ggwave` | — | — | `ioriproxy.donations` (no `ioriproxy.ggwave`) | sí | sí | Inicia GG Wave manual (`GgWaveCommand.java:22-34`) | IMPLEMENTED_UNTESTED |

### 1.7 Jugador — flags `PLAYTIME`, `PING`, `QUEUE`, `FRIENDS`

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/playtime` | — | `[jugador]` | `ioriproxy.playtime` · otros `ioriproxy.playtime.others` (+hardcode) | arg | sí | Tiempo jugado local/remoto (`PlaytimeCommand.java:29-70`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |
| `/ping` | latency, ms (Y+C) | `[jugador]` | `ioriproxy.ping` (`none`/vacío = abierto, `PingCommand.java:30-35`) · otros `ioriproxy.ping.other` | arg | sí | Ping local o cross-proxy (`PingCommand.java:44-92`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |
| `/queue` | cola (Y) | `<servidor>` · `leave` | `ioriproxy.queue` | no | sí | Entra/sale de cola con prioridad (`QueueCommand.java:34-51`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:109) |
| `/friend` | friends, amigos, a, f (Y) | `[list] \| add\|accept\|deny <j> \| remove <j> [confirm] \| request cancel <j> \| requests \| msg <j> <msg> \| broadcast <msg> \| help` | `ioriproxy.friends` · broadcast `ioriproxy.friends.broadcast` (interno, `FriendsCommand.java:141`) | no | sí | Amigos de red (default = list) (`FriendsCommand.java:18-32`) | IMPLEMENTED_AND_TESTED (histórico, IMAGE-INDEX:105) |

---

## 2. Core (IoriCore Bukkit/Paper)

Ruta base `bukkit/src/main/java/us/iorilabs/bukkit/`. Todos declarados en `bukkit/src/main/resources/plugin.yml:21-271`. Permiso raíz aplicado por Bukkit (`setPermission`) → sin permiso sale el mensaje **por defecto de Bukkit** (bug histórico `ed145421`, IMAGE-INDEX:112), no `GENERAL.NO-PERMISSION`. Alias = hardcode en `super(...)` (coinciden con `plugin.yml`/`commands.yml` salvo `/ranks`→`rank`, solo código+YAML). Ninguno probado en servidor → `IMPLEMENTED_UNTESTED` salvo nota.

### 2.1 Ranks / Punishments / StaffMode / IP / Skin / Settings / Scoreboard

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | Módulo (flag default) | ESTADO |
|---|---|---|---|---|---|---|---|---|
| `/ioricore` | icore | — | ninguno | — | — | Declarado en `plugin.yml:22-24`; **sin executor** (no hay `onCommand` en `IoriCorePlugin`) → Bukkit muestra uso | — | NOT_IMPLEMENTED |
| `/ranks` | rank | `[list] \| help` | `ioricore.ranks` | no | sí | Menú RankList (`ranks/RanksCommand.java:28-56`) | RANKS (true) | IMPLEMENTED_UNTESTED |
| `/ban` | tempban | `<j> [duración] [-s] [razón]` (tempban exige duración) | `ioricore.punish.ban` | sí | sí | Ban SQL + broadcast staff (`punishment/command/BanCommand.java`, `PunishmentActions.java`) | PUNISHMENTS (true) | IMPLEMENTED_UNTESTED |
| `/mute` | tempmute | `<j> [duración] [-s] [razón]` | `ioricore.punish.mute` | sí | sí | Mute SQL | PUNISHMENTS | IMPLEMENTED_UNTESTED |
| `/warn` | — | `<j> [-s] [razón]` | `ioricore.punish.warn` | sí | sí | Warn SQL | PUNISHMENTS | IMPLEMENTED_UNTESTED |
| `/kick` | — | `<j> [-s] [razón]` | `ioricore.punish.kick` | sí | sí | Kick + registro | PUNISHMENTS | IMPLEMENTED_UNTESTED |
| `/staffmode` | staff, sm, mod | `[toggle] \| freeze <j> \| help` | `ioricore.staffmode` · freeze `ioricore.staffmode.freeze` | solo help | sí | Modo staff con hotbar (`modules/staffmode/command/StaffModeCommand.java:23-73`) | STAFF-MODE (true) | IMPLEMENTED_UNTESTED |
| `/ip` | — | `<jugador>` | `ioricore.ip` (o `.admin`/`.staff`, `IpCommand.java:33-38`) | sí | sí | Info IP/geo staff | IP (true) | IMPLEMENTED_UNTESTED |
| `/skin` | — | `<jugador> [face\|head\|body\|body3d\|full]` | `ioricore.skin` | no | sí | Preview skin async | SKIN (true) | IMPLEMENTED_UNTESTED |
| `/settings` | preferencias | — | `ioricore.settings` | no | sí | Menú de ajustes (nLogin soft) | PLAYER-SETTINGS (true) | IMPLEMENTED_UNTESTED |
| `/scoreboard` | sb, sidebar | — | `ioricore.scoreboard` | no | sí | Toggle sidebar | SCOREBOARD (true) | IMPLEMENTED_UNTESTED |

### 2.2 Economy / Tags / Coinshop / ChatGames (módulos OFF por defecto, `settings.yml:41-46`)

| Comando | Alias | Sintaxis | Permiso | Consola | Jugador | Comportamiento | Módulo | ESTADO |
|---|---|---|---|---|---|---|---|---|
| `/balance` | money, bal | `[jugador]` | `ioricore.economy.balance` · otros `.balance.others` | arg | sí | Saldo | ECONOMY (false) | IMPLEMENTED_UNTESTED |
| `/pay` | — | `<j> <cantidad>` | `ioricore.economy.pay` | no | sí | Transferencia | ECONOMY | IMPLEMENTED_UNTESTED |
| `/eco` | economy | `give\|take\|set <j> <cantidad> \| help` | `ioricore.economy.eco` (raíz y args) | sí | sí | Admin economía | ECONOMY | IMPLEMENTED_UNTESTED |
| `/tag` | tags | `[list] \| equip <id> \| unequip \| help` | `ioricore.tags` · `ioricore.tags.equip` · tag `ioricore.tags.<id>` | no | sí | Lista/equipa tags | TAGS (false) | IMPLEMENTED_UNTESTED |
| `/coinshop` | shop, tienda | — | `ioricore.coinshop` | no | sí | Menú tienda coins | COINSHOP (false) | IMPLEMENTED_UNTESTED |
| `/chatgames` | cg, chatgame | `[help] \| start \| skip\|end` | `ioricore.chatgames` · start/skip `ioricore.chatgames.admin` | sí | sí | Ayuda / forzar / terminar ronda (`ChatGamesCommand.java:23-55`) | CHAT-GAMES (false) | IMPLEMENTED_UNTESTED |

Con el módulo OFF el comando sigue en `plugin.yml` pero sin executor → Bukkit responde con el uso genérico (NO VERIFICADO en servidor).

### 2.3 Essentials (flag `ESSENTIALS` true; registro `modules/essentials/EssentialsModule.java:152-204`)

Permiso = `ioricore.essentials.<cmd>`; todas las clases en `modules/essentials/command/`. `deny()` acepta `ioricore.admin` como bypass **solo dentro** de `execute` (`EssentialsCommand.java:33-37`).

| Comando | Alias | Sintaxis | Extra permiso | Consola | Jugador | Comportamiento | ESTADO |
|---|---|---|---|---|---|---|---|
| `/heal` | — | `[j]` | `.heal.others` | arg | sí | Vida/hambre/fuego | IMPLEMENTED_UNTESTED |
| `/healall` | — | — | — | sí | sí | Cura a todos | IMPLEMENTED_UNTESTED |
| `/feed` | — | `[j]` | `.feed.others` | arg | sí | Hambre | IMPLEMENTED_UNTESTED |
| `/fly` | — | `[j]` | `.fly.others` | arg | sí | Toggle vuelo | IMPLEMENTED_UNTESTED |
| `/god` | — | `[j]` | `.god.others` | arg | sí | Toggle dios | IMPLEMENTED_UNTESTED |
| `/gamemode` | gm, gms, gmc, gma, gmsp | `<modo\|0-3> [j]` · `/gmX [j]` | `.gamemode.others` | arg | sí | Cambia modo | IMPLEMENTED_UNTESTED |
| `/speed` | — | `<valor> [j]` | `.speed.others` | arg | sí | Walk/fly speed | IMPLEMENTED_UNTESTED |
| `/kill` | suicide | `[j]` | `.kill.others` | arg | sí | Mata | IMPLEMENTED_UNTESTED |
| `/weather` | sun, rain, thunder | `<tipo>` | — | sí | sí | Clima | IMPLEMENTED_UNTESTED |
| `/time` | day, night | `<valor>` | — | sí | sí | Hora | IMPLEMENTED_UNTESTED |
| `/top` | — | — | — | no | sí | Bloque más alto | IMPLEMENTED_UNTESTED |
| `/world` | — | `<mundo>` | — | no | sí | Spawn de mundo | IMPLEMENTED_UNTESTED |
| `/tp` | teleport | `<j>` · `<j1> <j2>` | `.tp.others` | arg (2 args) | sí | Teleport | IMPLEMENTED_UNTESTED |
| `/tphere` | — | `<j>` | — | no | sí | Trae jugador | IMPLEMENTED_UNTESTED |
| `/tppos` | teleportpos | `<x> <y> <z> [mundo] [yaw] [pitch]` | — | no | sí | TP coords | IMPLEMENTED_UNTESTED |
| `/back` | — | — | — | no | sí | Última ubicación | IMPLEMENTED_UNTESTED |
| `/nick` | nickname | `<nick\|off>` | — | no | sí | Nick | IMPLEMENTED_UNTESTED |
| `/realname` | — | `<nick>` | — | sí | sí | Nick → nombre | IMPLEMENTED_UNTESTED |
| `/repair` | fix | `[all]` | `.repair.all` | no | sí | Repara ítem(s) | IMPLEMENTED_UNTESTED |
| `/enderchest` | ec, echest | `[j]` | `.enderchest.others` | no | sí | Abre EC | IMPLEMENTED_UNTESTED |
| `/invsee` | — | `<j>` | — | no | sí | Ver inventario | IMPLEMENTED_UNTESTED |
| `/clear` | ci, clearinventory | `[j]` | `.clear.others` | arg | sí | Vacía inventario | IMPLEMENTED_UNTESTED |
| `/broadcast` | bc, bcast | `<msg>` | — | sí | sí | Broadcast servidor | IMPLEMENTED_UNTESTED |
| `/give` | i, item | `<j> <ítem> [n]` · `/i <ítem> [n]` | — | arg | sí | Da ítem | IMPLEMENTED_UNTESTED |
| `/getpos` | coords, position | `[j]` | `.getpos.others` | arg | sí | Coordenadas | IMPLEMENTED_UNTESTED |
| `/disposal` | trash, disposalchest | — | — | no | sí | Basura | IMPLEMENTED_UNTESTED |
| `/workbench` | craft, wb, craftingtable | — | — | no | sí | Mesa crafteo | IMPLEMENTED_UNTESTED |
| `/hat` | — | — | — | no | sí | Ítem como casco | IMPLEMENTED_UNTESTED |
| `/near` | — | `[radio]` | — | no | sí | Jugadores cercanos | IMPLEMENTED_UNTESTED |
| `/ping` | — | `[j]` | `.ping.others` | arg | sí | Ping local (⚠ sombra del proxy) | IMPLEMENTED_UNTESTED |
| `/me` | action | `<acción>` | — | no | sí | Emote | IMPLEMENTED_UNTESTED |
| `/skull` | head | `[j]` | — | no | sí | Cabeza | IMPLEMENTED_UNTESTED |
| `/ignore` | — | `<j>` | — | no | sí | Toggle ignore PM | IMPLEMENTED_UNTESTED |
| `/vanish` | v | `[j]` | `.vanish.others` · ver `ioricore.vanish.see` | arg | sí | Vanish local | IMPLEMENTED_UNTESTED |
| `/spawn` | — | `[j]` | `.spawn.others` | arg | sí | Spawn | IMPLEMENTED_UNTESTED |
| `/setspawn` | — | — | — | no | sí | Fija spawn | IMPLEMENTED_UNTESTED |
| `/home` | — | `[nombre]` | — | no | sí | Ir a home | IMPLEMENTED_UNTESTED |
| `/sethome` | — | `[nombre]` | `.sethome.unlimited` | no | sí | Crea home | IMPLEMENTED_UNTESTED |
| `/delhome` | deletehome | `<nombre>` | — | no | sí | Borra home | IMPLEMENTED_UNTESTED |
| `/homes` | — | — | — | no | sí | Lista homes | IMPLEMENTED_UNTESTED |
| `/warp` | warps | `[nombre\|list]` | — | solo list | sí | Ir/listar warps | IMPLEMENTED_UNTESTED |
| `/setwarp` | — | `<nombre>` | — | no | sí | Crea warp | IMPLEMENTED_UNTESTED |
| `/delwarp` | deletewarp | `<nombre>` | — | sí | sí | Borra warp | IMPLEMENTED_UNTESTED |
| `/kit` | kits | `[nombre]` | `ioricore.kit.<nombre>` (por kit, `kit/KitService.java:60`) | no | sí | Reclama/lista kits | IMPLEMENTED_UNTESTED |
| `/createkit` | kitcreate | `<nombre>` | — | no | sí | Kit desde inventario | IMPLEMENTED_UNTESTED |
| `/deletekit` | kitdelete | `<nombre>` | — | sí | sí | Borra kit | IMPLEMENTED_UNTESTED |
| `/tpa` | — | `<j>` | — | no | sí | Petición TP | IMPLEMENTED_UNTESTED |
| `/tpahere` | — | `<j>` | — | no | sí | Petición "ven" | IMPLEMENTED_UNTESTED |
| `/tpaccept` | tpyes | — | — | no | sí | Acepta | IMPLEMENTED_UNTESTED |
| `/tpdeny` | tpno | — | — | no | sí | Rechaza | IMPLEMENTED_UNTESTED |
| `/tpcancel` | tpacancel, tpc | — | — | no | sí | Cancela petición | IMPLEMENTED_UNTESTED |
| `/msg` | tell, w, whisper, message | `<j> <msg>` | — | no | sí | PM local (solo mismo backend) | IMPLEMENTED_UNTESTED |
| `/reply` | r | `<msg>` | — | no | sí | Responde PM | IMPLEMENTED_UNTESTED |

---

## 3. Declarados pero no registrados

| Elemento | Dónde se declara | Realidad |
|---|---|---|
| `/ioricore` (`icore`) | `bukkit/.../plugin.yml:22-24`, `commands.yml` `ROOT.NAME` | Sin executor ni reload Core → NOT_IMPLEMENTED |
| `/staff list` (proxy) | `.cursor/docs/features.md:10`, `.cursor/docs/phases.md:51`, `messages/*.yml` `STAFF.USAGE` | `StaffCommand.java` existe (114 líneas) pero **no** se instancia en `IoriProxy.registerCommands()` y no tiene entrada en `commands.yml` → PARTIAL (código no conectado) |
| Core `commands.yml` / `permissions.yml` completos | `bukkit/src/main/resources/commands.yml`, `permissions.yml` | Cargados, nunca leídos: editar aliases/permisos ahí **no tiene efecto** |
| Proxy `commands.yml` clave `PERMISSION` | soportada por `CommandsConfig.java:55-58` | Ninguna entrada la usa (solo informativo) |

## 4. Registrados sin entrada YAML / desalineados

| Caso | Detalle |
|---|---|
| Aliases solo en código (proxy) | `alts`→`multicuenta`; `announce`→`titleannounce`; `transfer`→`accounttransfer` (no están en `commands.yml`, no se pueden quitar por config) |
| Aliases código **y** YAML | `CommandRegistry.registerConfigured` + `registerCommands` unen ambos (`CommandRegistry.java:23-36`, `VelocityPlatform.java:140-147`): borrar un alias del YAML no lo elimina si está en `aliases()` |
| `/ranks` alias `rank` | En código + `commands.yml` Core, no en `plugin.yml`; se registra por reflexión (`BukkitCommandBridge.java:56-61`) |
| Clave de permiso ≠ nombre | `/friend` usa clave `friends`; `/ggwave` usa `donations`; `/reports` usa `report_receive`; `/request` usa `request_send` |

## 5. Conflictos de nombre/alias

| Label | Proxy | Core / built-in | Efecto |
|---|---|---|---|
| `/ping` | `PingCommand` (+`latency`,`ms`) si `MODULES.PING` | Core `/ping` (`ioricore.essentials.ping`, default true) | Con permiso proxy gana proxy; sin él Velocity reenvía al backend (NO VERIFICADO) → UX distinta según permiso |
| `/broadcast`, `/bc` | alias de `/alert` | Core `/broadcast` (`bc`,`bcast`) | Core inalcanzable por esos labels para quien tenga `ioriproxy.alert`; `/bcast` sí llega a Core |
| `/staff` | `StaffCommand` (no registrado) | alias de Core `/staffmode` | Hoy sin conflicto; si se registra `/staff list` en proxy, rompe `/staff` → staffmode |
| `/msg` | solo `/friend msg` | Core `/msg` (+tell,w,whisper,message) | Sin conflicto de label. Core `/msg` es **local al backend** (no cross-server) |
| `/seen`, `/info`, `/inspect` | proxy `/seen` | MODULE-INVENTORY pide `seen` en Core (stub→proxy) | Sin conflicto (Core no lo implementa) |
| `/server` | alias de `/servers` | built-in Velocity/Bungee `/server <x>` | Pisa el built-in: `/server lobby` lista en vez de conectar (NO VERIFICADO en servidor) |
| `/send`, `/glist`, `/find`, `/alert` | registrados | built-ins Velocity (`send`,`glist`,`server`) / módulos Bungee (`cmd_send`,`cmd_list`,`cmd_find`,`cmd_alert`) | Prioridad depende de plataforma/orden de carga (NO VERIFICADO) |
| `/ip` | — | Core `/ip` vs Bungee built-in `/ip` (`bungeecord.command.ip`) | En Bungee el built-in intercepta antes del backend (NO VERIFICADO) |
| `/transfer` | proxy | vanilla 1.20.5+ `/transfer` en backend | Proxy intercepta primero |
| `/a`, `/f` | alias de `/friend` | comunes en otros plugins | Riesgo bajo |
| vanilla `/tp /give /kill /time /weather /gamemode /clear /me /msg /tell /w` | — | Core las pisa vía `knownCommands` | Intencional |
| **Command whitelist proxy** | `settings.yml:61-96` `MODE: WHITELIST`, grupo `DEFAULT` solo `hub, lobby, friend, friends, ping, playtime, report, request, queue, glist, seen, announce` | `CommandExecuteEvent`/`ChatEvent` bloquea **todo** comando (también los de Core) | Jugadores sin `ioriproxy.staff`/bypass no pueden usar `/msg /r /spawn /home /tpa /kit /settings /tag /coinshop /skin /warp…` ni aliases `f, amigos, cola, reportar, ayuda, vestibulo` (`CommandBlockerService.java:19-41`, `VelocityCommandBlockerListener.java:20-27`) |

## 6. Pedidos en doc, no implementados

| Fuente | Comandos | ESTADO |
|---|---|---|
| `MODULE-INVENTORY.md:44` (essentials) | `anvil`, `enchant`, `more`, `seen` (Core), `burn`, `extinguish`, `lightning`, `ptime`, `pweather`, `tpall`, `jump`, `thru`, `kittycannon`, `socialspy`, `mail`, `sudo`, `powertool`, `loom`, `grindstone`, `smithing`, `compass` | NOT_IMPLEMENTED |
| `MODULE-INVENTORY.md:65` (punishments) | `notes`; además no existen `unban`, `unmute`, `history`/`check` (necesarios para cerrar el ciclo) | NOT_IMPLEMENTED |
| `MODULE-INVENTORY.md:61` (ranks) | RankEdit (¿subcomando/menú desde `/ranks`?) | NO VERIFICADO (ver [MENUS.md](MENUS.md)) |
| `MODULE-INVENTORY.md:134` | Discord JDA ban/mute… (slash). Hoy solo slash genéricos por `type` (`discord/DiscordBotService.java:406-414`) | PARTIAL (ver [INTEGRATIONS.md](INTEGRATIONS.md)) |
| `EXTRAS_BACKLOG.md:10` | Chat lock/mute (`/mutechat`/`/lockchat`) + `*.bypass` | NOT_IMPLEMENTED |
| `features.md:10`, `phases.md:51` | `/staff list` | PARTIAL (ver §3) |
| Core `permissions.yml` `RELOAD` | `/ioricore reload` | NOT_IMPLEMENTED |
