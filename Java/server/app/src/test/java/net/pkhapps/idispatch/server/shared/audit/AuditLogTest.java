package net.pkhapps.idispatch.server.shared.audit;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the services rely on the audit log for: that an event is only recorded together with its change, and that the
 * newest event of each part of an entity can be found.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(username = "audited")
class AuditLogTest {

    @Autowired
    AuditLog auditLog;
    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void eventIsNotRecordedOutsideTheTransactionOfItsChange() {
        var entity = newEntity();

        assertThatThrownBy(() -> auditLog.record(entity, AuditEventKind.CREATED))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditLog.findEvents(entity)).isEmpty();
    }

    @Test
    void eventOfAChangeThatIsRolledBackIsNotKept() {
        var entity = newEntity();

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            auditLog.record(entity, AuditEventKind.CREATED);
            status.setRollbackOnly();
        });

        assertThat(auditLog.findEvents(entity)).isEmpty();
    }

    @Test
    void newestEventOfEachPartIsFound() {
        var entity = newEntity();
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            auditLog.record(entity, AuditEventKind.CREATED);
            auditLog.record(entity, AuditEventKind.EDITED, "A");
            auditLog.record(entity, AuditEventKind.EDITED, "B");
            auditLog.record(entity, AuditEventKind.EDITED, "A");
            auditLog.record(entity, AuditEventKind.CLOSED);
        });

        var byPart = auditLog.findLatestByPart(entity);
        var all = auditLog.findEvents(entity);

        assertThat(byPart).containsOnlyKeys("A", "B");
        assertThat(byPart.get("A")).isEqualTo(all.get(1));
        assertThat(byPart.get("B")).isEqualTo(all.get(2));
        assertThat(auditLog.findLatest(entity)).contains(all.getFirst());
        assertThat(all.getFirst().user()).isEqualTo(new AuditUser("audited", null, null));
    }

    private static AuditedEntity newEntity() {
        // Far above the IDs the sequences give, so it is no entity of another test
        return new AuditedEntity(AuditedEntity.Type.MUNICIPALITY,
                ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE));
    }
}
