# MENUS — sistema de menús IoriCore-Bukkit

Auditoría 2026-11-03 · fuente de estado real = código (`bukkit/src/main/**`). Intención = `.cursor/docs/memory/DECISIONS.md`, `plan/MODULE-INVENTORY.md`.
Rutas Java relativas a `bukkit/src/main/java/us/iorilabs/bukkit/`; resources relativas a `bukkit/src/main/resources/`.
Relacionados: [RANKS](RANKS.md) · [STAFFMODE](STAFFMODE.md) · [CONFIGURATIONS](CONFIGURATIONS.md) · [PERMISSIONS](PERMISSIONS.md) · [CORE-MODULES](CORE-MODULES.md) · [FEATURE-STATUS](FEATURE-STATUS.md).

---

## 1. Estado actual

### 1.1 Dos frameworks coexistiendo

| Framework | Clases | Registrado | Usado por |
|---|---|---|---|
| **A · `menu.*` (clásico)** | `menu/Menu.java` (InventoryHolder, `title/rows/populate`), `MenuItem` (stack + `Consumer<ClickContext>`), `MenuListener`, `MenuConfig`, `MenuFiller`, `MenuColors` | `IoriCorePlugin.java:128` (`registerEvents(new MenuListener())`) | **Todos** los menús concretos (9 clases) |
| **B · `menu.api.*` (estilo Button)** | `api/Menu.java` (`getTitle/getButtons/getSize`, `openMenu`, `autoUpdate`, `updateAfterClick`), `Button`, `ButtonListener`, `MenuHolder`, `MenuApi` (sesiones + task 20 ticks), `SimpleMenu`, `ConfirmationMenu`, `ButtonSound`, `button/{Action,Back,Close,Display,Filler}Button` | `IoriCorePlugin.java:129` (`MenuApi.enable`) | **Nadie** fuera del propio paquete (grep `extends Menu|SimpleMenu` en `modules/`, `ranks/`) |
| Anvil input | `menu/anvil/AnvilInput.java` → `VersionWrapper.openAnvilInput` | vía abstraction | `RankEditMenu`, `SettingsMenu` |

Diferencias A vs B:

| Aspecto | A `menu.Menu` | B `menu.api.Menu` |
|---|---|---|
| Apertura | `open(player)`: crea inventario nuevo cada vez (`Menu.java:22-30`) | `openMenu(player)`: reutiliza holder si mismo tamaño+título (`api/Menu.java:116-122`), fuerza hilo principal (`:44-47`) |
| Título | `MenuColors.legacy(title)` **sin recorte** (`Menu.java:24`) | recorte a 32 chars (`api/Menu.java:21,144-146`) |
| Clic | cancela todo; solo slots del inventario superior; sonido `SOUND.CLICK` si el item tiene acción (`MenuListener.java:22-34`) | `shouldCancel/shouldUpdate` por botón; bloquea shift/doble-click desde inventario inferior (`ButtonListener.java:28-31`) |
| Cierre / quit | sin hooks | `onClose`, untrack en quit (`ButtonListener.java:67-82`) |
| Auto-refresh | no | `autoUpdate` cada 20 ticks (`MenuApi.java:16,97-107`) |
| Fillers | `fill`, `paintBorder`, `paintSlots` (`Menu.java:41-67`) | `fill`, `border`, `corners`, placeholder (`api/Menu.java:91-114`) |
| YAML | `MenuConfig` | ninguno |

### 1.2 Creación, apertura y navegación

- Cada menú es una clase Java; se instancia con `new XMenu(plugin, language, ranks/store/shop, menus, …).open(player)`.
- Entrada por comando: `/ranks` → `RanksCommand.java:59` (permiso `ioricore.ranks` o `ioricore.admin`, `:42`) · `/settings` (alias `preferencias`, permiso `ioricore.settings`, `SettingsCommand.java:35,54`) · `/coinshop` (`ioricore.coinshop`/`ioricore.admin`, `CoinshopCommand.java:40,50`).
- **Back** = botón que hace `new MenuPadre(...).open()` (sin pila de historial). Ej.: `RankEditMenu.java:252` (BACK→RankList), `SettingsToggleMenu.java:97`, `CoinshopCategoryMenu.java:88`.
- **Paginación: NO existe.** Listas dinámicas recorren `CONTENT-SLOTS` y hacen `break` al llenarse (`RankListMenu.java:62-74`, `CoinshopCategoryMenu.java:74-86`, `RankParentsMenu.java:70-82`). Lo que no cabe no se muestra.
- Refresh tras acción = volver a llamar `open(player)` (inventario nuevo).

### 1.3 Clics

| Tipo | Soporte |
|---|---|
| Cualquier clic | `MenuItem.handle(player, ClickType)` recibe el `ClickType` real (`MenuItem.java:26-29`) |
| Diferenciado por tipo | **solo** `RankEditMenu.handleColorClick`: SHIFT_LEFT/SHIFT_RIGHT → anvil; resto → ciclo de 16 colores (`RankEditMenu.java:259-273`) |
| Drag | cancelado (`MenuListener.java:37-42`) |
| Hotbar/number key, middle, drop | llegan como `ClickType` pero ningún menú los distingue |

### 1.4 Acciones existentes (todas en Java, ninguna en YAML)

| Acción | Dónde |
|---|---|
| Abrir otro menú | todos (lambdas `new XMenu().open`) |
| Cerrar | `CoinshopMenu.java:69-74` (CLOSE) · `player.closeInventory()` en Confirm SUCCESS |
| Toggle booleano + persistir + mensaje + reabrir | `RankEditMenu.toggle` (`:366-371`), `SettingsToggleMenu.java:73-95`, PREMIUM `SettingsMenu.java:100-120` |
| Input texto (anvil) + validación | `RankEditMenu.openAnvil` (`:373-387`; name regex `[A-Za-z0-9_\-]+` ≤32, display ≤64, weight 0-100000, prefix/suffix ≤64, color `[A-Za-z0-9_\-#]+`) · password nLogin (`SettingsMenu.java:130-154`) |
| Compra con economía | `CoinshopConfirmMenu.confirm` → `shop.purchase` + mensajes por `PurchaseResult` (`:88-117`) |
| Seleccionar idioma | `SettingsLangMenu.select` (`:73-81`) |
| "Próximamente" | Parents ADD/REMOVE solo envían `PARENT-ADD-SOON`/`PARENT-REMOVE-SOON` (`RankParentsMenu.java:79,87`) |

No hay: comandos player/console desde menú, give item, condiciones de visibilidad, permisos por botón, sonidos por botón (A), animaciones.

### 1.5 Condiciones, permisos, placeholders, sonidos

