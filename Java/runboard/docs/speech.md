# Reading notifications out loud

When new notifications arrive, the runboard plays the alarm and then reads each notification twice. The speech comes
from a *speech engine*, plugged in through a small service provider interface (SPI) in
`net.pkhapps.idispatch.runboard.speech`. [Piper](https://github.com/OHF-Voice/piper1-gpl) is the only engine for now.

## How an alert plays

1. The audio thread asks the engine whether it supports the language of any of the new notifications
   (`SpeechEngine.supportsLanguage`).
2. The alarm starts. It plays `idispatch.alarm.playsBeforeSpeech` times (default 3) if speech is expected, otherwise
   `idispatch.alarm.plays` times (default 9).
3. While it plays, the engine synthesizes the text of each notification (`SpeechEngine.synthesize`). The text comes
   from `Announcement` and is the same for every engine.
4. When the alarm ends, each notification is read `idispatch.speech.repeats` times (default 2), each time after
   `idispatch.speech.pause` milliseconds of silence (default 1000). If no speech came out after all, the alarm
   plays the rest of its `idispatch.alarm.plays` times instead.

Errors from the engine are logged and never stop the alarm. An engine that can't be found or created means alarm only.

## Configuration

The language comes from the server, so that it matches the language that assignment types and municipalities are
entered in:

```yaml
idispatch:
  runboard:
    language: fi   # ISO 639-1 code, default fi
```

Servers from before that setting send no language. For those, the runboard can fall back to a language of its own:

```properties
# Leave empty (the default) to only play the alarm for notifications without a language.
idispatch.speech.defaultLanguage=fi
```

The language also picks the words added around the notification's text ("Tehtävä"/"Uppdrag" and
"Yksiköt"/"Enheter").

The runboard picks the engine in `runboard.properties` (or as system properties):

```properties
# The engine's name, or 'none' to only play the alarm. Default: piper
idispatch.speech.engine=piper

# Timings, with their defaults. Invalid values are logged and replaced by the default.
idispatch.alarm.plays=9
idispatch.alarm.playsBeforeSpeech=3
idispatch.speech.repeats=2
idispatch.speech.pause=1000
```

Each engine reads its own settings from properties starting with `idispatch.speech.<engine name>.`. The engine sees
them without the prefix: Piper's `voice.fi` setting is the `idispatch.speech.piper.voice.fi` property.

### Piper

```properties
idispatch.speech.piper.voice.fi=voices/fi_FI-harri-medium.onnx
idispatch.speech.piper.voice.sv=voices/sv_SE-nst-medium.onnx
```

Each value is the path of a voice's `.onnx` file, with its `.onnx.json` file next to it. Voices are downloaded from
[rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices); check each voice's `MODEL_CARD` for its license.
Piper is used through [piper-jni](https://github.com/GiviMAD/piper-jni), which bundles the native libraries for Linux
(x86_64, arm64), macOS and Windows. The native libraries load when the runboard starts, and each voice the first time
a notification in its language arrives.

## Trying it on a desktop

`runboard/run-local.sh` builds the runboard and starts it in a window, connected to any server:

1. Run it once. It creates `runboard/local/runboard.properties`, which isn't in Git, and stops.
2. Set `idispatch.server.url` and `idispatch.runboardKey` in that file. Use a destination of its own for testing:
   the runboard acknowledges what it receives, so sharing a real station's key can take notifications away from
   that station.
3. Run it again. It downloads the Finnish and Swedish voices to `runboard/local/voices` on the first run, then
   builds and starts the runboard. `idispatch.speech.defaultLanguage` is set to `fi`, so speech also works with
   servers that don't send a language yet.

## Plugging in a different engine

You might need one if Piper stops being maintained, or a better voice turns up elsewhere. Candidates:

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) (Apache-2.0, has a Java API, supports Raspberry Pi). It runs
  the same Piper `.onnx` voices, so the voice files can stay.
