package net.pkhapps.idispatch.server;

import net.pkhapps.idispatch.server.jooq.Public;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the checked-in jOOQ metadata classes match the schema that the Flyway migrations create.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class JooqTest {

    @Autowired
    DSLContext dsl;

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
