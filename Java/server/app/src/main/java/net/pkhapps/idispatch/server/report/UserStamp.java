package net.pkhapps.idispatch.server.report;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Who did something to an {@link AssignmentReport}, such as editing or closing it, and when. The subject is the user's
 * ID in Keycloak, which stays the same even if the user changes their name. The names are copied from the ID token at
 * the time, and are {@code null} if the token had none.
 */
record UserStamp(String subject, @Nullable String firstName, @Nullable String lastName, Instant timestamp) {

    public UserStamp {
        requireNonNull(subject);
        requireNonNull(timestamp);
    }

    /**
     * Returns the first and last name of the user, or the subject if the user has no name.
     */
    public String displayName() {
        var name = Stream.of(firstName, lastName)
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? subject : name;
    }
}
