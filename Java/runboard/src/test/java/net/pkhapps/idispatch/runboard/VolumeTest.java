package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VolumeTest {

    private static final AudioFormat LITTLE_ENDIAN = new AudioFormat(22050, 16, 1, true, false);
    private static final AudioFormat BIG_ENDIAN = new AudioFormat(22050, 16, 1, true, true);

    @Test
    void amplify_minusSixDecibelsHalvesTheSamples() {
        final byte[] result = Volume.amplify(bytes(LITTLE_ENDIAN, 10000, -20000), LITTLE_ENDIAN, -6.0206);

        assertArrayEquals(new short[]{5000, -10000}, shorts(LITTLE_ENDIAN, result));
    }

    @Test
    void amplify_clipsInsteadOfWrappingAround() {
        final byte[] result = Volume.amplify(bytes(LITTLE_ENDIAN, 20000, -20000), LITTLE_ENDIAN, 6);

        assertArrayEquals(new short[]{Short.MAX_VALUE, Short.MIN_VALUE}, shorts(LITTLE_ENDIAN, result));
    }

    @Test
    void amplify_readsBigEndianSamples() {
        final byte[] result = Volume.amplify(bytes(BIG_ENDIAN, 300, -300), BIG_ENDIAN, 6.0206);

        assertArrayEquals(new short[]{600, -600}, shorts(BIG_ENDIAN, result));
    }

    @Test
    void amplify_leavesOtherFormatsAsTheyAre() {
        final AudioFormat eightBit = new AudioFormat(22050, 8, 1, true, false);
        final byte[] samples = {10, 20};

        assertSame(samples, Volume.amplify(samples, eightBit, -12));
    }

    @Test
    void normalize_bringsQuietSpeechUpToThePeak() {
        final SpeechAudio speech = SpeechAudio.ofMono16Bit(new short[]{1000, -4000, 2000}, 22050);

        final SpeechAudio result = Volume.normalize(speech);

        final int expectedPeak = (int) Math.round(32768 * Math.pow(10, Volume.SPEECH_PEAK_DB / 20));
        assertEquals(expectedPeak, Volume.peak(result.samples(), result.format()), 1);
    }

    @Test
    void normalize_bringsLoudSpeechDownToThePeak() {
        final SpeechAudio speech = SpeechAudio.ofMono16Bit(new short[]{Short.MIN_VALUE, 100}, 22050);

        final SpeechAudio result = Volume.normalize(speech);

        final int expectedPeak = (int) Math.round(32768 * Math.pow(10, Volume.SPEECH_PEAK_DB / 20));
        assertEquals(expectedPeak, Volume.peak(result.samples(), result.format()), 1);
    }

    @Test
    void normalize_amplifiesAtMostTheMaximumGain() {
        final SpeechAudio speech = SpeechAudio.ofMono16Bit(new short[]{10, -10}, 22050);

        final SpeechAudio result = Volume.normalize(speech);

        assertEquals(100, Volume.peak(result.samples(), result.format()));
    }

    @Test
    void normalize_leavesSilenceAsItIs() {
        final SpeechAudio silence = SpeechAudio.ofMono16Bit(new short[]{0, 0}, 22050);

        assertSame(silence, Volume.normalize(silence));
    }

    private static byte[] bytes(AudioFormat format, int... samples) {
        final ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(order(format));
        for (int sample : samples) {
            buffer.putShort((short) sample);
        }
        return buffer.array();
    }

    private static short[] shorts(AudioFormat format, byte[] bytes) {
        final short[] result = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(order(format)).asShortBuffer().get(result);
        return result;
    }

    private static ByteOrder order(AudioFormat format) {
        return format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }
}
