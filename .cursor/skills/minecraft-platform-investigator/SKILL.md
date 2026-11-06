---
name: minecraft-platform-investigator
description: Investigates Velocity, BungeeCord, Spigot, Paper, Folia APIs and event order before coding. Use when unsure about proxy/backend API, events, messaging, or version differences.
---

# minecraft-platform-investigator

## Before inventing APIs
1. Check official docs / JD (PaperMC, Velocity, SpigotMC).
2. Prefer evidence in local `_refs` / `_decompiled`.
3. Note version matrix (1.8.8 → 1.21+, Velocity 3.x, Bungee).

## Output
- Qué API existe de verdad
- Diferencias Velocity vs Bungee vs Paper
- Riesgos (event loop block, plugin message spoof)

No inventar métodos. Si falta evidencia, decirlo en una línea.
