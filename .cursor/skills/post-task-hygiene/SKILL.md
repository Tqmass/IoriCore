---
name: post-task-hygiene
description: Checklist after IoriProxy Java/YAML work before marking a task done.
---

# post-task-hygiene

Run this checklist when Java or resources changed:

1. **Packages** — Feature code duplicated under `velocity/` and `bungee/`; adapters only in `velocity/` or `bungee/` subpackages; command framework in `common/`.
2. **Sync** — After editing velocity feature files, copy the same paths to bungee (exclude adapter packages).
3. **Enums** — One enum per file; no nested enums in services.
4. **Lombok** — Fluent accessors on holders; no hand-written getters; `@RequiredArgsConstructor` on thin services.
5. **Comments** — No narrative Javadoc; decisions in `.cursor/docs/memory/DECISIONS.md`.
6. **Prettier** — `npx prettier --write` on touched Java/YAML; then Darling blank-between-methods.
7. **Messages** — Player strings in `messages/en.yml` and `messages/es.yml`.
8. **Config** — `ConfigManager.FILES` matches shipped resources.
9. **Imports / dead code** — Remove unused imports, empty methods, stale classes.
10. **Build** — `gradlew.bat :velocity:shadowJar :bungee:shadowJar --no-daemon` (JDK 17).
11. **Deploy** — Copy `IoriProxy-Velocity-*.jar` to
    `C:\Users\TqmassPC\Desktop\Minecraft Project\Servidores\1. Proxy\plugins`
    (delete old `IoriProxy-*.jar` first).

If any step fails, fix before reporting completion.
