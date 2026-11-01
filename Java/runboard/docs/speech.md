# Reading notifications out loud

When new notifications arrive, the runboard plays the alarm and then reads each notification twice. The speech comes
from a *speech engine*, plugged in through a small service provider interface (SPI) in
`net.pkhapps.idispatch.runboard.speech`. [Piper](https://github.com/OHF-Voice/piper1-gpl) is the only engine for now.

## How an alert plays

Two threads share the work: the *speech thread* runs the engine, and the *audio thread* plays the sound.

1. As soon as the notifications arrive, the speech thread starts synthesizing the text of each one
   (`SpeechEngine.synthesize`), one after the other, even if an earlier alert is still playing. The text comes from
   `Announcement` and is the same for every engine. It is made of phrases: each unit dispatched is one, and so are
   the assignment and the address. Each phrase is synthesized on its own, a part at a time (with Piper, a sentence at
   a time), and each part is handed to the audio thread as soon as it is ready. Between the phrases, the runboard adds
   `idispatch.speech.phrasePause` milliseconds of silence (default 500), so the units are not read in one breath,
   whatever the engine.
2. The audio thread asks the engine whether it supports the language of any of the new notifications
   (`SpeechEngine.supportsLanguage`).
3. The alarm starts. It plays `idispatch.alarm.playsBeforeSpeech` times (default 3) if speech is expected, otherwise
   `idispatch.alarm.plays` times (default 9).
4. When the alarm ends, each notification is read `idispatch.speech.repeats` times (default 2), each time after
   `idispatch.speech.pause` milliseconds of silence (default 1000). The first reading starts with the first part, while
   the rest is still being synthesized. Time spent waiting for the first part counts as part of the pause, so speech
   that is synthesized late starts as soon as it is ready, without a pause on top. If no speech came out after all,
   the alarm plays the rest of its `idispatch.alarm.plays` times instead.

When the runboard starts, the speech thread creates the engine and then *prepares* each language that it supports
(`SpeechEngine.prepare`): Piper loads the voice and synthesizes a few words, as the first synthesis with a voice is
slower than the ones after it. So the first alert after a start is as fast as the rest.

The amplifier is switched on before the alarm starts and off after the last reading (see [outputs.md](outputs.md)).
Errors from the engine are logged and never stop the alarm. An engine that can't be found or created means alarm only.

## Configuration

The language comes from the server: it is set for each runboard destination under Administration → Destinations
(Finnish or Swedish), and the server also sends the municipality name and assignment type description in that language
(or in the other one, if it has only been entered in one).

Servers from before that setting send no language. For those, the runboard can fall back to a language of its own:

```properties
# Leave empty (the default) to only play the alarm for notifications without a language.
idispatch.speech.defaultLanguage=fi
```

The language also picks the words added around the notification's text ("Hälytysilmoitus"/"Larmmeddelande" and
"Tehtävä"/"Uppdrag") and the radio words for urgencies.

The runboard picks the engine in `runboard.properties` (or as system properties):

```properties
# The engine's name, or 'none' to only play the alarm. Default: piper
idispatch.speech.engine=piper

# Timings, with their defaults. Invalid values are logged and replaced by the default.
idispatch.alarm.plays=9
idispatch.alarm.playsBeforeSpeech=3
idispatch.speech.repeats=2
idispatch.speech.pause=1000
idispatch.speech.phrasePause=500
```

### Volume

Speech is much quieter than the alarm: Piper's voices come out at about -27 LUFS (Finnish) and -21 LUFS (Swedish),
while the alarm, a tone with softened edges, is at about -7 LUFS. So the runboard sets the levels itself, in software:

- Every part of every engine's speech is normalized, amplified or attenuated so that its loudest sample is at -1 dBFS (`Volume`, at
  most +20 dB). The two voices then end up at about -20 LUFS, as loud as they can be without distorting.
- The alarm and the error beep are attenuated by `idispatch.alarm.volume` decibels when they are loaded. The default,
  -12, puts the alarm at about -19 LUFS, just above the speech. It can't be above 0.

