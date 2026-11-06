# INTEGRATIONS — storage, bus, Discord, staff auth, hooks

Auditoría 2026-11-03 (solo lectura). Fuente de estado real = código. Intención = `.cursor/docs/plan/PLAN.md`, `memory/DECISIONS.md`, `memory/EXTRAS_BACKLOG.md`.
Abreviaturas: **P:** = `velocity/src/main/java/us/iorilabs/proxy/` (en `bungee/…/proxy/` el fichero es **byte-idéntico**, verificado con `cmp` para MySqlStorageService, MongoStorageService, JedisRedisService, StaffAuthService, DiscordBotService, DiscordGatewayListener, WebhookClient, BanCheckService, DatabaseConfig) · **B:** = `bukkit/src/main/java/us/iorilabs/bukkit/` · **A:** = `api/src/main/java/us/iorilabs/api/`.
Ver también: `PROXY-MODULES.md`, `CORE-MODULES.md`, `CONFIGURATIONS.md`, `PERMISSIONS.md`, `STAFFMODE.md`, `FEATURE-STATUS.md`.

---

## 1. SQL — MariaDB/MySQL + H2

### 1.1 Clases, config, pools, drivers

| | Proxy (IoriVelo / IoriBungee) | Core (IoriCore-Bukkit) |
|---|---|---|
| Entrada | `P:database/StorageFactory.java:13` → `StorageType.from()` (`P:database/StorageType.java:11`, default **H2**, desconocido→H2) | `B:storage/CoreSqlStorage.java:35` `open()` (`IoriCorePlugin.java:116`) |
| Servicio | `P:database/mysql/MySqlStorageService.java` (MySQL **y** H2), `NoopStorageService` si falla/NONE (`StorageFactory.java:37-40`) | `CoreSqlStorage` implementa `A:storage/SqlStorage` |
| Config | `database.yml` → `DATABASE: H2\|MYSQL\|MONGODB\|NONE`, `MYSQL.{HOST,PORT,DATABASE=ioriproxy,USERNAME,PASSWORD,SSL}` (`P:config/DatabaseConfig.java`) | `settings.yml` → `STORAGE.BACKEND: h2\|mysql\|mongo`, `STORAGE.MYSQL.{HOST,PORT,DATABASE=ioricore,USER,PASSWORD,MAX-POOL-SIZE}`, `STORAGE.H2.FILE` (`bukkit/src/main/resources/settings.yml:15-29`) |
| H2 URL | `jdbc:h2:file:<data>/storage/ioriproxy;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE` (`MySqlStorageService.java:59-62`) | `jdbc:h2:file:<data>/storage/ioricore;MODE=MySQL;DATABASE_TO_LOWER=TRUE` (`CoreSqlStorage.java:85-87`) |
| Pool H2 | `IoriProxy-H2` max 8 / minIdle 1 (`:64-73`) | `IoriCore-H2` max 5 / minIdle 1 (`:82-93`) |
| Pool MySQL | `IoriProxy-MySQL` max **10 hardcode**, minIdle 2, timeout 15 s, lifetime 30 min, leakDetection 60 s, prepStmt cache, `useSSL` según `MYSQL.SSL` con `verifyServerCertificate=false` (`:86-117`, `DatabaseConfig.java:7-10`) | `IoriCore-MySQL` max `MAX-POOL-SIZE` (5), minIdle 1, `useSSL=false` **hardcode**, sin `characterEncoding`/`serverTimezone` (`:96-118`) |
| Fallback | Error → `NoopStorageService` (sin persistencia) | MySQL falla → H2 (`:52-58`); H2 falla → `IllegalStateException` |
| Drivers | `HikariCP 5.1.0`, `mysql-connector-j 9.1.0`, `h2 2.3.232` (`velocity/build.gradle.kts`, `bungee/build.gradle.kts`) | `HikariCP 4.0.3`, `mysql-connector-j 8.3.0`, `h2 2.1.214` (`bukkit/build.gradle.kts`) |
| Relocation | `us.iorilabs.libs.{hikari,mysql,h2}` | igual |

Dialecto: SQL de proxy usa `INSERT … ON DUPLICATE KEY UPDATE … VALUES(x)` en H2 y MySQL (AltService:160, AnalyticsService:101, HelpOpService:280, FriendService:707/912/972, PlayerDataSql:102/135, PlaytimeService:231, AccountTransferService:100, ReportService:375). Ejecución en H2 MODE=MySQL **NO VERIFICADO** en runtime (salvo arranque, ver estado). Core economía intenta `MERGE INTO … KEY` (H2) y cae a `ON DUPLICATE KEY` (MySQL) (`B:modules/economy/EconomySql.java:63-75`).

### 1.2 Tablas

