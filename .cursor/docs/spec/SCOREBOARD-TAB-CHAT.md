# SCOREBOARD · TAB · CHAT · NAMETAGS · JOIN MOTD · BOSSBAR TIPS — spec/auditoría (2026-11-03)

> Fuente de **estado real** = código. Intención = `.cursor/docs/memory/DESIGN_BRIEF.md`, `memory/EXTRAS_BACKLOG.md`, `plan/MODULE-INVENTORY.md` §3/§8/§9/§13.
> Rutas Bukkit abreviadas: `B/` = `bukkit/src/main/java/us/iorilabs/bukkit/`, `BR/` = `bukkit/src/main/resources/`, `V/` = `velocity/src/main/java/us/iorilabs/proxy/` (Bungee idéntico salvo adaptadores, ver brief §8).
> Cruces: permisos → `PERMISSIONS.md` · comandos → `COMMANDS.md` · ranks/prefijos → `RANKS.md` · staff nametag → `STAFFMODE.md` · hooks → `INTEGRATIONS.md` · YAML → `CONFIGURATIONS.md` · MOTD lista servidores/staffchat → `PROXY-MODULES.md`.

---

## 0. Resumen de estado

| Sistema | Módulo / toggle | ESTADO | Evidencia corta |
|---|---|---|---|
| Scoreboard sidebar | `ScoreboardModule` · `MODULES.SCOREBOARD: true` | `IMPLEMENTED_UNTESTED` | registrado `B/IoriCorePlugin.java:166`; timer + listener + `/scoreboard`; sin captura RESULT-OK |
| TAB header/footer | `TabModule` · `MODULES.TAB: true` | `PARTIAL` | solo header/footer (`B/modules/tab/TabService.java:48-63`); sin nombres/sorting/columnas; no-op en 1.12 |
| TAB sorting / player list names por rango | — | `NOT_IMPLEMENTED` | no hay equipos de orden ni `setPlayerListName` por rango |
| Nametags por rango | — | `NOT_IMPLEMENTED` | solo team `icStaff` de StaffMode (`B/modules/staffmode/StaffModeService.java:36,343-381`) |
| Chat format (parts, permiso, Vault/LP, tag, PAPI) | `ChatFormatModule` · `MODULES.CHAT-FORMAT: true` | `PARTIAL` | `HOVER`/`CLICK` del YAML ignorados (`ChatFormatService.java:148-156` solo lee `TEXT`) |
| Chat hover/click | — | `NOT_IMPLEMENTED` | envío `receiver.sendMessage(String)` (`ChatFormatService.java:127`) |
| Tags (equip + fragmento chat) | `TagsModule` · `MODULES.TAGS: false` | `IMPLEMENTED_UNTESTED` | `/tag`, `data/tags.yml`, `ChatFormatService.resolveTag` |
| Join MOTD texto modern/legacy | `JoinMotdModule` · `MODULES.JOIN-MOTD: true` | `IMPLEMENTED_UNTESTED` | `B/modules/joinmotd/JoinMotdService.java:273-292` |
| Join MOTD cara PictureLogin | idem · `JOIN-MOTD.PICTURE-LOGIN: true` | `STUB` | envía la **URL** como texto (`JoinMotdService.java:288,300`; `messages/es.yml JOIN-MOTD.PICTURE`) |
| Join status staff (Premium/Bedrock/Cracked) | `JoinStatusListener` · `JOIN.STATUS-LINE` | `IMPLEMENTED_UNTESTED` | registrado sin módulo `B/IoriCorePlugin.java:173` |
| Bossbar tips | `BossbarTipsModule` · `MODULES.BOSSBAR-TIPS: false` | `IMPLEMENTED_UNTESTED` | bar global reflexiva + fallback title (`BossbarTipsService.java:341-387`) |
| `%ioriproxy_*%` placeholders | proxy `PlaceholderService` · módulo `placeholders` | `PARTIAL` | solo consumidos por `AnnouncementService` (`V/IoriProxy.java:346-351`); no expuestos a Bukkit/PAPI |
| Staffchat red | proxy `StaffChatService` | `IMPLEMENTED_UNTESTED` (detalle en PROXY-MODULES) | capturas propias staffchat en IMAGE-INDEX §"Salidas proxy" (no verificadas aquí) |
| `PlayerHeadAscii` (common) | — | `STUB` | paleta pseudo-aleatoria por hash del nombre, **no lee la skin** (`common/.../utilities/PlayerHeadAscii.java:38-61`) |

