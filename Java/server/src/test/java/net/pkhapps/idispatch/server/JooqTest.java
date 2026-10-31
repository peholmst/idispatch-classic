package net.pkhapps.idispatch.server;

import net.pkhapps.idispatch.server.resource.Resource;
import net.pkhapps.idispatch.server.resource.ResourceRepository;
import net.pkhapps.idispatch.server.jooq.Public;
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

import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_TYPES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_TYPES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that jOOQ works alongside JPA while the features move over: the checked-in metadata classes match the schema
 * that the Flyway migrations create, and jOOQ queries take part in the same Spring transactions as JPA.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class JooqTest {

    @Autowired
    DSLContext dsl;
    @Autowired
    ResourceRepository resourceRepository;
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
        var callSign = "jOOQ " + System.nanoTime();
        var typeId = dsl.nextval(RESOURCE_TYPES_SEQ);
        dsl.insertInto(RESOURCE_TYPES).set(RESOURCE_TYPES.ID, typeId).set(RESOURCE_TYPES.VERSION, 0L)
                .set(RESOURCE_TYPES.CODE, callSign).set(RESOURCE_TYPES.DESCRIPTION_FI, "jOOQ")
                .set(RESOURCE_TYPES.DESCRIPTION_SV, "").set(RESOURCE_TYPES.ACTIVE, true).execute();

        var seen = tx.execute(status -> {
            var resource = new Resource();
            resource.setCallSign(callSign);
            resource.setTypeId(typeId);
            resourceRepository.saveAndFlush(resource);
            var count = dsl.fetchCount(RESOURCES, RESOURCES.CALL_SIGN.eq(callSign));
            status.setRollbackOnly();
            return count;
        });

        assertThat(seen).isEqualTo(1);
        assertThat(dsl.fetchCount(RESOURCES, RESOURCES.CALL_SIGN.eq(callSign))).as("Rolled back").isZero();
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
