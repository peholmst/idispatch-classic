package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;

import javax.sound.sampled.AudioFormat;

/**
 * Sets the level of signed 16-bit PCM sound in software, so that the alarm and the speech come out of the amplifier
 * at levels that suit each other whatever the mixer and the voice. Sound in any other format is left as it is.
 */
final class Volume {

    /**
     * The peak that speech is brought to, just below full scale so that nothing clips.
     */
    static final double SPEECH_PEAK_DB = -1.0;

    /**
     * The most that speech is amplified, so that a nearly silent result is not turned into loud noise.
     */
    static final double MAX_SPEECH_GAIN_DB = 20.0;

    private Volume() {
    }

    /**
     * Returns the speech amplified or attenuated so that its loudest sample is at {@link #SPEECH_PEAK_DB}. Voices
     * differ a lot in level, and are far quieter than the alarm, so this evens them out and makes them as loud as they
     * can be without clipping.
     */
    static SpeechAudio normalize(SpeechAudio audio) {
        if (!isPcm16(audio.format())) {
            return audio;
        }
        final int peak = peak(audio.samples(), audio.format());
        if (peak == 0) {
            return audio;
        }
        final double gain = Math.min(SPEECH_PEAK_DB - toDecibels(peak / 32768.0), MAX_SPEECH_GAIN_DB);
        return new SpeechAudio(amplify(audio.samples(), audio.format(), gain), audio.format());
    }

    /**
     * Returns the samples multiplied by the given gain, clipped to the range of a 16-bit sample.
     *
     * @param gain the gain in decibels: 0 keeps the level, -6 halves it
     */
    static byte[] amplify(byte[] samples, AudioFormat format, double gain) {
        if (!isPcm16(format) || gain == 0) {
            return samples;
        }
        final double factor = Math.pow(10, gain / 20);
        final boolean bigEndian = format.isBigEndian();
        final byte[] result = new byte[samples.length];
        for (int i = 0; i + 1 < samples.length; i += 2) {
            final long amplified = Math.round(sample(samples, i, bigEndian) * factor);
            final int value = (int) Math.clamp(amplified, Short.MIN_VALUE, Short.MAX_VALUE);
            final byte high = (byte) (value >> 8);
            final byte low = (byte) value;
            result[i] = bigEndian ? high : low;
            result[i + 1] = bigEndian ? low : high;
        }
        return result;
    }

    /**
     * Returns the absolute value of the loudest sample.
     */
    static int peak(byte[] samples, AudioFormat format) {
        int peak = 0;
        for (int i = 0; i + 1 < samples.length; i += 2) {
            peak = Math.max(peak, Math.abs(sample(samples, i, format.isBigEndian())));
        }
        return peak;
    }

    private static int sample(byte[] samples, int index, boolean bigEndian) {
        final int first = samples[index];
        final int second = samples[index + 1];
        return bigEndian ? (first << 8) | (second & 0xff) : (second << 8) | (first & 0xff);
    }

    private static boolean isPcm16(AudioFormat format) {
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && format.getSampleSizeInBits() == 16;
    }

    private static double toDecibels(double ratio) {
        return 20 * Math.log10(ratio);
    }
}
