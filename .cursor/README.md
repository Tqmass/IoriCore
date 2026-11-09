# IoriCore `.cursor`

Índice canónico: [`INDEX.md`](INDEX.md)

## Loop autónomo
Regla `rules/85-autonomous-dev-loop.mdc` · anti-dup `agents/repository-auditor.md` · higiene `skills/post-task-hygiene` · hook `hooks.json` (`stop`).

Flujo: analizar → auditar → planificar → coordinar → implementar → anti-dup → build/test → corregir → entregar.

## Build / deploy
```bat
gradlew.bat :velocity:shadowJar :bungee:shadowJar :bukkit:shadowJar --no-daemon
```
- `IoriVelo-*.jar` → plugins del proxy (borrar `IoriVelo-*` / `IoriProxy-*` viejos).
- `IoriCore-Bukkit-*.jar` → backends / Desktop.

## Prettier
```bat
npx prettier --write "velocity/src/**/*.{java,yml,yaml}" "bungee/src/**/*.{java,yml,yaml}" "bukkit/src/**/*.{java,yml,yaml}" "common/src/**/*.{java,yml,yaml}" "api/src/**/*.{java,yml,yaml}"
```

## Brand
RoyalCTW · mensajes modern/legacy · Lombok fluent · estilo Darling · NMS solo en Core (nunca en proxy).
