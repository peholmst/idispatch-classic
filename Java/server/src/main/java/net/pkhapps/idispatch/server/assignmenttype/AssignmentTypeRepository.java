package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.jooq.tables.records.AssignmentTypesRecord;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.ASSIGNMENT_TYPES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.ASSIGNMENT_TYPES;

/**
 * Stores the {@link AssignmentType}s in the {@code assignment_types} table. A code that is already in use is refused by
 * the table's unique constraint, which reaches the caller as a
 * {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class AssignmentTypeRepository {

    private final DSLContext dsl;

    AssignmentTypeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the assignment types whose code starts with the search term or whose description contains it, ignoring
     * case, ordered by code.
     */
    List<AssignmentTypeListItem> findListItems(boolean includeDeleted, @Nullable String searchTerm) {
        var conditions = new ArrayList<Condition>();
        if (!includeDeleted) {
            conditions.add(ASSIGNMENT_TYPES.ACTIVE.isTrue());
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            conditions.add(ASSIGNMENT_TYPES.CODE.startsWithIgnoreCase(searchTerm)
                    .or(ASSIGNMENT_TYPES.DESCRIPTION_FI.containsIgnoreCase(searchTerm))
                    .or(ASSIGNMENT_TYPES.DESCRIPTION_SV.containsIgnoreCase(searchTerm)));
        }
        return selectListItems()
                .where(DSL.and(conditions))
                .orderBy(ASSIGNMENT_TYPES.CODE)
                .fetch(AssignmentTypeRepository::toListItem);
    }

    Optional<AssignmentTypeListItem> findListItem(AssignmentTypeId id) {
        return selectListItems()
                .where(ASSIGNMENT_TYPES.ID.eq(id.value()))
                .fetchOptional(AssignmentTypeRepository::toListItem);
    }

    List<AssignmentTypeListItem> findListItems(Collection<AssignmentTypeId> ids) {
        return selectListItems()
                .where(ASSIGNMENT_TYPES.ID.in(ids.stream().map(AssignmentTypeId::value).toList()))
                .orderBy(ASSIGNMENT_TYPES.CODE)
                .fetch(AssignmentTypeRepository::toListItem);
    }

    Optional<AssignmentType> findById(AssignmentTypeId id) {
        return dsl.selectFrom(ASSIGNMENT_TYPES)
                .where(ASSIGNMENT_TYPES.ID.eq(id.value()))
                .fetchOptional(AssignmentTypeRepository::toAssignmentType);
    }

    AssignmentType insert(AssignmentTypeDetails details) {
        var id = new AssignmentTypeId(dsl.nextval(ASSIGNMENT_TYPES_SEQ));
        dsl.insertInto(ASSIGNMENT_TYPES)
                .set(ASSIGNMENT_TYPES.ID, id.value())
                .set(ASSIGNMENT_TYPES.VERSION, 0L)
                .set(ASSIGNMENT_TYPES.CODE, details.code().value())
                .set(ASSIGNMENT_TYPES.DESCRIPTION_FI, details.description().finnish())
                .set(ASSIGNMENT_TYPES.DESCRIPTION_SV, details.description().swedish())
                .set(ASSIGNMENT_TYPES.ACTIVE, true)
                .execute();
        return new AssignmentType(id, 0L, details.code(), details.description(), true);
    }

    /**
     * Saves the details of the type, provided that its version is still the stored one.
     *
     * @return the type with its new version
     * @throws OptimisticLockingFailureException if the type has been changed since this version, or no longer exists
     */
    AssignmentType update(AssignmentType type) {
        var updated = dsl.update(ASSIGNMENT_TYPES)
                .set(ASSIGNMENT_TYPES.CODE, type.code().value())
                .set(ASSIGNMENT_TYPES.DESCRIPTION_FI, type.description().finnish())
                .set(ASSIGNMENT_TYPES.DESCRIPTION_SV, type.description().swedish())
                .set(ASSIGNMENT_TYPES.VERSION, type.version() + 1)
                .where(ASSIGNMENT_TYPES.ID.eq(type.id().value()))
                .and(ASSIGNMENT_TYPES.VERSION.eq(type.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Assignment type %s has been changed or removed since version %d".formatted(type.id().value(),
                            type.version()));
        }
        return findById(type.id()).orElseThrow();
    }

    /**
     * Deletes the type softly, or restores it. Does nothing if there is no such type.
     */
    void setActive(AssignmentTypeId id, boolean active) {
        dsl.update(ASSIGNMENT_TYPES)
                .set(ASSIGNMENT_TYPES.ACTIVE, active)
                .set(ASSIGNMENT_TYPES.VERSION, ASSIGNMENT_TYPES.VERSION.plus(1))
                .where(ASSIGNMENT_TYPES.ID.eq(id.value()))
                .execute();
    }

    private org.jooq.SelectJoinStep<Record5<Long, String, String, String, Boolean>> selectListItems() {
        return dsl.select(ASSIGNMENT_TYPES.ID, ASSIGNMENT_TYPES.CODE, ASSIGNMENT_TYPES.DESCRIPTION_FI,
                        ASSIGNMENT_TYPES.DESCRIPTION_SV, ASSIGNMENT_TYPES.ACTIVE)
                .from(ASSIGNMENT_TYPES);
    }

    private static AssignmentTypeListItem toListItem(Record5<Long, String, String, String, Boolean> row) {
        return new AssignmentTypeListItem(new AssignmentTypeId(row.value1()), row.value2(), row.value3(), row.value4(),
                row.value5());
    }

    private static AssignmentType toAssignmentType(AssignmentTypesRecord row) {
        return new AssignmentType(new AssignmentTypeId(row.getId()), row.getVersion(),
                new AssignmentTypeCode(row.getCode()),
                new BilingualText(row.getDescriptionFi(), row.getDescriptionSv()), row.getActive());
    }
}
