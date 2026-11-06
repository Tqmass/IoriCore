# CONFIGURATIONS — ficheros YAML (estado real vs intención)

> Auditoría 2026-11-03 · solo lectura · fuente de verdad = código. Intención = `.cursor/rules/40-yaml-config.mdc`, `.cursor/design/yaml-comments.md`, `.cursor/docs/memory/DECISIONS.md`.
> Ver también: [PROXY-MODULES](PROXY-MODULES.md) · [CORE-MODULES](CORE-MODULES.md) · [COMMANDS](COMMANDS.md) · [PERMISSIONS](PERMISSIONS.md) · [MENUS](MENUS.md) · [RANKS](RANKS.md) · [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) · [INTEGRATIONS](INTEGRATIONS.md) · [FEATURE-STATUS](FEATURE-STATUS.md).
> Ningún YAML ha sido probado en servidor con evidencia `RESULT-OK` (IMAGE-INDEX sin entradas) → máximo `IMPLEMENTED_UNTESTED`.

## 0. Mecánica de carga (importa para todo lo demás)

| Aspecto | Proxy (Velocity/Bungee) | Core (Bukkit) |
|---|---|---|
| Lector | `YamlConfig` (SnakeYAML) `velocity/.../proxy/config/YamlConfig.java` | `YamlConfiguration` Bukkit |
| Registro | `ConfigManager.FILES` (11 ficheros) `ConfigManager.java:13`; webhooks aparte `WebhookRegistry.java:53`, `StatusWebhookService.java:37`; messages `LanguageManager.java:58-59` | `IoriCorePlugin.java:90-102` (`saveResource` ×9 + `loadYaml` ×3); resto por módulo |
| Mayúsculas | **Normaliza**: `_`→`-` y UPPERCASE en carga y lookup (`YamlConfig.java:257`). `"friends.max_friends.default"` = `FRIENDS.MAX-FRIENDS.DEFAULT` | **Case-sensitive**: la clave debe coincidir exacta (`SETTINGS.LANGUAGE`) |
| Defaults del jar | Solo se copia si el fichero no existe (`YamlConfig.java:72`). **No hay merge** → en upgrades faltan claves nuevas | Messages: jar primero + disco encima (`CoreLanguage.java:191-210`) → claves nuevas siempre resuelven. Config: no merge |
| Clave de mensaje ausente | `raw()` devuelve **la propia clave** (`LanguageManager.java:100`); legacy cae a modern | Fallback jar |
| Escritura | `saveTopLevelList` preserva comentarios (solo `WHITELIST`, `MaintenanceService.java:570`) | `yaml.save()` (p.ej. `ranks.yml`, `RankService.java:~170`) **borra comentarios** |
| Reload | `ConfigManager.reloadAll()` → `loadAll()` (webhooks no se recargan con él — NO VERIFICADO en `/ioriproxy reload`) | NO VERIFICADO |

## 1. Inventario

| Plataforma | Ficheros | Leídos realmente |
|---|---|---|
| Velocity `velocity/src/main/resources` | `settings, redis, database, permissions, motd, fallback, queue, commands, maintenance, donations, discord` + `webhooks/{blacklist,donations,maintenance,reports,request,staff-auth,status,stream}` + `messages/{en,es}` + `messages/legacy/{en,es}` (23) | 23/23 |
| Bungee `bungee/src/main/resources` | idénticos a Velocity + `plugin.yml` (24) | 23/23 + plugin.yml (descriptor) |
| Bukkit `bukkit/src/main/resources` | `settings, commands, permissions, ranks, sounds, plugin` · `data/{chatgames,coinshop,easter-eggs,tags}` · `messages/{en,es}` + `legacy/` · `chat/formats/{en,es}` + `legacy/` · `menus/{modern,legacy}/` ×11 (42) | 40/42 (`commands.yml`, `permissions.yml` cargados pero **nunca leídos**) |

### Velocity vs Bungee (`diff -r --strip-trailing-cr`)
- Idénticos salvo: `fallback.yml` (2 comentarios: EN en velocity `:4,:53`, ES en bungee) y `plugin.yml` solo en bungee.
- `messages/en.yml` y `es.yml` difieren **solo en fin de línea** (Velocity CRLF, Bungee LF).
- Código `config/*` y `LanguageManager` idénticos en ambos.
- → Duplicación pura: 23 ficheros mantenidos a mano ×2. Riesgo de deriva (ya ocurrió en `fallback.yml`).

## 2. Proxy — ficheros funcionales

Notación: claves en forma YAML; "Ruta" = lector. Tipos: B bool, I int, D double, S string, L lista, M mapa. Todas opcionales (fallback a default de código) salvo indicación.

### 2.1 `settings.yml` — `SettingsConfig`, `PlaytimeConfig`, `FriendsConfig`, `CommandWhitelistConfig`, `AnnouncementsConfig`, `AnalyticsConfig`, `StaffAuthConfig`, `DiscordConfig`

