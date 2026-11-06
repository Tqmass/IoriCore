package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class YamlSlotParser {

    private YamlSlotParser() {}

    public static List<Integer> parse(Object raw) {
        if (raw == null) return Collections.emptyList();
        if (raw instanceof Number) {
            return Collections.singletonList(((Number) raw).intValue());
        }
        if (raw instanceof List) {
            Set<Integer> slots = new LinkedHashSet<>();
            for (Object entry : (List<?>) raw) slots.addAll(parse(entry));
            return new ArrayList<>(slots);
        }
        return parse(String.valueOf(raw));
    }

    public static List<Integer> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        Set<Integer> slots = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) continue;
            int dash = token.indexOf('-');
            if (dash > 0) {
                try {
                    int from = Integer.parseInt(token.substring(0, dash).trim());
                    int to = Integer.parseInt(token.substring(dash + 1).trim());
                    int min = Math.min(from, to);
                    int max = Math.max(from, to);
                    for (int slot = min; slot <= max; slot++) slots.add(slot);
                } catch (NumberFormatException ignored) {}
                continue;
            }
            try {
                slots.add(Integer.parseInt(token));
            } catch (NumberFormatException ignored) {}
        }
        return new ArrayList<>(slots);
    }
}
