# RANKS — sistema de rangos y grants (spec)

Auditoría 2026-11-03 · solo lectura · ref. funcional **Helium** (`.cursor/_refs/Helium`, sin licencia → **nunca copiar código**, solo ideas de flujo/UX).
Docs relacionados: [PERMISSIONS](PERMISSIONS.md) · [COMMANDS](COMMANDS.md) · [MENUS](MENUS.md) · [SCOREBOARD-TAB-CHAT](SCOREBOARD-TAB-CHAT.md) · [INTEGRATIONS](INTEGRATIONS.md) · [CONFIGURATIONS](CONFIGURATIONS.md) · [PROXY-MODULES](PROXY-MODULES.md) · [FEATURE-STATUS](FEATURE-STATUS.md).

Abreviaturas: `H/` = `.cursor/_refs/Helium/bukkit/src/main/java/services/plasma/helium/` · `IC/` = `bukkit/src/main/java/us/iorilabs/bukkit/` · `RES/` = `bukkit/src/main/resources/`.

---

## 1. Qué hace Helium (flujo real)

### 1.1 Modelo `Rank` — `H/rank/Rank.java`
| Campo | Notas |
|---|---|
| `id` UUID, `name`, `displayName` | `setName` arrastra displayName si eran iguales (`Rank.java:138`) |
| `prefix`, `hexPrefix`, `suffix` | defaults `&f` |
| `priority` (= `getWeight()`) | orden de ranks y elección de grant activo |
| `type` `NORMAL/DONATOR/MEDIA/STAFF` | `H/rank/RankType.java` (ciclo `next()`) |
| `color` ChatColor + `bold` + `italic` | `getColors()` / `getColoredName()` |
| `defaultRank` | uno solo; `RankHandler.getOrCreateDefaultRank()` lo crea si falta (`RankHandler.java:205`) |
| `permissions: List<Permission>` | `Permission{permission, bungeecord, servers[]}` (`H/permission/Permission.java`) → **scope por permiso** (global si `servers` vacío; `bungeecord=true` → se aplica en proxy) |
| `parentList: List<UUID>` | herencia por **UUID**; `canParent()` evita ciclos (`Rank.java:157`) |
| `getCompoundedPermissions()` | propias + recursivo de padres (`Rank.java:173`) — sin negaciones, sin dedupe |
| `rankFlags` | `SUBSCRIPTION, MEDIA, HIDDEN, STAFF, HEAD_STAFF, REQUIRES_2FA, REQUIRES_PUNISHMENT_PROOF_*` (`H/rank/flags/RankFlag.java`) — staff/media/hidden son **flags**, no booleanos |

> La captura `_refs/img/20-helium-rank-editor.png` muestra `Default/Staff/Admin/Op/Visible` como booleanos: corresponde a otra versión de Helium/Plasma; el código de `_refs` usa flags. **Admin/Op no existen** en el `Rank` de `_refs`.

### 1.2 Grants — `H/profile/grant/`
- `Grant` base (`Grant.java`): `id, duration (Long.MAX_VALUE = permanente), servers[] (vacío = global), addedBy/addedReason/addedAt, removed/removedBy/removedReason/removedAt`; `canActivate()` compara `servers` con nombre del servidor.
- Tipos: `RankGrant(rankId)`, `PermissionGrant(permission, bungee)`, `TagGrant(tagId)` (`type/*.java`).
- Activo: `Profile.activateNextRankGrant()` ordena RankGrants por priority desc y toma el primero no expirado/no removido/activable (`H/profile/Profile.java:811`).
- Expiración: `Profile.checkGrants()` (`Profile.java:713`) marca `removed` con motivo "Expired Grant.", lanza `GrantExpireEvent` + `GrantExpirePacket` (Redis); garantiza siempre un grant **Default** permanente. Ejecutado cada 20 ticks async (`ProfileHandler.java:72`), en join (`ProfileListener.java:145`) y al recibir `GrantUpdatePacket` (`GrantPacketListener.java:24`).
- Motivos predefinidos: `reasons/GrantReason*` + packets `GrantReasonUpdate/Delete`.
- Eventos: `GrantAppliedEvent`, `GrantExpireEvent` → `GrantListener` (mensajes) y `modules/discord/DiscordGrantEvent` (log Discord).