| Clave | Tipo | Default código | ¿Leída? / Ruta |
|---|---|---|---|
| `SETTINGS.LANGUAGE` | S `ES`/`EN` | `es` | ✔ `SettingsConfig.java:31` (validada no vacía `:17`) |
| `SETTINGS.DEBUG` | B | false | ✔ `SettingsConfig.debug()` |
| `SETTINGS.LEGACY-MAX-VERSION` | S versión MC | protocolo 340 (`legacy-protocol-max`) | ✔ `SettingsConfig.java:47-54` |
| `PLAYTIME.FLUSH-INTERVAL-SECONDS` / `MEMORY-FALLBACK` | I(≥15) / B | 60 / true | ✔ `PlaytimeConfig` |
| `COOLDOWNS.{REPORT,HELPOP,STREAM,FRIENDS-REQUEST}` | I s | 30/20/60/10 | ✔ `SettingsConfig.cooldownSeconds` vía Report/HelpOp/Stream/FriendsConfig |
| `NOTIFICATIONS.FRIENDS.{JOIN,QUIT,SERVER-SWITCH}` | B | true | ✔ `FriendsConfig` |
| `FRIENDS.CONFIRM-REMOVE` / `MAX-FRIENDS.DEFAULT` / `MAX-FRIENDS.GROUPS.<X>.{PERMISSION,MAX}` | B / I / M | true / 50 | ✔ `FriendsConfig.java:17-46` |
| `COMMAND-WHITELIST.{MODE,HIDE-WITHOUT-PERMISSION,GLOBAL-BLOCKED,GROUPS.<X>.{PERMISSION,COMMANDS}}` | S `WHITELIST`/`BLACKLIST`, B, L, M | whitelist / true | ✔ `CommandWhitelistConfig` (`ConfigManager.java:116`) |
| `ANNOUNCEMENTS.{INTERVAL-SECONDS,RANDOM,SERVERS,SOUND.{ENABLED,LEGACY,MODERN,VOLUME,PITCH}}` | I,B,L,… | 180,true,[],true,`note.pling`,`block.note_block.pling`,1,1 | ✔ `AnnouncementsConfig` |
| `ANALYTICS.PERSIST-DAILY-SQL` | B | true | ✔ `AnalyticsConfig` |
| `MODULES.<ID>` (30 claves) | B | **true** si falta | ✔ `SettingsConfig.moduleEnabled`; 29 ids referenciados en código. **`MODULES.MULTILOBBY` muerta** (no hay `moduleEnabled("multilobby")`; el toggle real es `fallback.yml MULTILOBBY.ENABLED`) |
| `STAFF-AUTH.{STAFF-PERMISSION,MASK-IP,GEO-LOOKUP,GEO-URL,TOKEN-TTL-SECONDS,GRACE-SECONDS,WEBHOOK-ENABLE}` | S,B,B,S,I(≥60),I,B | `ioriproxy.staff`,true,true,ip-api,900,2,true | ✔ `StaffAuthConfig.java:19-29` |
| `DISCORD.BOT.{ENABLED,TOKEN,GUILD-ID,PREFIX,SEND-WEBHOOK-EVENTS,SLASH-COMMANDS,PREFIX-COMMANDS}` | B,S,S,S,B,B,B | false,"","","!",true,true,true | ✔ `DiscordConfig.java:28-35` |
| `DISCORD.BOT.CHANNELS.{8 ids}` | S snowflake | "" | ✔ lista fija `DiscordConfig.java:38-47` (claves nuevas se ignoran) |
| `DISCORD.BOT.COMMANDS.{CHANNEL-ID,ROLE-IDS,BAN,TEMPBAN,MUTE,TEMPMUTE,WARN,KICK}` | S,L,B… | "",[],true | ✔ `DiscordConfig.java:53-59` |

- Leídas por código y no presentes en YAML (compat): `language`, `debug`, `legacy-max-version`, `legacy-protocol-max` en raíz (`SettingsConfig.java:34,43,51,54`).
- ⚠ Secreto `DISCORD.BOT.TOKEN` vive en `settings.yml` (mezcla funcional+secreto; ver propuesta).

### 2.2 `redis.yml` — `RedisConfig` (raíz del fichero)

| Clave | Tipo | Default | Ruta |
|---|---|---|---|
| `ENABLED` | B | false | ✔ `RedisConfig.java:12` |
| `HOST` / `PORT` / `DATABASE` | S / I(1-65535) / I | 127.0.0.1 / 6379 / 0 | ✔ validados `:56-62` solo si ENABLED |
| `TIMEOUT-MS` / `SSL` | I / B | 2000 / false | ✔ |
| `KEY-PREFIX` / `CHANNEL` | S | ioriproxy / ioriproxy | ✔ (`JedisRedisService.java:126,226` publica/suscribe en `CHANNEL` tal cual) |
| `AUTHENTICATION.{ENABLED,USERNAME,PASSWORD}` | B,S,S | false,"","" | ✔ |
Sin claves muertas. Hardcode: origin id `"proxy"` (`SettingsConfig.ORIGIN_PROXY_ID`), reconexión 1000/3000 ms (`JedisRedisService.java:113,138`).

### 2.3 `database.yml` — `DatabaseConfig`

| Clave | Tipo | Default | Ruta |
|---|---|---|---|
| `DATABASE` | S `H2`/`MYSQL`/`MONGODB`/`NONE` | h2 | ✔ `DatabaseConfig.type()` |
| `MYSQL.{HOST,PORT,DATABASE,USERNAME,PASSWORD,SSL}` | S,I,S,S,S,B | 127.0.0.1,3306,**ioriproxy**,root,"",false | ✔ |
| `MONGODB.{URI-ENABLED,URI,HOST,PORT,DATABASE,AUTHENTICATION.*}` | … | false, mongodb://…/ioriproxy … | ✔ (Mongo: decisión de retirada pendiente, OPEN-QUESTIONS A9) |
- Hardcode que debería ser config: pool Hikari `MAX_POOL=10`, `MIN_IDLE=2`, `CONNECTION_TIMEOUT=15s`, `MAX_LIFETIME=30min` (`DatabaseConfig.java:7-10`). El comentario del YAML lo reconoce ("Pool Hikari hardcoded").

### 2.4 `permissions.yml` — `PermissionsConfig`
- `PERMISSIONS.*` y `EXTRA-PERMISSIONS.*` → índice plano clave→nodo (`PermissionsConfig.java:9-27`). Clave ausente → `ioriproxy.<clave>` (`:31`).
- `STAFF.GROUPS.{ADMIN,MOD,HELPER}` ✔ `staffGroups()`; si vacío usa `EXTRA-PERMISSIONS.STAFF-*`.
- Duplicados internos: `FIND`=`WHEREIS`=`ioriproxy.find`; `STAFF.GROUPS.*` = `EXTRA-PERMISSIONS.STAFF-*`; `QUEUE-BYPASS` duplicado en `queue.yml BYPASS-PERMISSION`; `MAINTENANCE-BYPASS` duplicado en `maintenance.yml BYPASS-PERMISSION`; `FRIENDS-LIMIT-*` duplicado en `settings.yml FRIENDS.MAX-FRIENDS.GROUPS.*.PERMISSION`; `QUEUE-PRIORITY-*` duplicado en `queue.yml GROUPS.*.PERMISSION`. Detalle por nodo → [PERMISSIONS](PERMISSIONS.md).

