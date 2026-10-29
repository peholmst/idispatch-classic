package net.pkhapps.idispatch.runboard.output.gpio;

import net.pkhapps.idispatch.runboard.output.OutputSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs without GPIO hardware, so only the failures can be checked.
 */
class GpioOutputDriverTest {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void missingChip_failsWithTheReason(@TempDir Path dir) {
        var chip = dir.resolve("gpiochip0");

        var ex = assertThrows(IOException.class,
                () -> new GpioOutputDriver(settings(Map.of("chip", chip.toString(), "light.line", "27"))));

        assertEquals("Could not open " + chip + ": No such file or directory (errno 2)", ex.getMessage());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void fileThatIsNoChip_cannotHandOutLines(@TempDir Path dir) throws IOException {
        var chip = Files.createFile(dir.resolve("gpiochip0"));

        var ex = assertThrows(IOException.class,
                () -> new GpioOutputDriver(settings(Map.of("chip", chip.toString(), "light.line", "27"))));

        assertTrue(ex.getMessage().startsWith("Could not request line 27 of " + chip + " as an output"),
                ex.getMessage());
    }

    @Test
    void invalidLine_isRejected() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new GpioOutputDriver(settings(Map.of("amplifier.line", "GPIO17"))));

        assertTrue(ex.getMessage().contains("amplifier.line"), ex.getMessage());
    }

    @Test
    void withoutLines_nothingIsRequested() throws Exception {
        try (var driver = new GpioOutputDriver(settings(Map.of("chip", "/no/such/chip")))) {
            driver.set(net.pkhapps.idispatch.runboard.output.Output.LIGHT, true);
        }
    }

    private static OutputSettings settings(Map<String, String> values) {
        return name -> Optional.ofNullable(values.get(name));
    }
}