### 1.3 Persistencia / caché / sync
| Aspecto | Helium |
|---|---|
| BD | **MongoDB** (`resources/storage.yml`), colección `ranks` (`RankHandler.RANK_COLLECTION`); grants **embebidos** en el documento de perfil (`Profile.java:313/517`) |
| Caché | `List<Rank>` en memoria, búsqueda lineal (`RankHandler.getRank`) |
| Sync | Redis packets: `RankUpdatePacket`/`RankDeletePacket` (`H/rank/packet/`) → `pullUpdate(id)` relee de Mongo; `GrantUpdatePacket`/`GrantAppliedPacket`/`GrantExpirePacket`; `RankPermissionRefreshListener` reaplica permisos al cambiar un rank; broadcast staff `ServerBroadcastPacket` en create/update/delete (`RankHandler.java:160-189`) |
| Import/Export | `exportToFile/importFromFile` → `ranks/<name>.yml` (`RankHandler.java:50/92`) |

### 1.4 Permisos efectivos
- `Profile.setupPermissionAttachments()` (`Profile.java:949`): junta permisos compuestos del rank activo + **todos** los RankGrants activos + PermissionGrants; separa bukkit/bungee; limpia y recrea un `PermissionAttachment` propio; `recalculatePermissions()`; bungee → `PlayerPermissionsPacket` vía Redis; fija `displayName`.
- Inyección de `PermissibleBase` propio por reflexión (`H/permission/bukkit/BukkitPermissibleBase.java`, `BukkitPermissionListener.java` campo `CraftHumanEntity.perm`) para wildcards/op.
- Proxy: `bungee/.../listener/PermissionListener.java` responde `PermissionCheckEvent` con mapa `uuid→perms` (soporta `prefix.*`).

### 1.5 Integraciones de presentación
- Chat: `H/chat/ChatListener.java:97-103` → `<prefix>/<suffix>` del rank activo.
- Nametags: `H/nametag/NametagHandler.java:25/65` (prefijo + colores del rank).
- PAPI: `H/papi/HeliumPlaceholderExpansion.java:93-103` → `player_rank_name|display_name|color|prefix|suffix`.
- Disguise rank (`profile/command/menu/DisguiseRankMenu.java`), free rank (`modules/freerank`), coinshop rank selector, web editor (`modules/webeditor/pages/RankPage.java`).

### 1.6 Menús
| Menú | Clase |
|---|---|
| Rank list | `H/rank/menu/RankListMenu.java` + `button/RankInfoButton.java` (tooltip = img 15) |
| Rank editor | `H/rank/menu/editor/RankEditorMenu.java` (+ `RankEditButton`, `RankPreviewButton`) |
| Parents | `editor/parent/RankViewParentsMenu`, `RankParentAddMenu` |
| Permisos | `editor/permission/RankViewPermissionsMenu`, `RankEditPermissionMenu` |
| Flags | `editor/flags/RankViewFlagsMenu`, `flags/menu/RankFlagMenu` |
| Prompts (chat) | `H/rank/prompt/RankColorPrompt, RankEditString/Integer/DurationPrompt, RankPermissionAddPrompt, RankPermissionServerAddPrompt` |
| Grant (wizard) | `grant/menu/grant/GrantMenu` → `rank/RankGrantMenu` (o `tag/TagGrantMenu`) → `GrantDurationMenu` → `GrantReasonMenu` → `GrantScopesMenu`/`GrantServersMenu` (+ prompts `GrantDuration/Reason/Server/Permission/ExpandDuration/RemovePrompt`) |
| Historial | `grant/menu/GrantViewAllMenu`, `rank/RankGrantsMenu`, `tag/TagGrantsMenu`, `permission/PermissionGrantsMenu` |

