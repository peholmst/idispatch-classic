package net.pkhapps.idispatch.server.util;

public final class Strings {
    private Strings() {
        throw new AssertionError("No net.pkhapps.idispatch.server.util.Strings instances for you!");
    }

    public static String requireNoLongerThan(int maxLength, String s) {
        if (s != null && s.length() > maxLength) {
            throw new IllegalArgumentException("String cannot be longer than %d characters".formatted(maxLength));
        }
        return s;
    }
}
