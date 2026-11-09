---
name: post-task-hygiene
description: Checklist after IoriCore Java/YAML work before marking a task done. Includes anti-dup via repository-auditor.
---

# post-task-hygiene

Run this checklist when Java or resources changed:

1. **Anti-dup** — Leer/invocar `.cursor/agents/repository-auditor.md` sobre módulos/archivos tocados. Si hay `REAL-DUP` introducido → corregir antes de seguir.
2. **Packages** — Feature proxy duplicada bajo `velocity/` y `bungee/`; adapters solo en subpaquetes de plataforma; command framework en `common/`.
3. **Sync** — Tras editar features en velocity, copiar los mismos paths a bungee (excluir paquetes adapter).
4. **Enums** — Un enum por archivo; sin enums anidados en services.
5. **Lombok** — Fluent accessors en holders; sin getters a mano; `@RequiredArgsConstructor` en servicios finos.
6. **Comments** — Sin Javadoc narrativo; decisiones en `.cursor/docs/memory/DECISIONS.md`.
7. **Prettier** — `npx prettier --write` en Java/YAML tocados; luego blank entre métodos estilo Darling.
8. **Messages** — Strings de jugador en `messages/en.yml` y `messages/es.yml` (+ legacy si aplica).
9. **Config** — `ConfigManager.FILES` / resources shippeados alineados; una fuente de verdad por dato.
10. **Imports / dead code** — Quitar imports sin uso, métodos vacíos, clases stale.
11. **Build** — `gradlew.bat :velocity:shadowJar :bungee:shadowJar :bukkit:shadowJar --no-daemon` (JDK 17) si tocó esos módulos.
12. **Deploy** (solo si el usuario lo pidió) — `IoriVelo-*.jar` → plugins del proxy; `IoriCore-Bukkit-*.jar` → backends; borrar jars viejos del mismo artifact.

Si algún paso falla, corregir antes de dar la tarea por hecha.