### 2.5 `motd.yml` — `MotdConfig`
`RANDOM` B true · `CENTER` B true · `FAVICON` S "" · `MODERN` L(bloques `|`) · `LEGACY` L — todo ✔ (`MotdConfig.java:15-33`). Sin comentarios `# ~` (único funcional sin ellos). Modern usa MiniMessage, legacy `&`.

### 2.6 `fallback.yml` — `LobbyConfig`

| Clave | Default código | YAML | Nota |
|---|---|---|---|
| `PING-DELAY` / `PING-TIMEOUT` | 1000 / 3000(≥100) | igual | ✔ |
| `HUB.ENABLED` | true | true | ✔ |
| `JOIN-BALANCING.{ENABLED,GROUP}` | true / DEFAULT | igual | ✔ |
| `MULTILOBBY.ENABLED` | **true** | **false** | ⚠ default código ≠ YAML |
| `MULTILOBBY.{BALANCING,LOBBIES}` | least_players / → `FALLBACK.GROUPS.DEFAULT` | igual | ✔ |
| `FALLBACK.MODE` / `FALLBACK.GROUPS.DEFAULT` | DEFAULT / — | igual | ✔ `LobbyConfig.java:23-30`; **solo se lee el grupo `DEFAULT`** (otros grupos serían muertos) |
| `ON-SERVER-KICK`, `ON-SERVER-DOWN`, `CLEAR-CHAT`, `IGNORED-REASONS` | true,true,true,— | igual | ✔ |
- Compat leída y ausente: `fallback_servers` (`LobbyConfig.java:26`).

### 2.7 `queue.yml` — `QueueConfig`
`TICK-SECONDS` (2, ≥1) · `DEFAULT-MAX-PLAYERS` (80) · `SERVER-MAX-PLAYERS` M · `GROUPS.<X>.{WEIGHT,PERMISSION}` · `BYPASS-PERMISSION` — todo ✔ (`QueueConfig.java:18-57`). Hardcode: tick actionbar 1 s (`QueueService.java:33`).

### 2.8 `commands.yml` — `CommandsConfig`
- `ROOT.NAME` ✔ (`CommandsConfig.java:63`); `ROOT.ALIASES` leído (`:68`) pero **no está en el YAML**.
- `COMMANDS.<CMD>.{ALIASES,PERMISSION}` ✔ (`:29-58`); `PERMISSION` opcional (override de `permissions.yml`), ningún comando lo usa.
- 30 entradas; `StaffCommand` (`/staff`) no tiene entrada. Detalle → [COMMANDS](COMMANDS.md).
- Estilo: separadores por comando `# — /x ———` ✔ conforme a `yaml-comments.md`.

### 2.9 `maintenance.yml` — `MaintenanceConfig`
Todas las claves ✔ (`MaintenanceConfig.java:25-160`): `DEFAULT-REASON`, `KICK-NON-WHITELISTED`, `BYPASS-PERMISSION`, `MOTD.{OVERRIDE,SHOW-END-TIMER,MODERN,LEGACY,TIMER-MODERN,TIMER-LEGACY}`, `PLAYER-LIST.{ENABLED,COUNT-MESSAGE,COUNT-TIMER-MESSAGE,HOVER,HOVER-TIMER,SAMPLE-PLAYERS}`, `ALLOW-DURING-MAINTENANCE`, `KICK.{REDIRECT-LOBBY,DESCRIPTION,DESCRIPTION-LEGACY}`, `COUNTDOWN.{ENABLED,INTERVALS-SECONDS,ACTIONBAR,TITLE,CHAT,TITLE-MAIN,TITLE-SUB,STARTED,ENDED}`, `MODES`, `WHITELIST` (escrito por `/maintenance whitelist`).
- ⚠ Texto visible al jugador (MOTD, hover, kick, countdown) en fichero funcional y **solo en ES** — no pasa por `messages/{lang}`. `COUNTDOWN.CHAT/TITLE-*/STARTED/ENDED` vacíos por diseño ("textos en messages") → semi-muertas (override).
- Brand: "IoriLabs" / `discord.IoriLabs.net` (OPEN-QUESTIONS A15).

### 2.10 `donations.yml` — `DonationsConfig`

| Clave | Default código | YAML | Nota |
|---|---|---|---|
| `STORE-URL` | `https://tienda.IoriLabs.net` | `https://store.iorilabs.com` | ⚠ discrepancia |
| `DEFAULT-SKIN` | Steve | Steve | ✔ |
| `GOAL.{ENABLED,META-FALLBACK,BAR-LENGTH}` | true,500(→`GOAL.META`),20 | igual | ✔ |
| `TEBEX/BUILTBYBIT/CRAFTINGSTORE.{ENABLED,SECRET}` | false,"" | igual | ✔ (secretos en fichero funcional) |
| `GGWAVE.{ENABLED,DURATION-SECONDS,MAX-PER-PLAYER,MAX-TOTAL,INPUTS,RAINBOW,RAINBOW-ONLY-MESSAGE,NICK-FORMAT,MESSAGE-FORMAT}` | … | igual | ✔; compat `GGWAVE.TRIGGER-WORD` leída (`:59`) y ausente |
| `SOUNDS.{ENABLED,VOLUME,PITCH,LEGACY,MODERN}` | true,1,1,`random.levelup`,`entity.player.levelup` | igual | ✔ |

### 2.11 `discord.yml` — `DiscordConfig.loadEmbeds`
10 plantillas fijas (`DiscordConfig.java:87-97`): `STAFF-CHAT, WELCOME, GOODBYE, BOOST, STORE, STATUS, STATUS-START, STATUS-STOP, PUNISHMENT, STAFF-AUTH`, campos `COLOR, AUTHOR-NAME, AUTHOR-ICON-URL, TITLE, DESCRIPTION, FOOTER, THUMBNAIL-URL, TIMESTAMP` → todo ✔. Sin muertas.
- ⚠ Esquema **distinto** al de `webhooks/*.yml` (plano `AUTHOR-NAME` vs anidado `EMBED.AUTHOR.NAME`; webhooks tienen `CONTENT`, `TITLE-URL`, `IMAGE-URL`, `FOOTER-ICON-URL`). Mismo concepto, dos formatos.
- Título "RoyalCTW | Discord bot embeds" (resto de ficheros: "IoriProxy |").

