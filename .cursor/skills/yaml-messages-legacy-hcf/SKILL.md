---
name: yaml-messages-legacy-hcf
description: Estilo messages legacy 1.8–1.12 IoriProxy. Sep &7&m-----&r. Help sin center. SOLO anuncios con <center>. PREFIX IoriProxy.
---

# Messages Legacy (1.8–1.12.2)

Solo `&` codes.

## Centrado

**SOLO** `ANNOUNCEMENTS` usa `<center>...</center>`.
Help, analytics, whitelist, friends, reports → **sin** center.

## Separadores

```
'&7&m------------------------------------------&r'
```

Siempre `&m` + `&r` al final. Prohibido `▬`. Listas con `rawList` línea a línea.

## Help

```yaml
- '&7&m------------------------------------------&r'
- '&8• &f/maintenance on [duracion] &7- &eActivar (auto-off).'
- '&8• &f/maintenance on countdown <tiempo> [duracion]'
- '&7&m------------------------------------------&r'
```

## Maintenance args

- `[duracion]` = cuánto dura (auto `/off`), **no** countdown
- `countdown <tiempo>` = aviso antes de activar

## PREFIX

`&8[&bIoriProxy&8] &r`