| Tabla | Columnas clave | Crea (ruta:línea) | Lee / escribe | Dueño migración | Versión esquema |
|---|---|---|---|---|---|
| `ip_schema_version` | `version PK, applied_at` | `MySqlStorageService.java:134` | migrador proxy | proxy | — (es el registro) |
| `ip_players` | `uuid PK, name(idx), first_join, last_join` | `:158/180` (v1) | `P:player/PlayerDataSql.java:82-158`, `P:player/friends/FriendService.java:706,737`, `P:discord/DiscordPunishmentHandler.java:187` | proxy | v1 |
| `ip_playtime` | `(uuid, scope) PK, millis` | `:169/191` (v1) | `P:player/playtime/PlaytimeService.java:147,230,253`, `P:player/transfer/AccountTransferService.java:90-115` | proxy | v1 |
| `ip_reports` | `id PK, reporter_*, target_name, reason, detail, server, created_at, status(idx), resolver_name` | `:219/251` (v2) | `P:report/ReportService.java:347,372` | proxy | v2 |
| `ip_helpop` | `id PK, player_*, message, server, created_at, open_flag(idx)` | `:236/268` (v2) | `P:helpop/HelpOpService.java:278` | proxy | v2 |
| `ip_analytics` | `(stat_day, proxy_id) PK, joins, peak_online` | `:300` (v3) | `P:analytics/AnalyticsService.java:99` | proxy | v3 |
| `ip_friends` | `(uuid_a, uuid_b) PK, since` | `:328/354` (v4) | `FriendService.java:850,970,1002` | proxy | v4 |
| `ip_friend_requests` | `(from_uuid, to_uuid) PK, created_at` | `:340/366` (v4) | `FriendService.java:862,910,941` | proxy | v4 |
| `ip_account_ips` | `(uuid, ip) PK, last_seen, idx ip` | `:395/407` (v5) | `P:alts/AltService.java:159,190,217` | proxy | v5 |
| `ic_punishments` | `id AI PK, type, status, target_uuid, target_name, actor_*, reason, created_at, expires_at, silent; idx(target_uuid,status,type)` | **proxy** `:435/456` (v6) **y core** `B:punishment/PunishmentSql.java:18-34` (`ensureSchema` :44, llamado `PunishmentService.java:41`) | R: `P:punishment/BanCheckService.java:79` · W: `P:discord/DiscordPunishmentHandler.java:145`, `B:punishment/PunishmentSql.java:125,144` | **duplicado** | proxy v6 / core sin versión |
| `ic_ranks` | `id PK, name, display_name, prefix, suffix, priority, type, color_key, italic, bold, staff, media, admin, operator, visible, default_rank, scope, server_key, parents; idx(name,scope,server_key)` | `B:ranks/RankSql.java:21-52` (`ensureSchema` :68, `RankService.java:103`) + ALTER ad-hoc `:54-58` | `RankSql.java:96,131,153` | core | sin versión (ALTER con excepción ignorada) |
| `ic_rank_permissions` | `(rank_id, permission) PK` | `RankSql.java:44` | `RankSql.java:153` | core | sin versión |
| `ic_balances` | `player_uuid PK, balance DOUBLE` | `B:modules/economy/EconomySql.java:14` (solo si `persist && useSql`, `EconomyService.java:36-37`) | `EconomySql.java:45,63,73` | core | sin versión |

Hallazgos SQL:
- **DDL duplicada `ic_punishments`** (A8 de OPEN-QUESTIONS sigue abierta). Las dos definiciones coinciden hoy columna a columna; cualquier cambio futuro divergirá.
- **`CREATE INDEX IF NOT EXISTS` en MySQL**: Core lo ejecuta también contra MySQL (`PunishmentSql.java:33`, `RankSql.java:51`) con la excepción tragada. MySQL 8 no soporta esa sintaxis (MariaDB ≥10.1.4 sí) → en MySQL puro, si Core crea la tabla primero, queda **sin índice**. El proxy lo hace bien (INDEX inline en la rama MySQL).
- **BD distintas por defecto**: proxy `ioriproxy` vs Core `ioricore`; en H2 son ficheros separados por proceso. Mientras no apunten a la **misma** base MySQL, `BanCheckService` (proxy) **no ve** los bans escritos por Core y Core no ve los de Discord.
- Migrador proxy sin transacción por paso; v5 no actualiza `current` (`:427`, inocuo hoy). Si un paso falla a medias, el arranque cae a Noop (`StorageFactory.java:37`).
- `BanCheckCache` sin TTL y cachea negativos (`P:punishment/BanCheckCache.java`, `BanCheckService.java:34-40`): un "no baneado" queda cacheado hasta invalidación Redis o reinicio (ver §3).
- H2 2.1.214 (Core) vs 2.3.232 (proxy): ficheros no intercambiables entre lados (compatibilidad de formato **NO VERIFICADO**); unificar versiones.

**ESTADO**: proxy SQL `IMPLEMENTED_UNTESTED` (arranque H2 visto en consola, img `4a319cbb` "arranque H2 OK" — histórico, lado/flujo de queries NO VERIFICADO) · Core SQL `IMPLEMENTED_UNTESTED` · esquema compartido `PARTIAL`.

---

## 2. MongoDB

