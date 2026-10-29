package net.pkhapps.idispatch.runboard.output;

/**
 * Creates an {@link OutputDriver}. Implementations are found with {@link java.util.ServiceLoader} and must have a
 * public no-argument constructor.
 */
public interface OutputDriverProvider {

    /**
     * Returns the name that selects this driver in {@code idispatch.output.driver}, such as {@code gpio}. Its own
     * settings are read from properties starting with {@code idispatch.output.<name>.}.
     */
    String name();

    /**
     * Creates the driver, claiming the hardware it switches. Called once when the runboard starts. If this throws,
     * the runboard logs the error and runs without switching anything.
     *
     * @param settings the driver's own settings
     */
    OutputDriver create(OutputSettings settings) throws Exception;
}
