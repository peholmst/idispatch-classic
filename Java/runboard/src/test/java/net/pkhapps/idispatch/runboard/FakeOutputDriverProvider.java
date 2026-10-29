package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.output.Output;
import net.pkhapps.idispatch.runboard.output.OutputDriver;
import net.pkhapps.idispatch.runboard.output.OutputDriverProvider;
import net.pkhapps.idispatch.runboard.output.OutputSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code fake} driver records every switch in {@link #SWITCHES}, as {@code amplifier on} and the like, and
 * {@code closed} when it is closed. It fails to switch the outputs listed in its {@code broken} setting.
 */
public class FakeOutputDriverProvider implements OutputDriverProvider {

    static final List<String> SWITCHES = new ArrayList<>();

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public OutputDriver create(OutputSettings settings) {
        final List<String> broken = settings.get("broken").map(value -> List.of(value.split(","))).orElse(List.of());
        return new OutputDriver() {
            @Override
            public void set(Output output, boolean on) {
                if (broken.contains(output.settingName())) {
                    throw new IllegalStateException("Line busy");
                }
                SWITCHES.add(output.settingName() + (on ? " on" : " off"));
            }

            @Override
            public void close() {
                SWITCHES.add("closed");
            }
        };
    }

    /**
     * The {@code failing} driver cannot be created.
     */
    public static class Failing implements OutputDriverProvider {

        @Override
        public String name() {
            return "failing";
        }

        @Override
        public OutputDriver create(OutputSettings settings) throws Exception {
            throw new Exception("No GPIO chip");
        }
    }
}