Ninguna captura `RESULT-OK` de scoreboard/TAB/chat/join MOTD propios en `IMAGE-INDEX.md` → nada es `IMPLEMENTED_AND_TESTED`.

---

## 1. Selección Modern / Legacy (común a todos los sistemas Bukkit)

| Pieza | Comportamiento | Ref |
|---|---|---|
| Detección protocolo receptor | `ProtocolProbe.protocol(player, fallback)`: reflexión `com.viaversion.viaversion.api.Via.getAPI().getPlayerVersion(Player)` (resuelta en `static {}`) | `B/util/ProtocolProbe.java:62-85` |
| Sin ViaVersion en backend | devuelve `fallback` | idem :84 |
| Fallback A (scoreboard, TAB, joinmotd, `CoreLanguage.rawFor/listFor`) | `serverFallbackProtocol()` = versión del **servidor** (`Bukkit.getBukkitVersion()`) | `ProtocolProbe.java:87-93` |
| Fallback B (chat format) | `plugin.fallbackProtocol()` = `SETTINGS.FALLBACK-VERSION` (default `1.8.9` → 47) | `B/IoriCorePlugin.java:216-226`, `ChatFormatService.java:116,257` |
| Umbral legacy | `SETTINGS.LEGACY-MAX-VERSION: 1.12.2` → 340; `protocol <= max` ⇒ legacy | `B/IoriCorePlugin.java:203-214`, `BR/settings.yml:11` |
| Bolsas de texto | `messages/{lang}.yml` vs `messages/legacy/{lang}.yml`; `chat/formats/{lang}.yml` vs `chat/formats/legacy/{lang}.yml`; clave ausente en legacy → cae a modern | `B/language/CoreLanguage.java:54-59,73-78,113-118,143-148,179-182` |
| Colores | solo `ChatColor.translateAlternateColorCodes('&')` → **sin hex** (`&#rrggbb`/`<#..>`) en Bukkit | `CoreLanguage.java:~283` (`color()`), `ChatFormatService.java:273-276` |
| Proxy | protocolo nativo `player.getProtocolVersion()`; `LEGACY-MAX-VERSION` propio | `V/velocity/impl/listener/VelocityPresenceListener.java:64`, `velocity/src/main/resources/settings.yml:12` |

Problemas:
- **Fallback inconsistente**: en un backend 1.20 sin Via, scoreboard/TAB usan modern y el chat usa legacy (fallback 1.8.9).
- **Via sólo en el proxy** (setup típico Velocity+ViaVersion/ViaRewind): el backend no ve la versión real del cliente → todos reciben la variante del fallback. No existe reenvío del protocolo proxy→backend por plugin messaging (búsqueda `protocol` en `B/messaging` vacía).
- `ProtocolProbe` cachea `Via.getAPI()` en carga de clase; si la clase se carga antes de que Via habilite → `null` permanente (softdepend en `BR/plugin.yml` lo mitiga). Compatibilidad con Via 5.x `getPlayerVersion(Player)` (deprecado) **NO VERIFICADO** en runtime.
- `SETTINGS.LEGACY-PROTOCOL-MAX` (número) pasa por `maxProtocol(String)` que acepta enteros → OK (`ProtocolVersions.java:63-65`).

### Límites de protocolo relevantes
| Cliente | Sidebar | Team prefix/suffix | Hex | Bossbar | Header/footer |
|---|---|---|---|---|---|
| 1.8–1.12 | título ≤32, 15 líneas | 16 + 16 (entry ≤40) | no | 1.8 no (1.9+ sí) | sí (packet 1.8+) |
| 1.13–1.15 | título componente | componente (Spigot API: 64 chars) | no | sí | sí |
| 1.16+ | idem | idem | sí | sí | sí |
| 1.20.3+ | `NumberFormat` blank nativo (ocultar números sin Apollo) | — | sí | sí | sí |

