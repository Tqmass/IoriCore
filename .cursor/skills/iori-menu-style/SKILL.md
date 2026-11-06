---
name: iori-menu-style
description: Sistema de diseño de menús (inventarios) y lore de ítems de IoriCore, basado en la captura "Manage Death Effects" - título con id, separadores, lista con marcador de seleccionado, línea de acción aqua. Incluye temas compartidos, 3 estados (sin permiso / faltan requisitos / disponible), submenús, sonidos configurables y compatibilidad 1.8.9-1.21. Úsala SIEMPRE que crees, rediseñes, migres o revises un menú yml, un ítem con lore, un submenú, el coinshop, punish, settings-lang o cualquier GUI, aunque el usuario no diga "diseño".
---

# Diseño de menús Iori

Objetivo: que cualquier menú se vea como parte del mismo producto - oscuro, sobrio, premium, con jerarquía clara - y que el servidor pueda personalizar textos, estados y sonidos sin tocar código.

## 1. Gramática del lore (la captura de referencia)

Todo ítem interactivo sigue este esqueleto. Cada bloque es opcional salvo título y acción.

```
&9&lBlood &8(#0331)              TÍTULO: nombre en color de acento + negrita, id/contexto en &8
{sep}                            SEPARADOR (gris oscuro)
&7Descripción corta en            DESCRIPCIÓN: 1-2 líneas, &7, máx ~32 caracteres por línea
&7una o dos líneas.
{sep}
&c▪ &7None                       LISTA: opción no seleccionada = bullet rojo + texto gris
&a▸ &f&lBlood                    seleccionada = flecha verde + blanco en negrita
&c▪ &7Explosion
{sep}
&b» Click para cambiar           ACCIÓN: aqua, siempre la última línea
```

Reglas:
- `{sep}` es un token que se resuelve al cargar el menú a `&8&m<N espacios>&r` (tachado sobre espacios, funciona en 1.8). `N` se define una vez en `MENUS.STYLE.SEPARATOR-WIDTH` (por defecto 26).
- Máximo ~10 líneas por lore. Si no cabe, sobra contenido.
- Un color de acento por menú; el resto es `&8` (marcos), `&7` (texto), `&f` (resaltado).
- Símbolos seguros en 1.8: `▪ ▸ » ✔ ✖ • ● ❤ ★ ✦`. Nada de emojis; hex solo en versión moderna.
- Acento sugerido: general `&9`, sanciones `&c`, tienda `&6`, acciones `&b`, éxito `&a`.

## 2. Estructura de archivos

```
menus/
  punish.yml
  coinshop.yml
  settings-lang.yml
  submenus/
    punish-reasons.yml
    coinshop-tags.yml
```
- `ID` = nombre de archivo si no se declara. IDs únicos en todo `menus/**`; si hay duplicado, error claro con las dos rutas.
- Un submenú declara `PARENT: <id>`; `[back]` vuelve al padre.
- **Tema compartido** en `settings.yml`; cada menú lo referencia y solo pisa lo que cambia:

```yaml
# settings.yml
MENUS:
  STYLE:
    SEPARATOR-WIDTH: 26
  THEMES:
    default:
      BORDER: true
      FILLER:
        MATERIAL: GRAY_STAINED_GLASS_PANE     # el resolver mapea a STAINED_GLASS_PANE:7 en 1.8
        NAME: " "
      ACCENT-FILLER:
        MATERIAL: BLACK_STAINED_GLASS_PANE
        NAME: " "
        CORNERS: true                          # slots 0, 8, last-8, last
      SOUND:
        OPEN: MENU-OPEN
        CLICK: MENU-CLICK
        BACK: MENU-BACK
        PAGE: MENU-PAGE
```

## 3. Tres estados por ítem

Equivalente a `BaseLore + NoPermissionLore + NoAchievementLore + FinalLore` de IoriCTW Kits ES, pero genérico. Orden de resolución:

1. `NO-PERMISSION` - falla un requisito **duro** (permiso/rango).
2. `MISSING-REQUIREMENTS` - tiene permiso pero falla uno **blando** (monedas, nivel, tiempo jugado, logro).
3. `AVAILABLE` - cumple todo.