- **Permisos:** solo al abrir (comando). Dentro de `RankEditMenu` cualquier portador de `ioricore.ranks` puede togglear `OP`/`ADMIN` (`:220-229`) — ver [PERMISSIONS](PERMISSIONS.md) / riesgo §6.
- **Condiciones:** ninguna declarativa. Estado dinámico solo en lore (ej. coinshop `OWNED-LINE` vs `BUY-LINE`, `CoinshopCategoryMenu.java:109-110`).
- **Placeholders:** `Map<String,String>` por menú, sustituidos por `CoreLanguage.substitute` en formatos `<k>`, `{k}`, `%k%` (+minúsculas) y `<prefix>` (`language/CoreLanguage.java` ~`:159-176`). Sin PlaceholderAPI en menús (PAPI solo en `ChatFormatService.java:165`).
  - Ranks: `id name display prefix suffix player priority type scope color default italic bold staff media admin op visible permissions parents-count` (`RankListMenu.java:112-135`) + `parent parent-display parent-priority` (Parents).
  - Settings: `value lang` · Coinshop: `id target price display preview type balance`.
- **Sonidos:** framework A → una clave lógica por menú `SOUND.CLICK` (todos los YAML: `UI-CLICK`), reproducida en cualquier clic con acción (`MenuListener.java:30-33`). Framework B → `ButtonSound` enum `CLICK SUCCESS FAIL DENY CONFIRM PAGE` → `SoundKey` (`api/ButtonSound.java:13-19`). `sounds.yml` mapea claves → `NAMES` (primero que resuelva, 1.8 y moderno) + `VOLUME/PITCH` (`sounds.yml:8-61`). **El toggle `/settings` de sonidos no se respeta** (`fx/SoundService.java` no consulta `PlayerSettings`).
- **Animaciones:** NOT_IMPLEMENTED (solo `autoUpdate` del framework B, sin frames).
- **Textos:** todos vía `CoreLanguage.raw/list` con claves `RANKS.MENU.*`, `SETTINGS.{MENU,SOUNDS,PARTICLES,LANG}.*`, `COINSHOP.{MENU,TAGS,COSMETICS,CONFIRM}.*`, `STAFF-MODE.HOTBAR.*` en `messages/{en,es}.yml` (+ copia en `messages/legacy/`). Excepción: `ConfirmationMenu.java:65,77`, `BackButton.java:18`, `CloseButton.java:16` tienen texto ES hardcodeado (`&a&lConfirmar`, `&cVolver`, `&cCerrar`).

### 1.6 Modern vs Legacy — cómo se elige y qué cambia

- Selección **por servidor**, una vez: `MenuConfig.serverLegacy()` = `Material.valueOf("GRAY_STAINED_GLASS_PANE")` falla → legacy (`MenuConfig.java:57-64`). ⇒ **1.8–1.12 = legacy, 1.13+ = modern** (DECISIONS dice Modern **1.16+**; 1.13–1.15 caen en modern).
- Carpeta: `menus/{modern|legacy}/<id-lowercase>.yml` (`MenuConfig.java:49-53`), se copia al dataFolder si no existe.
- **Textos de menú no usan el bag legacy:** `language.raw(key)` usa `protocol = Integer.MAX_VALUE` → siempre `messages/{lang}.yml` modern (`CoreLanguage.java:65-71`); `messages/legacy/*.yml` nunca se lee para menús (no se usa `rawFor/listFor`).
- **Idioma global**, no por jugador: `CoreLanguage.load(languageCode)` único; `SettingsLangMenu` guarda `settings.language` pero nadie lo lee (grep `.language()`).
- Diferencias reales entre ficheros modern/legacy (diff completo): **solo materiales y DATA**. Ej. `GRAY_STAINED_GLASS_PANE/0` ↔ `STAINED_GLASS_PANE/7`; `LIGHT_BLUE…` ↔ `…/3`; `WRITABLE_BOOK`↔`BOOK_AND_QUILL`; `OAK_SIGN`↔`SIGN`; `RED_DYE/0`↔`INK_SACK/1`; `GOLDEN_HELMET`↔`GOLD_HELMET`; `COMMAND_BLOCK`↔`COMMAND`; `ENDER_EYE`↔`EYE_OF_ENDER`; `GRAY_DYE/0`↔`INK_SACK/8`. Slots, filas, patrones idénticos → hoy **no** son implementaciones visuales independientes (contradice DECISIONS 2026-11-03).
- Resolución de material tolerante: `LegacyMaterials.of(nombres…)` prueba alternativas; si nada resuelve → `STONE`, y `parseMaterial` sustituye `STONE` por el fallback Java **en silencio** (`MenuFiller.java:69-74`, `util/LegacyMaterials.java:29-38`).

### 1.7 Claves YAML soportadas HOY (parser exacto)

| Clave | Lector | Notas |
|---|---|---|
| `ROWS` | `MenuConfig.rows` (`:70-72`) | clamp 1–6 |
| `BORDER-PATTERN` | `:100-102` | true → solo borde; false → relleno completo |
| `SOUND.CLICK` | `:117-120` | clave de `sounds.yml` |
| `FILLER.{MATERIAL,DATA,NAME,GLOW,SLOTS}` | `MenuFiller.from` (`:41-52`) | `SLOTS` de FILLER no se usa (borde/fill ignoran slots) |
| `ACCENT-FILLER.{MATERIAL,DATA,NAME,GLOW,SLOTS}` | `MenuConfig.accentFiller` (`:78-84`) | pintado **antes** del contenido → ocupa slots de `CONTENT-SLOTS` |
| `CONTENT-SLOTS` | `:86-98` | lista int; default = interior |
| `SLOTS.<KEY>` | `:104-106` | int |
| `ITEMS.<KEY>.MATERIAL` / `.DATA` | `:108-115` | **solo** material+data; nombre/lore siempre de messages |
| `TITLE` | `MenuConfig.title` (`:66-68`) | **existe pero ningún menú lo llama** (títulos desde messages) |

No soportado: AMOUNT, NAME/LORE por item, ENCHANTS, FLAGS (hardcode `HIDE_ATTRIBUTES/HIDE_ENCHANTS` solo en fillers), CUSTOM-MODEL-DATA, SKULL-OWNER/TEXTURE, rangos de slots `'10-16'`, PATTERN, ACTIONS, CONDITIONS, PERMISSION, PAGINATION, TYPE de inventario.
Ids registrados: `RANK-LIST RANK-EDIT RANK-PARENTS STAFF-MODE SETTINGS SETTINGS-SOUNDS SETTINGS-PARTICLES SETTINGS-LANG COINSHOP COINSHOP-TAGS COINSHOP-CONFIRM` (`MenuConfig.java:25-37`).

### 1.8 Estado por menú

