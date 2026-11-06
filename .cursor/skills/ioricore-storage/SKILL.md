---
name: ioricore-storage
description: Persistencia de IoriCore (MariaDB/MySQL, H2 fallback, MongoDB opcional, Hikari, esquema compartido ip_/ic_, migraciones, caché, Redis como bus). Usar al crear tablas, queries, servicios con datos o invalidación de caché.
---

# ioricore-storage

## Mapa actual
| Lado | Clase | Backends |
|---|---|---|
| Proxy | `database/StorageFactory` → `mysql/MySqlStorageService` (MySQL **y** H2), `mongo/…`, `NoopStorageService` · `StorageType {NONE,H2,MYSQL,MONGODB}` (default H2) | `database.yml` |
| Core | `storage/CoreSqlStorage` (H2/MySQL, implementa `api.storage.SqlStorage`) · `SoftMongoStorage` | `settings.yml → STORAGE` |

Tablas: proxy `ip_players ip_playtime ip_friends ip_friend_requests ip_account_ips ip_reports ip_helpop ip_analytics ip_schema_version` · core `ic_punishments ic_balances ic_ranks ic_rank_permissions`. `ic_punishments` la crean **ambos** lados (DDL duplicada, OPEN-QUESTIONS A8).

## Reglas
- Patrón holder + `*Service` (cache, API) + `*Sql` (solo SQL, `PreparedStatement`). Rule `15-playerdata-darling`.
- Todo async; pools Hikari acotados (3–5/instancia); cerrar recursos (try-with-resources).
- DDL `CREATE TABLE IF NOT EXISTS` idéntica en ambos lados mientras no haya runner central; cualquier cambio de esquema → versionar (`ip_schema_version`) y anotar en DECISIONS si se aprueba.
- Nueva tabla: prefijo `ic_` (core/compartida); no crear más `ip_` sin decisión.
- SQL compatible MySQL/MariaDB **y** H2 (modo MySQL). Probar ambos.
- H2 es local por proceso: no hay datos compartidos entre servidores sin MySQL.
- Ban/mute check: **fail-open** + caché; invalidar por Redis `PUNISHMENT_INVALIDATE`. Nunca polling SQL para comunicar servidores.
- Credenciales solo en YAML del servidor; nunca en logs ni en repo.
- Mongo: implementado pero no confirmado en el master prompt (A9) → no ampliar sin OK.