| Dónde | Qué hace | Evidencia |
|---|---|---|
| Proxy `P:database/mongo/MongoStorageService.java` | Conecta (URI o host+auth), `ping`, upsert `ip_meta {_id:schema, version:1}` (`:69-79`). **Ningún servicio lo usa**: todos piden `storage.mysql()` (`StorageService.java:13-15` devuelve `Optional.empty()`) → con `DATABASE: MONGODB` friends/playtime/reports/helpop/alts/analytics/bans/Discord-SQL quedan **desactivados en silencio** | grep `storage.mysql()` en BanCheckService:72, DiscordPunishmentHandler:138/184, etc. |
| Proxy config | `database.yml:22-35` (`MONGODB.*`), `DatabaseConfig.java:62-92`, `StorageType.MONGODB` (`StorageType.java:9,17`), `StorageFactory.java:28-32` | |
| Proxy Gradle | `implementation("org.mongodb:mongodb-driver-sync:5.2.1")` + `relocate com.mongodb`, `org.bson` (velocity y bungee `build.gradle.kts`) | |
| Core `B:storage/SoftMongoStorage.java` | Reflexión: `MongoClients.create(uri)` + `ping`; sin uso posterior. Con `BACKEND: mongo` el SQL **sigue en H2** y la etiqueta de backend es `MONGODB` (`CoreSqlStorage.java:38-45`) | `compileOnly` driver (no shaded) → noop salvo jar externo |
| API | `A:storage/StorageBackend.java` `MONGODB` | |
| Kits / otros | Sin uso (grep `mongo` en bukkit solo storage) | |

Tamaño medido (jars actuales `build/libs`): `us/iorilabs/libs/{mongodb,bson}` = **1 715 entradas, 6,16 MB sin comprimir / ≈2,12 MB comprimidos** en IoriVelo (26,7 MB) e IoriBungee (26,9 MB) → **≈8 % de cada jar proxy**. Core: 0 (compileOnly).

Razón técnica para mantenerlo: **ninguna** encontrada (no hay datos, ningún servicio lo usa, el usuario confirma que no hay datos que preservar).

### 2.1 Plan de retirada limpia — PROPUESTA (no ejecutado)
1. Borrar `P:database/mongo/MongoStorageService.java` (velocity + bungee).
2. `StorageType`: quitar `MONGODB` y el case `"mongo","mongodb"`; decidir si un valor `mongo` en YAML → warning + H2 (recomendado) en vez de fallo.
3. `StorageFactory`: quitar import y case `MONGODB` (`:5,28-32`).
4. `DatabaseConfig`: quitar `mongo*()` (`:62-92`).
5. `database.yml` (velocity + bungee): quitar bloque `MONGODB` y `MONGODB` del comentario `DATABASE:`.
6. Gradle velocity/bungee: quitar `mongodb-driver-sync` y `relocate("com.mongodb")`, `relocate("org.bson")`.
7. Core: borrar `B:storage/SoftMongoStorage.java`; `CoreSqlStorage`: quitar rama MONGODB (`:38-45`), campo/getter `softMongo`, `close()` (`:143`), `parseBackend` mongo → H2 con warning.
8. Gradle bukkit: quitar `compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")`.
9. API: `StorageBackend` → `{MYSQL, H2}` (rompe binario si un tercero lo usa: hoy nadie fuera del repo, NO VERIFICADO).
10. `settings.yml` Core: quitar `STORAGE.MONGO` y "mongo" del comentario `BACKEND`.
11. Docs: `ARCHITECTURE.md:25,40,58`, `INSTALL.md`, `features.md`, `phases.md`, `JAVA_REPORT.md`, `plan/MODULE-INVENTORY.md`, `plan/PHASE-STATUS.md`, `memory/PROGRESS.md`, skill `.claude/skills/ioricore-storage/SKILL.md:3,11,12,25`; cerrar A9 en OPEN-QUESTIONS → DECISIONS.
12. Verificar: `gradlew build`, `unzip -l` sin `libs/mongodb|libs/bson`, arranque con `DATABASE: MONGODB` legado → warning + H2.
Ahorro esperado: ≈2,1 MB por jar proxy; menos superficie (driver + bson + SPI).

**ESTADO**: proxy `PARTIAL` (conecta, sin consumidores) · Core `STUB`.

---

## 3. Redis

### 3.1 Proxy `P:redis/JedisRedisService.java`
| Aspecto | Real | Ref |
|---|---|---|
| Config | `redis.yml`: `ENABLED=false`, `HOST/PORT/DATABASE/TIMEOUT-MS/SSL`, `KEY-PREFIX=ioriproxy`, `CHANNEL=ioriproxy`, `AUTHENTICATION.{ENABLED,USERNAME,PASSWORD}` | `P:config/RedisConfig.java` |
| Pool | JedisPool max16/idle8/min1, testOnBorrow, ping al conectar | `:57-106` |
| TLS / ACL | `ssl(bool)` sin truststore/hostname config; ACL user+password opcional | `:66-80` |
| Suscripción | 1 hilo daemon, `subscribe(channel)`; al perderla espera 3 s y `reconnectQuiet()` | `:108-148`, `:257-270` |
| Envelope | JSON `{type, schema_version(=1), origin_proxy_id, event_id(UUID), timestamp, data{}}` | `P:redis/RedisEvent.java:14,54-80` |
| Filtro origen | descarta si `origin_proxy_id == proxyId` | `:154` |
| Dedupe | `Set<event_id>`, se **vacía entero** a 2048 | `:155,184-188` |
| Versionado | `schema_version` se lee pero **no se valida** | `RedisEvent.java:73` |
| Keys | `<prefix>:presence:<uuid>`, `:presence:name:<n>`, `:blacklist`, `:maintenance:state:*`, `:maintenance:whitelist` | `PresenceService.java:194-198`, `BlacklistService.java:113-124`, `MaintenanceService.java:508-627` |
| Disabled/fallo | `NoopRedisService` | `IoriProxy.java:554-576` |

