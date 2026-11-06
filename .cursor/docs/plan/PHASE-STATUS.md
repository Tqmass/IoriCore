# PHASE-STATUS — estado reconstruido (auditoría 2026-11-03)

Fuentes: código actual + `PLAN.md` (H0–H10) + `MODULE-INVENTORY.md` (W1–W3/FINAL) + `memory/PROGRESS.md` (log histórico; versión previa en `.cursor.rar`).
Leyenda: **✓** código completo y compila · **◐** parcial/stubs · **○** no empezado · **?** no verificable sin runtime.
"Compila" ≠ "funciona": no hay tests ni QA en servidor registrada en esta auditoría. Avanzar de fase **requiere OK del usuario**.

| Fase | Objetivo | Estado | Evidencia | Pendiente / bloqueo |
|---|---|---|---|---|
| H0 | Unificación + decisiones | ✓ | `DECISIONS.md` "H0 LOCK" | — |
| H1 | Skeleton monorepo + common | ◐ | Módulos common/api/velocity/bungee/bukkit/nms; build verde | **Sin `proxy-common`** (127 ficheros duplicados); `common` usa paquete `us.iorilabs.proxy.*` (objetivo `us.iorilabs.common`) |
| H2 | NMS / abstraction | ◐ | `VersionWrapper` + `VersionMatcher`; v1_8_R3 y v1_21_R1 reales | v1_12/16/20 stubs; 1.9–11, 1.13–15, 1.17–19 sin mapping; 1.21.x todo → v1_21_R1 (compilado 1.21.1) |
| H3 | Proxy rename + canales | ✓ (casi) | IoriVelo/IoriBungee, id `iorivelo`, canal `ioricore:main` | `@Plugin version` hardcode 1.0.0-SNAPSHOT; permisos siguen `ioriproxy.*`; Redis channel `ioriproxy` |
| H4 | Moderation | ✓? | bukkit `punishment/*` (ban/mute/kick/warn), `ic_punishments`, LiteBans soft, proxy BanCheck fail-open+cache, Redis `PUNISHMENT_*` | Notas no localizadas; DDL duplicada; QA runtime |
| H5 | Staff auth + staffmode | ✓? | proxy `StaffAuthService` (+Approve Discord), bukkit `StaffAuthEnforceModule` (OFF default), `staffmode` (freeze/inspect/vanish/randomtp) | QA runtime; mensaje plugin sin firma |
| H6 | Social / profile / PM | ◐ | Friends + `/seen` (proxy), `/msg` `/r` locales (essentials Core) | Sin `/profile`; sin PM network; sin Profile en Core |
| H7 | Discord JDA | ✓? | `discord/*` en proxy (bot, embeds, sanciones, gateway/bridge) | Flujo `/sync` OTC sin confirmar; QA |
| H8 | Visual | ✓? | scoreboard, tab, joinmotd, bossbartips, chatformat modern/legacy, menús | Nametag parcial; QA visual vs `IMAGE-INDEX.md` |
| H9 | Tags + ChatGames | ◐ | `tags`, `coinshop`, `chatgames` | Ofertas coinshop marcadas `stub` |
| H10 | Prettier + seguridad + QA | ○ | Tooling Prettier existe | Sin tests; strings hardcode en proxy (HelpOp/Stream hover); auditoría seguridad pendiente |

## Oleadas W (inventario)
- **W1/W2/W3**: marcadas ✓ en PROGRESS. Código presente (essentials 54 cmds, FX, chat, hooks, staffmode, settings, skin, ip, joinmotd, ranks editor, economy, tags, scoreboard).
- **FINAL**: PROGRESS (02-11 21:53) dice cerrado; `MODULE-INVENTORY` (02-11 21:32) dice "en curso". Código existe (coinshop, chatgames, bossbar, JDA, kits, SoftMongo) con stubs.

## Stubs conocidos
`GenderModule` · `ChatLogService` purge/cap · `CoinshopOffer.stub` · NMS v1_12/16/20 · rank parents (según PROGRESS antiguo — verificar).

## Siguiente paso propuesto (requiere OK)
1. Decidir contradicciones de `OPEN-QUESTIONS.md` §A.
2. H1-bis: extraer `proxy-common` (mover 127 ficheros idénticos, sin cambiar lógica).
3. H10 parcial: tests mínimos de lógica pura + QA checklist por módulo.