### 1.7 Comandos (ACF) — `H/rank/command/RankCommands.java`, `H/profile/grant/command/GrantCommands.java`
| Comando | Permiso |
|---|---|
| `/rank create <name>` | `helium.command.rank.create` |
| `/rank delete <rank>` | `helium.command.rank.delete` |
| `/ranks` · `/rank list` | `helium.command.rank.list` |
| `/rank edit <rank>` | `helium.command.rank.edit` |
| `/rank permission\|perm add\|set <rank> <perm>` · `remove\|unset` | `helium.command.rank.edit` |
| `/grant <player>` (menú) | `helium.command.grant` |
| `/grants <player>` (historial) | `helium.command.grants` |
| `/cleargrants\|wipegrants <player>` | `helium.command.grantremove` |
| `/consolegrant\|cgrant\|ogrant <type> <ctx> <player> <dur> <reason>` | solo consola |
| `/removegrant\|rgrant <type> <ctx> <player>` | solo consola |

---

## 2. Qué hace IoriCore hoy

### 2.1 Inventario
| Pieza | Ruta |
|---|---|
| Módulo | `IC/ranks/RanksModule.java` (registrado en `IC/IoriCorePlugin.java:161`; flag `MODULES.RANKS` `RES/settings.yml:53`) |
| Modelo | `IC/ranks/Rank.java` (id, name, displayName, prefix, suffix, priority, type, scope, colorKey, italic, bold, staff, media, admin, op, visible, defaultRank, parents **por nombre**, `permissionCount`) |
| Enums | `IC/ranks/enums/RankType.java` (`NORMAL/STAFF/DONOR/MEDIA`), `RankScope.java` (`GLOBAL/SERVER`) |
| Servicio | `IC/ranks/RankService.java` (cache `ConcurrentHashMap` byName/byId, YAML o SQL según `RANKS.STORAGE` `RES/settings.yml:211-213`) |
| SQL | `IC/ranks/RankSql.java` (`ic_ranks`, `ic_rank_permissions`) |
| Comando | `IC/ranks/RanksCommand.java` + `argument/RanksListArgument`, `RanksHelpArgument` |
| Menús | `IC/ranks/menu/RankListMenu.java`, `RankEditMenu.java`, `RankParentsMenu.java`; YAML `RES/menus/{modern,legacy}/rank-{list,edit,parents}.yml` |
| Datos | `RES/ranks.yml` (6 ranks: default, founder, developer, admin, mod, helper) |
| Mensajes | `RES/messages/{en,es}.yml` + `legacy/` bloque `RANKS:` |
| LP | `IC/hook/SoftLuckPermsHook.java`, interfaz `api/.../hook/LuckPermsHook.java`; proxy `velocity|bungee/.../hook/luckperms/LuckPermsHook.java` |

