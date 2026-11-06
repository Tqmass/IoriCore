# Mega-refactor status · 2026-11-06

Versión jars: **1.1.6**.

## Estructura raíz
`velocity/` `bungee/` `bukkit/` `paper/` `common/` `api/` `NMS_api/` `NMS_v1_8_R3/` `NMS_v1_20_R3/` `NMS_21_R1/` `NMS_26_R1/`
- Borrado: `nms/v1_12*`, `nms/v1_16*`, `PortableGit-*`, `.claude/` (migrado a `.cursor`)
- Pack iori-cursor: agents/skills/rules fusionados

## Hecho
| WS | Estado |
|----|--------|
| 0 Reconocimiento | OK |
| 1 settings+discord move | OK (migrador one-shot) |
| 2 Queue deleted | OK → ROADMAP HubCore |
| 3 NETWORK-MSG out | OK; Core tiene /msg /r; sin socialspy Core |
| 4 Help+center | OK (`GENERAL.HELP-BAR` literal; TextCenterTest) |
| 5 discord.yml | Parcial (webhook SC, welcome/boost, status, punishments por tipo, staff-auth flow) |
| 6 community | Plantilla + DEFERRED tickets/panels/giveaways |
| 7 storage/data | OK punishments.yml catálogo; tags+PRICE; economy names→messages; settings limpio |
| 8 menus infra | Incremental (recursive, themes, states); no rediseño aesthetic |
| 9 TAB/chat/GGWAVE | OK infra; GGWAVE → Redis Core decorate |
| 10 build | shadowJars OK; deploy proxy+lobby+desktop |

## Checklist manual
- [ ] Reiniciar proxy con settings viejo → ver migración .bak
- [ ] Discord: MD cerrados + override staff-auth
- [ ] 1.8.9 + 1.21 menús/TAB
- [ ] Tickets/sorteos (aún deferred)
