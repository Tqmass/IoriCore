# STYLE_GUIDE

## Comentarios
- **Cero** Javadoc narrativo.
- Decisiones → `DECISIONS.md`.

## Lombok
- Holders: `@Getter` `@Setter` `@Accessors(fluent = true)` (o `@Data` si el estilo Darling del feature lo pide).
- Services con deps final: `@RequiredArgsConstructor`.
- **Prohibido** getters manuales en holders.

## PlayerData
- `PlayerData` + `PlayerDataService` + `PlayerDataSql` (ver rule `15-playerdata-darling`).

## Formato
1. Prettier Java/YAML.
2. Blank entre bloques lógicos / métodos.
3. Nunca blank entre cada línea.
4. Guards cortos en 1 línea.

## Orden de clase
1. static constants
2. fields
3. ctor
4. API pública
5. privados / SQL helpers

## Utils
Helpers reutilizables → `utilities/` / `common`, no métodos privados sueltos duplicados.

## Refs
Darling Profile / CommandParent: `_refs/README.md`
