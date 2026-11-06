---
name: ioricore-nms
description: Abstraction NMS por versión (VersionWrapper, VersionMatcher, módulos nms/v*) de IoriCore-Bukkit. Usar al tocar nms/, abstraction, anvil input, títulos/actionbar/tab legacy o al añadir soporte a una versión de Minecraft.
---

# ioricore-nms

Modelo: AnvilGUI de WesJD (`abstraction` + `VersionWrapper` + `VersionMatcher` + módulo por revisión). Decisión en `DECISIONS.md` "Abstraction (no NMS bridge)". **Nunca** NMS en proxy/common/api.

## Ficheros
- Contrato: `nms/api/src/main/java/us/iorilabs/abstraction/VersionWrapper.java` (`revision`, `openAnvilInput`, `closeAnvilInput`, `sendLegacyTitle`, `sendActionBar`, `sendTabHeaderFooter`; defaults vacíos).
- Carga: `bukkit/.../abstraction/VersionMatcher.java` (1.8→v1_8_R3, 1.12→v1_12_R1, 1.16→v1_16_R3, 1.20→v1_20_R3, 1.21.*→v1_21_R1, **26.***→v26_R1; resto → fallback) + `FxAwareVersionWrapper` / `BukkitNativeFx` / `NoopVersionWrapper`.
- Impl: `nms/v<rev>/…/VersionWrapper_v<rev>.java`. Reales: v1_8_R3…v1_21_R1 shaded; **v26_R1** embebidо en `META-INF/ioricore-nms/v26_R1.jar` (bytecode 69 / JDK 25; Shadow no lo mergea — `VersionMatcher` lo carga con `URLClassLoader`).

## Añadir / completar una revisión
1. Confirmar que Bukkit/Paper API o Adventure no bastan (preferir API pública).
2. `nms/v<rev>/build.gradle.kts`: `implementation(project(":nms-api"))` + `compileOnly(files(".../Spigot Versions/spigot-<ver>-server.jar"))` + `libs-<ver>`; `release` = Java mínimo (8→1.16, 17→1.20.4, 21→1.21, **25→26.x** con TargetJvm 17). En 26.x: `-proc:none` (Lombok no soporta javac 25).
3. `settings.gradle.kts`: `include("nms-v<rev>")` + `projectDir`.
4. `bukkit/build.gradle.kts`: `implementation(project(":nms-v<rev>"))`.
5. Mapping en `VersionMatcher`.
6. Registrar versión verificada en `.cursor/docs/memory/RESEARCH_LOG.md`.

## Riesgos conocidos
- 1.21.x completo apunta a v1_21_R1 compilado contra 1.21.1: verificar clases en 1.21.2+.
- Huecos 1.9–1.11, 1.13–1.15, 1.17–1.19 → fallback Bukkit.
- Jars Spigot por ruta absoluta local (build no portable).
- Nunca filtrar tipos NMS por la interfaz pública (solo `UUID`, `String`, primitivos, callbacks).
