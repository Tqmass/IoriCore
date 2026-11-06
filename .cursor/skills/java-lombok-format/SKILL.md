---
name: java-lombok-format
description: Lombok usage, formatting, imports, and module split rules for IoriProxy Java code.
---

# java-lombok-format

## Lombok (mandatory on holders)

- `@Getter` + `@Accessors(fluent = true)` on `IoriProxy`, config classes, DTOs, command parents — **no hand-written** `platform()`, `configs()`, etc.
- `@RequiredArgsConstructor` for services with final deps when it reduces noise.
- `@Builder` only for many optional fields.
- `@UtilityClass` for static-only helpers.
- Lombok: `compileOnly` + `annotationProcessor` only.

## Comments

- **No** narrative Javadoc. Names + clean code only.
- Non-obvious why → `docs/memory/DECISIONS.md`.

## Formatting (mandatory)

- One blank line between logical blocks (import groups, fields, ctor, methods, guard vs work).
- Never blank line between every line of code.
- Short guards on one line: `if (uuid == null) return;`
- Class order: constants → fields → ctor → public API → private.
- After Prettier, still ensure blank line **between methods** and after multi-line guards.
- Import order: JDK, blank, third-party, blank, `me.tqmass.dev.proxy.*`.
- **Always** run (skill `prettier-always`):
  `npx prettier --write "velocity/src/**/*.{java,yml,yaml}" "bungee/src/**/*.{java,yml,yaml}" "common/src/**/*.{java,yml,yaml}"`

## Packages

- `commands/` commands + arguments only.
- `modules/` lifecycle modules.
- `server/` routing/lobby/transfers/servers.
- `player/` friends/playtime/player-facing.
- `common/` utils + command framework only.
- Mirror velocity ↔ bungee feature trees.
