iDispatch Runboard
==================

Version ${project.version}

The runboard shows dispatch notifications from an iDispatch server on a screen at a station, plays an alarm, and
reads the notifications out loud.


Requirements
------------

- Java 25 or later, for example from https://adoptium.net. The launchers use JAVA_HOME if it is set, otherwise the
  java command on the PATH (on macOS, the newest installed Java 25 or later).
- Windows 10 or later on x64, macOS 12 or later, or Linux on x64 or arm64 (such as a Raspberry Pi 4).
  On Windows on ARM, the runboard only plays the alarm.
- A runboard key: the key of a destination in the iDispatch management console.


Installing
----------

1. Unzip this package. Installing a new version over an old one keeps runboard.properties and the voices.
2. Start the runboard once: runboard.cmd on Windows, runboard.command on macOS and Linux. It creates
   runboard.properties, opens it for editing, and stops.
3. In runboard.properties, set
     idispatch.server.url    wss://<server>/idispatch/ws/runboard
     idispatch.runboardKey   the destination's runboard key
4. To have notifications read out loud, run download-voices.cmd (Windows) or download-voices.command (macOS and
   Linux). They download the Finnish and Swedish voices, about 120 MB, to the voices folder. Without voices, the
   runboard only plays the alarm.
5. Start the runboard again.

The runboard starts in full screen. Set idispatch.undecorated=false in runboard.properties for a normal window.
It logs to the console window that the launcher runs in.

On macOS, a downloaded package may be blocked by Gatekeeper. Control-click the launcher and choose Open, or allow it
in System Settings > Privacy & Security. On Windows, choose More info > Run anyway if SmartScreen asks, or unblock the
zip in its Properties before unzipping.

Every setting is described in runboard.properties.template.


Licenses
--------

iDispatch is licensed under the GNU Affero General Public License, version 3 (licenses/LICENSE). Source code:
https://github.com/peholmst/idispatch-classic

The runboard bundles Piper text-to-speech (piper-jni, with piper1-gpl and espeak-ng), which is licensed under the
GNU General Public License, version 3 (licenses/GPL-3.0.txt). Section 13 of the AGPL-3.0 permits combining the two;
the GPL-3.0 still applies to those parts of the combined work.

licenses/THIRD-PARTY.txt lists every bundled library and its license. The license texts are in the licenses folder,
and licenses/notices has each library's NOTICE files and the licenses of code embedded in it.
The voices are not part of this package; check each voice's MODEL_CARD at
https://huggingface.co/rhasspy/piper-voices for its license.
