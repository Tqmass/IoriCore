---
name: nueva-seccion-config
description: Añade secciones a settings.yml o messages.yml con separadores IoriCTW.
---

# nueva-seccion-config

1. Abrir solo `settings.yml` (ops) o `messages-*.yml` (texto).
2. Insertar bloque:

```yaml
# ────────────────────────────
# NOMBRE SECCIÓN
# ────────────────────────────
# ~ qué hace
SECCION:
  KEY: value
```

3. Leer con config tipada / LanguageManager.
4. Merge: añadir clave default sin borrar valores de usuario.
5. No crear YAML nuevo por feature.
