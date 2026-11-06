package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.menu.anvil.AnvilInput;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.RankService;
import us.iorilabs.bukkit.ranks.RanksModule;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;

public final class RanksMenuInternals {

    private static final String FLAGS_PERMISSION = "ioricore.ranks.edit.flags";
    private static final String[] COLOR_KEYS = {
        "BLACK",
        "DARK_BLUE",
        "DARK_GREEN",
        "DARK_AQUA",
        "DARK_RED",
        "DARK_PURPLE",
        "GOLD",
        "GRAY",
        "DARK_GRAY",
        "BLUE",
        "GREEN",
        "AQUA",
        "RED",
        "LIGHT_PURPLE",
        "YELLOW",
        "WHITE",
    };

    private RanksMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("ranks.all", (player, ctx) -> {
            RankService ranks = ranks(plugin);
            if (ranks == null) return List.of();
            List<Map<String, String>> out = new ArrayList<>();
            for (Rank rank : ranks.all()) out.add(placeholders(plugin.language(), rank, player.getName()));
            return out;
        });

        menus.dynamics().register("ranks.parents", (player, ctx) -> {
            RankService ranks = ranks(plugin);
            Rank rank = resolveRank(plugin, ctx);
            if (ranks == null || rank == null) return List.of();
            List<Map<String, String>> out = new ArrayList<>();
            for (String parentName : rank.parents()) {
                Map<String, String> row = new HashMap<>();
                row.put("parent", parentName);
                row.put("name", parentName);
                Rank parent = ranks.resolve(parentName).orElse(null);
                if (parent != null) {
                    row.put("display", parent.displayName() == null ? parent.name() : parent.displayName());
                    row.put("priority", String.valueOf(parent.priority()));
                } else {
                    row.put("display", parentName);
                    row.put("priority", "?");
                }
                out.add(row);
            }
            return out;
        });

        menus.internals().registerEnricher("rank-", (player, menuId, ctx) -> {
            Rank rank = resolveRank(plugin, ctx);
            if (rank == null) return;
            ctx.putAll(placeholders(plugin.language(), rank, player.getName()));
        });

        menus.internals().register("ranks.open-edit", (player, argument, ctx, service) -> {
            Map<String, String> next = new HashMap<>(ctx);
            if (argument != null && !argument.trim().isEmpty()) next.put("id", argument.trim());
            service.open(player, "rank-edit", next);
        });

        menus.internals().register("ranks.open-parents", (player, argument, ctx, service) -> {
            Map<String, String> next = new HashMap<>(ctx);
            service.open(player, "rank-parents", next);
        });

        menus.internals().register("ranks.cycle-color", (player, argument, ctx, service) -> {
            RankService ranks = ranks(plugin);
            Rank rank = resolveRank(plugin, ctx);
            if (ranks == null || rank == null) return;
            if ("anvil".equalsIgnoreCase(argument)) {
                openAnvil(plugin, service, player, rank, "RANKS.MENU.ANVIL-COLOR", rank.colorKey(), input -> {
                    applyColor(plugin, ranks, player, rank, input, service);
                });
                return;
            }
            String current = rank.colorKey() == null ? "" : rank.colorKey().trim().toUpperCase(Locale.ROOT);
            int next = (indexOfColor(current) + 1) % COLOR_KEYS.length;
            rank.colorKey(COLOR_KEYS[next]);
            ranks.save(rank);
            player.sendMessage(
                plugin.language().raw("RANKS.MENU.SET-COLOR", placeholders(plugin.language(), rank, player.getName()))
            );
            service.refresh(player);
        });

        menus
            .internals()
            .register("ranks.toggle-bold", (player, argument, ctx, service) ->
                toggle(plugin, player, ctx, service, rank -> rank.bold(!rank.bold()), "TOGGLE-BOLD", false)
            );
        menus
            .internals()
            .register("ranks.toggle-italic", (player, argument, ctx, service) ->
                toggle(plugin, player, ctx, service, rank -> rank.italic(!rank.italic()), "TOGGLE-ITALIC", false)
            );
        menus
            .internals()
            .register("ranks.toggle-default", (player, argument, ctx, service) ->
                toggle(
                    plugin,
                    player,
                    ctx,
                    service,
                    rank -> rank.defaultRank(!rank.defaultRank()),
                    "TOGGLE-DEFAULT",
                    false
                )
            );
        menus
            .internals()
            .register("ranks.toggle-staff", (player, argument, ctx, service) ->
                toggle(plugin, player, ctx, service, rank -> rank.staff(!rank.staff()), "TOGGLE-STAFF", false)
            );
        menus
            .internals()
            .register("ranks.toggle-admin", (player, argument, ctx, service) ->
                toggle(plugin, player, ctx, service, rank -> rank.admin(!rank.admin()), "TOGGLE-ADMIN", true)
            );
        menus
            .internals()
            .register("ranks.toggle-visible", (player, argument, ctx, service) ->
                toggle(plugin, player, ctx, service, rank -> rank.visible(!rank.visible()), "TOGGLE-VISIBLE", false)
            );

