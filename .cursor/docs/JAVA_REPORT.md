# IoriProxy — Informe Java / stack

## Runtime y toolchain

| Item | Valor |
|------|--------|
| Lenguaje | Java **17** (`release 17` en Gradle) |
| Build | Gradle 8.x, multi-módulo |
| Group / version | `me.tqmass.dev.proxy` / `1.0.0-SNAPSHOT` |

## Módulos Gradle

| Módulo | Rol |
|--------|-----|
| `common` | Comandos, utilidades, enums compartidos. Sin imports Velocity/Bungee. |
| `velocity` | Plugin Velocity 3.4.x API (`compileOnly`), shadow **IoriProxy-Velocity**. |
| `bungee` | Plugin BungeeCord API (`compileOnly`), shadow **IoriProxy-Bungee**. |

Duplicación intencional de features en `velocity/` y `bungee/` con adaptadores de plataforma (`VelocityPlatform`, `BungeePlatform`).

## Bibliotecas embebidas (shadow)

Relocadas bajo `me.tqmass.dev.proxy.libs.*`:

- **HikariCP** — pool MySQL
- **mysql-connector-j** — JDBC
- **mongodb-driver-sync** — Mongo opcional
- **Jedis** — Redis
- **SnakeYAML 2.3** — configs
- **Gson** — JSON (eventos, maintenance, webhooks)
- **Adventure** (API + MiniMessage + legacy + gson en Velocity; Bungee añade **adventure-platform-bungeecord** relocada)

APIs de proxy (**Velocity**, **BungeeCord**) quedan `compileOnly` — las provee el servidor.

## Adventure

- Velocity: Adventure nativo del API.
- Bungee: `BungeeAudiences` (cerrar en disable del plugin).

Textos de jugador en `messages/*.yml` con MiniMessage.

## Hooks externos (soft, solo si hace falta)

No hay dependencia Gradle de LuckPerms / PlaceholderAPI / LiteBans.
Cuando una feature lo necesite: hook opcional por reflexión/API `compileOnly`, nunca hard-require del plugin.

## Dual platform — implicaciones

- Un solo `common` evita bifurcar lógica de negocio.
- Cualquier feature que toque API del servidor vive en dos árboles espejo; cambios en uno deben reflejarse en el otro (salvo `velocity/` vs `bungee/` adapters).
- Jars finales independientes; no hay fat-jar único.