---

## 2. Scoreboard (sidebar)

| Aspecto | Real | Ref |
|---|---|---|
| Ficheros | `ScoreboardModule`, `ScoreboardService`, `ScoreboardListener`, `command/ScoreboardCommand` | `B/modules/scoreboard/**` |
| Board | un `Scoreboard` nuevo **por jugador** (`getNewScoreboard`), objective `ioricore` SIDEBAR | `ScoreboardService.java:69-73,140-142` |
| Líneas | `SCOREBOARD.LINES` / `SCOREBOARD.LEGACY.LINES` (máx 15), título `SCOREBOARD.TITLE` / `SCOREBOARD.LEGACY.TITLE` (cortado a 32) | :126-131,144-150 |
| Técnica | 1 team `ic<i>` por línea, entry `ChatColor.values()[i]+RESET`, texto en prefix(16)+suffix(16 con `getLastColors`) → **máx 32 chars visibles por línea en todas las versiones** | :151-176 |
| Frecuencia | `SCOREBOARD.UPDATE-TICKS: 40` (mín 20); además `apply` a 5 y 40 ticks tras join | :38,45,102-106; `BR/settings.yml:156-160` |
| Placeholders | `{player} {online} {max} {server} {ping} {world} %vault_prefix% {vault-prefix}`; cada clave acepta `<k>`, `{k}`, `%k%`; `<prefix>` | `B/util/BoardPlaceholders.java:15-28`, `CoreLanguage.substitute` |
| PAPI | **no** se aplica en scoreboard | — |
| Rango | `vault_prefix`: Vault → LuckPerms → `""`. **No** usa `RankService` (ranks.yml PREFIX) | `BoardPlaceholders.java:30-43` |
| Toggle | `/scoreboard` (aliases `sb`,`sidebar`) perm `ioricore.scoreboard` o `ioricore.admin`; persiste en `PlayerSettings.scoreboard` | `ScoreboardCommand.java:303-326`, `B/modules/settings/PlayerSettings.java:18` |
| Apollo | `SCOREBOARD.DISABLE-LUNAR-NUMBERS: true` → `apollo.setNumbersDisabled` en **cada** apply | :195-200 |
| Anti-flicker | **No**: cada tick `unregister` objective + todos los teams `ic*` y vuelve a crearlos | :133-142 |

Bugs / riesgos:
1. **Flicker y spam de packets**: re-registro completo cada 40 ticks (objective remove/create + 15 teams).
2. **Borra el nametag de StaffMode**: limpia teams cuyo nombre empieza por `ic` (`:136-138`) y StaffMode usa `TEAM_NAME = "icStaff"` (`StaffModeService.java:36`) en los boards por jugador → el prefijo staff desaparece al siguiente tick.
3. Split a 16 chars puede partir un código `§x` (no se comprueba `§` final en `prefix`).
4. Líneas modern también limitadas a 32 chars (no aprovecha 1.13+).
5. `{ping}` usa sólo el campo NMS `ping` (`BoardPlaceholders.java:45-52`); en 1.17+ no existe → **0 ms**. (`ChatFormatService.safePing` sí prueba `getPing()`).
6. `{online}` = jugadores del **backend**, no de la red (HonlyCraft muestra red). `%ioriproxy_online%` no llega al backend.
7. Board propio por jugador sustituye el main scoreboard → teams de otros plugins (nametags LP/TAB) se pierden.

### vs imagen `06-menu-duelos-style.png` (scoreboard HonlyCraft)
Ref: título `HONLYCRAFT「1」`, línea fecha `• ⌚ 17/05/2025 •`, nick con icono, `Rango: ¡Adquiérelo!`, `Nivel: [0★]`, `Etiqueta:`, `Amigos: 0`, `HonlyCoins: 0`, `Conexión: [65ms]`, `Conectados: 1`, `store.honlycraft.us`; iconos unicode por línea, números de score visibles en rojo.
Falta: placeholders fecha/hora, rango con texto "sin rango", nivel, tag equipado (`TagService.fragment`), amigos (proxy), monedas (economy), online de red, iconos (posible ya en YAML), ocultar números (Apollo/1.20.3 NumberFormat), líneas >32 chars en modern, PAPI.

