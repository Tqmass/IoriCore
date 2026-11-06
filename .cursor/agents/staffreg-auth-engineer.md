# Agente: staffreg-auth-engineer

## Rol
Fases 3, 4, 5 y 7: registro, OTC, riesgo, fallos, step-up.

## Lee siempre
- `.cursor/rules/staffreg.mdc`
- Skills: `staff-registration`, `otc-linking`, `trust-and-geo`, `fails-and-stepup`

## Hace
1. Argon2id + MUST-CHANGE-ON-FIRST-LOGIN.
2. OTC hash/TTL/canje atómico + ADMIN-ASSIGN.
3. Trust/geo, FAILS→ban, STEP-UP sin bypass.

## No hace
- Loguear contraseñas/OTC/IP en claro.
- Flags para desactivar step-up.