### 2.2 Estado por funcionalidad
| Funcionalidad | ESTADO | Evidencia |
|---|---|---|
| Carga ranks desde `ranks.yml` | IMPLEMENTED_UNTESTED | `RankService.java:51-60,124` |
| Carga ranks SQL (GLOBAL + SERVER por `SETTINGS.SERVER-NAME`) con seed desde YAML | IMPLEMENTED_UNTESTED | `RankService.java:99-122`, `RankSql.java:96-131` |
| Persistencia de edición (YAML/SQL async) | PARTIAL | `RankService.save` `:62`; YAML reescribe todo el fichero (pierde comentarios) y **escribe `PERMISSIONS: []`** (`:163`) |
| Permisos de rank (almacenar/aplicar) | NOT_IMPLEMENTED | solo `permissionCount`; `ic_rank_permissions` nunca se escribe; `PERMISSIONS` se ignora salvo para contarlos (`RankService.java:233-239`) |
| Herencia (parents) efectiva | NOT_IMPLEMENTED | parents solo se muestran; sin resolución de permisos ni detección de ciclos |
| Editar parents (añadir/quitar) | STUB | `RankParentsMenu.java:79,87` → mensajes `PARENT-REMOVE-SOON` / `PARENT-ADD-SOON` |
| Rank List menú + tooltip | IMPLEMENTED_UNTESTED | `RankListMenu.java` (lore `RANKS.MENU.LORE` + parents) — img 15 es **referencia Helium**, no prueba nuestra |
| Rank Editor (name, display, weight, prefix, suffix, color, bold, italic, default, staff, admin, op, visible) | IMPLEMENTED_UNTESTED | `RankEditMenu.java:85-256`, anvil `AnvilInput` `:373` |
| Editar type / scope / media desde menú | NOT_IMPLEMENTED | no hay botón en `RankEditMenu` |
| Crear / borrar rank | NOT_IMPLEMENTED | sin subcomando ni botón |
| Rank por defecto (unicidad, asignación) | PARTIAL | flag `DEFAULT` existe; no se garantiza unicidad ni se asigna a nadie |
| Grants (asignar rank a jugador, duración, motivo, quién, scope) | NOT_IMPLEMENTED | no existe modelo/tabla/comando |
| Rank activo del jugador | NOT_IMPLEMENTED | ningún servicio resuelve `UUID→Rank` |
| Permisos efectivos (attachments) | NOT_IMPLEMENTED | ningún `addAttachment` en `IC/ranks/**` |
| Flag `OP` / `ADMIN` / `STAFF` aplicados | NOT_IMPLEMENTED | solo datos; nadie los consume |
| Sync entre servidores (Redis) | NOT_IMPLEMENTED | bukkit solo publica invalidaciones de sanciones (`IC/redis/SoftRedisPublisher.java:56`), no suscribe |
| Chat format usa rank IoriCore | NOT_IMPLEMENTED | `IC/modules/chatformat/ChatFormatService.java:139-160` → Vault → LP |
| TAB / scoreboard usan rank | NOT_IMPLEMENTED | `IC/util/BoardPlaceholders.java:25-40` → `vault_prefix` (Vault → LP) |
| Nametags por rank | NOT_IMPLEMENTED | solo nametag staffmode (`IC/modules/staffmode/StaffModeService.java:357-377`) y teams de scoreboard |
| Hook LuckPerms (lectura prefix/suffix) | IMPLEMENTED_UNTESTED | `SoftLuckPermsHook.java:46-69` (reflexión); `applyPrefix` vacío (`:41`) = STUB |
| Comando `/ranks` (`list`, `help`) | IMPLEMENTED_UNTESTED | `RanksCommand.java:28-37`, `plugin.yml:25`, perm `ioricore.ranks` (+ fallback `ioricore.admin` `:42`) |
| Proxy: "rank" de staff | IMPLEMENTED_UNTESTED (no es sistema de ranks) | `velocity/.../staff/StaffGroups.java` → grupos por permiso `ioriproxy.staff.{admin,mod,helper}` (`velocity/src/main/resources/permissions.yml:104-107`) + prefijo LP (`displayRank`) usado por staffchat/adminchat/`/staff` |

---

## 3. Faltantes (vs Helium + DECISIONS)
1. Permisos por rank (CRUD + almacenamiento + scope por permiso/servidor + negaciones).
2. Resolución de herencia (por id, anti-ciclos, caché de permisos compuestos).
3. Grants: modelo, tabla, `/grant` wizard (rank → duración → motivo → scope), `/grants` historial, revocar, expiración periódica, auditoría (addedBy/removedBy).
4. Rank por defecto garantizado (único; grant implícito, no persistido por jugador).
5. Rank activo + permisos efectivos (attachment Bukkit) + permisos proxy.
6. Sync Redis (rank changed/deleted, grant added/removed/expired) y subscriber en bukkit.
7. Crear/borrar/clonar rank; editar type, scope, media, flags; añadir/quitar parents y permisos desde menú.
8. Proveedor de prefix/suffix propio para chat/TAB/scoreboard/nametag (placeholders `rank_*`) + PAPI expansion opcional.
9. Menús: grant wizard, grant history, permissions editor (paginado), confirm delete.
10. Import/export YAML ↔ SQL explícito (`/rank export|import`).

