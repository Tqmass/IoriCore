package us.iorilabs.bukkit.modules.chatformat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import us.iorilabs.bukkit.util.ProtocolProbe;

@UtilityClass
public class ChatText {

    public static final String COLOR_PERMISSION = "ioricore.chat.color";
    public static final String SLOT = "\u0001MSG\u0001";

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final int HEX_PROTOCOL = 735;

    public static String sanitize(CommandSender sender, String message) {
        if (message == null) return "";
        String stripped = ChatColor.stripColor(message).replace(SLOT, "");
        if (sender == null || !sender.hasPermission(COLOR_PERMISSION)) return stripped;
        return colorize(stripped);
    }

    public static String inject(String rendered, String playerText) {
        if (rendered == null) return "";
        return rendered.replace(SLOT, playerText == null ? "" : playerText);
    }

    private static String colorize(String input) {
        String out = input;
        if (ProtocolProbe.serverFallbackProtocol() >= HEX_PROTOCOL) {
            Matcher matcher = HEX.matcher(out);
            StringBuilder builder = new StringBuilder();
            while (matcher.find()) {
                StringBuilder hex = new StringBuilder("§x");
                for (char c : matcher.group(1).toCharArray()) hex.append('§').append(c);
                matcher.appendReplacement(builder, Matcher.quoteReplacement(hex.toString()));
            }
            matcher.appendTail(builder);
            out = builder.toString();
        }
        return ChatColor.translateAlternateColorCodes('&', out);
    }
}
