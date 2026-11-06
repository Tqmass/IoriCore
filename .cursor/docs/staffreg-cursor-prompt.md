# StaffReg (RoyalCTW / IoriCore) — Prompt maestro

Fuente de verdad operativa: `.cursor/rules/staffreg.mdc`.
Agentes: `.cursor/agents/staffreg-*.md` · Skills: `.cursor/skills/staff-*` / `otc-linking` / `trust-and-geo` / `discord-buttons` / `bridge-redis` / `fails-and-stepup` / `enforcement-events` / `security-checklist`.
Config: `velocity|bungee/src/main/resources/discord.yml` (JDA único en proxy).

## Fases

| # | Fase | Aceptación |
|---|---|---|
| 1 | Dominio y puertos | Compila sin Bukkit ni JDA |
| 2 | Storage | TTL, expiración, un solo uso |
| 3 | Registro admin | Argon2id, sin secretos en logs |
| 4 | Vínculo Discord | OTC + ADMIN-ASSIGN |
| 5 | Confianza y riesgo | Cambio de lugar → reto |
| 6 | Discord proxy + BRIDGE | Un solo JDA; Core sin JDA |
| 7 | FAILS + STEP-UP | Sin bypass |
| 8 | Enforcement | Eventos cancelados exhaustivos |
| 9 | Auditoría admin | 2º admin en unlink/reset |
| 10 | Security review | Checklist sin bloqueantes |

Al cerrar cada fase: build + resumen + riesgos → esperar OK del usuario.