---

## 3. TAB

| Aspecto | Real | Ref |
|---|---|---|
| Qué hace | header/footer: `TAB.HEADER/FOOTER` o `TAB.LEGACY.HEADER/FOOTER` unidos con `\n` | `B/modules/tab/TabService.java:48-63,88-95` |
| Envío | `VersionWrapper.sendTabHeaderFooter` → `FxAwareVersionWrapper` intenta `Player#setPlayerListHeaderFooter(String,String)` (Bukkit 1.13+) → si no, NMS | `B/abstraction/FxAwareVersionWrapper.java:62-67`, `BukkitNativeFx.java:60-82` |
| NMS | sólo `v1_8_R3` implementa (packet `PacketPlayOutPlayerListHeaderFooter` con JSON `{"text":"§.."}`); `v1_12_R1` hereda `default {}` → **no-op en 1.12** | `nms/v1_8_R3/.../VersionWrapper_v1_8_R3.java:155-178`, `nms/api/.../VersionWrapper.java:31` |
| Frecuencia | `TAB.UPDATE-TICKS: 40` (mín 20) + 5/40 ticks tras join; quit → envía vacío | `TabService.java:26,32,70-74`; `TabListener` |
| Placeholders | mismos que scoreboard (`BoardPlaceholders`) | — |
| Player list name | no gestionado por TAB. Otros escriben `setPlayerListName`: nick (`B/modules/essentials/EssentialsState.java:95-102`, sin color, ≤16), vanish (`" "`, `VanishService.java:38,67,113`), staffmode (prefijo) | — |
| Sorting | **NOT_IMPLEMENTED** (no teams de orden `00_owner`…) | — |
| Toggle por jugador | no | — |

### vs imagen `image-1e1ddc2e-*.png` (TAB HonlyCraft)
Ref: header `• HONLYCRAFT NETWORK •` + `¡75% DE DESCUENTO! @ store…`; **4 columnas** con fake players (Tú / Información: Rango, Tag, HonlyCoins, Amigos / Online `1/500 (1 aquí)` / modalidades con estado y conectados / Lista de Staffs / redes); footer `Hub-01 / 160ms / 20.0 TPS` + `1 jugadores conectados (1 aquí)`.
Falta: layout de 80 slots con entradas falsas (requiere packets PlayerInfo por versión o plugin TAB externo), estado/online por servidor (proxy → backend), TPS, ping, online red vs local, lista staff, nombres con prefijo/orden por rango.

---

## 4. Nametags

| Pregunta | Respuesta |
|---|---|
| ¿Existe módulo nametag? | **No** (`MODULE-INVENTORY §8` lo planea bajo `scoreboard/tab/nametag`). |
| Teams existentes | `icStaff` (StaffMode): `setPrefix(STAFF-MODE.NAMETAG-PREFIX truncado)` en main board + boards de cada viewer; `setDisplayName`/`setPlayerListName` | `StaffModeService.java:343-381`; re-sync al join de otros `:221` |
| Conflicto | Scoreboard elimina `icStaff` cada tick (ver §2 bug 2). |
| Lunar | DESIGN_BRIEF: "Lunar [Staff Mode] nametag" → `SoftApolloHook` sólo tiene `setNumbersDisabled` en uso (`B/hook/SoftApolloHook.java:92`); nametag Apollo **NO VERIFICADO**/no usado. |
| Helium (idea, sin licencia) | `nametag/Nametag.java`: team por objetivo `nt_<name>` (≤16) en el board de **cada viewer**, prefix = vanish + modmode + tag símbolo + color rango; refresco en join/rank change/disguise (`NametagListener`). Sin sorting. Solo referencia funcional. |

Diseño mínimo sugerido (no implementado): un único "TeamManager" que posea todos los teams en los boards por jugador (prefijo `ic_nt_`/`ic_sb_` separados), orden por `RankService.weight`, prefix 16 chars legacy / 64 modern, re-sync en join/rank/tag/staffmode.

