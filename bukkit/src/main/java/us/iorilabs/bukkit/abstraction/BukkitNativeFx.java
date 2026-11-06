package us.iorilabs.bukkit.abstraction;

import java.lang.reflect.Method;
import org.bukkit.entity.Player;

public final class BukkitNativeFx {

    private static final Method SEND_TITLE;
    private static final Method SET_TAB_HEADER_FOOTER;
    private static final Method PLAYER_SPIGOT;
    private static final Method SPIGOT_SEND_MESSAGE;
    private static final Object ACTION_BAR_TYPE;
    private static final Method FROM_LEGACY_TEXT;

    static {
        SEND_TITLE = resolveSendTitle();
        SET_TAB_HEADER_FOOTER = resolveSetTabHeaderFooter();
        PLAYER_SPIGOT = resolvePlayerSpigot();
        ACTION_BAR_TYPE = resolveActionBarType();
        FROM_LEGACY_TEXT = resolveFromLegacyText();
        SPIGOT_SEND_MESSAGE = resolveSpigotSendMessage();
    }

    private BukkitNativeFx() {}

    public static boolean sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || SEND_TITLE == null) return false;
        try {
            SEND_TITLE.invoke(
                player,
                title == null ? "" : title,
                subtitle == null ? "" : subtitle,
                fadeIn,
                stay,
                fadeOut
            );
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return false;
        if (
            PLAYER_SPIGOT == null || SPIGOT_SEND_MESSAGE == null || ACTION_BAR_TYPE == null || FROM_LEGACY_TEXT == null
        ) {
            return false;
        }
        try {
            Object spigot = PLAYER_SPIGOT.invoke(player);
            Object components = FROM_LEGACY_TEXT.invoke(null, message);
            SPIGOT_SEND_MESSAGE.invoke(spigot, ACTION_BAR_TYPE, components);
            return true;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return false;
        }
    }

    public static boolean sendTabHeaderFooter(Player player, String header, String footer) {
        if (player == null || SET_TAB_HEADER_FOOTER == null) return false;
        try {
            SET_TAB_HEADER_FOOTER.invoke(player, header == null ? "" : header, footer == null ? "" : footer);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method resolveSendTitle() {
        try {
            return Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method resolveSetTabHeaderFooter() {
        try {
            return Player.class.getMethod("setPlayerListHeaderFooter", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method resolvePlayerSpigot() {
        try {
            return Player.class.getMethod("spigot");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object resolveActionBarType() {
        try {
            Class type = Class.forName("net.md_5.bungee.api.ChatMessageType");
            return Enum.valueOf(type, "ACTION_BAR");
        } catch (ClassNotFoundException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Method resolveFromLegacyText() {
        try {
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            return textComponent.getMethod("fromLegacyText", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method resolveSpigotSendMessage() {
        if (PLAYER_SPIGOT == null || ACTION_BAR_TYPE == null) return null;
        try {
            Class<?> spigotClass = PLAYER_SPIGOT.getReturnType();
            Class<?> chatType = ACTION_BAR_TYPE.getClass();
            Class<?> baseComponentArray = Class.forName("[Lnet.md_5.bungee.api.chat.BaseComponent;");
            return spigotClass.getMethod("sendMessage", chatType, baseComponentArray);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            return null;
        }
    }
}
