# Roadmap

## Queue → HubCore

El módulo de cola del proxy (`/queue`, `queue.yml`, `QueueService`) se eliminó. La cola de modalidad/hub irá en **HubCore** (backend), no en el proxy.

## Discord · ROLE-SYNC (futuro, no prioridad)

Otorgar/quitar rangos in-game y comandos según roles Discord (cuenta vinculada + LuckPerms). Requiere intent `GUILD_MEMBERS`, re-sync al login y al cambio de roles. Referencia: Helium. Bloque `ROLE-SYNC: { ENABLED: false }` comentado en `discord-community.yml`. **No implementar ahora.**

## Discord community (parcial)

Plantilla `discord-community.yml` lista. Pendiente: tickets / paneles / sorteos con persistencia SQL|Mongo real (ideas Snowfall Apache-2.0 — sin importar el repo entero).

## Discord backlog (Snowfall)

Sugerencias, verificación y antiraid: fuera de alcance. Evaluar más adelante si hace falta.

## NMS soportado

Solo: `NMS_v1_8_R3`, `NMS_v1_20_R3`, `NMS_21_R1`, `NMS_26_R1` (+ `NMS_api`). Otras revisiones eliminadas; VersionMatcher hace fallback al más cercano.