---

## 5. Chat format

| Aspecto | Real | Ref |
|---|---|---|
| Listener | `AsyncPlayerChatEvent` `HIGHEST, ignoreCancelled` → copia recipients, **cancela** el evento, re-envía por receptor | `B/modules/chatformat/ChatFormatListener.java:19-30` |
| Formato por receptor | `formatsForProtocol(protocolOf(receiver))` → modern/legacy YAML | `ChatFormatService.java:123-134` |
| Selección de formato | primera sección de `FORMATS` (orden YAML) cuyo `PERMISSION` tenga el emisor; si no `default` | :160-173 |
| Formatos shipped | `owner` (`ioricore.chat.format.owner`), `staff` (`ioricore.chat.format.staff`), `default` | `BR/chat/formats/es.yml`, `legacy/es.yml`; `BR/permissions.yml:118-119` |
| Parts | orden fijo `tag, rank, player, separator, message` + extras; sólo `TEXT` | :103,141-156,175-184 |
| Tokens | `{PLAYER} {MESSAGE} {TAG} %tag% %TAG% %vault_prefix% %vault_suffix% %player_name% %player_ping%` + PAPI (reflexión) si presente | :186-199,244-254 |
| Tag | `TagsModule.service().fragment(uuid)` (`CHAT` → `EMOJI` → `DISPLAY`); si la part `tag` no tiene token y hay tag → part = fragmento | :201-218; `B/modules/tags/Tag.java:272-281` |
| Prefijo/sufijo | Vault → LuckPerms → `""`; **no** `RankService` | :220-242 |
| Nick | usa `sender.getName()`; ignora `getDisplayName()` (nick/staff prefix) | :189 |
| Consola | formato con protocolo `MAX_VALUE` | :130 |
| Legacy YAML | rangos hardcode `&4[Dueño] `, `&c[Staff] ` (no prefix LP) | `BR/chat/formats/legacy/es.yml` |

Bugs / riesgos:
1. **Inyección de color**: `stripColor` quita `§` pero luego `colored()` traduce `&` sobre la línea completa incluido el mensaje → cualquier jugador usa `&k`, `&l`, colores (`ChatFormatService.java:122,157`).
2. **Inyección de placeholders**: PAPI se aplica después de insertar `{MESSAGE}` → el jugador puede escribir `%algo%` y se resuelve; también `%vault_prefix%`/`{TAG}` dentro del mensaje se sustituyen (:188-198).
3. `HOVER`/`CLICK` presentes en YAML (`/msg {PLAYER} `, store URL) **nunca** se usan → no se alcanza `15-chat-hover-format.png`.
4. `%ioricore_playtime%` en hovers: no existe `PlaceholderExpansion` en el repo (grep vacío).
5. Al cancelar el evento en `HIGHEST`, `ChatLogListener` (`MONITOR, ignoreCancelled=true`, `B/modules/chatlogs/ChatLogListener.java:21`) **no registra** mensajes con chat format activo; otros plugins de chat/Discord bridge tampoco lo ven.
6. PAPI por reflexión + formateo **por receptor** en hilo async (coste O(receptores)); `Player#hasPermission` async.
7. Ruta real `chat/formats/` vs doc `messages/formats/` (`MODULE-INVENTORY §3`, OPEN-QUESTIONS A5).

### vs imágenes
- `_refs/img/21-chatformat-tag-1.8.png` (legacy): `(♥i♥) [Founder] Tqmass » a` → alcanzable con tag `&8(&f&ki&r&c&l❤&f&ki&r&8)` + prefix LP; falta usar prefix LP en legacy YAML.
- `15-chat-hover-format.png` (modern): hover multilinea (rango+nick+tag, Level, Experience, League, Money, "Click here…") → requiere BaseComponent/Adventure por part + placeholders de stats.
- Geyser "marca Bedrock en format" (EXTRAS_BACKLOG) → no implementado en chat (sí en joinmotd/join status).

---

## 6. Join MOTD (bienvenida en chat)