## 4. Duplicados / solapes
| Solape | Dónde | Riesgo |
|---|---|---|
| Dos instancias `SoftLuckPermsHook` | `IoriCorePlugin.java:132` (respeta `MODULES.LUCKPERMS-HOOK`) y `RanksModule.java:51` (fuerza `true`, campo sin uso) | ignora el toggle; log "LuckPerms detected" doble |
| `saveResource("ranks.yml")` doble | `IoriCorePlugin.java:92` y `RanksModule.java:45` | inocuo, redundante |
| Prefijo de chat/TAB | `ranks.yml PREFIX` vs Vault/LP (`ChatFormatService`, `BoardPlaceholders`) | hoy el PREFIX de IoriCore **no se muestra en ningún sitio** |
| Staff/Admin del rank vs `StaffGroups` proxy (permisos) vs `ioricore.rank.owner/staff/donor` (`RES/permissions.yml:126-128`, `IC/modules/chatlogs/ChatLogListener.java:35-41`) | 3 fuentes para "es staff" | incoherencias de etiquetas en logs/staffchat |
| `RankType` STAFF/MEDIA vs booleanos `staff`/`media` | `Rank.java` | dos campos para lo mismo |
| `PRIORITY` vs label UI "Weight" | `RankEditMenu` WEIGHT ↔ `priority` | nomenclatura |
| `ioricore.ranks` vs `ioricore.admin` fallback | `RanksCommand.java:42` | permiso implícito no documentado |
| Mensaje `RANKS.EDIT-SOON` obsoleto | `RES/messages/en.yml:21` | texto muerto |
| Tags (`IC/modules/tags`) ≈ `TagGrant` Helium | — | decidir si tags usan el mismo sistema de grants |

## 5. Qué conservar
- `RankService` con caché `ConcurrentHashMap` byName/byId y persistencia async.
- `RankScope GLOBAL|SERVER` + `server_key` (DECISIONS "Ranks: ranks.yml + SQL scope GLOBAL|SERVER").
- Seed SQL desde `ranks.yml` cuando `ic_ranks` está vacío.
- Menús configurables por YAML modern/legacy (`MenuConfig`, slots/ITEMS/fillers) y anvil input con validación (`RankEditMenu.java:275-364`).
- IDs UUID estables (`nameUUIDFromBytes("rank:"+name)` como fallback).
- Comando con `CommandParent`/`CommandArgument` y textos en `messages/*`.

## 6. Qué optimizar
| Problema | Propuesta |
|---|---|
| Parents por **nombre** (rename rompe herencia; `applyName` `RankEditMenu.java:287`) | parents por **id** en SQL; YAML por key estable (`founder`) |
| `persistYaml` borra `PERMISSIONS` y reescribe todo | YAML = semilla/config editable a mano; runtime en SQL; si YAML-mode, escribir solo campos cambiados y conservar permisos |
| `parents` en columna TEXT CSV | tabla `ic_rank_parents` |
| Scope por rank solamente | scope **por permiso** (`server_key` en `ic_rank_permissions`) + por grant |
| Mutación del objeto cacheado antes de persistir (no atómico) | copy-on-write + evento `RANK_UPDATED` tras commit |
| `CREATE INDEX IF NOT EXISTS` / `ON DUPLICATE KEY` | NO VERIFICADO en H2 (requiere `MODE=MySQL`); centralizar DDL (OPEN-QUESTIONS A8) |
| `ALTER TABLE` silenciosos (`RankSql.java:53-57`) | migraciones versionadas (`ic_schema_version`) |
| Default de scope distinto YAML (`SERVER`) vs SQL (`GLOBAL`) (`RankService.java:228`) | default único `GLOBAL` |
| Flags booleanos sueltos | set `FLAGS: [STAFF, ADMIN, MEDIA, HIDDEN]` + `TYPE` solo para agrupar en UI |
| Booleano `OP` | **eliminar** (dar OP por rank es riesgo de seguridad; usar `*` explícito restringido a consola) |

## 7. Comandos y permisos propuestos
Raíz de permisos `ioricore.rank.*` / `ioricore.grant.*`. Ver [COMMANDS](COMMANDS.md) / [PERMISSIONS](PERMISSIONS.md).

