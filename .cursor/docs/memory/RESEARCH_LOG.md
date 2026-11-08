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

## No verificado aquí
- Mapeos NMS exactos Paper 1.21+ para features futuras (no hay backend aún).
- LICENSE formal de Helium/CoreDub en raíz (no encontrada en audit rápido → tratar como no redistribuible).
