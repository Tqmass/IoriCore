# PROXY-MODULES — inventario funcional IoriVelo / IoriBungee (auditoría 2026-11-03)

Fuente del **estado real** = código. Fuente de la **intención** = `.cursor/docs/features.md`, `phases.md`, `plan/PLAN.md`, `plan/MODULE-INVENTORY.md` (§ Módulos PROXY), rules `20-proxy-compat`, `40-yaml-config`, `50-seguridad-rendimiento`.
Rutas abreviadas: `V/` = `velocity/src/main/java/us/iorilabs/proxy/`, `B/` = `bungee/src/main/java/us/iorilabs/proxy/`, `R/` = `velocity/src/main/resources/`. Salvo que se diga, lo que se cita en `V/` es idéntico en `B/` (ver §3).
Cruces: comandos → [COMMANDS.md](COMMANDS.md) §1 · permisos → [PERMISSIONS.md](PERMISSIONS.md) · config → [CONFIGURATIONS.md](CONFIGURATIONS.md) · Redis/SQL/Discord/webhooks → [INTEGRATIONS.md](INTEGRATIONS.md) · estado global → [FEATURE-STATUS.md](FEATURE-STATUS.md) · lado Core → [CORE-MODULES.md](CORE-MODULES.md).

Estados: `IMPLEMENTED_AND_TESTED` (solo con captura propia en `IMAGE-INDEX.md` § "Salidas proxy propias" — **histórico**, la captura puede ser de una versión anterior del código) · `IMPLEMENTED_UNTESTED` · `PARTIAL` · `STUB` · `NOT_IMPLEMENTED`. No hay tests automatizados (OPEN-QUESTIONS B9).

---

## 0. Arquitectura real del arranque

| Pieza | Realidad | Evidencia |
|---|---|---|
| Entrada Velocity | `@Plugin(id="iorivelo", version="1.0.0-SNAPSHOT")` hardcode, dep. opcional `luckperms` | `V/velocity/IoriProxyVelocity.java:23-30` |
| Entrada Bungee | `extends Plugin`, `plugin.yml` con `${version}` expandido por Gradle, `softDepends: [LuckPerms]` | `B/bungee/IoriProxyBungee.java:15`, `bungee/src/main/resources/plugin.yml`, `bungee/build.gradle.kts` (processResources) |
| Núcleo | `IoriProxy` (678 LOC) **instancia todos los servicios a mano** según `MODULES.*`; es un god-object + singleton `IoriProxy.get()` | `V/IoriProxy.java:149-408` |
| `ModuleManager` | Solo registra `CoreModule` (que solo loguea). **No** es el sistema de módulos real | `V/modules/ModuleManager.java:21-23`, `V/modules/core/CoreModule.java:16-21` |
| Toggles | `SettingsConfig.moduleEnabled(id)` → `MODULES.<ID>` con `_`→`-`, **default true** si falta | `V/config/SettingsConfig.java:57-61` |
| Registro comandos | `IoriProxy.registerCommands()` → `CommandRegistry` (common) → `platform.registerCommands` | `V/IoriProxy.java:576-677` |
| Listeners | Registrados en el bootstrap de plataforma tras `core.enable()` | `V/velocity/IoriProxyVelocity.java:57-65`, `B/bungee/IoriProxyBungee.java:28-38` |
| ServerHealthMonitor | Creado en el bootstrap, no en `IoriProxy`; no se recarga ni se guarda referencia | `V/velocity/IoriProxyVelocity.java:67-87` |
| Proxy id | **Constante `"proxy"`**, no configurable | `V/config/SettingsConfig.java:8,26-28` |

Toggles de `R/settings.yml:126-156` **nunca leídos**: `MULTILOBBY` (se usa `fallback.yml MULTILOBBY.ENABLED`). `CORE` lo lee `ModuleManager`. Flags leídos sin entrada propia: ninguno.

---

## 1. Módulos / features

Formato por módulo: responsabilidad · funcionalidades · clases · eventos · comandos · permisos · config · integraciones · comunicación · **ESTADO** · pendientes.

### 1.1 Infra: Config + i18n + Logger
- **Resp.:** cargar 11 YAML + 8 webhooks + 4 messages; idioma modern/legacy por protocolo.
- **Clases:** `config/ConfigManager` (`FILES` `V/config/ConfigManager.java:13-25`), `config/YamlConfig` (SnakeYAML, claves normalizadas UPPER-kebab `:257-259`), 22 `*Config`, `config/ConfigValidation`, `language/LanguageManager` (bags modern/legacy, placeholders `<k>`/`{k}`/`%k%` `:113-121`, fallback legacy→modern `:67-70`), `logging/ProxyLogger` (ANSI vía `ColorUtil.toAnsi` `:56-58`), `logging/LibraryLogSilence`.
- **Config:** `settings.yml` (SETTINGS, PLAYTIME, COOLDOWNS, NOTIFICATIONS, FRIENDS, COMMAND-WHITELIST, ANNOUNCEMENTS, ANALYTICS, MODULES, STAFF-AUTH, DISCORD), `messages/{en,es}.yml`, `messages/legacy/{en,es}.yml`. Report/HelpOp/Stream/Blacklist config ya no tienen YAML propio (rule 40 "Eliminados") → `ReportConfig`/`HelpOpConfig`/`StreamConfig` leen de `settings` (`ConfigManager.java:123-126`).
- **ESTADO:** `IMPLEMENTED_UNTESTED` (arranque visto en captura `4a319cbb` "arranque H2 OK", histórico).
- **Pendientes:** `messages/legacy/en.yml` está en **español** (p.ej. `:36,49,52,75,211`); `es.yml:364` en inglés ("has been reported for"); banner/prefijo dicen `IoriProxy`, no IoriVelo/RoyalCTW (OPEN-QUESTIONS A15).

