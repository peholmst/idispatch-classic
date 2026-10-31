package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_REPORTS;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_TYPES;
import static net.pkhapps.idispatch.server.jooq.Tables.MUNICIPALITIES;
import static net.pkhapps.idispatch.server.report.ClosedAssignmentListItem.*;

/**
 * Lists the closed assignments for the reports, straight from the tables of the assignments, their types and
 * municipalities, and their reports.
 */
@Repository
class ClosedAssignmentQuery {

    private static final Field<Boolean> REPORT_CLOSED = DSL.field(DSL.exists(DSL.selectOne()
            .from(ASSIGNMENT_REPORTS)
            .where(ASSIGNMENT_REPORTS.ASSIGNMENT_ID.eq(ASSIGNMENTS.ID))
            .and(ASSIGNMENT_REPORTS.CLOSED_BY_SUBJECT.isNotNull())));

    private static final Map<String, Field<?>> SORT_FIELDS = Map.of(
            SORT_NUMBER, ASSIGNMENTS.ID,
            SORT_CLOSED, ASSIGNMENTS.CLOSED,
            SORT_URGENCY, ASSIGNMENTS.URGENCY,
            SORT_TYPE_CODE, ASSIGNMENT_TYPES.CODE,
            SORT_TYPE_DESCRIPTION_FI, ASSIGNMENT_TYPES.DESCRIPTION_FI,
            SORT_TYPE_DESCRIPTION_SV, ASSIGNMENT_TYPES.DESCRIPTION_SV,
            SORT_MUNICIPALITY_NAME_FI, MUNICIPALITIES.NAME_FI,
            SORT_MUNICIPALITY_NAME_SV, MUNICIPALITIES.NAME_SV,
            SORT_ADDRESS, ASSIGNMENTS.ADDRESS,
            SORT_REPORT_CLOSED, REPORT_CLOSED);

    private final DSLContext dsl;

    ClosedAssignmentQuery(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Returns a page of the closed assignments matching the filter, sorted as the page asks, and by number last so that
     * pages neither overlap nor skip assignments that are equal in every other respect.
     */
    List<ClosedAssignmentListItem> find(ClosedAssignmentFilter filter, Pageable pageable) {
        return dsl.select(ASSIGNMENTS.ID, ASSIGNMENTS.CLOSED, ASSIGNMENTS.URGENCY, ASSIGNMENT_TYPES.CODE,
                        ASSIGNMENT_TYPES.DESCRIPTION_FI, ASSIGNMENT_TYPES.DESCRIPTION_SV, MUNICIPALITIES.NAME_FI,
                        MUNICIPALITIES.NAME_SV, ASSIGNMENTS.ADDRESS, REPORT_CLOSED)
                .from(ASSIGNMENTS)
                .leftJoin(ASSIGNMENT_TYPES).on(ASSIGNMENT_TYPES.ID.eq(ASSIGNMENTS.TYPE_ID))
                .leftJoin(MUNICIPALITIES).on(MUNICIPALITIES.ID.eq(ASSIGNMENTS.MUNICIPALITY_ID))
                .where(conditions(filter))
                .orderBy(order(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(row -> new ClosedAssignmentListItem(new AssignmentId(row.value1()), row.value2().toInstant(),
                        AssignmentUrgency.valueOf(row.value3()), row.value4(), row.value5(), row.value6(),
                        row.value7(), row.value8(), row.value9(), row.value10()));
    }

    int count(ClosedAssignmentFilter filter) {
        return dsl.fetchCount(ASSIGNMENTS, conditions(filter));
    }

    private static List<Condition> conditions(ClosedAssignmentFilter filter) {
        var zone = ZoneId.systemDefault();
        var conditions = new ArrayList<Condition>();
        conditions.add(ASSIGNMENTS.CLOSED.isNotNull());
        if (filter.closedFrom() != null) {
            conditions.add(ASSIGNMENTS.CLOSED.ge(OffsetDateTime.from(filter.closedFrom().atStartOfDay(zone))));
        }
        if (filter.closedTo() != null) {
            conditions.add(ASSIGNMENTS.CLOSED.lt(OffsetDateTime.from(filter.closedTo().plusDays(1).atStartOfDay(zone))));
        }
        if (filter.municipality() != null) {
            conditions.add(ASSIGNMENTS.MUNICIPALITY_ID.eq(filter.municipality().value()));
        }
        if (filter.type() != null) {
            conditions.add(ASSIGNMENTS.TYPE_ID.eq(filter.type().value()));
        }
        if (filter.reportClosed() != null) {
            conditions.add(filter.reportClosed() ? REPORT_CLOSED.isTrue() : REPORT_CLOSED.isFalse());
        }
        return conditions;
    }

    private static List<SortField<?>> order(Sort sort) {
        var order = new ArrayList<SortField<?>>();
        for (var sortOrder : sort) {
            var field = SORT_FIELDS.get(sortOrder.getProperty());
            if (field == null) {
                throw new IllegalArgumentException("Cannot sort by " + sortOrder.getProperty());
            }
            order.add(sortOrder.isAscending() ? field.asc() : field.desc());
        }
        if (sort.getOrderFor(SORT_NUMBER) == null) {
            order.add(ASSIGNMENTS.ID.asc());
        }
        return order;
    }
}
