---
name: java-minecraft-platform-engineer
description: Implement and review Java code for IoriProxy Velocity/Bungee modules, Redis, SQL/Mongo, Adventure/MiniMessage, and async proxy work.
---

# java-minecraft-platform-engineer

## Instructions

1. Conocer las diferencias entre Java 8, 11, 17, 21 y 25 sin asumir que una API es compatible con todas las versiones.
2. Verificar los requisitos reales de Velocity, BungeeCord, Adventure y las dependencias utilizadas. IoriProxy targets Java 17; Velocity 3.4 needs 17+.
3. Separar las APIs de cada plataforma mediante adaptadores (`velocity/`, `bungee/`). `common/` = utilidades + command framework; la lógica de features se duplica en velocity y bungee bajo `me.tqmass.dev.proxy.*`.
4. Evitar bloqueos del hilo principal y gestionar correctamente las operaciones asíncronas.
5. Priorizar estructuras de datos apropiadas, concurrencia segura y cierre correcto de recursos.
6. Utilizar las APIs oficiales en lugar de reflexión o acceso interno cuando sea posible.
7. Mantener el código simple y evitar capas innecesarias.
8. Añadir pruebas de unidad para la lógica común y pruebas de integración para Redis, SQL y los adaptadores.
9. Revisar compatibilidad binaria y bytecode cuando existan módulos con diferentes versiones de Java.
10. No inventar métodos, clases ni opciones de configuración.
11. Usar Java (no Kotlin). Build con Gradle.
12. Paquete base: `me.tqmass.dev.proxy`. Permisos: `ioriproxy.*`.
13. MiniMessage + `<#RRGGBB>` en mensajes de usuario; perfiles legacy cuando el protocolo no soporte hex/RGB.
