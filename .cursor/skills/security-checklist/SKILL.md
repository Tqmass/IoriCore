---
name: security-checklist
description: Checklist de seguridad StaffReg (fase 10). Solo lectura; reporta bloqueantes.
---

# security-checklist

- [ ] Ningún secreto, OTC, contraseña o IP en claro en logs, YAML, SQL ni embeds
- [ ] OTC de un solo uso, canje atómico, TTL y rate-limit
- [ ] Botones validan `discord_id`, firma, TTL y estado del reto; un admin nunca aprueba lo suyo
- [ ] Un solo JDA activo con el mismo token; Core sin JDA
- [ ] Eventos del BRIDGE firmados; el proxy ignora los no firmados
- [ ] Cambio de país/región = permisos fuera, rango conservado
- [ ] 3 fallos = baneo, sesiones revocadas, alerta enviada
- [ ] Step-up sin flag de bypass; OP y `*` sin auth no conservan permisos
- [ ] Redis, SQL o Discord caídos = staff restringido
- [ ] Online-mode con forwarding seguro verificado
- [ ] Unlink y reset con aprobación de un segundo admin; todo en auditoría solo-append

Bloqueante = ítem fallido que permite bypass de auth o fuga de secretos.
