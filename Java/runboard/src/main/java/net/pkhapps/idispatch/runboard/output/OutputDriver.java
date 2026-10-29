package net.pkhapps.idispatch.runboard.output;

/**
 * Switches the {@linkplain Output outputs} on and off. The runboard calls a driver one call at a time, so it does not
 * have to be thread safe.
 */
public interface OutputDriver extends AutoCloseable {

    /**
     * Switches the output on or off. Called only when the output changes, and once for each output, off, when the
     * runboard starts. Must return quickly. An output that the driver has not been set up for is left alone.
     *
     * @throws Exception if the output cannot be switched; the runboard logs it and carries on, trying again on the
     *                   next change
     */
    void set(Output output, boolean on) throws Exception;

    /**
     * Releases the driver's resources. Called once when the runboard exits, from a shutdown hook, after every output
     * has been switched off. The default does nothing.
     */
    @Override
    default void close() throws Exception {
    }
}
