---
name: iori-discord-jda
description: Diseño e implementación del bot de Discord (JDA) de la red Iori - discord.yml por bloques con canal propio, staff-chat por webhook, welcome/goodbye/boost de Discord, comando status por servidor, embeds de sanciones, aprobación staff-auth por MD con override visible, tickets, sorteos y paneles con botones y desplegables, y cómo reutilizar Snowfall (Apache-2.0). Úsala SIEMPRE que toques discord.yml, discord-community.yml, JDA, botones, desplegables, webhooks, tickets, sorteos o staff-auth en Discord.
---

# Bot de Discord Iori (JDA)

## 1. Principios

- **Un solo gateway por token.** Destino: JDA en el proxy; IoriCore habla por Redis. Antes de tocar nada averigua qué JVM lo abre hoy. Nunca dos conexiones con el mismo token.
- Código del bot en un paquete `discord/` independiente del host (proxy o Core) para poder moverlo.
- Config por bloque: `ENABLED` + `CHANNEL-ID` propio + plantilla. Canal vacío = bloque inactivo o canal por defecto; nunca excepción.
- Persistencia con los repositorios de IoriCore (Mongo o MySQL). Sin tablas ad hoc.
- Solo **dos** ymls: `discord.yml` (infra + eventos de red) y `discord-community.yml` (tickets, sorteos, paneles).

## 2. Intents y permisos

| Necesidad | Intent / permiso |
|---|---|
| welcome / goodbye / boost, futuro Role Sync | `GUILD_MEMBERS` (privilegiado: activar en el portal) |
| comandos solo slash | ningún `MESSAGE_CONTENT` |
| comandos con prefijo | `MESSAGE_CONTENT` (privilegiado) - evitarlo si es posible |
| webhook auto-creado | `MANAGE_WEBHOOKS` |
| tickets | `MANAGE_CHANNELS`, `MANAGE_ROLES` (permisos por canal) |
| MD | el usuario debe permitirlos; siempre hay camino si fallan |

## 3. Interacciones

- Responder en < 3 s: `deferReply(true)` si hay trabajo (DB, canales).
- Respuestas **efímeras** por defecto para paneles, desplegables y confirmaciones.
- `custom ID`: `iori:<módulo>:<acción>:<arg>` (máx. 100 caracteres). **Validar en servidor** quién pulsa, permisos y vigencia; nunca confiar en el cliente.
- Los mensajes de panel persisten: al arrancar se registran los listeners por prefijo de ID, no por mensaje en memoria.
- Límites: 5 filas × 5 botones por mensaje, 25 opciones por desplegable, 5 campos por modal.

## 4. Bloques de `discord.yml`

**STAFF-CHAT (webhook por usuario):**
```yaml
STAFF-CHAT:
  ENABLED: true
  CHANNEL-ID: ""
  WEBHOOK-URL: ""                 # o AUTO-CREATE-WEBHOOK: true (requiere MANAGE_WEBHOOKS)
  USERNAME: "%player%"
  AVATAR-URL: "https://visage.surgeplay.com/bust/152/%player%"
  FORMAT: "**[%server%]** %message%"
```
- `allowed_mentions` vacío siempre; sanea `@everyone`, `@here`, roles y enlaces de invitación si no se permiten.
- Cliente de webhook cacheado; cola con respeto de rate limit; si falla, un solo aviso en consola.

**WELCOME / GOODBYE / BOOST (del servidor de Discord):** `GuildMemberJoinEvent`, `GuildMemberRemoveEvent`, boost del servidor. Placeholders: `%user% %mention% %guild% %members% %booster% %boosts%`. Nada de Minecraft aquí.

**STORE:** se mantiene el embed actual (comprador, paquete, precio, modalidad, bust del jugador). Se registra solo si `MODULES.DONATIONS` está activo.

**STATUS:** `/status <server>` con autocompletado de servidores. Campos: estado, online/max, mantenimiento, ping/TPS si SERVER-HEALTH los aporta. Datos del servidor pedido, no del proxy. Sin avisos de arranque/parada del proxy.

**PUNISHMENTS:** un bloque por tipo (`BAN`, `TEMPBAN`, `UNBAN`, `MUTE`, `TEMPMUTE`, `UNMUTE`, `WARN`, `KICK`) con `CHANNEL-ID` propio (vacío = `DEFAULT-CHANNEL-ID`) y embed propio. Placeholders: `%player% %type% %duration% %reason% %actor%`.

## 5. STAFF-AUTH por MD (flujo y seguridad)

1. Aviso **informativo** en `NOTIFY-CHANNEL-ID` para todo el staff (jugador, IP enmascarada, país). Sin botón de aprobar.
2. **MD al staff** (su Discord vinculado) con `Aprobar` / `Denegar`. Custom ID con token; al pulsar:
   - quien pulsa == Discord vinculado a ese staff (o tiene override);
   - token existente, no usado y dentro de `TOKEN-TTL-SECONDS`;
   - se consume el token (un solo uso) antes de aprobar.
