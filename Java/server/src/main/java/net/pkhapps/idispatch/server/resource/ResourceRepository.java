package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.jooq.tables.records.ResourcesRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.SelectOnConditionStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.RESOURCES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;
import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCE_TYPES;

/**
 * Stores the {@link Resource}s in the {@code resources} table. A call sign that is already in use is refused by the
 * table's unique constraint, which reaches the caller as a {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class ResourceRepository {

    private final DSLContext dsl;

    ResourceRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the resources whose call sign contains the search term, ignoring case, ordered by call sign.
     */
    List<ResourceListItem> findListItems(boolean includeDeleted, @Nullable String searchTerm) {
        var conditions = new ArrayList<Condition>();
        if (!includeDeleted) {
            conditions.add(RESOURCES.ACTIVE.isTrue());
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            conditions.add(RESOURCES.CALL_SIGN.containsIgnoreCase(searchTerm));
        }
        return selectListItems()
                .where(DSL.and(conditions))
                .orderBy(RESOURCES.CALL_SIGN)
                .fetch(ResourceRepository::toListItem);
    }

    /**
     * Finds the resources with the given IDs, whether deleted or not, ordered by call sign.
     */
    List<ResourceListItem> findListItems(Collection<ResourceId> ids) {
        return selectListItems()
                .where(RESOURCES.ID.in(ids.stream().map(ResourceId::value).toList()))
                .orderBy(RESOURCES.CALL_SIGN)
                .fetch(ResourceRepository::toListItem);
    }

    Optional<Resource> findById(ResourceId id) {
        return dsl.selectFrom(RESOURCES)
                .where(RESOURCES.ID.eq(id.value()))
                .fetchOptional(ResourceRepository::toResource);
    }

    Resource insert(ResourceDetails details) {
        var id = new ResourceId(dsl.nextval(RESOURCES_SEQ));
        dsl.insertInto(RESOURCES)
                .set(RESOURCES.ID, id.value())
                .set(RESOURCES.VERSION, 0L)
                .set(RESOURCES.CALL_SIGN, details.callSign().value())
                .set(RESOURCES.TYPE_ID, details.typeId().value())
                .set(RESOURCES.ACTIVE, true)
                .execute();
        return new Resource(id, 0L, details.callSign(), details.typeId(), true);
    }

    /**
     * Saves the details of the resource, provided that its version is still the stored one.
     *
     * @return the resource with its new version
     * @throws OptimisticLockingFailureException if the resource has been changed since this version, or no longer
     *                                           exists
     */
    Resource update(Resource resource) {
        var updated = dsl.update(RESOURCES)
                .set(RESOURCES.CALL_SIGN, resource.callSign().value())
                .set(RESOURCES.TYPE_ID, resource.typeId().value())
                .set(RESOURCES.VERSION, resource.version() + 1)
                .where(RESOURCES.ID.eq(resource.id().value()))
                .and(RESOURCES.VERSION.eq(resource.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Resource %s has been changed or removed since version %d".formatted(resource.id().value(),
                            resource.version()));
        }
        return findById(resource.id()).orElseThrow();
    }

    /**
     * Deletes the resource softly, or restores it. Does nothing if there is no such resource.
     */
    void setActive(ResourceId id, boolean active) {
        dsl.update(RESOURCES)
                .set(RESOURCES.ACTIVE, active)
                .set(RESOURCES.VERSION, RESOURCES.VERSION.plus(1))
                .where(RESOURCES.ID.eq(id.value()))
                .execute();
    }

    private SelectOnConditionStep<Record5<Long, String, String, String, Boolean>> selectListItems() {
        return dsl.select(RESOURCES.ID, RESOURCES.CALL_SIGN, RESOURCE_TYPES.DESCRIPTION_FI,
                        RESOURCE_TYPES.DESCRIPTION_SV, RESOURCES.ACTIVE)
                .from(RESOURCES)
                .join(RESOURCE_TYPES).on(RESOURCE_TYPES.ID.eq(RESOURCES.TYPE_ID));
    }

    private static ResourceListItem toListItem(Record5<Long, String, String, String, Boolean> row) {
        return new ResourceListItem(new ResourceId(row.value1()), row.value2(), row.value3(), row.value4(),
                row.value5());
    }

    private static Resource toResource(ResourcesRecord row) {
        return new Resource(new ResourceId(row.getId()), row.getVersion(), new CallSign(row.getCallSign()),
                new ResourceTypeId(row.getTypeId()), row.getActive());
    }
}
