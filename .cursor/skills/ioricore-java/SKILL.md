---
name: ioricore-java
description: Estilo Java de IoriCore (Darling, Lombok, formato, orden de clase, paquetes, Prettier). Usar al escribir o revisar cualquier fichero .java del proyecto.
---

# ioricore-java

Fuentes (leer solo la sección necesaria): `.cursor/rules/05-comentarios.mdc`, `10-estilo-darling.mdc`, `15-playerdata-darling.mdc`, `60-formato-lombok-paquetes.mdc`, `80-prettier-obligatorio.mdc`, `.cursor/docs/memory/STYLE_GUIDE.md`.
Ref Darling: `D:\Minecraft\Plugins\SoupPvP\Darling\src\main\java\me\vals\darling` → `profile/Profile.java`, `profile/enums/`, `utils/command/` (CommandParent/Argument), `map/*/command/argument/`. Solo patrones; no copiar.

## Reglas
- Feature = paquete: servicio/manager, `command/` + `command/argument/`, `impl/`, `enums/` (un enum por fichero). Nombres `*Service *Manager *Command *Argument *Listener *Module *Sql`.
- Lombok (`compileOnly` + `annotationProcessor`): holders `@Getter @Setter @Accessors(fluent = true)`; servicios `@RequiredArgsConstructor`; `@UtilityClass` estáticos. Prohibido getters manuales en holders.
- Datos persistentes: holder + `*Service` (cache/API) + `*Sql` (solo SQL).
- Orden: constantes static → campos → ctor → API pública → privados/helpers.
- Formato: blank entre bloques lógicos y entre métodos; nunca entre cada línea; guards de 1 línea `if (x == null) return;`. Imports JDK → terceros → `us.iorilabs.*`.
- Cero Javadoc narrativo; comentario solo para un porqué no obvio. Decisiones → `DECISIONS.md`.
- Sin clases Dios (>~400 LOC = partir por responsabilidad), sin abstracciones sin uso, sin métodos vacíos presentados como funcionalidad.
- Comandos: `CommandParent`/`CommandArgument`/`CommandSource` (`common/.../commands`); Bukkit vía `BukkitCommandBridge`/`BukkitCommandSource`. Lógica en el servicio, no en el comando.
- I/O (SQL, HTTP, Redis, disco) siempre async; volver al hilo principal para tocar jugadores/mundo.
- Java: proxy/common/bukkit `release 17`; `api`/`nms-api`/nms ≤1.20 `release 8` → no usar APIs >8 ahí.

## Cierre
`npm run format` (o `npx prettier --write` sobre los ficheros tocados) → restaurar aires Darling → `gradlew.bat build --no-daemon` con JDK 17.
