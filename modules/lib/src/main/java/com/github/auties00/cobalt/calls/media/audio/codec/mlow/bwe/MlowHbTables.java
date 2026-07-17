package com.github.auties00.cobalt.calls.media.audio.codec.mlow.bwe;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dequantized high band vector quantization tables for the MLow speech codec.
 *
 * <p>The high band extension stores its line spectral frequency (LSF) and gain codebooks in a compressed delta
 * form (delta coded byte streams plus affine min and scale pairs and delta cumulative mass function streams)
 * and expands them once at startup into the float codebooks and {@code uint16} cumulative mass functions (CMFs)
 * the decode path reads. This class holds that expanded runtime form: the dequantized LSF codebook vectors (one
 * per voicing and rate class), the dequantized gain shape vectors (one per frame length, voicing, and rate
 * class), the entropy CMFs the high band parameter decode reads, the conditional LSF CMFs and their row
 * selection map, and the gain power exponents the gain dequantizer reads.
 *
 * <p>The expanded values are serialized into a small binary resource ({@code mlow_hb_tables.bin}) this class
 * loads at class initialization. The resource is little endian: a {@code "MHB1"} magic, then a sequence of
 * named blocks, each a one byte type ({@code 0} float, {@code 1} int), a length prefixed UTF-8 name, an
 * {@code int32} entry count, and the little endian payload. Storing the data as a resource rather than as
 * source literals keeps the dequantized values exact while staying within the bytecode limits a 40 kB literal
 * table would breach.
 *
 * <p>The index layout is: LSF tables are indexed {@code [voiced][lowRate]} and gain tables
 * {@code [framelen20][voiced][lowRate]}, where {@code framelen20} is {@code 1} for the 20 ms (four high band
 * subframe) frame and {@code 0} for the 10 ms (two high band subframe) frame, {@code voiced} is the frame
 * voicing, and {@code lowRate} is the rate class. The conditional LSF CMFs are indexed {@code [voiced]} and
 * hold {@value #HB_LPC_CB_N_COND} concatenated CMF rows, each {@code size + 1} entries; the
 * {@link #selCond(int)} row selection map picks the row from the previous frame's LSF index.
 *
 * @implNote This implementation loads the resource once into the static fields and exposes them through
 * accessors that return the live backing arrays (the decode path never mutates them). The float codebooks and
 * the gain shapes carry the affine unpacked values; the {@code uint16} CMFs are widened to {@code int} so the
 * unsigned range carries without sign extension, exactly as the entropy decoder reads them.
 */
final class MlowHbTables {
    /**
     * The logger for {@link MlowHbTables}.
     */
    private static final System.Logger LOGGER = Log.get(MlowHbTables.class);

    /**
     * The high band linear prediction order ({@code 4}); the LSF vector length of every high band codebook
     * entry.
     */
    static final int HB_LPC_ORDER = 4;

    /**
     * The number of conditional LSF CMF rows per voicing class ({@code 8}); the conditional CMF block for a
     * voicing holds this many concatenated rows.
     */
    static final int HB_LPC_CB_N_COND = 8;

    /**
     * The resource path of the dequantized high band table blob, relative to this class.
     */
    private static final String RESOURCE = "mlow_hb_tables.bin";

    // TODO: flatten the fixed index dimensions of the multi dimensional tables below (LSF_* [voiced][lowRate]
    //  and GAIN_* [framelen20][voiced][lowRate]) into single dimension arrays keyed the same way GAIN_PWRS
    //  already is (e.g. (framelen20 << 2) | (voiced << 1) | lowRate), preserving the value at every index.
    //  Held back: the high band path is not wired into the low band decode and is not exercised by the
    //  MlowBitIdentityTest oracle, so a flatten here cannot be proven bit identical by the golden and waits on
    //  the bandwidth extension path (and its own bit identity coverage) landing.

    /**
     * The LSF codebook sizes, indexed {@code [voiced][lowRate]}; the count of LSF vectors in each codebook.
     */
    private static final int[][] LSF_SIZES = new int[2][2];

    /**
     * The gain codebook sizes, indexed {@code [framelen20][voiced][lowRate]}; the count of gain shape vectors
     * in each codebook.
     */
    private static final int[][][] GAIN_SIZES = new int[2][2][2];

    /**
     * The dequantized LSF codebooks, indexed {@code [voiced][lowRate]}, each a flat
     * {@code size * HB_LPC_ORDER} float array of LSF vectors.
     */
    private static final float[][][] LSF_CB = new float[2][2][];

    /**
     * The LSF entropy CMFs, indexed {@code [voiced][lowRate]}, each {@code size + 1} cumulative entries.
     */
    private static final int[][][] LSF_CMF = new int[2][2][];

    /**
     * The conditional LSF entropy CMFs, indexed {@code [voiced]}, each {@value #HB_LPC_CB_N_COND} concatenated
     * rows of {@code size_lowRate + 1} entries.
     */
    private static final int[][] LSF_CMF_COND = new int[2][];

    /**
     * The conditional LSF row selection map, indexed {@code [voiced]}, mapping a previous frame LSF index to a
     * conditional CMF row in {@code [0, HB_LPC_CB_N_COND)}.
     */
    private static final int[][] SEL_COND = new int[2][];

    /**
     * The dequantized gain shape codebooks, indexed {@code [framelen20][voiced][lowRate]}, each a flat float
     * array of {@code size} shape vectors, each {@code numHbSubframes} entries long.
     */
    private static final float[][][][] GAIN_CB = new float[2][2][2][];

    /**
     * The gain entropy CMFs, indexed {@code [framelen20][voiced][lowRate]}, each {@code size + 1} cumulative
     * entries.
     */
    private static final int[][][][] GAIN_CMF = new int[2][2][2][];

    /**
     * The gain power exponents, a flat array keyed by {@code (framelen20 << 2) | (voiced << 1) | lowRate}; the
     * subframe energy redistribution exponent the gain dequantizer reads.
     */
    private static final float[] GAIN_PWRS = new float[8];

    static {
        load();
    }

    /**
     * Prevents instantiation; this class only exposes the static high band tables.
     */
    private MlowHbTables() {
    }

    /**
     * Returns the LSF codebook size for a voicing and rate class.
     *
     * @param voiced  the voicing index ({@code 0} unvoiced, {@code 1} voiced)
     * @param lowRate the rate index ({@code 0} high rate, {@code 1} low rate)
     * @return the number of LSF vectors in the codebook
     */
    static int lsfSize(int voiced, int lowRate) {
        return LSF_SIZES[voiced][lowRate];
    }

    /**
     * Returns the gain codebook size for a frame length, voicing, and rate class.
     *
     * @param framelen20 the frame length index ({@code 1} for the 20 ms four subframe frame, {@code 0} for the
     *                   10 ms two subframe frame)
     * @param voiced     the voicing index
     * @param lowRate    the rate index
     * @return the number of gain shape vectors in the codebook
     */
    static int gainSize(int framelen20, int voiced, int lowRate) {
        return GAIN_SIZES[framelen20][voiced][lowRate];
    }

    /**
     * Returns the dequantized LSF codebook for a voicing and rate class, a flat
     * {@code size * HB_LPC_ORDER} array.
     *
     * @param voiced  the voicing index
     * @param lowRate the rate index
     * @return the live backing LSF codebook array
     */
    static float[] lsfCb(int voiced, int lowRate) {
        return LSF_CB[voiced][lowRate];
    }

    /**
     * Returns the LSF entropy CMF for a voicing and rate class.
     *
     * @param voiced  the voicing index
     * @param lowRate the rate index
     * @return the live backing CMF array, {@code size + 1} cumulative entries
     */
    static int[] lsfCmf(int voiced, int lowRate) {
        return LSF_CMF[voiced][lowRate];
    }

    /**
     * Returns the conditional LSF entropy CMF block for a voicing class; the {@value #HB_LPC_CB_N_COND}
     * concatenated rows the conditional high band LSF decode windows by {@link #selCond(int)}.
     *
     * @param voiced the voicing index
     * @return the live backing conditional CMF array
     */
    static int[] lsfCmfCond(int voiced) {
        return LSF_CMF_COND[voiced];
    }

    /**
     * Returns the conditional LSF row selection map for a voicing class; maps a previous frame LSF index to a
     * conditional CMF row.
     *
     * @param voiced the voicing index
     * @return the live backing row selection array
     */
    static int[] selCond(int voiced) {
        return SEL_COND[voiced];
    }

    /**
     * Returns the dequantized gain shape codebook for a frame length, voicing, and rate class, a flat array of
     * {@code size} shape vectors, each {@code numHbSubframes} entries long.
     *
     * @param framelen20 the frame length index
     * @param voiced     the voicing index
     * @param lowRate    the rate index
     * @return the live backing gain shape codebook array
     */
    static float[] gainCb(int framelen20, int voiced, int lowRate) {
        return GAIN_CB[framelen20][voiced][lowRate];
    }

    /**
     * Returns the gain entropy CMF for a frame length, voicing, and rate class.
     *
     * @param framelen20 the frame length index
     * @param voiced     the voicing index
     * @param lowRate    the rate index
     * @return the live backing CMF array, {@code size + 1} cumulative entries
     */
    static int[] gainCmf(int framelen20, int voiced, int lowRate) {
        return GAIN_CMF[framelen20][voiced][lowRate];
    }

    /**
     * Returns the gain power exponent for a frame length, voicing, and rate class.
     *
     * @param framelen20 the frame length index
     * @param voiced     the voicing index
     * @param lowRate    the rate index
     * @return the gain power exponent
     */
    static float gainPwr(int framelen20, int voiced, int lowRate) {
        return GAIN_PWRS[(framelen20 << 2) | (voiced << 1) | lowRate];
    }

    /**
     * Loads the dequantized high band tables from the binary resource into the static fields.
     *
     * <p>Reads the {@code "MHB1"} magic and every named block, dispatching each block name into the matching
     * static field. The block order is fixed by the resource generator; an unrecognized name or a truncated
     * stream is a build error.
     *
     * @throws UncheckedIOException if the resource is missing, truncated, or has the wrong magic
     */
    private static void load() {
        try (var in = MlowHbTables.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("missing high-band table resource " + RESOURCE);
            }
            var data = new DataInputStream(in);
            if (data.read() != 'M' || data.read() != 'H' || data.read() != 'B' || data.read() != '1') {
                throw new IOException("bad high-band table magic");
            }
            Map<String, float[]> floats = new HashMap<>();
            Map<String, int[]> ints = new HashMap<>();
            while (true) {
                var type = data.read();
                if (type < 0) {
                    break;
                }
                var name = readName(data);
                var count = readLeInt(data);
                if (type == 0) {
                    var arr = new float[count];
                    for (var i = 0; i < count; i++) {
                        arr[i] = Float.intBitsToFloat(readLeInt(data));
                    }
                    floats.put(name, arr);
                } else {
                    var arr = new int[count];
                    for (var i = 0; i < count; i++) {
                        arr[i] = readLeInt(data);
                    }
                    ints.put(name, arr);
                }
            }
            var lsfSizes = ints.get("LSF_SIZES");
            for (var v = 0; v < 2; v++) {
                for (var lr = 0; lr < 2; lr++) {
                    LSF_SIZES[v][lr] = lsfSizes[v * 2 + lr];
                    LSF_CB[v][lr] = floats.get("LSF_CB_" + v + "_" + lr);
                    LSF_CMF[v][lr] = ints.get("LSF_CMF_" + v + "_" + lr);
                }
                LSF_CMF_COND[v] = ints.get("LSF_CMF_COND_" + v);
                SEL_COND[v] = ints.get("SEL_COND_" + v);
            }
            var gainSizes = ints.get("GAIN_SIZES");
            for (var fl = 0; fl < 2; fl++) {
                for (var v = 0; v < 2; v++) {
                    for (var lr = 0; lr < 2; lr++) {
                        GAIN_SIZES[fl][v][lr] = gainSizes[(fl * 2 + v) * 2 + lr];
                        GAIN_CB[fl][v][lr] = floats.get("GAIN_CB_" + fl + "_" + v + "_" + lr);
                        GAIN_CMF[fl][v][lr] = ints.get("GAIN_CMF_" + fl + "_" + v + "_" + lr);
                    }
                }
            }
            System.arraycopy(floats.get("GAIN_PWRS"), 0, GAIN_PWRS, 0, GAIN_PWRS.length);
        } catch (IOException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "mlow high-band table load failed", e);
            }
            throw new UncheckedIOException("failed to load MLow high-band tables", e);
        }
    }

    /**
     * Reads a length prefixed UTF-8 block name from the resource stream.
     *
     * @param data the resource stream positioned at the name length
     * @return the decoded block name
     * @throws IOException if the stream ends before the name is complete
     */
    private static String readName(DataInputStream data) throws IOException {
        var len = readLeInt(data);
        var bytes = new byte[len];
        data.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads one little endian {@code int32} from the resource stream.
     *
     * @param data the resource stream
     * @return the decoded value
     * @throws IOException  if the stream ends before four bytes are read
     * @throws EOFException if the stream ends before a full value is read
     */
    private static int readLeInt(DataInputStream data) throws IOException {
        var b0 = data.read();
        var b1 = data.read();
        var b2 = data.read();
        var b3 = data.read();
        if ((b0 | b1 | b2 | b3) < 0) {
            throw new EOFException("truncated high-band table resource");
        }
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
