# IoriProxy — Instalación local

## Requisitos

- **Java 17** en el host del proxy (Velocity o BungeeCord).
- **Redis** (opcional): presencia, staff chat, maintenance sync, eventos de red.
- **MySQL/MariaDB** (opcional): playtime, analytics, blacklist persistente, reportes.
- **MongoDB** (opcional): alternativa a MySQL si `DATABASE: MONGODB` en `database.yml`.

## JARs de salida (Gradle shadow)

Tras `gradlew.bat build --no-daemon`:

| Archivo | Destino |
|---------|---------|
| `velocity/build/libs/IoriProxy-Velocity-<version>.jar` | `plugins/` de **Velocity** |
| `bungee/build/libs/IoriProxy-Bungee-<version>.jar` | `plugins/` de **BungeeCord** |

No mezcles el jar de Velocity en Bungee ni viceversa. Cada jar incluye `common` y dependencias empaquetadas (Hikari, Jedis, SnakeYAML, Gson, drivers SQL/Mongo, Adventure).

## Configuración

En el primer arranque se generan bajo la carpeta del plugin:

- `settings.yml` — language, debug, legacy-max-version, playtime, modules.
- `database.yml` — `DATABASE` type + MySQL/Mongo credenciales (pool Hikari hardcoded).
- `permissions.yml` — nodos de permiso + STAFF.GROUPS.
- `messages/en.yml`, `messages/es.yml` (+ legacy/) — textos MiniMessage / legacy.
- YAMLs por feature (`motd.yml`, `donations.yml`, `announcements.yml`, …).
- `webhooks/*.yml` — plantillas Discord.

Credenciales Redis/SQL solo en YAML local, nunca en el repo. Origin Redis fijo: `proxy` (sin multiproxy id).

## Redis

Ver `redis.yml` (`ENABLED`, `HOST`, `PORT`, auth…).

## MySQL

Ver `database.yml`:

```yaml
DATABASE: MYSQL # MYSQL | MONGODB | NONE
MYSQL:
  HOST: 127.0.0.1
  PORT: 3306
  DATABASE: ioriproxy
  USERNAME: ...
  PASSWORD: ...
```

Las migraciones se aplican al habilitar storage SQL.

## Comprobación rápida

1. Arranca el proxy con el jar correcto.
2. Revisa el banner del plugin en consola.
3. Con Redis: `/redistest` (permiso staff).
4. Ajusta `MODULES.*` en `settings.yml` y recarga.

## Permisos

Prefijo `ioriproxy.*`. Lista completa en `permissions.yml`.
