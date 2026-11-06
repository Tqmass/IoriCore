---
name: ioricore-bukkit
description: Desarrollo en IoriCore-Bukkit (módulos Core, menús, eventos, comandos, FX, hooks soft, idioma modern/legacy). Usar al tocar bukkit/src o al crear una feature de backend Spigot/Paper.
---

# ioricore-bukkit

Raíz: `bukkit/src/main/java/us/iorilabs/bukkit/` · entrada `IoriCorePlugin` (holder Lombok, registra todo).

## Patrón de módulo
- `modules/<feature>/` con `<Feature>Module implements CoreModule` → registrar en `IoriCorePlugin` (`modules.register(...)`) + toggle `settings.yml → MODULES.<FEATURE>` + id en `api/.../modules/ModuleId`.
- Servicio con la lógica, `command/` + `command/argument/`, `menu/`, `enums/`.
- Leer un módulo hermano parecido antes de crear (p.ej. `tags/`, `economy/`, `staffmode/`).

## Infra disponible (reutilizar, no reinventar)
| Necesito | Usar |
|---|---|
| Menú | `menu/` + `menu/api/` (`MenuApi`, botones) · layout `menus/{modern,legacy}/<menu>.yml` · textos en messages |
| Input texto | `menu/anvil` → `VersionWrapper.openAnvilInput` |
| Sonido/Title/ActionBar | `fx/SoundService` (`sounds.yml` ids lógicos), `TitleService`, `ActionBarService` |
| Mensajes | `language/CoreLanguage` (modern vs legacy según protocolo ≤ `SETTINGS.LEGACY-MAX-VERSION`) |
| Comandos | `commands/BukkitCommandBridge` + `CommandParent` (common) · meta `commands.yml` / `permissions.yml` · permisos `ioricore.*` |
| Hooks | `plugin.vaultHook() luckPermsHook() apolloHook() nLoginHook() geyserHook()` (Soft*, Noop si falta) |
| Storage | `plugin.storage()` (`CoreSqlStorage`) · ver skill `ioricore-storage` |
| Redis | `plugin.redisPublisher()` — noop sin Jedis externo |

## Reglas
- Compila contra Spigot **1.8.8** (jar local) y corre en 1.8–1.21: API moderna solo con comprobación de versión o vía `VersionWrapper`/`BukkitNativeFx`.
- Nada de NMS fuera de `nms/v*` (skill `ioricore-nms`).
- Cero texto al jugador en código: claves en `messages/{en,es}.yml` y `messages/legacy/{en,es}.yml`.
- I/O async (`runTaskAsynchronously`), vuelta a main thread para API Bukkit.
- No duplicar lo que es del proxy (report/helpop, friends, find, staffchat de red…).
