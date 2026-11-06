# ARCHITECTURE — estado REAL del código (auditoría 2026-11-03)

Objetivo/decisiones → `docs/plan/PLAN.md` + `docs/memory/DECISIONS.md`. Aquí solo lo que existe hoy. Divergencias → `OPEN-QUESTIONS.md`.

## Monorepo `IoriCore` (Gradle 8.13 KTS · versión `1.1.0` · group `us.iorilabs`)
Ruta: `D:\Minecraft\Plugins\Proxy` (no es repo git; `.gitignore` excluye `.cursor/` y `docs/`).

| Proyecto Gradle | Dir | Paquete raíz | `release` | Artefacto | Java / LOC |
|---|---|---|---|---|---|
| `:common` | `common/` | `us.iorilabs.proxy.{commands,utilities,placeholders,maintenance}` ⚠ | 17 | lib | 18 / ~1k |
| `:api` | `api/` | `us.iorilabs.api.{hook,messaging,modules,punishment,storage}` | 8 | lib | 18 / ~0.3k |
| `:velocity` | `velocity/` | `us.iorilabs.proxy.*` + adapter `…proxy.velocity` | 17 | **IoriVelo-1.1.0.jar** (~26 MB, JDA) | 140 / ~17k |
| `:bungee` | `bungee/` | `us.iorilabs.proxy.*` + adapter `…proxy.bungee` | 17 | **IoriBungee-1.1.0.jar** | 140 / ~17k |
| `:bukkit` | `bukkit/` | `us.iorilabs.bukkit.*` | 17 | **IoriCore-Bukkit-1.1.0.jar** (~8.5 MB) | 228 / ~18k |
| `:nms-api` | `nms/api/` | `us.iorilabs.abstraction.VersionWrapper` | 8 | lib | 1 |
| `:nms-v1_8_R3` | `nms/v1_8_R3/` | `us.iorilabs.nms.v1_8_R3` | 8 | lib | 1 (real, ~300 LOC) |
| `:nms-v1_12_R1`, `v1_16_R3`, `v1_20_R3` | `nms/…` | idem | 8 | lib | **stubs** (log warning) |
| `:nms-v1_21_R1` | `nms/v1_21_R1/` | idem | 21 (atributo JVM forzado a 17) | lib | 1 (real, ~230 LOC) |

**No existen**: `proxy-common/`, `paper/`. `package.json` = solo Prettier.

### Dependencias entre módulos
```
bukkit  → common, api, nms-api, nms-v1_8_R3 … nms-v1_21_R1   (shade)
velocity→ common, api   (+ JDA, Jedis, Hikari, MySQL, H2, Mongo, SnakeYAML, Gson, Adventure)
bungee  → common, api   (+ lo mismo + adventure-platform-bungeecord)
common  → adventure-api/minimessage/legacy/ansi (api)
nms-v*  → nms-api
```
Compilación Spigot por **jars locales** `C:/Users/TqmassPC/Documents/BuildTools/Spigot Versions/` (1.8.8, 1.21.1 + libs). Toolchain JDK 17 (21 para v1_21_R1).

## Reglas no negociables — verificadas
- ✓ Sin `net.minecraft`/`org.bukkit`/CraftBukkit en `velocity/`, `bungee/`, `common/`.
- ✓ Sin imports Velocity/Bungee/Bukkit en `common/` ni `api/`.
- ✓ JDA solo en proxy (velocity+bungee). Ninguno en `bukkit/`.

## Proxy (velocity ≡ bungee)
**127 de 129 ficheros compartidos son idénticos byte a byte** (sin imports de plataforma) → código de `proxy-common` duplicado. Difieren: `motd/MotdService` (comentario), `player/transfer/AccountTransferService` (formato). Adapters propios: `velocity/{IoriProxyVelocity,VelocityPlatform,command/*,impl/listener/*,player/*}` y equivalentes bungee.

Paquetes de feature (`us.iorilabs.proxy.*`): `adminchat alts analytics announcements blacklist commands config database{mysql,mongo} discord donations helpop hook/luckperms language logging maintenance modules motd placeholders platform player{friends,playtime,transfer} punishment redis report server staff staffauth staffchat stream webhooks`.
Comandos: Send Goto Find Glist Servers Hub Queue Maintenance Staff StaffChat AdminChat Alert Announce Report Reports Request(HelpOp) Friends Playtime Seen Alts Blacklist Analytics Donation GgWave Stream Ping StaffAuth Transfer RedisTest Root.
Clases grandes: `FriendService` 1019 · `IoriProxy` 678 · `MaintenanceService` 653 · `MySqlStorageService` 538 · `DiscordBotService` 436.
Plugin ids: Velocity `iorivelo` (⚠ `@Plugin version="1.0.0-SNAPSHOT"` hardcode) · Bungee `IoriBungee`. Permisos `ioriproxy.*`.

