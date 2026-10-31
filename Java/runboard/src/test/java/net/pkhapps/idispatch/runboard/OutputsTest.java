package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.output.OutputSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OutputsTest {

    private static final OutputSettings NO_SETTINGS = name -> Optional.empty();

    @BeforeEach
    void clearSwitches() {
        FakeOutputDriverProvider.SWITCHES.clear();
    }

    @Test
    void configuredDriver_switchesEveryOutputOffAtStart() {
        initialized("fake", NO_SETTINGS);

        assertEquals(List.of("amplifier off", "light off"), FakeOutputDriverProvider.SWITCHES);
    }

    @Test
    void outputs_areSwitchedOnlyWhenTheyChange() throws InterruptedException {
        var outputs = initialized("fake", NO_SETTINGS);
        FakeOutputDriverProvider.SWITCHES.clear();

        outputs.setLight(true);
        outputs.setLight(true);
        outputs.switchOnAmplifier();
        outputs.switchOnAmplifier();
        outputs.switchOffAmplifier();
        outputs.setLight(false);

        assertEquals(List.of("light on", "amplifier on", "amplifier off", "light off"),
                FakeOutputDriverProvider.SWITCHES);
    }

    @Test
    void switchingOnTheAmplifier_waitsForItToWarmUp() throws InterruptedException {
        var outputs = new Outputs("fake", NO_SETTINGS, Duration.ofMillis(200));
        outputs.initialize();

        var start = System.nanoTime();
        outputs.switchOnAmplifier();
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofMillis(200)) >= 0);

        start = System.nanoTime();
        outputs.switchOnAmplifier();
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofMillis(100)) < 0,
                "No warm-up when the amplifier already is on");
    }

    @Test
    void withoutDriver_theAmplifierDoesNotWarmUp() throws InterruptedException {
        var outputs = new Outputs(Outputs.NONE, NO_SETTINGS, Duration.ofSeconds(10));
        outputs.initialize();

        var start = System.nanoTime();
        outputs.switchOnAmplifier();
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(1)) < 0);
    }

    @Test
    void close_switchesEverythingOffAndClosesTheDriver() throws InterruptedException {
        var outputs = initialized("fake", NO_SETTINGS);
        outputs.switchOnAmplifier();
        outputs.setLight(true);
        FakeOutputDriverProvider.SWITCHES.clear();

        outputs.close();
        outputs.close();
        outputs.setLight(true);

        assertEquals(List.of("amplifier off", "light off", "closed"), FakeOutputDriverProvider.SWITCHES);
    }

    @Test
    void brokenOutput_doesNotStopTheOthers() throws InterruptedException {
        var outputs = initialized("fake", name -> Optional.ofNullable(Map.of("broken", "light").get(name)));
        FakeOutputDriverProvider.SWITCHES.clear();

        outputs.setLight(true);
        outputs.switchOnAmplifier();

        assertEquals(List.of("amplifier on"), FakeOutputDriverProvider.SWITCHES);
    }

    @Test
    void driverNone_unknownDriverAndDriverThatCannotBeCreated_switchNothing() throws InterruptedException {
        for (String driver : List.of(Outputs.NONE, "missing", "failing")) {
            var outputs = initialized(driver, NO_SETTINGS);
            outputs.switchOnAmplifier();
            outputs.setLight(true);
            outputs.close();
        }

        assertTrue(FakeOutputDriverProvider.SWITCHES.isEmpty());
    }

    private static Outputs initialized(String driverName, OutputSettings settings) {
        var outputs = new Outputs(driverName, settings, Duration.ZERO);
        outputs.initialize();
        return outputs;
    }
}