## ROADMAP R1–R9 (desde 2026-11-03, ver ROADMAP.md)
| Fase | Estado | Notas |
|---|---|---|
| R1 Estabilización | ✓ código · build verde 2026-11-03 · sin QA servidor | whitelist SCOPE=PROXY, PROXY-ID, BanCheck TTL + async Bungee, staff auth sin token visible/self-approve, STAFF_AUTH_SESSION al server correcto, i18n en/legacy completo; Core: protecciones staffmode, inventario a disco, freeze completo, vanish único, scoreboard sin flicker, chat saneado, chatlogs, mutes con TTL + /msg /r /me, ranks persistYaml seguro, índices MySQL, títulos ≤32 |
| R2 Infra | ✓ código · build verde 2026-11-03 · sin QA | Runner migraciones común; misma BD `ioricore`; blacklist + staff sessions SQL; Redis Core + CLIENT_PROTOCOL; reload/perms/i18n. **OVERRIDE 03-11 noche:** Mongo restaurado (proxy MONGODB + Core SoftMongo); legacy 1.12.2 hardcoded; Discord JDA → Core (plan); Proxy=IoriProxy |
| R3 Ranks | ✓ código · compile verde · sin QA | Helium-like: grants, /rank|/grant|/ungrant|/grants, Redis RANK/GRANT_UPDATE, display chat/TAB/SB + nametag; LP SYNC stub; menús = R4 |
| R4 Menús | ✓ código · compileJava verde 2026-11-03 · sin QA | `menu.api.yaml` + internals/dynamics; menus modern/legacy + kits/staff-inspect; callers migrados; MenuConfig/Java inventory menus retirados |
| Discord Core | ✓ código · compile verde · sin QA | /sync+/unsync OTC, slash /sync, staff-auth DM Redis dual-path, embeds/BOTS stub; JDA proxy aún presente |
| JoinEffects ref | ✓ | `_refs/JoinEffects` |
| R5 Staffmode | ✓ código · compile verde · sin QA | Hotbar YAML, vanish único, freeze, staff-players/inspect, invsee copia; gaps: Redis sync staff, CPS/follow, punish tips |
| R6 Visual | ✓ código · compile verde · sin QA | Scoreboard estilos lobby/compact anti-flicker; TAB sort por rank weight + icStaff; JoinMOTD legacy+PictureLogin modern; chat hover/click; Apollo soft |
| R7 Red/social | ✓ código · compile V+B · sin QA | Profile/Sync/Unsync + NETWORK-MESSAGES (default off) en Velocity y Bungee; mute ic_punishments; JDA proxy aún vivo |
| R8 Contenido | ✓ código · compile verde · sin QA | JoinEffects→YamlMenu; ChatGames TRIVIA/REACTION/UNSCRAMBLE/MATH/ALGEBRA; coinshop lore owned/stub; tags quit unload |
| R9 QA | ✓ + redeploy oleada final 2026-11-03 | Build verde + deploy Proxy/Lobby/Desktop; sin suite tests / QA runtime in-game |

## Calificación — **~8.2/10** (2026-11-03 noche, post-oleada final)
NMS 1.12/16/20 reales · JDA proxy gated · Redis freeze/vanish · seguridad P0 · R7 V+B · parents+LP SYNC · stubs ChatLog/Gender/coinshop. Resta: proxy-common, rename ioriproxy.*, NMS 26.3, QA runtime, dual JDA code aún en árbol (off).

## GAPS conocidos (post-R9 — no bloquean deploy)
- **LP SYNC** — mínimo real (`ic_<key>` + inheritance en grant); sin LP → fallback OFF. Wildcards LP-style aún no.
- **JDA proxy** — `MODULES.DISCORD-BOT: false` por defecto; webhooks OK; Core dueño bot.
- **Redis staffmode sync** — freeze/vanish vía `FREEZE_UPDATE`/`VANISH_UPDATE` + `STAFF-MODE.REDIS-SYNC` ✓.
- **Rank parents** — menú add/remove reales (anvil); herencia CLI OK.
- **Discord BOTS stub** — multi-bot solo log; PRIMARY activo.
- **Coinshop COSMETIC** — rechazo claro sin cobro; TAG real.
- **ChatLogService** — purge/cap reales por PURGE-DAYS / MAX-LINES.
- **GenderModule** — off por defecto, enable no-op.
- **NMS** — v1_12 / v1_16 / v1_20 anvil+title+actionbar+TAB reales; 1.21 actionbar/TAB vía BukkitNativeFx.
- ~~proxy-common~~ — **WONTFIX** (usuario): `common/` basta; V↔B duplicado OK.
- ~~ioriproxy.* rename~~ — **WONTFIX** (usuario): legado aceptado.
- **R7** — `/profile`+PM network en proxy (V+B); friends proxy OK; JDA dual pendiente de retirar.
- **R5 extras** — CPS/follow, punish tips en hotbar.
- **Sin tests automatizados / QA runtime** — H10 abierto.