| Menú (id YAML) | Clase | Módulo (flag `settings.yml` MODULES) | ESTADO | Evidencia / huecos |
|---|---|---|---|---|
| RANK-LIST | `ranks/menu/RankListMenu.java` | RANKS: true | PARTIAL | Lista + tooltip + click→edit. Sin paginación; accent en 22/40 roba 2 de 28 slots → máx. 26 rangos (`rank-list.yml:56-60` + `RankListMenu.java:65`). Sin "crear rango". |
| RANK-EDIT | `ranks/menu/RankEditMenu.java` | RANKS | IMPLEMENTED_UNTESTED | 15 campos (name/display/weight/prefix/suffix/color/bold/italic/default/staff/admin/op/visible/parents/back). Anvil solo en `v1_8_R3` y `v1_21_R1` (`supportsAnvilInput`), resto → `ANVIL-UNSUPPORTED`. `media/type/scope/permisos` no editables. |
| RANK-PARENTS | `ranks/menu/RankParentsMenu.java` | RANKS | STUB | Lista padres; ADD/REMOVE = mensaje "SOON" (`:79,87`). |
| SETTINGS | `modules/settings/menu/SettingsMenu.java` | PLAYER-SETTINGS: true | IMPLEMENTED_UNTESTED | 5 botones; PASSWORD requiere nLogin + anvil. |
| SETTINGS-SOUNDS / -PARTICLES | `SettingsToggleMenu.java` | PLAYER-SETTINGS | PARTIAL | Persiste toggle; ningún consumidor lo lee (SoundService, partículas). |
| SETTINGS-LANG | `SettingsLangMenu.java` | PLAYER-SETTINGS | PARTIAL | Guarda `language`; CoreLanguage es global. |
| COINSHOP | `modules/coinshop/menu/CoinshopMenu.java` | COINSHOP: false | PARTIAL | Solo TAGS+BALANCE+CLOSE; claves `COSMETICS-*` existen en messages sin botón. |
| COINSHOP-TAGS | `CoinshopCategoryMenu.java` | COINSHOP | IMPLEMENTED_UNTESTED | Path fijo `COINSHOP-TAGS` también para cosmetics (`:46`). Sin paginación. |
| COINSHOP-CONFIRM | `CoinshopConfirmMenu.java` | COINSHOP | IMPLEMENTED_UNTESTED | Confirm/Cancel/Info. |
| STAFF-MODE (hotbar) | `modules/staffmode/StaffModeService.java:268-336` | STAFF-MODE: true | IMPLEMENTED_UNTESTED | No es inventario: lee solo `SLOTS`/`ITEMS`; `ROWS/BORDER/SOUND` del YAML ignorados. Vanish on/off usa DATA hardcode 10/8 (`:321`) → en modern `GRAY_DYE` no cambia de color. |
| Inspección staff | `StaffModeListener.java:162-169` | STAFF-MODE | NOT_IMPLEMENTED (menú) | Abre `target.getInventory()` crudo (editable), sin menú de inspección. Ver [STAFFMODE](STAFFMODE.md). |
| Selector de kits | — | — | NOT_IMPLEMENTED | Solo referencia IoriCTW. |
| `ConfirmationMenu` (api) | `menu/api/ConfirmationMenu.java` | — | IMPLEMENTED_UNTESTED | Sin uso; texto hardcodeado. |
| Framework B (`menu.api`) | `menu/api/**` | core | IMPLEMENTED_UNTESTED | Registrado, 0 consumidores. |
| Paginación / acciones YAML / condiciones / animaciones | — | — | NOT_IMPLEMENTED | — |

`IMPLEMENTED_AND_TESTED`: ninguno — no hay captura RESULT-OK de menús propios en `IMAGE-INDEX.md` ni tests (`bukkit/src/test` inexistente).

### 1.9 Limitaciones 1.8.9 vs 1.16+

| Tema | 1.8.9 (legacy) | 1.16+ (modern) | Impacto hoy |
|---|---|---|---|
| Materiales | nombres pre-flattening + `DATA` (durabilidad) | nombres planos, DATA irrelevante | `ITEMS.*.DATA` + `LegacyMaterials` |
| Título | ≤32 chars (validación CraftBukkit 1.8) | sin límite práctico | **Framework A no recorta**: `EDIT-TITLE "&8Editor · <name>"` (11 + name≤32) puede superar 32 → excepción en 1.8 (NO VERIFICADO en servidor) |
| Color | `&` 16 colores | hex `&#RRGGBB` / componentes | `CoreLanguage.color` solo `&` (`:272-275`) → hex no soportado en ninguna versión |
| CustomModelData | no | 1.14+ (`item_model` 1.21.4+) | no soportado |
| Cabezas | `SKULL_ITEM:3` + `SkullMeta.setOwner` | `PLAYER_HEAD` + `setOwningPlayer`/textura | solo `LegacyMaterials.playerHead()`; sin TEXTURE |
| ItemFlags | `HIDE_ENCHANTS/ATTRIBUTES/UNBREAKABLE/DESTROYS/PLACED_ON/POTION_EFFECTS` | + `HIDE_DYE` (1.16.2+), glint override (1.20.5+) | glow = `DURABILITY 1` + HIDE_ENCHANTS en fillers |
| ClickType | sin `SWAP_OFFHAND` | `SWAP_OFFHAND` (F) | no usado |
| Sonidos | `CLICK`, `NOTE_BASS`… | `UI_BUTTON_CLICK`… | resuelto por `sounds.yml NAMES` |
| Anvil input | NMS `v1_8_R3` ✔ | `v1_21_R1` ✔; `v1_12_R1/v1_16_R3/v1_20_R3` ✘ | ver [CORE-MODULES](CORE-MODULES.md) |

---

## 2. Referencia IoriCTW / IoriCTWL (propio, reutilizable)

### 2.1 `IoriCTW/plugin/src/main/resources/language/kits/kits-es.yml` (1678 líneas, principal)
- Cabecera `# *** IoriCTW | Kits (ES) ***`, secciones `# ────` + título MAYÚSCULAS (`ENABLED KITS`, `KITS`), notas `# ~` (`:1-8,52-55`).
- `EnabledKits:` lista ordenada = orden en el menú (slot = índice) (`:9-50`).
- Bloque por kit (`Bow:` `:56-93`): `DisplayItem{Material, Data, DisplayName, BaseLore, NoPermissionLore, NoAchievementLore, FinalLore}`, `KitCommands` (consola, `%player%`), `PurchaseAlert.Enabled`, `Requirements{Price, Premission{Enabled, Node, NoPermissionMessage}, Achievement{Enabled, AchievementRequired}}`.
- Lore con jerarquía visual fija: categoría `&8» …`, secciones `&a&l» INFO` / `» RECOMPENSAS` / `&c&l» REQUISITOS` / `&6&l» PRECIO &7&m--&r &c25⛃`, bullets `&b&l•`.
- Legible por: un bloque autocontenido por item, lore por **estado** (base + sufijo según permiso/logro/desbloqueado), requisitos agrupados, comentarios inline solo en el primer ejemplo (`Arrows`, `:124-146`).
- Defectos: PascalCase (no estándar del proyecto), typo `Premission`, texto inline en fichero por idioma (kits-es/kits-en).