### 2.12 `webhooks/*.yml` — `WebhookRegistry` (+ `StatusWebhookService`)
- 8 ficheros, nombres fijos `WebhookRegistry.java:18-25`. Claves: `ENABLED, URL, USERNAME, AVATAR-URL, CONTENT, EMBED.{COLOR,TIMESTAMP,AUTHOR.{NAME,URL,ICON-URL},TITLE,TITLE-URL,DESCRIPTION,THUMBNAIL-URL,IMAGE-URL,FOOTER,FOOTER-ICON-URL}` → todas ✔ (`:75-104`). `stream.yml` usa prefijos `STREAM.`/`VIDEO.` ✔ (`:57-60`). `status.yml UPDATE-INTERVAL-SECONDS` ✔ (`StatusWebhookService.java:16`, ≥15).
- Compat leídas y ausentes: `EMBED.FOOTER.TEXT`, `EMBED.FOOTER.ICON-URL`.
- No registrados en `ConfigManager.FILES` (cargados por su servicio). Bloque de comentario de avatar duplicado en los 8.
- `settings.yml STAFF-AUTH.WEBHOOK-ENABLE` duplica `webhooks/staff-auth.yml ENABLED`.

## 3. Proxy — `messages/` (LanguageManager)

Carga: `messages/{lang}.yml` (modern) + `messages/legacy/{lang}.yml` (protocolo ≤ `LEGACY-MAX-VERSION`). Legacy ausente → copia modern; clave ausente en legacy → modern; ausente en ambos → **se muestra la clave** (`LanguageManager.java:58-100`).

| Fichero | Rutas (claves+secciones) | Bloques top-level |
|---|---|---|
| `messages/es.yml` | 309 | 29 |
| `messages/en.yml` | 257 | 25 (faltan `ANNOUNCE, ALERT, GLIST, SEEN`) |
| `messages/legacy/es.yml` | 205 | 23 (faltan además `ALTS, ACCOUNT-TRANSFER`) |
| `messages/legacy/en.yml` | 205 | = legacy/es (0 diferencias) |

- **en vs es**: es tiene 53 rutas más; en tiene 1 propia (`PLAYTIME.RESULT`, sustituida en es por `RESULT-HEADER/…/RESULT-FOOTER`). Faltan en en: `ANNOUNCE.*`, `ALERT.*`, `GLIST.*`, `SEEN.*` (24), `STAFFCHAT/ADMINCHAT.FORMAT-PLAIN`, `ADMINCHAT.REQUEST-{ALERT,SENT}`, `REPORT.{TOGGLE-USAGE,TITLE-MAIN,TITLE-SUB,TITLE-MUTED,TITLE-UNMUTED}`, `PLAYTIME.{RESULT-HEADER,SERVERS-LABEL,SERVER-ENTRY,SERVERS-EMPTY,RESULT-FOOTER}`, `FRIENDS.{USAGE-REQUEST,USAGE-REQUEST-CANCEL,REQUEST-CANCELLED}`, `QUEUE.ACTIONBAR`.
- **Bug funcional (LANGUAGE: EN)**: 34 claves que el código pide literalmente no existen en `en.yml` → el jugador ve `seen.header`, `glist.footer`, `report.title_main`… (extracción de `raw("…")` literales; lista completa reproducible con el diff anterior).
- **modern vs legacy (es)**: legacy carece de 70 claves usadas (`GENERAL.HELP-*`, `ALERTS.{SERVER-UP,SERVER-DOWN,COMMAND-BLOCKED,MAINTENANCE-ATTEMPT,ALT-EVASION}`, `STREAM.CLICK-*`, `ANALYTICS.VERSIONS-*`, `FRIENDS.*-BUTTON/*-HOVER/REQUESTS-*`, `DONATIONS.BAR-*`, `ALTS.*`, `ACCOUNT-TRANSFER.*`, `SEEN.*`, `GLIST.*`…) → caen a modern (funciona, pero legacy deja de ser diseño propio). Legacy tiene 2 propias: `STREAM.BROADCAST-LEGACY`, `DONATIONS.GG-TRIGGER`.
- Claves de `es.yml` sin referencia literal en código (candidatas muertas, NO VERIFICADO acceso dinámico): `FRIENDS.{ACCEPTED-NOTIFY,REQUEST-BUTTONS,USAGE}`, `GENERAL.UNKNOWN-COMMAND`, `HELPOP.NOTIFY`, `LOBBY.{FALLBACK,FALLBACK-DOWN,TITLE-LOBBY,SUBTITLE-LOBBY}`, `REPORT.{OFFLINE-OK,REASONS-HEADER}`, `SEEN.STATUS-BANNED`, `STAFFCHAT.USAGE` (13).
- Código usa claves en minúscula snake (`"helpop.not_found"`) que funcionan solo por la normalización de `YamlConfig`.
- Separadores: usa `# — FEATURE ———` (28 en es) ✔; cabecera "Messages Modern ES · 1.16+" ✔. Placeholders `<player>` (73 distintos) sin bloque `Placeholder's ↓`.

## 4. Core (Bukkit)

### 4.1 `settings.yml` (`IoriCorePlugin.settings()`) — 27 lectores

