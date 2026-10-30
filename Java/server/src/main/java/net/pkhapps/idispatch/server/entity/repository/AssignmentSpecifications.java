package net.pkhapps.idispatch.server.entity.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.time.Instant;
import java.util.ArrayList;

public final class AssignmentSpecifications {

    /**
     * Sort property for whether a reviewer has closed the assignment's report, which {@link #orderedBy(Sort)} sorts
     * by even though it is not a property of {@link Assignment}. Open reports come first in ascending order.
     */
    public static final String REPORT_CLOSED = "reportClosed";

    private AssignmentSpecifications() {
    }

    public static Specification<Assignment> closedOnly() {
        return (root, query, cb) -> cb.isNotNull(root.get(Assignment.PROP_CLOSED));
    }

    public static Specification<Assignment> closedAtOrAfter(@NonNull Instant instant) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(Assignment.PROP_CLOSED), instant);
    }

    public static Specification<Assignment> closedBefore(@NonNull Instant instant) {
        return (root, query, cb) -> cb.lessThan(root.get(Assignment.PROP_CLOSED), instant);
    }

    public static Specification<Assignment> municipality(@NonNull Municipality municipality) {
        return (root, query, cb) -> cb.equal(root.get(Assignment.PROP_MUNICIPALITY), municipality);
    }

    public static Specification<Assignment> type(@NonNull AssignmentType type) {
        return (root, query, cb) -> cb.equal(root.get(Assignment.PROP_TYPE), type);
    }

    public static Specification<Assignment> reportClosed(boolean closed) {
        return (root, query, cb) -> closed ? hasClosedReport(root, query, cb) : cb.not(hasClosedReport(root, query, cb));
    }

    /**
     * Sorts the assignments like the sort would, also by {@link #REPORT_CLOSED}, and by ID last, so that pages don't
     * overlap or skip assignments that are equal in every other respect. Use with an unsorted page request, since
     * sorting the page request replaces this order.
     */
    public static Specification<Assignment> orderedBy(@NonNull Sort sort) {
        return (root, query, cb) -> {
            var orders = new ArrayList<Order>();
            for (var order : sort) {
                if (order.getProperty().equals(REPORT_CLOSED)) {
                    var reportClosed = cb.<Integer>selectCase().when(hasClosedReport(root, query, cb), 1).otherwise(0);
                    orders.add(order.isAscending() ? cb.asc(reportClosed) : cb.desc(reportClosed));
                } else {
                    // Joins optional associations, such as the type, with outer joins so that sorting keeps the
                    // assignments without one
                    orders.addAll(QueryUtils.toOrders(Sort.by(order), root, cb));
                }
            }
            if (sort.getOrderFor(AbstractEntity.PROP_ID) == null) {
                orders.add(cb.asc(root.get(AbstractEntity.PROP_ID)));
            }
            query.orderBy(orders);
            return cb.conjunction();
        };
    }

    private static Predicate hasClosedReport(Root<Assignment> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        var subquery = query.subquery(Integer.class);
        var report = subquery.from(AssignmentReport.class);
        subquery.select(cb.literal(1)).where(
                cb.equal(report.get("assignment"), root),
                cb.isNotNull(report.get("closed").get("subject")));
        return cb.exists(subquery);
    }
}
