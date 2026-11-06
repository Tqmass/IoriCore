# ROADMAP — plan de implementación (2026-11-03)

Base: [FEATURE-STATUS](../spec/FEATURE-STATUS.md). Autorizado por el usuario: "todas las fases y la core funcionando". Cada fase: build verde + docs actualizados antes de pasar a la siguiente.

## Defaults aplicados a decisiones pendientes (cambiables; marcar en DECISIONS si se ratifican)
| Tema | Default elegido |
|---|---|
| Command whitelist proxy | permitir comandos de backend: el filtro solo actúa sobre comandos del proxy + lista bloqueada |
| Menús | corte Modern **1.16+** por protocolo del **jugador** (ViaVersion; sin Via → versión servidor); layout `menus/{modern,legacy}/`, textos en `messages/` (A5) |
| Framework menús | uno solo (`menu/api` + motor YAML); el viejo se migra |
| Migraciones | runner en `common` con tabla `ic_schema_version(component, version)`; misma BD proxy+core |
| Mongo | retirar |
| Redis Core | Jedis shaded/relocado en Core |
| proxy-id | configurable en settings proxy |
| Vanish | uno solo (servicio essentials, staffmode lo usa) |
| LuckPerms | modo OFF por defecto; READ/SYNC opcionales |
| Flag OP en ranks | eliminado |
| Inventario staffmode | persistido en disco |
| Logout congelado | aviso + comando configurable (vacío por defecto) |

## Fases
| Fase | Objetivo | Depende |
|---|---|---|
| **R1 Estabilización** | 12 bugs críticos de FEATURE-STATUS | — |
| **R2 Infra** | runner migraciones común + BD única, Jedis Core + eventos Redis Core, ban/mute invalidación, retirar Mongo, i18n completo (en/legacy), `/ioricore reload`, permisos declarados, commands/permissions.yml cableados, idioma por jugador | R1 |
| **R3 Ranks** | permisos efectivos + herencia + grants (temporales) + comandos + Redis sync + prefijos en chat/TAB/scoreboard/nametag + LP opcional ([RANKS](../spec/RANKS.md)) | R2 |
| **R4 Menús** | framework YAML único (acciones, condiciones, clics, paginación) con implementaciones Modern/Legacy independientes ([MENUS](../spec/MENUS.md)) | R2 |
| **R5 StaffMode** | herramientas StaffModeX útiles, menús inspect/players, freeze completo, sync red ([STAFFMODE](../spec/STAFFMODE.md)) | R3, R4 |
| **R6 Visual** | scoreboard sin flicker, TAB sorting, nametags, chat hover/click, hex Bukkit, cara PictureLogin (MIT) | R3 |
| **R7 Red/social** | /profile, PM de red, Discord /sync + DM approve, staff auth persistente | R2 |
| **R8 Contenido** | ChatGames tipos, economy/coinshop, easter eggs, /settings aplicadas, essentials faltantes, NMS 1.12/1.16/1.20/1.21.3+ | R4 |
| **R9 QA** | tests unitarios lógica pura, security-auditor, prettier, deploy | todas |

Estado de cada fase → [PHASE-STATUS](PHASE-STATUS.md).
