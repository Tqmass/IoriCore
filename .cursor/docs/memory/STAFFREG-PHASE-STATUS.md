# StaffReg phase status (2026-11-06 overnight)

Shipped: **1.1.6** — `IoriVelo` / `IoriBungee` / `IoriCore-Bukkit` (proxy + Lobby + Desktop).

Honest matrix (aligned with [Repo overnight](83f198c6-4e02-4c80-bb2c-d02ce3b31e81) + follow-up):

| Fase | Hecho | Stub / pendiente |
|------|--------|-------------------|
| **1 Dominio** | Ports, enums, DTOs, crypto (Argon2id, HMAC, bridge signer, button id) | — |
| **2 Storage** | PROXY mig 5–6; SQL accounts/audit; **SQL challenges/sessions/devices** vía `StaffRegSqlRunner` si MySQL/H2 up; memory fallback | Redis TTL OTC dedicado en proxy service |
| **3 Create** | `/staffreg create` Velo+Bungee+Bukkit; Argon2id; **REQUIRE-STEP-UP** en create | — |
| **4 Link** | `DISCORD_LINK_UPDATE`; ADMIN-ASSIGN / LINK_CONFIRM path | OTC StaffReg propio end-to-end; botones confirm firmados |
| **5 Trust/geo** | HMAC IP + `DefaultRiskEngine` + LOCATION challenge | GeoIP real; VPN detector; TRUST.EXPIRE-DAYS full |
| **6 Bridge** | JDA solo proxy; Bukkit BRIDGE-only; consumer IN + publisher | Cola DROP; CHANNEL-OUT resultados completos |
| **7 Fails/step-up** | Contador → BANNED_SECURITY; step-up **siempre** (ignora ENABLED=false); create exige step-up | Ban punish 24h LiteBans-style; botones Discord HMAC |
| **8 Enforcement** | Velocity restrict+cmds; Bukkit events 1.8-safe + Redis session | Listener Bungee; strip LP backend |
| **9 Admin** | lock/unlock/sessions; Bukkit create/link/status | reset/unlink 2º admin real |
| **10 Security** | Build verde; step-up no bypass | H10 formal / auditor falló por cuota Claude |

## Gaps next session
1. Unificar `StaffRegCore` (common) con `proxy.staffreg.StaffRegService`.
2. Bukkit create → SQL (`StaffRegSqlRunner` + Core storage).
3. Botones Discord firmados + CHANNEL-OUT.
4. GeoIP + VPN.
5. Unlink/reset two-admin flow.
