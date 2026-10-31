package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.output.Output;
import net.pkhapps.idispatch.runboard.output.OutputDriver;
import net.pkhapps.idispatch.runboard.output.OutputDriverProvider;
import net.pkhapps.idispatch.runboard.output.OutputSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Switches the amplifier and the flashing light with the {@link OutputDriver} selected by
 * {@code idispatch.output.driver}, found with {@link ServiceLoader}. With the default, {@link #NONE}, nothing is
 * switched, as on a desktop. Errors from the driver are logged and never reach the caller, so that a broken output only
 * costs that output, not the alarm. Thread safe; start with {@link #initialize()} and end with {@link #close()}.
 */
class Outputs {

    /**
     * The driver name that switches nothing.
     */
    static final String NONE = "none";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String driverName;
    private final OutputSettings settings;
    private final Duration amplifierWarmUp;
    private final Map<Output, Boolean> states = new EnumMap<>(Output.class);
    private OutputDriver driver;

    Outputs(Configuration configuration) {
        this(configuration.getOutputDriver(), configuration.getOutputSettings(), configuration.getAmplifierWarmUp());
    }

    /**
     * @param amplifierWarmUp how long to wait after switching the amplifier on before playing sound
     */
    Outputs(String driverName, OutputSettings settings, Duration amplifierWarmUp) {
        this.driverName = driverName;
        this.settings = settings;
        this.amplifierWarmUp = amplifierWarmUp;
    }

    /**
     * Creates the driver and switches every output off. If the driver cannot be found or created, nothing is switched.
     */
    synchronized void initialize() {
        if (NONE.equals(driverName)) {
            logger.info("No output driver, the amplifier and the flashing light will not be switched");
            return;
        }
        try {
            final List<OutputDriverProvider> providers = ServiceLoader.load(OutputDriverProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();
            final Optional<OutputDriverProvider> provider = providers.stream()
                    .filter(p -> p.name().equals(driverName))
                    .findFirst();
            if (provider.isEmpty()) {
                logger.error("No output driver named '{}', the amplifier and the flashing light will not be switched. "
                        + "Available: {}", driverName, providers.stream().map(OutputDriverProvider::name).toList());
                return;
            }
            driver = provider.get().create(settings);
            logger.info("Using output driver '{}'", driverName);
        } catch (Exception | ServiceConfigurationError | LinkageError ex) {
            logger.error("Could not create output driver '{}', the amplifier and the flashing light will not be "
                    + "switched", driverName, ex);
            return;
        }
        for (Output output : Output.values()) {
            set(output, false);
        }
    }

    /**
     * Switches every output off and releases the driver. Nothing is switched after this.
     */
    synchronized void close() {
        if (driver == null) {
            return;
        }
        for (Output output : Output.values()) {
            set(output, false);
        }
        try {
            driver.close();
            logger.info("Closed output driver '{}'", driverName);
        } catch (Exception | LinkageError ex) {
            logger.error("Could not close output driver '{}'", driverName, ex);
        } finally {
            driver = null;
        }
    }

    /**
     * Switches the amplifier on and, if it was off, waits for it to warm up, so that the sound played next is heard
     * from the start.
     */
    void switchOnAmplifier() throws InterruptedException {
        if (set(Output.AMPLIFIER, true) && amplifierWarmUp.isPositive()) {
            Thread.sleep(amplifierWarmUp);
        }
    }

    void switchOffAmplifier() {
        set(Output.AMPLIFIER, false);
    }

    void setLight(boolean on) {
        set(Output.LIGHT, on);
    }

    /**
     * Returns whether the output was switched: false if it already was in that state, or could not be switched.
     */
    private synchronized boolean set(Output output, boolean on) {
        if (driver == null || Boolean.valueOf(on).equals(states.get(output))) {
            return false;
        }
        try {
            driver.set(output, on);
            states.put(output, on);
            logger.info("Switched the {} {}", output.settingName(), on ? "on" : "off");
            return true;
        } catch (Exception | LinkageError ex) {
            logger.error("Output driver '{}' could not switch the {} {}", driverName, output.settingName(),
                    on ? "on" : "off", ex);
            return false;
        }
    }
}
