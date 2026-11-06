---
name: ioricore-security
description: Seguridad de la network IoriCore (staff auth, permisos, validación de args, plugin messaging anti-spoof, Redis, secretos, rate-limit, licencias de código). Usar al tocar autenticación, sanciones, mensajería entre servidores, comandos staff, webhooks/Discord o al revisar código de terceros.
---

# ioricore-security

Fuentes: `.cursor/rules/50-seguridad-rendimiento.mdc`, skills Cursor `plugin-messaging-seguro`, `security-data-optimizer`.

## Checklist
- **Args**: validar longitud, charset, UUID/nombre, URLs solo http/https. Permiso en cada comando y argumento (`ioriproxy.*` / `ioricore.*`).
- **Plugin messaging** `ioricore:main`: proxy marca handled/cancelled antes de procesar y solo acepta origen backend (✓ hoy). Backend no tiene firma: exige forwarding moderno Velocity / BungeeGuard y backends no expuestos. Acciones privilegiadas (p.ej. `STAFF_AUTH_SESSION`) nunca desde cliente.
- **Redis**: payload versionado y pequeño, dedupe por id, ignorar origen propio, auth/TLS por config. No confiar en datos sin validar.
- **Staff auth**: gate en proxy solo si cambia IP/geo vs última sesión; aprobación por Discord DM (JDA proxy); Core aplica blind/immobile/strip si falta sesión. Hash fuerte (Argon2/BCrypt). **Nunca** contraseñas, códigos ni tokens en logs/mensajes de error.
- **Secretos** (token bot, webhooks, BD, Redis): solo en YAML del servidor; no en repo, docs, capturas ni salidas.
- **Rate-limit**: report, helpop, stream, donations, GG, friends, comandos staff (cooldowns en settings).
- **Ban-check** fail-open + caché + log (decisión aprobada); no cambiar a fail-closed sin OK.
- **Licencias**: Darling/IoriCTW/IoriCTWL/CoreDub/propio = patrones OK. Helium/Plasma/BuiltByBit/pDiscord/temas LiteBans = solo ideas. kennytv/Maintenance GPL-3 = no copiar. Nada decompilado en `src/`.
- **Rendimiento**: I/O nunca en event loop / main thread.

## Salida esperada al auditar
Lista `SEV | fichero:línea | problema | fix` — solo hallazgos verificados en código.
