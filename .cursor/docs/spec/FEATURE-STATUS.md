# FEATURE-STATUS — estado por funcionalidad (2026-11-03)

Estados: `IMPLEMENTED_AND_TESTED` (solo con captura de prueba real; histórica) · `IMPLEMENTED_UNTESTED` · `PARTIAL` · `STUB` · `NOT_IMPLEMENTED`. Detalle y evidencias (`ruta:línea`) en el doc de cada sección.

## Proxy → [PROXY-MODULES.md](PROXY-MODULES.md) · [COMMANDS.md](COMMANDS.md)
| Feature | Estado | Nota |
|---|---|---|
| friend, report/reports, request(helpop), stream, announce, maintenance, analytics, servers, queue, ping, seen, playtime, staffchat, /ioriproxy | IMPLEMENTED_AND_TESTED (histórico) | capturas pre-refactor; revalidar |
| send, goto, find, hub, blacklist, adminchat, alts, transfer, ggwave, glist, alert, redistest | IMPLEMENTED_UNTESTED | |
| /donation | PARTIAL | solo `test` |
| /staff list | PARTIAL | clase sin registrar |
| Command whitelist | IMPLEMENTED_UNTESTED | **bloquea comandos Core** a jugadores |
| SQL proxy (migraciones v1–v6) | IMPLEMENTED_UNTESTED | H2 visto arrancar (img `4a319cbb`) |
| Redis pub/sub | IMPLEMENTED_UNTESTED | **multi-proxy roto** (origin fijo) |
| Ban-check fail-open | PARTIAL | caché sin TTL, no recibe invalidaciones de Core |
| Discord JDA bot | IMPLEMENTED_UNTESTED | |
| Staff auth (gate) | PARTIAL | en memoria, token visible, auto-aprobado tras reinicio |
| Staff auth DM approve · /sync OTC | NOT_IMPLEMENTED | |
| Webhooks | IMPLEMENTED_UNTESTED | |
| MongoDB | PARTIAL | conectado, ningún servicio lo usa |
| i18n en | PARTIAL | faltan 34 claves; legacy cae a modern (70) |

## Core → [CORE-MODULES.md](CORE-MODULES.md)
| Feature | Estado | Nota |
|---|---|---|
| Essentials (53 cmds) | PARTIAL | faltan ~20; spawn/home/warp/tpa/kits/back/nick/god UNTESTED |
| msg/reply · ignore | PARTIAL | sin check de mute; ignore solo en msg |
| World management | IMPLEMENTED_UNTESTED | global, no por mundo |
| Punishments | PARTIAL | sin unban/unmute/history; mute cache sin TTL; bypass /msg /r /me |
| Ranks | PARTIAL | → [RANKS.md](RANKS.md): sin permisos efectivos, herencia, grants; **persistYaml borra permisos** |
| StaffMode | PARTIAL | → [STAFFMODE.md](STAFFMODE.md): **staff puede romper/pegar**; inventario solo en memoria |
| Freeze | PARTIAL | evasión por logout, sin bloqueo de cmds/chat |
| Vanish | IMPLEMENTED_UNTESTED | dos vanish (essentials + staffmode) |
| Scoreboard | IMPLEMENTED_UNTESTED | parpadeo; borra team `icStaff` |
| TAB header/footer | PARTIAL | sin sorting; 1.12 nada |
| Nametags por rank | NOT_IMPLEMENTED | |
| Chat format | PARTIAL | hover/click ignorados; **jugadores usan `&`/PAPI** |
| Chatlogs/cmdlogs | PARTIAL | no registra con chatformat activo; purge/cap stub |
| Join MOTD texto | IMPLEMENTED_UNTESTED | |
| Join MOTD cara (PictureLogin) | STUB | envía URL |
| Bossbar tips | IMPLEMENTED_UNTESTED | OFF |
| Tags | IMPLEMENTED_UNTESTED | OFF |
| Economy | PARTIAL | 1 moneda, no provider Vault |
| Coinshop | PARTIAL | solo tags |
| ChatGames | PARTIAL | solo trivia (ref ChatGames tiene más tipos) |
| Easter eggs | PARTIAL | recompensa nunca se entrega |
| /settings | PARTIAL | preferencias no se aplican (salvo scoreboard) |
| Skin · IP | PARTIAL | |
| Staff auth enforce | PARTIAL | defaults no restringen |
| Plugin messaging | PARTIAL | STAFF_AUTH_SESSION posiblemente al server anterior |
| Redis publisher · Mongo | STUB | jars no incluidos |
| Gender | STUB | |
| Profiles, donor-join, /ioricore reload, PAPI expansion | NOT_IMPLEMENTED | |
| NMS | PARTIAL | reales v1_8_R3, v1_21_R1; 1.12/1.16/1.20 esqueleto; 1.21.3+ Noop |
| Idioma legacy / por jugador | PARTIAL | legacy solo en 4 módulos; idioma global |
| Menús | PARTIAL | → [MENUS.md](MENUS.md): modern/legacy mismo layout; 2 frameworks; sin acciones YAML |
| commands.yml / permissions.yml Core | STUB | cargados, no leídos |

## Bugs críticos (prioridad fase R1 de [ROADMAP](../plan/ROADMAP.md))
1. Whitelist proxy bloquea comandos Core. 2. Staff en staffmode rompe/pega. 3. persistYaml de ranks borra permisos. 4. Chat: `&`/PAPI de jugadores. 5. Mute bypass + cache sin TTL. 6. Ban Core no llega a proxy. 7. Redis multi-proxy. 8. Staff auth bypass. 9. Scoreboard borra nametag staff / parpadeo. 10. Chatlogs vacíos con chatformat. 11. en.yml proxy incompleto. 12. `CREATE INDEX IF NOT EXISTS` en MySQL.
