package us.iorilabs.bukkit.modules.chatformat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.api.hook.LuckPermsHook;
import us.iorilabs.api.hook.VaultHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.chatlogs.ChatLogService;
import us.iorilabs.bukkit.modules.chatlogs.ChatLogsModule;
import us.iorilabs.bukkit.modules.gender.GenderModule;
import us.iorilabs.bukkit.modules.gender.GenderService;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.TagsModule;
import us.iorilabs.bukkit.ranks.RankDisplay;
import us.iorilabs.bukkit.ranks.RankService;
import us.iorilabs.bukkit.ranks.RanksModule;
import us.iorilabs.bukkit.util.ProtocolProbe;

public final class ChatFormatService {

    private static final String[] DEFAULT_PART_ORDER = { "tag", "rank", "player", "separator", "message" };

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final VaultHook vault;
    private final LuckPermsHook luckPerms;
    private final int fallbackProtocol;

    public ChatFormatService(IoriCorePlugin plugin, CoreLanguage language, VaultHook vault, LuckPermsHook luckPerms) {
        this.plugin = plugin;
        this.language = language;
        this.vault = vault;
        this.luckPerms = luckPerms;
        this.fallbackProtocol = plugin.fallbackProtocol();
    }

    public void broadcast(Player sender, String message, Set<Player> recipients) {
        if (sender == null || message == null || recipients == null || recipients.isEmpty()) return;

        String playerText = ChatText.sanitize(sender, message);
        for (Player receiver : recipients) {
            if (receiver == null) continue;
            int protocol = protocolOf(receiver);
            BaseComponent[] components = formatComponents(sender, playerText, protocol);
            if (components.length == 0) continue;
            try {
                receiver.spigot().sendMessage(components);
            } catch (NoSuchMethodError | UnsupportedOperationException ex) {
                receiver.sendMessage(BaseComponent.toLegacyText(components));
            }
        }

        Bukkit.getConsoleSender().sendMessage(format(sender, playerText, Integer.MAX_VALUE));
        logChat(sender, message);
    }

    public String format(Player sender, String playerText, int protocol) {
        return BaseComponent.toLegacyText(formatComponents(sender, playerText, protocol));
    }

    private BaseComponent[] formatComponents(Player sender, String playerText, int protocol) {
        String fallback = colored("&7" + sender.getName() + "&8 » &f") + playerText;
        FileConfiguration formats = language.formatsForProtocol(protocol);
        ConfigurationSection root = formats.getConfigurationSection("FORMATS");
        if (root == null) return TextComponent.fromLegacyText(fallback);

        ConfigurationSection format = pickFormat(root, sender);
        if (format == null) return TextComponent.fromLegacyText(fallback);

        ConfigurationSection parts = format.getConfigurationSection("PARTS");
        if (parts == null) return TextComponent.fromLegacyText(fallback);
        List<String> partOrder = partOrderFromLayout(formats, parts);

        String prefix = resolvePrefix(sender);
        String suffix = resolveSuffix(sender);
        String tag = resolveTag(sender);
        Tag equippedTag = resolveEquippedTag(sender);
        String gender = resolveGender(sender);
        List<BaseComponent> out = new ArrayList<>();

        for (String partKey : orderedPartKeys(parts, partOrder)) {
            ConfigurationSection part = parts.getConfigurationSection(partKey);
            if (part == null) continue;
            String text = part.getString("TEXT", "");
            if (text == null) text = "";
            if ("tag".equalsIgnoreCase(partKey)) text = applyTagPart(text, tag);
            if ("message".equalsIgnoreCase(partKey)) {
                text = text.replace("{MESSAGE}", ChatText.SLOT).replace("%message%", ChatText.SLOT);
            }
            text = applyTokens(sender, prefix, suffix, tag, gender, text);
            text = ChatText.inject(text, playerText);
            if (text.isEmpty()) continue;

            String coloredText = colored(text);
            BaseComponent[] components = TextComponent.fromLegacyText(coloredText);
            HoverEvent hover = hoverOf(partKey, part, equippedTag, sender, prefix, suffix, tag, gender);
            ClickEvent click = clickOf(part, sender, prefix, suffix, tag, gender);
            for (BaseComponent component : components) {
                if (hover != null) component.setHoverEvent(hover);
                if (click != null) component.setClickEvent(click);
                out.add(component);
            }
        }

        if (out.isEmpty()) return TextComponent.fromLegacyText(fallback);
        return out.toArray(new BaseComponent[0]);
    }

