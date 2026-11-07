package us.iorilabs.bukkit.hook;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import us.iorilabs.api.hook.ApolloHook;

public final class SoftApolloHook implements ApolloHook {

    private final boolean present;
    private Object glowModule;
    private Object nametagModule;
    private Object recipientsEveryone;
    private Object modSettingModule;
    private Object numbersOption;
    private Object legacySerializer;

    public SoftApolloHook(Logger logger, boolean moduleEnabled) {
        if (!moduleEnabled) {
            this.present = false;
            return;
        }

        this.present =
            Bukkit.getPluginManager().getPlugin("Apollo-Bukkit") != null ||
            Bukkit.getPluginManager().getPlugin("Apollo") != null;
        if (present) {
            logger.fine("Apollo detected (soft-hook)");
            tryLoadGlow();
            tryLoadNametag();
            tryLoadNumbers();
        }
    }

    private void tryLoadGlow() {
        try {
            Class<?> apolloClass = Class.forName("com.lunarclient.apollo.Apollo");
            Object moduleManager = apolloClass.getMethod("getModuleManager").invoke(null);
            Class<?> glowClass = Class.forName("com.lunarclient.apollo.module.glow.GlowModule");
            Method getModule = moduleManager.getClass().getMethod("getModule", Class.class);
            glowModule = getModule.invoke(moduleManager, glowClass);

            Class<?> recipientsClass = Class.forName("com.lunarclient.apollo.recipients.Recipients");
            recipientsEveryone = recipientsClass.getMethod("ofEveryone").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            glowModule = null;
            recipientsEveryone = null;
        }
    }

    private void tryLoadNametag() {
        try {
            Class<?> apolloClass = Class.forName("com.lunarclient.apollo.Apollo");
            Object moduleManager = apolloClass.getMethod("getModuleManager").invoke(null);
            Class<?> nametagClass = Class.forName("com.lunarclient.apollo.module.nametag.NametagModule");
            Method getModule = moduleManager.getClass().getMethod("getModule", Class.class);
            nametagModule = getModule.invoke(moduleManager, nametagClass);

            Class<?> serializerClass = Class.forName(
                "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
            );
            legacySerializer = serializerClass.getMethod("legacySection").invoke(null);

            if (recipientsEveryone == null) {
                Class<?> recipientsClass = Class.forName("com.lunarclient.apollo.recipients.Recipients");
                recipientsEveryone = recipientsClass.getMethod("ofEveryone").invoke(null);
            }
        } catch (ReflectiveOperationException ignored) {
            nametagModule = null;
            legacySerializer = null;
        }
    }

    private void tryLoadNumbers() {
        try {
            Class<?> apolloClass = Class.forName("com.lunarclient.apollo.Apollo");
            Object moduleManager = apolloClass.getMethod("getModuleManager").invoke(null);
            Class<?> modSettingClass = Class.forName("com.lunarclient.apollo.module.modsetting.ModSettingModule");
            Method getModule = moduleManager.getClass().getMethod("getModule", Class.class);
            modSettingModule = getModule.invoke(moduleManager, modSettingClass);

            Class<?> modScoreboard = resolveModScoreboard();
            if (modScoreboard == null) return;
            Field numbers = modScoreboard.getField("NUMBERS");
            numbersOption = numbers.get(null);
        } catch (ReflectiveOperationException ignored) {
            modSettingModule = null;
            numbersOption = null;
        }
    }