| Clave | Tipo | Default código | ¿Leída? / Ruta |
|---|---|---|---|
| `SETTINGS.LANGUAGE` | S | ES | ✔ `IoriCorePlugin.java:107` |
| `SETTINGS.DEBUG` | B | — | ✘ **muerta** |
| `SETTINGS.SERVER-NAME` | S | ⚠ 4 defaults: `core` (`SoftRedisPublisher.java:43`), `lobby` (`IpCommand.java:61`, `JoinMotdService.java:73`), `lobby-1` (`RankService.java:44`), `server` (`ChatLogService.java:57`) | ✔ |
| `SETTINGS.LEGACY-MAX-VERSION` / `FALLBACK-VERSION` | S | 1.12.2 / 1.8.9 | ✔ `IoriCorePlugin.java:204-228`; compat `LEGACY-PROTOCOL-MAX`, `FALLBACK-PROTOCOL` leídas y ausentes |
| `STORAGE.BACKEND` | S `mysql`/`h2`/`mongo` | h2 | ✔ `CoreSqlStorage.java:36` |
| `STORAGE.MYSQL.{HOST,PORT,DATABASE,USER,PASSWORD,MAX-POOL-SIZE}` | … | localhost,3306,**ioricore**,ioricore,"",5 | ✔ `CoreSqlStorage.java:97-102` (sin `SSL`) |
| `STORAGE.H2.FILE` / `STORAGE.MONGO.{URI,DATABASE}` | S | ioricore / … | ✔ (H2 pool 5 hardcoded `CoreSqlStorage.java:90`) |
| `REDIS.{ENABLED,HOST,PORT,CHANNEL}` | B,S,I,S | false,127.0.0.1,6379,ioriproxy | ✔ `SoftRedisPublisher.java:29-43` — **sin AUTH/SSL/DATABASE/TIMEOUT** |
| `MODULES.*` (28) | B | según módulo (off: ECONOMY, CHAT-GAMES, GENDER, TAGS, COINSHOP, BOSSBAR-TIPS, EASTER-EGGS, STAFF-AUTH) | ✔ 27; **`MODULES.DONOR-JOIN` muerta** (no existe módulo) |
| `JOIN.{STATUS-LINE,STAFF-PERMISSION,SEE-PERMISSION}` | B,S,S | true,`ioricore.staff`,`ioricore.join.brand` | ✔ `JoinStatusListener.java:25-27` (sin `MODULES.JOIN`) |
| `PERSISTENCE.{HOMES,WARPS,BACK,STATS,KITS,ECONOMY,TAGS}` | B | true | ✔; **`PERSISTENCE.INVENTORY` muerta** |
| `ECONOMY.{STORAGE,STARTING-BALANCE,PRIMARY-NAME.{ES,EN},CREDITS-NAME.*,GEMS-NAME.*}` | S,D,S | YAML,0,Royales/Créditos/Gemas | ✔ `EconomyService.java:30-121` (texto por idioma en settings) |
| `TAGS: {}`, `COINSHOP: {}`, `CHAT-GAMES: {}` | — | — | Secciones vacías, nada se lee (placeholders de documentación) |
| `BOSSBAR-TIPS.{INTERVAL-SECONDS,STAY-SECONDS}` | I | 60/8 | ✔ |
| `ESSENTIALS.{TPA-TIMEOUT-SECONDS,MAX-HOMES,SPAWN-ON-JOIN,SPAWN-ON-DEATH,NEAR-RADIUS}` | I/B/D | 60,3,false,true,100 | ✔ |
| `SCOREBOARD.{UPDATE-TICKS,DISABLE-LUNAR-NUMBERS}` · `TAB.UPDATE-TICKS` | I,B | 40,true · 40 | ✔ → [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) |
| `WORLD.DISABLE-*` (9) | B | false | ✔ `WorldManagementListener.java:34-42` |
| `STAFF-MODE.{FLIGHT,NIGHT-VISION,INVULNERABLE,FREEZE-ACTIONBAR-TICKS}` | B,I | true,20 | ✔ → [STAFFMODE](STAFFMODE.md) |
| `STAFF-AUTH.{STAFF-PERMISSION,GRACE-TICKS,ENFORCE-WITHOUT-PROXY,RESTRICT-ON-DENY}` | S,I,B,B | `ioriproxy.staff`,40,false,false | ✔ |
| `RANKS.STORAGE` | S YAML/SQL | YAML | ✔ `RankService.java:42` |
| `CHAT-LOGS.{PLAYER-FOLDER,MAX-LINES,PURGE-DAYS}` | S,I,I | NAME,100000,60 | ✔ (purga = stub según comentario) |
| `EASTER-EGGS.{CLICK-RADIUS,AUTO-PLACE,TITLE-FADE-IN,TITLE-STAY,TITLE-FADE-OUT}` | D,B,I | 2.5,false,10,50,10 | ✔ `EasterEggService.java:51-57` |
| `JOIN-MOTD.{DELAY-TICKS,PICTURE-LOGIN,GIVE-HEAD}` | I,B,B | 20,true,false | ✔ |

### 4.2 Resto de ficheros Core

| Fichero | Lector | Estado claves |
|---|---|---|
| `commands.yml` | `IoriCorePlugin.java:101` carga y **nadie lo lee** (0 refs a `commands`) | Todo muerto. Aliases reales en `plugin.yml` (70 comandos vs 69 en commands.yml). Comentario "Stub" lo admite |
| `permissions.yml` | `IoriCorePlugin.java:102` carga y **nadie lo lee** | Todo muerto. 221 literales `"ioricore.*"/"ioriproxy.*"` hardcodeados en Java; `plugin.yml permissions:` (`:271`) es el registro real |
| `plugin.yml` | Bukkit | Descriptor; duplica aliases/perm de commands.yml/permissions.yml. `api-version: "1.13"` |
| `ranks.yml` | `RankService.java:180-243` | `RANKS.<id>.{ID,NAME,DISPLAY-NAME,PREFIX,SUFFIX,PRIORITY,TYPE,SCOPE,COLOR,ITALIC,BOLD,STAFF,MEDIA,ADMIN,OP,VISIBLE,DEFAULT,PARENTS,PERMISSIONS,PERMISSION-COUNT}` ✔. Se reescribe con `yaml.save` → pierde comentarios. → [RANKS](RANKS.md) |
| `sounds.yml` | `SoundService.java:33-72` | `SOUNDS.<KEY>.{NAMES|NAME,VOLUME,PITCH}` ✔ (acepta minúsculas también) |
| `data/tags.yml` | `TagService.java:48-56` | `TAGS.<id>.{DISPLAY,PERMISSION,EMOJI,CHAT}` ✔. `DISPLAY` = texto ES no localizado |
| `data/coinshop.yml` | `CoinshopService.java:52-60` | `OFFERS.<id>.{TYPE,TARGET,PRICE,MATERIAL,DATA}` ✔; `STUB` leído y ausente |
| `data/chatgames.yml` | `ChatGamesService.java:56-65` | `INTERVAL-SECONDS,TIMEOUT-SECONDS,MIN-PLAYERS,WINNER-COOLDOWN-SECONDS,REWARD.{MONEY,COMMANDS},QUESTIONS[].{QUESTION,ANSWERS|ANSWER}` ✔. Preguntas solo ES |
| `data/easter-eggs.yml` | `EasterEggService.java:273-278` | `EGGS.<id>.{ENABLED,WORLD,X,Y,Z,DISPLAY,TEXTURE,ARMOR-STAND,TITLE,SUBTITLE,REWARD-COMMAND}` ✔ |
| `chat/formats/{en,es}.yml` (+`legacy/`) | `CoreLanguage.java:180-181`, `ChatFormatService.java:54` | `FORMATS.<grupo>.{PERMISSION,PARTS.<part>.{TEXT,HOVER,CLICK.{TYPE,VALUE}}}`. en=es (68 rutas); legacy 39 (sin HOVER/CLICK, coherente con 1.8). Claves de grupo/parte en **minúscula** (`owner`, `tag`). Funcional (PERMISSION, URLs `store.example.com`) duplicado ×4 |
| `menus/{modern,legacy}/*.yml` (11+11) | `MenuConfig.java:26-138` | `ROWS, BORDER-PATTERN, SOUND.CLICK, FILLER.*, ACCENT-FILLER.*, CONTENT-SLOTS, SLOTS.*, ITEMS.<k>.{MATERIAL,DATA}` ✔. **Sin `TITLE`** en ningún YAML (cae a fallback/messages). → [MENUS](MENUS.md) |

