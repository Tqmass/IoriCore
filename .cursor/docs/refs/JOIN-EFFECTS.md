# JoinEffects → módulo IoriCore (Bukkit)

Ref local: `.cursor/_refs/JoinEffects/` (CFR + YAML).

## Adoptar (ideas)

- Menú de **un efecto activo** por jugador + permiso por efecto + botón “desactivar todo”.
- Claves de persistencia estables (`firework`, `zeus`, volcanes…) en PlayerData / SQL, no solo YAML suelto.
- Efectos como **listeners + runnables** con cleanup si el jugador se desconecta (patrón `FallingBlock` / entidades temporales).
- Fuegos artificiales: tag UUID + cancelar `EntityDamageByEntityEvent`.
- Volcanes: mezcla de **partículas (`Particle.DustOptions`)**, **sonidos globales cercanos** y entidades visuales sin romper bloques del mapa.
- GUI YAML-driven (icono, lore `%status%`, skull custom) al estilo IoriCTW menús.
- Adaptador de skull/profile solo si hace falta legacy 1.8–1.12 en Core (IoriCore NMS wrapper, no copiar `VersionManager`).

## Evitar / cautela

- **Licencia**: Ivan FX / Spigot — sin código copiado; reescribir APIs Bukkit/Paper propias.
- **LicenseManager** y telemetría a `ivanfx.lol`.
- MySQL con columnas `ALTER` dinámicas — preferir esquema IoriCore (`ic_` / PlayerData).
- Emojis en nombres GUI del original; RoyalCTW puede usar MiniMessage/legacy según skill UI.

## Efectos detectados (7)

1. Fireworks  
2. Zeus (trueno visual)  
3. Zeus Fireworks  
4. Ender Teleport  
5. Wool Volcano  
6. TNT Volcano  
7. Mining Volcano (“Millionaire”)