| Comando | Permiso | Estado |
|---|---|---|
| `/ranks` · `/rank list` (menú) | `ioricore.ranks` | **EXISTENTE** (`RanksCommand`) — mantener alias |
| `/ranks help` | `ioricore.ranks` | **EXISTENTE** |
| `/rank info <rank>` | `ioricore.rank.info` | NUEVO |
| `/rank create <name> [weight]` | `ioricore.rank.create` | NUEVO |
| `/rank delete <rank>` (confirm menú) | `ioricore.rank.delete` | NUEVO |
| `/rank edit <rank>` (abre RankEditMenu) | `ioricore.rank.edit` | NUEVO (menú existe) |
| `/rank set <rank> <prefix\|suffix\|display\|weight\|color\|type\|scope> <valor>` | `ioricore.rank.edit` | NUEVO |
| `/rank flag <rank> <flag> [true\|false]` | `ioricore.rank.edit` | NUEVO |
| `/rank parent <add\|remove> <rank> <parent>` | `ioricore.rank.edit` | NUEVO |
| `/rank perm <add\|remove> <rank> <perm> [server]` · `/rank perm list <rank>` | `ioricore.rank.permissions` | NUEVO |
| `/rank export\|import [rank]` (YAML↔SQL) | `ioricore.rank.admin` | NUEVO |
| `/rank reload` | `ioricore.rank.admin` | NUEVO |
| `/grant <player>` (wizard menú) | `ioricore.grant` | NUEVO |
| `/grant <player> <rank> <dur\|perm> [server\|global] [motivo]` | `ioricore.grant` | NUEVO |
| `/grants <player>` (historial menú) | `ioricore.grant.history` | NUEVO |
| `/ungrant <player> <rank> [motivo]` | `ioricore.grant.remove` | NUEVO |
| `/cleargrants <player>` | `ioricore.grant.clear` (+ solo consola configurable) | NUEVO |
| `/checkperm <player> <perm>` | `ioricore.rank.checkperm` | NUEVO |
| Límite jerárquico | `ioricore.grant.bypass-weight` (sin él solo se otorgan ranks con weight < propio) | NUEVO |

Reglas: validar nombre `[A-Za-z0-9_-]{1,32}` (ya en `RankEditMenu.java:277`); rate-limit grants; log de auditoría en cada mutación (ver `ioricore-security`).

## 8. YAML propuesto (`ranks.yml`, estilo IoriCTW)
Principios: sección `DEFAULTS` aplicada a todos los ranks; solo se escriben las diferencias; permisos **solo** en el rank que los introduce (herencia vía `PARENTS`); visual separado de lógica; textos de jugador siguen en `messages/*`.

```yaml
# *************************************
# *         IoriCore | Ranks          *
# *************************************
# ~ Fuente de semilla. Con RANKS.STORAGE=SQL se importa a ic_ranks si está vacía.
# ~ PARENTS por KEY (no por nombre visible). Permisos se heredan: no repetir.
# ~ Permiso con server: "perm@lobby-1" · negación: "-perm"

DEFAULTS:
  SCOPE: GLOBAL
  TYPE: NORMAL
  FLAGS: []
  VISUAL:
    COLOR: GRAY
    BOLD: false
    ITALIC: false
    SUFFIX: ""

DEFAULT-RANK: member

RANKS:
  # ── Jugadores ────────────────────────
  member:
    WEIGHT: 0
    VISUAL:
      DISPLAY: "&7Member"
      PREFIX: "&7"
    PERMISSIONS:
      - ioricore.settings
      - ioricore.skin

  vip:
    WEIGHT: 400
    TYPE: DONOR
    PARENTS: [member]
    VISUAL:
      DISPLAY: "&a[VIP]"
      PREFIX: "&a[VIP] "
      COLOR: GREEN
    PERMISSIONS:
      - ioricore.chat.color
      - ioricore.join.brand

  # ── Staff ────────────────────────────
  helper:
    WEIGHT: 2000
    TYPE: STAFF
    FLAGS: [STAFF]
    PARENTS: [vip]
    VISUAL:
      DISPLAY: "&9[Helper]"
      PREFIX: "&9[Helper] "
      COLOR: BLUE
    PERMISSIONS:
      - ioriproxy.staff.helper
      - ioricore.staffmode
      - ioricore.punish.mute

  mod:
    WEIGHT: 2600
    TYPE: STAFF
    FLAGS: [STAFF]
    PARENTS: [helper]
    VISUAL:
      DISPLAY: "&2[Mod]"
      PREFIX: "&2[Mod] "
      COLOR: DARK_GREEN
    PERMISSIONS:
      - ioriproxy.staff.mod
      - ioricore.punish.ban
      - "-ioricore.punish.unban"

  founder:
    WEIGHT: 4000
    TYPE: STAFF
    FLAGS: [STAFF, ADMIN]
    PARENTS: [mod]
    VISUAL:
      DISPLAY: "&4[Founder]"
      PREFIX: "&4[Founder] "
      COLOR: DARK_RED
      BOLD: true
    PERMISSIONS:
      - ioricore.rank.*
      - ioricore.grant.*
      - "worldedit.*@build-1"
```

