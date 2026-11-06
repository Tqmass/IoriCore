---
name: trust-and-geo
description: Primer login StaffReg, HMAC de IP, país/región/ciudad, VPN/proxy, CHANGE-DETECTION. Usar en fase 5.
---

# trust-and-geo

## Pasos
1. Primer login exitoso → guardar HMAC(IP), country, region, city según `AUTH.TRUST.STORE`.
2. `EXPIRE-DAYS: 0` = sin caducidad temporal; invalidar solo por cambio/bloqueo/revocación.
3. `CHANGE-DETECTION`: country/region (city solo si true); VPN-PROXY = cambio.
4. `ON-CHANGE`: strip permisos, keep rank tag, challenge MD + password fallback.
5. IP en embeds siempre enmascarada; storage solo HMAC.

## No hacer
- Guardar IP en claro.
- Activar CITY sin aceptar falsos positivos.
- Pedir login en cada join si el lugar no cambió.