Parser — `IoriCTW/.../ctw/kits/items/KitConfigHandler.java`:
- **Ya soporta doble esquema**: UPPERCASE nuevo (`ENABLED`, `KITS.<id>.DISPLAY.MATERIAL|DATA|NAME`, `DISPLAY.LORE.BASE|NO-PERMISSION|NO-ACHIEVEMENT|UNLOCKED`, `COMMANDS`, `REQUIREMENTS.PRICE|PERMISSION.ENABLED|NODE|MESSAGE|ACHIEVEMENT.ENABLED|REQUIRED`, `PURCHASE-ALERT.ENABLED`) con fallback al PascalCase (métodos `getEnabledKits`, `readKitString`, `readKitLore`, `kitBase`).
- `ItemKitHandler`: item = base lore + lore de estado (`updateItem`); click por índice de slot (`sendKit`); comprueba permiso → logro → saldo; ejecuta comandos como consola; sonidos fail/pickup.

### 2.2 IoriCTWL `src/main/resources/menus/{games,glow}.yml` + `menus/Menu*.java`
- Cabecera con chuleta `# ~ SLOTS: 10 | '1-5' | '0-8' | '1-5,7,9'`, `# ~ ACTION: CLOSE | QUEUE | OPEN | STORE | COMMAND | NONE`, lista de placeholders (`games.yml:1-14`).
- Claves UPPERCASE: `TITLE`, `SIZE`, `FILLER{ENABLED,SLOTS,MATERIAL,DATA,NAME,LORE}`, `ITEMS.<id>{SLOTS,MATERIAL,DATA,AMOUNT,SKULL-OWNER,NAME,LORE,ACTION,COMMAND|OPEN|COLOR}`, sección dinámica `SERVERS{SLOTS,DATA-ONLINE,DATA-OFFLINE,LORE,LORE-OFFLINE,STATUS-*}`, estados `COLORS{NAME,LORE,SELECTED,LOCKED{…,MESSAGE}}` (`glow.yml:70-103`).
- Parser: `MenuSlots.parse` (int, lista, rangos, comas), `MenuItems.build` (MATERIAL/AMOUNT/DATA/NAME/LORE/SKULL-OWNER/COLOR cuero), `MenuClicks.handle` (ACTION → CLOSE/QUEUE/OPEN/STORE/GLOW/COMMAND con `player.performCommand`).
- Reutilizable directamente (código propio): parser de slots con rangos, item builder por sección, dispatcher de ACTION, patrón de estados (available/SELECTED/LOCKED).

---

## 3. Referencias visuales (abiertas: 5)

| Imagen | Fam | Qué se ve | Reproducir / mejorar |
|---|---|---|---|
| `refs/img/image-0bf93c04…` | M | Solo tooltip: título `DUELOS [★]` amarillo; `» Categoría (icono)` gris; `» DESCRIPCIÓN` amarillo bold; bullet `▪` + 2 líneas blancas; `» Usa este acceso rápido! (✓)` gris con check verde; footer `[icono] ¡Click para revisar!` amarillo. Fuente custom (resource pack). | Estructura de lore por secciones `» TÍTULO` + footer de acción. Modern: hex + glyphs; Legacy: mismo esquema con `&e/&7` y sin fuente custom. |
| `refs/img/15-rank-founder-menu.png` | L | Inventario "Rank Lis…" 6 filas (lana/bloque blanco como iconos en col 1), tooltip enorme: `[Founder] (founder)`, ID UUID, Default Rank, Name/Display/Prefix/Suffix/Priority/Type, Color + sub-bullets `× Italic/Bold`, Staff/Media, Permissions, `Parents: 17` con 17 líneas `× [Rank], #prio`, `Click to edit rank`. Etiquetas aqua, valores blanco/rojo (False). | Ya reproducido en `RANKS.MENU.LORE` + `PARENT-LINE`. Mejorar: limitar padres mostrados (`+N más`), icono por color del rango, paginación. |
| `_refs/img/20-helium-rank-editor.png` | L | "Rank Editor \| 1/1" (paginado en título); 2 filas de iconos de colores (lana/tintes por color), 1 icono verde suelto; tooltip `PlatAdmin (#0035/14)` con barra roja `\|` + Name/setName/Weight/Prefix/Suffix/Color/Bold/Italic/Default/Staff/Admin/Op/Visible (Yes verde / No rojo). | Helium = **solo referencia funcional**. Idea: icono del rango coloreado según `color`, `página x/y` en título, índice `#id/total`. |
| `refs/img/image-2d366fa2…` | L | Tooltip pico `Tqmass's Pickaxe (#0278)`: secciones `Statistics` (verde) y `Enchantments` (rojo) con barra de color `\|` a la izquierda y valores `21 / 100`. | Patrón "barra lateral por sección" + `actual / máximo`; útil para stats e inspección staff. |
| `refs/img/image-bf4de872…` | L (índice) | "Perfiles - Menu Principal" 6 filas: slot 0 cartel, slot 8 esmeralda; fila 0 paneles claros; slot 9 panel lima (marca de sección); interior mezcla paneles grises oscuros + cabezas texturizadas, yunque, tag, papel, tarta; slot 49 bloque rojo (cerrar). | Layout: fila superior de navegación, acento de color para "seleccionado", cerrar centrado abajo (49). Cabezas con textura → requiere `TEXTURE` (no existe hoy). |

---

## 4. PROPUESTA — formato YAML unificado (NO implementado)

### 4.1 Ubicación (razonamiento)
- **Ficheros separados** `menus/modern/<id>.yml` y `menus/legacy/<id>.yml` = implementaciones visuales independientes (DECISIONS 2026-11-03) y encaja con el layout actual (`MenuConfig.java:49`). Secciones `MODERN:/LEGACY:` en un único fichero mezclarían materiales incompatibles y harían el fichero ilegible.
- **Lógica compartida** `menus/logic/<id>.yml` (botones por id: acciones, condiciones, permisos, fuente dinámica). El layout visual referencia ids; puede **sobrescribir** `ACTIONS` si un diseño legacy necesita otro flujo.
- **Textos** nunca en layout (decisión i18n 2026-11-01 "cero strings hardcodeados"): claves `MENUS.<ID>.<BUTTON>.{NAME,LORE,LORE-<STATE>}`. Dónde: **pendiente A5** — opción (a) mantener en `messages/{lang}.yml` + `messages/legacy/{lang}.yml` (actual); opción (b) `messages/menus/{lang}.yml` + `messages/legacy/menus/{lang}.yml` (DECISIONS 2026-11-01). Recomendado (b): los textos de menú son voluminosos y legacy necesita lore distinto (sin hex/glyphs). Resolver con `rawFor/listFor(player)` para que legacy use su bag.

