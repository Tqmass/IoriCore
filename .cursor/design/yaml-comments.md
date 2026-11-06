# Estilo comentarios YAML (aprobado)

Usar en configs nuevas / al tocar YAMLs.

## Placeholders (bloque intro)

```yaml
#  Placeholder's ↓
#   · {PLAYERNAME} {DISPLAYNAME} {RANK}
#   · {RANKCOLOR} {COLOR} {MESSAGE}
#  Also supports PlaceholderAPI if installed --
```

## Placeholders (inline sobre una sección)

```yaml
CHAT:
  ENABLED: true
  # ~ Placeholders: {PREFIX} {NAME} {TAG} {RANK} {RANKCOLOR} {COLOR} {MESSAGE}
  # ~ Also supports PlaceholderAPI if installed
```

## Separadores de sección

```yaml
# ────────────────────────────
# NOMBRE SECCIÓN
# ────────────────────────────
```

## Separadores por comando (commands.yml)

```yaml
  # — /ping ————————————————————
  PING: {}
```

## Separadores por feature (messages)

```yaml
# — DONATIONS ———————————————————
DONATIONS:
```

## Notas cortas

- `# ~ ...` para explicar qué hace un flag/campo (estilo IoriCTW).
- Bullets de lista en comentarios: `#   · item`