Cambios vs actual: `PRIORITY`→`WEIGHT`, `ID` opcional (derivado de key), sin `PERMISSION-COUNT`, sin `OP`, sin `NAME` redundante (=key), `STAFF/ADMIN/MEDIA/VISIBLE` → `FLAGS` (`HIDDEN` = no visible), `DEFAULT: true` por rank → `DEFAULT-RANK` único. `RANKS.STORAGE` permanece en `settings.yml`. Menús siguen en `menus/{modern,legacy}/rank-*.yml` (+ nuevos `grant.yml`, `grant-duration.yml`, `grant-history.yml`, `rank-permissions.yml`) con formato `kits-es.yml` de IoriCTW (ver [MENUS](MENUS.md)).

## 9. Integración

### 9.1 Ownership
| Pieza | Dueño |
|---|---|
| Definición de ranks, grants, attachments, menús, comandos | **Core (bukkit)** — DECISIONS 2026-11-01 |
| Resolución de permisos en proxy (staffchat, `/staff`, comandos proxy) | Proxy lee `ic_*` (solo lectura) + escucha Redis; **sin** UI |
| DDL | Pendiente OPEN-QUESTIONS **A8**. Recomendado: DDL en `common` (sin deps de plataforma) ejecutado por Core; proxy solo `SELECT` |

### 9.2 MySQL propuesto (MariaDB principal, H2 fallback)
| Tabla | Columnas clave | Notas |
|---|---|---|
| `ic_ranks` (existe) | `id PK, key UNIQUE, display, prefix, suffix, color, bold, italic, weight, type, flags (SET/CSV), scope, server_key, updated_at, version` | migrar `priority→weight`, quitar `operator`, `admin/staff/media/visible`→`flags` (no hay datos que migrar) |
| `ic_rank_parents` | `rank_id, parent_id` PK compuesta | reemplaza `parents TEXT` |
| `ic_rank_permissions` (existe) | `rank_id, permission, server_key ('' = global), value BOOL` PK `(rank_id, permission, server_key)` | añadir `server_key`, `value` (negación) |
| `ic_grants` | `id PK, player_uuid IDX, type (RANK/PERMISSION), target, server_key, added_by, added_reason, added_at, expires_at NULL, removed_by, removed_reason, removed_at` | historial completo; default NO se persiste |
| `ic_schema_version` | módulo/versión | migraciones ordenadas |

### 9.3 Caché
- Ranks: mapa inmutable `id→Rank` + `key→id`; permisos compuestos precalculados por rank (invalidar en cascada a hijos).
- Grants: solo jugadores online (`uuid→List<Grant>`), carga async en `AsyncPlayerPreLoginEvent`, descarga en quit.
- Proxy: `uuid→Set<perm>` para jugadores conectados, rellenado desde SQL en login + Redis.

