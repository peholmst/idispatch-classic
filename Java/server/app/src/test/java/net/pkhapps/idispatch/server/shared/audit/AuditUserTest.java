package net.pkhapps.idispatch.server.shared.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditUserTest {

    @Test
    void displayNameFallsBackOnTheSubject() {
        assertThat(new AuditUser("subject-ella", "Ella", null).displayName()).isEqualTo("Ella");
        assertThat(new AuditUser("subject-erik", "Erik", "Explorer").displayName()).isEqualTo("Erik Explorer");
        assertThat(new AuditUser("subject-x", " ", null).displayName()).isEqualTo("subject-x");
    }
}
