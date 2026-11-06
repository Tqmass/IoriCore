# IMAGE-INDEX — referencias visuales

Auditoría visual 2026-11-03 · 158 PNG (155 en `docs/refs/img/` + 3 en `_refs/img/`) · **125 únicas** · 33 duplicados exactos (md5).
Ids cortos = `image-<id8>…png` en `.cursor/docs/refs/img/`. Familias: **M** = MODERN 1.16+ · **L** = LEGACY 1.8–1.12 · **D** = Discord · **C** = consola/código.
Abrir solo la(s) imagen(es) de la feature en curso. Descripciones = lo visible; "?" = no confirmado.

## ⚠ Nombres desplazados (verificado a mano 05 y 06)
Los ficheros `01`–`14` **no contienen lo que dice su nombre** (y `docs/refs/img/README.md`, `DESIGN_BRIEF.md` y `PLAN.md` los citan por nombre). Contenido real:

| Fichero | Contenido real | Fam |
|---|---|---|
| `01-scoreboard-tab-honly` | Recorte HUD (hotbar + corazones), sin scoreboard | ? |
| `01b-scoreboard-honly-detail` | Bossbar cian "¡Estás jugando en mc.honlycraft.us!" | M |
| `02-discord-webhook-format` | In-game, nametag "JuanLinsa" + "Under Review" sobre hotbar | M? |
| `05-staff-2fa-title` | **PM**: "(To X) msg" / "(From X) msg" gris/blanco/cian | M |
| `06-menu-duelos-style` | **Scoreboard HonlyCraft** "HONLYCRAFT「1」" fecha, Rango, Nivel [0★], Etiqueta, Amigos, HonlyCoins, Conexión, Conectados, store | M |
| `07-bossbar-playing` | Bossbar verde "TIP: Si necesitas ayuda usa (/request)" | M |
| `07b-bossbar-tip-request` | Bossbar roja "TIP: ¡Aprovecha los descuentos! (/store)" | M |
| `07c-bossbar-store` | Bossbar cian "¡Únete a nuestro discord! (/discord)" | M |
| `07d-bossbar-discord` | **Join MOTD PictureLogin** cara pixel + "HONLYCRAFT NETWORK (1.8–1.21)", TIENDA/DISCORD, "¡Gracias por jugar aquí! ♥" | M |
| `08-picturelogin-motd` | Title verde "SUCCESFULLY LINKED" + chat "PDISCORD ▶ … verified" | M |
| `12-ban-screen-es` | Ban screen **teal, inglés**, fuente small-caps: DATE / EXPIRES IN / REASON / BANNED BY / "PENALTY ID 81", placeholders "SERVER NAME" | M |
| `12b-ban-screen-modern` | Mismo layout que 12 en **rojo** | M |
| `13-discord-sync-success` | Embed pDiscord "Account Verification" + botones Verify/Lock/Update/2FA | D |
| `13b-pdiscord-verify-embed` | Modal Discord "Verify Account" (nombre + código) | D |
| `13c-verify-modal` | Chat in-game con código sync "Here is your code ▶ 501394" | M |
| `13d-sync-code-ingame` | Title **"2FA / ON"** + chat "PDISCORD ▶ Two Factor Authentication." | M |
| `14-pm-format` | Ban screen **español vanilla** (sakurapvp): Baneado por / Motivo / Expira en / Discord | L |

Correctos: `15-chat-hover-format`, `15-rank-founder-menu`, `16`, `17`, `18`, `_refs/img/19`, `20`, `21`.
No existe imagen de webhook Visage (ref "02"). La ref "DUELOS" real es `0bf93c04`. La ref TAB HonlyCraft real es `1e1ddc2e`.

## Índice por feature

### Scoreboard / TAB / nametag
| Id | Fam | Contenido |
|---|---|---|
| `06-menu-duelos-style` | M | Scoreboard HonlyCraft (ver arriba) |
| `1e1ddc2e` | M | **TAB HonlyCraft**: header "HONLYCRAFT NETWORK", "¡75% DE DESCUENTO!", columnas Tú/Información/Online/modalidades/Staff/redes, footer "Hub-01 / ms / TPS" |
| `76d66228` | M | Sidebar fragmento: Bounty, Salud, Kit, Distancia (iconos) |
| `02-discord-webhook-format` | M? | Nametag in-game "Under Review" |