## Core (bukkit)
`IoriCorePlugin` registra en `ModuleRegistry` (toggle `settings.yml → MODULES.*`): Economy, ChatGames, Gender(stub), Tags, Coinshop, StaffMode, BossbarTips, ChatLogs, ChatFormat, EasterEggs, Essentials, Ranks, Punishments, StaffAuthEnforce, PluginMessaging, PlayerSettings, Scoreboard, Tab, Skin, Ip, JoinMotd (+ `JoinStatusListener`).
Infra: `menu/` (framework + `menu/api` + anvil), `fx/` (Sound/Title/ActionBar), `language/CoreLanguage` (modern/legacy por protocolo, `SETTINGS.LEGACY-MAX-VERSION`), `hook/Soft*` (Vault, LP, LiteBans, Apollo, nLogin, Geyser), `commands/` (`BukkitCommandBridge` sobre `CommandParent` de common), `abstraction/{VersionMatcher,FxAwareVersionWrapper,BukkitNativeFx,Noop}`.
Permisos `ioricore.*`. Sin sistema `Profile`/PlayerData propio en Core.
`VersionMatcher`: 1.8→v1_8_R3 · 1.12→v1_12_R1 · 1.16→v1_16_R3 · 1.20→v1_20_R3 · 1.21.*→v1_21_R1 · resto → fallback.

## Datos
| Tabla | Crea | Uso |
|---|---|---|
| `ip_players`, `ip_playtime`, `ip_friends`, `ip_friend_requests`, `ip_account_ips`, `ip_reports`, `ip_helpop`, `ip_analytics`, `ip_schema_version` | proxy `MySqlStorageService` | proxy |
| `ic_punishments` | **proxy y bukkit** (DDL duplicada) | bukkit escribe · proxy BanCheck lee |
| `ic_balances`, `ic_ranks`, `ic_rank_permissions` | bukkit (`EconomySql`, `RankSql`) | bukkit |

Backends: proxy `StorageType {NONE,H2,MYSQL,MONGODB}` (default H2) · bukkit `CoreSqlStorage` (H2/MySQL) + `SoftMongoStorage`. Versiones de driver distintas por lado (Hikari 4.0.3 vs 5.1.0, mysql-j 8.3 vs 9.1, H2 2.1.214 vs 2.3.232). H2 = fichero por proceso → no hay esquema compartido real sin MySQL.
Ban-check: proxy `BanCheckService`+`BanCheckCache` fail-open; bukkit `PunishmentService` fail-open.

## Mensajería
- **Redis** (`redis.yml CHANNEL: ioriproxy`): proxy `JedisRedisService` pub/sub con `ProxyEventType` {PLAYER_JOIN/QUIT/SWITCH, MAINTENANCE_*, BLACKLIST_UPDATE, STAFFCHAT/ADMINCHAT_MESSAGE, STREAM/DONATION_BROADCAST, GGWAVE_START, ANNOUNCEMENT_RELOAD, REPORT_*, HELPOP_*, FRIEND_*, PUNISHMENT_APPLIED/INVALIDATE, REDIS_TEST}. Bukkit `SoftRedisPublisher`: solo publica, Jedis `compileOnly` + `Class.forName` → **noop si ningún otro plugin aporta Jedis**.
- **Plugin messaging** `ioricore:main` (`api.messaging.PluginChannels`): `PluginMessageType {HANDSHAKE_PING, HANDSHAKE_PONG, STAFF_AUTH_SESSION}`. Velocity `handled()` / Bungee `setCancelled` + solo acepta origen backend. Backend sin firma/HMAC (depende de forwarding seguro).
- **Discord**: JDA en proxy (`discord/DiscordBotService`, `DiscordEmbeds`, `DiscordGatewayListener`, `DiscordPunishmentHandler`) + webhooks `webhooks/*.yml`.

## Configuración (resources)
- Proxy: `settings commands permissions database redis discord donations fallback maintenance motd queue .yml` · `messages/{en,es}.yml` + `messages/legacy/{en,es}.yml` · `webhooks/{blacklist,donations,maintenance,reports,request,staff-auth,status,stream}.yml`.
- Bukkit: `settings commands permissions ranks sounds .yml` · `messages/{en,es}.yml` + `messages/legacy/` · `chat/formats/{en,es}.yml` + `chat/formats/legacy/` · `menus/{modern,legacy}/<menu>.yml` (layout; textos en messages) · `data/{easter-eggs,tags,coinshop,chatgames}.yml` · `kits/` runtime.

## Build / deploy
`set JAVA_HOME=C:\Program Files\Java\jdk-17` (el `java` del PATH es 11) → `gradlew.bat build --no-daemon` ✓ verde 2026-11-03. Sin tests (`src/test` no existe). Deploy: regla `70-deploy-proxy`.
