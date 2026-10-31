package net.pkhapps.idispatch.server;

import jakarta.persistence.EntityManager;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.jooq.Public;
import net.pkhapps.idispatch.server.report.AssignmentReport;
import net.pkhapps.idispatch.server.ui.TestData;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_REPORTS;
import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that jOOQ works alongside JPA while the features move over: the checked-in metadata classes match the schema
 * that the Flyway migrations create, and jOOQ queries take part in the same Spring transactions as JPA.
 */
@SpringBootTest
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class JooqTest {

    @Autowired
    DSLContext dsl;
    @Autowired
    EntityManager entityManager;
    @Autowired
    TestData testData;
    @Autowired
    PlatformTransactionManager txManager;

    /**
     * Fails when a migration has been added without regenerating the metadata classes with
     * {@code mvn -Pjooq-codegen generate-sources}.
     */
    @Test
    void generatedMetadataMatchesTheMigratedSchema() {
        var migrated = dsl.meta().getSchemas().stream()
                .filter(schema -> schema.getName().equals(Public.PUBLIC.getName()))
                .flatMap(schema -> schema.getTables().stream())
                .filter(table -> !table.getName().equals("flyway_schema_history"))
                .toList();

        assertThat(migrated).as("Migrated tables").isNotEmpty();
        assertThat(columnsByTable(Public.PUBLIC.getTables().toArray(Table[]::new)))
                .isEqualTo(columnsByTable(migrated.toArray(Table[]::new)));
    }

    @Test
    void jooqSeesWhatJpaHasFlushedInTheSameTransaction() {
        var tx = new TransactionTemplate(txManager);
        var assignmentId = testData.openAssignment(testData.municipality(unique("jOOQ ")),
                testData.assignmentType(unique("jOOQ"), "jOOQ"), "jOOQ Street 1");

        var seen = tx.execute(status -> {
            entityManager.persist(new AssignmentReport(new AssignmentId(assignmentId)));
            entityManager.flush();
            var count = dsl.fetchCount(ASSIGNMENT_REPORTS, ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(assignmentId));
            status.setRollbackOnly();
            return count;
        });

        assertThat(seen).isEqualTo(1);
        assertThat(dsl.fetchCount(ASSIGNMENT_REPORTS, ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(assignmentId)))
                .as("Rolled back").isZero();
    }

    private static Map<String, String> columnsByTable(Table<?>... tables) {
        var columns = new TreeMap<String, String>();
        for (var table : tables) {
            columns.put(table.getName(), java.util.Arrays.stream(table.fields())
                    .map(field -> field.getName() + " " + field.getDataType().getTypeName()
                            + (field.getDataType().nullable() ? "" : " not null"))
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        return columns;
    }
}