### Bossbar
`01b`, `07`, `07b`, `07c` (HonlyCraft, M, tips con comando entre paréntesis) · `c6de15bc` (M, "BOSSBAR MESSAGES" 3 barras "TIP: Type [/menu]…") · `ed767177` (M, queue actionbar rosa "Position 1/1 … ETA").

### MOTD lista de servidores
| Id | Fam | Contenido |
|---|---|---|
| `a7c897ce` | L | HonlyCraft 4 estados "NUEVO »" / "¡APERTURA! »" |
| `79f049e4` | L | HaliaCraft (competidor) |
| `cce9b43c` | L | Propio "IoriLabs PROJECT [1.8 - 1.21]" |
| `83f34044` | M | Propio "♦ ROYALCTW MAINTENANCE ♦" |
| `d1a3ceee` | L | Propio maintenance; **bug** hover "{MOTIVO=…}" sin resolver |
| `25cade3c`, `a256cfc6` | C | `motd.yml` MODERN/LEGACY (MiniMessage vs `&`) |

### Join MOTD / PictureLogin
`07d-bossbar-discord` (M, cara + branding HonlyCraft) · `1d15dd83` (M, texto "Bienvenido, X a HonlyCraft", soporte/tienda/redes, separadores cian).

### Menús / GUI / lore
| Id | Fam | Contenido |
|---|---|---|
| `0bf93c04` | M | **Tooltip "DUELOS [★]"**: Categoría, "» DESCRIPCIÓN", "¡Click para revisar!" |
| `15-rank-founder-menu` | L | Rank List tooltip: ID, Name, Prefix/Suffix, Priority, Type, Staff/Media, Permissions, Parents, "Click to edit rank" |
| `_refs/img/20-helium-rank-editor` | L | Rank Editor: Weight, Prefix, Color, Bold/Italic, Default/Staff/Admin/Op/Visible |
| `2d366fa2` | L | Lore pico: Statistics / Enchantments con barras "|" |
| `bf4de872` | L | "Perfiles - Menu Principal" (GUI simple) |

### Chat format / hover / PM
| Id | Fam | Contenido |
|---|---|---|
| `15-chat-hover-format` | M | Hover "OWNER+ X [AMETHYST]": Level, Experience, League, Money, "Click here…" (AlonsoTags) |
| `_refs/img/21-chatformat-tag-1.8` | L | Corazones + "[Founder] Tqmass » a" |
| `b85181f4` | M | "[Staff] [Admin] X (Lobby): Hello!" |
| `30e20acd` | L | GG wave arcoíris "[WILD+] X » GG" (otro server) |
| `05-staff-2fa-title` | M | PM "(To X)/(From X)" |

### Sanciones
`12-ban-screen-es` (teal) · `12b-ban-screen-modern` (rojo) · `14-pm-format` (ES vanilla) · `2002160f` (M, HonlyCraft "TU CUENTA HA SIDO SUSPENDIDA", fecha/expira/motivo/responsable/"ID de sanción #4") · `718c5c43` (M, anuncio "USUARIO SANCIONADO … Staff encargado").

### Staff auth / 2FA
`13d-sync-code-ingame` (title "2FA / ON") · ref texto del título en `DESIGN_BRIEF.md`.

### Discord
| Id | Contenido |
|---|---|
| `13`, `13b`, `13c`, `08` | Flujo pDiscord: embed → modal → código in-game → "SUCCESFULLY LINKED" |
| `4268a0f2` | Modal "Enter your Minecraft Sync Code" (HonlyCraft NW) |
| `8b7fdb8c` | Bot HonlyCraft "/sync" → "✅ Successfully linked…" |
| `6fcd4f34`, `15c36e90` | Bridge chat MC↔Discord propio ("[Lobby] …", "[Discord] …") |
| `eac81be5` | Embed bienvenida MEE6 (UniversoCraft) |
| `ec214f49` | Embed compra (ChampiCraft) |
| `_refs/img/19-discord-skin-modes` | Slash `/skin` tipos Face/Head/Body Front/Body 3D/Full |

### Logs por jugador
`17-chatlogs-per-player` · `18-cmdlogs-per-player` (paste Plasma/Helium: header Player/Total/Generated, `----- fecha -----`, `[HH:mm:ss] (server) [rank] name: …`, `[✓ ALLOWED]`/`[! BLOCKED]`).

