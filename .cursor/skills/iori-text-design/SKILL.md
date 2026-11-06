---
name: iori-text-design
description: Diseño de texto en pantalla para IoriCore e IoriProxy - TAB (header/footer/lista) con layouts distintos para 1.8.9 y 1.20+, chat format con una sola definición, help paginado con barra de cierre, anuncios con <center> y barras compartidas. Úsala SIEMPRE que toques tab.yml, chat-formats.yml, messages (help, anuncios, bloques con barras), centrado de texto o cualquier cabecera/pie visible para el jugador, aunque no se pida "diseño".
---

# Diseño de texto Iori

Estilo: oscuro, sobrio, premium. Grises `&8/&7` para marcos y texto, **un** color de marca, blanco para resaltar. Marca actual: **RoyalCTW** (`play.royalctw.net`); si cambia, se cambia en un sitio (`{brand}`), no en cada línea.

## 1. Versiones de cliente

| Layout | Clientes | Permite |
|---|---|---|
| `LEGACY` | ≤ 1.12.2 (incluye 1.8.9) | solo `&` clásico, símbolos seguros `▪ ▸ » • ● ✔ ✖ ❤ ★ ✦`, `&m` sobre espacios |
| `MODERN` | ≥ 1.13 | MiniMessage (`<gradient>`, `<#hex>`); 1.13-1.15 degradan hex al color más cercano |

Detección: ViaVersion/ProtocolSupport si están, si no el protocolo que reenvíe el proxy. Si no se sabe, **LEGACY** (funciona en todos).

## 2. TAB (`tab.yml`)

Estructura propuesta (reemplaza `MODERN: HEADER/FOOTER` + `LEGACY: HEADER/FOOTER` sueltos):

```yaml
# *************************************
# *         IoriCore | TAB            *
# *************************************
# ~ Placeholders: {player} {online} {max} {server} {mode} {ping} {world} {rank} {playtime} {coins} {brand} {promo}
# ~ {promo}: si está vacío, su línea desaparece (sin hueco)
# ~ LEGACY = clientes ≤1.12.2 (1.8.9 incluido) · MODERN = ≥1.13 (MiniMessage)

# ────────────────────────────
# SETTINGS
# ────────────────────────────
SETTINGS:
  UPDATE-TICKS: 40
  CENTER: true                 # rellena con espacios según la línea más ancha
  FALLBACK-LAYOUT: LEGACY

# ────────────────────────────
# TEXTOS COMUNES
# ────────────────────────────
TEXT:
  BRAND: "RoyalCTW"
  PROMO: ""                    # ej. "&c&l50% OFF! &7@ &6store.royalctw.net"
  MODES:
    LOBBY: "Lobby"
    CTW: "Capture The Wool"
    DEFAULT: "Network"

# ────────────────────────────
# ANIMACIÓN (opcional)
# ────────────────────────────
ANIMATION:
  ENABLED: true
  INTERVAL-TICKS: 6
  FRAMES-LEGACY:               # destello recorriendo ROYAL; {brand} usa el frame activo
    - "&f&lR&c&lOYAL&f&lCTW"
    - "&c&lR&f&lO&c&lYAL&f&lCTW"
    - "&c&lRO&f&lY&c&lAL&f&lCTW"
    - "&c&lROY&f&lA&c&lL&f&lCTW"
    - "&c&lROYA&f&lL&f&lCTW"
    - "&c&lROYAL&f&lCTW"
    - "&c&lROYAL&f&lCTW"
    - "&c&lROYAL&f&lCTW"

# ────────────────────────────
# LAYOUT LEGACY (1.8.9)
# ────────────────────────────
LEGACY:
  HEADER:
    - ""
    - "&8&m      &r  {brand} &b&lNETWORK  &8&m      &r"
    - "&7{mode} &8• &f1.8 &7- &f1.21"
    - ""
  FOOTER:
    - ""
    - "&7Estás en &b{server} &8• &a{online}&8/&f{max} &8• &f{ping}ms"
    - "&c&lTIENDA &8» &fstore.royalctw.net"
    - "&9&lDISCORD &8» &fdiscord.gg/royalctw"
    - "{promo}"
    - ""
  PLAYER-LIST: "{rank_prefix}{player}{rank_suffix}"

# ────────────────────────────
# LAYOUT MODERN (1.20+)
# ────────────────────────────
MODERN:
  HEADER:
    - ""
    - "<gradient:#FF3B3B:#FF9A3B><bold>ROYAL</bold></gradient><white><bold>CTW</bold></white> <dark_gray>•</dark_gray> <gradient:#4FC3F7:#6C63FF><bold>NETWORK</bold></gradient>"
    - "<gray>{mode} <dark_gray>•</dark_gray> <white>1.8</white> <dark_gray>–</dark_gray> <white>1.21</white>"
    - ""
  FOOTER:
    - ""
    - "<gray>Estás en <aqua>{server}</aqua> <dark_gray>•</dark_gray> <green>{online}</green><dark_gray>/</dark_gray><white>{max}</white> <dark_gray>•</dark_gray> <white>{ping}ms</white>"
    - "<gradient:#FFD54F:#FF8F00><bold>TIENDA</bold></gradient> <dark_gray>»</dark_gray> <white>store.royalctw.net</white>"
    - "<gradient:#7289DA:#99AAFF><bold>DISCORD</bold></gradient> <dark_gray>»</dark_gray> <white>discord.gg/royalctw</white>"
    - "{promo}"
    - ""
  PLAYER-LIST: "{rank_prefix}{player}{rank_suffix}"
```