**Bug crítico — origen fijo**: `SettingsConfig.ORIGIN_PROXY_ID = "proxy"` y `proxyId()` lo devuelve siempre (`P:config/SettingsConfig.java:8,26-28`; `redis.yml:39` lo documenta). Con ≥2 proxies todos comparten id → cada proxy **descarta todos los eventos de los demás** (`JedisRedisService.java:154`; `AdminChatService.java:114` repite el filtro). Multi-proxy por Redis queda inoperante; además `ip_analytics.proxy_id` mezcla proxies y `PresenceService` atribuye todo a "proxy".
Otros: `connect()` desde el hilo suscriptor encola otro `startSubscriber()` en cada reconexión (tareas acumuladas, `:95,265`); `KEYS` en mantenimiento (`MaintenanceService.java:535`) bloquea Redis con muchas claves; sin firma/HMAC → cualquiera con acceso a Redis inyecta `PUNISHMENT_*`, `STAFFCHAT_MESSAGE`, etc.

### 3.2 `ProxyEventType` (`P:redis/ProxyEventType.java`)
| Evento | Publica | Consume | Payload `data` |
|---|---|---|---|
| PLAYER_JOIN / QUIT / SWITCH | `P:server/PresenceService.java:43,50,58` | `PresenceService.java:34-36,82-100` | `uuid, name, server` |
| REDIS_TEST | `P:commands/RedisTestCommand.java:48` | log `IoriProxy.java:564-567` | `sender, message` |
| MAINTENANCE_UPDATE / _WHITELIST | `P:maintenance/MaintenanceService.java:586,598` | `:58-59` | `scope_type, scope_id, active, reason, enabled_by, start_at, end_at` / whitelist |
| BLACKLIST_UPDATE | `P:blacklist/BlacklistService.java:144` | `:42` (recarga desde Redis set) | vacío |
| STAFFCHAT_MESSAGE | `P:staffchat/StaffChatService.java:102` | `:45` | `player, rank, server, message` |
| ADMINCHAT_MESSAGE | `P:adminchat/AdminChatService.java:88,110` | `:46` | `kind, player, rank, server, message` |
| STREAM_BROADCAST | `P:stream/StreamService.java:104` | `:59` | `player, title, url, video` |
| ANNOUNCEMENT_RELOAD | `P:announcements/AnnouncementService.java:101` | `:60` | vacío |
| REPORT_CREATED / RESOLVED | `P:report/ReportService.java:213,220` | `:64-65` | `id, reporter, reporter_uuid, target, reason, detail, server, created_at, status, resolver` |
| HELPOP_CREATED / REPLY | `P:helpop/HelpOpService.java:203,126` | `:69-70` | `id, player, player_uuid, message, server, created_at, staff` |
| DONATION_BROADCAST / GGWAVE_START | `P:donations/DonationService.java:117,179` | `:59-60` | `player, package, price, mode, duration, collected, meta` |
| FRIEND_REQUEST / ACCEPT / REMOVE / NOTIFY | `P:player/friends/FriendService.java:613-639` | `:73-76` | `from_uuid, from_name, to_uuid, to_name, uuid_a, uuid_b, kind, server…` |
| PUNISHMENT_APPLIED | `P:discord/DiscordPunishmentHandler.java:127` | `IoriProxy.java:307` → `banCheck.invalidate` | `type, target_uuid, target_name, actor, reason, expires_at` |
| PUNISHMENT_INVALIDATE | Core `B:redis/SoftRedisPublisher.java:56-82` | `IoriProxy.java:296-306` | `uuid` |

Ningún backend consume eventos (Core no suscribe nada).

