---
name: clean-plugin-engineering
description: Keep IoriProxy professional, modular, and maintainable with clear packages, config separation, and no fake implementations.
---

# clean-plugin-engineering

## Instructions

1. Cada clase debe tener una responsabilidad clara.
2. No duplicar lógica entre módulos salvo el patrón velocity/bungee acordado para features.
3. No crear helpers o interfaces sin una necesidad concreta.
4. **Config ops:** `settings.yml`, `redis.yml`, `database.yml` son base; feature YAMLs solo flags operativos (sin textos de jugador).
5. **Mensajes:** todo texto visible al jugador en `messages/en.yml` y `messages/es.yml`.
6. **Enums:** un archivo por enum (`MaintenanceType`, `CommandBlockerMode`, etc.).
7. **Lombok:** permitido en data holders; no reemplazar lógica de servicios.
8. **common/:** utilidades + command framework únicamente; features duplicadas en velocity/bungee.
9. Utilizar nombres consistentes y validaciones explícitas.
10. Evitar comentarios que repitan lo que ya expresa el código.
11. No dejar métodos vacíos, implementaciones ficticias ni TODO que se presenten como funcionalidad terminada.
12. Ejecutar `gradlew.bat build` después de cada fase importante.
13. Features grandes = toggle en `settings.yml` → `modules.*`.
