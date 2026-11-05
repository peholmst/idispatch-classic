package net.pkhapps.idispatch.server.shared.audit;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * The user behind an {@link AuditEvent}. The subject is the user's ID in Keycloak, which stays the same even if the user
 * changes their name. The names are copied from the ID token when the event happens, and are {@code null} if the token
 * had none; later changes in Keycloak don't change them.
 */
public record AuditUser(String subject, @Nullable String firstName, @Nullable String lastName) {

    public AuditUser {
        requireNonNull(subject);
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
