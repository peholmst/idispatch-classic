package net.pkhapps.idispatch.runboard.output;

import java.util.Optional;

/**
 * The settings of one output driver, read from properties starting with {@code idispatch.output.<driver name>.} in
 * {@code runboard.properties} or the system properties.
 */
@FunctionalInterface
public interface OutputSettings {

    /**
     * Returns the value of a setting, or an empty optional if it is missing or blank.
     *
     * @param name the setting's name without the driver prefix, such as {@code amplifier.line} for
     *             {@code idispatch.output.gpio.amplifier.line}
     */
    Optional<String> get(String name);
}
