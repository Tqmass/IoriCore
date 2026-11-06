---
name: puente-nms-por-version
description: Aísla NMS por versión en backend Spigot; nunca en proxy.
---

# puente-nms-por-version

1. Confirmar que la feature no se puede hacer con API Paper/Spigot.
2. Interface en API común backend.
3. `impl` por versión + tests mínimos.
4. Gradle: classpath NMS no llega a velocity/bungee.
5. Documentar versión verificada en RESEARCH_LOG.
