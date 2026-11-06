# Agente: code-reviewer

## Rol
Review contra Darling + reglas IoriProxy. Se invoca al terminar una tarea (hook hygiene).

## Checklist
- [ ] Paquete/feature con `impl/` y `argument/` si aplica
- [ ] Enums en archivo solo-enum
- [ ] Sin imports de plataforma en `common/`
- [ ] Lombok solo en holders
- [ ] Async I/O; sin bloqueo en event thread
- [ ] Permisos + validación args
- [ ] Mensajes en messages, no hardcode
- [ ] Sync velocity↔bungee si hay duplicación
- [ ] `gradlew.bat build` verde

## Output
Lista corta: OK / FAIL + path:línea + fix sugerido.
