package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Who last changed part of an {@link AssignmentReport}, and when. The subject is the user's ID in Keycloak, which
 * stays the same even if the user changes their name. The names are copied from the ID token at the time of the edit,
 * and are {@code null} if the token had none.
 */
@Embeddable
public class LastEdit implements Serializable {

    @Column(name = "last_edited_by_subject")
    private String subject;
    @Column(name = "last_edited_by_first_name")
    private String firstName;
    @Column(name = "last_edited_by_last_name")
    private String lastName;
    @Column(name = "last_edited_at")
    private Instant editedAt;

    protected LastEdit() {
    }

    public LastEdit(String subject, String firstName, String lastName, Instant editedAt) {
        this.subject = checkNotNull(subject);
        this.firstName = firstName;
        this.lastName = lastName;
        this.editedAt = checkNotNull(editedAt);
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

    public Instant getEditedAt() {
        return editedAt;
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
