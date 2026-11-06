# PERMISSIONS — catálogo de permisos IoriCore (auditoría 2026-11-03)

> Comandos → [COMMANDS.md](COMMANDS.md). Configuración → [CONFIGURATIONS.md](CONFIGURATIONS.md). Staff mode → [STAFFMODE.md](STAFFMODE.md). Estado global → [FEATURE-STATUS.md](FEATURE-STATUS.md).

## 0. Cómo se resuelven

| Plataforma | Resolución | Defaults | Evidencia |
|---|---|---|---|
| Proxy | `CommandsConfig.permission(key)` → override `COMMANDS.<X>.PERMISSION` (no usado) → `PermissionsConfig` (`PERMISSIONS` + `EXTRA-PERMISSIONS`, key normalizada `_`→`-`) → fallback `"ioriproxy." + key` | **Ninguno**: Velocity/Bungee no tienen `default`; sin LuckPerms todo = false salvo consola | `velocity/.../config/CommandsConfig.java:67-73`, `velocity/.../config/PermissionsConfig.java:9-31` |
| Proxy (otros YAML) | `maintenance.yml BYPASS-PERMISSION`, `queue.yml BYPASS-PERMISSION`/`GROUPS.*.PERMISSION`, `settings.yml FRIENDS.MAX-FRIENDS.GROUPS.*.PERMISSION`, `COMMAND-WHITELIST.GROUPS.*.PERMISSION`, `STAFF-AUTH.STAFF-PERMISSION` | — | `MaintenanceConfig.java:32-33`, `QueueConfig.java:44-57`, `FriendsConfig.java:23-28`, `CommandWhitelistConfig.java:55-60`, `StaffAuthConfig.java:26` |
| Core | **Hardcode** en cada clase. `permissions.yml` Core se carga y nunca se lee. `plugin.yml` da defaults y children | `plugin.yml permissions:` | `bukkit/.../IoriCorePlugin.java:97-102`, `bukkit/src/main/resources/plugin.yml:272-559` |
| Core (otros YAML) | `settings.yml JOIN.STAFF-PERMISSION/SEE-PERMISSION`, `STAFF-AUTH.STAFF-PERMISSION`; `chat/formats/*.yml PERMISSION`; `data/tags.yml PERMISSION`; kits `permission:` | — | `JoinStatusListener.java:26-27`, `StaffAuthEnforceModule.java:36`, `ChatFormatService.java:87`, `TagService.java:93-94`, `KitService.java:60` |

Columnas: **Decl.** = `permissions.yml` (P) / `plugin.yml` (PL) / otro YAML (Y). **Código** = ruta donde se comprueba (base `velocity/src/main/java/us/iorilabs/proxy/` idéntica en bungee; `bukkit/src/main/java/us/iorilabs/bukkit/`). **Doc** = mencionado en `.cursor/docs` (fuera de spec). Ningún permiso tiene prueba en servidor documentada → estado máximo `IMPLEMENTED_UNTESTED`.

---

## 1. `ioriproxy.*` (Velocity + Bungee, `permissions.yml` idéntico)