### 4.2 Opciones

| Opción | Hoy | Propuesta (clave) |
|---|---|---|
| id | parcial (`MenuConfig.MENU_IDS` hardcode) | `ID` = nombre de fichero; registro automático por carpeta |
| título | parcial (`TITLE` leído, no usado; real en messages) | `TITLE-KEY: MENUS.<ID>.TITLE`; recorte 32 en legacy |
| tamaño | sí `ROWS` | `ROWS` (1-6) |
| tipo | no | `TYPE: CHEST\|HOPPER\|DISPENSER\|ANVIL-INPUT` |
| slots / patrones | parcial (`SLOTS.<K>` int, `CONTENT-SLOTS` lista) | `SLOT`/`SLOTS` int\|lista\|`'10-16'`\|`'1-5,7'` (como IoriCTWL) + `PATTERN` (filas de 9 chars) + `KEYS` |
| material | sí `ITEMS.<K>.MATERIAL` | `MATERIAL` (+ `MATERIAL-FALLBACK` lista) y **warning** si no resuelve |
| cantidad | no | `AMOUNT` (o placeholder `{count}`) |
| data/durabilidad | sí `DATA` | `DATA` (solo legacy) |
| custom-model-data | no | `CUSTOM-MODEL-DATA`, `ITEM-MODEL` (solo modern) |
| nombre / lore | no (siempre messages) | `TEXT: MENUS.<ID>.<BTN>` → `.NAME`/`.LORE`; `LORE-STATE` |
| enchants / flags / glow | parcial (`GLOW` solo fillers; flags hardcode) | `GLOW`, `ENCHANTS: [DURABILITY:1]`, `FLAGS: [HIDE_ATTRIBUTES, …]` |
| cabezas / texturas | no | `SKULL-OWNER: '{player}'`, `TEXTURE: <base64>` (legacy: `SKULL_ITEM` DATA 3) |
| placeholders | parcial (mapa por clase, `<k>{k}%k%`) | ídem + PAPI opcional + `{page} {pages} {viewer}` |
| visibilidad / condiciones | no | `VIEW-IF`, `CLICK-IF` + `DENY` |
| permisos | solo comando | `PERMISSION` (menú y botón), `PERMISSION-HIDE: true` |
| acciones por clic | no (Java) | `ACTIONS.{LEFT,RIGHT,SHIFT-LEFT,SHIFT-RIGHT,MIDDLE,DROP,NUMBER,SWAP-OFFHAND,ANY}` |
| sonidos | parcial (`SOUND.CLICK` por menú) | `SOUND.{OPEN,CLICK,DENY,PAGE}` menú + `SOUND` por botón (clave `sounds.yml`), respetando toggle jugador |
| mensajes | sí (claves) | acción `MESSAGE: <i18n-key>` (nunca literal) |
| navegación | parcial (botón Java) | acciones `BACK` (pila por sesión), `OPEN-MENU`, `PAGE-PREV/NEXT` + `NAVIGATION` block |
| decoración / fillers | sí `FILLER`, `ACCENT-FILLER`, `BORDER-PATTERN` | `DECORATION: [{SLOTS|PATTERN-KEY, MATERIAL, DATA, GLOW}]` múltiples |
| elementos dinámicos | no (Java, sin paginar) | `DYNAMIC{SOURCE, SLOTS, TEMPLATE, STATES, EMPTY, SORT}` + `PAGINATION` |
| animaciones | no | `ANIMATION{INTERVAL, FRAMES:[MATERIAL/DATA/TEXT]}` (opcional, ambos) |
| refresh | no (A) / `autoUpdate` (B) | `UPDATE-INTERVAL` ticks |

### 4.3 Catálogo de acciones

| Acción | Hoy | Propuesta (sintaxis `TIPO: arg`) |
|---|---|---|
| player-cmd | no (IoriCTWL `COMMAND`) | `PLAYER-CMD: spawn` |
| console-cmd | no (IoriCTW `KitCommands`) | `CONSOLE-CMD: give {player} BOW 1` (validar args, ver [PERMISSIONS](PERMISSIONS.md)) |
| message | sí (Java) | `MESSAGE: RANKS.MENU.SET-NAME` |
| open-menu | sí (Java) | `OPEN-MENU: rank-edit` |
| close | sí (Java) | `CLOSE` |
| back | parcial (botón hardcode) | `BACK` (pila de sesión) |
| page prev/next | no | `PAGE-PREV`, `PAGE-NEXT` |
| internal id | — (todo es interno) | `INTERNAL: ranks.toggle-bold` → `MenuActionRegistry` en Java (toggles, anvil, compra) |
| anvil input | sí (Java) | `INPUT: {KEY: RANKS.MENU.ANVIL-NAME, THEN: INTERNAL ranks.set-name}` |
| give item | no | `GIVE: {MATERIAL, AMOUNT}` |
| buy / sell (economy) | sí compra (coinshop Java) | `BUY: {PRICE: 25, THEN: […]}` vía `EconomyService` |
| permission/condition check | no | `REQUIRE: PERMISSION iorictw.kit.Bow` · `REQUIRE: BALANCE >= 25` |
| sound | parcial | `SOUND: CONFIRM` |
| refresh | sí (reabrir) | `REFRESH` |
| connect (proxy) | no | `CONNECT: lobby` vía plugin messaging (ver [PROXY-MODULES](PROXY-MODULES.md)) |

Condiciones propuestas: `PERMISSION <node>`, `!PERMISSION`, `BALANCE >= n`, `SETTING <key> == true`, `PLACEHOLDER %x% == y`, `STATE <id>` (estado provisto por la fuente dinámica: `OWNED`, `LOCKED`, `SELECTED`).

### 4.4 Ejemplos

#### a) Menú sencillo — SETTINGS

