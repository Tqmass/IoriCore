package us.iorilabs.proxy.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.List;
import java.util.Locale;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.ProxyCommand;

public final class VelocityBrigadierCommand {

    private VelocityBrigadierCommand() {}

    public static BrigadierCommand create(IoriProxy proxy, ProxyCommand command, String label) {
        String literal = label.toLowerCase(Locale.ROOT);
        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.<CommandSource>literal(literal)
            .requires(source -> allows(command, source))
            .executes(ctx -> {
                run(proxy, command, ctx.getSource(), literal, new String[0]);
                return Command.SINGLE_SUCCESS;
            });

        RequiredArgumentBuilder<CommandSource, String> args = RequiredArgumentBuilder.<CommandSource, String>argument(
            "args",
            StringArgumentType.greedyString()
        )
            .suggests((ctx, builder) -> {
                String remaining = builder.getRemainingLowerCase();
                String[] parts = remaining.isBlank() ? new String[] { "" } : remaining.split(" ", -1);
                String last = parts[parts.length - 1];
                VelocityCommandSource source = new VelocityCommandSource(ctx.getSource());
                List<String> suggestions = command.suggest(source, literal, parts);
                for (String suggestion : suggestions) {
                    String lower = suggestion.toLowerCase(Locale.ROOT);
                    if (last.isBlank() || lower.startsWith(last)) {
                        builder.suggest(suggestion);
                    }
                }

                return builder.buildFuture();
            })
            .executes(ctx -> {
                String raw = StringArgumentType.getString(ctx, "args");
                String[] split = raw.isBlank() ? new String[0] : raw.split(" ");
                run(proxy, command, ctx.getSource(), literal, split);
                return Command.SINGLE_SUCCESS;
            });

        LiteralCommandNode<CommandSource> node = root.then(args).build();
        return new BrigadierCommand(node);
    }

    private static void run(IoriProxy proxy, ProxyCommand command, CommandSource source, String label, String[] args) {
        VelocityCommandSource wrapped = new VelocityCommandSource(source);
        if (!allows(command, source)) {
            wrapped.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        command.execute(wrapped, label, args == null ? new String[0] : args);
    }

    private static boolean allows(ProxyCommand command, CommandSource source) {
        if (isConsole(source)) return true;

        String permission = command.permission();
        if (permission == null || permission.isBlank()) return true;

        return source.hasPermission(permission);
    }

    private static boolean isConsole(CommandSource source) {
        return !(source instanceof com.velocitypowered.api.proxy.Player);
    }
}
