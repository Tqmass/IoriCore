package us.iorilabs.api.modules;

public enum ModuleId {
    ECONOMY,
    CHAT_GAMES,
    GENDER,
    TAGS,
    STAFF_MODE,
    GEYSER_HOOK,
    LITEBANS_HOOK,
    CHAT_LOGS,
    CHAT_FORMAT,
    RANKS,
    PUNISHMENTS,
    PLUGIN_MESSAGING,
    STAFF_AUTH,
    PLAYER_SETTINGS,
    CUSTOM_JOINS,
    ACCOUNT_BRAND,
    ACTION_BAR,
    VAULT_HOOK,
    APOLLO_HOOK,
    NLOGIN_HOOK,
    CORE,

    /** @deprecated use {@link #CORE} */
    @Deprecated
    ESSENTIALS,

    SCOREBOARD,
    TAB,
    COINSHOP,
    DISCORD,
    NAMETAG;

    public ModuleId canonical() {
        return this == ESSENTIALS ? CORE : this;
    }

    public String modulesConfigKey() {
        ModuleId id = canonical();
        return id.name().replace('_', '-');
    }
}