### Easter eggs
`16-easter-egg-gift` (title "¡REGALO ENCONTRADO!", cabeza regalo, "TAG 2025 / ¡Evento temporal!") · `a78b9774` (M, "PALABRA SECRETA (Coloca el cursor encima)") ?.

### Donaciones / tienda
`3168fc4f` (L, ChampiCraft cara + barra [100$|500$] + "GG") · `3cfedb5d` (L, "AVISO OFICIAL … COMPRAR") · `69b624e6` (M, propio "-> NEW DONOR" EN) · `3ddad046`, `a12f51e3` (M, propio **bug** `</hover>` crudo).

### Salidas proxy propias (resultado / bug histórico)
- Help: `04fe36b0` (plano, antes) → `2325b427`, `3134cd73`, `e55b3ac5` (categorizado).
- Friends: `77b10549`, `56d71ce0`, `579e86aa`, `d7b8151b`, `eec9cdef`, `289be63d`, `2a24043c`, `ffb3bf4c`, `8d8f6500` · ref NexusProxy `949bf8e3`.
- Report/HelpOp: `c17b549d`, `d04d2a27`, `40a9b623`, `914c1f4e`, `2c6fa489` (title "NEW REPORT"), refs `a86b3330` ([MOVE]/[REPLY]), `9772a9cf`, `c3a5485e`.
- Stream: `06b89acd`, `43de7153`, `43ff4a2d`, `df604a58`, `f97c7d3a` · ref Twitch `e5362826`.
- Anuncios: `8e4919ec`, `59fe6026`, `df512d01`.
- Maintenance: `cea4833c`, `1496b1cb`, `2bbf338c`, `ee4bc9f6` (bug "Whitelist - X").
- Analytics/servers/queue/ping/seen/playtime: `7d32b6f3`, `4f77ebfb`, `755bee67`, `a73ac451`, `ba89284d`, `85d896dd`, `13cbd036`, `cd57dfeb`, `192cee4d`.
- Staffchat/network: `11ac8b51`, `beaaaa59`, `eefaf12e`, `65f7723b`, `23da8ca2`.
- Bugs históricos: `5f2331b8`/`f0b0ed8e` ("already connected" EN), `9e2a3e6a` ("tú enviado a lobby por"), `8b48f45b` (/ver lista plugins), `ed145421` (no-permission Bukkit por defecto).

### Consola / código / otros
`0facfc43` (banner Velocity, **bug** `§` crudo), `4a319cbb` (arranque H2 OK), `04b5d2b7` (LuckPerms), `d756ada9`, `35ec270f`/`6f3969bd`/`064294ab`/`8387251f` (árboles antiguos `me.tqmass.dev.proxy`), `5437a4da`, `ff1c5c1d` (código antiguo), refs terceros `0adebada` (Dolphin), `13d3129d`, `1918c1b5`, `1268d99f`, `1b480af8` (Stats).

## Duplicados exactos (md5)
`01`≡fb44c513 · `01b`≡113f9b9c · `02`≡622272cc · `05`≡fc15ce6e · `06`≡3e355262 · `07`≡5ab78b84 · `07b`≡91ee8676 · `07c`≡352b3dc5 · `07d`≡d0b71770 · `08`≡b1910939 · `12`≡8ea2c2a3 · `12b`≡0864e03f · `13`≡92a15a88 · `13b`≡71e0c2ab · `13c`≡d9fe3097 · `13d`≡e62b0753 · `14`≡a695f64c · `15-chat`≡eab7fd9a · `15-rank`≡f7b5dd27 · `16`≡02338684 · `17`≡54a8163a · `18`≡8383f26d · `19`≡ecdadbde · `20`≡ccdd8d2b · `21`≡83fd2180 · `0adebada`≡4ff58fa5 · `1268d99f`≡`1268d99f-…-03342605` · `13d3129d`≡b9453527≡fbe0ba06 · `1b480af8`≡cf7edf25 · `1d15dd83`≡599f7bf7 · `23da8ca2`≡4bf686e1 · `a78b9774`≡abd30bc1.

## Fuentes visibles
HonlyCraft (scoreboard, TAB, bossbars, MOTD, ban, sync) · pDiscord (flujo verify/2FA) · Helium/Plasma (logs, rank editor) · AlonsoTags (hover) · ChampiCraft, Kotsu, UniversoCraft, Mineluxe, NexusProxy, HaliaCraft, sakurapvp (refs de terceros). **Solo ideas**: no copiar layouts completos.