`menus/logic/settings.yml`
```yaml
# *************************************
# *     IoriCore | Menu SETTINGS      *
# *************************************
# ~ Lógica compartida modern/legacy · textos → messages/menus/{lang}.yml (MENUS.SETTINGS.*)

PERMISSION: ioricore.settings

# ────────────────────────────
# BUTTONS
# ────────────────────────────
BUTTONS:
  SOUNDS:
    ACTIONS:
      ANY:
        - "OPEN-MENU: settings-sounds"
  PREMIUM:
    # ~ Estado {value} lo provee el INTERNAL
    ACTIONS:
      ANY:
        - "INTERNAL: settings.toggle-premium"
        - "MESSAGE: SETTINGS.TOGGLED-PREMIUM"
        - "REFRESH"
  PASSWORD:
    VIEW-IF: "HOOK nlogin"
    ACTIONS:
      ANY:
        - "INPUT: SETTINGS.PASSWORD-ANVIL-TITLE -> settings.change-password"
  CLOSE:
    ACTIONS:
      ANY: [ "CLOSE" ]
```
`menus/modern/settings.yml`
```yaml
# *************************************
# *  IoriCore | SETTINGS (Modern)     *
# *************************************
ROWS: 3
TITLE-KEY: MENUS.SETTINGS.TITLE
SOUND:
  OPEN: UI-CLICK
  CLICK: UI-CLICK

# ────────────────────────────
# PATTERN
# ────────────────────────────
# ~ Cada char = KEY; '#' = filler
PATTERN:
  - "#########"
  - "##SPLAW##"
  - "####X####"
KEYS:
  "#": { DECORATION: GLASS }
  S: { BUTTON: SOUNDS, MATERIAL: NOTE_BLOCK, TEXT: MENUS.SETTINGS.SOUNDS }
  P: { BUTTON: PARTICLES, MATERIAL: BLAZE_POWDER, TEXT: MENUS.SETTINGS.PARTICLES }
  L: { BUTTON: LANGUAGE, MATERIAL: PLAYER_HEAD, TEXTURE: "<base64-globo>", TEXT: MENUS.SETTINGS.LANGUAGE }
  A: { BUTTON: PREMIUM, MATERIAL: GOLD_INGOT, GLOW: "{premium}", TEXT: MENUS.SETTINGS.PREMIUM }
  W: { BUTTON: PASSWORD, MATERIAL: TRIAL_KEY, MATERIAL-FALLBACK: [TRIPWIRE_HOOK], CUSTOM-MODEL-DATA: 1001, TEXT: MENUS.SETTINGS.PASSWORD }
  X: { BUTTON: CLOSE, MATERIAL: BARRIER, TEXT: MENUS.COMMON.CLOSE }

DECORATION:
  GLASS: { MATERIAL: GRAY_STAINED_GLASS_PANE, NAME-KEY: MENUS.COMMON.EMPTY }
```
`menus/legacy/settings.yml` (difiere: materiales/DATA, sin CMD ni textura, layout más compacto)
```yaml
# *************************************
# *  IoriCore | SETTINGS (Legacy)     *
# *************************************
ROWS: 3
TITLE-KEY: MENUS.SETTINGS.TITLE    # ~ ≤32 chars tras color (se recorta)
SOUND:
  CLICK: UI-CLICK

PATTERN:
  - "#########"
  - "#S#P#L#A#"
  - "####W####"
KEYS:
  "#": { DECORATION: GLASS }
  S: { BUTTON: SOUNDS, MATERIAL: NOTE_BLOCK, TEXT: MENUS.SETTINGS.SOUNDS }
  P: { BUTTON: PARTICLES, MATERIAL: BLAZE_POWDER, TEXT: MENUS.SETTINGS.PARTICLES }
  L: { BUTTON: LANGUAGE, MATERIAL: BOOK, TEXT: MENUS.SETTINGS.LANGUAGE }
  A: { BUTTON: PREMIUM, MATERIAL: GOLD_INGOT, GLOW: "{premium}", TEXT: MENUS.SETTINGS.PREMIUM }
  W: { BUTTON: PASSWORD, MATERIAL: IRON_DOOR, TEXT: MENUS.SETTINGS.PASSWORD }

DECORATION:
  GLASS: { MATERIAL: STAINED_GLASS_PANE, DATA: 7, NAME-KEY: MENUS.COMMON.EMPTY }
```

#### b) Selector de kits (estilo `kits-es.yml`, UPPERCASE)

`menus/logic/kits.yml` (datos de kit = lógica; ya compatible con el esquema UPPERCASE de `KitConfigHandler`)
```yaml
# *************************************
# *       IoriCore | Kits             *
# *************************************
# ~ ENABLED = orden de aparición en DYNAMIC
# ~ Textos: MENUS.KITS.<ID>.{NAME,LORE,LORE-NO-PERMISSION,LORE-LOCKED,LORE-UNLOCKED}

# ────────────────────────────
# ENABLED
# ────────────────────────────
ENABLED:
  - BOW
  - ARROWS

# ────────────────────────────
# KITS
# ────────────────────────────
KITS:
  BOW:
    COMMANDS:
      - "give {player} BOW 1"
    REQUIREMENTS:
      PRICE: 25.0
      PERMISSION:
        ENABLED: false
        NODE: ioricore.kit.bow
      ACHIEVEMENT:
        ENABLED: false
        REQUIRED: ""
    # ~ Orden: permiso → logro → saldo (como ItemKitHandler.sendKit)
    ACTIONS:
      LEFT:
        - "REQUIRE: PERMISSION {node} | DENY: MESSAGE KITS.NO-PERMISSION, SOUND DENY"
        - "BUY: {price}"
        - "CONSOLE-CMD: {commands}"
        - "MESSAGE: KITS.RECEIVED"
        - "SOUND: SUCCESS"
      RIGHT:
        - "OPEN-MENU: kit-preview {id}"
```
`menus/modern/kits.yml`
```yaml
ROWS: 6
TITLE-KEY: MENUS.KITS.TITLE
DYNAMIC:
  SOURCE: kits.enabled
  SLOTS: [ '10-16', '19-25', '28-34' ]
  TEMPLATE:
    MATERIAL: "{material}"          # ~ por kit, ver ICONS
    TEXT: "MENUS.KITS.{id}"
    LORE-STATE: "{state}"           # ~ UNLOCKED | NO-PERMISSION | LOCKED
    FLAGS: [ HIDE_ATTRIBUTES, HIDE_POTION_EFFECTS ]
  ICONS:
    BOW: { MATERIAL: BOW }
    ARROWS: { MATERIAL: ARROW, AMOUNT: 16 }
PAGINATION: { PREV: 45, NEXT: 53, INFO: 49 }
DECORATION:
  - { SLOTS: [ '0-9', 17, 18, 26, 27, 35, 36, '37-44' ], MATERIAL: GRAY_STAINED_GLASS_PANE }
```
`menus/legacy/kits.yml` (difiere: pociones por DATA)
```yaml
ROWS: 6
TITLE-KEY: MENUS.KITS.TITLE
DYNAMIC:
  SOURCE: kits.enabled
  SLOTS: [ '10-16', '19-25', '28-34' ]
  TEMPLATE: { MATERIAL: "{material}", DATA: "{data}", TEXT: "MENUS.KITS.{id}", LORE-STATE: "{state}" }
  ICONS:
    BOW: { MATERIAL: BOW }
    HARMING-II: { MATERIAL: POTION, DATA: 16428 }
PAGINATION: { PREV: 45, NEXT: 53, INFO: 49 }
DECORATION:
  - { SLOTS: [ '0-9', 17, 18, 26, 27, 35, 36, '37-44' ], MATERIAL: STAINED_GLASS_PANE, DATA: 7 }
```
Messages (`messages/menus/es.yml`, lore por estado al estilo IoriCTW):
```yaml
# — KITS ——————————————————————
MENUS:
  KITS:
    TITLE: "&8Kits"
    BOW:
      NAME: "&a&lARCO &8[&a&l•&8]"
      LORE:
        - "&8» Combate"
        - ""
        - "&a&l» INFO"
        - "&7Dispara desde lejos."
      LORE-NO-PERMISSION: [ " ", "&c&l» REQUISITOS", "&b&l• &cRango: &6&lULTRA" ]
      LORE-UNLOCKED: [ " ", "&6&l» PRECIO &7&m--&r &c<price>⛃" ]
```

