package net.pkhapps.idispatch.runboard.output.gpio;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.*;

/**
 * One GPIO line requested as an output from the Linux GPIO character device, through the GPIO v2 uAPI of
 * {@code linux/gpio.h}, called with the Foreign Function and Memory API. The line stays requested, and nothing else can
 * switch it, until it is closed or the process exits. Needs read and write access to the chip, such as membership of
 * the {@code gpio} group on Raspberry Pi OS.
 */
final class GpioLine implements AutoCloseable {

    // From linux/gpio.h
    private static final long GPIO_V2_GET_LINE_IOCTL = 0xC250B407L; // _IOWR(0xB4, 0x07, struct gpio_v2_line_request)
    private static final long GPIO_V2_LINE_SET_VALUES_IOCTL = 0xC010B40FL; // _IOWR(0xB4, 0x0F, struct gpio_v2_line_values)
    private static final long GPIO_V2_LINE_FLAG_ACTIVE_LOW = 1L << 1;
    private static final long GPIO_V2_LINE_FLAG_OUTPUT = 1L << 3;

    // Offsets in struct gpio_v2_line_request, which is 592 bytes. The kernel requires every other field to be zero.
    private static final long LINE_REQUEST_SIZE = 592;
    private static final long LINE_REQUEST_OFFSETS = 0; // __u32 offsets[64]
    private static final long LINE_REQUEST_CONSUMER = 256; // char consumer[32]
    private static final long LINE_REQUEST_CONFIG_FLAGS = 288; // struct gpio_v2_line_config config, starting with flags
    private static final long LINE_REQUEST_NUM_LINES = 560;
    private static final long LINE_REQUEST_FD = 588;
    private static final int CONSUMER_MAX_LENGTH = 31;

    // From fcntl.h, the same on Linux arm64 and x64
    private static final int O_RDWR = 2;
    private static final int O_CLOEXEC = 0x80000;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final StructLayout CALL_STATE = Linker.Option.captureStateLayout();
    private static final VarHandle ERRNO = CALL_STATE.varHandle(MemoryLayout.PathElement.groupElement("errno"));
    private static final MethodHandle OPEN = downcall("open",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT), Linker.Option.firstVariadicArg(2));
    private static final MethodHandle IOCTL = downcall("ioctl",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS), Linker.Option.firstVariadicArg(2));
    private static final MethodHandle CLOSE = downcall("close", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MethodHandle STRERROR = LINKER.downcallHandle(
            LINKER.defaultLookup().find("strerror").orElseThrow(), FunctionDescriptor.of(ADDRESS, JAVA_INT));

    private final String name;
    private int fd;

    private GpioLine(String name, int fd) {
        this.name = name;
        this.fd = fd;
    }

    /**
     * Requests a line of a chip as an output, switched off.
     *
     * @param chip      the chip's device, such as {@code /dev/gpiochip0}
     * @param offset    the line's number on the chip; on a Raspberry Pi, the GPIO number (BCM numbering), not the
     *                  header's pin number
     * @param activeLow whether the line is low when on, as some relay boards need
     * @param consumer  the name that the line is shown as used by, such as in {@code gpioinfo}
     */
    static GpioLine requestOutput(Path chip, int offset, boolean activeLow, String consumer) throws IOException {
        final String name = "line " + offset + " of " + chip;
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment state = arena.allocate(CALL_STATE);
            final int chipFd = (int) OPEN.invokeExact(state, arena.allocateFrom(chip.toString()), O_RDWR | O_CLOEXEC, 0);
            if (chipFd < 0) {
                throw error("Could not open " + chip, state);
            }
            try {
                final MemorySegment request = arena.allocate(LINE_REQUEST_SIZE, 8);
                request.set(JAVA_INT, LINE_REQUEST_OFFSETS, offset);
                final String truncatedConsumer = consumer.length() > CONSUMER_MAX_LENGTH
                        ? consumer.substring(0, CONSUMER_MAX_LENGTH) : consumer;
                request.setString(LINE_REQUEST_CONSUMER, truncatedConsumer);
                request.set(JAVA_LONG, LINE_REQUEST_CONFIG_FLAGS,
                        GPIO_V2_LINE_FLAG_OUTPUT | (activeLow ? GPIO_V2_LINE_FLAG_ACTIVE_LOW : 0));
                request.set(JAVA_INT, LINE_REQUEST_NUM_LINES, 1);
                if ((int) IOCTL.invokeExact(state, chipFd, GPIO_V2_GET_LINE_IOCTL, request) < 0) {
                    throw error("Could not request " + name + " as an output", state);
                }
                // The line's own file descriptor stays valid after the chip's is closed.
                return new GpioLine(name, request.get(JAVA_INT, LINE_REQUEST_FD));
            } finally {
                final int ignored = (int) CLOSE.invokeExact(state, chipFd);
            }
        } catch (IOException | RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IOException("Could not request " + name, ex);
        }
    }

    /**
     * Switches the line on (high, or low if it is active low) or off.
     */
    synchronized void set(boolean on) throws IOException {
        if (fd < 0) {
            throw new IOException(name + " is closed");
        }
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment state = arena.allocate(CALL_STATE);
            // struct gpio_v2_line_values { __aligned_u64 bits; __aligned_u64 mask; }
            final MemorySegment values = arena.allocate(16, 8);
            values.set(JAVA_LONG, 0, on ? 1 : 0);
            values.set(JAVA_LONG, 8, 1);
            if ((int) IOCTL.invokeExact(state, fd, GPIO_V2_LINE_SET_VALUES_IOCTL, values) < 0) {
                throw error("Could not switch " + name + (on ? " on" : " off"), state);
            }
        } catch (IOException | RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IOException("Could not switch " + name, ex);
        }
    }

    /**
     * Releases the line.
     */
    @Override
    public synchronized void close() {
        if (fd < 0) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            final int ignored = (int) CLOSE.invokeExact(arena.allocate(CALL_STATE), fd);
        } catch (Throwable ex) {
            // Nothing more can be done; the line is released when the process exits.
        } finally {
            fd = -1;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static MethodHandle downcall(String function, FunctionDescriptor descriptor, Linker.Option... options) {
        final Linker.Option[] withErrno = java.util.Arrays.copyOf(options, options.length + 1);
        withErrno[options.length] = Linker.Option.captureCallState("errno");
        return LINKER.downcallHandle(LINKER.defaultLookup().find(function).orElseThrow(), descriptor, withErrno);
    }

    private static IOException error(String message, MemorySegment state) throws Throwable {
        final int errno = (int) ERRNO.get(state, 0L);
        final MemorySegment description = (MemorySegment) STRERROR.invokeExact(errno);
        return new IOException(message + ": " + description.reinterpret(256).getString(0) + " (errno " + errno + ")");
    }
}
