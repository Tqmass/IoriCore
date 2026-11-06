package us.iorilabs.proxy.utilities;

public final class Validate {

    private Validate() {}

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }

        return value;
    }

    public static String notBlank(String value, String name) {
        notNull(value, name);

        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }

        return value;
    }
}
