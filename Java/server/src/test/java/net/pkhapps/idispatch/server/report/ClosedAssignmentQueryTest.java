package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;

import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENTS_SEQ;
import static net.pkhapps.idispatch.server.jooq.Sequences.MUNICIPALITIES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENTS;
import static net.pkhapps.idispatch.server.jooq.Tables.MUNICIPALITIES;
import static net.pkhapps.idispatch.server.report.ClosedAssignmentListItem.SORT_MUNICIPALITY_NAME_FI;
import static net.pkhapps.idispatch.server.report.ClosedAssignmentListItem.SORT_MUNICIPALITY_NAME_SV;
import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the reports sort by a bilingual name, which the stories cannot show since they run in English: by the name in
 * the reader's language first, which the view asks for by the order of the sort properties.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class ClosedAssignmentQueryTest {

    @Autowired
    ClosedAssignmentQuery query;
    @Autowired
    DSLContext dsl;

    @Test
    void sortsByTheNameInTheReadersLanguageFirst() {
        var marker = unique("");
        // Ordered one way by the Finnish names and the other way by the Swedish ones
        var first = closedAssignmentIn("A" + marker, "Z" + marker);
        var second = closedAssignmentIn("Z" + marker, "A" + marker);

        var inFinnish = query.find(new ClosedAssignmentFilter(null, null, null, null, null),
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(SORT_MUNICIPALITY_NAME_FI, SORT_MUNICIPALITY_NAME_SV)));
        var inSwedish = query.find(new ClosedAssignmentFilter(null, null, null, null, null),
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(SORT_MUNICIPALITY_NAME_SV, SORT_MUNICIPALITY_NAME_FI)));

        assertThat(inFinnish.stream().map(ClosedAssignmentListItem::id).filter(id -> id.equals(first) || id.equals(second)))
                .containsExactly(first, second);
        assertThat(inSwedish.stream().map(ClosedAssignmentListItem::id).filter(id -> id.equals(first) || id.equals(second)))
                .containsExactly(second, first);
    }

    private AssignmentId closedAssignmentIn(String municipalityFi, String municipalitySv) {
        var municipality = dsl.nextval(MUNICIPALITIES_SEQ);
        dsl.insertInto(MUNICIPALITIES)
                .set(MUNICIPALITIES.ID, municipality).set(MUNICIPALITIES.VERSION, 0L)
                .set(MUNICIPALITIES.NAME_FI, municipalityFi).set(MUNICIPALITIES.NAME_SV, municipalitySv)
                .set(MUNICIPALITIES.ACTIVE, true)
                .execute();
        var assignment = dsl.nextval(ASSIGNMENTS_SEQ);
        dsl.insertInto(ASSIGNMENTS)
                .set(ASSIGNMENTS.ID, assignment).set(ASSIGNMENTS.VERSION, 0L)
                .set(ASSIGNMENTS.OPENED, OffsetDateTime.now()).set(ASSIGNMENTS.CLOSED, OffsetDateTime.now())
                .set(ASSIGNMENTS.URGENCY, "B").set(ASSIGNMENTS.MUNICIPALITY_ID, municipality)
                .set(ASSIGNMENTS.DESCRIPTION, "").set(ASSIGNMENTS.ADDRESS, "Sort Street 1")
                .execute();
        return new AssignmentId(assignment);
    }
}