### 4.3 Core `messages/` (CoreLanguage)

| Fichero | Rutas | Diferencias |
|---|---|---|
| `messages/es.yml` | 565 | — |
| `messages/en.yml` | 565 | 0 |
| `messages/legacy/es.yml` | 565 | 0 claves; texto difiere solo en 17 líneas (cabecera, 3 comentarios, `SCOREBOARD` lines) |
| `messages/legacy/en.yml` | 565 | 0 |

- Todas las claves literales que pide el código existen (151 extraídas; resto dinámicas por prefijo `COINSHOP.MENU.`, `SETTINGS.LANG.`, `RANKS.MENU.`, `STAFF-MODE.HOTBAR.`).
- Muertas: `JOIN.STATUS-{BEDROCK,PREMIUM,CRACKED}` (marcadas "Legacy keys (compat)", `es.yml:254-257`); candidatas `GENERAL.{FEATURE-DISABLED,RELOAD-SUCCESS,UNKNOWN-COMMAND}` (NO VERIFICADO dinámico).
- ⚠ Legacy es copia casi literal de modern → no hay diseño legacy real (contradice decisión 2026-11-03 de sistemas independientes).
- Placeholders en `{minúscula}` (`{player}`, `{online}`) y `%vault_prefix%`; `yaml-comments.md` propone `{PLAYERNAME}` UPPERCASE; `easter-eggs.yml` usa `{PLAYER}`.

## 5. Duplicación Proxy ↔ Core

| Concepto | Proxy | Core | Problema |
|---|---|---|---|
| Versión legacy | `settings.yml SETTINGS.LEGACY-MAX-VERSION` | `settings.yml SETTINGS.LEGACY-MAX-VERSION` | Sincronía manual; nada lo propaga |
| Idioma | `SETTINGS.LANGUAGE` | `SETTINGS.LANGUAGE` | Idem (por servidor es aceptable) |
| BD tipo | `database.yml DATABASE: H2/MYSQL/MONGODB/NONE` | `STORAGE.BACKEND: h2/mysql/mongo` | Vocabulario distinto |
| BD credenciales | `MYSQL.USERNAME`, db `ioriproxy`, pool fijo 10, `SSL` | `MYSQL.USER`, db `ioricore`, pool 5 config, sin SSL | Defaults apuntan a **BDs distintas** pese a esquema compartido `ip_/ic_` (ver [INTEGRATIONS](INTEGRATIONS.md), skill storage) |
| Redis | `redis.yml`: ENABLED, HOST, PORT, DATABASE, TIMEOUT-MS, SSL, KEY-PREFIX, CHANNEL, AUTHENTICATION | `settings.yml REDIS`: ENABLED, HOST, PORT, CHANNEL | Core no puede conectar a Redis con password/SSL/db≠0; `CHANNEL` debe coincidir a mano |
| Staff-auth | `STAFF-AUTH.STAFF-PERMISSION`, `GRACE-SECONDS: 2` | `STAFF-AUTH.STAFF-PERMISSION`, `GRACE-TICKS: 40` | Mismo valor en dos unidades; comentario proxy lo admite (`settings.yml:163`) |
| Nodo staff | `ioriproxy.staff` | `ioriproxy.staff` (auth) **y** `ioricore.staff` (JOIN) | Dos nodos "staff" |
| Módulos | `MODULES.{PUNISHMENTS,PLUGIN-MESSAGING,STAFF-AUTH}` | idem | Toggles independientes; desalineación posible (proxy true / core STAFF-AUTH false por defecto) |
| commands/permissions | ficheros leídos | ficheros **no leídos** + plugin.yml | Core: triple fuente (yml muerto, plugin.yml, literales Java) |
| Sonidos | inline `SOUND.{LEGACY,MODERN}` en settings/donations | `sounds.yml` con `NAMES` fallback | Dos modelos para lo mismo |

## 6. Hardcode en Java que debería ser config

