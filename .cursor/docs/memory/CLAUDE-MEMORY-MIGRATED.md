# MEMORY — índice de conocimiento IoriCore
Actualizado: 2026-11-03 (auditoría inicial Claude Code). Mantener < 60 líneas.

## Estado
- Versión `1.1.0` · JDK 17 · **sin tests** · sin git.
- Roadmap R1–R8 código ✓ (R7 ◐) · **R9 cerrado** (prettier+build+deploy) → `.cursor/docs/plan/PHASE-STATUS.md` (GAPS post-R9).
- H0 ✓ · H1 ◐ · H2 ◐ · H3 ✓ · H4/H5/H7/H8 ✓? · H6/H9 ◐ · H10 ○ (tests/auditoría).
- Contradicciones A1–A15 abiertas → `.cursor/docs/OPEN-QUESTIONS.md`.

## Arquitectura (resumen; detalle `.cursor/docs/ARCHITECTURE.md`)
- Gradle: `:common :api :velocity :bungee :bukkit :nms-api :nms-v1_8_R3 :nms-v1_12_R1 :nms-v1_16_R3 :nms-v1_20_R3 :nms-v1_21_R1`.
- Paquetes: proxy+common `us.iorilabs.proxy.*` · `us.iorilabs.api.*` · `us.iorilabs.bukkit.*` · `us.iorilabs.abstraction` · `us.iorilabs.nms.v*`.
- velocity ≡ bungee: 127/129 ficheros idénticos (deuda principal).
- Tablas `ip_*` (proxy) + `ic_*` (core; `ic_punishments` compartida). Redis channel `ioriproxy`. Plugin channel `ioricore:main`.

## Decisiones críticas aprobadas (`.cursor/docs/memory/DECISIONS.md`)
NMS solo Core · JDA en Core (proxy JDA temporal dual-path) · Redis bus · MySQL + H2 fallback · ban-check fail-open+caché · punishments propios + LiteBans soft · ranks propios + LP soft · staff auth proxy IP/geo + Redis→Core DM approve · ChatGames solo Core · `bukkit/` (no `/core/`) · `paper/` solo si hace falta · CommandParent + BukkitCommandBridge · i18n en/es · brand RoyalCTW.

## Mapa de documentos (leer bajo demanda)
| Necesito | Fuente |
|---|---|
| Objetivo, ownership, hitos H0–H10 | `.cursor/docs/plan/PLAN.md` |
| Módulos y features E2E (W1–W3/FINAL) | `.cursor/docs/plan/MODULE-INVENTORY.md` |
| Estado real por fase | `.cursor/docs/plan/PHASE-STATUS.md` |
| Arquitectura real | `.cursor/docs/ARCHITECTURE.md` |
| Decisiones aprobadas | `.cursor/docs/memory/DECISIONS.md` |
| Contradicciones / dudas / bugs | `.cursor/docs/OPEN-QUESTIONS.md` |
| Estilo Java/YAML | `.cursor/docs/memory/STYLE_GUIDE.md` + `.cursor/rules/60-*.mdc`, `40-*.mdc` + `.cursor/design/` |
| Diseño visual dual | `.cursor/docs/memory/DESIGN_BRIEF.md` + `.cursor/docs/IMAGE-INDEX.md` (⚠ nombres 01–14 desplazados) |
| Backlog extras | `.cursor/docs/memory/EXTRAS_BACKLOG.md` |
| Refs de código y licencias | `.cursor/_refs/README.md` |
| APIs verificadas / fuentes | `.cursor/docs/memory/RESEARCH_LOG.md` |
| Historial de progreso | `.cursor/docs/memory/PROGRESS.md` (log, no canónico) |

## Bloqueos actuales
- Sin `proxy-common`; regla 60 aún manda duplicar → requiere OK (A3).
- Layout YAML Core distinto a DECISIONS (A5).
- Redis en Core = noop sin Jedis externo.
- Transcript del master prompt no recibido.