```properties
idispatch.alarm.volume=-12
```

Set the overall volume with the system mixer and the amplifier so that the speech is heard well, then adjust
`idispatch.alarm.volume` if the alarm is too loud or quiet next to it.

### Engine settings

Each engine reads its own settings from properties starting with `idispatch.speech.<engine name>.`. The engine sees
them without the prefix: Piper's `voice.fi` setting is the `idispatch.speech.piper.voice.fi` property.

### Piper

```properties
idispatch.speech.piper.voice.fi=voices/fi_FI-harri-medium.onnx
idispatch.speech.piper.voice.sv=voices/sv_SE-lisa-medium.onnx
```

Each value is the path of a voice's `.onnx` file, with its `.onnx.json` file next to it. Voices are downloaded from
[rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices); check each voice's `MODEL_CARD` for its license.
Piper is used through [piper-jni](https://github.com/jvoice-project/piper-jni), which bundles the native libraries for Linux
(x86_64, arm64), macOS and Windows. The native libraries and the voices load when the runboard starts. The Swedish
voice is `lisa`, which sounds better than `nst`; an installation upgraded from an older version still has `nst` in its
`runboard.properties` and needs the line above, and the voice downloaded with the download script.

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

    // Optional: hand over each sentence as soon as it is ready, so that the reading starts sooner. Without this,
    // the whole text from the method above is handed over as one part.
    @Override
    public boolean synthesize(String language, String text, Consumer<SpeechAudio> parts) throws Exception {
        if (!supportsLanguage(language)) {
            return false;
        }
        for (short[] sentence : ...) { // the engine's output, a sentence at a time
            parts.accept(SpeechAudio.ofMono16Bit(sentence, sampleRate));
        }
        return true;
    }

    // Optional: load the voice and warm it up when the runboard starts, instead of on the first notification.
    @Override
    public void prepare(String language) throws Exception {
        ...
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
| `create(settings)` | Once, on the speech thread, when the runboard starts | Load the engine itself. Throw if the engine can't run here. The first alert waits for this. |
| `prepare(language)` | On the speech thread after `create`, once for each language notifications can come in that the engine supports | Optional. Load the language's voice and do whatever makes the first synthesis as fast as the rest. Throw on failure. |
| `supportsLanguage(language)` | On the audio thread before every alert, before the alarm starts, possibly while the speech thread is synthesizing | Answer quickly from the configuration, without loading anything or touching what synthesis uses |
| `synthesize(language, text, parts)` | On the speech thread, as soon as a notification arrives | Optional. Hand each part to `parts` as soon as it is ready, all in the same format. Return false for an unsupported language, throw on failure. By default calls `synthesize(language, text)`. |
| `synthesize(language, text)` | Through the method above, unless the engine overrides it | Return empty for an unsupported language, throw on failure |
| `close()` | Once, when the runboard exits, from a shutdown hook after the speech thread has stopped | Free native resources. Native libraries whose objects are still alive when the JVM exits can crash it: Piper's onnxruntime aborts with *mutex lock failed*. |

- **Threads:** an engine is used from the speech thread, one call at a time, so it doesn't need to be thread safe.
  Only `supportsLanguage` is called from the audio thread.
- **Languages:** ISO 639-1 codes (`fi`, `sv`). The code is empty if the server didn't send one.
- **Audio:** `SpeechAudio` can be any format that `javax.sound.sampled` can play. `ofMono16Bit` covers the usual
  case of signed 16-bit mono samples.
- **Synthesis time:** the first part must be ready within the alarm, about 6 seconds with the default
  `idispatch.alarm.playsBeforeSpeech` (each play is 2 seconds), or the speech starts late, and each following part
  before the one before it has been read, or there is a gap. Measure it on the target hardware, a Raspberry Pi 4: the
  log says `Synthesized N parts in X ms, the first in Y ms`, and `Speech started N ms late` when the first part wasn't
  ready in time.

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