| Valor | Ubicación | Propuesta |
|---|---|---|
| Hikari proxy 10/2/15s/30min | `DatabaseConfig.java:7-10` | `database.yml POOL.*` |
| Hikari H2 core 5 | `CoreSqlStorage.java:90` | `STORAGE.H2.POOL` o fijo documentado |
| Timezone `UTC` | `SettingsConfig.java:38` | `SETTINGS.TIMEZONE` |
| Analytics tick 60 s / Server-health 15 s | `AnalyticsService.java:37`, `ServerHealthMonitor.java:28` | `ANALYTICS.TICK-SECONDS`, `SERVER-HEALTH.INTERVAL-SECONDS` |
| Redis reconexión 1/3 s | `JedisRedisService.java:113,138` | `redis.yml RECONNECT-MS` |
| Title times 250ms/2s/250ms (Velocity) | `VelocityPlatform.java:249` | por mensaje o `settings.yml TITLES` |
| Flag blacklist `-s` | `BlacklistConfig.java:5` | aceptable fijo |
| Prefijo consola `[IoriProxy]` | `ProxyLogger.java:8` | brand (A15) |
| Lista canales/embeds Discord y nombres webhook | `DiscordConfig.java:38-47,87-97`, `WebhookRegistry.java:18-25` | iterar secciones del YAML |
| IDs de menú | `MenuConfig.java:26-36` | descubrir ficheros de `menus/` |
| ConfirmationMenu slots 11/13/15/22, colores 5/14, size 27 | `ConfirmationMenu.java:16-24` | `menus/*/confirm.yml` |
| Title defaults 10/40/10 | `TitleService.java:11-13` | `settings.yml TITLES` |
| 221 nodos de permiso literales (core) | `bukkit/**` | cablear `permissions.yml` o eliminarlo |

## 7. Calidad y estilo vs referencia (IoriCTW + reglas)

Referencia IoriCTW (`.cursor/_refs/IoriCTW/plugin/src/main/resources/`, propio): cabecera `# *** … ***`, separador `# ────`, `# ~` explicativo, sub-separador `# ──── GAME · JOIN`. **Pero** `language/kits/kits-es.yml` usa claves **PascalCase** (`EnabledKits`, `DisplayItem`, `BaseLore`, `Material`) e item+texto en el mismo fichero; `messages-*.yml` y `settings.yml` sí usan UPPERCASE kebab. Contradicción interna de la referencia vs regla 40 ("claves UPPERCASE kebab").

| Criterio | Proxy | Core |
|---|---|---|
| Cabecera `# ***` | ✔ todos | ✔ todos (menús con ancho irregular) |
| Separador `# ────` | ✔ funcionales; messages usan `# — X ———` (conforme a yaml-comments) | Parcial: `settings.yml` sin separadores en `SETTINGS/STORAGE/MODULES` (`:5-70`); menús/data usan `# ── X ──` |
| `# ~` | ✔ salvo `motd.yml`, `messages/legacy/*` (0) | ✔ salvo `plugin.yml` |
| UPPERCASE kebab | ✔ (y el lector normaliza) | ✔ excepto ids (`default`, `heart`, `owner`, `tag`) — aceptables como ids, pero `chat/formats` mezcla `PARTS.tag.TEXT` |
| Bloque `Placeholder's ↓` | ✘ 0 ficheros | ✘ 0 ficheros (se usa `# ~ Placeholders:` inline en 14 ficheros) |
| Sintaxis placeholder | `<x>` messages · `%x%` webhooks/discord/ggwave · `<time>` maintenance | `{x}` messages · `{X}` easter-eggs · `%x%` PAPI/vault |
| Idioma comentarios | Mezcla ES/EN (redis/fallback EN, resto ES; velocity≠bungee) | Mezcla ES/EN (settings EN+ES, easter-eggs EN) |
| Texto en fichero funcional | `maintenance.yml` (MOTD/kick/hover ES), `motd.yml`, `donations.yml GGWAVE.*-FORMAT` | `settings.yml ECONOMY.*-NAME`, `data/tags.yml DISPLAY`, `data/chatgames.yml` |
| Brand | IoriProxy / IoriLabs / RoyalCTW mezclados | IoriCore / RoyalCTW |

## 8. Contradicción de layout (OPEN-QUESTIONS A5)

| Elemento | Intención (DECISIONS.md:12-14, master prompt) | Código actual |
|---|---|---|
| Menús | `messages/menus/{en,es}.yml` (textos de menú por idioma) | `menus/{modern,legacy}/<menu>.yml` (layout/materiales; textos en `messages/{lang}.yml`) — `MenuConfig.java:49` |
| Chat formats | `messages/formats/{en,es}.yml` + `messages/legacy/formats/{en,es}.yml` | `chat/formats/{en,es}.yml` + `chat/formats/legacy/` — `CoreLanguage.java:180-181` |
| Easter eggs | `easter-eggs.yml` en raíz + settings `EASTER-EGGS:` | `data/easter-eggs.yml` (+ `data/{tags,coinshop,chatgames}`) — `IoriCorePlugin.java:91` |
| Legacy messages | regla 60/design: `messages/{lang}-legacy.yml` (A6) | `messages/legacy/{lang}.yml` |
| Selector modern/legacy de menús | Decisión 2026-11-03: Modern **1.16+** / Legacy **1.8.9**, sistemas independientes | `MenuConfig.serverLegacy()` = existe `GRAY_STAINED_GLASS_PANE` (1.13+) por **servidor**; comentarios dicen "Modern (1.13+)"; ficheros modern/legacy difieren solo en `MATERIAL/DATA` (6–22 líneas de diff) |

## 9. Estado para FEATURE-STATUS

| Feature | Módulo | Estado | Evidencia |
|---|---|---|---|
| Carga config proxy (11 + webhooks) | proxy | IMPLEMENTED_UNTESTED | `ConfigManager.java:13,105-131` |
| Normalización claves proxy | proxy | IMPLEMENTED_UNTESTED | `YamlConfig.java:257` |
| Merge defaults en upgrade (proxy) | proxy | NOT_IMPLEMENTED | `YamlConfig.java:72` |
| i18n proxy en/es | proxy | PARTIAL | en sin 34 claves usadas |
| Messages legacy proxy | proxy | PARTIAL | 70 claves caen a modern |
| Core settings | core | IMPLEMENTED_UNTESTED | 27 lectores, 3 muertas |
| Core commands.yml / permissions.yml | core | STUB | cargados, 0 lecturas |
| Core i18n en/es + legacy | core | IMPLEMENTED_UNTESTED | 565=565=565=565 |
| Menús YAML modern/legacy | core | PARTIAL | layout ✔, legacy = copia, sin TITLE |
| Config Redis core | core | PARTIAL | sin auth/ssl/db |