### 1.2 Storage (SQL / H2 / Mongo)
- **Resp.:** pool + migraciones del esquema compartido.
- **Clases:** `database/StorageFactory` (H2|MYSQL|MONGODB|NONE, fallback a Noop si falla `:13-38`), `database/mysql/MySqlStorageService` (Hikari, H2 o MySQL, migraciones v1–v6 `:130-483`), `database/mongo/MongoStorageService`, `NoopStorageService`.
- **Tablas:** v1 `ip_players`, `ip_playtime` · v2 `ip_reports`, `ip_helpop` · v3 `ip_analytics` · v4 `ip_friends`, `ip_friend_requests` · v5 `ip_account_ips` · v6 `ic_punishments` · control `ip_schema_version`. Mongo: colección `ip_meta` solo.
- **Config:** `database.yml` (`DATABASE: H2` por defecto).
- **ESTADO:** SQL/H2 `IMPLEMENTED_AND_TESTED` (histórico, `4a319cbb`); **Mongo `STUB`**: conecta pero ningún servicio lo usa — todos piden `storage.mysql()` (`V/database/StorageService.java:13-15`; usos en Alt/Analytics/Friend/HelpOp/PlayerDataSql/Playtime/Discord). Con `MONGODB` el proxy funciona en memoria.
- **Pendientes:** retirada limpia de Mongo (decisión usuario 2026-11-03: sin datos) → borrar `database/mongo/`, `StorageType.MONGODB`, `mongodb-driver-sync` + relocations en ambos `build.gradle.kts`. Ver [INTEGRATIONS.md](INTEGRATIONS.md).

### 1.3 Redis bus
- **Resp.:** pub/sub JSON versionado entre proxies (+ Core publica `PUNISHMENT_INVALIDATE`).
- **Clases:** `redis/JedisRedisService` (pool, TLS, password, reconexión `:252-268`, descarta eventos con `origin == proxyId` `:154`), `RedisEvent` (`schema_version`, `origin_proxy_id`, `event_id`), `ProxyEventType` (23 tipos), `NoopRedisService`.
- **Event types:** `PLAYER_JOIN/QUIT/SWITCH`, `REDIS_TEST`, `MAINTENANCE_UPDATE/WHITELIST`, `BLACKLIST_UPDATE`, `STAFFCHAT_MESSAGE`, `ADMINCHAT_MESSAGE`, `STREAM_BROADCAST`, `ANNOUNCEMENT_RELOAD`, `REPORT_CREATED/RESOLVED`, `HELPOP_CREATED/REPLY`, `DONATION_BROADCAST`, `GGWAVE_START`, `FRIEND_REQUEST/ACCEPT/REMOVE/NOTIFY`, `PUNISHMENT_APPLIED/INVALIDATE` (`V/redis/ProxyEventType.java`).
- **Comandos:** `redistest`. **Config:** `redis.yml`.
- **ESTADO:** `PARTIAL` — **bug multi-proxy**: `proxyId()` devuelve siempre `"proxy"` (`SettingsConfig.java:26-28`) y el listener ignora eventos de su mismo origen (`JedisRedisService.java:154`) ⇒ con ≥2 proxies **ningún evento de otro proxy se procesa**. Con 1 proxy no hay efecto visible. `RedisService.withJedis(Function<Jedis,…>)` filtra Jedis en la interfaz (`V/redis/RedisService.java:19`).
- **Pendientes:** `SETTINGS.PROXY-ID` configurable; quitar `Jedis` de la interfaz.

### 1.4 Presence + Routing (send / goto / servers / find / glist / hub / fallback)
- **Resp.:** presencia global UUID→servidor, envío entre backends, lobby, fallback al kick.
- **Clases:** `server/PresenceService` (Redis hash `<prefix>:presence:<uuid>` + `:presence:name:<name>`, TTL 86400 `:127-143,194-198`), `server/ServerRouter` (`pickLobby`, `pickFallback`, `locate` `:20-95`), `server/ServerModes`, `platform/BackendServer`.
- **Eventos:** Velocity `ServerConnectedEvent`/`DisconnectEvent`/`KickedFromServerEvent` (`V/velocity/impl/listener/VelocityPresenceListener.java:73-229`); Bungee `ServerConnectedEvent`/`PlayerDisconnectEvent`/`ServerKickEvent` (`B/bungee/impl/listener/BungeePresenceListener.java:87-240`).
- **Comandos:** `send`, `goto`, `servers` (alias `server`), `find` (`whereis`), `glist` (`gcount`), `hub` (`lobby`, `vestibulo`) → [COMMANDS.md](COMMANDS.md) §1.2.
- **Config:** `fallback.yml`; `messages LOBBY.*`, `TRANSFER.*`.
- **Redis:** `PLAYER_JOIN/QUIT/SWITCH`.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico: servers/glist `IMAGE-INDEX` "Analytics/servers/queue/…"); hub/fallback `IMPLEMENTED_UNTESTED`.
- **Pendientes / bugs:**
  - Config muerta: `FAVICON` (`MotdConfig.java:22`), `PING-DELAY`, `PING-TIMEOUT`, `ON-SERVER-DOWN`, `JOIN-BALANCING`, `FALLBACK.MODE` (`LobbyConfig.java:14-75`, 0 usos). No hay balanceo en el join inicial (no hay listener `PlayerChooseInitialServerEvent`).
  - `/hub` con un solo lobby online y el jugador ya en él, o con `BALANCING: random`, lo reconecta al mismo servidor → mensaje nativo "already connected" (`V/server/ServerRouter.java:62-81`, `V/commands/HubCommand.java:44-55`). `/goto` no comprueba mismo servidor (`GotoCommand.java:62`).
  - Alias `server` en `/servers` sustituye al `/server <nombre>` nativo por un listado (`R/commands.yml:23-26`, `ServersCommand.java:33-61`).