### 3.3 Core `B:redis/SoftRedisPublisher.java`
- Solo publica `PUNISHMENT_INVALIDATE` en BAN/TEMPBAN (`B:punishment/PunishmentService.java:141-148`); no hay unban/pardon en `PunishmentService` (métodos `:22-207`).
- **Noop en la práctica**: Jedis es `compileOnly` y no se shadea (`bukkit/build.gradle.kts`), busca `redis.clients.jedis.JedisPool` sin relocar (`:34`); solo funciona si otro plugin expone Jedis **sin relocar** (OPEN-QUESTIONS B4).
- Sin AUTH, TLS ni DB (`:40-47`, `new JedisPool(host,port)`); `available=true` aunque Redis no responda (el pool no conecta al crearse); errores tragados (`:79-81`).
- Publish **síncrono** en el hilo que llama a `punish()` (normalmente main thread, comando) → I/O bloqueante (NO VERIFICADO el hilo exacto; viola `rules/50-seguridad-rendimiento.mdc:20`).
- Origen = `SETTINGS.SERVER-NAME` (`lobby-1`) ≠ `proxy` → sí pasaría el filtro de origen del proxy.
- Consecuencia: ban aplicado en Core **no invalida** la caché negativa del proxy → jugador ya cacheado como "no baneado" reentra hasta reinicio del proxy (requiere además misma BD, §1.2).

### 3.4 Qué debería ir por Redis según doc (PLAN.md:36,66; skill ioricore-storage)
| Evento core | Doc | Real |
|---|---|---|
| Ban/kick red (ban kick, invalidación) | Redis | Solo `PUNISHMENT_INVALIDATE` desde Core (noop) |
| Mute / warn / unban | implícito (caché + invalidación) | No existe evento |
| Rank update (ic_ranks GLOBAL) | caché + invalidación Redis | **No existe** evento `RANK_*` (NOT_IMPLEMENTED) |
| Staffmode / vanish sync | PLAN: plugin messaging (acción local) | NOT_IMPLEMENTED |
| Discord bridge / presence / gbc | Redis | presence y staffchat sí (rotos multi-proxy por id fijo) |

**ESTADO**: proxy Redis `IMPLEMENTED_UNTESTED` (multi-proxy roto por id fijo) · Core publisher `STUB` (noop sin Jedis externo) · eventos core (rank/mute/staffmode) `NOT_IMPLEMENTED`.

---

## 4. Plugin messaging `ioricore:main`

| Aspecto | Real | Ref |
|---|---|---|
| Canal | `ioricore:main` | `A:messaging/PluginChannels.java` |
| Tipos | `HANDSHAKE_PING(0)`, `HANDSHAKE_PONG(1)`, `STAFF_AUTH_SESSION(2)` = `ordinal()` | `A:messaging/PluginMessageType.java` |
| Formato | `[typeByte]` · `STAFF_AUTH_SESSION`: `[2, ok(0/1)]` | `P:staffauth/StaffAuthService.java:156-159` |
| Gate | módulo proxy `PLUGIN-MESSAGING: true` (`settings.yml:154`); Core `PluginMessagingModule` (`B:messaging/PluginMessagingModule.java:31`) | |
| Flujo | Proxy `ServerConnected` → PING + `STAFF_AUTH_SESSION` (si staff) · Core responde PONG · proxy reenvía sesión al aprobar (`StaffAuthService.java:151`) o al pasar a pending (`:185`) vía `sendBackendPluginMessage` (`P:velocity/VelocityPlatform.java:269-285`, `BungeePlatform.java:293`) | `P:velocity/impl/listener/VelocityPluginMessageListener.java:61-79`, `bungee/…/BungeePluginMessageListener.java:63-78`, `B:messaging/PluginMessagingBridge.java:41-59` |
| Anti-spoof proxy | consume todo mensaje del canal (`ForwardResult.handled()` / `setCancelled(true)`) **antes** de mirar origen → clientes no pueden inyectar hacia backend; solo procesa `ServerConnection`/`Server` | Velocity `:46-48`, Bungee `:47-49` |
| Validación Core | longitud ≥1, tipo conocido; **sin firma ni nonce**; confía en el canal | `PluginMessagingBridge.java:44-58` |

Riesgos:
- **Envío al servidor equivocado (probable)**: en `ServerConnectedEvent` Velocity aún no ha actualizado `player.getCurrentServer()` (se fija tras el evento) → `player.getCurrentServer().ifPresent(...)` (`VelocityPluginMessageListener.java:66`) envía PING/sesión al servidor **anterior** o a ninguno en el primer join. Bungee: el PING usa `event.getServer()` (bien) pero la sesión usa `player.getServer()` (`BungeePluginMessageListener.java:72,77`) = servidor anterior durante el evento. Comportamiento de la API **NO VERIFICADO** en runtime; si se confirma, el backend nunca recibe `ok=1`.
- Sin firma: si un backend es alcanzable directo sin forwarding moderno/BungeeGuard, un cliente puede falsificar `STAFF_AUTH_SESSION ok=1` (OPEN-QUESTIONS B8).
- Tipos por `ordinal()`: reordenar el enum rompe compatibilidad proxy↔core entre versiones.

**ESTADO**: `IMPLEMENTED_UNTESTED` (handshake) · entrega de sesión `PARTIAL` (probable bug de destino).

---

## 5. Discord / JDA (solo proxy)