### 9.4 Redis (eventos) — canal `REDIS.CHANNEL` (hoy `ioriproxy`)
| Evento | Payload | Consumidores |
|---|---|---|
| `RANK_UPDATED` | `rankId, version, origin` | Core (recarga rank + recompute attachments de online con ese rank o descendientes), Proxy |
| `RANK_DELETED` | `rankId` | Core, Proxy |
| `GRANT_ADDED` / `GRANT_REMOVED` / `GRANT_EXPIRED` | `grantId, playerUuid, target, server_key` | servidor donde está el jugador, Proxy, Discord log |
- Bukkit necesita **subscriber** (hoy solo `SoftRedisPublisher`). Sin Redis: fallback a polling de `updated_at` (configurable) o solo aplica en el servidor local.
- Firmar `origin` y validar tipo (ver `ioricore-security`, anti-spoof).

### 9.5 Permisos efectivos
- Un `PermissionAttachment` por jugador del plugin; recálculo en main thread a partir de set precalculado async.
- Orden: permisos de rank activo + ranks de grants activos (de mayor a menor weight) + PermissionGrants; negaciones ganan; filtrar por `server_key`.
- Wildcards `x.*`: resolver al construir el set (sin inyectar `PermissibleBase` por reflexión como Helium).
- Rank **visible** = mayor weight entre grants activos y activables (como `activateNextRankGrant`).
- Expiración: tarea async cada 30–60 s solo para online + comprobación en login.

### 9.6 Presentación
| Destino | Integración |
|---|---|
| Chat | `ChatFormatService.resolvePrefix` → orden: **IoriCore rank** → Vault → LP; parte `rank` de formats usa `%rank_prefix%` / `%rank_display%` |
| TAB / scoreboard | `BoardPlaceholders` añade `rank_prefix, rank_suffix, rank_display, rank_color, rank_weight`; mantener `vault_prefix` como alias |
| Nametags | team por rank ordenado por weight (`zz{weight inverso}`), prefijo truncado (≤16 en legacy); coexistir con nametag staffmode (prioridad staffmode) |
| PAPI | expansión opcional `ioricore_rank_*` |
| Proxy | `StaffGroups.displayRank` → prefijo IoriCore (vía SQL/Redis) con fallback LP; staff = flag `STAFF` |
| Chatlogs | etiqueta `[rank]` desde rank activo (reemplaza `ioricore.rank.owner/staff/donor`) |

### 9.7 Menús
`rank-list` (existe) · `rank-edit` (existe; añadir TYPE, SCOPE, FLAGS, PERMISSIONS, DELETE) · `rank-parents` (existe; implementar add/remove) · NUEVOS `rank-permissions`, `rank-create`, `grant` (selección rank), `grant-duration`, `grant-reason`, `grant-scope`, `grant-history` — doble set modern/legacy independiente.

## 10. LuckPerms como integración opcional
IoriCore es **propietario** de ranks/grants; LP nunca es fuente de verdad salvo en modo `READ` explícito.

| Modo `RANKS.LUCKPERMS.MODE` | Comportamiento | Uso |
|---|---|---|
| `OFF` (default si IoriCore aplica permisos) | IoriCore aplica attachments; LP ignorado para ranks | red sin LP |
| `SYNC` (IoriCore → LP) | IoriCore no crea attachments; por cada rank crea/actualiza grupo LP `ic_<key>` (weight, prefix/suffix meta, permisos, inheritance); grants → `InheritanceNode` con expiry/contexto `server` | redes que quieren LP como motor de permisos (Vault/otros plugins) |
| `READ` (LP → display) | IoriCore no gestiona permisos; lee prefix/suffix/grupo primario de LP para chat/TAB/nametag (comportamiento actual de `SoftLuckPermsHook`) | migración / redes existentes |

Reglas:
- Hook siempre soft (reflexión o `compileOnly` + clase aislada); única instancia en `IoriCorePlugin` (eliminar la de `RanksModule.java:51`).
- En `SYNC`, cambios hechos a mano en grupos `ic_*` de LP se sobrescriben (IoriCore gana); grupos sin prefijo `ic_` no se tocan.
- `SoftLuckPermsHook.applyPrefix` hoy vacío (`SoftLuckPermsHook.java:41`) → sustituir por `syncRank/syncGrant`.
- Proxy: hook LP de `velocity|bungee/.../hook/luckperms/LuckPermsHook.java` solo como fallback de display.