### 1.5 Maintenance
- **Resp.:** mantenimiento global / por modo / por servidor, countdown, whitelist, kick, MOTD/hover.
- **Clases:** `maintenance/MaintenanceService` (653 LOC), `MaintenanceState`, `common: maintenance/MaintenanceType`, `commands/MaintenanceCommand` + `commands/argument/MaintenanceOps`.
- **Eventos:** Velocity global en `LoginEvent` (`VelocityPresenceListener.java:54-70`); Bungee global en `PostLoginEvent` (`BungeePresenceListener.java:67-85`); por servidor en `ServerConnectedEvent` (tras conectar → mueve a fallback o kick).
- **Comandos:** `maintenance` (`mantenimiento`) on/off/list/status/whitelist/reload.
- **Permisos:** `ioriproxy.maintenance`, `.whitelist`, `.maintenance.bypass`.
- **Config:** `maintenance.yml` (whitelist se guarda en el propio YAML: `saveWhitelist` `MaintenanceService.java:569-570`).
- **Redis:** `MAINTENANCE_UPDATE`, `MAINTENANCE_WHITELIST` (`:58-59,586,598`). **Webhook** `webhooks/maintenance.yml`: se carga pero **nunca se envía** (no hay `webhooks.send("maintenance")`). StatusWebhook/Discord sí leen estado.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `cea4833c`, `1496b1cb`, `2bbf338c`; bug "Whitelist - X" `ee4bc9f6` no reverificado).
- **Pendientes:** estado activo no persistente (memoria + Redis) — NO VERIFICADO si sobrevive reinicio; texto fallback hardcode `"Maintenance"` (`VelocityPresenceListener.java:66`); acoplamiento `IoriProxy.get()` (`MaintenanceService.java:207`); bloqueo por servidor debería ir en pre-connect.

### 1.6 MOTD / server list
- **Clases:** `motd/MotdService` (perfiles MODERN/LEGACY por protocolo, RANDOM, CENTER, override de mantenimiento, hover/sample `:47-116`).
- **Eventos:** `ProxyPingEvent` (ambas plataformas).
- **Config:** `motd.yml`, `maintenance.yml MOTD/PLAYER-LIST`.
- **ESTADO:** `IMPLEMENTED_UNTESTED` (favicon `NOT_IMPLEMENTED`).
- **Nota:** única diferencia de código core Velocity↔Bungee: un comentario en `MotdService.java:72`.

### 1.7 Blacklist
- **Clases:** `blacklist/BlacklistService`, `commands/BlacklistCommand` (x2: blacklist/unblacklist, flag `-s`).
- **Eventos:** `LoginEvent` (ambas).
- **Persistencia:** **solo Redis SET** `<prefix>:blacklist` (`BlacklistService.java:110-133`). Sin Redis ⇒ **se pierde al reiniciar**. No usa SQL (features.md dice SQL/Mongo).
- **Redis:** `BLACKLIST_UPDATE` (lista completa). **Webhook** `blacklist.yml` cargado pero **nunca enviado**.
- **ESTADO:** `PARTIAL`.
- **Pendientes:** persistir en SQL; broadcasts hardcode `"<red>Blacklist:</red>…"`/`"Unblacklist"` (`:68,86`) y fallback `"You are blacklisted…"` (`:100`) violan i18n.

### 1.8 Ban check en join (punishments enforce)
- **Clases:** `punishment/BanCheckService`, `BanCheckCache`; suscripción inline en `IoriProxy.java:294-308`.
- **SQL:** lee `ic_punishments` (escrito por Core y por Discord).
- **Redis:** consume `PUNISHMENT_INVALIDATE` (publicado por Core `bukkit/.../redis/SoftRedisPublisher.java:14,56-75`) y `PUNISHMENT_APPLIED`.
- **Eventos:** `LoginEvent` (ambas).
- **ESTADO:** `IMPLEMENTED_UNTESTED`.
- **Riesgo:** consulta SQL **síncrona** en el `LoginEvent` de Bungee sin `registerIntent` (`BungeePresenceListener.java:56-62` → `BanCheckService.java:31-46,76`) — bloquea hilo de red (rule 50 rendimiento §1). En Velocity el evento ya es async.

