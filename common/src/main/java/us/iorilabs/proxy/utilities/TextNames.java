package us.iorilabs.proxy.utilities;

public final class TextNames {

    private TextNames() {}

    public static String displayServer(String raw) {
        if (raw == null || raw.isBlank()) return "?";

        StringBuilder out = new StringBuilder(raw.trim().length());
        boolean capitalizeNext = true;

        for (char c : raw.trim().toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                out.append(c);
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(Character.toLowerCase(c));
            }
        }

        return out.toString();
    }
}