| Permiso | Funcionalidad | Módulo | Comandos | Jerarquía | Default | Decl. | Código | Doc | ESTADO |
|---|---|---|---|---|---|---|---|---|---|
| `ioriproxy.reload` | Todo `/ioriproxy` (help/info/reload) | core | `/ioriproxy` | — | no declarado | P | `commands/RootCommand.java:22` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.redistest` | Test pub/sub | redis | `/redistest` | — | no declarado | P | `commands/RedisTestCommand.java:31` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.send` | Enviar a servidor | send | `/send` | padre de `.other` (no automático) | no declarado | P | `commands/SendCommand.java:30` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.send.other` | Enviar a otros | send | `/send <j> <s>` | `.other` (singular) | no declarado | P | `commands/SendCommand.java:81,132` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.goto` | Ir al servidor de un jugador | send | `/goto` | — | no declarado | P | `commands/GotoCommand.java:27` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.find` | Localizar jugador | whereis | `/find` `/whereis` | — | no declarado | P (`FIND` y `WHEREIS`) | `commands/FindCommand.java:32` | `phases.md:32` | IMPLEMENTED_UNTESTED |
| `ioriproxy.servers` | Lista servidores | send | `/servers` `/server` | — | no declarado | P | `commands/ServersCommand.java:29` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.hub` | Ir al lobby | lobby | `/hub` `/lobby` | — | no declarado | P | `commands/HubCommand.java:28` | `phases.md:32` | IMPLEMENTED_UNTESTED |
| `ioriproxy.playtime` | Tiempo jugado propio | playtime | `/playtime` | — | no declarado | P | `commands/PlaytimeCommand.java:29` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.playtime.others` | Tiempo de otros | playtime | `/playtime <j>` | `.others` (plural) | no declarado | P (`PLAYTIME-OTHER`) | `commands/PlaytimeCommand.java:42-43,110-112` (+ literal duplicado) | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.ping` | Ping propio | ping | `/ping` | `none` = abierto | no declarado | P | `commands/PingCommand.java:30-35` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.ping.other` | Ping de otros | ping | `/ping <j>` | `.other` (singular) | no declarado | P | `commands/PingCommand.java:45,104` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.friends` | Sistema de amigos | friends | `/friend` | — | no declarado | P | `commands/FriendsCommand.java:18` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.friends.broadcast` | `/friend broadcast` | friends | `/friend broadcast` | hijo lógico | no declarado | P | `commands/FriendsCommand.java:141` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.friends.spy` | Ver PM de amigos | friends | — (pasivo) | — | no declarado | P | `player/friends/FriendService.java:427-433` (fallback literal) | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.friends.limit.vip` / `.staff` | Límite de amigos 100/200 | friends | — | gana el MAX más alto | no declarado | P (EXTRA, **ignorado**) + Y `settings.yml:51,54` (fuente real) | `config/FriendsConfig.java:23-28` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.glist` | Online global | core | `/glist` | — | no declarado | P | `commands/GlistCommand.java:33` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.announce` | Title announce | announcements | `/announce` | — | no declarado | P | `commands/AnnounceCommand.java:31` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.alert` | Broadcast red | core | `/alert` `/bc` | — | no declarado | P | `commands/AlertCommand.java:28` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.seen` | Ficha jugador | core | `/seen` | — | no declarado | P | `commands/SeenCommand.java:41` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.seen.admin` | Todos los campos | core | `/seen` | implica ip+country (en código) | no declarado | P | `commands/SeenCommand.java:52-54` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.seen.ipaddress` / `.country` | Mostrar IP / país | core | `/seen` | — | no declarado | P | `commands/SeenCommand.java:53-54` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.alts` | Multicuentas | alts | `/alts` | — | no declarado | P | `commands/AltsCommand.java:32` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.transfer` | Migrar cuenta | transfer | `/transfer` | — | no declarado | P | `commands/TransferCommand.java:31` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.server.health` | Alertas caída/subida backend | server-health | — | — | no declarado | P | `velocity/IoriProxyVelocity.java:72`, `bungee/IoriProxyBungee.java:45`, `server/ServerHealthMonitor.java:53,65` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.staff` | Raíz staff: isStaff, grupo command-whitelist STAFF (`*`), staff-auth candidato, Discord→MC staffchat | staff | — | **no** es padre real de `ioriproxy.staff.*` | no declarado | P + Y `settings.yml:94,165` | `IoriProxy.java:186,480` → `staff/StaffGroups.java:27`; `config/StaffAuthConfig.java:26`; `discord/DiscordBotService.java:252` (literal) | `features.md:27`, `phases.md:51` | IMPLEMENTED_UNTESTED |
| `ioriproxy.staff.list` | `/staff list` | staff | (no registrado) | — | no declarado | P | solo `commands/StaffCommand.java:29` (clase no registrada) | `phases.md:51` | PARTIAL |
| `ioriproxy.staff.admin` / `.mod` / `.helper` | Rango display sin LP | staff | staffchat/list | `STAFF.GROUPS` manda; `EXTRA` solo fallback | no declarado | P (`STAFF.GROUPS` + `EXTRA`) | `config/PermissionsConfig.java:33-47`, `staff/StaffGroups.java:29-38` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.staffauth.approve` | Aprobar sesión staff | staff-auth | `/staffauth approve` | raíz = arg | no declarado | P (`STAFFAUTH`) | `commands/StaffAuthCommand.java:14`, `argument/StaffAuthApproveArgument.java:14` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.alerts` | Recibir alertas staff (send…) | staff | — | — | no declarado | P | `staff/StaffAlertService.java:20,70` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.staffchat.send` / `.receive` | Staff chat | staffchat | `/sc` | — | no declarado | P | `commands/StaffChatCommand.java:31`, `staffchat/StaffChatService.java:42-43`; `.receive` literal en `discord/DiscordBotService.java:252` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.adminchat.send` / `.receive` | Admin chat | adminchat | `/ac` | — | no declarado | P | `commands/AdminChatCommand.java:48`, `adminchat/AdminChatService.java:42-43` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.analytics` | Analytics | analytics | `/analytics` | — | no declarado | P | `commands/AnalyticsCommand.java:35` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.maintenance` | Gestionar mantenimiento (todos los subcomandos) | maintenance | `/maintenance` | — | no declarado | P | `commands/MaintenanceCommand.java:19` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.maintenance.bypass` | Entrar en mantenimiento | maintenance | — | + literal `ioriproxy.maintenance.*` | no declarado | P (EXTRA, **ignorado**) + Y `maintenance.yml:12` (fuente real) | `config/MaintenanceConfig.java:33`, `maintenance/MaintenanceService.java:109` | `features.md:27` | IMPLEMENTED_UNTESTED |
| `ioriproxy.whitelist` | ? | maintenance | — | — | no declarado | P | **nunca comprobado** | — | NOT_IMPLEMENTED |
| `ioriproxy.blacklist` | Blacklist y unblacklist | blacklist | `/blacklist` `/unblacklist` | — | no declarado | P | `commands/BlacklistCommand.java:27` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.report.send` | Reportar | report | `/report` | raíz del comando abierta | no declarado | P | `commands/ReportCommand.java:47` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.report.receive` | Ver/recibir reportes, toggle title | report | `/reports`, `/report toggle` | — | no declarado | P | `commands/ReportCommand.java:91,114,136`, `ReportsCommand.java:17,57`, `report/ReportService.java:156,191` | `features.md:27` | IMPLEMENTED_UNTESTED |
| `ioriproxy.report.resolve` | Resolver | report | `/reports resolve` | arg | no declarado | P | `commands/ReportsCommand.java:23` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.report.title.bypass` | No recibir title de reporte | report | — | — | no declarado | P | `report/ReportService.java:171,180` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.request.send` | HelpOp; también `/ac` para no-admins | helpop | `/request`, `/ac` | — | no declarado | P | `commands/RequestCommand.java:21,36`, `AdminChatCommand.java:68` | `phases.md:32` (`request.*`) | IMPLEMENTED_UNTESTED |
| `ioriproxy.request.receive` | Recibir HelpOp y solicitudes `/ac` | helpop | — | — | no declarado | P | `helpop/HelpOpService.java:155,167`, `adminchat/AdminChatService.java:44` | `phases.md:32` | IMPLEMENTED_UNTESTED |
| `ioriproxy.request.reply` | Responder (y mover) | helpop | `/request reply` | `.reply` también habilita move | no declarado | P | `commands/RequestCommand.java:28,106-107,149` | `phases.md:32`; `features.md:27` lo llama `ioriproxy.helpop.reply` (obsoleto) | IMPLEMENTED_UNTESTED |
| `ioriproxy.request.move` | Ir al servidor del ticket | helpop | `/request move` | — | no declarado | P | `commands/RequestCommand.java:105-107` | `phases.md:32` | IMPLEMENTED_UNTESTED |
| `ioriproxy.stream` / `ioriproxy.youtube` | Anunciar directo/vídeo | stream | `/stream` `/youtube` | — | no declarado | P | `commands/StreamCommand.java` (`permission(commandKey)`) | `phases.md:32` (youtube) | IMPLEMENTED_UNTESTED |
| `ioriproxy.donations` | Test donación y GG Wave | donations | `/donation`, `/ggwave` | — | no declarado | P | `commands/DonationCommand.java:26`, `GgWaveCommand.java:23` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.queue` | Usar cola | queue | `/queue` | — | no declarado | P | `commands/QueueCommand.java:24` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.queue.bypass` | Saltar cola | queue | — | — | no declarado | P (EXTRA, **ignorado**) + Y `queue.yml:9` (fuente real) | `config/QueueConfig.java:57`, `server/QueueService.java:37` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.queue.priority.vip` / `.staff` | Prioridad cola | queue | — | — | no declarado | P (EXTRA, **ignorado**) + Y `queue.yml:28,31` (fuente real) | `config/QueueConfig.java:44-48`, `server/QueueService.java:92` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.commandwhitelist.bypass` | Saltar whitelist/filtro tab | command-whitelist | todos | — | no declarado | P (EXTRA) | `config/CommandWhitelistConfig.java:38`, `commands/CommandBlockerService.java:20`, `TabCompleteFilterService.java:40` | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.commandwhitelist.notify` | Aviso de comando bloqueado | command-whitelist | — | — | no declarado | P (EXTRA) | `velocity/impl/listener/VelocityCommandBlockerListener.java:29-37` (+ Bungee equivalente) | — | IMPLEMENTED_UNTESTED |
| `ioriproxy.maintenance.*` | Wildcard literal | maintenance | — | comprobado como string literal | no declarado | — | `maintenance/MaintenanceService.java:109` | — | IMPLEMENTED_UNTESTED |

**Comandos abiertos (sin permiso raíz):** `/adminchat` (`AdminChatCommand.java:30-33`), `/report` (`ReportCommand.java:30-33`), `/ping` si `PING: none`.

## 2. `ioricore.*` (Bukkit, `plugin.yml` = única fuente efectiva de defaults)

Essentials: todos `ioricore.essentials.<cmd>`, comprobado en `modules/essentials/command/<Cmd>Command.java` (constructor + `deny()` en `execute`), declarado en PL y P. Casi todos los `.others` **no están declarados**: sin declarar valen false salvo para OP, y `ioricore.admin` no los concede.

| Permiso | Funcionalidad | Módulo | Comandos | Jerarquía | Default | Decl. | Código | Doc | ESTADO |
|---|---|---|---|---|---|---|---|---|---|
| `ioricore.admin` | Bypass "admin" en `deny()`/checks | todos | todos | **sin `children`** → no es wildcard; Bukkit bloquea antes por `setPermission` | op | PL, P | `EssentialsCommand.java:34`, `StaffModeCommand.java:38,60`, `punishment/command/*Command.java:31-32`, `ranks/RanksCommand.java:42`, `ChatGamesCommand.java:32-55`, `CoinshopCommand.java:40`, `EconomyCommandBase.java:49`, `TagCommand.java:45`, `IpCommand.java:36`, `ScoreboardCommand.java:27`, `VanishService.java:103`, `StaffModeService.java:471`, `StaffModeListener.java:155-197`, `PunishmentActions.java:144`, `JoinStatusListener.java:54` | `ARCHITECTURE.md:48` (`ioricore.*`) | PARTIAL |
| `ioricore.reload` | Recarga Core | core | (no existe) | — | no declarado (PL) | P | **nunca comprobado** | — | NOT_IMPLEMENTED |
| `ioricore.ranks` | Menú rangos | ranks | `/ranks` | — | op | PL, P | `ranks/RanksCommand.java:28,42` | — | IMPLEMENTED_UNTESTED |
| `ioricore.punish.ban` / `.mute` / `.warn` / `.kick` | Sanciones | punishments | `/ban /mute /warn /kick` | — | op | PL, P | `punishment/command/{Ban,Mute,Warn,Kick}Command.java:24-32` | — | IMPLEMENTED_UNTESTED |
| `ioricore.staffmode` | Modo staff | staffmode | `/staffmode` | children PL: freeze, vanish, inspect, randomtp | op | PL, P | `StaffModeCommand.java:25,38`, `argument/StaffModeToggleArgument.java:14`, `StaffModeHelpArgument.java:14` | `settings.yml:189` comentario | IMPLEMENTED_UNTESTED |
| `ioricore.staffmode.freeze` | Congelar | staffmode | `/staffmode freeze`, hotbar | hijo | op | PL, P | `StaffModeCommand.java:60`, `StaffModeFreezeArgument.java:14`, `StaffModeListener.java:155` | — | IMPLEMENTED_UNTESTED |
| `ioricore.staffmode.inspect` / `.vanish` / `.randomtp` | Hotbar staff | staffmode | — | hijos | op | PL, P | `StaffModeListener.java:164,188,197` | — | IMPLEMENTED_UNTESTED |
| `ioricore.staff` | Marca staff Core (broadcast sanciones, ver vanish, `/ip`, logs, join-status, staff-auth candidato) | varios | — | — | op | PL, P, Y `settings.yml:77` | `PunishmentActions.java:144`, `VanishService.java:102`, `StaffModeService.java:470`, `IpCommand.java:37`, `ChatLogListener.java:38`, `ChatLogService.java:76`, `JoinStatusListener.java:26`, `StaffAuthEnforceService.java:105` | — | IMPLEMENTED_UNTESTED |
| `ioricore.settings` | `/settings` | player-settings | `/settings` | — | true | PL, P | `modules/settings/command/SettingsCommand.java:35` | — | IMPLEMENTED_UNTESTED |
| `ioricore.skin` | `/skin` | skin | `/skin` | — | true | PL, P | `modules/skin/command/SkinCommand.java:29` | — | IMPLEMENTED_UNTESTED |
| `ioricore.ip` | `/ip` | ip | `/ip` | alternativa `.admin`/`.staff` | op | PL, P | `modules/ip/command/IpCommand.java:27,35` | — | IMPLEMENTED_UNTESTED |
| `ioricore.scoreboard` | Toggle sidebar | scoreboard | `/scoreboard` | — | true | PL, P | `modules/scoreboard/command/ScoreboardCommand.java:20,27` | — | IMPLEMENTED_UNTESTED |
| `ioricore.vanish.see` | Ver vanished | essentials/staffmode | — | o `.staff`/`.admin` | op | PL, P | `VanishService.java:17,101`, `StaffModeService.java:37,469` | — | IMPLEMENTED_UNTESTED |
| `ioricore.world.bypass` | Ignorar flags de mundo | essentials | — | — | op | PL, P | `modules/essentials/WorldManagementListener.java:109,116` | — | IMPLEMENTED_UNTESTED |
| `ioricore.essentials.<cmd>` (53: heal, healall, feed, fly, god, gamemode, speed, kill, weather, time, top, world, tp, tphere, tppos, back, nick, realname, repair, enderchest, invsee, clear, broadcast, give, getpos, disposal, workbench, hat, near, ping, me, skull, ignore, vanish, spawn, setspawn, home, sethome, delhome, homes, warp, setwarp, delwarp, kit, createkit, deletekit, tpa, tpahere, tpaccept, tpdeny, tpcancel, msg, reply) | Comando homónimo | essentials | `/<cmd>` | — | `true`: top, back, enderchest, spawn, home, sethome, delhome, homes, warp, tpa, tpahere, tpaccept, tpdeny, msg, reply, getpos, disposal, kit, workbench, hat, near, ping, tpcancel, me, ignore · resto `op` | PL, P | `modules/essentials/command/*Command.java` | `MODULE-INVENTORY.md:44` (lista de comandos, no permisos) | IMPLEMENTED_UNTESTED |
| `ioricore.essentials.ping.others` | Ping de otros | essentials | `/ping <j>` | `.others` | op | PL, P | `PingCommand.java:25` | — | IMPLEMENTED_UNTESTED |
| `ioricore.essentials.{heal,feed,fly,god,gamemode,speed,kill,clear,enderchest,getpos,spawn,tp,vanish}.others` | Aplicar a otros | essentials | `/<cmd> <j>` | `.others` | **no declarado** (false salvo OP) | — | `HealCommand.java:28`, `FeedCommand.java:28`, `FlyCommand.java:29`, `GodCommand.java:32`, `GamemodeCommand.java:48`, `SpeedCommand.java:41`, `KillCommand.java:25`, `ClearCommand.java:28`, `EnderchestCommand.java:24`, `GetPosCommand.java:26`, `SpawnCommand.java:33`, `TpCommand.java:42`, `VanishCommand.java:32` | — | PARTIAL |
| `ioricore.essentials.repair.all` | `/repair all` | essentials | `/repair all` | sub | **no declarado** | — | `RepairCommand.java:24` | — | PARTIAL |
| `ioricore.essentials.sethome.unlimited` | Homes ilimitados | essentials | `/sethome` | sub | **no declarado** | — | `SetHomeCommand.java:32` | — | PARTIAL |
| `ioricore.kit.<nombre>` | Acceso por kit (configurable en kit yml `permission:`) | essentials | `/kit <n>` | dinámico | **no declarado** | Y (kits/*.yml generados) | `modules/essentials/kit/KitService.java:60,88,98` | — | IMPLEMENTED_UNTESTED |
| `ioricore.economy.balance` / `.balance.others` / `.pay` / `.eco` | Economía | economy (OFF) | `/balance /pay /eco` | `.others` | true / op / true / op | PL, P | `modules/economy/command/{Balance,Pay,Eco}Command.java`, `argument/Eco{Give,Take,Set}Argument.java` | — | IMPLEMENTED_UNTESTED |
| `ioricore.tags` / `.tags.equip` | Tags | tags (OFF) | `/tag` | — | true / true | PL, P | `modules/tags/command/TagCommand.java:30,45`, `argument/Tag{Equip,Unequip}Argument.java` | — | IMPLEMENTED_UNTESTED |
| `ioricore.tags.<id>` (heart, star, crown, verified, dollar, fire, ice, lightning, flower, moon, sun, skull, sword, bow, music, king, queen, vip, elite, christmas, halloween, ultra…) | Poseer tag (se compra en coinshop) | tags | `/tag equip` | dinámico | solo heart/star/crown declarados (op); resto **no declarado** | PL (3), P (3), Y `data/tags.yml` | `modules/tags/TagService.java:93-94` (desde YAML) | — | IMPLEMENTED_UNTESTED |
| `ioricore.coinshop` | Tienda | coinshop (OFF) | `/coinshop` | — | true | PL, P | `CoinshopCommand.java:27,40` | — | IMPLEMENTED_UNTESTED |
| `ioricore.chatgames` / `.chatgames.admin` | ChatGames / forzar-skip | chatgames (OFF) | `/chatgames` | `.admin` | true / op | PL, P | `ChatGamesCommand.java:24-55` | — | IMPLEMENTED_UNTESTED |
| `ioricore.chat.format.owner` / `.staff` | Formato de chat | chat-format | — | — | **no declarado** | P, Y `chat/formats/*.yml` | `modules/chatformat/ChatFormatService.java:87` (desde YAML) | — | IMPLEMENTED_UNTESTED |
| `ioricore.logs.staff` / `.logs.donor` | Carpeta de log | chat-logs | — | o `.staff` | **no declarado** | P | `ChatLogService.java:75-76`, `ChatLogListener.java:41` | — | IMPLEMENTED_UNTESTED |
| `ioricore.rank.owner` / `.staff` / `.donor` | Etiqueta de rango en logs | chat-logs | — | owner o `isOp()` | **no declarado** | P | `ChatLogListener.java:35-41` | — | IMPLEMENTED_UNTESTED |
| `ioricore.join.brand` | Ver línea join-status | join | — | o `JOIN.STAFF-PERMISSION` | **no declarado** | P, Y `settings.yml:78` | `JoinStatusListener.java:27,55` | — | IMPLEMENTED_UNTESTED |

## 3. Otros prefijos

| Permiso | Dónde | Detalle | ESTADO |
|---|---|---|---|
| `ioriproxy.staff` **en Core** | `bukkit/.../settings.yml:201` → `StaffAuthEnforceModule.java:36`, `StaffAuthEnforceService.java:27-28,104` | Backend comprueba un nodo `ioriproxy.*`; requiere LuckPerms sincronizado en backend | IMPLEMENTED_UNTESTED (módulo `STAFF-AUTH: false`) |
| Discord `MODERATE_MEMBERS` / `BAN_MEMBERS` / `ADMINISTRATOR` | `discord/DiscordGatewayListener.java:172,189` | Permisos de Discord para botones/slash (ver [INTEGRATIONS.md](INTEGRATIONS.md)) | IMPLEMENTED_UNTESTED |
| BungeeCord built-ins (`bungeecord.command.*`) | plataforma | Pueden interceptar `/ip`, `/send`, `/glist`, `/find`, `/alert`, `/server` (ver COMMANDS §5) | NO VERIFICADO |

---

## 4. Inconsistencias

### 4.1 Comprobados en código, no declarados
- Core `.others` ×13, `repair.all`, `sethome.unlimited` → sin entrada en `plugin.yml` ni `permissions.yml` (default false; solo OP). Rompe la convención usada en `ping.others`/`balance.others`.
- Core `ioricore.kit.<n>`, `ioricore.tags.<id>` (19 de 22 tags), `chat.format.*`, `logs.*`, `rank.*`, `join.brand` → no están en `plugin.yml` (los cuatro últimos sí en `permissions.yml`, que no se lee).
- Proxy `ioriproxy.maintenance.*` (literal wildcard, `MaintenanceService.java:109`): LuckPerms ya resuelve wildcards; comprobar el string `.*` es redundante y no estándar.

### 4.2 Declarados, nunca comprobados (o ignorados)
- Proxy: `ioriproxy.whitelist` (`WHITELIST`), `WHEREIS` (duplica `find`), `ioriproxy.staff.list` (solo en `StaffCommand` no registrado).
- Proxy `EXTRA-PERMISSIONS` **ignorados** porque el código lee otro YAML: `MAINTENANCE-BYPASS` (lee `maintenance.yml`), `QUEUE-BYPASS`, `QUEUE-PRIORITY-*` (lee `queue.yml`), `FRIENDS-LIMIT-*` (lee `settings.yml`). Cambiarlos en `permissions.yml` no hace nada.
- Proxy `STAFF-ADMIN/MOD/HELPER` en `EXTRA` solo se usan si `STAFF.GROUPS` está vacío (`PermissionsConfig.java:41-45`).
- Core: `ioricore.reload` (no hay comando), `ioricore.tags.heart/star/crown` declarados en `plugin.yml` pero comprobados solo vía `data/tags.yml` (no hardcode). Todo `bukkit/.../permissions.yml` es inerte.

### 4.3 Convención de nombres
- `.other` (proxy `send.other`, `ping.other`) vs `.others` (proxy `playtime.others`, Core `*.others`). Key YAML `PLAYTIME-OTHER` → valor `.others`.
- Prefijo `ioriproxy.*` usado en Core (`STAFF-AUTH.STAFF-PERMISSION`); decisión pendiente en `OPEN-QUESTIONS.md:17` (A10).
- Doc obsoleta: `features.md:27` cita `ioriproxy.helpop.reply` (real `ioriproxy.request.reply`) y "Ver `commands.yml`" (real `permissions.yml`). `ARCHITECTURE.md:41` lista `Staff` como comando registrado (no lo está).
- `/ggwave` reutiliza `ioriproxy.donations`; `/unblacklist` reutiliza `ioriproxy.blacklist`; `/request move` acepta `.reply`.

### 4.4 Hardcode vs configurable
| Plataforma | Configurable | Hardcode |
|---|---|---|
| Proxy | Casi todo vía `permissions.yml` (`CommandsConfig.permission`) | `ioriproxy.playtime.others` duplicado (`PlaytimeCommand.java:43,112`), `ioriproxy.staffchat.receive`/`ioriproxy.staff` (`DiscordBotService.java:252`), `ioriproxy.friends.spy` fallback (`FriendService.java:429`), `ioriproxy.maintenance.*` |
| Core | Solo `JOIN.*-PERMISSION`, `STAFF-AUTH.STAFF-PERMISSION`, formatos de chat, tags, kits | **Todos** los permisos de comandos y bypass (`ioricore.admin`, `ioricore.staff`) |

### 4.5 Riesgos funcionales
- `ioricore.admin` no tiene `children` y Bukkit aplica `setPermission` antes de `execute` (`BukkitCommandBridge.java:35-37`) → el bypass `deny(...|| ioricore.admin)` solo funciona para OP; un admin no-OP sin cada nodo recibe el "no permission" de Bukkit.
- Sin defaults en proxy: los jugadores normales necesitan `ioriproxy.hub/friends/ping/playtime/queue/report.send/request.send/seen/glist…` por LuckPerms; además el command-whitelist `DEFAULT` del proxy bloquea todos los comandos Core no listados (ver COMMANDS §5).
- Mensaje de no-permiso Core = el de Bukkit (no i18n) porque no se llama `setPermissionMessage` (bug histórico `ed145421`, `IMAGE-INDEX.md:112`).