| Aspecto | Real | Ref |
|---|---|---|
| Lib | `JDA 5.6.1` sin opus, relocado `us.iorilabs.libs.jda` (+okhttp, okio, trove, nv) | `velocity/build.gradle.kts` |
| Config | `settings.yml:180-200` `DISCORD.BOT.{ENABLED=false,TOKEN,GUILD-ID,PREFIX,SEND-WEBHOOK-EVENTS,SLASH-COMMANDS,PREFIX-COMMANDS,CHANNELS.*,COMMANDS.*}` · plantillas `discord.yml` (`STAFF-CHAT, WELCOME, GOODBYE, BOOST, STORE, STATUS, STATUS-START, STATUS-STOP, PUNISHMENT, STAFF-AUTH`) | `P:config/DiscordConfig.java` |
| Arranque | async, `awaitReady`, fail-soft sin token | `P:discord/DiscordBotService.java:95-133` |
| Intents | `GUILD_MESSAGES, MESSAGE_CONTENT, GUILD_MEMBERS, GUILD_MODERATION` (2 privilegiados) + `MemberCachePolicy.ALL`, `ChunkingFilter.NONE` | `:110-119` |
| Slash | `/ban /tempban /mute /tempmute /warn /kick` (`player`, `duration`, `reason`), guild si `GUILD-ID`, si no global | `:406-432`, `enums/DiscordPunishmentType.java` |
| Prefix | `!ban …` etc. | `DiscordGatewayListener.java:48-60` |
| Permisos Discord | `COMMANDS.CHANNEL-ID` + `ROLE-IDS`; sin roles → `MODERATE_MEMBERS`/`BAN_MEMBERS` | `DiscordGatewayListener.java:166-185` |
| Sanción desde Discord | inserta `ic_punishments` (BD proxy), kick, invalida caché local, publica `PUNISHMENT_APPLIED` | `DiscordPunishmentHandler.java:60-160`; mensaje kick **hardcode EN** (`:88`) |
| Embeds | `DiscordEmbeds.fromTemplate` / `fromWebhookFormat`; avatar Visage | `DiscordEmbeds.java`, `discord.yml` |
| Staffchat bridge | MC→Discord embed (`onStaffChatFromMinecraft` `:206`); Discord→MC a staff online **solo del proxy local** (no Redis) (`:222-258`, `Gateway:36-45`) | |
| Staff-auth | embed + botón `staffauth:approve:<token>` en **canal** `CHANNELS.STAFF-AUTH` (`:268-285`); botón valida rol/permiso (`Gateway:128-158`) | **No hay DM** (decisión: "Discord DM Approve", DECISIONS:63) |
| Welcome/goodbye/boost/store/status | plantillas + timer status | `:186-266` |
| `/sync` OTC (vincular cuenta) | **NOT_IMPLEMENTED** (grep sin resultados; sin tabla `discord_links` de PLAN.md:61) | PHASE-STATUS H7 "sin confirmar" |

### 5.1 Webhooks
- `P:webhooks/WebhookRegistry.java:48-67` carga `webhooks/{blacklist,donations,maintenance,reports,request,staff-auth,status,stream}.yml`; `send()` (`:120-135`) postea si hay URL y además hace **mirror** al bot (`DiscordBotService.mirrorWebhook:162-185`, excluye `staff-auth` para no duplicar).
- `WebhookClient.post` async, timeout 15 s, sin reintento ni manejo de 429/`Retry-After`, sin `allowed_mentions` (`WebhookClient.java:123-140`) → textos de jugador (reports/helpop) podrían mencionar `@everyone` si el contenido llega a `content` (NO VERIFICADO qué campos van a content vs embed).
- Llamadores: donations, helpop, reports, staff-auth, stream/video, status (`grep webhooks.send`).

**ESTADO**: bot/embeds/sanciones/staffchat `IMPLEMENTED_UNTESTED` · staff-auth approve por canal `IMPLEMENTED_UNTESTED` · DM approve `NOT_IMPLEMENTED` · `/sync` OTC `NOT_IMPLEMENTED` · webhooks `IMPLEMENTED_UNTESTED`.

---

## 6. Staff auth

### 6.1 IoriCore actual
| Pieza | Real | Ref |
|---|---|---|
| Proxy servicio | `StaffAuthService` (módulo `STAFF-AUTH: true`, `IoriProxy.java:388-393`) · hook en login `VelocityPresenceListener.java:123-125`, quit `:169` | `P:staffauth/StaffAuthService.java` |
| Staff = | `StaffGroups.isStaff` o permiso `STAFF-AUTH.STAFF-PERMISSION` (`ioriproxy.staff`) | `:61-66` |
| Lógica | 1.ª vez → aprobado (TOFU) y baseline país · misma IP → aprobado + geo async; si país cambia → pending · IP distinta → pending (token 12 hex, TTL 900 s) | `:68-98`, `:171-202` |
| Geo | `http://ip-api.com/json/%ip%` (**HTTP plano**), timeout 8 s | `P:config/StaffAuthConfig.java`, `:295-320` |
| Aprobación | `/staffauth approve <token>` (perm `ioriproxy.staffauth.approve`, `permissions.yml:43`) o botón Discord | `P:commands/argument/StaffAuthApproveArgument.java` |
| Persistencia | **ninguna**: `lastSeen`/`sessions` en memoria (`:36-38`) | |
| Core enforce | `StaffAuthEnforceModule` (**`MODULES.STAFF-AUTH: false`** por defecto, `settings.yml:57`) · ceguera + walk/fly 0 + cancela move/interact/break/place/drop/damage | `B:modules/staffauth/StaffAuthEnforceService.java`, `…Listener.java` |
| Flags Core | `ENFORCE-WITHOUT-PROXY=false`, `RESTRICT-ON-DENY=false` (`settings.yml:200-206`) → con defaults **nunca restringe**, ni en pending | `StaffAuthEnforceService.java:33-35,63-74` |

