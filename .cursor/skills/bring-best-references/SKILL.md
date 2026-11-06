---
name: bring-best-references
description: Extrae lo mejor de Darling, Helium, CoreDub e IoriCTW sin copiar código incompatible. Usar en refactors de arquitectura.
---

# bring-best-references

## Fuentes locales
| Fuente | Ruta | Tomar | Descartar |
|--------|------|-------|-----------|
| Darling | `D:\Minecraft\Plugins\SoupPvP\Darling\...` | estructura paquetes, `impl/`, arguments, enums file-per-enum, Tasks/Logger/command framework (reescribir) | `Utils` NMS, menús/hologramas Spigot, licencia “no redistribuir” |
| Helium | `...\Helium\Helium-master` | ideas MOTD, maintenance, presencia Redis | estructura caótica, duplicación velocity/bungee sin capa limpia, copiar clases |
| CoreDub Proxy-API | `...\SurvivalDubCore\Proxy-API` | tipado mensajes, reconnect, operaciones | sockets/TCP custom si Redis ya cubre; pegar API entera |
| IoriCTW | `D:\Old\Minecraft\CTW\2026\IoriCTW` | separadores YAML, merge claves, Hikari/messages | forzar Bukkit APIs en proxy |

## Licencias
- Darling: sin redistribución → **solo ideas**.
- kennytv/Maintenance: GPL-3 → patrones, no código.
- Helium/CoreDub: sin LICENSE clara en raíz auditada → **reescribir**.

## Output esperado
Lista de adopciones + DECISIONS.md entry + plan de migración incremental.
