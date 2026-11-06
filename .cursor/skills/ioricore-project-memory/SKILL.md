---
name: ioricore-project-memory
description: Mantener el conocimiento persistente de IoriCore (MEMORY.md, PHASE-STATUS, MODULE-INVENTORY, DECISIONS, OPEN-QUESTIONS, ARCHITECTURE, IMAGE-INDEX). Usar al terminar una tarea importante, al tomar/aprobar una decisión, o al empezar sesión para recuperar contexto.
---

# ioricore-project-memory

## Recuperar contexto (inicio de sesión)
`.claude/MEMORY.md` (auto-cargado) → doc concreto de su tabla → código. No releer todo `.cursor`.

## Qué actualizar (solo si cambió)
| Cambio | Documento |
|---|---|
| Estado de fase / módulo | `.cursor/docs/plan/PHASE-STATUS.md` (+ línea "Estado" de `MEMORY.md`) |
| Feature nueva / cerrada / stub eliminado | `.cursor/docs/plan/MODULE-INVENTORY.md` |
| Decisión **aprobada explícitamente por el usuario** | `.cursor/docs/memory/DECISIONS.md` (fecha, 2–4 bullets, qué sustituye) |
| Propuesta, contradicción, bug, duda | `.cursor/docs/OPEN-QUESTIONS.md` |
| Módulo/dependencia/tabla/canal nuevo | `.cursor/docs/ARCHITECTURE.md` |
| API verificada / fuente externa | `.cursor/docs/memory/RESEARCH_LOG.md` |
| Imagen nueva | `.cursor/docs/IMAGE-INDEX.md` |
| Log histórico de hitos (opcional) | `.cursor/docs/memory/PROGRESS.md` |

## Reglas
- Separar siempre: **confirmado** / **propuesto** / **pendiente**; **terminado** / **parcial** / **pendiente**. Una clase o esqueleto ≠ feature terminada.
- Nunca registrar una propuesta propia como decisión.
- Concisión: bullets, rutas, sin copiar código ni docs. `MEMORY.md` < 60 líneas.
- No registrar cambios triviales (formato, renombres locales, typos).
- Al resolver una pregunta de OPEN-QUESTIONS: moverla a DECISIONS y tacharla.
- Fechas absolutas (AAAA-MM-DD).
