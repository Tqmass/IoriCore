---
name: nuevo-modulo
description: Crea un módulo/feature IoriProxy al estilo Darling (paquetes, impl, enums, commands). Usar al añadir features nuevas.
---

# nuevo-modulo

## Pasos
1. Definir paquete `me.tqmass.dev.proxy.<feature>/`.
2. Crear:
   - servicio/manager
   - `enums/` → un archivo por enum
   - `command/` + `argument/` si hay subcomandos
   - `impl/` para listeners/adapters de plataforma (o en `velocity/.../impl` / `bungee/.../impl`)
3. Toggle en `settings.yml` → `modules.<feature>`.
4. Textos en `messages`.
5. Wire en `IoriProxy` enable/reload.
6. Duplicar/sync o mover lógica pura a `common/` según reglas.
7. Build.
