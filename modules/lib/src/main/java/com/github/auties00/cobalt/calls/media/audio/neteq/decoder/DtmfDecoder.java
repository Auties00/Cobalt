package com.github.auties00.cobalt.calls.media.audio.neteq.decoder;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Renders RFC 4733 telephone event (DTMF) tones as a permitted {@link AudioDecoder}, the pseudo decoder
 * the jitter buffer registers for the telephone event payload type.
 *
 * <p>A telephone event payload carries an event code, an end flag, a volume, and a cumulative duration. A
 * {@link #decode(byte[], int, boolean)} call parses the payload and synthesizes one frame of the dual tone
 * multi frequency signal the event names, advancing an internal phase so successive frames of the same
 * event are continuous; a {@link #conceal(int)} call extends the current tone with no new payload, the
 * concealment of a telephone event packet lost mid tone. An event code outside the synthesizable range, or
 * the end flag, yields a silence frame. The forward error correction flag is ignored.
 *
 * <p>The decoder is single writer, driven by the jitter buffer's pull thread; it is pure Java synthesis
 * with no native state.
 *
 * @implNote This implementation parses the four byte event header (event, end and reserved bits with
 * volume, 16 bit duration) and selects the low and high tone pair for the event. The tone is the second
 * order sinusoid recursion {@code y[n] = (a*y[n-1] >> 14) - y[n-2]} with {@code a = 2*cos(2*pi*f/fs)} in
 * Q14, seeded with {@code x[-1] = 0}, {@code x[-2] = sin(2*pi*f/fs)}; the {@link #COEFF_LOW},
 * {@link #COEFF_HIGH}, {@link #INIT_LOW}, {@link #INIT_HIGH}, and {@link #AMPLITUDE} tables and the
 * {@link #AMP_MULTIPLIER} 3 dB low tone attenuation are the verbatim WebRTC tone generator constants. The
 * generator has no onset or offset amplitude ramp; the resonator initialization gives a clean tone start
 * and the recursion memory carries across frames so a sustained tone is continuous.
 */
public final class DtmfDecoder implements AudioDecoder {
    /**
     * The logger for {@link DtmfDecoder}.
     */
    private static final System.Logger LOGGER = Log.get(DtmfDecoder.class);

    /**
     * The minimum telephone event payload length, the four byte event header.
     *
     * <p>A shorter payload is rejected as malformed.
     */
    private static final int EVENT_HEADER_BYTES = 4;

    /**
     * The number of synthesizable DTMF events, the digits, letters, and special keys {@code 0..15}.
     *
     * <p>An event code at or above this is not a tone this decoder synthesizes and yields silence.
     */
    private static final int MAX_TONE_EVENT = 16;

    /**
     * The low frequency oscillator coefficient {@code a = 2*cos(2*pi*f/fs)} in Q14, indexed by sample rate
     * row {@code {8000, 16000, 32000, 48000}} then by event {@code 0..15}.
     *
     * <p>The resonator recursion {@code y[n] = (a*y[n-1] >> 14) - y[n-2]} for the low group tone uses the
     * row matching the decoder sample rate.
     */
    private static final int[][] COEFF_LOW = {
            {24219, 27980, 27980, 27980, 26956, 26956, 26956, 25701, 25701, 25701, 24219, 24219, 27980, 26956, 25701, 24219},
            {30556, 31548, 31548, 31548, 31281, 31281, 31281, 30951, 30951, 30951, 30556, 30556, 31548, 31281, 30951, 30556},
            {32210, 32462, 32462, 32462, 32394, 32394, 32394, 32311, 32311, 32311, 32210, 32210, 32462, 32394, 32311, 32210},
            {32520, 32632, 32632, 32632, 32602, 32602, 32602, 32564, 32564, 32564, 32520, 32520, 32632, 32602, 32564, 32520}
    };

    /**
     * The high frequency oscillator coefficient {@code a = 2*cos(2*pi*f/fs)} in Q14, indexed by sample rate
     * row {@code {8000, 16000, 32000, 48000}} then by event {@code 0..15}.
     *
     * <p>The resonator recursion for the high group tone uses the row matching the decoder sample rate.
     */
    private static final int[][] COEFF_HIGH = {
            {16325, 19073, 16325, 13085, 19073, 16325, 13085, 19073, 16325, 13085, 19073, 13085, 9315, 9315, 9315, 9315},
            {28361, 29144, 28361, 27409, 29144, 28361, 27409, 29144, 28361, 27409, 29144, 27409, 26258, 26258, 26258, 26258},
            {31647, 31849, 31647, 31400, 31849, 31647, 31400, 31849, 31647, 31400, 31849, 31400, 31098, 31098, 31098, 31098},
            {32268, 32359, 32268, 32157, 32359, 32268, 32157, 32359, 32268, 32157, 32359, 32157, 32022, 32022, 32022, 32022}
    };

    /**
     * The low frequency oscillator seed {@code x[-2] = sin(2*pi*f/fs)} in Q14, indexed by sample rate row
     * then by event {@code 0..15}.
     *
     * <p>Seeded into the low group recursion memory at tone onset so the resonator starts on the correct
     * sinusoid phase.
     */
    private static final int[][] INIT_LOW = {
            {11036, 8528, 8528, 8528, 9315, 9315, 9315, 10163, 10163, 10163, 11036, 11036, 8528, 9315, 10163, 11036},
            {5918, 4429, 4429, 4429, 4879, 4879, 4879, 5380, 5380, 5380, 5918, 5918, 4429, 4879, 5380, 5918},
            {3010, 2235, 2235, 2235, 2468, 2468, 2468, 2728, 2728, 2728, 3010, 3010, 2235, 2468, 2728, 3010},
            {2013, 1493, 1493, 1493, 1649, 1649, 1649, 1823, 1823, 1823, 2013, 2013, 1493, 1649, 1823, 2013}
    };

    /**
     * The high frequency oscillator seed {@code x[-2] = sin(2*pi*f/fs)} in Q14, indexed by sample rate row
     * then by event {@code 0..15}.
     *
     * <p>Seeded into the high group recursion memory at tone onset.
     */
    private static final int[][] INIT_HIGH = {
            {14206, 13323, 14206, 15021, 13323, 14206, 15021, 13323, 14206, 15021, 13323, 15021, 15708, 15708, 15708, 15708},
            {8207, 7490, 8207, 8979, 7490, 8207, 8979, 7490, 8207, 8979, 7490, 8979, 9801, 9801, 9801, 9801},
            {4249, 3853, 4249, 4685, 3853, 4249, 4685, 3853, 4249, 4685, 3853, 4685, 5164, 5164, 5164, 5164},
            {2851, 2582, 2851, 3148, 2582, 2851, 3148, 2582, 2851, 3148, 2582, 3148, 3476, 3476, 3476, 3476}
    };

    /**
     * The amplitude multiplier in Q14 for each RFC 4733 volume value {@code 0..63}, corresponding to
     * {@code 0 dBm0} through {@code -63 dBm0}.
     *
     * <p>Scales the normalized dual tone sample to the requested volume; entry zero is the loudest.
     */
    private static final int[] AMPLITUDE = {
            16141, 14386, 12821, 11427, 10184, 9077, 8090, 7210, 6426, 5727, 5104, 4549, 4054, 3614, 3221, 2870,
            2558, 2280, 2032, 1811, 1614, 1439, 1282, 1143, 1018, 908, 809, 721, 643, 573, 510, 455,
            405, 361, 322, 287, 256, 228, 203, 181, 161, 144, 128, 114, 102, 91, 81, 72,
            64, 57, 51, 45, 41, 36, 32, 29, 26, 23, 20, 18, 16, 14, 13, 11
    };

    /**
     * The Q15 multiplier applied to the low group tone, a fixed 3 dB attenuation relative to the
     * high group tone.
     */
    private static final int AMP_MULTIPLIER = 23171;

    /**
     * The output sample rate in hertz this decoder reports.
     */
    private final int sampleRate;

    /**
     * The output channel count this decoder reports.
     */
    private final int channels;

    /**
     * The sample rate row index into the coefficient tables for the configured sample rate.
     *
     * <p>Maps {@code 8000, 16000, 32000, 48000} hertz to rows {@code 0, 1, 2, 3}; an unsupported rate
     * falls back to the 16 kHz row.
     */
    private final int fsIndex;

    /**
     * The event code of the tone currently being rendered, or {@code -1} when none is active.
     */
    private int currentEvent;

    /**
     * The volume of the tone currently being rendered, or {@code -1} when none is active.
     *
     * <p>Tracked so the oscillator is reseeded only when the event or volume changes, keeping the
     * recursion continuous across the frames of one sustained tone.
     */
    private int currentVolume;

    /**
     * The low group oscillator coefficient in Q14 for the current tone.
     */
    private int coeffLow;

    /**
     * The high group oscillator coefficient in Q14 for the current tone.
     */
    private int coeffHigh;

    /**
     * The amplitude multiplier in Q14 for the current tone, from the event volume.
     */
    private int amplitude;

    /**
     * The last two low group oscillator samples, the recursion memory carried across frames.
     */
    private final short[] historyLow;

    /**
     * The last two high group oscillator samples, the recursion memory carried across frames.
     */
    private final short[] historyHigh;

    /**
     * Whether the decoder has been closed.
     *
     * <p>Declared {@code volatile} so the pull thread that drives decode and conceal observes a close
     * performed on the call teardown thread without a data race.
     */
    private volatile boolean closed;

    /**
     * Constructs a telephone event decoder for the given output geometry.
     *
     * @param sampleRate the output sample rate in Hz
     * @param channels   the output channel count, {@code 1} for mono
     */
    public DtmfDecoder(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.fsIndex = sampleRateIndex(sampleRate);
        this.currentEvent = -1;
        this.currentVolume = -1;
        this.coeffLow = 0;
        this.coeffHigh = 0;
        this.amplitude = 0;
        this.historyLow = new short[2];
        this.historyHigh = new short[2];
        this.closed = false;
    }

    /**
     * Maps a sample rate to its row index in the oscillator coefficient tables.
     *
     * <p>Recognizes the four DTMF coding rates {@code 8000, 16000, 32000, 48000} hertz; an unrecognized
     * rate falls back to the 16 kHz row.
     *
     * @param sampleRate the output sample rate in hertz
     * @return the table row index in {@code 0..3}
     */
    private static int sampleRateIndex(int sampleRate) {
        return switch (sampleRate) {
            case 8000 -> 0;
            case 32000 -> 2;
            case 48000 -> 3;
            default -> 1;
        };
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation parses the four byte RFC 4733 event header and, when the event or
     * volume differs from the tone in progress, seeds the dual oscillator from the event code and volume;
     * an unchanged event continues the existing oscillator so the recursion stays continuous. It then
     * synthesizes one frame. The end flag or an event code outside the synthesizable range stops the tone
     * and yields silence. The {@code fec} flag is ignored.
     */
    @Override
    public short[] decode(byte[] payload, int frameSamples, boolean fec) {
        Objects.requireNonNull(payload, "payload cannot be null");
        requireOpen();
        if (payload.length < EVENT_HEADER_BYTES) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "dtmf payload too short, length={0}", payload.length);
            }
            throw new WhatsAppCallException.Rtp(
                    "DTMF telephone-event payload too short: " + payload.length);
        }
        var event = payload[0] & 0xFF;
        var endFlag = (payload[1] & 0x80) != 0;
        var volume = payload[1] & 0x3F;
        if (endFlag || event >= MAX_TONE_EVENT) {
            if (currentEvent >= 0 && Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dtmf tone stopped, event={0}", new LogRedactable.Code(String.valueOf(currentEvent)));
            }
            currentEvent = -1;
            currentVolume = -1;
            return new short[frameSamples];
        }
        if (event != currentEvent || volume != currentVolume) {
            initTone(event, volume);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dtmf tone started, event={0} volume={1}",
                        new LogRedactable.Code(String.valueOf(event)), volume);
            }
        }
        return synthesize(frameSamples);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation extends the current tone with no new payload, the concealment of a lost
     * telephone event packet; with no active tone it returns silence.
     */
    @Override
    public short[] conceal(int frameSamples) {
        requireOpen();
        if (currentEvent < 0) {
            return new short[frameSamples];
        }
        return synthesize(frameSamples);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation always returns {@code false}: a telephone event payload renders a DTMF
     * tone, not speech, so a frame synthesized from it is reported voice inactive.
     */
    @Override
    public boolean packetHasVoiceActivity(byte[] payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation stops the current tone and clears the oscillator recursion memory.
     */
    @Override
    public void reset() {
        requireOpen();
        currentEvent = -1;
        currentVolume = -1;
        amplitude = 0;
        historyLow[0] = 0;
        historyLow[1] = 0;
        historyHigh[0] = 0;
        historyHigh[1] = 0;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dtmf decoder reset");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sampleRate() {
        return sampleRate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int channels() {
        return channels;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation marks the decoder closed; there is no native state to release.
     */
    @Override
    public void close() {
        closed = true;
    }

    /**
     * Seeds the dual oscillator for a new tone from the event code and volume.
     *
     * <p>Looks up the low group and high group resonator coefficients for the configured sample rate and
     * the event, the amplitude multiplier for the volume, and seeds each recursion memory with the
     * sinusoid initialization {@code x[-1] = 0}, {@code x[-2] = sin(2*pi*f/fs)} so the resonator starts on
     * the correct phase. A volume beyond the table is clamped to the quietest entry.
     *
     * @param event  the DTMF event code, in {@code 0..15}
     * @param volume the RFC 4733 volume field, in {@code 0..63}
     */
    private void initTone(int event, int volume) {
        var attenuation = Math.clamp(volume, 0, AMPLITUDE.length - 1);
        currentEvent = event;
        currentVolume = volume;
        coeffLow = COEFF_LOW[fsIndex][event];
        coeffHigh = COEFF_HIGH[fsIndex][event];
        amplitude = AMPLITUDE[attenuation];
        historyLow[0] = (short) INIT_LOW[fsIndex][event];
        historyLow[1] = 0;
        historyHigh[0] = (short) INIT_HIGH[fsIndex][event];
        historyHigh[1] = 0;
    }

    /**
     * Synthesizes one frame of the current dual tone signal, advancing the resonator recursion.
     *
     * <p>Runs the second order sinusoid recursion {@code y[n] = (a*y[n-1] >> 14) - y[n-2]} in Q14 for the
     * low group and high group oscillators, attenuates the low tone 3 dB, normalizes the pair to Q14, and
     * scales by the volume amplitude. The recursion memory carries over so the next frame continues the
     * tone without a discontinuity.
     *
     * @param frameSamples the number of samples to produce
     * @return a fresh array of {@code frameSamples} signed 16 bit tone samples
     */
    private short[] synthesize(int frameSamples) {
        var out = new short[Math.max(frameSamples, 0)];
        for (var i = 0; i < out.length; i++) {
            var low = (short) (((coeffLow * historyLow[1] + 8192) >> 14) - historyLow[0]);
            var high = (short) (((coeffHigh * historyHigh[1] + 8192) >> 14) - historyHigh[0]);
            historyLow[0] = historyLow[1];
            historyLow[1] = low;
            historyHigh[0] = historyHigh[1];
            historyHigh[1] = high;
            var mixed = AMP_MULTIPLIER * low + high * (1 << 15);
            mixed = (mixed + 16384) >> 15;
            out[i] = (short) ((mixed * amplitude + 8192) >> 14);
        }
        return out;
    }

    /**
     * Verifies that the decoder is still open.
     *
     * @throws IllegalStateException if the decoder has been closed
     */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("DtmfDecoder is closed");
        }
    }
}
