package us.iorilabs.bukkit.modules.lobby;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class LobbyMenuSlots {

    private LobbyMenuSlots() {}

    public static List<Integer> from(ConfigurationSection section) {
        if (section == null) return List.of();
        if (section.contains("SLOTS")) return parse(section.get("SLOTS"));
        if (section.contains("SLOT")) return parse(section.get("SLOT"));
        return List.of();
    }

    public static List<Integer> parse(Object raw) {
        Set<Integer> slots = new LinkedHashSet<>();
        add(slots, raw);
        return new ArrayList<>(slots);
    }

    private static void add(Set<Integer> slots, Object raw) {
        if (raw == null) return;
        if (raw instanceof Number number) {
            addSlot(slots, number.intValue());
            return;
        }
        if (raw instanceof List<?> list) {
            for (Object entry : list) add(slots, entry);
            return;
        }
        String text = String.valueOf(raw).trim();
        if (text.contains(",")) {
            for (String part : text.split(",")) addRange(slots, part.trim());
            return;
        }
        addRange(slots, text);
    }

    private static void addRange(Set<Integer> slots, String text) {
        if (text.isEmpty()) return;
        int dash = text.indexOf('-');
        if (dash <= 0 || dash == text.length() - 1) {
            addSlot(slots, parseInt(text));
            return;
        }
        int start = parseInt(text.substring(0, dash).trim());
        int end = parseInt(text.substring(dash + 1).trim());
        if (start < 0 || end < 0) return;
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        for (int slot = start; slot <= end; slot++) addSlot(slots, slot);
    }

    private static void addSlot(Set<Integer> slots, int slot) {
        if (slot >= 0 && slot <= 53) slots.add(slot);
    }

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