- espeak-ng on its own: robotic but tiny, with Finnish and Swedish. Good as an emergency voice.
- The Piper voices run directly with [onnxruntime](https://onnxruntime.ai/) for Java, with espeak-ng turning text
  into phonemes. That needs more code, but it is all ours.

### 1. Implement the engine

Put it in a package of its own, next to `speech/piper`:

```java
package net.pkhapps.idispatch.runboard.speech.example;

public class ExampleSpeechEngineProvider implements SpeechEngineProvider {

    @Override
    public String name() {
        return "example"; // selected with idispatch.speech.engine=example
    }

    @Override
    public SpeechEngine create(SpeechSettings settings) throws Exception {
        return new ExampleSpeechEngine(settings);
    }
}

class ExampleSpeechEngine implements SpeechEngine {

    private final SpeechSettings settings;

    ExampleSpeechEngine(SpeechSettings settings) throws Exception {
        this.settings = settings;
        // Load native libraries and other heavy things here.
    }

    @Override
    public boolean supportsLanguage(String language) {
        // idispatch.speech.example.voice.<language>
        return settings.get("voice." + language).isPresent();
    }

    @Override
    public Optional<SpeechAudio> synthesize(String language, String text) throws Exception {
        if (!supportsLanguage(language)) {
            return Optional.empty();
        }
        short[] samples = ...; // 16-bit mono PCM from the engine
        return Optional.of(SpeechAudio.ofMono16Bit(samples, sampleRate));
    }

    @Override
    public void close() {
        // Release native resources.
    }
}
```

The contract, as documented on the interfaces:

| Method | Called | Must |
|---|---|---|
| `SpeechEngineProvider()` | When the runboard starts, by `ServiceLoader` | Be public with no arguments, and do nothing heavy |
| `name()` | When the runboard starts | Return a unique name, lowercase by convention |
| `create(settings)` | Once, on the audio thread, when the runboard starts | Load everything that is slow to load. Throw if the engine can't run here. |
| `supportsLanguage(language)` | Before every alert, before the alarm starts | Answer quickly from the configuration, without loading anything, since it delays the alarm |
| `synthesize(language, text)` | While the alarm plays | Return empty for an unsupported language, throw on failure |
| `close()` | When the engine is released | Free native resources |

- **Threads:** an engine is only used from the audio thread, so it doesn't need to be thread safe.
- **Languages:** ISO 639-1 codes (`fi`, `sv`). The code is empty if the server didn't send one.
- **Audio:** `SpeechAudio` can be any format that `javax.sound.sampled` can play. `ofMono16Bit` covers the usual
  case of signed 16-bit mono samples.
- **Synthesis time:** must fit within the alarm, about 6 seconds with the default `idispatch.alarm.playsBeforeSpeech`
  (each play is 2 seconds), or the speech starts late. Measure it on the target
  hardware: a Raspberry Pi 4.

### 2. Register it

Add the provider's class name as a line in
`src/main/resources/META-INF/services/net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider`:

```
net.pkhapps.idispatch.runboard.speech.piper.PiperSpeechEngineProvider
net.pkhapps.idispatch.runboard.speech.example.ExampleSpeechEngineProvider
```

Add the engine's dependencies to `runboard/pom.xml`.

An engine can also live in a jar of its own, with its own `META-INF/services` file, as long as that jar is on the
runboard's classpath. If it is bundled into the fat jar instead, note that the `jar-with-dependencies` assembly
keeps only one copy of each `META-INF/services` file. In that case, configure the `metaInf-services` container
descriptor handler in the assembly so that the files are merged.

### 3. Configure and test it

Select it with `idispatch.speech.engine=example` and put its settings under `idispatch.speech.example.`. At startup
the log says `Using speech engine 'example'`. If the name is wrong, it lists the engines that were found.

Unit tests can test the engine directly. `SpeechTest` shows how the runboard's handling of engines is tested with
fake engines registered in `src/test/resources/META-INF/services`. Then try it on the real hardware with real
notifications in both languages, and listen.

### Removing an engine

To remove Piper, for example once another engine has replaced it:
1. Delete `speech/piper` and its line in the `META-INF/services` file.
2. Remove the `piper-jni` dependency.
3. Change the default of `idispatch.speech.engine` in `Configuration.getSpeechEngine()`.

piper-jni bundles GPL-3.0 code (piper1-gpl and espeak-ng). Check the license of any engine and voice before bundling
it; the project itself is AGPL-3.0.