Problemas:
- **Token mostrado al propio jugador** (`messages/es.yml:226` "Token: <token>") y logueado en consola (`StaffAuthService.java:186,281`). Si el staff (o quien robó la cuenta) tiene `ioriproxy.staffauth.approve`, se auto-aprueba.
- **Reinicio del proxy = todos aprobados** (TOFU en memoria): un atacante tras un restart entra sin reto.
- Sin bloqueo de chat ni comandos en Core (CoreDub sí lo hace); sin bloqueo de inventario.
- El aprobador Discord es cualquier miembro con rol/permiso; no se comprueba que sea otro distinto del solicitante ni identidad vinculada.
- Geo por HTTP plano: IP del staff viaja sin cifrar a un tercero.

### 6.2 Referencia CoreDub (propia, `.cursor/_refs/CoreDub/Bukkit/…/coredub/`)
| CoreDub | Detalle | Ref |
|---|---|---|
| Registro | tabla `core_staffs(uuid, name, password VARCHAR(36), address, status, auth, anticmds)`; filas creadas fuera del plugin ("Tienes que ser registrado por un Staff administrativo"); `staffregister` declarado en plugin.yml sin executor | `persistence/PlayerAdapterSQL.java:73,85`; `commands/admin/LoginCommand.java` |
| Hash | **ninguno**: contraseña en claro, `args[0].equals(password)` | `LoginCommand.java` |
| 2FA | `/auth register` → Google Authenticator TOTP (clave mostrada en chat **y en log Debug**) | `commands/admin/AuthCommand.java` |
| Login | `/login` (`l, sl, stafflogin`) contraseña o código TOTP; guarda IP | `LoginCommand.java` |
| Sesión | `SecurityState {STAFF_NOT_LOGGED, STAFF_LOGGED, STAFF_ROOT}`; se exige login si IP ≠ última o rango staff sin registro | `players/SecurityState.java`, `controllers/PlayerController.java:370-384` |
| Bloqueos | ceguera, gamemode join, chat bloqueado (`:389-403`), comandos bloqueados salvo `/stafflogin`/`/sl` (`:432-436`), no-registrado → kick al usar comando | `PlayerController.java` |
| Root | `/anticmds login <pass>` → `STAFF_ROOT` desbloquea comandos peligrosos (`getBlockedCommands`) | `commands/admin/AntiCmdsCommand.java:32-37` |
| Alertas | `sendSecurityAlert` por bus propio a `SECURITY_CLIENT` (IP change, login ok/fail, register, 2FA) | `controllers/CoreController.java:315`, `connection/CoreRedisConnector.java:232` |

### 6.3 Gap vs decisión aprobada (DECISIONS:63, EXTRAS_BACKLOG:40-43: auth en proxy **solo** si cambia IP/geo, Discord **DM** Approve, backend blind/immobile)
| Requisito | Estado |
|---|---|
| Reto solo si cambia IP/geo | `PARTIAL` (lógica sí; sin persistencia → tras reinicio no hay referencia) |
| Discord DM con país, IP parcial, botón Approve | `PARTIAL` (embed+botón en canal, no DM; no hay vínculo Discord↔MC para saber a quién mandar DM — depende de `/sync`) |
| Backend blind/immobile hasta auth | `PARTIAL` (código existe; módulo off + `RESTRICT-ON-DENY=false`; probable bug de destino del paquete §4) |
| Bloqueo chat/comandos mientras pending | `NOT_IMPLEMENTED` (proxy no filtra comandos; Core no cancela chat/comandos) |
| Token no visible para el solicitante | `NOT_IMPLEMENTED` |
| PLAN.md:31 "register/login + Argon2/BCrypt" | Contradice la decisión 01-11 (sin contraseña). Documentado: **ambas versiones**; vigente = DECISIONS. CoreDub aporta ideas (estado de sesión, bloqueo chat/cmd, root), **no** su almacenamiento en claro |

**ESTADO**: proxy staff auth `PARTIAL` · Core enforce `PARTIAL` (off por defecto).

---

## 7. Hooks

