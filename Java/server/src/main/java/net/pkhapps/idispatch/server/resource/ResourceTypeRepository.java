package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.jooq.tables.records.ResourceTypesRecord;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCE_TYPES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_TYPES;

/**
 * Stores the {@link ResourceType}s in the {@code resource_types} table. A code that is already in use is refused by the
 * table's unique constraint, which reaches the caller as a {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class ResourceTypeRepository {

    private final DSLContext dsl;

    ResourceTypeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the resource types whose code starts with the search term or whose description contains it, ignoring case,
     * ordered by code.
     */
    List<ResourceTypeListItem> findListItems(boolean includeDeleted, @Nullable String searchTerm) {
        var conditions = new ArrayList<Condition>();
        if (!includeDeleted) {
            conditions.add(RESOURCE_TYPES.ACTIVE.isTrue());
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            conditions.add(RESOURCE_TYPES.CODE.startsWithIgnoreCase(searchTerm)
                    .or(RESOURCE_TYPES.DESCRIPTION_FI.containsIgnoreCase(searchTerm))
                    .or(RESOURCE_TYPES.DESCRIPTION_SV.containsIgnoreCase(searchTerm)));
        }
        return selectListItems()
                .where(DSL.and(conditions))
                .orderBy(RESOURCE_TYPES.CODE)
                .fetch(ResourceTypeRepository::toListItem);
    }

    Optional<ResourceTypeListItem> findListItem(ResourceTypeId id) {
        return selectListItems()
                .where(RESOURCE_TYPES.ID.eq(id.value()))
                .fetchOptional(ResourceTypeRepository::toListItem);
    }

    Optional<ResourceType> findById(ResourceTypeId id) {
        return dsl.selectFrom(RESOURCE_TYPES)
                .where(RESOURCE_TYPES.ID.eq(id.value()))
                .fetchOptional(ResourceTypeRepository::toResourceType);
    }

    ResourceType insert(ResourceTypeDetails details) {
        var id = new ResourceTypeId(dsl.nextval(RESOURCE_TYPES_SEQ));
        dsl.insertInto(RESOURCE_TYPES)
                .set(RESOURCE_TYPES.ID, id.value())
                .set(RESOURCE_TYPES.VERSION, 0L)
                .set(RESOURCE_TYPES.CODE, details.code().value())
                .set(RESOURCE_TYPES.DESCRIPTION_FI, details.description().finnish())
                .set(RESOURCE_TYPES.DESCRIPTION_SV, details.description().swedish())
                .set(RESOURCE_TYPES.ACTIVE, true)
                .execute();
        return new ResourceType(id, 0L, details.code(), details.description(), true);
    }

    /**
     * Saves the details of the type, provided that its version is still the stored one.
     *
     * @return the type with its new version
     * @throws OptimisticLockingFailureException if the type has been changed since this version, or no longer exists
     */
    ResourceType update(ResourceType type) {
        var updated = dsl.update(RESOURCE_TYPES)
                .set(RESOURCE_TYPES.CODE, type.code().value())
                .set(RESOURCE_TYPES.DESCRIPTION_FI, type.description().finnish())
                .set(RESOURCE_TYPES.DESCRIPTION_SV, type.description().swedish())
                .set(RESOURCE_TYPES.VERSION, type.version() + 1)
                .where(RESOURCE_TYPES.ID.eq(type.id().value()))
                .and(RESOURCE_TYPES.VERSION.eq(type.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Resource type %s has been changed or removed since version %d".formatted(type.id().value(),
                            type.version()));
        }
        return findById(type.id()).orElseThrow();
    }

    /**
     * Deletes the type softly, or restores it. Does nothing if there is no such type.
     */
    void setActive(ResourceTypeId id, boolean active) {
        dsl.update(RESOURCE_TYPES)
                .set(RESOURCE_TYPES.ACTIVE, active)
                .set(RESOURCE_TYPES.VERSION, RESOURCE_TYPES.VERSION.plus(1))
                .where(RESOURCE_TYPES.ID.eq(id.value()))
                .execute();
    }

    private org.jooq.SelectJoinStep<Record5<Long, String, String, String, Boolean>> selectListItems() {
        return dsl.select(RESOURCE_TYPES.ID, RESOURCE_TYPES.CODE, RESOURCE_TYPES.DESCRIPTION_FI,
                        RESOURCE_TYPES.DESCRIPTION_SV, RESOURCE_TYPES.ACTIVE)
                .from(RESOURCE_TYPES);
    }

    private static ResourceTypeListItem toListItem(Record5<Long, String, String, String, Boolean> row) {
        return new ResourceTypeListItem(new ResourceTypeId(row.value1()), row.value2(), row.value3(), row.value4(),
                row.value5());
    }

    private static ResourceType toResourceType(ResourceTypesRecord row) {
        return new ResourceType(new ResourceTypeId(row.getId()), row.getVersion(), new ResourceTypeCode(row.getCode()),
                new BilingualText(row.getDescriptionFi(), row.getDescriptionSv()), row.getActive());
    }
}
