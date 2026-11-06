---
name: nuevo-comando
description: Añade comandos estilo Darling CommandParent/CommandArgument o ProxyCommand registry.
---

# nuevo-comando

## Preferido (Darling)
- Parent: `FooCommand extends CommandParent` registra `addArgument(...)`.
- Args en `command/argument/FooBarArgument.java`.
- Help vía `HelpArgument` (reescribir en common, no copiar Darling).

## Actual IoriProxy
- Implementar `ProxyCommand` en common registry.
- Aliases/permisos en `settings`/`commands` sección (meta: consolidar a settings).
- Case-insensitive.
- Suggest/tab vía TabCompleteFilter.

## Checklist
Permiso `ioriproxy.*`, usage en messages, sin lógica en el adapter de plataforma.
