# OPEN-QUESTIONS — contradicciones, problemas y decisiones pendientes

Auditoría 2026-11-03. **Nada de aquí está decidido**: requiere OK del usuario. Al resolver → mover a `memory/DECISIONS.md` y tachar aquí.

## A. Contradicciones documentales (bloquean reglas/skills)
| # | Versión 1 | Versión 2 | Realidad código | Propuesta |
|---|---|---|---|---|
| A1 | `rules/30-nms-versiones` (alwaysApply): repo **solo proxy**, prohibido módulo backend/NMS | DECISIONS H0 (01-11): monorepo con Core Bukkit + `nms/v*`; "NMS solo Core, proxy nunca" | bukkit + nms existen | Reescribir regla 30 → "NMS solo en `bukkit`/`nms`; nunca en velocity/bungee/common/api" |
| A2 | rules 10/20/60, skills `java-*`, `nuevo-modulo`, `post-task-hygiene`, `prettier-always`, agents `nms-specialist`, README: `me.tqmass.dev.proxy`, IoriProxy, "solo Velocity+Bungee", jars `IoriProxy-*` | DECISIONS H1b: `us.iorilabs.*`, IoriVelo/IoriBungee/IoriCore-Bukkit; rule 70 ya actualizada | `us.iorilabs.*` | Actualizar textos obsoletos (sin cambiar intención) |
| A3 | rule 60 + skill `clean-plugin-engineering`: "features se duplican velocity/bungee, sync siempre" | PLAN H1 + master prompt: `proxy-common/` con lógica compartida | Sin `proxy-common`; 127 ficheros idénticos | Aprobar extracción `proxy-common` y actualizar regla 60 |
| A4 | PLAN "Core JVM": bytecode **8** | DECISIONS "Core JVM multi": bytecode **17**; entrada H1 dice "reemplazado por release 8 (ver Core JVM multi)" (incoherente) | bukkit/common 17 · api/nms 8 · v1_21 21 | Confirmar 17 (servidores 1.8.8 deben correr JDK 17+) |
| A5 | DECISIONS 01-11 + master prompt: `messages/menus/{en,es}.yml`, `messages/formats/{en,es}.yml` + `messages/legacy/formats/`, `easter-eggs.yml` raíz | — | `menus/{modern,legacy}/<menu>.yml` (layout, textos en messages) · `chat/formats/{en,es}.yml` + `chat/formats/legacy/` · `data/easter-eggs.yml` (+ `data/tags|coinshop|chatgames`) | ¿Migrar código a la decisión o ratificar layout actual? |
| A6 | rule 60 / `design/README`: legacy `messages/{lang}-legacy.yml` | MODULE-INVENTORY: `messages/legacy/{lang}.yml` | `messages/legacy/{lang}.yml` | Corregir regla |
| A7 | PLAN + master prompt: Report/HelpOp = **PROXY** | MODULE-INVENTORY/PROGRESS: "por modalidad (plugin modo o Core toggleable)" | Solo proxy | Confirmar PROXY |
| A8 | PLAN: un dueño de migraciones (proxy) | — | bukkit crea `ic_balances/ic_ranks/ic_rank_permissions`; `ic_punishments` creada en **ambos** | ¿Centralizar DDL (p.ej. en `api`/`common` + runner en proxy) o aceptar dueño por módulo? |
| A9 | ~~Mongo retirada Claude~~ | OVERRIDE 03-11: Mongo soft/opción vuelve | Proxy `MONGODB` + Core `SoftMongo` restaurados | **Cerrado** — MySQL principal, Mongo opcional |
| A10 | DECISIONS "Abiertas": renombrar plugin id `ioriproxy`→`iorivelo` | — | Ya es `iorivelo` | Cerrar; decidir permisos `ioriproxy.*` y Redis channel `ioriproxy` |
| A11 | PROGRESS (02-11 21:53): todo FINAL cerrado | MODULE-INVENTORY (21:32): "FINAL en curso" | Código con stubs | Usar `PHASE-STATUS.md` como estado canónico |
| A12 | `docs/refs/img/README`, DESIGN_BRIEF, PLAN citan imágenes por nombre | Contenido real desplazado (ver `IMAGE-INDEX.md`) | — | Renombrar ficheros por contenido o corregir referencias |
| A13 | DESIGN_BRIEF cita img 09, 10 | — | No existen | Eliminar refs o aportar imágenes |
| A14 | `common` objetivo `us.iorilabs.common` (PLAN, master prompt) | — | `us.iorilabs.proxy.{commands,utilities…}` usado también por bukkit | ¿Renombrar paquete (refactor amplio)? |
| A15 | Brand: "RoyalCTW, nunca Iori Network" | Capturas: prefijo `[IoriProxy]`, MOTD "IoriLabs PROJECT", discord.gg/ioriproxy | Mixto | Definir prefijo chat/consola oficial |

## B. Problemas técnicos
1. Duplicación velocity↔bungee ~16k LOC (deriva ya iniciada: 2 ficheros difieren).
2. Build no portable: rutas absolutas a jars Spigot locales.
3. Versiones de drivers distintas proxy vs bukkit (Hikari, mysql-j, H2).
4. Bukkit Redis = noop salvo Jedis externo (compileOnly, no shaded) → Core no publica eventos en la práctica.
5. NMS: stubs 1.12/1.16/1.20; huecos de versión; 1.21.x único wrapper compilado contra 1.21.1.
6. `@Plugin(version="1.0.0-SNAPSHOT")` hardcode en Velocity (gradle = 1.1.0).
7. Strings de jugador hardcode (proxy): `HelpOpService` "Responder"/"Ir al jugador", `StreamService` "Clic para abrir".
8. Plugin messaging backend sin firma → requiere forwarding moderno Velocity / BungeeGuard y backends no expuestos.
9. Cero tests automatizados.
10. Clases grandes: `FriendService` 1019, `IoriProxy` 678, `MaintenanceService` 653.
11. Sin git; `.gitignore` excluye `.cursor/` y `docs/` (conocimiento sin versionar). `.cursor.rar` (23 MB) = backup anterior al estado actual.
12. `_refs/` contiene decompilado EssentialsLite y OpenPhoenix (59 MB): solo referencia, nunca a `src/`.
13. Bugs vistos en capturas (estado actual desconocido): `</hover>` crudo en donación, `{MOTIVO=…}` sin resolver en hover maintenance, `§` crudo en banner consola Velocity, "already connected" en inglés, texto "tú enviado a lobby por".

## C. Pendiente de input del usuario
- Transcript mencionado en el master prompt: **no recibido** en esta sesión.
- Confirmar cuáles de A1–A15 se aprueban antes de editar `.cursor/rules|skills|agents`.
