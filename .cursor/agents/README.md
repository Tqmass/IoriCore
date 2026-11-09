# Agentes IoriCore (monorepo IoriVelo / IoriBungee / IoriCore-Bukkit)

Orquestación: regla `85-autonomous-dev-loop`. Anti-duplicación: `repository-auditor` (solo lectura).

| Agente | Archivo | Cuándo |
|--------|---------|--------|
| repository-auditor | `repository-auditor.md` | Mapa repo + dups/huérfanos/solapes (antes/después de implementar) |
| architecture-reviewer | `architecture-reviewer.md` | Ownership Proxy/Core y deps Gradle |
| arquitecto-darling | `arquitecto-darling.md` | Refactors de paquetes/estructura |
| proxy-compat | `proxy-compat.md` | Velocity/Bungee / IoriVelo-IoriBungee |
| nms-specialist | `nms-specialist.md` | Core NMS bridges (nunca proxy) |
| config-architect | `config-architect.md` | Reordenar/borrar ymls, storage |
| config-curator | `config-curator.md` | settings/messages dual i18n/design |
| java-code-reviewer | `java-code-reviewer.md` | Review estilo + build del diff |
| code-reviewer | `code-reviewer.md` | Checklist fin de tarea / hook |
| qa-reviewer | `qa-reviewer.md` | Cierre workstream |
| security-auditor | `security-auditor.md` | Auth, messaging, secretos |
| discord-bot-engineer | `discord-bot-engineer.md` | JDA / discord.yml |
| menu-designer | `menu-designer.md` | Menús / lore |
| ui-reference-analyst | `ui-reference-analyst.md` | Refs img UI |
| context-keeper | `context-keeper.md` | Decisiones → docs/memory |

### Roles H0→H10 (solo prompt; sin `.md` propio — mapear a agentes reales)
| Rol (legado) | Usar en su lugar |
|--------------|------------------|
| planner / orquestador | Regla `85-autonomous-dev-loop` |
| ref-auditor | `repository-auditor` + skill `bring-best-references` |
| moderation-engineer | Feature punish/staffmode (Core) + docs PLAN |
| staff-auth-engineer | `staffreg-auth-engineer` / `staffreg-*` |
| discord-engineer | `discord-bot-engineer` |
| ui-designer | `menu-designer` + `ui-reference-analyst` |
| security-reviewer | `security-auditor` (+ `staffreg-security-reviewer` si StaffReg) |
| formatter | skill `prettier-always` + `ioricore-java` al final de hito |

Invocar leyendo el `.md` del agente + rules relacionadas. Plan canónico: `docs/plan/PLAN.md`.