| Hook | Lado / clase | API | Aporta | Fallback |
|---|---|---|---|---|
| LuckPerms | Proxy `P:hook/luckperms/LuckPermsHook.java` (instanciado `IoriProxy.java:184,458`; usado por `StaffGroups`) | reflexión `LuckPermsProvider` | prefix para staff groups/placeholders | `""`; dependencia opcional Velocity `@Dependency(id="luckperms", optional=true)`, Bungee `softDepends` |
| LuckPerms | Core `B:hook/SoftLuckPermsHook.java` (`IoriCorePlugin.java:132`, `RanksModule.java:47`) | reflexión | prefix/suffix en chat (`ChatFormatService.java:144-157`), board (`BoardPlaceholders.java:37`) | Noop; `applyPrefix` es **vacío** (`:41-43`) |
| Vault | Core `SoftVaultHook` (`:131`) | reflexión `ServicesManager` Chat/Economy | prefix/suffix, economía delegada (`EconomyService.java:43-69`) | economía propia `ic_balances`/YAML. Core **no** se registra como proveedor Vault |
| LiteBans | Core `SoftLiteBansHook` (`PunishmentsModule.java:48`) | reflexión `litebans.api.Database` | `isBanned/isMuted` adicionales (`PunishmentService.java:61,83`) | sistema propio. Proxy: sin hook LiteBans |
| PlaceholderAPI | Core `ChatFormatService.java:165-170` | reflexión `PlaceholderAPI.setPlaceholders` | placeholders en formato chat | texto sin cambios. Sin expansión propia registrada |
| Geyser/Floodgate | Core `SoftGeyserHook` (`:135`) | reflexión `GeyserApi`/`FloodgateApi` | `isBedrock` en `/ip`, join status, join MOTD | false |
| Apollo (Lunar) | Core `SoftApolloHook` (`:133`) | reflexión `Apollo.getModuleManager` GlowModule | glow staffmode (`StaffModeModule.java:45`), scoreboard numbers (`ScoreboardService.java:197`) | noop |
| nLogin | Core `SoftNLoginHook` (`:134`) | reflexión `nLoginAPI` (Identity o String) | auth/premium en join status, cambio de contraseña en `/settings` (`PlayerSettingsModule.java:47`) | Noop |
| ViaVersion | Core `B:util/ProtocolProbe.java:13` | reflexión `Via.getAPI` | protocolo cliente → MODERN/LEGACY en idioma/formatos | protocolo del servidor |
| Essentials | solo `softdepend` en `plugin.yml` | — | ninguna integración (módulo essentials propio) | — |

Todos con flag `MODULES.*-HOOK` en `settings.yml` Core. Ninguno `compileOnly` → firmas de API sin comprobar en compilación (rotura silenciosa si cambian). **ESTADO**: todos `IMPLEMENTED_UNTESTED`, salvo LuckPerms proxy (img `04b5d2b7` "LuckPerms" en consola — histórico, contenido exacto NO VERIFICADO) y Essentials `NOT_IMPLEMENTED`.

---

## 8. Propietario de tablas y runner único — PROPUESTA

Coherente con PLAN.md:60-62 ("un esquema MariaDB/MySQL", "un dueño de migraciones (proxy arranque)"), DECISIONS 11-03 (MariaDB/MySQL principal, H2 fallback, sin datos a migrar) y "no `proxy-common` automático; preferir `common/`".

| Tabla | Dueño propuesto | Escriben |
|---|---|---|
| `ip_*` (9) | proxy | proxy |
| `ic_punishments` | **definición compartida** (una sola DDL), aplicada por el runner | Core (comandos) + proxy (Discord) |
| `ic_ranks`, `ic_rank_permissions`, `ic_balances` | Core (dominio), DDL en el mismo catálogo | Core |
| futuras (`discord_links`, `auth_staff`, notes, tags) | según PLAN | — |

Runner propuesto:
1. Catálogo de migraciones **solo `java.sql`** en `common/` (o `api/`, release 8) — sin nuevas dependencias — p.ej. `us.iorilabs.…storage.migration.Migration{component, version, sqlMysql, sqlH2}`.
2. Tabla única `ic_schema_version(component VARCHAR(32), version INT, applied_at, PRIMARY KEY(component, version))`; componentes `proxy`, `core`, `shared`. Como no hay datos, se puede empezar en v1 limpio y abandonar `ip_schema_version` (o mapear su v6 → `proxy` v1).
3. Ambos lados ejecutan el **mismo runner** al arrancar, cada uno sus componentes + `shared`; lock de arranque (`GET_LOCK` en MySQL / transacción en H2) para evitar carreras entre N backends y proxies.
4. Una sentencia por paso, en transacción cuando el motor lo permita; fallo → log claro + modo Noop/H2 según lado (sin "excepción ignorada").
5. Índices: inline en MySQL, `CREATE INDEX IF NOT EXISTS` solo en H2 (corrige §1.2).
6. Unificar drivers (Hikari 5.1.0, mysql-connector-j 9.x, H2 2.3.232) y defaults de BD (misma `DATABASE` en proxy y Core si se quiere `ic_punishments` compartida).
7. Quitar `ensureSchema()` ad-hoc de `PunishmentSql`, `RankSql` (ALTERs), `EconomySql` y los bloques v1–v6 de `MySqlStorageService.migrate`.

Pendiente de decisión: A8 (dueño único vs por módulo) y si Core puede migrar sin proxy (servidores sueltos con H2).