Notas de implementación:
- Header/footer con API nativa cuando exista (Paper 1.13+); en 1.8 por paquete (NMS/ProtocolLib). Las líneas se unen con `\n`.
- Si `{promo}` está vacío, su línea se elimina (no dejar líneas en blanco extra).
- Lista de jugadores ordenada por peso de rango con equipos de scoreboard. En 1.8 el prefijo de equipo tiene **límite de 16 caracteres**: recorta de forma segura y no pises otros plugins de scoreboard.
- Ejemplo de referencia de estilo del usuario: `'&r       &f• &c&lIORI&f&lLABS &b&lPROJECT &f•    &r  '` y `'    &c&l&o50% OFF! &7@ &6&oiorilabs.com'`. Con `CENTER: true` no hacen falta los espacios a mano.

## 3. Chat format (`chat-formats.yml`)

Problema: 12 bloques casi iguales (owner/staff/default × EN/ES × modern/legacy). Solución: **una definición**; el idioma solo está en los hover (vienen de messages) y legacy degrada solo.

```yaml
# *************************************
# *      IoriCore | Chat formats      *
# *************************************
# ~ {TAG} {RANK} {PLAYER} {MESSAGE} + Vault/LuckPerms/PAPI
# ~ Las partes vacías se colapsan (sin dobles espacios). Hover: claves de messages (CHAT.*)

LAYOUT: "{tag}{rank}{player}{separator}{message}"

PARTS:
  tag:
    TEXT: "{TAG}"
    HOVER-KEY: CHAT.TAG-HOVER
  rank:
    TEXT: "{RANK-COLOR}%vault_prefix% "
    HOVER-KEY: CHAT.RANK-HOVER
    CLICK: { TYPE: OPEN_URL, VALUE: "https://store.example.com" }
  player:
    TEXT: "{NAME-COLOR}{PLAYER}"
    HOVER-KEY: CHAT.PLAYER-HOVER        # rango, playtime, ping
    CLICK: { TYPE: SUGGEST_COMMAND, VALUE: "/msg {PLAYER} " }
  separator:
    TEXT: " &8» "
  message:
    TEXT: "{MESSAGE-COLOR}{MESSAGE}"

FORMATS:                                # el primero cuyo PERMISSION cumple gana
  owner:
    PERMISSION: ioricore.chat.format.owner
    RANK-COLOR: "&4"
    NAME-COLOR: "&4&l"
    MESSAGE-COLOR: "&f"
  staff:
    PERMISSION: ioricore.chat.format.staff
    RANK-COLOR: "&c"
    NAME-COLOR: "&c"
    MESSAGE-COLOR: "&f"
  default:
    RANK-COLOR: "&7"
    NAME-COLOR: "&f"
    MESSAGE-COLOR: "&7"
```

Resultado esperado: `&8(&c❤&8) &4Owner &4&lTqmass &8» &fhola` y `&7Miembro &fJugador &8» &7hola`.

Reglas:
- Un `FORMAT` solo pisa colores o partes concretas; nunca duplica `PARTS`.
- Legacy: hex/MiniMessage se convierten al color más cercano; hover y click se mantienen (1.8 los soporta con JSON chat).
- `{MESSAGE}` es la única parte que **GGWAVE** decora; no se toca `LAYOUT` ni `NICK-FORMAT`.

## 4. Help paginado y barras

Estructura de **todas** las páginas (la genera el código, las páginas solo traen contenido):

```
<bar>
&b&lProxy &8» &7Comandos
(contenido)

&8[◀] &7Página &f1&7/&f3 &8[▶]
<bar>
```
- `BAR` se define **una vez** (`<bar>` como `<prefix>`), en una sola variante; no repetir `&7&m----…&r` por el archivo.
- Prev/next clicables, desactivados (gris) en los extremos. Claves: `HELP-HEADER`, `HELP-FOOTER`, `HELP-PAGE-LABEL`, `HELP-PREV-HOVER`, `HELP-NEXT-HOVER`.
- La misma estructura en `messages/legacy`.

## 5. Anuncios y `<center>`

- Cada línea centrada = `<center>…</center>`, una por línea, sin depender de espacios manuales.
- El ancho se calcula sobre el texto **sin** códigos de color (`&x`) y con anchos de fuente reales (negrita +1 px).
- Si una línea falla solo al final de un bloque `|` de YAML: sospecha de `\n` final, de `\r`, o de que el linkificador recorta los espacios iniciales de una URL. Reproduce con test unitario antes de tocar el código; el test queda.
- Las URLs largas que superen el ancho útil del chat se parten por diseño, no se centran.

## 6. Checklist
- [ ] Probado mentalmente en 1.8.9 (sin hex, símbolos seguros) y en 1.21.
- [ ] Una sola variante de barra, definida una vez.
- [ ] Sin líneas en blanco extra cuando un placeholder está vacío.
- [ ] Ningún texto duplicado entre idiomas salvo en messages.
