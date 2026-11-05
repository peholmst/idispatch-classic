package net.pkhapps.idispatch.server.shared.audit;

import net.pkhapps.idispatch.server.jooq.tables.records.AuditEventsRecord;
import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.AUDIT_EVENTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.AUDIT_EVENTS;

/**
 * Keeps who created and changed the entities, and when, in the {@code audit_events} table. Only that something was
 * done is kept, not what changed. The events are kept forever.
 * <p>
 * The services record an event in the same transaction as the change, so that a change is never saved without its
 * event, and an event is never kept for a change that was refused or rolled back; recording outside a transaction
 * fails. The user is the one logged in: their Keycloak subject, and their names from the ID token at the time.
 * <p>
 * Reading is not secured, like the features' {@code *Queries}: anyone who can see an entity may see its history.
 */
@Service
public class AuditLog {

    private final DSLContext dsl;

    AuditLog(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Records that the logged-in user did something to the whole entity.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditedEntity entity, AuditEventKind kind) {
        record(entity, kind, null);
    }

    /**
     * Records that the logged-in user did something to a part of the entity, or to the whole if the part is
     * {@code null}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditedEntity entity, AuditEventKind kind, @Nullable String part) {
        var user = currentUser();
        dsl.insertInto(AUDIT_EVENTS)
                .set(AUDIT_EVENTS.ID, dsl.nextval(AUDIT_EVENTS_SEQ))
                .set(AUDIT_EVENTS.ENTITY_TYPE, entity.type().name())
                .set(AUDIT_EVENTS.ENTITY_ID, entity.id())
                .set(AUDIT_EVENTS.KIND, kind.name())
                .set(AUDIT_EVENTS.PART, part)
                .set(AUDIT_EVENTS.USER_SUBJECT, user.subject())
                .set(AUDIT_EVENTS.USER_FIRST_NAME, user.firstName())
                .set(AUDIT_EVENTS.USER_LAST_NAME, user.lastName())
                .set(AUDIT_EVENTS.OCCURRED_AT, now().atOffset(ZoneOffset.UTC))
                .execute();
    }

    /**
     * Records that the logged-in user edited the whole entity, merging the edit into the latest event if that is an
     * edit of the whole entity by the same user less than the given time ago: its time is moved forward instead of a
     * new event being added. For entities that are saved field by field, so that filling in a form is one event.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordMergedEdit(AuditedEntity entity, Duration mergeWindow) {
        var user = currentUser();
        var now = now();
        var latest = dsl.selectFrom(AUDIT_EVENTS)
                .where(AUDIT_EVENTS.ENTITY_TYPE.eq(entity.type().name()))
                .and(AUDIT_EVENTS.ENTITY_ID.eq(entity.id()))
                .orderBy(AUDIT_EVENTS.OCCURRED_AT.desc(), AUDIT_EVENTS.ID.desc())
                .limit(1)
                .forUpdate()
                .fetchOptional();
        var mergeable = latest.filter(event -> AuditEventKind.EDITED.name().equals(event.getKind())
                && event.getPart() == null
                && user.subject().equals(event.getUserSubject())
                && event.getOccurredAt().toInstant().isAfter(now.minus(mergeWindow)));
        if (mergeable.isPresent()) {
            dsl.update(AUDIT_EVENTS)
                    .set(AUDIT_EVENTS.OCCURRED_AT, now.atOffset(ZoneOffset.UTC))
                    .set(AUDIT_EVENTS.USER_FIRST_NAME, user.firstName())
                    .set(AUDIT_EVENTS.USER_LAST_NAME, user.lastName())
                    .where(AUDIT_EVENTS.ID.eq(mergeable.get().getId()))
                    .execute();
        } else {
            record(entity, AuditEventKind.EDITED);
        }
    }

    /**
     * Returns every event of the entity, newest first.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<AuditEvent> findEvents(AuditedEntity entity) {
        return dsl.selectFrom(AUDIT_EVENTS)
                .where(AUDIT_EVENTS.ENTITY_TYPE.eq(entity.type().name()))
                .and(AUDIT_EVENTS.ENTITY_ID.eq(entity.id()))
                .orderBy(AUDIT_EVENTS.OCCURRED_AT.desc(), AUDIT_EVENTS.ID.desc())
                .fetch(AuditLog::toEvent);
    }

    /**
     * Returns the newest event of the entity, of any kind and part, or empty if none has been recorded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<AuditEvent> findLatest(AuditedEntity entity) {
        return dsl.selectFrom(AUDIT_EVENTS)
                .where(AUDIT_EVENTS.ENTITY_TYPE.eq(entity.type().name()))
                .and(AUDIT_EVENTS.ENTITY_ID.eq(entity.id()))
                .orderBy(AUDIT_EVENTS.OCCURRED_AT.desc(), AUDIT_EVENTS.ID.desc())
                .limit(1)
                .fetchOptional(AuditLog::toEvent);
    }

    /**
     * Returns the newest event of each part of the entity that has any, by part. Events of the whole entity are left
     * out.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Map<String, AuditEvent> findLatestByPart(AuditedEntity entity) {
        var latest = new HashMap<String, AuditEvent>();
        dsl.selectFrom(AUDIT_EVENTS)
                .where(AUDIT_EVENTS.ENTITY_TYPE.eq(entity.type().name()))
                .and(AUDIT_EVENTS.ENTITY_ID.eq(entity.id()))
                .and(AUDIT_EVENTS.PART.isNotNull())
                .orderBy(AUDIT_EVENTS.OCCURRED_AT.desc(), AUDIT_EVENTS.ID.desc())
                .fetch(AuditLog::toEvent)
                .forEach(event -> latest.putIfAbsent(event.part(), event));
        return latest;
    }

    /**
     * Returns the logged-in user as the events record them.
     *
     * @throws IllegalStateException if nobody is logged in
     */
    private static AuditUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Nobody is logged in to record the event for");
        }
        if (authentication.getPrincipal() instanceof OidcUser user) {
            return new AuditUser(user.getSubject(), user.getGivenName(), user.getFamilyName());
        }
        // Only when not logged in through Keycloak, such as in tests
        return new AuditUser(authentication.getName(), null, null);
    }

    private static Instant now() {
        // As precise as the database keeps it, so that an event reads back as it was recorded
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static AuditEvent toEvent(AuditEventsRecord row) {
        return new AuditEvent(
                new AuditedEntity(AuditedEntity.Type.valueOf(row.getEntityType()), row.getEntityId()),
                AuditEventKind.valueOf(row.getKind()),
                row.getPart(),
                new AuditUser(row.getUserSubject(), row.getUserFirstName(), row.getUserLastName()),
                row.getOccurredAt().toInstant());
    }
}
