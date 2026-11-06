---
name: adaptador-plataforma
description: Crea/ajusta adaptadores Velocity o Bungee en impl/ sin ensuciar common.
---

# adaptador-plataforma

1. Interface/port en `common`.
2. `velocity/.../impl/*` y `bungee/.../impl/*` implementan el port.
3. Listeners de plataforma solo traducen eventos → servicio.
4. Adventure: Velocity nativo; BungeeAudiences create/close.
5. Probar ambos jars con `shadowJar`.