    private static Class<?> resolveModScoreboard() {
        String[] candidates = {
            "com.lunarclient.apollo.mods.impl.ModScoreboard",
            "com.lunarclient.apollo.mods.ModScoreboard",
        };
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    @Override
    public boolean available() {
        return present;
    }

    @Override
    public void overrideGlow(UUID targetId, Integer rgb) {
        if (targetId == null || glowModule == null || recipientsEveryone == null) return;
        try {
            if (rgb == null) {
                glowModule
                    .getClass()
                    .getMethod(
                        "overrideGlow",
                        Class.forName("com.lunarclient.apollo.recipients.Recipients"),
                        UUID.class
                    )
                    .invoke(glowModule, recipientsEveryone, targetId);
                return;
            }
            Color color = new Color(rgb);
            glowModule
                .getClass()
                .getMethod(
                    "overrideGlow",
                    Class.forName("com.lunarclient.apollo.recipients.Recipients"),
                    UUID.class,
                    Color.class
                )
                .invoke(glowModule, recipientsEveryone, targetId, color);
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void resetGlow(UUID targetId) {
        if (targetId == null || glowModule == null || recipientsEveryone == null) return;
        try {
            glowModule
                .getClass()
                .getMethod("resetGlow", Class.forName("com.lunarclient.apollo.recipients.Recipients"), UUID.class)
                .invoke(glowModule, recipientsEveryone, targetId);
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void overrideNametag(UUID targetId, List<String> legacyLines, String visibilityOverride) {
        if (
            targetId == null ||
            legacyLines == null ||
            legacyLines.isEmpty() ||
            nametagModule == null ||
            recipientsEveryone == null ||
            legacySerializer == null
        ) return;
        try {
            Method deserialize = legacySerializer.getClass().getMethod("deserialize", String.class);
            List<Object> components = new ArrayList<>(legacyLines.size());
            for (String line : legacyLines) {
                if (line == null) continue;
                String colored = ChatColor.translateAlternateColorCodes('&', line);
                components.add(deserialize.invoke(legacySerializer, colored));
            }
            if (components.isEmpty()) return;

            Class<?> nametagClass = Class.forName("com.lunarclient.apollo.module.nametag.Nametag");
            Object builder = nametagClass.getMethod("builder").invoke(null);
            builder.getClass().getMethod("lines", List.class).invoke(builder, components);

            Class<?> visibilityClass = Class.forName("com.lunarclient.apollo.module.nametag.NametagVisibilityOverride");
            String visKey = visibilityOverride == null ? "NONE" : visibilityOverride.trim().toUpperCase(Locale.ROOT);
            Object visibility = Enum.valueOf(visibilityClass.asSubclass(Enum.class), visKey);
            builder.getClass().getMethod("visibilityOverride", visibilityClass).invoke(builder, visibility);

            Object nametag = builder.getClass().getMethod("build").invoke(builder);
            nametagModule
                .getClass()
                .getMethod(
                    "overrideNametag",
                    Class.forName("com.lunarclient.apollo.recipients.Recipients"),
                    UUID.class,
                    nametagClass
                )
                .invoke(nametagModule, recipientsEveryone, targetId, nametag);
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void resetNametag(UUID targetId) {
        if (targetId == null || nametagModule == null || recipientsEveryone == null) return;
        try {
            nametagModule
                .getClass()
                .getMethod("resetNametag", Class.forName("com.lunarclient.apollo.recipients.Recipients"), UUID.class)
                .invoke(nametagModule, recipientsEveryone, targetId);
        } catch (ReflectiveOperationException ignored) {}
    }

    @Override
    public void setNumbersDisabled(UUID viewerId, boolean disabled) {
        if (viewerId == null || modSettingModule == null || numbersOption == null) return;
        try {
            Class<?> apolloClass = Class.forName("com.lunarclient.apollo.Apollo");
            Object playerManager = apolloClass.getMethod("getPlayerManager").invoke(null);
            Object optional = playerManager
                .getClass()
                .getMethod("getPlayer", UUID.class)
                .invoke(playerManager, viewerId);
            if (!(optional instanceof Optional)) return;
            Optional<?> apolloPlayer = (Optional<?>) optional;
            if (!apolloPlayer.isPresent()) return;

            Object options = modSettingModule.getClass().getMethod("getOptions").invoke(modSettingModule);
            Method set = null;
            for (Method method : options.getClass().getMethods()) {
                if (!"set".equals(method.getName()) || method.getParameterCount() != 3) continue;
                set = method;
                break;
            }
            if (set == null) return;
            set.invoke(options, apolloPlayer.get(), numbersOption, !disabled);
        } catch (ReflectiveOperationException ignored) {}
    }
}