| Aspecto | Real | Ref |
|---|---|---|
| Disparo | `PlayerJoinEvent MONITOR` + `JOIN-MOTD.DELAY-TICKS: 20` | `B/modules/joinmotd/JoinMotdListener.java:195-199` |
| Texto | `JOIN-MOTD.MODERN` / `JOIN-MOTD.LEGACY` según protocolo; sin centrado (`TextCenter` de common no se usa en Bukkit) | `JoinMotdService.java:276-284` |
| Placeholders | `<player> <online> <server> <brand>(fijo "RoyalCTW") <bedrock>` (+ `<url>` en PICTURE) | :319-328 |
| "PictureLogin" | sólo modern y `JOIN-MOTD.PICTURE-LOGIN: true`: arma `https://crafthead.net/helm/<name>/64` y envía `JOIN-MOTD.PICTURE` = `"<prefix>&eTu cabeza: &b<url>"` → **no dibuja la cara**; tarea async vacía | :286-301 |
| Cabeza item | `JOIN-MOTD.GIVE-HEAD: false` → da skull con `setOwner(name)` | :304-316 |
| Condiciones | no respeta PlayerSettings, staff-auth/login (nLogin) ni "first join" | — |
| Settings | `JOIN-MOTD: DELAY-TICKS 20 · PICTURE-LOGIN true · GIVE-HEAD false` | `BR/settings.yml:238-241` |

### Intención (contradicción doc)
- `MODULE-INVENTORY §9`: legacy = texto centrado; **1.16+ = cara PictureLogin**.
- `DESIGN_BRIEF.md:9`: "Legacy 1.8.9–1.12: … PictureLogin head ASCII/MOTD". → Decidir (ver §11).

### vs imágenes
- `07d-bossbar-discord.png` (= join MOTD PictureLogin HonlyCraft): cara 8×8 a la izquierda (bloques `█` coloreados, hex), texto a la derecha de cada fila: `HONLYCRAFT NETWORK` (gradiente), `(1.8 – 1.21)`, `TIENDA ~ store…`, `DISCORD ~ discord…`, `¡Gracias por jugar aquí! ♥`.
- `image-1d15dd83-*.png` (texto): separadores cian cortos, `Bienvenido, <player> a HonlyCraft`, `Actualmente hay 0 jugadores conectados.`, `Soporte » / Tienda » / Redes »`.
Falta: render real de la cara, texto en paralelo por fila, centrado (`TextCenter`), online de red, hex/gradiente.

### PictureLogin (`.cursor/_refs/PictureLogin/`, **MIT**, © 2015-2018 Nathan Glover; fork "PictureLogin-Continued")
| Paso | Cómo lo hace | Ref |
|---|---|---|
| Fuente skin | HTTP GET a `config.url` (default `https://minepic.org/avatar/8/%uuid%`; cracked `%pname%`) con User-Agent; `ImageIO.read`; si falla → imagen fallback local | `util/PictureUtil.java:27-63`, `resources/config.yml:~165` |
| Hilo | `PictureWrapper` BukkitRunnable, `async: true` por defecto, `message-delay` | `PictureUtil.java:87-102` |
| Redimensionado | 8 filas; ancho por ratio (≤10); `AffineTransformOp` NEAREST_NEIGHBOR | `util/ImageMessage.java:23-49` |
| Carácter | `ImageChar` BLOCK `█` / shades `▓▒░`; transparente → espacio | `com/bobacadodl/imgmessage/ImageChar.java`, `util/ImageMessage.java:15,51-69` |
| Color | **hex** por pixel: `&#rrggbb&█` (formato MineDown) → `MineDown.parse(line)` → `player.spigot().sendMessage` | `util/ImageMessage.java:60,71-73,146-150` |
| Legacy (16 colores) | **no** en este fork: `getClosestChatColor` = `ChatColor.of(color)` (hex); `getDistance` (métrica "redmean") queda sin uso | `com/bobacadodl/imgmessage/ImageMessage.java:95-116` |
| Texto | `appendText` (espacio + línea) o `appendCenteredText` (font widths `DefaultFontInfo`, CENTER_PX=154, resta 8 por la cabeza) | `util/ImageMessage.java:75-144` |
| Destino | `sendOutPictureMessage` → **todos** los online (+ clear-chat opcional); permiso `picturelogin.show`; first-join distinto | `PictureUtil.java:77-85`, `PictureWrapper.java:21-55` |
| Bug ref | `ConfigManager.getMessage`: `count > msg.length` (debería `>=`) → AIOOBE con >8 líneas | `config/ConfigManager.java:~50` |

