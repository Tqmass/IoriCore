---
name: ioricore-i18n
description: Configuración YAML e internacionalización de IoriCore (settings vs messages, en/es, modern/legacy, formats, menús, separadores y comentarios IoriCTW, placeholders). Usar al crear o editar cualquier .yml o al añadir texto visible al jugador.
---

# ioricore-i18n

Fuentes: `.cursor/rules/40-yaml-config.mdc`, `.cursor/design/yaml-comments.md`, `.cursor/design/messages/*.yml`, skill Cursor `nueva-seccion-config`.

## Separación
- **Funcional** (flags, tiempos, slots, coords, toggles) → `settings.yml` / YAML de dominio. **Nunca** textos de jugador.
- **Texto** → `messages/{en,es}.yml` (modern) + `messages/legacy/{en,es}.yml`. Mismas claves en los 4 ficheros (y en velocity + bungee).
- Toggles de módulo → `settings.yml → MODULES.*`. Meta → `commands.yml` + `permissions.yml`.

## Layout real hoy (≠ DECISIONS, pendiente A5 — no migrar sin OK)
- Core: `chat/formats/{en,es}.yml` + `chat/formats/legacy/` · `menus/{modern,legacy}/<menu>.yml` · `data/{easter-eggs,tags,coinshop,chatgames}.yml` · `ranks.yml` · `sounds.yml`.
- Proxy: `settings commands permissions database redis discord donations fallback maintenance motd queue .yml` + `webhooks/*.yml` (estilo Helium TITLE/DESCRIPTION).
- Decisión aprobada (01-11): menús en `messages/menus/{en,es}.yml`, formats en `messages/formats/{en,es}.yml` + `messages/legacy/formats/`, easter eggs `easter-eggs.yml` + `settings.yml → EASTER-EGGS:`. Mientras A5 esté abierta, seguir el patrón **del fichero que se toca** y no crear un tercer layout.

## Estilo
- Claves UPPERCASE kebab (`GENERAL.NO-PERMISSION`). Separadores `# ***` cabecera, `# ────` sección, `# — /cmd ———` por comando. `# ~` explica un flag. Bloque `#  Placeholder's ↓` + `#   · {KEY}`.
- Colores: `&`, `<#RRGGBB>` y MiniMessage (normaliza `ColorUtil`). Legacy: solo `&`.
- Placeholders `{UPPER}` en messages; webhooks `%player%`/`<player>`. Emojis unicode reales.
- Merge: añadir claves nuevas sin pisar valores del usuario.
- Prettier YAML tras editar (`tabWidth 2`).
