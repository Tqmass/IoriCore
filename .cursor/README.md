# IoriProxy `.cursor`

Índice canónico: [`INDEX.md`](INDEX.md)

## Build / deploy
```bat
gradlew.bat :velocity:shadowJar :bungee:shadowJar --no-daemon
```
Copiar `IoriProxy-Velocity-*.jar` a:
`C:\Users\TqmassPC\Desktop\Minecraft Project\Servidores\1. Proxy\plugins`
(borrar jars `IoriProxy-*.jar` viejos antes).

## Prettier
```bat
npx prettier --write "velocity/src/**/*.{java,yml,yaml}" "bungee/src/**/*.{java,yml,yaml}" "common/src/**/*.{java,yml,yaml}"
```

## Brand
RoyalCTW · mensajes modern/legacy · Lombok fluent · estilo Darling · sin NMS en proxy.
