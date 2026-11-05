package net.pkhapps.idispatch.runboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void maven_writes_the_version_into_the_resources() {
        assertTrue(Version.get().matches("\\d+\\.\\d+(\\.\\d+)?(-SNAPSHOT)?"), () -> "Not a version: '" + Version.get() + "'");
    }
}
