# .cursor — índice IoriCore

Fuente de verdad local (IoriVelo / IoriBungee / IoriCore-Bukkit). El agente **debe** ser fiel a esto.

## Estructura

| Ruta | Uso |
|------|-----|
| `rules/` | Reglas always-apply / globs (estilo, deploy, seguridad) |
| `skills/` | Skills accionables (comando, módulo, prettier, investigación…) |
| `docs/memory/` | Decisiones, estilo, contexto (ahorro de tokens) |
| `design/` | Símbolos, mensajes, capturas de diseño OK |
| `_refs/` | Punteros a Darling / CoreDub / Helium / IoriCTW |
| `_decompiled/` | Extractos decompilados para análisis |
| `agents/` | Roles de agente auxiliares |

## Loop autónomo
- Regla `85-autonomous-dev-loop` — orquestación por solicitud.
- Anti-dup: agente `repository-auditor` (solo lectura) al planificar y tras implementar.
- No crear agentes/skills paralelos si ya cubren el dominio.

## Reglas clave
- `00-token-saver` — español corto, diffs mínimos
- `05-comentarios` — **cero** Javadoc narrativo
- `10-estilo-darling` — paquetes + Lombok fluent
- `15-playerdata-darling` — Profile-like split
- `60-formato-lombok-paquetes` — formato + imports
- `70-deploy-proxy` — shadowJar + copy jar
- `80-prettier-obligatorio` — prettier siempre
- `85-autonomous-dev-loop` — analizar→auditar→implementar→anti-dup→build→entregar

## Skills clave
- `prettier-always`, `java-lombok-format`, `playerdata-darling`
- `minecraft-platform-investigator`, `hooks-expert`, `nms-expert`
- `java-version-expert`, `public-src-researcher`, `security-data-optimizer`
- `post-task-hygiene`, `nuevo-modulo`, `nuevo-comando`
- `ioricore-architecture` — ownership Proxy/Core antes de features

## Refs locales
- Darling: `D:\Minecraft\Plugins\SoupPvP\Darling\src\main\java\me\vals\darling`
- Ver `_refs/README.md`
