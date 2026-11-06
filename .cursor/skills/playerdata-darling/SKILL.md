---
name: playerdata-darling
description: Create or extend PlayerData like Darling Profile (holder + Service + Sql). Use when adding player persistence, /seen fields, transfer, or profile-like data.
---

# playerdata-darling

## Pattern
1. `player/PlayerData.java` — `@Getter @Setter @Accessors(fluent = true)`.
2. `player/PlayerDataService.java` — cache + API pública.
3. `player/PlayerDataSql.java` — SQL only.

## Rules
- No narrative comments.
- No hand getters on holders.
- Mirror velocity → bungee.
- Run `prettier-always` after edits.
