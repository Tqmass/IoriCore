package us.iorilabs.proxy.utilities;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerHeadAscii {

    private static final String[] PALETTE = {
        "<#f2d3b1>",
        "<#e0b090>",
        "<#c48a6a>",
        "<#8b5a3c>",
        "<#5c3a21>",
        "<#3d2414>",
        "<#2a1810>",
        "<white>",
        "<gray>",
        "<dark_gray>",
        "<black>",
        "<red>",
        "<gold>",
        "<yellow>",
        "<green>",
        "<aqua>",
    };

    private static final ConcurrentHashMap<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private PlayerHeadAscii() {}

    public static List<String> linesFor(String playerName) {
        String key = playerName == null || playerName.isBlank() ? "Steve" : playerName;
        return CACHE.computeIfAbsent(key.toLowerCase(), ignored -> generate(key));
    }

    private static List<String> generate(String name) {
        byte[] seed = name.getBytes(StandardCharsets.UTF_8);
        int hash = 0;

        for (byte b : seed) {
            hash = 31 * hash + (b & 0xff);
        }

        List<String> lines = new ArrayList<>(8);

        for (int row = 0; row < 8; row++) {
            StringBuilder line = new StringBuilder();

            for (int col = 0; col < 8; col++) {
                int idx = Math.floorMod(hash + row * 17 + col * 31, PALETTE.length);
                line.append(PALETTE[idx]).append('█');
            }

            line.append("<reset>");
            lines.add(line.toString());
        }

        return List.copyOf(lines);
    }
}
