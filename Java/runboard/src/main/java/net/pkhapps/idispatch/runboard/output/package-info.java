/**
 * Service provider interface for the drivers that switch the runboard's {@linkplain
 * net.pkhapps.idispatch.runboard.output.Output outputs}: the amplifier and the flashing light.
 * <p>
 * A driver is plugged in by implementing {@link net.pkhapps.idispatch.runboard.output.OutputDriverProvider} and
 * listing the implementation in {@code META-INF/services/net.pkhapps.idispatch.runboard.output.OutputDriverProvider},
 * so that {@link java.util.ServiceLoader} finds it. The runboard uses the provider whose
 * {@linkplain net.pkhapps.idispatch.runboard.output.OutputDriverProvider#name() name} is set by
 * {@code idispatch.output.driver}. See {@code runboard/docs/outputs.md}.
 */
package net.pkhapps.idispatch.runboard.output;
