package net.pkhapps.idispatch.runboard.output.gpio;

import net.pkhapps.idispatch.runboard.output.Output;
import net.pkhapps.idispatch.runboard.output.OutputDriver;
import net.pkhapps.idispatch.runboard.output.OutputSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Switches each output through a GPIO line of its own. See {@link GpioOutputDriverProvider} for the settings.
 */
class GpioOutputDriver implements OutputDriver {

    private static final String CONSUMER = "idispatch-runboard";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Output, GpioLine> lines = new EnumMap<>(Output.class);

    GpioOutputDriver(OutputSettings settings) throws IOException {
        final Path chip = Path.of(settings.get("chip").orElse("/dev/gpiochip0"));
        try {
            for (Output output : Output.values()) {
                final Optional<String> line = settings.get(output.settingName() + ".line");
                if (line.isEmpty()) {
                    logger.warn("No GPIO line set for the {} ({}.line), it will not be switched", output.settingName(),
                            output.settingName());
                    continue;
                }
                final boolean activeLow = settings.get(output.settingName() + ".activeLow")
                        .map(Boolean::parseBoolean)
                        .orElse(false);
                final GpioLine gpioLine = GpioLine.requestOutput(chip, parseLine(output, line.get()), activeLow,
                        CONSUMER);
                lines.put(output, gpioLine);
                logger.info("Switching the {} through {}{}", output.settingName(), gpioLine,
                        activeLow ? ", active low" : "");
            }
        } catch (IOException | RuntimeException ex) {
            close();
            throw ex;
        }
    }

    private static int parseLine(Output output, String value) {
        try {
            final int line = Integer.parseInt(value);
            if (line >= 0) {
                return line;
            }
        } catch (NumberFormatException ignored) {
            // Falls through to the exception below
        }
        throw new IllegalArgumentException("Invalid GPIO line '" + value + "' for the " + output.settingName()
                + " (" + output.settingName() + ".line), must be a whole number of at least 0");
    }

    @Override
    public void set(Output output, boolean on) throws IOException {
        final GpioLine line = lines.get(output);
        if (line != null) {
            line.set(on);
        }
    }

    @Override
    public void close() {
        lines.values().forEach(GpioLine::close);
        lines.clear();
    }
}
