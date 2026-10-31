package net.pkhapps.idispatch.server.report;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Who did something to an {@link AssignmentReport}, such as editing or closing it, and when. The subject is the user's
 * ID in Keycloak, which stays the same even if the user changes their name. The names are copied from the ID token at
 * the time, and are {@code null} if the token had none. Each use names its own columns with {@code @AttributeOverride}.
 */
@Embeddable
public class UserStamp implements Serializable {

    private String subject;
    private String firstName;
    private String lastName;
    private Instant timestamp;

    protected UserStamp() {
    }

    public UserStamp(String subject, String firstName, String lastName, Instant timestamp) {
        this.subject = checkNotNull(subject);
        this.firstName = firstName;
        this.lastName = lastName;
        this.timestamp = checkNotNull(timestamp);
    }

    public String getSubject() {
        return subject;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the first and last name of the user, or the subject if the user has no name.
     */
    public String getDisplayName() {
        var name = Stream.of(firstName, lastName)
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? subject : name;
    }
}
