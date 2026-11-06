package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.bukkit.modules.staffmode.freeze.FreezeService;
import us.iorilabs.bukkit.punishment.PunishmentConfig;
import us.iorilabs.bukkit.punishment.PunishmentsModule;

public final class StaffMenuInternals {

    private StaffMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("staff.players", (viewer, ctx) -> {
            List<Map<String, String>> entries = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (viewer != null && online.getUniqueId().equals(viewer.getUniqueId())) continue;
                if (
                    viewer != null &&
                    !us.iorilabs.bukkit.modules.core.vanish.VanishVisibility.canSee(plugin, viewer, online)
                ) {
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                row.put("player", online.getName());
                row.put("target", online.getName());
                row.put("uuid", online.getUniqueId().toString());
                row.put("health", String.valueOf((int) Math.ceil(online.getHealth())));
                row.put("food", String.valueOf(online.getFoodLevel()));
                row.put("gamemode", online.getGameMode().name());
                row.put("world", online.getWorld() == null ? "" : online.getWorld().getName());
                entries.add(row);
            }
            return entries;
        });

        menus.internals().registerEnricher("staff-inspect", (player, menuId, ctx) -> {
            Player target = resolveTarget(ctx);
            if (target == null) return;
            ctx.put("target", target.getName());
            ctx.put("player", target.getName());
            ctx.put("uuid", target.getUniqueId().toString());
            ctx.put("health", String.valueOf((int) Math.ceil(target.getHealth())));
            ctx.put("food", String.valueOf(target.getFoodLevel()));
            ctx.put("gamemode", target.getGameMode().name());
            ctx.put("world", target.getWorld() == null ? "" : target.getWorld().getName());
            StaffModeService staff = staff(plugin);
            if (staff != null) {
                ctx.put("frozen", staff.frozen(target.getUniqueId()) ? "true" : "false");
                ctx.put("vanished", staff.vanished(target.getUniqueId()) ? "true" : "false");
            }
        });

        menus.internals().register("staff.freeze", (player, argument, ctx, service) -> {
            StaffModeService staff = staff(plugin);
            Player target = resolveTarget(ctx, argument);
            if (staff == null || target == null) return;
            if (!player.hasPermission(FreezeService.STAFF_PERM) && !player.hasPermission("ioricore.admin")) {
                player.sendMessage(plugin.language().rawFor(player, "GENERAL.NO-PERMISSION"));
                return;
            }
            staff.toggleFreeze(player, target);
            service.refresh(player);
        });

        menus.internals().register("staff.teleport", (player, argument, ctx, service) -> {
            Player target = resolveTarget(ctx, argument);
            if (target == null) return;
            if (
                !player.hasPermission("ioricore.staffmode.teleport") &&
                !player.hasPermission("ioricore.staffmode.inspect") &&
                !player.hasPermission("ioricore.admin")
            ) {
                player.sendMessage(plugin.language().rawFor(player, "GENERAL.NO-PERMISSION"));
                return;
            }
            player.closeInventory();
            player.teleport(target.getLocation());
            player.sendMessage(
                plugin.language().rawFor(player, "STAFF-MODE.RANDOM-TP", Map.of("player", target.getName()))
            );
        });

        menus.internals().register("staff.invsee-readonly", (player, argument, ctx, service) -> {
            StaffModeService staff = staff(plugin);
            Player target = resolveTarget(ctx, argument);
            if (staff == null || target == null) return;
            if (!player.hasPermission("ioricore.staffmode.inspect") && !player.hasPermission("ioricore.admin")) {
                player.sendMessage(plugin.language().rawFor(player, "GENERAL.NO-PERMISSION"));
                return;
            }
            player.closeInventory();
            staff.openInvsee(player, target);
        });

        menus.internals().register("staff.punish", (player, argument, ctx, service) -> {
            Player target = resolveTarget(ctx, null);
            if (target == null) return;
            String type = argument == null ? "" : argument.trim().toLowerCase(Locale.ROOT);
            player.closeInventory();
            if (runConfiguredPunish(plugin, player, target, type)) return;

            String key = switch (type) {
                case "ban" -> "STAFF-MODE.PUNISH-BAN";
                case "mute" -> "STAFF-MODE.PUNISH-MUTE";
                case "warn" -> "STAFF-MODE.PUNISH-WARN";
                default -> "STAFF-MODE.PUNISH-TIP";
            };
            Map<String, String> placeholders = Map.of("player", target.getName());
            for (String line : plugin.language().listFor(player, key, placeholders)) {
                player.sendMessage(line);
            }
        });
    }

    private static boolean runConfiguredPunish(IoriCorePlugin plugin, Player staff, Player target, String type) {
        String section = punishSection(type);
        if (section == null) return false;

        PunishmentConfig config = punishmentConfig(plugin);
        List<String> console = config == null ? List.of() : config.staffInspectConsole(section);
        List<String> playerCmds = config == null ? List.of() : config.staffInspectPlayer(section);
        if ((console == null || console.isEmpty()) && (playerCmds == null || playerCmds.isEmpty())) {
            return false;
        }

        Map<String, String> placeholders = Map.of(
            "player",
            target.getName(),
            "target",
            target.getName(),
            "staff",
            staff.getName()
        );

        if (console != null) {
            for (String raw : console) {
                String cmd = resolveCommand(raw, placeholders);
                if (cmd.isEmpty()) continue;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
        if (playerCmds != null) {
            for (String raw : playerCmds) {
                String cmd = resolveCommand(raw, placeholders);
                if (cmd.isEmpty()) continue;
                staff.performCommand(cmd);
            }
        }
        return true;
    }

    private static String punishSection(String type) {
        if (type == null || type.isEmpty()) return null;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "ban" -> "BAN";
            case "mute" -> "MUTE";
            case "warn" -> "WARN";
            default -> null;
        };
    }

    private static String resolveCommand(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String resolved = raw.trim();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved
                .replace("<" + entry.getKey() + ">", entry.getValue())
                .replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (resolved.startsWith("/")) resolved = resolved.substring(1);
        return resolved.trim();
    }

    private static Player resolveTarget(Map<String, String> ctx) {
        return resolveTarget(ctx, null);
    }

    private static Player resolveTarget(Map<String, String> ctx, String argument) {
        String name =
            argument == null || argument.trim().isEmpty()
                ? ctx == null
                    ? null
                    : ctx.getOrDefault("target", ctx.get("player"))
                : argument.trim();
        if (name == null || name.isEmpty()) return null;
        return Bukkit.getPlayerExact(name);
    }

    private static StaffModeService staff(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        return module == null ? null : module.service();
    }

    private static PunishmentConfig punishmentConfig(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        PunishmentsModule module = plugin.modules().get(ModuleId.PUNISHMENTS);
        return module == null ? null : module.config();
    }
}
