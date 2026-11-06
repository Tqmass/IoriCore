---
name: iori-config-hygiene
description: Reglas para ordenar la configuración de IoriProxy e IoriCore - qué es config, contenido, mensaje, menú o dato de runtime; un dato una fuente de verdad; pocos yml con banners y separadores; carpeta data/ solo para runtime; storage MongoDB o MySQL sin bridge; migradores con backup; borrado completo de módulos. Úsala SIEMPRE que muevas, borres, dividas o crees un yml, toques settings.yml, discord.yml, tags, coinshop, punish, data/ o la base de datos, aunque el usuario solo diga "ordena esto".
---

# Higiene de configuración Iori

## 1. ¿Dónde va cada cosa?

Antes de mover un archivo, clasifícalo:

| Tipo | Ejemplo | Destino |
|---|---|---|
| Ajuste / interruptor | `MODULES`, cooldowns, staff-mode, `DATABASE` | `settings.yml` |
| Texto visible | help, avisos, hover | `messages` (por idioma) |
| Catálogo de contenido | tags (con precio), motivos de sanción | su propio yml (uno por catálogo) |
| Interfaz | menús, submenús | `menus/` y `menus/submenus/` |
| Sonidos | claves de sonido | `sounds.yml` |
| Dato de runtime | sanciones, tags equipados, sesiones de staff | **DB** (Mongo/MySQL); `data/` solo como fallback sin DB |

Regla de oro: **`data/` es data.** Si un jugador/admin lo edita para cambiar el comportamiento, no es data, es config.

## 2. Un dato, una fuente de verdad

Síntomas de duplicación y su arreglo:
- Mismo id en dos ymls (`tags` y `coinshop`) → el precio vive en el tag; la tienda se genera desde el catálogo.
- Permisos que siguen un patrón (`ioricore.tags.<id>`) → plantilla en `DEFAULTS`, solo se escribe el permiso si se sale del patrón.
- Fallbacks repetidos por ítem (`MATERIAL-FALLBACK`) → un resolver central en código.
- Bloques de estilo copiados (filler, acento) → `THEMES`.
- Idioma repetido en estructura (EN/ES × legacy) → la estructura es única; solo los textos cambian por idioma.
- Un yml que solo apunta a otros (`MENUS: …`, `REASONS-PACK: …`) → se borra; se resuelve por convención de rutas.
- Valores derivables (comando de sanción por categoría) → plantilla en `settings.yml`, no en cada ítem.

## 3. Estilo de archivo

```yaml
# *************************************
# *       IoriCore | Nombre           *
# *************************************
# ~ Qué contiene y placeholders disponibles

# ────────────────────────────
# SECCIÓN
# ────────────────────────────
CLAVE-EN-KEBAB: valor
```
- Pocos archivos; no crees uno nuevo si cabe en uno existente.
- Claves `UPPER-KEBAB`; comentarios `# ~` para notas, `#` para decoración.
- Orden: lo que se mira primero va arriba (`SETTINGS` → `MODULES` → resto).
- Valores por defecto sensatos; ningún valor peligroso por defecto sin comentario (p. ej. autoban permanente al desconectar en freeze).

## 4. Storage: Mongo o MySQL, sin bridge

- `DATABASE.TYPE: MONGODB | MYSQL`. Un repositorio por agregado: interfaz mínima + dos implementaciones **directas**.
- Prohibido: capa genérica que traduzca entre motores, sincronización/migración entre ellos, ORM propio.
- El modo fichero (`data/`) es el fallback ya existente para cuando no hay DB: se mantiene, no se amplía y no exige paridad de funciones nuevas.
- Conexiones en un pool, operaciones de DB fuera del hilo principal, sin bloqueos en eventos de chat/join.
- Lo que guarda el registro de staff (StaffReg), mínimo: UUID, Discord vinculado, hash de contraseña (si se mantiene), última sesión aprobada (IP **hasheada o enmascarada**, país, ciudad), contador de fallos, timestamps. Todo lo derivable, fuera.

## 5. Migraciones

- `CONFIG-VERSION: N` en el yml. Al cargar: si `N` es menor, backup `archivo.yml.bak-<fecha>`, migrar, avisar en consola.
- Idempotente: ejecutarla dos veces no cambia nada.
- Conserva valores del usuario (permisos, precios, textos) aunque cambie la estructura.
- Al mover secciones entre archivos (p. ej. `STAFF-AUTH` de `settings.yml` a `discord.yml`): copiar una vez, avisar, no volver a escribir en el origen.

## 6. Borrar un módulo entero

Checklist (ejemplo: Queue, `/msg` de red):
1. Clases y registro del módulo; interruptor en `MODULES`.
2. Comandos, aliases, permisos, `plugin.yml`/descriptor.
3. Messages en **todos** los idiomas y `legacy`.
4. Entradas en help, whitelist de comandos, tab-complete.
5. Claves/canales Redis, placeholders, analytics.
6. Ymls propios (`queue.yml`), docs y README.
7. Grep de verificación; distinguir usos legítimos (`java.util.Queue`).
8. Anotar en `docs/ROADMAP.md` si el módulo vuelve en otro proyecto.

## 7. Validación al cargar
- Avisar (una vez) de claves desconocidas y de valores inválidos, con ruta completa (`archivo → SECCIÓN.CLAVE`).
- Nada de excepciones por config: valor inválido = default + aviso.
- Secretos (`TOKEN`, webhooks) nunca en logs.