### 1.9 Command whitelist / blocker + TabComplete filter
- **Clases:** `commands/CommandBlockerService`, `commands/TabCompleteFilterService`, `config/CommandWhitelistConfig`; `common: CommandBlockerMode`.
- **Eventos:** Velocity `CommandExecuteEvent` + `PlayerAvailableCommandsEvent` + `TabCompleteEvent` legacy; Bungee `ChatEvent` + `TabCompleteEvent`.
- **Config:** `settings.yml COMMAND-WHITELIST` (WHITELIST/BLACKLIST, GROUPS). Permisos bypass/notify.
- **ESTADO:** `IMPLEMENTED_UNTESTED` (bug histórico `/ver` lista plugins `8b48f45b` — ahora `ver` en GLOBAL-BLOCKED `R/settings.yml:69`).
- **Diferencia:** Bungee no puede filtrar el árbol Brigadier 1.13+ (no hay `PlayerAvailableCommandsEvent`).

### 1.10 StaffChat / AdminChat
- **Clases:** `staffchat/StaffChatService`, `adminchat/AdminChatService`, `staff/StaffGroups` (rango vía `LuckPermsHook` reflexivo).
- **Eventos:** chat (Velocity `PlayerChatEvent`, Bungee `ChatEvent`) con toggle; untoggle al salir.
- **Comandos:** `staffchat` (`sc`), `adminchat` (`ac`).
- **Redis:** `STAFFCHAT_MESSAGE`, `ADMINCHAT_MESSAGE`. **Discord:** bridge bidireccional staffchat ↔ canal `STAFF-CHAT` (`IoriProxy.java:324-327`, `DiscordGatewayListener.java:36-37`).
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `11ac8b51`, `beaaaa59`, `eefaf12e`).
- **Riesgo NO VERIFICADO:** denegar `PlayerChatEvent` de clientes 1.19.1+ (chat firmado) en Velocity puede desconectar al jugador (`VelocityStaffChatListener.java:35,43`; igual en `VelocityDonationChatListener.java:27`).

### 1.11 Staff alerts / staff list
- **Clases:** `staff/StaffAlertService` (join/quit/switch/intento en mantenimiento → staff con `ioriproxy.alerts`), `commands/StaffCommand`.
- **ESTADO:** alerts `IMPLEMENTED_UNTESTED`; **`/staff list` `NOT_IMPLEMENTED` en flujo**: `StaffCommand` existe (`V/commands/StaffCommand.java:12-17`) pero **no se registra** en `registerCommands` — `phases.md` Fase 6 lo marca ✅.

### 1.12 Staff auth (gate proxy)
- **Resp.:** si cambia IP/país de un staff respecto a la última sesión aprobada → PENDING + token; aprobación por comando o botón Discord; envía sesión al backend.
- **Clases:** `staffauth/StaffAuthService` (geo HTTP `ip-api.com`, `MASK-IP`, TTL token), `commands/StaffAuthCommand` + `argument/StaffAuthApproveArgument`.
- **Comandos:** `staffauth` (`staff-auth`, `sauth`). **Permiso:** `ioriproxy.staffauth.approve`.
- **Config:** `settings.yml STAFF-AUTH`. **Webhook** `staff-auth.yml` (`StaffAuthService.java:263`). **Discord:** botón Approve (`DiscordGatewayListener.java:129-155`).
- **Plugin msg:** `STAFF_AUTH_SESSION` `[type, ok]` (`StaffAuthService.java:156-159`).
- **ESTADO:** `PARTIAL`.
- **Pendientes / riesgos:** `lastSeen`/`sessions` **solo en memoria** (`:36-38`): tras reiniciar el proxy la primera IP se aprueba sola (`:80-86`, TOFU). Payload de sesión enviado al servidor **anterior** (ver §1.21). Sin hash/password en proxy (Core enforce, ver [STAFFMODE.md](STAFFMODE.md)).

### 1.13 Report
- **Clases:** `report/ReportService`, `Report`, `ReportStatus`; `commands/ReportCommand`, `ReportsCommand` (resolve).
- **SQL:** `ip_reports`. **Redis:** `REPORT_CREATED/RESOLVED`. **Webhook:** `reports.yml`. Cooldown `COOLDOWNS.REPORT`.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `c17b549d`, `d04d2a27`, `2c6fa489`).
- **Ownership:** MODULE-INVENTORY dice "Report/HelpOp por modalidad, no duplicar en proxy si el modo los posee" — el código los tiene siempre en proxy (toggle global).

### 1.14 HelpOp / Request
- **Clases:** `helpop/HelpOpService`, `HelpOpTicket`; `commands/RequestCommand` (help/move/… ; alias `ayuda`).
- **SQL:** `ip_helpop`. **Redis:** `HELPOP_CREATED/REPLY`. **Webhook:** `request.yml`. Cooldown `COOLDOWNS.HELPOP`.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `40a9b623`, `914c1f4e`).
- **Pendientes:** hovers hardcode `"Responder"`, `"Ir al jugador"` (`HelpOpService.java:180,183`); `/helpop` no existe como label (phases.md Fase 7 dice "alias /request").

### 1.15 Stream / YouTube
- **Clases:** `stream/StreamService` (hosts permitidos YouTube/Twitch/Kick `:30-32`), `commands/StreamCommand` (`stream`→`directo`, `youtube`→`video`).
- **Redis:** `STREAM_BROADCAST`. **Webhook:** `stream.yml` (formatos STREAM y VIDEO `WebhookRegistry.java:57-60`).
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `06b89acd`, `43de7153`).
- **Pendientes:** hover hardcode `"Clic para abrir"` (`StreamService.java:169`); patrón `</hover>` roto en `messages/legacy/{en,es}.yml:211` (ver §5).

