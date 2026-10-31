# Switching the amplifier and the flashing light

The runboard switches two outputs:

- **The amplifier** (and loudspeaker) is switched on before the alarm or the error beep starts, and off when the last
  sound has played. After switching it on, the runboard waits `idispatch.output.amplifierWarmUp` milliseconds before
  playing, so that the start isn't lost.
- **The flashing light** is on while notifications are shown on the screen. It goes off when the last one leaves the
  screen, `idispatch.notification.shownFor` seconds (default 180) after it was sent.

Both are off when the runboard starts and when it exits. The outputs are switched by an *output driver*, plugged in
through a small service provider interface (SPI) in `net.pkhapps.idispatch.runboard.output`, in the same way as the
[speech engines](speech.md). Errors from the driver are logged and never stop the alarm or the speech. A driver that
can't be found or created means nothing is switched.

## Configuration

```properties
# 'none' (the default) switches nothing, as on a desktop. 'gpio' switches GPIO lines, as on a Raspberry Pi.
idispatch.output.driver=none

# How long to wait after switching the amplifier on before playing sound (in milliseconds). Default: 500
idispatch.output.amplifierWarmUp=500
```

Each driver reads its own settings from properties starting with `idispatch.output.<driver name>.`, and each output's
settings start with its name, `amplifier` or `light`.

### GPIO

The `gpio` driver switches each output through a line of a Linux GPIO chip:

```properties
idispatch.output.driver=gpio
# The chip's device. Default: /dev/gpiochip0, where a Raspberry Pi 4 or 5 has the header's GPIOs.
idispatch.output.gpio.chip=/dev/gpiochip0
# The line of each output: on a Raspberry Pi, the GPIO number, not the header pin number.
idispatch.output.gpio.amplifier.line=17
idispatch.output.gpio.light.line=27
# Whether the line is low when the output is on, as some relay boards need. Default: false
idispatch.output.gpio.amplifier.activeLow=false
idispatch.output.gpio.light.activeLow=false
```

An output without a line isn't switched, so an installation with only a light leaves out `amplifier.line`.

The driver uses the GPIO character device (the GPIO v2 uAPI of `linux/gpio.h`) through Java's Foreign Function and
Memory API, so it needs no native libraries of its own and no root: the user must only be able to read and write the
chip, as members of the `gpio` group can on Raspberry Pi OS. It works on any Linux machine with GPIO, not only a Pi.
The runboard holds the lines for as long as it runs, so `gpioinfo` (package `gpiod`) shows them used by
`idispatch-runboard`, and nothing else can switch them meanwhile. [raspberry-pi.md](raspberry-pi.md) shows how to
wire and test the outputs.

## Plugging in a different driver

For example for a USB relay board, or for outputs on another machine. Implement `OutputDriverProvider` and
`OutputDriver` in a package of their own, next to `output/gpio`, and list the provider in
`src/main/resources/META-INF/services/net.pkhapps.idispatch.runboard.output.OutputDriverProvider`:

| Method | Called | Must |
|---|---|---|
| `OutputDriverProvider()` | When the runboard starts, by `ServiceLoader` | Be public with no arguments, and do nothing heavy |
| `name()` | When the runboard starts | Return a unique name, lowercase by convention |
| `create(settings)` | Once, when the runboard starts | Claim the hardware. Throw if the driver can't run here. |
| `set(output, on)` | When an output changes, one call at a time, and once for each output (off) at startup | Return quickly. Leave outputs it hasn't been set up for alone. |
| `close()` | Once, when the runboard exits, after every output has been switched off | Release the hardware |