3. **Override:** admin/owner con `OVERRIDE.ROLE-IDS` o permiso MC `ioriproxy.staffauth.override`. Siempre visible: se edita el aviso del canal ("Aprobado por @X (override)") y se registra en `OVERRIDE.LOG-CHANNEL-ID`.
4. MD cerrados: el aviso de canal queda y solo el override puede resolver. Nunca se aprueba sin identidad.
5. Tras resolver: deshabilitar botones y editar el mensaje (estado final + quién).
6. Mantener `/staffauth approve <token>` en juego, con `SELF-APPROVE` bloqueado salvo override.

## 6. `discord-community.yml`

### Paneles
```yaml
PANELS:
  support:
    CHANNEL-ID: ""
    EMBED: { COLOR: "#5865F2", TITLE: "Soporte", DESCRIPTION: "Abre un ticket para hablar con el staff." }
    COMPONENTS:
      - TYPE: BUTTON
        ID: open
        LABEL: "Abrir ticket"
        STYLE: PRIMARY
        ACTION: "OPEN_TICKET_MENU"          # abre el desplegable de categorías (efímero)
      - TYPE: STRING_SELECT
        ID: quick
        PLACEHOLDER: "Acceso rápido"
        OPTIONS:
          - { LABEL: "Vincular cuenta", VALUE: link, ACTION: "LINK_ACCOUNT" }
          - { LABEL: "Reglas", VALUE: rules, ACTION: "REPLY:RULES" }
```
- Acciones soportadas: `OPEN_TICKET_MENU`, `OPEN_TICKET:<categoría>`, `LINK_ACCOUNT`, `ROLE_TOGGLE:<roleId>`, `REPLY:<clave>`.
- `/panel send <id> [canal]`, `/panel list`. El mensaje enviado se guarda (id de canal/mensaje) para editarlo en lugar de duplicarlo.

### Tickets
- Flujo: botón → **desplegable de categorías** → modal opcional (motivo) → canal privado bajo la categoría de Discord de esa categoría → mensaje de bienvenida con botones `Reclamar`, `Cerrar`, `Transcript`.
- `CATEGORIES.<id>`: `NAME`, `EMOJI`, `DESCRIPTION`, `DISCORD-CATEGORY-ID`, `STAFF-ROLE-IDS`, `NAME-FORMAT` (`ticket-%number%`), `MODAL` opcional.
- `SETTINGS`: `MAX-OPEN-PER-USER`, `COOLDOWN-SECONDS`, `CLOSE-CONFIRMATION`, `DELETE-AFTER-SECONDS`, `LOG-CHANNEL-ID`.
- Comandos: `/ticket add|remove|rename|close`.
- Cerrar: confirmar → transcript (archivo adjunto en el log) → eliminar canal tras el retraso. Todo registrado.
- Numeración secuencial atómica en DB.

### Sorteos
- `/giveaway start <premio> <duración> <ganadores>`, `end`, `reroll`, `cancel`, `list`.
- Botón participar; participación = documento/fila con **índice único** (usuario + sorteo), no reacciones.
- Requisitos: rol, cuenta de MC vinculada. Mensaje efímero explicando por qué no entra.
- Contador en el embed actualizado por intervalo (≥ 30 s), no por click.
- Fin automático con scheduler **persistente**: al arrancar se reprograman los activos y se cierran los vencidos.
- Ganadores por MD (con fallback al canal), reroll sin repetir.

## 7. Snowfall como referencia (Apache-2.0)

Repo: https://github.com/v4lss/Snowfall (paquete `club.watschin.snowfall`, JDA + Maven + MongoDB, config en `config.yml`, `messages.yml`, `categories.yml`, `moderation.yml`).
- Úsalo para entender **comportamiento y estructura** de tickets y sorteos.
- No lo importes entero: exige MongoDB y su propio cargador de config; aquí se usa el storage y la config de IoriCore.
- Si adaptas código: conserva cabecera de licencia/atribución, indica que está modificado y añade `NOTICE`.
- Sugerencias, verificación y antiraid de Snowfall quedan fuera de alcance (backlog).

## 8. Role Sync (futuro, NO implementar ahora)

Otorgará rangos in-game y ejecutará comandos según los roles de Discord del jugador, y los quitará al perderlos. Requiere cuenta vinculada, LuckPerms, intent `GUILD_MEMBERS` y re-sincronizar al login y al cambiar roles. Helium es la referencia de código. Ahora solo: sección en `docs/ROADMAP.md` y bloque `ROLE-SYNC: { ENABLED: false }` comentado en `discord-community.yml`.

## 9. Checklist
- [ ] Un solo gateway; intents declarados y documentados.
- [ ] Todo bloque con `ENABLED` y `CHANNEL-ID`; canal vacío no rompe.
- [ ] Webhook con `allowed_mentions` vacío.
- [ ] Botones validados por identidad y token de un solo uso con TTL.
- [ ] Paneles, tickets y sorteos sobreviven un reinicio.
- [ ] Caminos definidos para MD cerrados, canal borrado, bot sin permisos.
- [ ] Tokens y URLs de webhook fuera de los logs.