### 1.16 Announcements / Alert / Announce (title)
- **Clases:** `announcements/AnnouncementService` (timer, RANDOM, por modo, sonido modern/legacy, placeholders), `commands/AnnounceCommand` (title a all/servidor), `AlertCommand` (`broadcast`, `bc`).
- **Redis:** `ANNOUNCEMENT_RELOAD`. **Config:** `settings.yml ANNOUNCEMENTS`, textos `messages ANNOUNCEMENTS.<MODO>`.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `8e4919ec`, `59fe6026`). `announce`/`alert`/`glist`/`seen` se registran **sin flag** (`IoriProxy.java:647-650`).

### 1.17 Donations + GG Wave
- **Clases:** `donations/DonationService`, `commands/DonationCommand` (`donacion`; solo subcomando `test`), `GgWaveCommand`; listener chat GG.
- **Redis:** `DONATION_BROADCAST`, `GGWAVE_START`. **Webhook:** `donations.yml`. **Discord:** canal STORE.
- **Config:** `donations.yml` (TEBEX/BuiltByBit/meta).
- **ESTADO:** `PARTIAL` — la única entrada de donaciones es `/donation test` (`DonationCommand.java:49`); `applyStoreGoal` sin llamadas; Tebex/BuiltByBit solo loguea "wire store webhook" (`DonationService.java:83-90`). Captura propia `69b624e6` (histórico, EN) + bug `</hover>` `3ddad046`/`a12f51e3`.
- **Pendientes:** receptor de webhook de tienda; para jugadores legacy usa `DONATIONS.BROADCAST` del bag **modern** (`rawList` sin protocolo, `DonationService.java:236-237`).

### 1.18 Friends
- **Clases:** `player/friends/FriendService` (1019 LOC), `FriendPair`, `FriendNotify`, `commands/FriendsCommand` (add/accept/deny/remove/list/msg/broadcast/request/requests/help).
- **SQL:** `ip_friends`, `ip_friend_requests`. **Redis:** `FRIEND_REQUEST/ACCEPT/REMOVE/NOTIFY`. Presencia para estado online.
- **Config:** `settings.yml FRIENDS`, `NOTIFICATIONS.FRIENDS`, `COOLDOWNS.FRIENDS-REQUEST`; aliases `friends, amigos, a, f`.
- **ESTADO:** `IMPLEMENTED_AND_TESTED` (histórico `77b10549`, `56d71ce0`, `579e86aa`…).

### 1.19 Player data / Seen / Playtime / Analytics / Alts / Transfer
| Feature | Clases | SQL | Comando | ESTADO |
|---|---|---|---|---|
| PlayerData | `player/PlayerDataService`, `PlayerDataSql` (upsert en join) | `ip_players` | — | `IMPLEMENTED_UNTESTED` (siempre activo, sin flag `IoriProxy.java:334`) |
| Seen | `commands/SeenCommand` (IP/país con permisos, estado blacklist) | `ip_players` | `seen` (`inspect`, `info`) | `IMPLEMENTED_AND_TESTED` (histórico, grupo "…seen/playtime") |
| Playtime | `player/playtime/PlaytimeService` (total/servidor/modo, flush, memoria si cae DB) | `ip_playtime` | `playtime` | `IMPLEMENTED_AND_TESTED` (histórico) |
| Analytics | `analytics/AnalyticsService` (joins, pico, persistencia diaria `:60-89`) | `ip_analytics` | `analytics` | `IMPLEMENTED_AND_TESTED` (histórico `7d32b6f3`…) |
| Alts | `alts/AltService` (IPs por cuenta, alerta staff, estado BANNED si blacklist) | `ip_account_ips` | `alts` (`alt`, `multicuenta`, `accounts`) | `IMPLEMENTED_UNTESTED` |
| Transfer | `player/transfer/AccountTransferService` (friends, playtime, playerData, alts, whitelist mantenimiento) | varias | `transfer` (`migrate`, `accounttransfer`) | `IMPLEMENTED_UNTESTED` (no transfiere reports/helpop/punishments) |

### 1.20 Queue / Ping / Placeholders / Server health
| Feature | Clases | Notas | ESTADO |
|---|---|---|---|
| Queue | `server/QueueService` (prioridad por permiso, bypass, actionbar 1s), `commands/QueueCommand` | memoria local, **no Redis** (features.md dice Redis); `reload` recrea servicio y **vacía colas** (`IoriProxy.java:524-526`) | `IMPLEMENTED_AND_TESTED` (histórico) |
| Ping | `commands/PingCommand` (`latency`, `ms`) | local + presencia | `IMPLEMENTED_AND_TESTED` (histórico) |
| Placeholders | `placeholders/PlaceholderService`, `common: PlaceholderTokens` | solo consumido por Announcements (`AnnouncementService.java:63,182`) | `PARTIAL` |
| Server health | `server/ServerHealthMonitor` (ping 15 s, aviso a staff `server.health`) | instanciado en bootstrap, sin reload | `IMPLEMENTED_UNTESTED` |

