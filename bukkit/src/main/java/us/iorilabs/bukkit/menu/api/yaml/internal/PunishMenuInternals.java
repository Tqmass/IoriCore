package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.punishment.PunishReasonService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;

public final class PunishMenuInternals {

    private PunishMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("punish.reasons", (player, ctx) -> {
            PunishReasonService reasons = reasons(plugin);
            if (reasons == null) return List.of();
            String type = ctx == null ? "" : ctx.getOrDefault("category", ctx.get("type"));
            List<Map<String, String>> rows = new ArrayList<>();
            for (PunishReasonService.PunishReason reason : reasons.byType(type)) {
                Map<String, String> row = new HashMap<>();
                row.put("id", reason.id());
                row.put("type", reason.type());
                row.put("material", reason.material());
                row.put("material-fallback", reason.materialFallback());
                row.put("name", reason.name());
                row.put("reason", reason.reason());
                row.put("duration", reason.duration() == null ? "" : reason.duration());
                int loreIndex = 1;
                for (String line : reason.lore()) {
                    row.put("lore" + loreIndex, line);
                    loreIndex++;
                }
                rows.add(row);
            }
            return rows;
        });

        menus.internals().registerEnricher("punish", (player, menuId, ctx) -> {
            String target = ctx == null ? null : ctx.getOrDefault("target", ctx.get("player"));
            if (target == null || target.isBlank()) return;
            ctx.put("target", target);
            ctx.put("player", target);
        });

        menus.internals().register("punish.category", (player, argument, ctx, service) -> {
            Map<String, String> next = new HashMap<>(ctx == null ? Map.of() : ctx);
            if (argument != null && !argument.isBlank()) next.put("category", argument.trim().toUpperCase(Locale.ROOT));
            service.open(player, menuReasons(plugin), next);
        });

        menus.internals().register("punish.apply", (player, argument, ctx, service) -> {
            PunishReasonService reasons = reasons(plugin);
            if (reasons == null) return;

            String id = argument == null ? "" : argument.trim();
            PunishReasonService.PunishReason reason = reasons.byId(id);
            if (reason == null) return;

            String targetName = ctx == null ? null : ctx.getOrDefault("target", ctx.get("player"));
            if (targetName == null || targetName.isBlank()) return;

            player.closeInventory();
            String cmd = reason
                .command()
                .replace("{player}", targetName)
                .replace("{target}", targetName)
                .replace("{reason}", reason.reason() == null ? "" : reason.reason())
                .replace("{duration}", reason.duration() == null ? "" : reason.duration())
                .trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (cmd.isEmpty()) return;

            if ("PLAYER".equals(reason.executor())) {
                Bukkit.dispatchCommand(player, cmd);
            } else {
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                Bukkit.dispatchCommand(console, cmd);
            }

            player.sendMessage(
                plugin
                    .language()
                    .rawFor(
                        player,
                        "PUNISH.APPLIED",
                        Map.of(
                            "player",
                            targetName,
                            "target",
                            targetName,
                            "reason",
                            reason.reason() == null ? "" : reason.reason(),
                            "type",
                            reason.type() == null ? "" : reason.type()
                        )
                    )
            );
        });
    }

    private static PunishReasonService reasons(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        PunishmentsModule module = plugin.modules().get(ModuleId.PUNISHMENTS);
        return module == null ? null : module.reasons();
    }

    private static String menuReasons(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return "punish-reasons";
        PunishmentsModule module = plugin.modules().get(ModuleId.PUNISHMENTS);
        if (module == null || module.config() == null) return "punish-reasons";
        return module.config().menuReasons();
    }
}
