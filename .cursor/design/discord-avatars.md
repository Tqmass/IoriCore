# Discord webhook — avatares / skins

## Default (bust / mitad de skin)

```yaml
avatar-url: "https://visage.surgeplay.com/bust/152/%player%"
# o en IoriProxy:
AVATAR-URL: "https://visage.surgeplay.com/bust/152/%player%"
THUMBNAIL-URL: "https://visage.surgeplay.com/bust/152/%player%"
```

## Alternativas

```yaml
# ────────────────────────────
# CONFIGURACIÓN DE DISCORD WEBHOOK
# ────────────────────────────
#
# Puedes usar el avatar-url
#  · https://minotar.net/helm/%player%/128.png
# Para usar la cabeza del jugador, si no puedes usar:
#  · https://visage.surgeplay.com/bust/152/%player% para usar la mitad de tu skin
```

| Uso | URL |
|-----|-----|
| Bust (default RoyalCTW) | `https://visage.surgeplay.com/bust/152/%player%` |
| Helm / cabeza | `https://minotar.net/helm/%player%/128.png` |

Placeholder: `%player%` (nombre Minecraft).

## Placeholders en webhooks
Soportados en **todo** el YAML (content, title, description, urls, footer, avatar):
- `%player%` / `<player>`
- `%server%` / `<server>`
- `%url%` / `<url>`
- etc. según el evento

## Emojis
Usar emoji Unicode real (`🎥` `🛒` `🆘`), **no** shortcodes Discord (`:red_circle:`).

## stream.yml
Misma connection; embeds separados:
- `STREAM:` → `/stream`
- `VIDEO:` → `/video`