### 1.21 Plugin messaging proxy↔backend
- **Canal:** `ioricore:main` (`api/.../messaging/PluginChannels.java`). Tipos: `HANDSHAKE_PING`, `HANDSHAKE_PONG`, `STAFF_AUTH_SESSION` (`api/.../PluginMessageType.java`).
- **Clases:** `V/velocity/impl/listener/VelocityPluginMessageListener`, `B/bungee/impl/listener/BungeePluginMessageListener`. Backend: `bukkit/.../messaging/PluginMessagingBridge.java:49-54`.
- **Seguridad:** marca handled/cancelled antes de mirar el source y exige source backend (`Velocity…:44-48`, `Bungee…:44-49`) ✔ rule 50 §3. Sin firma (OPEN-QUESTIONS B8).
- **ESTADO:** `PARTIAL` — **bug**: en `ServerConnectedEvent` se usa el servidor actual del jugador, que todavía es el **anterior** (Velocity `player.getCurrentServer()` `:68-71`; Bungee `player.getServer()` `:72-77`; Bungee sí usa `event.getServer()` para el PING `:67`). En el primer join no hay servidor ⇒ no se envía sesión staff-auth. NO VERIFICADO en runtime; usar `ServerPostConnectEvent` / `event.getServer()`.

### 1.22 Discord (webhooks + bot JDA)
- **Webhooks:** `webhooks/WebhookRegistry` (8 ficheros `:17-26`), `WebhookClient` (HttpClient async), `StatusWebhookService`. Enviados de verdad: `reports`, `request`, `stream`/`video`, `donations`, `status`, `staff-auth`. Cargados y **nunca enviados**: `maintenance`, `blacklist`.
- **Bot:** `discord/DiscordBotService` (JDA, intents GUILD_MESSAGES/MESSAGE_CONTENT/GUILD_MEMBERS/GUILD_MODERATION `:109-120`), `DiscordGatewayListener` (prefijo, slash, botón staff-auth, boost), `DiscordPunishmentHandler` (escribe `ic_punishments`, publica `PUNISHMENT_APPLIED` `:127`), `DiscordEmbeds`, enums `DiscordChannelKey` (staff-chat, punishments, welcome, goodbye, boost, store, status, staff-auth), `DiscordPunishmentType` (BAN, TEMPBAN, MUTE, TEMPMUTE, WARN, KICK).
- **Funciones:** mirror de webhooks al bot; welcome/goodbye en join/quit MC; staffchat bridge; slash/prefix punish con rol o permiso Discord (`:164-182`); approve staff-auth; boost embed; status timer.
- **Config:** `settings.yml DISCORD.BOT` (ENABLED false por defecto), `discord.yml`, `webhooks/*.yml`.
- **ESTADO:** webhooks `IMPLEMENTED_UNTESTED`; bot `IMPLEMENTED_UNTESTED` (PLAN lo marca "FINAL"; sin captura propia).
- **Pendientes:** respuestas Discord hardcode EN (`DiscordGatewayListener.java:64,90,115,135,143,150,153`); kick hardcode `"&cRoyalCTW &8» &fYou were …ed"` (`DiscordPunishmentHandler.java:90`); welcome/goodbye en cada join (spam). Ver [INTEGRATIONS.md](INTEGRATIONS.md).

---

## 2. Comunicación con backend (Core)

| Dirección | Medio | Contenido | Evidencia |
|---|---|---|---|
| Core → Proxy | Redis | `PUNISHMENT_INVALIDATE {uuid}` (Jedis por reflexión, soft) | `bukkit/.../redis/SoftRedisPublisher.java:56-75` |
| Core → Proxy | SQL compartida | `ic_punishments` (BanCheck) | `V/punishment/BanCheckService.java:76` |
| Proxy → Core | Plugin msg | `HANDSHAKE_PING`, `STAFF_AUTH_SESSION` | §1.21 |
| Core → Proxy | Plugin msg | `HANDSHAKE_PONG` (solo log en debug) | `VelocityPluginMessageListener.java:56-58` |
| Proxy ↔ Proxy | Redis | resto de event types | §1.3 |

Core usa de `common/`: `CommandSource` (87 imports), `CommandParent` (15), `CommandArgument` (13), `ProtocolVersions`, `ColorUtil`, `ProxyCommand`.

---

## 3. Velocity vs Bungee

### 3.1 Código
- `us.iorilabs.proxy.**` fuera de `velocity/`/`bungee/`: **127 ficheros idénticos byte a byte**; difieren 2 (`cmp`):
  - `motd/MotdService.java` — solo un comentario (`V:72`).
  - `player/transfer/AccountTransferService.java` — mismo comportamiento, formato distinto (`V:33-51` vs `B:33-37`).
- Contratos `ProxyPlatform`, `ProxyPlayer`, `BackendServer` viven **duplicados** en `velocity/` y `bungee/`, no en `common/` (contradice rule 20 §2).