#### c) Ranks — lista + edición

`menus/logic/rank-list.yml`
```yaml
PERMISSION: ioricore.ranks
DYNAMIC:
  SOURCE: ranks.all            # ~ ordenado por priority desc
  ACTIONS:
    LEFT: [ "OPEN-MENU: rank-edit {id}" ]
    SHIFT-RIGHT: [ "REQUIRE: PERMISSION ioricore.ranks.delete", "OPEN-MENU: confirm rank-delete {id}" ]
BUTTONS:
  CREATE:
    PERMISSION: ioricore.ranks.create
    ACTIONS:
      ANY: [ "INPUT: RANKS.MENU.ANVIL-NAME -> ranks.create" ]
```
`menus/modern/rank-list.yml`
```yaml
ROWS: 6
TITLE-KEY: MENUS.RANK-LIST.TITLE   # ~ "&8Rangos {page}/{pages}"
DYNAMIC:
  SLOTS: [ '10-16', '19-25', '28-34', '37-43' ]
  TEMPLATE:
    MATERIAL: "{color}_WOOL"        # ~ color del rango
    TEXT: MENUS.RANK-LIST.ENTRY     # ~ lore = RANKS.MENU.LORE actual
    GLOW: "{default}"
  LIMITS: { PARENT-LINES: 8 }       # ~ "+N más"
PAGINATION: { PREV: 48, NEXT: 50, INFO: 49 }
SLOTS: { CREATE: 4 }
KEYS:
  CREATE: { MATERIAL: LIME_DYE, TEXT: MENUS.RANK-LIST.CREATE }
DECORATION:
  - { BORDER: true, MATERIAL: GRAY_STAINED_GLASS_PANE }
```
`menus/legacy/rank-list.yml`
```yaml
ROWS: 6
TITLE-KEY: MENUS.RANK-LIST.TITLE
DYNAMIC:
  SLOTS: [ '10-16', '19-25', '28-34', '37-43' ]
  TEMPLATE: { MATERIAL: WOOL, DATA: "{color-data}", TEXT: MENUS.RANK-LIST.ENTRY }
PAGINATION: { PREV: 48, NEXT: 50, INFO: 49 }
SLOTS: { CREATE: 4 }
KEYS:
  CREATE: { MATERIAL: INK_SACK, DATA: 10, TEXT: MENUS.RANK-LIST.CREATE }
DECORATION:
  - { BORDER: true, MATERIAL: STAINED_GLASS_PANE, DATA: 7 }
```
`menus/logic/rank-edit.yml` (fragmento; misma lógica que `RankEditMenu` hoy)
```yaml
PERMISSION: ioricore.ranks
BUTTONS:
  NAME:   { ACTIONS: { ANY: [ "INPUT: RANKS.MENU.ANVIL-NAME -> ranks.set-name {id}" ] } }
  COLOR:
    ACTIONS:
      LEFT:       [ "INTERNAL: ranks.cycle-color {id}", "REFRESH" ]
      SHIFT-LEFT: [ "INPUT: RANKS.MENU.ANVIL-COLOR -> ranks.set-color {id}" ]
  OP:
    PERMISSION: ioricore.ranks.edit.op   # ~ hoy NO existe: cualquiera con ioricore.ranks
    ACTIONS: { ANY: [ "INTERNAL: ranks.toggle op {id}", "MESSAGE: RANKS.MENU.TOGGLE-OP", "REFRESH" ] }
  BACK:   { ACTIONS: { ANY: [ "BACK" ] } }
```
(Layouts `menus/{modern,legacy}/rank-edit.yml` = los actuales + `TEXT`/`KEYS`; diferencias solo de material/DATA ya listadas en §1.6.)

#### d) Staffmode — inspección de jugador

`menus/logic/staff-inspect.yml`
```yaml
PERMISSION: ioricore.staffmode
UPDATE-INTERVAL: 20               # ~ ticks (vida/ping en vivo)
BUTTONS:
  FREEZE:   { ACTIONS: { ANY: [ "INTERNAL: staff.freeze {target}", "REFRESH" ] } }
  TELEPORT: { ACTIONS: { ANY: [ "INTERNAL: staff.teleport {target}", "CLOSE" ] } }
  INVSEE:   { ACTIONS: { LEFT: [ "INTERNAL: staff.invsee-readonly {target}" ] } }
  PUNISH:
    PERMISSION: ioricore.punish
    ACTIONS: { ANY: [ "PLAYER-CMD: punish {target}" ] }
  CHATLOGS: { ACTIONS: { ANY: [ "OPEN-MENU: chatlogs {target}" ] } }
```
`menus/modern/staff-inspect.yml`
```yaml
ROWS: 3
TITLE-KEY: MENUS.STAFF-INSPECT.TITLE   # ~ "&8Inspeccionar · {target}"
PATTERN:
  - "####H####"
  - "#F#T#I#P#"
  - "####C####"
KEYS:
  H: { MATERIAL: PLAYER_HEAD, SKULL-OWNER: "{target}", TEXT: MENUS.STAFF-INSPECT.INFO }   # ~ estilo 2d366fa2: barras por sección
  F: { BUTTON: FREEZE, MATERIAL: PACKED_ICE, GLOW: "{frozen}", TEXT: MENUS.STAFF-INSPECT.FREEZE }
  T: { BUTTON: TELEPORT, MATERIAL: ENDER_PEARL, TEXT: MENUS.STAFF-INSPECT.TELEPORT }
  I: { BUTTON: INVSEE, MATERIAL: CHEST, TEXT: MENUS.STAFF-INSPECT.INVSEE }
  P: { BUTTON: PUNISH, MATERIAL: MACE, MATERIAL-FALLBACK: [IRON_AXE], TEXT: MENUS.STAFF-INSPECT.PUNISH }
  C: { BUTTON: CHATLOGS, MATERIAL: WRITABLE_BOOK, TEXT: MENUS.STAFF-INSPECT.CHATLOGS }
  "#": { MATERIAL: BLACK_STAINED_GLASS_PANE }
```
`menus/legacy/staff-inspect.yml`
```yaml
ROWS: 3
TITLE-KEY: MENUS.STAFF-INSPECT.TITLE
PATTERN:
  - "####H####"
  - "#F#T#I#P#"
  - "####C####"
KEYS:
  H: { MATERIAL: SKULL_ITEM, DATA: 3, SKULL-OWNER: "{target}", TEXT: MENUS.STAFF-INSPECT.INFO }
  F: { BUTTON: FREEZE, MATERIAL: PACKED_ICE, GLOW: "{frozen}", TEXT: MENUS.STAFF-INSPECT.FREEZE }
  T: { BUTTON: TELEPORT, MATERIAL: ENDER_PEARL, TEXT: MENUS.STAFF-INSPECT.TELEPORT }
  I: { BUTTON: INVSEE, MATERIAL: CHEST, TEXT: MENUS.STAFF-INSPECT.INVSEE }
  P: { BUTTON: PUNISH, MATERIAL: IRON_AXE, TEXT: MENUS.STAFF-INSPECT.PUNISH }
  C: { BUTTON: CHATLOGS, MATERIAL: BOOK_AND_QUILL, TEXT: MENUS.STAFF-INSPECT.CHATLOGS }
  "#": { MATERIAL: STAINED_GLASS_PANE, DATA: 15 }
```