```yaml
ITEMS:
  ELITE-POTION:
    SLOT: 12
    REQUIREMENTS:
      - "PERMISSION ioricore.kit.elite"
      - "COINS 200"
      - "PLAYTIME 10h"
    BASE:                                   # se hereda en los tres estados
      NAME: "&b&lPoción Élite &8(#0012)"
      LORE:
        - "{sep}"
        - "&7Efecto potenciado para"
        - "&7partidas competitivas."
        - "{sep}"
        - "{state-lore}"                    # lo aporta el estado activo
        - "{sep}"
        - "{footer}"
    STATES:
      NO-PERMISSION:
        MATERIAL: BARRIER
        STATE-LORE: ["&c✖ &7Requiere rango &b&lELITE"]
        FOOTER: "&c» Compra rango en la tienda"
        SOUND: MENU-LOCKED
        ACTIONS: { ANY: ["[message] NO-PERMISSION"] }
      MISSING-REQUIREMENTS:
        MATERIAL: POTION
        STATE-LORE: ["{requirements}"]      # genera líneas &a✔ / &c✖ por requisito
        FOOTER: "&e» Te faltan requisitos"
        SOUND: MENU-LOCKED
      AVAILABLE:
        MATERIAL: POTION
        GLOW: true
        STATE-LORE: ["&a✔ &7Todo listo"]
        FOOTER: "&b» Click para comprar"
        SOUND: MENU-SUCCESS
        ACTIONS: { ANY: ["[internal] shop.buy elite-potion"] }
```
- Un estado puede omitir cualquier clave: hereda de `BASE`.
- `{requirements}` se autogenera con el texto de cada requisito y su marca `&a✔` / `&c✖`.
- Los textos pueden ser `NAME-KEY` / `LORE-KEY` (messages) o literales; no mezcles en el mismo ítem.

## 4. Layout estándar por filas

| Filas | Info/cabecera | Contenido | Navegación |
|---|---|---|---|
| 3 (27) | slot 4 | 10-16 | cerrar/atrás 22 |
| 4 (36) | slot 4 | 10-16, 19-25 | atrás 31 |
| 5 (45) | slot 4 | 10-16, 19-25, 28-34 | prev 39, atrás 40, next 41 |
| 6 (54) | slot 4 | 10-16 … 37-43 | prev 48, atrás 49, next 50 |

- Fila 0 y última = borde con esquinas de acento. Contenido centrado, sin pegar a los bordes.
- Slot 4 es la "tarjeta" del contexto (cabeza del jugador, objetivo, categoría) con su propio lore.
- Páginas: `PAGE-TURN`/`MENU-PAGE`; flechas desactivadas (cristal) en los extremos.

## 5. Sonidos

Los menús **solo referencian claves** de `sounds.yml`. Claves mínimas a añadir: `MENU-OPEN`, `MENU-CLICK`, `MENU-BACK`, `MENU-PAGE`, `MENU-LOCKED`, `MENU-SUCCESS`, `MENU-ERROR` (más las ya existentes). Cada clave tiene `NAMES` (lista: primero el nombre moderno, luego el legacy 1.8), `VOLUME`, `PITCH`. El primer nombre que resuelva gana; si ninguno resuelve, un solo aviso en consola con el nombre de la clave. Así cualquier sonido de Spigot se puede poner desde el yml.

## 6. Compatibilidad 1.8.9 ↔ 1.21

- **Nunca** `MATERIAL-FALLBACK` en un yml. Un único resolver con tabla de alias (`GRAY_STAINED_GLASS_PANE` ↔ `STAINED_GLASS_PANE:7`, `WRITABLE_BOOK` ↔ `BOOK_AND_QUILL`, `SKULL_ITEM`/`PLAYER_HEAD`…). `DATA` solo se usa en legacy y lo calcula el resolver.
- Cabezas: textura base64 o nombre de jugador; en 1.8 vía GameProfile.
- Sin `&#hex` ni MiniMessage en lore legacy; la versión moderna puede usar degradado en el título.

## 7. Plantillas listas

**punish** (3 filas, acento `&c`):