| Aspecto | Velocity (IoriVelo) | Bungee (IoriBungee) |
|---|---|---|
| Descriptor | `@Plugin` anotación → `velocity-plugin.json` generado; versión hardcode `1.0.0-SNAPSHOT` | `plugin.yml` con `${version}` (processResources) |
| Ciclo de vida | `ProxyInitializeEvent` / `ProxyShutdownEvent` | `onEnable` / `onDisable` + `platform.close()` (cierra `BungeeAudiences`) |
| Adventure | nativo (`player.sendMessage(Component)`) | `adventure-platform-bungeecord:4.3.4` (`BungeeAudiences`, relocado); kicks vía `TextComponent.fromLegacyText(toLegacySection)` (`BungeePlatform.java:220-225`) |
| Comandos | `VelocityBrigadierCommand` + `VelocityCommandSource` | `BungeeProxyCommand` + `BungeeCommandSource` |
| Login | `LoginEvent` (blacklist, ban, **mantenimiento global**) | `LoginEvent` (blacklist, ban) + `PostLoginEvent` (mantenimiento global) |
| Kick fallback | `KickedFromServerEvent` | `ServerKickEvent` |
| Command blocker | `CommandExecuteEvent` | `ChatEvent` (comandos) |
| Tab filter | `PlayerAvailableCommandsEvent` (árbol 1.13+) + `TabCompleteEvent` legacy | solo `TabCompleteEvent` — **sin filtro de árbol 1.13+** |
| Chat (staff/GG) | `PlayerChatEvent` | `ChatEvent` |
| Plugin msg | `ChannelRegistrar` + `ServerConnection.sendPluginMessage` | `registerChannel` + `Server.sendData` |
| Health ping | `RegisteredServer.ping()` | `ServerInfo.ping(callback)` |
| Sonido | `player.playSound` | `audiences.player(p).playSound` |
| Shade | no relocaliza `net.kyori` y **shadea adventure-api** aunque Velocity lo trae (riesgo de clases duplicadas, NO VERIFICADO) | relocaliza solo `net.kyori.adventure.platform` |

### 3.2 Resources (`cmp`/`diff`)
- 23 ficheros comunes; Bungee añade `plugin.yml`.
- Idénticos: 19 (commands, database, discord, donations, maintenance, motd, permissions, queue, redis, settings, `messages/legacy/*`, `webhooks/*` x8).
- `fallback.yml`: difiere solo en idioma de 2 comentarios (`:4`, `:53`: EN en Velocity, ES en Bungee).
- `messages/en.yml`, `messages/es.yml`: contenido idéntico, solo **CRLF (Velocity) vs LF (Bungee)** (`diff --strip-trailing-cr` vacío).

---

## 4. Qué es compartible en `common/`

### 4.1 Estado actual de `common/`
18 ficheros (`us.iorilabs.proxy.{commands,maintenance,placeholders,utilities}`), dependencia `api(adventure-api/minimessage/legacy/ansi)` (`common/build.gradle.kts`). Lo consumen velocity, bungee **y bukkit**.

### 4.2 Análisis de los 129 ficheros core del proxy (script de cierre transitivo, imports + referencias)
- **0** ficheros importan `com.velocitypowered` / `net.md_5` fuera de `velocity/`·`bungee/` → todo el core es independiente de plataforma.
- Imports directos de librerías: Gson 18 · `java.sql` 11 · Jedis 4 · JDA 3 · Hikari 2 · HttpClient 2 · SnakeYAML 1 · Mongo 1.
- **Cierre transitivo** (lo que arrastras si mueves el fichero): YAML 98 · Gson 65 · Jedis 60 · Hikari 60 · SQL 59 · HTTP 53 · Mongo 47 · JDA 46. Causa: `IoriProxy` god-object (lo referencian todos los comandos y `MaintenanceService.java:207`) y `StorageService.mysql()` → `MySqlStorageService` (Hikari).
- **Limpios (24, sin libs):** `platform/ProxyPlatform`, `platform/BackendServer`, `player/ProxyPlayer`, `player/PlayerData`, `player/PlayerPresence`, `player/friends/FriendPair`, `…/FriendNotify`, `logging/ProxyLogger`, `config/BlacklistConfig`, `config/ConfigValidation`, `database/StorageType`, `discord/enums/*` (2), `helpop/HelpOpTicket`, `hook/luckperms/LuckPermsHook` (reflexión), `maintenance/MaintenanceState`, `punishment/BanCheckCache`, `redis/ProxyEventType`, `report/Report`, `report/ReportStatus`, `server/ServerModes`, `staffauth/enums/StaffAuthState`, `webhooks/WebhookEndpoint`, `webhooks/WebhookFormat`.
- **Solo SnakeYAML/Gson (31):** `config/*` (22 + `YamlConfig`, `ConfigManager`), `language/LanguageManager`, `redis/RedisEvent`, `RedisEventListener`, `commands/CommandBlockerService`, `TabCompleteFilterService`, `server/QueueService`, `ServerHealthMonitor`, `staff/StaffAlertService`, `StaffGroups`.

### 4.3 Qué implica mover a `common/`
| Mover | Arrastra a bukkit | Veredicto |
|---|---|---|
| Los 24 limpios | nada | **OK**; elimina duplicación de contratos (`ProxyPlatform`/`ProxyPlayer`) y cumple rule 20 §2 |
| Config/i18n (31) | SnakeYAML 2.3 (Spigot 1.8.8 trae 1.x; bukkit ya relocaliza `org.yaml.snakeyaml` pero no lo declara) y Gson (Spigot 1.8.8 trae Gson 2.2.4) | Posible con `compileOnly` en common + `implementation`+relocate en velocity/bungee; requiere probar choques en 1.8.8. Aporta poco a bukkit (tiene su propio YAML/i18n) |
| Servicios con Redis/SQL/JDA/Mongo/HTTP (~74) + `IoriProxy` + comandos | **Jedis, Hikari 5.1 (bukkit usa 4.0.3), mysql-j 9.1 (bukkit 8.3.0), H2 2.3 (bukkit 2.1), JDA, Mongo** | **NO**: introduce dependencias incorrectas en Core (choque de versiones, JDA en cada backend = riesgo PLAN §Riesgos) |

