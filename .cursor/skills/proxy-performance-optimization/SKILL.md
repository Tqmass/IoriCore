---
name: proxy-performance-optimization
description: Optimize IoriProxy against stats overload, Redis/SQL pressure, chat spam, queues, and cross-proxy fan-out so the proxy stays stable under load.
---

# proxy-performance-optimization

## Purpose

Evitar que analíticas, presencia, anuncios, reportes, friends o Discord tumben el proxy.

## Instructions

1. **Nunca bloquear el event loop / hilo principal** con Redis, SQL, Mongo, HTTP (Discord/Tebex) o I/O de disco pesado.
2. **Batch writes**: playtime, analytics y presencia se acumulan en memoria y se flushean por intervalo configurable, no por cada tick/evento si no es crítico.
3. **Rate limits** en report, helpop, stream, friend requests, staff spam y GG wave.
4. **Backpressure Redis**: payloads pequeños, schema versionado, dedupe por `event_id`, ignorar origen propio, no loguear cuerpos sensibles.
5. **SQL**: índices, prepared statements, pool Hikari acotado, queries agregadas (no full scan histórico en `/analytics`).
6. **Presencia**: hash/set por proxy + heartbeat; reconciliación al reconectar; no confiar solo en pub/sub.
7. **Broadcasts**: un scheduler de anuncios por grupo/servidor, no un timer por jugador.
8. **Countdown maintenance**: una tarea coordinada, no N tareas × jugadores × segundo.
9. **Discord webhooks**: cola async, timeout, reintento limitado, circuit breaker si falla.
10. **TabCompleteFilter**: barato (set lookups), sin I/O.
11. **Queue**: estructuras concurrentes; prioridades O(log n) o buckets; métricas de tamaño.
12. **Placeholders**: cache corta TTL para valores caros (playtime, ping).
13. **Métricas internas**: contadores de eventos/s, tamaño de colas, latencia Redis/SQL; alertar en logger sin flooded spam.
14. Medir antes de “optimizar”; preferir límites de config a micro-optimizaciones opacas.
15. Si una feature puede generar fan-out O(players × proxies), documentar coste y añadir throttle.