        menus.internals().register("ranks.set-name", (player, argument, ctx, service) ->
            anvilField(
                plugin,
                service,
                player,
                ctx,
                "RANKS.MENU.ANVIL-NAME",
                rank -> rank.name(),
                (rank, input) -> {
                    String value = input == null ? "" : input.trim();
                    RankService ranks = ranks(plugin);
                    if (value.isEmpty() || value.length() > 32 || !value.matches("[A-Za-z0-9_\\-]+")) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-NAME"));
                        return false;
                    }
                    Optional<Rank> taken = ranks.resolve(value);
                    if (taken.isPresent() && !taken.get().id().equals(rank.id())) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.NAME-TAKEN"));
                        return false;
                    }
                    rank.name(value);
                    return true;
                },
                "SET-NAME"
            )
        );
        menus.internals().register("ranks.set-display", (player, argument, ctx, service) ->
            anvilField(
                plugin,
                service,
                player,
                ctx,
                "RANKS.MENU.ANVIL-DISPLAY",
                rank -> rank.displayName() == null ? rank.name() : rank.displayName(),
                (rank, input) -> {
                    String value = input == null ? "" : input.trim();
                    if (value.isEmpty() || value.length() > 64) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-DISPLAY"));
                        return false;
                    }
                    rank.displayName(value);
                    return true;
                },
                "SET-DISPLAY"
            )
        );
        menus.internals().register("ranks.set-weight", (player, argument, ctx, service) ->
            anvilField(
                plugin,
                service,
                player,
                ctx,
                "RANKS.MENU.ANVIL-WEIGHT",
                rank -> String.valueOf(rank.priority()),
                (rank, input) -> {
                    try {
                        int weight = Integer.parseInt(input == null ? "" : input.trim());
                        if (weight < 0 || weight > 100000) throw new NumberFormatException();
                        rank.priority(weight);
                        return true;
                    } catch (NumberFormatException ex) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-WEIGHT"));
                        return false;
                    }
                },
                "SET-WEIGHT"
            )
        );
        menus.internals().register("ranks.set-prefix", (player, argument, ctx, service) ->
            anvilField(
                plugin,
                service,
                player,
                ctx,
                "RANKS.MENU.ANVIL-PREFIX",
                rank -> rank.prefix() == null ? "" : rank.prefix(),
                (rank, input) -> {
                    String value = input == null ? "" : input;
                    if (value.length() > 64) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-PREFIX"));
                        return false;
                    }
                    rank.prefix(value);
                    return true;
                },
                "SET-PREFIX"
            )
        );
        menus.internals().register("ranks.set-suffix", (player, argument, ctx, service) ->
            anvilField(
                plugin,
                service,
                player,
                ctx,
                "RANKS.MENU.ANVIL-SUFFIX",
                rank -> rank.suffix() == null ? "" : rank.suffix(),
                (rank, input) -> {
                    String value = input == null ? "" : input;
                    if (value.length() > 64) {
                        player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-SUFFIX"));
                        return false;
                    }
                    rank.suffix(value);
                    return true;
                },
                "SET-SUFFIX"
            )
        );

        menus.internals().register("ranks.parent-add", (player, argument, ctx, service) -> {
            RankService ranks = ranks(plugin);
            Rank rank = resolveRank(plugin, ctx);
            if (ranks == null || rank == null) return;
            openParentsAnvil(plugin, service, player, rank, "RANKS.MENU.ANVIL-PARENT", "", input -> {
                String parentKey = input == null ? "" : input.trim();
                RankEditResult result = ranks.addParent(rank.key(), parentKey);
                Map<String, String> placeholders = placeholders(plugin.language(), rank, player.getName());
                placeholders.put("parent", parentKey);
                placeholders.put("rank", rank.key());
                if (result == RankEditResult.OK) {
                    player.sendMessage(plugin.language().raw("RANKS.PARENT-ADDED", placeholders));
                } else {
                    player.sendMessage(plugin.language().raw(editResultKey(result), placeholders));
                }
                service.open(player, "rank-parents", placeholders(plugin.language(), rank, player.getName()), false);
            });
        });

        menus.internals().register("ranks.parent-remove", (player, argument, ctx, service) -> {
            RankService ranks = ranks(plugin);
            Rank rank = resolveRank(plugin, ctx);
            if (ranks == null || rank == null) return;
            String parentKey =
                argument == null || argument.trim().isEmpty()
                    ? ctx == null
                        ? ""
                        : ctx.getOrDefault("parent", "")
                    : argument.trim();
            if (parentKey == null || parentKey.isEmpty()) return;
            RankEditResult result = ranks.removeParent(rank.key(), parentKey);
            Map<String, String> placeholders = placeholders(plugin.language(), rank, player.getName());
            placeholders.put("parent", parentKey);
            placeholders.put("rank", rank.key());
            if (result == RankEditResult.OK) {
                player.sendMessage(plugin.language().raw("RANKS.PARENT-REMOVED", placeholders));
            } else {
                player.sendMessage(plugin.language().raw(editResultKey(result), placeholders));
            }
            service.refresh(player);
        });
    }

    private static String editResultKey(RankEditResult result) {
        if (result == null) return "RANKS.FAILED";
        switch (result) {
            case UNKNOWN_RANK:
                return "RANKS.UNKNOWN";
            case UNKNOWN_PARENT:
                return "RANKS.UNKNOWN-PARENT";
            case INVALID_NAME:
                return "RANKS.INVALID-NAME";
            case INVALID_VALUE:
                return "RANKS.INVALID-VALUE";
            case ALREADY_EXISTS:
                return "RANKS.ALREADY-EXISTS";
            case NOT_PRESENT:
                return "RANKS.NOT-PRESENT";
            case CYCLE:
                return "RANKS.CYCLE";
            case DEFAULT_RANK:
                return "RANKS.DEFAULT-PROTECTED";
            case OK:
                return "RANKS.OK";
            default:
                return "RANKS.FAILED";
        }
    }

    private static void openParentsAnvil(
        IoriCorePlugin plugin,
        YamlMenuService service,
        org.bukkit.entity.Player player,
        Rank rank,
        String titleKey,
        String initial,
        Consumer<String> onComplete
    ) {
        player.closeInventory();
        Map<String, String> reopen = placeholders(plugin.language(), rank, player.getName());
        boolean opened = AnvilInput.open(
            plugin,
            player,
            plugin.language().plain(titleKey),
            initial == null ? "" : initial,
            onComplete,
            () -> service.open(player, "rank-parents", reopen, false)
        );
        if (!opened) {
            player.sendMessage(plugin.language().raw("RANKS.MENU.ANVIL-UNSUPPORTED"));
            service.open(player, "rank-parents", reopen, false);
        }
    }

    private static void toggle(
        IoriCorePlugin plugin,
        org.bukkit.entity.Player player,
        Map<String, String> ctx,
        YamlMenuService service,
        Consumer<Rank> mutate,
        String messageKey,
        boolean flags
    ) {
        RankService ranks = ranks(plugin);
        Rank rank = resolveRank(plugin, ctx);
        if (ranks == null || rank == null) return;
        if (flags && !player.hasPermission(FLAGS_PERMISSION) && !player.hasPermission("ioricore.admin")) {
            player.sendMessage(
                plugin
                    .language()
                    .raw("RANKS.MENU.FLAGS-NO-PERMISSION", placeholders(plugin.language(), rank, player.getName()))
            );
            return;
        }
        mutate.accept(rank);
        ranks.save(rank);
        player.sendMessage(
            plugin.language().raw("RANKS.MENU." + messageKey, placeholders(plugin.language(), rank, player.getName()))
        );
        service.refresh(player);
    }

    private interface FieldApplier {
        boolean apply(Rank rank, String input);
    }

    private interface FieldReader {
        String read(Rank rank);
    }

    private static void anvilField(
        IoriCorePlugin plugin,
        YamlMenuService service,
        org.bukkit.entity.Player player,
        Map<String, String> ctx,
        String titleKey,
        FieldReader reader,
        FieldApplier applier,
        String messageKey
    ) {
        RankService ranks = ranks(plugin);
        Rank rank = resolveRank(plugin, ctx);
        if (ranks == null || rank == null) return;
        openAnvil(plugin, service, player, rank, titleKey, reader.read(rank), input -> {
            if (!applier.apply(rank, input)) {
                service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false);
                return;
            }
            ranks.save(rank);
            player.sendMessage(
                plugin
                    .language()
                    .raw("RANKS.MENU." + messageKey, placeholders(plugin.language(), rank, player.getName()))
            );
            service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false);
        });
    }

    private static void applyColor(
        IoriCorePlugin plugin,
        RankService ranks,
        org.bukkit.entity.Player player,
        Rank rank,
        String input,
        YamlMenuService service
    ) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || value.length() > 32 || !value.matches("[A-Za-z0-9_\\-#]+")) {
            player.sendMessage(plugin.language().raw("RANKS.MENU.INVALID-COLOR"));
            service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false);
            return;
        }
        rank.colorKey(value.toUpperCase(Locale.ROOT));
        ranks.save(rank);
        player.sendMessage(
            plugin.language().raw("RANKS.MENU.SET-COLOR", placeholders(plugin.language(), rank, player.getName()))
        );
        service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false);
    }

    private static void openAnvil(
        IoriCorePlugin plugin,
        YamlMenuService service,
        org.bukkit.entity.Player player,
        Rank rank,
        String titleKey,
        String initial,
        Consumer<String> onComplete
    ) {
        player.closeInventory();
        boolean opened = AnvilInput.open(
            plugin,
            player,
            plugin.language().plain(titleKey),
            initial == null ? "" : initial,
            onComplete,
            () -> service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false)
        );
        if (!opened) {
            player.sendMessage(plugin.language().raw("RANKS.MENU.ANVIL-UNSUPPORTED"));
            service.open(player, "rank-edit", placeholders(plugin.language(), rank, player.getName()), false);
        }
    }

    private static Rank resolveRank(IoriCorePlugin plugin, Map<String, String> ctx) {
        RankService ranks = ranks(plugin);
        if (ranks == null || ctx == null) return null;
        String id = ctx.get("id");
        if (id == null || id.isEmpty()) id = ctx.get("rank");
        if (id == null || id.isEmpty()) return null;
        try {
            return ranks.byId(java.util.UUID.fromString(id)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return ranks.resolve(id).orElse(null);
        }
    }

    private static RankService ranks(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        RanksModule module = plugin.modules().get(ModuleId.RANKS);
        return module == null ? null : module.ranks();
    }

    static Map<String, String> placeholders(CoreLanguage language, Rank rank, String sampleName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(rank.id()));
        placeholders.put("name", nullToEmpty(rank.name()));
        placeholders.put("display", nullToEmpty(rank.displayName()));
        placeholders.put("prefix", nullToEmpty(rank.prefix()));
        placeholders.put("suffix", nullToEmpty(rank.suffix()));
        placeholders.put("player", sampleName);
        placeholders.put("priority", String.valueOf(rank.priority()));
        placeholders.put("type", rank.type() == null ? "Normal" : rank.type().display());
        placeholders.put("scope", rank.scope() == null ? "SERVER" : rank.scope().name());
        placeholders.put("color", nullToEmpty(rank.colorKey()));
        placeholders.put("default", language.bool(rank.defaultRank()));
        placeholders.put("italic", language.bool(rank.italic()));
        placeholders.put("bold", language.bool(rank.bold()));
        placeholders.put("staff", language.bool(rank.staff()));
        placeholders.put("media", language.bool(rank.media()));
        placeholders.put("admin", language.bool(rank.admin()));
        placeholders.put("op", language.bool(rank.op()));
        placeholders.put("visible", language.bool(rank.visible()));
        placeholders.put("permissions", String.valueOf(rank.permissionCount()));
        placeholders.put("parents-count", String.valueOf(rank.parents().size()));
        return placeholders;
    }

    private static int indexOfColor(String current) {
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            if (COLOR_KEYS[i].equalsIgnoreCase(current)) return i;
        }
        return -1;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
