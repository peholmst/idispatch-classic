package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.jooq.tables.records.MunicipalitiesRecord;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.pkhapps.idispatch.server.jooq.Sequences.MUNICIPALITIES_SEQ;
import static net.pkhapps.idispatch.server.jooq.Tables.MUNICIPALITIES;

/**
 * Stores the {@link Municipality}s in the {@code municipalities} table. A name that another municipality already has in
 * the same language is refused by the table's unique indexes, which reaches the caller as a
 * {@link org.springframework.dao.DataIntegrityViolationException}.
 */
@Repository
class MunicipalityRepository {

    private final DSLContext dsl;

    MunicipalityRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds the municipalities whose name in either language starts with the search term, ignoring case.
     */
    List<MunicipalityListItem> findListItems(boolean includeDeleted, @Nullable String searchTerm) {
        var conditions = new ArrayList<Condition>();
        if (!includeDeleted) {
            conditions.add(MUNICIPALITIES.ACTIVE.isTrue());
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            conditions.add(MUNICIPALITIES.NAME_FI.startsWithIgnoreCase(searchTerm)
                    .or(MUNICIPALITIES.NAME_SV.startsWithIgnoreCase(searchTerm)));
        }
        return selectListItems()
                .where(DSL.and(conditions))
                .fetch(MunicipalityRepository::toListItem);
    }

    List<MunicipalityListItem> findListItems(Collection<MunicipalityId> ids) {
        return selectListItems()
                .where(MUNICIPALITIES.ID.in(ids.stream().map(MunicipalityId::value).toList()))
                .fetch(MunicipalityRepository::toListItem);
    }

    Optional<MunicipalityListItem> findListItem(MunicipalityId id) {
        return selectListItems()
                .where(MUNICIPALITIES.ID.eq(id.value()))
                .fetchOptional(MunicipalityRepository::toListItem);
    }

    Optional<Municipality> findById(MunicipalityId id) {
        return dsl.selectFrom(MUNICIPALITIES)
                .where(MUNICIPALITIES.ID.eq(id.value()))
                .fetchOptional(MunicipalityRepository::toMunicipality);
    }

    Municipality insert(MunicipalityDetails details) {
        var id = new MunicipalityId(dsl.nextval(MUNICIPALITIES_SEQ));
        dsl.insertInto(MUNICIPALITIES)
                .set(MUNICIPALITIES.ID, id.value())
                .set(MUNICIPALITIES.VERSION, 0L)
                .set(MUNICIPALITIES.NAME_FI, details.name().finnish())
                .set(MUNICIPALITIES.NAME_SV, details.name().swedish())
                .set(MUNICIPALITIES.ACTIVE, true)
                .execute();
        return new Municipality(id, 0L, details.name(), true);
    }

    /**
     * Saves the details of the municipality, provided that its version is still the stored one.
     *
     * @return the municipality with its new version
     * @throws OptimisticLockingFailureException if the municipality has been changed since this version, or no longer
     *                                           exists
     */
    Municipality update(Municipality municipality) {
        var updated = dsl.update(MUNICIPALITIES)
                .set(MUNICIPALITIES.NAME_FI, municipality.name().finnish())
                .set(MUNICIPALITIES.NAME_SV, municipality.name().swedish())
                .set(MUNICIPALITIES.VERSION, municipality.version() + 1)
                .where(MUNICIPALITIES.ID.eq(municipality.id().value()))
                .and(MUNICIPALITIES.VERSION.eq(municipality.version()))
                .execute();
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Municipality %s has been changed or removed since version %d".formatted(
                            municipality.id().value(), municipality.version()));
        }
        return findById(municipality.id()).orElseThrow();
    }

    /**
     * Deletes the municipality softly, or restores it. Does nothing if there is no such municipality.
     */
    void setActive(MunicipalityId id, boolean active) {
        dsl.update(MUNICIPALITIES)
                .set(MUNICIPALITIES.ACTIVE, active)
                .set(MUNICIPALITIES.VERSION, MUNICIPALITIES.VERSION.plus(1))
                .where(MUNICIPALITIES.ID.eq(id.value()))
                .execute();
    }

    private SelectJoinStep<Record4<Long, String, String, Boolean>> selectListItems() {
        return dsl.select(MUNICIPALITIES.ID, MUNICIPALITIES.NAME_FI, MUNICIPALITIES.NAME_SV, MUNICIPALITIES.ACTIVE)
                .from(MUNICIPALITIES);
    }

    private static MunicipalityListItem toListItem(Record4<Long, String, String, Boolean> row) {
        return new MunicipalityListItem(new MunicipalityId(row.value1()), row.value2(), row.value3(), row.value4());
    }

    private static Municipality toMunicipality(MunicipalitiesRecord row) {
        return new Municipality(new MunicipalityId(row.getId()), row.getVersion(),
                new BilingualText(row.getNameFi(), row.getNameSv()), row.getActive());
    }
}