```yaml
ID: punish
TITLE: "&8Sancionar &7» &f{target}"
ROWS: 3
THEME: default
ITEMS:
  TARGET:
    SLOT: 4
    MATERIAL: PLAYER_HEAD            # {target}
    NAME: "&f&l{target} &8(#{short-uuid})"
    LORE:
      - "{sep}"
      - "&7Estado: &f{target-status}"
      - "&7Sanciones activas: &f{active-punishments}"
      - "{sep}"
      - "&8Elige una categoría"
  WARN:
    SLOT: 10
    MATERIAL: PAPER
    NAME: "&e&lAdvertencia &8(WARN)"
    LORE:
      - "{sep}"
      - "&7Faltas menores de chat."
      - "{sep}"
      - "&e▸ &f{count:WARN} &7motivos"
      - "&b» Click para elegir"
    ACTIONS: { ANY: ["[internal] punish.category WARN"] }
  MUTE:
    SLOT: 12
    MATERIAL: BOOK
    NAME: "&6&lSilencio &8(MUTE)"
    LORE: ["{sep}", "&7Quita el chat al jugador.", "{sep}", "&6▸ &f{count:MUTE} &7motivos", "&b» Click para elegir"]
    ACTIONS: { ANY: ["[internal] punish.category MUTE"] }
  BAN:
    SLOT: 14
    MATERIAL: REDSTONE_BLOCK
    NAME: "&c&lBaneo &8(BAN)"
    LORE: ["{sep}", "&7Expulsa de la red.", "{sep}", "&c▸ &f{count:BAN} &7motivos", "&b» Click para elegir"]
    ACTIONS: { ANY: ["[internal] punish.category BAN"] }
  KICK:
    SLOT: 16
    MATERIAL: IRON_BOOTS
    NAME: "&7&lExpulsión &8(KICK)"
    LORE: ["{sep}", "&7Saca al jugador del servidor.", "{sep}", "&7▸ &f{count:KICK} &7motivos", "&b» Click para elegir"]
    ACTIONS: { ANY: ["[internal] punish.category KICK"] }
  CLOSE:
    SLOT: 22
    MATERIAL: BARRIER
    NAME: "&c» Cerrar"
    SOUND: MENU-BACK
    ACTIONS: { ANY: ["[close]"] }
```

**punish-reasons** (submenú, plantilla única de ítem):

```yaml
ID: punish-reasons
PARENT: punish
TITLE: "&8Sancionar &7» &f{target} &8· &f{category}"
ROWS: 5
THEME: default
DYNAMIC:
  SOURCE: punish.reasons
  SLOTS: ["10-16", "19-25", "28-34"]
  PAGINATE: true
  TEMPLATE:
    MATERIAL: "{material}"
    NAME: "{name} &8(#{id})"
    LORE:
      - "{sep}"
      - "&7Motivo: &f{reason}"
      - "&7Duración: &f{duration-or-permanent}"
      - "{sep}"
      - "{state-lore}"
      - "&b» Click para aplicar"
    STATES:
      NO-PERMISSION:
        MATERIAL: BARRIER
        STATE-LORE: ["&c✖ &7No puedes aplicar esta sanción"]
        SOUND: MENU-LOCKED
      AVAILABLE:
        ACTIONS: { ANY: ["[internal] punish.apply {id}"] }
```

**settings-lang** (el idioma actual se marca como en la captura):

```yaml
ID: settings-lang
TITLE-KEY: SETTINGS.LANG.TITLE
ROWS: 3
THEME: default
DYNAMIC:
  SOURCE: settings.languages            # EN, ES… desde messages
  SLOTS: ["11-15"]
  TEMPLATE:
    MATERIAL: "{icon}"
    NAME: "&9&l{language} &8(#{code})"
    LORE:
      - "{sep}"
      - "{marker} &f{language}"          # &a▸ &f&l si es el actual, &c▪ &7 si no
      - "{sep}"
      - "&b» Click para usar este idioma"
    ACTIONS: { ANY: ["[internal] settings.set-lang {code}"] }
ITEMS:
  BACK:
    SLOT: 22
    MATERIAL: ARROW
    NAME-KEY: SETTINGS.MENU.BACK-NAME
    ACTIONS: { ANY: ["[back]"] }
```

## 8. Checklist antes de dar un menú por bueno
- [ ] Usa `THEME`, sin filler copiado.
- [ ] Todo ítem sigue la gramática: título, `{sep}`, acción aqua al final.
- [ ] Ítems con requisitos tienen los 3 estados.
- [ ] Sonidos por clave; todas existen en `sounds.yml`.
- [ ] Sin `MATERIAL-FALLBACK`; materiales válidos en 1.8 y moderno.
- [ ] Archivo en `menus/` o `menus/submenus/`; `[back]` probado.
- [ ] Descripción de 3-5 líneas de cómo se ve (filas y slots clave) para revisar sin abrir el juego.