Reutilizable con atribución MIT (aviso de copyright en el fichero + `THIRD-PARTY`): pipeline resize 8×8 + pixel→`█` hex, `centerMessage`+`DefaultFontInfo` (el método centerMessage acredita a SirSpoodles/Spigot; revisar). Clases `com.bobacadodl.*` son de otro autor sin cabecera de licencia → preferible reescribir. No traer MineDown: emitir hex como `§x§r§r§g§g§b§b` o Adventure. Para legacy hay que añadir cuantización a los 16 colores (distancia redmean) — p.ej. sustituir `PlayerHeadAscii` (hoy falso). Fuente skin: preferir texturas de la sesión (`PlayerProfile`/GameProfile property `textures` → skin PNG, recorte cara 8,8 + capa hat 40,8) o servicio configurable (crafthead/minepic/mc-heads) con caché y timeout; offline/cracked → por nombre o SkinsRestorer.

---

## 7. Join status staff (`JOIN:`)

| Aspecto | Real | Ref |
|---|---|---|
| Qué | al join (+5 ticks) envía a staff `JOIN.STAFF-BRAND` = `&8[&bS&8] &f{player} &8» &8[&eCuenta&8] {account}` | `B/modules/join/JoinStatusListener.java:30-51`, `BR/messages/es.yml JOIN` |
| Cuenta | Geyser Bedrock → nLogin premium/cracked → `online-mode` premium → `UNKNOWN` | :59-76 |
| Ver | `ioricore.admin` · `JOIN.SEE-PERMISSION ioricore.join.brand` · `JOIN.STAFF-PERMISSION ioricore.staff` | :23-28,53-57 |
| Join/quit públicos | no se tocan (`setJoinMessage` no existe en Bukkit; `MODULES.DONOR-JOIN` sin módulo) | grep |

---

## 8. Bossbar tips

| Aspecto | Real | Ref |
|---|---|---|
| Rotación | cada `BOSSBAR-TIPS.INTERVAL-SECONDS: 60` (mín 5), visible `STAY-SECONDS: 8` (mín 2), índice circular | `BossbarTipsService.java:318-357`, `BR/settings.yml:139-141` |
| API | detecta `org.bukkit.boss.BossBar` (servidor 1.9+); **una barra global** AQUA/SOLID progreso 1.0 añadida a todos los online en cada tick | :359-410 |
| Servidor 1.8 | fallback `plugin.titles().send(title, tip, 10, stay, 10)` a todos (title a pantalla completa) | :353-356 |
| Modern/legacy | `language.list("BOSSBAR-TIPS.LINES")` sin protocolo → siempre bolsa modern; sin color/estilo por tip; sin placeholders | :321 |
| Toggle jugador | no (PlayerSettings no tiene bossbar) | — |
| Join durante stay | no recibe la barra hasta el siguiente tick | — |

vs `01b-scoreboard-honly-detail.png` (cian, permanente "¡Estás jugando en mc.honlycraft.us!" con `✖` decorativos) y `07-bossbar-playing.png` (verde "TIP: Si necesitas ayuda usa (/request)", progreso parcial): falta color/estilo/progreso por tip, barra permanente "playing", progreso animado (cuenta atrás), legacy = off según DESIGN_BRIEF (hoy: title).

---

## 9. Proxy: placeholders, presencia, MOTD, chat de red

