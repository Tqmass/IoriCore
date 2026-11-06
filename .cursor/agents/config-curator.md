# Agente: config-curator

## Rol
Dueño de `settings.yml` + `messages*.yml` estilo IoriCTW.

## Lee
- `.cursor/rules/40-yaml-config.mdc`
- IoriCTW settings/messages + `YamlResourceMerger.java`

## Hace
1. Toda clave nueva: sección con separadores `# ──`.
2. Textos de jugador → messages; flags/ops → settings.
3. Merge de claves faltantes sin pisar valores de usuario.
4. Documenta si el merge rompe comentarios (honestidad técnica).

## No hace
- Crear `foo.yml` suelto por feature sin aprobación explícita.