    private HoverEvent hoverOf(
        String partKey,
        ConfigurationSection part,
        Tag equippedTag,
        Player sender,
        String prefix,
        String suffix,
        String tag,
        String gender
    ) {
        List<String> lines = null;
        if ("tag".equalsIgnoreCase(partKey) && equippedTag != null && !equippedTag.hoverLines().isEmpty()) {
            lines = equippedTag.hoverLines();
        }
        if (lines == null || lines.isEmpty()) lines = part.getStringList("HOVER");
        if (lines == null || lines.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) builder.append('\n');
            String line = applyTokens(sender, prefix, suffix, tag, gender, lines.get(i) == null ? "" : lines.get(i));
            builder.append(colored(line));
        }
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(builder.toString()));
    }

    private ClickEvent clickOf(
        ConfigurationSection part,
        Player sender,
        String prefix,
        String suffix,
        String tag,
        String gender
    ) {
        ConfigurationSection click = part.getConfigurationSection("CLICK");
        if (click == null) return null;
        String type = click.getString("TYPE", "");
        String value = click.getString("VALUE", "");
        if (type == null || type.isEmpty() || value == null || value.isEmpty()) return null;
        value = applyTokens(sender, prefix, suffix, tag, gender, value);
        value = ChatText.inject(value, "");
        ClickEvent.Action action = clickAction(type);
        if (action == null) return null;
        if (action == ClickEvent.Action.OPEN_URL && !(value.startsWith("http://") || value.startsWith("https://"))) {
            return null;
        }
        return new ClickEvent(action, value);
    }

    private static ClickEvent.Action clickAction(String type) {
        String raw = type.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ClickEvent.Action.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void logChat(Player sender, String message) {
        if (plugin.modules() == null) return;
        CoreModule module = plugin.modules().get(ModuleId.CHAT_LOGS);
        if (!(module instanceof ChatLogsModule) || !module.enabled()) return;
        ChatLogService logs = ((ChatLogsModule) module).service();
        if (logs != null) logs.logChat(sender, ChatColor.stripColor(message));
    }

    private ConfigurationSection pickFormat(ConfigurationSection root, Player sender) {
        List<String> keys = new ArrayList<>(root.getKeys(false));
        ConfigurationSection fallback = root.getConfigurationSection("default");
        for (String key : keys) {
            if ("default".equalsIgnoreCase(key)) continue;
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            String permission = section.getString("PERMISSION", "");
            if (permission != null && !permission.isEmpty() && sender.hasPermission(permission)) {
                return section;
            }
        }
        return fallback;
    }

    private List<String> orderedPartKeys(ConfigurationSection parts, List<String> layoutOrder) {
        List<String> ordered = new ArrayList<>();
        if (layoutOrder != null) {
            for (String key : layoutOrder) {
                if (parts.isConfigurationSection(key)) ordered.add(key);
            }
        }
        for (String key : DEFAULT_PART_ORDER) {
            if (parts.isConfigurationSection(key) && !ordered.contains(key)) ordered.add(key);
        }
        for (String key : parts.getKeys(false)) {
            if (!ordered.contains(key)) ordered.add(key);
        }
        return ordered;
    }

    private static final Pattern LAYOUT_TOKEN = Pattern.compile("\\{([^}]+)}");

    private static List<String> partOrderFromLayout(FileConfiguration formats, ConfigurationSection parts) {
        if (formats == null || parts == null) return null;
        String layout = formats.getString("LAYOUT", "");
        if (layout == null || layout.isEmpty()) return null;
        Matcher matcher = LAYOUT_TOKEN.matcher(layout);
        List<String> keys = new ArrayList<>();
        while (matcher.find()) {
            String k = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (parts.isConfigurationSection(k) && !keys.contains(k)) keys.add(k);
        }
        return keys.isEmpty() ? null : keys;
    }

    private String applyTokens(Player sender, String prefix, String suffix, String tag, String gender, String text) {
        String safeTag = tag == null ? "" : tag;
        String safeGender = gender == null ? "" : gender;
        String out = text
            .replace("{PLAYER}", sender.getName())
            .replace("{MESSAGE}", ChatText.SLOT)
            .replace("{TAG}", safeTag)
            .replace("{GENDER}", safeGender)
            .replace("{gender}", safeGender)
            .replace("%tag%", safeTag)
            .replace("%TAG%", safeTag)
            .replace("%gender%", safeGender)
            .replace("%GENDER%", safeGender)
            .replace("%vault_prefix%", prefix == null ? "" : prefix)
            .replace("%vault_suffix%", suffix == null ? "" : suffix)
            .replace("%rank_prefix%", prefix == null ? "" : prefix)
            .replace("%rank_suffix%", suffix == null ? "" : suffix)
            .replace("%rank_display%", rankDisplay(sender))
            .replace("%player_name%", sender.getName())
            .replace("%player_ping%", String.valueOf(safePing(sender)));
        return applyPapi(sender, out);
    }

    private String applyTagPart(String text, String tag) {
        String safeTag = tag == null ? "" : tag;
        if (text.contains("{TAG}") || text.contains("%tag%") || text.contains("%TAG%")) {
            return text.replace("{TAG}", safeTag).replace("%tag%", safeTag).replace("%TAG%", safeTag);
        }
        if (!safeTag.isEmpty()) return safeTag;
        return text;
    }

    private String resolveTag(Player sender) {
        Tag tag = resolveEquippedTag(sender);
        return tag == null ? "" : tag.fragment();
    }

    private Tag resolveEquippedTag(Player sender) {
        if (sender == null || plugin.modules() == null) return null;
        CoreModule module = plugin.modules().get(ModuleId.TAGS);
        if (!(module instanceof TagsModule) || !module.enabled()) return null;
        TagsModule tags = (TagsModule) module;
        if (tags.service() == null) return null;
        return tags.service().equippedTag(sender.getUniqueId());
    }

    private String resolveGender(Player sender) {
        if (sender == null || plugin.modules() == null) return "";
        CoreModule module = plugin.modules().get(ModuleId.GENDER);
        if (!(module instanceof GenderModule) || !module.enabled()) return "";
        GenderService gender = ((GenderModule) module).service();
        return gender == null ? "" : gender.display(sender);
    }

    private String resolvePrefix(Player sender) {
        RankDisplay display = rankOf(sender);
        if (display != null && display.prefix() != null && !display.prefix().isEmpty()) return display.prefix();
        if (vault != null && vault.available()) {
            String value = vault.prefix(sender.getUniqueId());
            if (value != null && !value.isEmpty()) return value;
        }
        if (luckPerms != null && luckPerms.available()) {
            String value = luckPerms.prefix(sender.getUniqueId());
            if (value != null && !value.isEmpty()) return value;
        }
        return "";
    }

    private String resolveSuffix(Player sender) {
        RankDisplay display = rankOf(sender);
        if (display != null && display.suffix() != null && !display.suffix().isEmpty()) return display.suffix();
        if (vault != null && vault.available()) {
            String value = vault.suffix(sender.getUniqueId());
            if (value != null && !value.isEmpty()) return value;
        }
        if (luckPerms != null && luckPerms.available()) {
            String value = luckPerms.suffix(sender.getUniqueId());
            if (value != null && !value.isEmpty()) return value;
        }
        return "";
    }

    private String rankDisplay(Player sender) {
        RankDisplay display = rankOf(sender);
        return display == null || display.display() == null ? "" : display.display();
    }

    private RankDisplay rankOf(Player sender) {
        if (sender == null || plugin.modules() == null) return null;
        CoreModule module = plugin.modules().get(ModuleId.RANKS);
        if (!(module instanceof RanksModule) || !module.enabled()) return null;
        RankService ranks = ((RanksModule) module).ranks();
        return ranks == null ? null : ranks.display(sender);
    }

    private String applyPapi(Player player, String text) {
        if (text == null || !text.contains("%")) return text;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, player, text);
            return result == null ? text : String.valueOf(result);
        } catch (ReflectiveOperationException ex) {
            return text;
        }
    }

    private int protocolOf(Player player) {
        return ProtocolProbe.protocol(player, fallbackProtocol);
    }

    private static int safePing(Player player) {
        try {
            Object ping = player.getClass().getMethod("getPing").invoke(player);
            if (ping instanceof Integer) return (Integer) ping;
        } catch (ReflectiveOperationException ignored) {}
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object ping = handle.getClass().getField("ping").get(handle);
            if (ping instanceof Integer) return (Integer) ping;
        } catch (ReflectiveOperationException ignored) {}
        return 0;
    }

    private static String colored(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
