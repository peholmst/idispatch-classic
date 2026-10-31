package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.shared.BilingualText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the administration view relies on the municipality service for, beyond what the stories show: that two
 * administrators cannot overwrite each other's changes, and that names stay unique in each language.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
@WithMockUser(roles = "ADMIN")
class MunicipalityManagementServiceTest {

    @Autowired
    MunicipalityManagementService service;

    @Test
    void changeBasedOnAnOutdatedVersionIsRefused() {
        var created = service.create(details(unique("Original "), ""));
        var changedByOther = service.update(created.withDetails(details(unique("First "), "")));

        assertThat(changedByOther.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> service.update(created.withDetails(details(unique("Second "), ""))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(service.findById(created.id()).orElseThrow().name()).isEqualTo(changedByOther.name());
    }

    @Test
    void nameInUseIsRefusedInEitherLanguage() {
        var finnish = unique("Parainen ");
        var swedish = unique("Pargas ");
        service.create(details(finnish, swedish));

        assertThatThrownBy(() -> service.create(details(finnish, "")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.create(details("", swedish)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void municipalitiesWithoutASwedishNameDoNotClash() {
        service.create(details(unique("Only Finnish "), ""));

        assertThatCode(() -> service.create(details(unique("Also only Finnish "), ""))).doesNotThrowAnyException();
    }

    private static MunicipalityDetails details(String finnish, String swedish) {
        return new MunicipalityDetails(new BilingualText(finnish, swedish));
    }
}
