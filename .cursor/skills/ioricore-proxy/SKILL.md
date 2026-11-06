---
name: ioricore-proxy
description: Desarrollo en IoriVelo/IoriBungee (Velocity, BungeeCord, routing, Redis pub/sub, plugin messaging, Discord JDA, configs proxy). Usar al tocar velocity/, bungee/ o lógica de red.
---

# ioricore-proxy

Raíces: `velocity/src/main/java/us/iorilabs/proxy/` y `bungee/…/proxy/` · núcleo `IoriProxy` (+ `IoriProxyVelocity` / `IoriProxyBungee`, `VelocityPlatform` / `BungeePlatform`).
Reglas: `.cursor/rules/20-proxy-compat.mdc`, `50-seguridad-rendimiento.mdc` · skills Cursor `adaptador-plataforma`, `plugin-messaging-seguro`, `proxy-performance-optimization` (leer solo si aplica).

## Duplicación (estado actual)
Todo lo que no está en `proxy/velocity/**` o `proxy/bungee/**` es **idéntico** en ambos módulos. Al editar una feature: editar en `velocity`, copiar el fichero idéntico a `bungee` y verificar con `cmp`. Adaptadores (listeners, comandos, player) sí difieren. Extraer `proxy-common` está pendiente de OK (OPEN-QUESTIONS A3); no hacerlo sin aprobación.

## Piezas
- Comandos: `commands/*Command` sobre `CommandParent` (common); Velocity `VelocityBrigadierCommand`, Bungee `BungeeProxyCommand`. Case-insensitive. Permisos `ioriproxy.*`.
- Config: `config/*Config` + `ConfigManager` (resources `settings commands permissions database redis discord donations fallback maintenance motd queue .yml`, `webhooks/*.yml`).
- Idioma: `language/` con `messages/{en,es}.yml` + `messages/legacy/`.
- Redis: `redis/RedisService` (`JedisRedisService` / `NoopRedisService`), eventos `ProxyEventType`, `RedisEvent`. Payload pequeño, ignorar origen propio.
- Plugin messaging: listeners `impl/listener/*PluginMessageListener` — canal `ioricore:main`, marcar handled/cancelled **antes** de procesar, aceptar solo origen backend.
- Discord: `discord/` (JDA, único proceso de la red). Webhooks `webhooks/` (HTTP async).
- Ban-check: `punishment/BanCheckService` + `BanCheckCache` (fail-open).

## Reglas
- Nunca bloquear el event loop: SQL/Redis/HTTP async.
- Velocity: Adventure nativo. Bungee: `BungeeAudiences` (cerrar en disable).
- Verificar APIs en JD Velocity 3.4 / Bungee antes de usarlas; no inventar.
- Nada de Bukkit/NMS. Nada que pertenezca al Core.
