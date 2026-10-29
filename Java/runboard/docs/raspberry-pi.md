# Running the runboard on a Raspberry Pi

The runboard runs on a Raspberry Pi 4 or 5 with 2 GB of memory or more, and on other small Linux machines. It needs
a 64-bit (arm64 or x64) Linux: Piper's native libraries exist only for those, so a 32-bit system only plays the
alarm. These instructions are for Raspberry Pi OS (Debian based). On another Debian or Ubuntu machine, the same
steps apply; skip `raspi-config`.

There are two ways to set it up:

- **Raspberry Pi OS Lite and cage (recommended).** No desktop: the Pi boots straight into the runboard, full screen,
  through [cage](https://github.com/cage-kiosk/cage), a Wayland compositor that shows one application. This leaves
  the most memory and CPU for Piper.
- **Raspberry Pi OS with desktop.** Easier to set up and maintain with a keyboard and mouse, and the runboard starts
  when the desktop does. Costs a few hundred MB of memory and some CPU.

Piper uses every core for a few seconds while the alarm plays. Give the Pi a heat sink or a fan, or it slows down when
it gets hot, and the speech can start late.

## 1. Install the operating system

Write Raspberry Pi OS (64-bit), Lite or with desktop, to the SD card with
[Raspberry Pi Imager](https://www.raspberrypi.com/software/). In Imager's settings, set the user name and password,
the network, and enable SSH, so that the rest can be done over SSH.

## 2. Install Java 25 and the packages the runboard needs

Raspberry Pi OS doesn't have Java 25, so install it from [Adoptium](https://adoptium.net/installation/linux/):

```bash
sudo apt install -y wget gpg
sudo mkdir -p /etc/apt/keyrings
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/keyrings/adoptium.asc >/dev/null
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
    | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-25-jre unzip curl fontconfig fonts-dejavu-core
java -version
```

Without fonts (`fonts-dejavu-core`), Java can't draw text and the runboard fails to start. On Lite, also install
cage, and Xwayland, which runs the runboard's Swing window under it:

```bash
sudo apt install -y cage xwayland
```

## 3. Install the runboard

Copy the package (`iDispatchRunboard-<version>.zip`, see the README in it) to the Pi, for example with
`scp iDispatchRunboard-3.0.0.zip <user>@<pi>:`, and then on the Pi:

```bash
unzip iDispatchRunboard-3.0.0.zip     # creates ~/iDispatchRunboard
cd iDispatchRunboard
./runboard.command                    # creates runboard.properties and stops
nano runboard.properties              # set idispatch.server.url and idispatch.runboardKey
./download-voices.command             # about 120 MB
```

`runboard.properties.template` describes every setting. On a small screen, set `idispatch.lowres=true` if the text
doesn't fit. To update, unzip a new version over the old one; that keeps `runboard.properties` and the voices.

## 4. Set up the sound

Java plays through ALSA's default device. A Pi 4 has an analog output (the 3.5 mm jack, `Headphones`) and one for each
HDMI port; USB sound cards show up as cards of their own. List them and test each one:

```bash
aplay -l
speaker-test -D plughw:CARD=Headphones -c 2 -t wav -l 1
```

On the desktop, pick the output from the speaker icon in the taskbar. On Lite, make the card the default in
`~/.asoundrc`, with the card name from `aplay -l`:

```
defaults.pcm.card Headphones
defaults.ctl.card Headphones
```

Then `speaker-test -c 2 -t wav -l 1` should play through it. Set the volume with `alsamixer`. The user must be in the
`audio` group, as the user created by Imager is.

## 5. Connect the amplifier and the flashing light

The runboard can switch the amplifier on only while it plays sound, and a flashing light on while notifications are
shown, each through a GPIO pin. Skip this if the amplifier is always on and there is no light.

A GPIO pin gives 3.3 V and a few milliamps, enough for the input of a relay or a transistor board, never for the
amplifier or the light themselves. Wire each board's input to a GPIO and a ground pin. Use GPIO numbers in the
settings, not header pin numbers: GPIO 17 is pin 11 and GPIO 27 is pin 13 ([pinout.xyz](https://pinout.xyz) shows
the header).

Test the wiring before starting the runboard, with the runboard stopped. `pinctrl` comes with Raspberry Pi OS:

```bash
pinctrl set 17 op dh    # GPIO 17 high: the amplifier should come on
pinctrl set 17 op dl    # and go off
pinctrl set 27 op dh    # GPIO 27 high: the light should come on
pinctrl set 27 op dl
```

If a board switches on when its pin is low, it is *active low*. Then set its pins in `runboard.properties`:

```properties
idispatch.output.driver=gpio
idispatch.output.gpio.amplifier.line=17
idispatch.output.gpio.light.line=27
# For a board that switches on when its pin is low:
#idispatch.output.gpio.light.activeLow=true
# The time the amplifier needs after switching on before it plays sound, in milliseconds:
idispatch.output.amplifierWarmUp=500
```

The user must be in the `gpio` group, as the user created by Imager is. [outputs.md](outputs.md) has every setting.

## 6. Start the runboard when the Pi starts

### Lite: cage

Have the user log in automatically on the console:

```bash
sudo raspi-config nonint do_boot_behaviour B2
```

Add this to the end of `~/.profile`. On the first console only, it starts the runboard in cage, and restarts it
if it stops. The log goes to `~/iDispatchRunboard/runboard.log`; the previous run's log is kept in `runboard.log.1`.

```bash
if [[ -z ${WAYLAND_DISPLAY:-} && $(tty) == /dev/tty1 ]]; then
    while true; do
        mv -f ~/iDispatchRunboard/runboard.log ~/iDispatchRunboard/runboard.log.1 2>/dev/null
        cage -- ~/iDispatchRunboard/runboard.command > ~/iDispatchRunboard/runboard.log 2>&1
        sleep 5
    done
fi
```

Reboot with `sudo reboot`. Logging in over SSH doesn't start a second runboard. To work on the Pi's own console,
switch to another one with Ctrl+Alt+F2.

### Desktop

Turn off screen blanking with `sudo raspi-config` (Display Options > Screen Blanking), have the desktop log in
automatically (System Options > Boot / Auto Login > Desktop Autologin), and create
`~/.config/autostart/idispatch-runboard.desktop`:

```ini
[Desktop Entry]
Type=Application
Name=iDispatch Runboard
Exec=sh -c 'exec ~/iDispatchRunboard/runboard.command > ~/iDispatchRunboard/runboard.log 2>&1'
```

This works on any desktop that follows the XDG autostart specification, not just Raspberry Pi OS's.

## 7. Check it

Send a test notification to the runboard's destination and watch `runboard.log` (`tail -f
~/iDispatchRunboard/runboard.log` over SSH):

- `Using speech engine 'piper'` and `Initialized Piper` at startup. If Piper can't load, the runboard only plays the
  alarm, and the log says why.
- `Loaded voice ... in N ms` the first time a notification in a language arrives. Loading a voice adds to the first
  alert's synthesis time, so the first alert after a start is the slowest.
- The speech should start right after the alarm. If it starts late, increase `idispatch.alarm.playsBeforeSpeech` to
  give Piper more time (see [speech.md](speech.md)), and check that the Pi isn't throttling:
  `vcgencmd get_throttled` should print `throttled=0x0`.
- With the outputs connected: `Switching the amplifier through line 17 of /dev/gpiochip0` at startup, then
  `Switched the amplifier on` before the alarm and `off` after the speech, and the light on until the notification
  leaves the screen (three minutes after it was sent).
