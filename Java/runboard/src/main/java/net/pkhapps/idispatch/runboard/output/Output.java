package net.pkhapps.idispatch.runboard.output;

/**
 * The devices the runboard switches on and off.
 */
public enum Output {

    /**
     * The amplifier and loudspeaker, on while the runboard plays sound.
     */
    AMPLIFIER("amplifier"),

    /**
     * The flashing light, on while notifications are shown.
     */
    LIGHT("light");

    private final String settingName;

    Output(String settingName) {
        this.settingName = settingName;
    }

    /**
     * Returns the name that the output's driver settings start with, such as {@code amplifier} for
     * {@code amplifier.line}.
     */
    public String settingName() {
        return settingName;
    }
}
