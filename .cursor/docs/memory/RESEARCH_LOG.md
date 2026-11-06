# RESEARCH_LOG

## Velocity / Adventure
- JD 3.4: https://jd.papermc.io/velocity/3.4.0/
- Dev guide: https://docs.papermc.io/velocity/dev/
- Plugin messaging (anti-spoof `handled()`): https://docs.papermc.io/velocity/dev/plugin-messaging/
- Adventure Bungee platform: docs indican **unmaintained**; seguir usándolo solo para Bungee legacy con `BungeeAudiences.close()`.

## Multi-plataforma de referencia
- LuckPerms layout API/common/platform: https://luckperms.net/wiki/Contributing
- Network install LP: proxy ≠ reemplazo de backend permissions.

## Plugin messaging Spigot
- Messenger/channels: Spigot JD `org.bukkit.plugin.messaging`
- Paper messaging: https://docs.papermc.io/paper/dev/plugin-messaging/
- Riesgo: cliente puede spoofear canales custom si el proxy reenvía.

## Spigot 26.x NMS (2026-11-03)
- BuildTools: `--rev 26.1` / `26.2` / `26.3` → jars en `Documents\BuildTools\Spigot Versions`.
- CraftBukkit **sin** paquete `v26_Rx`: `org.bukkit.craftbukkit.*` + Mojang (`AnvilMenu`, `ServerPlayer`).
- AnvilMenu API idéntica 26.1.2 / 26.2 / 26.3 → un módulo `nms/v26_R1` compilado vs 26.3 (+ `libs-26.3`).
- Classfile major **69** (Java 25). Compile con toolchain 25; Lombok AP off.
- VersionMatcher: `26.*` → `v26_R1`.

## No verificado aquí
- Smoke in-game anvil en 26.1/26.2/26.3.
- LICENSE formal de Helium/CoreDub en raíz (no encontrada en audit rápido → tratar como no redistribuible).
