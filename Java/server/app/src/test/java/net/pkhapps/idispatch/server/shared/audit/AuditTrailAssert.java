package net.pkhapps.idispatch.server.shared.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks what the services recorded in the {@link AuditLog}.
 */
public final class AuditTrailAssert {

    /**
     * The user that {@code @WithKeycloakUser(username = "anna", firstName = "Anna", lastName = "Admin", …)} logs in.
     */
    public static final AuditUser ANNA = new AuditUser("subject-anna", "Anna", "Admin");

    private AuditTrailAssert() {
    }

    /**
     * Returns now, as precisely as the audit log records time, for comparing with the events recorded after it.
     */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Asserts that the entity's events are of the given kinds, newest first, all by the user and none before the given
     * time.
     */
    public static void assertEvents(AuditLog auditLog, AuditedEntity entity, AuditUser user, Instant since,
                                    AuditEventKind... newestFirst) {
        List<AuditEvent> events = auditLog.findEvents(entity);
        assertThat(events).extracting(AuditEvent::kind).containsExactly(newestFirst);
        var until = Instant.now();
        assertThat(events).allSatisfy(event -> {
            assertThat(event.user()).isEqualTo(user);
            assertThat(event.part()).isNull();
            assertThat(event.timestamp()).isBetween(since, until);
        });
    }

    /**
     * Returns the kinds of the entity's events, newest first.
     */
    public static List<AuditEventKind> kindsOf(AuditLog auditLog, AuditedEntity entity) {
        return auditLog.findEvents(entity).stream().map(AuditEvent::kind).toList();
    }
}
