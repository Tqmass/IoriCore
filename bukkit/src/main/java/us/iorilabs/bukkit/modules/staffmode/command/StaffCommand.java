package us.iorilabs.bukkit.modules.staffmode.command;

import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.staffauth.StaffAuthEnforceModule;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffmode.StaffChatService;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class StaffCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final StaffModeService staffMode;
    private final StaffChatService staffChat;

    public StaffCommand(
        IoriCorePlugin plugin,
        CoreLanguage language,
        StaffModeService staffMode,
        StaffChatService staffChat
    ) {
        super("staff", java.util.Collections.emptyList(), "ioricore.staff");
        this.plugin = plugin;
        this.language = language;
        this.staffMode = staffMode;
        this.staffChat = staffChat;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.staff") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }

        if (args.length == 0) {
            runDefault(source);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            sendUsage(source);
            return;
        }
        if (sub.equals("register")) {
            runRegister(source, args);
            return;
        }
        if (sub.equals("login")) {
            runLogin(source, args);
            return;
        }
        if (sub.equals("oauth") || sub.equals("2fa")) {
            runOauth(source);
            return;
        }
        if (sub.equals("logout")) {
            runLogout(source);
            return;
        }
        if (sub.equals("menu")) {
            openMenu(source);
            return;
        }
        if (sub.equals("chat") || sub.equals("sc")) {
            toggleChat(source);
            return;
        }
        if (sub.equals("mode") || sub.equals("toggle")) {
            toggleMode(source);
            return;
        }
        if (sub.equals("freeze")) {
            freeze(source, args.length > 1 ? args[1] : null);
            return;
        }

        sendUsage(source);
    }

    private void runRegister(CommandSource source, String[] args) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        StaffRegService reg = staffReg();
        if (reg == null) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.MODULE-DISABLED"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.REGISTER-USAGE"));
            return;
        }
        reg.register(player, args[1]);
    }

    private void runLogin(CommandSource source, String[] args) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        StaffRegService reg = staffReg();
        if (reg == null) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.MODULE-DISABLED"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.LOGIN-USAGE"));
            return;
        }
        reg.login(player, args[1]);
    }

    private void runOauth(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        StaffRegService reg = staffReg();
        if (reg == null) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.MODULE-DISABLED"));
            return;
        }
        reg.oauth(player);
    }

    private void runLogout(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        StaffRegService reg = staffReg();
        if (reg == null) {
            source.sendMessage(language.raw(source, "STAFF-AUTH.MODULE-DISABLED"));
            return;
        }
        reg.logout(player);
    }

    private StaffRegService staffReg() {
        if (plugin.modules() == null) return null;
        StaffAuthEnforceModule module = plugin.modules().get(ModuleId.STAFF_AUTH);
        return module == null ? null : module.staffReg();
    }

    private void runDefault(CommandSource source) {
        String action = staffChat.defaultStaffAction();
        if ("MENU".equals(action)) {
            openMenu(source);
            return;
        }
        toggleChat(source);
    }

    private void toggleChat(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        staffChat.toggle(player);
    }

    private void toggleMode(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!source.hasPermission("ioricore.staffmode") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        staffMode.toggle(player);
    }

    private void openMenu(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        staffMode.openStaffHub(player);
    }

    private void freeze(CommandSource source, String targetName) {
        Player staff = EssentialsPlayers.player(source);
        if (staff == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!source.hasPermission("ioricore.staffmode.freeze") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (targetName == null || targetName.trim().isEmpty()) {
            sendUsage(source);
            return;
        }
        Player target = EssentialsPlayers.findOnline(source, targetName);
        if (target == null) {
            source.sendMessage(language.raw("STAFF-MODE.PLAYER-NOT-FOUND", Map.of("player", targetName)));
            return;
        }
        staffMode.toggleFreeze(staff, target);
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : language.list("STAFF-COMMAND.USAGE")) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