### 4.4 Propuesta (más sencilla)
1. **Fase A (sin módulos nuevos):** mover a `common/` los 24 limpios (mismo paquete `us.iorilabs.proxy.*`, cero cambios de import). Borrar las copias de `velocity/` y `bungee/`.
2. **Fase B (sin módulos nuevos, elimina el resto de la duplicación):** dejar el core proxy en **un único árbol** y que `bungee` lo compile como fuente compartida: en `bungee/build.gradle.kts` `sourceSets.main { java.srcDir("../velocity/src/main/java"); java.exclude("us/iorilabs/proxy/velocity/**") }` y `resources.srcDir("../velocity/src/main/resources")` (o mover el árbol a una carpeta neutra fuera de `velocity/` referenciada por ambos). Ventaja: Jedis/JDA/Hikari nunca tocan `common`/bukkit. Inconveniente: truco de Gradle, peor soporte del IDE.
3. **Alternativa con justificación técnica:** un módulo Gradle `proxy-common` (solo consumido por velocity/bungee). Justificación: en Gradle la **unidad de aislamiento de dependencias es el módulo**; `common` lo consume bukkit y no puede llevar Jedis/Hikari 5/JDA/Mongo. Es lo que planteaba `PLAN.md` §Monorepo y el patrón LuckPerms (rule 20). Solo si el usuario acepta; si no, la opción 2 cubre el mismo objetivo.
- Antes de cualquier mover: romper `IoriProxy.get()` en `MaintenanceService.java:207` y sacar `Jedis` de `RedisService` (§1.3) y `MySqlStorageService` de `StorageService` (§1.2).
- Duplicados conceptuales con `api/`: `api/hook/LuckPermsHook` vs `V/hook/luckperms/LuckPermsHook`; `api/punishment/PunishmentType` vs `discord/enums/DiscordPunishmentType`.

---

## 5. Bugs históricos (OPEN-QUESTIONS §B.13)

| Bug (captura) | ¿Sigue? | Evidencia |
|---|---|---|
| `</hover>` crudo en donación (`3ddad046`, `a12f51e3`) | **Donación: no se reproduce en el YAML actual** (línea modern solo con tags MiniMessage, `R/messages/es.yml:529`). **Mismo patrón vivo en Stream legacy**: `&a` dentro de `<hover>` → `ColorUtil` lo convierte a `<reset><green>` (`common/.../ColorUtil.java:96`), el `<reset>` cierra click/hover y `</hover></click>` queda huérfano | `R/messages/legacy/es.yml:211`, `R/messages/legacy/en.yml:211`. Render exacto NO VERIFICADO en runtime |
| `{MOTIVO=…}` sin resolver en hover de maintenance | **No presente**: el hover no usa motivo y los placeholders `<k>`/`{k}`/`%k%` se resuelven | `R/maintenance.yml:52-58`, `V/motd/MotdService.java:94-116`, `V/language/LanguageManager.java:113-121`. NO VERIFICADO en runtime |
| `§` crudo en banner consola Velocity (`0facfc43`) | **Corregido**: logger convierte a ANSI | `V/logging/ProxyLogger.java:16-30,56-58` |
| "already connected" en inglés (`5f2331b8`, `f0b0ed8e`) | **Sigue posible** en `/hub` (un lobby / `random`) y `/goto`; `/send` sí lo controla con `TRANSFER.ALREADY-CONNECTED` | `V/server/ServerRouter.java:62-81`, `V/commands/HubCommand.java:44-55`, `V/commands/GotoCommand.java:62`; correcto en `SendCommand.java:59-60,100-101` |
| "tú enviado a lobby por" (`9e2a3e6a`) | **No presente**: textos actuales "Has sido enviado a … por" / "Te movimos a" | `R/messages/es.yml:102,126` |

Otros puntos de §B con código: B6 versión hardcode → sigue (`IoriProxyVelocity.java:26`); B7 strings hardcode → siguen (`HelpOpService.java:180,183`, `StreamService.java:169`) + nuevos (`BlacklistService.java:68,86,100`, `DiscordPunishmentHandler.java:90`, `DiscordGatewayListener.java`); B10 tamaños confirmados (Friend 1019, IoriProxy 678, Maintenance 653).

---

## 6. Contradicciones doc ↔ código (resumen)

| Doc dice | Código | Ref |
|---|---|---|
| `/staff list` ✅ (phases F6) | clase sin registrar | §1.11 |
| `/helpop` alias `/request` (phases F7) | solo `request` + `ayuda` | §1.14 |
| Blacklist SQL/Mongo, Queue Redis (features.md) | Blacklist solo Redis; Queue solo memoria | §1.7, §1.20 |
| MOTD favicon (features.md) | config sin uso | §1.6 |
| Mongo storage opcional | STUB, ningún servicio lo usa | §1.2 |
| Contratos en `common` (rule 20) | `ProxyPlatform`/`ProxyPlayer` duplicados por plataforma | §3.1 |
| Paquete `me.tqmass.dev.proxy` (phases.md) / `us.iorilabs.common` (PLAN) | `us.iorilabs.proxy` (también en common) | OPEN-QUESTIONS A14 |
| Webhooks maintenance/blacklist (features.md) | cargados, no enviados | §1.22 |
| Multi-proxy por Redis | roto por proxyId constante | §1.3 |
