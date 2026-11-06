---
name: network-architecture-research
description: Investigate and compare proxy/network solutions before architectural changes in IoriProxy or related Minecraft proxy plugins.
---

# network-architecture-research

## Instructions

1. Inspeccionar el repositorio antes de proponer cambios.
2. Priorizar documentación oficial y código fuente verificable.
3. Comprobar las versiones de las APIs y dependencias.
4. Separar hechos verificados, inferencias y cuestiones pendientes.
5. Comparar alternativas solo cuando exista una decisión real.
6. Priorizar soluciones simples, eficientes y mantenibles.
7. Revisar licencias antes de reutilizar o redistribuir código (p. ej. GPL de kennytv/Maintenance = no copiar código a proyectos propietarios sin cumplir GPL).
8. No introducir dependencias ni servicios adicionales sin una justificación concreta.
9. Presentar riesgos de compatibilidad antes de modificar módulos compartidos.
10. No afirmar que una funcionalidad funciona hasta que haya pruebas que lo demuestren.
11. Referencias locales a auditar cuando aplique: Helium, CoreDub Proxy-API, IoriCTW, Darling, HonlyCore, Fallback-Server ideas.
12. Distinguir transporte real Redis (Jedis) de middlewares TCP custom (CoreDub RedisDub).
