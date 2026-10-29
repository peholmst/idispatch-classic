package net.pkhapps.idispatch.runboard.speech;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Speech as PCM samples in the given format, ready to be written to a {@link javax.sound.sampled.SourceDataLine}.
 */
public record SpeechAudio(byte[] samples, AudioFormat format) {

    /**
     * Creates speech from signed 16-bit mono samples, the format most engines produce.
     */
    public static SpeechAudio ofMono16Bit(short[] samples, float sampleRate) {
        final ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(samples);
        return new SpeechAudio(buffer.array(), new AudioFormat(sampleRate, 16, 1, true, false));
    }
}
