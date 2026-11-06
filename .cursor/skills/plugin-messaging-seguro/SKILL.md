---
name: plugin-messaging-seguro
description: Canal proxy↔backend seguro (anti-spoof). Usar al hablar plugin messages.
---

# plugin-messaging-seguro

Fuente: https://docs.papermc.io/velocity/dev/plugin-messaging/

1. Canal `ioriproxy:<name>`.
2. En Velocity `PluginMessageEvent`: si el identifier coincide → `setResult(handled())` **antes** de procesar.
3. Solo aceptar mensajes cuyo source sea el lado esperado (backend vs proxy), nunca confiar en el cliente.
4. Backend: no ejecutar acciones privilegiadas solo porque “llegó un plugin message”.
5. Preferir Redis firmado/validado entre proxies; plugin messaging para hints locales.