## 10. Propuesta de estructura final (PROPUESTA — no migrada, requiere aprobación)

### 10.1 Principios
1. **Funcional ≠ texto**: ningún texto visible al jugador en ficheros funcionales; todo texto en `messages/` (o `lang/`) por idioma. Funcional nunca duplicado por idioma.
2. **Modern ≠ legacy** solo donde el diseño difiere: textos (`messages/legacy/`) y menús (`menus/legacy/` con layout propio 1.8.9, no solo materiales). Corte por **protocolo del jugador** (`LEGACY-MAX-VERSION`) en texto; para menús, decidir si por jugador o por servidor (ver decisiones).
3. **Secretos aparte**: `secrets.yml` (o sección única) para TOKEN Discord, store SECRETs, contraseñas DB/Redis — fácil de excluir de backups/commits.
4. **Una fuente compartida** para lo que debe coincidir entre proxy y core (`network.yml` con mismas claves en ambos: storage, redis, LEGACY-MAX-VERSION, nodo staff).
5. Velocity/Bungee: resources **generados desde un único origen** (p.ej. `common/src/main/resources/proxy/` copiado por Gradle) en vez de 23×2 ficheros.

### 10.2 Árbol propuesto

```
proxy (Velocity y Bungee, mismo contenido; bungee añade plugin.yml)
├── settings.yml        # SETTINGS, MODULES, COOLDOWNS, NOTIFICATIONS, FRIENDS, COMMAND-WHITELIST,
│                       # ANNOUNCEMENTS (solo timing/sonido), ANALYTICS, STAFF-AUTH, DISCORD.BOT (sin TOKEN)
├── network.yml         # COMPARTIDO con core: LEGACY-MAX-VERSION, STORAGE (MariaDB/H2 + POOL), REDIS (+AUTH/SSL)
├── secrets.yml         # DISCORD.TOKEN, STORES.*.SECRET, (opcional) contraseñas
├── commands.yml        # labels/aliases
├── permissions.yml     # nodos (única fuente; quitar *-PERMISSION de queue/maintenance/settings)
├── motd.yml  fallback.yml  queue.yml  maintenance.yml(sin textos)  donations.yml(sin formatos)
├── discord/            # embeds bot + webhooks con UN esquema (EMBED.AUTHOR.NAME…)
│   ├── bot.yml
│   └── webhooks/{reports,request,maintenance,stream,donations,blacklist,status,staff-auth}.yml
└── messages/
    ├── en.yml  es.yml              # modern 1.16+ (incluye MAINTENANCE.MOTD/KICK, MOTD textos, GGWAVE formatos)
    └── legacy/en.yml  legacy/es.yml

core (Bukkit)
├── plugin.yml
├── settings.yml        # SETTINGS (SERVER-NAME obligatorio, sin DEBUG muerto), MODULES, JOIN, PERSISTENCE,
│                       # ECONOMY (sin nombres de moneda → messages), ESSENTIALS, SCOREBOARD, TAB, WORLD,
│                       # STAFF-MODE, STAFF-AUTH, RANKS, CHAT-LOGS, EASTER-EGGS, JOIN-MOTD, BOSSBAR-TIPS
├── network.yml         # idéntico al del proxy
├── permissions.yml     # cableado de verdad o eliminado (decisión)
├── sounds.yml
├── ranks.yml
├── data/               # definiciones funcionales (ids, precios, coords) SIN texto
│   ├── tags.yml  coinshop.yml  chatgames.yml(preguntas → messages/chatgames/{lang}.yml)  easter-eggs.yml
├── chat/formats.yml    # funcional: grupos, PERMISSION, CLICK URLs (una sola vez)
├── menus/
│   ├── modern/<menu>.yml   # layout 1.16+ (ROWS, SLOTS, ITEMS)
│   └── legacy/<menu>.yml   # layout propio 1.8.9
└── messages/
    ├── en.yml  es.yml                      # modern
    ├── legacy/en.yml  legacy/es.yml
    ├── menus/{en,es}.yml  menus/legacy/…   # títulos + nombres/lore (estilo kits-es: bloque por item)
    └── formats/{en,es}.yml  formats/legacy/…  # solo TEXT/HOVER de chat formats
```
Esto ratifica DECISIONS para `messages/menus/` y `messages/formats/` **como textos**, y conserva `menus/{modern,legacy}/`, `chat/` y `data/` del código **como funcional** — resuelve A5 sin elegir uno de los dos.

### 10.3 Convención de estilo obligatoria
- Cabecera:
  ```yaml
  # *************************************
  # *     <Plugin> | <Fichero> (<ES|EN>)   *
  # *************************************
  ```
  `<Plugin>` = un único brand por plataforma (decidir A15).
- Sección: `# ────────────────────────────` / `# NOMBRE` / `# ────────────────────────────`; sub-sección `# ──── PADRE · HIJO`; en messages y commands `# — /cmd ———` o `# — FEATURE ———`.
- `# ~` = qué hace el campo + valores aceptados (`# ~ H2 | MARIADB`); una línea por campo no obvio. Comentarios en **un idioma** (ES) en todos los ficheros funcionales.
- Claves **UPPERCASE kebab** siempre (también en Core, aunque Bukkit sea case-sensitive); ids de usuario (`heart`, `owner`) en minúscula permitidos solo como clave de mapa.
- Placeholders: una sintaxis por plataforma documentada (propuesta: `{UPPER}` en todo texto propio; `%papi%` solo para PlaceholderAPI). Bloque obligatorio al inicio de cada fichero de texto o sección:
  ```yaml
  #  Placeholder's ↓
  #   · {PLAYER} {SERVER} {REASON}
  #  Also supports PlaceholderAPI if installed --
  ```
- Menús: un bloque por item (como `kits-es.yml`: `MATERIAL/DATA/SLOT` en `menus/`, `NAME/LORE` en `messages/menus/`), `TITLE` obligatorio en messages.
- Prohibido: texto visible en ficheros funcionales; secretos fuera de `secrets.yml`; claves sin lector (añadir test de "claves muertas" en CI con el mismo diff de esta auditoría).
- Loader: merge de defaults del jar también en proxy (como `CoreLanguage`) y log de claves desconocidas.