| Pieza | Real | Ref |
|---|---|---|
| `%ioriproxy_<k>%` | regex `%ioriproxy_([a-z0-9_]+)%` (case-insensitive) | `common/.../placeholders/PlaceholderTokens.java:10` |
| Claves soportadas (exactas) | `online` (proxy local), `proxy_id`, `server`, `playtime_total`, `maintenance` (`on`/`off`); resto → `""`; extra `%luckperms_prefix%` (proxy LP) | `V/placeholders/PlaceholderService.java:20-36` |
| Consumidores | sólo `AnnouncementService` si `modules.placeholders` | `V/IoriProxy.java:346-351` |
| Hacia backend | **no** (sin PAPI expansion, sin plugin message de online de red) | grep |
| Presencia | `PresenceService` Redis (`PLAYER_JOIN/QUIT/SWITCH`), usado por find/friends/staffalerts; no alimenta TAB/scoreboard | `V/server/PresenceService.java:25-58`; `VelocityPresenceListener.java:99-190` |
| MOTD lista servidores | `MotdService` modern/legacy por protocolo del ping + `TextCenter.centerMotd` → ver PROXY-MODULES | `V/motd/MotdService.java:47-74` |
| Staffchat | `STAFFCHAT.FORMAT` `&b[SC] <rank> &c<player> &7(<server>) &8» &f<message>`; perms `ioriproxy.staffchat.send/receive`; Redis cross-proxy; siempre bolsa modern (`raw` sin protocolo) | `V/staffchat/StaffChatService.java:42-45,124-146`, `velocity/src/main/resources/messages/es.yml:248-254`, `LanguageManager.java:93-95` |
| Chat público en proxy | no (sólo listeners staffchat/donation chat) | `V/velocity/impl/listener/Velocity{StaffChat,DonationChat}Listener.java` |
| TAB/prefijos en proxy | no se calcula nada | grep `PlayerListHeader` vacío salvo MOTD/Maintenance |

---

## 10. Utilidades `common`

| Clase | Qué | Uso real |
|---|---|---|
| `ColorUtil` | MiniMessage/Adventure + `&#hex` / `<#hex>` → Component/legacy/ANSI | proxy + `BukkitLogger` (no scoreboard/chat Bukkit) |
| `TextCenter` | centrado por píxeles, `MOTD_WIDTH=45`, reconoce `&#`, `§x` | proxy MOTD/announcements; **no** Bukkit |
| `ProtocolVersions` | mapa versión→protocolo máx (1.8→47 … 1.21.5→770); acepta número; default 340 | Bukkit+proxy |
| `PlayerHeadAscii` | 8×8 `█` con paleta fija elegida por hash del nombre (MiniMessage) — **no es la skin** | sólo `DonationService` proxy (`V/donations/DonationService.java:266`) |

---

## 11. Gap vs intención original

| Intención (doc) | Estado |
|---|---|
| Formats con Hover/Click por part (EXTRAS_BACKLOG, INV §3) | sin hover/click |
| Modern 1.16+ hex/gradients/MiniMessage (DESIGN_BRIEF) | Bukkit sólo `&` |
| Scoreboard Honly-style + Apollo (INV §8) | básico, Apollo numbers sí |
| TAB opcional img 09/`1e1ddc2e` | header/footer simple |
| Nametag (INV §8) | inexistente |
| Join MOTD PictureLogin (INV §9 / DESIGN_BRIEF) | STUB (URL) |
| Bossbar tips default OFF (DESIGN_BRIEF) | cumple OFF; legacy usa titles en vez de off |
| Geyser bedrock en format (EXTRAS) | sólo joinmotd/join status |
| Protocolo del **receptor** vía ViaVersion | sí, con fallbacks inconsistentes; sin protocolo desde proxy |

## 12. Decisiones pendientes
1. Cara PictureLogin: ¿solo 1.16+ (hex) o también legacy cuantizada a 16 colores? (INV §9 vs DESIGN_BRIEF).
2. Fuente de skin: servicio externo (crafthead/minepic) vs texturas de sesión/SkinsRestorer; ¿a quién se envía (sólo al que entra o broadcast como PictureLogin)?
3. Protocolo real del cliente cuando Via está en el proxy: ¿reenviarlo por plugin messaging al backend?
4. TAB HonlyCraft de columnas: ¿implementación propia por packets (NMS por versión) o delegar a plugin TAB externo con PAPI de IoriCore?
5. Fuente de prefijos/orden (chat, scoreboard, nametag, sorting): Vault/LP vs `RankService` propio.
