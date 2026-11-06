package us.iorilabs.bukkit.ranks.enums;

public enum RankEditResult {
    OK,
    UNKNOWN_RANK,
    UNKNOWN_PARENT,
    INVALID_NAME,
    INVALID_VALUE,
    ALREADY_EXISTS,
    NOT_PRESENT,
    CYCLE,
    DEFAULT_RANK,
}
