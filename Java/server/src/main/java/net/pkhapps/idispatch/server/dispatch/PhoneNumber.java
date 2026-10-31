package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A phone number that an {@link SmsDestination} sends to, such as {@code +358401234567}: digits, optionally after a
 * plus sign.
 */
public record PhoneNumber(String value) {

    private static final Pattern VALID = Pattern.compile("\\+?\\d+");

    public PhoneNumber {
        requireNonNull(value);
        if (!isValid(value)) {
            throw new IllegalArgumentException("Not a phone number: " + value);
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && VALID.matcher(value).matches();
    }

    /**
     * Reads the phone numbers from text that separates them with commas or line breaks. Anything but digits and plus
     * signs is left out, so that spaces and dashes don't matter, and so are separators with nothing between them.
     */
    public static Set<PhoneNumber> parse(@Nullable String text) {
        var numbers = new LinkedHashSet<PhoneNumber>();
        if (text == null) {
            return numbers;
        }
        for (var part : text.split("[,\\n]")) {
            var number = part.replaceAll("[^+\\d]", "");
            if (isValid(number)) {
                numbers.add(new PhoneNumber(number));
            }
        }
        return numbers;
    }

    @Override
    public String toString() {
        return value;
    }
}
