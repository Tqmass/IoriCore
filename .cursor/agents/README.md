# Agentes IoriProxy

| Agente | Archivo | Cuándo |
|--------|---------|--------|
| arquitecto-darling | `arquitecto-darling.md` | Refactors de paquetes/estructura |
| proxy-compat | `proxy-compat.md` | Velocity/Bungee / IoriVelo-IoriBungee |
| nms-specialist | `nms-specialist.md` | Core NMS bridges (nunca proxy) |
| config-curator | `config-curator.md` | settings/messages dual i18n/design |
| code-reviewer | `code-reviewer.md` | Fin de tarea / PR |
| context-keeper | `context-keeper.md` | Decisiones → docs/memory |

### StaffReg (fases 1–10)

| Agente | Archivo | Cuándo |
|--------|---------|--------|
| staffreg-architect | `staffreg-architect.md` | Fases 1, 5, 6 · límites módulos |
| staffreg-storage-engineer | `staffreg-storage-engineer.md` | Fase 2 · SQL/Redis/TTL |
| staffreg-auth-engineer | `staffreg-auth-engineer.md` | Fases 3–5, 7 · hash/OTC/riesgo |
| staffreg-discord-integrator | `staffreg-discord-integrator.md` | Fase 6 · JDA proxy + BRIDGE |
| staffreg-enforcement-engineer | `staffreg-enforcement-engineer.md` | Fase 8 · cancelación eventos |
| staffreg-security-reviewer | `staffreg-security-reviewer.md` | Fase 10 · checklist solo lectura |
| staffreg-qa-tester | `staffreg-qa-tester.md` | Todas · abuso y edge cases |

Plan: `.cursor/docs/staffreg-cursor-prompt.md` · regla: `.cursor/rules/staffreg.mdc`.

### Roles H0→H10 (usar Task + prompt; archivo dedicado solo si se repite)
| Rol | Responsabilidad |
|-----|-----------------|
| planner | Orquesta hitos; no código features sin OK |
| ref-auditor | Solo lectura Helium/Darling/CTW… |
| moderation-engineer | bans/mute/notes/freeze |
| staff-auth-engineer | /staff register-login + staffmode |
| discord-engineer | JDA bridge único en proxy |
| ui-designer | menús/TAB/scoreboard/bossbar/MOTD |
| security-reviewer | secrets, async, plugin msg |
| formatter | Prettier+Lombok **solo al final de hito** |

Invocar leyendo el `.md` del agente + rules relacionadas. Plan canónico: `docs/plan/PLAN.md`.