#### e) Dinámico paginado — tags del coinshop

`menus/logic/coinshop-tags.yml`
```yaml
PERMISSION: ioricore.coinshop
DYNAMIC:
  SOURCE: coinshop.offers TAG
  # ~ Estados provistos por la fuente: OWNED | AFFORDABLE | EXPENSIVE
  ACTIONS:
    LEFT:
      - "REQUIRE: !STATE OWNED | DENY: MESSAGE COINSHOP.ALREADY-OWNED, SOUND DENY"
      - "OPEN-MENU: coinshop-confirm {id}"
BUTTONS:
  BACK: { ACTIONS: { ANY: [ "BACK" ] } }
```
`menus/modern/coinshop-tags.yml`
```yaml
ROWS: 6
TITLE-KEY: MENUS.COINSHOP-TAGS.TITLE     # ~ "&8Tags &7{page}/{pages}"
SOUND: { PAGE: PAGE-TURN }
DYNAMIC:
  SLOTS: [ '10-16', '19-25', '28-34', '37-43' ]
  TEMPLATE:
    MATERIAL: NAME_TAG
    TEXT: MENUS.COINSHOP-TAGS.ENTRY
    LORE-STATE: "{state}"
    GLOW: "{owned}"
  EMPTY: { SLOT: 22, MATERIAL: BARRIER, TEXT: MENUS.COINSHOP-TAGS.EMPTY }
PAGINATION:
  PREV: { SLOT: 48, MATERIAL: ARROW, TEXT: MENUS.COMMON.PREV, HIDE-IF-FIRST: true }
  NEXT: { SLOT: 50, MATERIAL: ARROW, TEXT: MENUS.COMMON.NEXT, HIDE-IF-LAST: true }
  INFO: { SLOT: 49, MATERIAL: GOLD_INGOT, TEXT: MENUS.COINSHOP.BALANCE }
SLOTS: { BACK: 45 }
KEYS: { BACK: { MATERIAL: OAK_DOOR, TEXT: MENUS.COMMON.BACK } }
DECORATION:
  - { BORDER: true, MATERIAL: GRAY_STAINED_GLASS_PANE }
  - { SLOTS: [ 4 ], MATERIAL: LIGHT_BLUE_STAINED_GLASS_PANE }
```
`menus/legacy/coinshop-tags.yml`
```yaml
ROWS: 6
TITLE-KEY: MENUS.COINSHOP-TAGS.TITLE
SOUND: { PAGE: PAGE-TURN }
DYNAMIC:
  SLOTS: [ '10-16', '19-25', '28-34', '37-43' ]
  TEMPLATE: { MATERIAL: NAME_TAG, TEXT: MENUS.COINSHOP-TAGS.ENTRY, LORE-STATE: "{state}", GLOW: "{owned}" }
  EMPTY: { SLOT: 22, MATERIAL: BARRIER, TEXT: MENUS.COINSHOP-TAGS.EMPTY }
PAGINATION:
  PREV: { SLOT: 48, MATERIAL: ARROW, TEXT: MENUS.COMMON.PREV }
  NEXT: { SLOT: 50, MATERIAL: ARROW, TEXT: MENUS.COMMON.NEXT }
  INFO: { SLOT: 49, MATERIAL: GOLD_INGOT, TEXT: MENUS.COINSHOP.BALANCE }
SLOTS: { BACK: 45 }
KEYS: { BACK: { MATERIAL: WOOD_DOOR, TEXT: MENUS.COMMON.BACK } }
DECORATION:
  - { BORDER: true, MATERIAL: STAINED_GLASS_PANE, DATA: 7 }
  - { SLOTS: [ 4 ], MATERIAL: STAINED_GLASS_PANE, DATA: 3 }
```

### 4.5 Notas de implementación (propuesta)
- Unificar en **un** framework: B (`menu.api`) tiene mejor base (reuse holder, hilo principal, onClose, auto-update, recorte de título); A tiene `MenuConfig`. Propuesta: motor YAML sobre B, migrar los 9 menús, retirar A.
- Acciones de negocio siempre por `INTERNAL` registrado en Java (no lógica crítica en strings). `CONSOLE-CMD` restringido a placeholders saneados.
- Validación al cargar: material no resuelto → warning con fichero:clave (hoy cae a fallback silencioso).
- Legacy: recorte de título a 32, bag `messages/legacy` vía `rawFor/listFor`.

---

## 5. Hallazgos / riesgos

1. Framework B registrado pero sin consumidores; A no recorta títulos → posible excepción 1.8 con nombres de rango largos (`RANKS.MENU.EDIT-TITLE`, `PARENTS-TITLE`).
2. Menús ignoran `messages/legacy/` (usan `raw()` con protocolo MAX) y el idioma por jugador.
3. Modern/legacy hoy = mismos layouts con distinto material → no cumple "implementaciones independientes".
4. Sin paginación: rank list máx. 26 visibles (accent 22/40 ocupa contenido), coinshop tags 28, parents 14.
5. `RankEditMenu` permite togglear OP/ADMIN con solo `ioricore.ranks`.
6. Toggles de sonidos/partículas/idioma persistidos sin efecto.
7. Vanish hotbar en modern no cambia de aspecto (DATA 10/8 sobre `GRAY_DYE`).
8. Texto hardcodeado en `ConfirmationMenu`, `BackButton`, `CloseButton`.

## 6. Decisiones pendientes
- A5: ratificar `menus/{modern,legacy}/` para layout y decidir textos en `messages/{lang}.yml` vs `messages/menus/{lang}.yml` (+legacy).
- Umbral modern: 1.13+ (código) vs 1.16+ (DECISIONS); ¿qué hacer con 1.13–1.15?
- Framework único (B + YAML) y retirada de A.
- ¿Lógica en `menus/logic/<id>.yml` compartida o totalmente duplicada por versión?
- Permisos finos en editor de rangos (`ioricore.ranks.edit.op|admin`).
