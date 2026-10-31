package net.pkhapps.idispatch.runboard.output.gpio;

import net.pkhapps.idispatch.runboard.output.OutputDriver;
import net.pkhapps.idispatch.runboard.output.OutputDriverProvider;
import net.pkhapps.idispatch.runboard.output.OutputSettings;

/**
 * Provides the {@code gpio} driver, which switches each output through a GPIO line of a Linux machine, such as a pin on
 * a Raspberry Pi's header.
 * <p>
 * Settings: {@code chip} is the GPIO chip's device (default {@code /dev/gpiochip0}). For each output,
 * {@code <output>.line} is its line on the chip, and {@code <output>.activeLow} whether the line is low when the output
 * is on (default {@code false}). An output without a line is not switched.
 */
public class GpioOutputDriverProvider implements OutputDriverProvider {

    @Override
    public String name() {
        return "gpio";
    }

    @Override
    public OutputDriver create(OutputSettings settings) throws Exception {
        return new GpioOutputDriver(settings);
    }
}
