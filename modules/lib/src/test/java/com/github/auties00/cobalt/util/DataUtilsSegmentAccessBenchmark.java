package com.github.auties00.cobalt.util;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Compares candidate implementations of order-controlled UNALIGNED primitive access on a
 * {@link MemorySegment}, to pick the fastest before adding {@code MemorySegment} overloads to
 * {@link DataUtils}. Five strategies decode and encode {@code short}/{@code int}/{@code long}/
 * {@code float}/{@code double} at a byte offset under a caller-supplied {@link ByteOrder}:
 * <ul>
 *   <li>A: per-order static {@link VarHandle} from {@code JAVA_*_UNALIGNED.withOrder(BE/LE).varHandle()}, branch on order.</li>
 *   <li>B: {@code MemorySegment.get(layout, offset)} convenience with order-specialized layouts cached static final.</li>
 *   <li>C: single native-order {@link VarHandle} plus {@code Integer/Short/Long.reverseBytes} (float/double via {@code *ToRawIntBits}/{@code *BitsTo*}) when the requested order differs from {@link ByteOrder#nativeOrder()}.</li>
 *   <li>D: byte-by-byte {@code get(JAVA_BYTE, ...)} with manual shift/combine assembly.</li>
 *   <li>E: copy-to-heap then the existing {@link DataUtils} {@code byte[]} accessor (baseline).</li>
 * </ul>
 * The axes are swept as {@code @Param}: primitive type, byte order (native vs opposite), offset
 * (aligned {@code 0} vs unaligned {@code +1}), operation (get vs put), and backing (heap
 * {@link MemorySegment#ofArray(byte[])} vs native {@link Arena#ofConfined()} allocation). Every
 * result is fed to a {@link Blackhole}; {@link #setup()} asserts each strategy is bit-identical to
 * the corresponding {@link DataUtils} {@code byte[]} accessor on the same bytes before any timing
 * runs, so the throughput numbers only ever compare implementations proven to agree.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DataUtilsSegmentAccessBenchmark {
    /**
     * The primitive type read or written by the benchmarked strategy.
     */
    @Param({"SHORT", "INT", "LONG", "FLOAT", "DOUBLE"})
    private String type;

    /**
     * The byte order to interpret the field with, expressed relative to {@link ByteOrder#nativeOrder()}
     * so the fast native path ({@code NATIVE}) and the byte-reversing path ({@code OPPOSITE}) are both
     * exercised on any host.
     */
    @Param({"NATIVE", "OPPOSITE"})
    private String order;

    /**
     * The byte offset into the segment: {@code 0} is naturally aligned for every primitive, {@code 1}
     * forces an unaligned access.
     */
    @Param({"0", "1"})
    private int offset;

    /**
     * The operation exercised: {@code GET} decodes a value from the segment, {@code PUT} encodes one into it.
     */
    @Param({"GET", "PUT"})
    private String op;

    /**
     * The segment backing: {@code HEAP} for {@link MemorySegment#ofArray(byte[])}, {@code NATIVE} for
     * an off-heap {@link Arena} allocation.
     */
    @Param({"HEAP", "NATIVE"})
    private String backing;

    /**
     * The primitive width dispatched on inside each benchmark method.
     */
    private enum Type {SHORT, INT, LONG, FLOAT, DOUBLE}

    /**
     * A mutating action over a segment, used to replay each put strategy against a scratch copy during verification.
     */
    private interface SegOp {
        void run(MemorySegment segment);
    }

    private static final ByteOrder NATIVE = ByteOrder.nativeOrder();

    private static final ValueLayout.OfShort S_BE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfShort S_LE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfInt I_BE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfInt I_LE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfLong L_BE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfLong L_LE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfFloat F_BE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfFloat F_LE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfDouble D_BE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
    private static final ValueLayout.OfDouble D_LE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    private static final VarHandle AH_S_BE = S_BE.varHandle();
    private static final VarHandle AH_S_LE = S_LE.varHandle();
    private static final VarHandle AH_I_BE = I_BE.varHandle();
    private static final VarHandle AH_I_LE = I_LE.varHandle();
    private static final VarHandle AH_L_BE = L_BE.varHandle();
    private static final VarHandle AH_L_LE = L_LE.varHandle();
    private static final VarHandle AH_F_BE = F_BE.varHandle();
    private static final VarHandle AH_F_LE = F_LE.varHandle();
    private static final VarHandle AH_D_BE = D_BE.varHandle();
    private static final VarHandle AH_D_LE = D_LE.varHandle();

    private static final VarHandle NH_S = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle();
    private static final VarHandle NH_I = ValueLayout.JAVA_INT_UNALIGNED.varHandle();
    private static final VarHandle NH_L = ValueLayout.JAVA_LONG_UNALIGNED.varHandle();

    private final byte[] refBytes = new byte[32];
    private Type kind;
    private boolean get;
    private ByteOrder byteOrder;
    private Arena arena;
    private MemorySegment segment;
    private short sVal;
    private int iVal;
    private long lVal;
    private float fVal;
    private double dVal;

    @Setup(Level.Trial)
    public void setup() {
        var random = new Random(0xC0BA17L);
        random.nextBytes(refBytes);
        kind = Type.valueOf(type);
        get = "GET".equals(op);
        byteOrder = "OPPOSITE".equals(order)
                ? (NATIVE == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN)
                : NATIVE;
        arena = Arena.ofConfined();
        if ("NATIVE".equals(backing)) {
            segment = arena.allocate(refBytes.length);
            MemorySegment.copy(refBytes, 0, segment, JAVA_BYTE, 0, refBytes.length);
        } else {
            segment = MemorySegment.ofArray(refBytes.clone());
        }
        sVal = DataUtils.getShort(refBytes, offset, byteOrder);
        iVal = DataUtils.getInt(refBytes, offset, byteOrder);
        lVal = DataUtils.getLong(refBytes, offset, byteOrder);
        fVal = DataUtils.getFloat(refBytes, offset, byteOrder);
        dVal = DataUtils.getDouble(refBytes, offset, byteOrder);
        verify();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        arena.close();
    }

    @Benchmark
    public void strategyA(Blackhole bh) {
        switch (kind) {
            case SHORT -> { if (get) bh.consume(aGetShort(segment, offset, byteOrder)); else { aPutShort(segment, offset, sVal, byteOrder); bh.consume(segment); } }
            case INT -> { if (get) bh.consume(aGetInt(segment, offset, byteOrder)); else { aPutInt(segment, offset, iVal, byteOrder); bh.consume(segment); } }
            case LONG -> { if (get) bh.consume(aGetLong(segment, offset, byteOrder)); else { aPutLong(segment, offset, lVal, byteOrder); bh.consume(segment); } }
            case FLOAT -> { if (get) bh.consume(aGetFloat(segment, offset, byteOrder)); else { aPutFloat(segment, offset, fVal, byteOrder); bh.consume(segment); } }
            case DOUBLE -> { if (get) bh.consume(aGetDouble(segment, offset, byteOrder)); else { aPutDouble(segment, offset, dVal, byteOrder); bh.consume(segment); } }
        }
    }

    @Benchmark
    public void strategyB(Blackhole bh) {
        switch (kind) {
            case SHORT -> { if (get) bh.consume(bGetShort(segment, offset, byteOrder)); else { bPutShort(segment, offset, sVal, byteOrder); bh.consume(segment); } }
            case INT -> { if (get) bh.consume(bGetInt(segment, offset, byteOrder)); else { bPutInt(segment, offset, iVal, byteOrder); bh.consume(segment); } }
            case LONG -> { if (get) bh.consume(bGetLong(segment, offset, byteOrder)); else { bPutLong(segment, offset, lVal, byteOrder); bh.consume(segment); } }
            case FLOAT -> { if (get) bh.consume(bGetFloat(segment, offset, byteOrder)); else { bPutFloat(segment, offset, fVal, byteOrder); bh.consume(segment); } }
            case DOUBLE -> { if (get) bh.consume(bGetDouble(segment, offset, byteOrder)); else { bPutDouble(segment, offset, dVal, byteOrder); bh.consume(segment); } }
        }
    }

    @Benchmark
    public void strategyC(Blackhole bh) {
        switch (kind) {
            case SHORT -> { if (get) bh.consume(cGetShort(segment, offset, byteOrder)); else { cPutShort(segment, offset, sVal, byteOrder); bh.consume(segment); } }
            case INT -> { if (get) bh.consume(cGetInt(segment, offset, byteOrder)); else { cPutInt(segment, offset, iVal, byteOrder); bh.consume(segment); } }
            case LONG -> { if (get) bh.consume(cGetLong(segment, offset, byteOrder)); else { cPutLong(segment, offset, lVal, byteOrder); bh.consume(segment); } }
            case FLOAT -> { if (get) bh.consume(cGetFloat(segment, offset, byteOrder)); else { cPutFloat(segment, offset, fVal, byteOrder); bh.consume(segment); } }
            case DOUBLE -> { if (get) bh.consume(cGetDouble(segment, offset, byteOrder)); else { cPutDouble(segment, offset, dVal, byteOrder); bh.consume(segment); } }
        }
    }

    @Benchmark
    public void strategyD(Blackhole bh) {
        switch (kind) {
            case SHORT -> { if (get) bh.consume(dGetShort(segment, offset, byteOrder)); else { dPutShort(segment, offset, sVal, byteOrder); bh.consume(segment); } }
            case INT -> { if (get) bh.consume(dGetInt(segment, offset, byteOrder)); else { dPutInt(segment, offset, iVal, byteOrder); bh.consume(segment); } }
            case LONG -> { if (get) bh.consume(dGetLong(segment, offset, byteOrder)); else { dPutLong(segment, offset, lVal, byteOrder); bh.consume(segment); } }
            case FLOAT -> { if (get) bh.consume(dGetFloat(segment, offset, byteOrder)); else { dPutFloat(segment, offset, fVal, byteOrder); bh.consume(segment); } }
            case DOUBLE -> { if (get) bh.consume(dGetDouble(segment, offset, byteOrder)); else { dPutDouble(segment, offset, dVal, byteOrder); bh.consume(segment); } }
        }
    }

    @Benchmark
    public void strategyE(Blackhole bh) {
        switch (kind) {
            case SHORT -> { if (get) bh.consume(eGetShort(segment, offset, byteOrder)); else { ePutShort(segment, offset, sVal, byteOrder); bh.consume(segment); } }
            case INT -> { if (get) bh.consume(eGetInt(segment, offset, byteOrder)); else { ePutInt(segment, offset, iVal, byteOrder); bh.consume(segment); } }
            case LONG -> { if (get) bh.consume(eGetLong(segment, offset, byteOrder)); else { ePutLong(segment, offset, lVal, byteOrder); bh.consume(segment); } }
            case FLOAT -> { if (get) bh.consume(eGetFloat(segment, offset, byteOrder)); else { ePutFloat(segment, offset, fVal, byteOrder); bh.consume(segment); } }
            case DOUBLE -> { if (get) bh.consume(eGetDouble(segment, offset, byteOrder)); else { ePutDouble(segment, offset, dVal, byteOrder); bh.consume(segment); } }
        }
    }

    // Strategy A: per-order static VarHandle, branch on order.

    private static short aGetShort(MemorySegment s, int o, ByteOrder ord) { return (short) (ord == ByteOrder.BIG_ENDIAN ? AH_S_BE : AH_S_LE).get(s, (long) o); }
    private static int aGetInt(MemorySegment s, int o, ByteOrder ord) { return (int) (ord == ByteOrder.BIG_ENDIAN ? AH_I_BE : AH_I_LE).get(s, (long) o); }
    private static long aGetLong(MemorySegment s, int o, ByteOrder ord) { return (long) (ord == ByteOrder.BIG_ENDIAN ? AH_L_BE : AH_L_LE).get(s, (long) o); }
    private static float aGetFloat(MemorySegment s, int o, ByteOrder ord) { return (float) (ord == ByteOrder.BIG_ENDIAN ? AH_F_BE : AH_F_LE).get(s, (long) o); }
    private static double aGetDouble(MemorySegment s, int o, ByteOrder ord) { return (double) (ord == ByteOrder.BIG_ENDIAN ? AH_D_BE : AH_D_LE).get(s, (long) o); }

    private static void aPutShort(MemorySegment s, int o, short v, ByteOrder ord) { (ord == ByteOrder.BIG_ENDIAN ? AH_S_BE : AH_S_LE).set(s, (long) o, v); }
    private static void aPutInt(MemorySegment s, int o, int v, ByteOrder ord) { (ord == ByteOrder.BIG_ENDIAN ? AH_I_BE : AH_I_LE).set(s, (long) o, v); }
    private static void aPutLong(MemorySegment s, int o, long v, ByteOrder ord) { (ord == ByteOrder.BIG_ENDIAN ? AH_L_BE : AH_L_LE).set(s, (long) o, v); }
    private static void aPutFloat(MemorySegment s, int o, float v, ByteOrder ord) { (ord == ByteOrder.BIG_ENDIAN ? AH_F_BE : AH_F_LE).set(s, (long) o, v); }
    private static void aPutDouble(MemorySegment s, int o, double v, ByteOrder ord) { (ord == ByteOrder.BIG_ENDIAN ? AH_D_BE : AH_D_LE).set(s, (long) o, v); }

    // Strategy B: MemorySegment.get/set convenience with order-specialized cached layouts.

    private static short bGetShort(MemorySegment s, int o, ByteOrder ord) { return s.get(ord == ByteOrder.BIG_ENDIAN ? S_BE : S_LE, o); }
    private static int bGetInt(MemorySegment s, int o, ByteOrder ord) { return s.get(ord == ByteOrder.BIG_ENDIAN ? I_BE : I_LE, o); }
    private static long bGetLong(MemorySegment s, int o, ByteOrder ord) { return s.get(ord == ByteOrder.BIG_ENDIAN ? L_BE : L_LE, o); }
    private static float bGetFloat(MemorySegment s, int o, ByteOrder ord) { return s.get(ord == ByteOrder.BIG_ENDIAN ? F_BE : F_LE, o); }
    private static double bGetDouble(MemorySegment s, int o, ByteOrder ord) { return s.get(ord == ByteOrder.BIG_ENDIAN ? D_BE : D_LE, o); }

    private static void bPutShort(MemorySegment s, int o, short v, ByteOrder ord) { s.set(ord == ByteOrder.BIG_ENDIAN ? S_BE : S_LE, o, v); }
    private static void bPutInt(MemorySegment s, int o, int v, ByteOrder ord) { s.set(ord == ByteOrder.BIG_ENDIAN ? I_BE : I_LE, o, v); }
    private static void bPutLong(MemorySegment s, int o, long v, ByteOrder ord) { s.set(ord == ByteOrder.BIG_ENDIAN ? L_BE : L_LE, o, v); }
    private static void bPutFloat(MemorySegment s, int o, float v, ByteOrder ord) { s.set(ord == ByteOrder.BIG_ENDIAN ? F_BE : F_LE, o, v); }
    private static void bPutDouble(MemorySegment s, int o, double v, ByteOrder ord) { s.set(ord == ByteOrder.BIG_ENDIAN ? D_BE : D_LE, o, v); }

    // Strategy C: single native-order VarHandle plus reverseBytes / raw-bits reinterpretation when order != native.

    private static short cGetShort(MemorySegment s, int o, ByteOrder ord) {
        var v = (short) NH_S.get(s, (long) o); return ord == NATIVE ? v : Short.reverseBytes(v); }
    private static int cGetInt(MemorySegment s, int o, ByteOrder ord) {
        var v = (int) NH_I.get(s, (long) o); return ord == NATIVE ? v : Integer.reverseBytes(v); }
    private static long cGetLong(MemorySegment s, int o, ByteOrder ord) {
        var v = (long) NH_L.get(s, (long) o); return ord == NATIVE ? v : Long.reverseBytes(v); }
    private static float cGetFloat(MemorySegment s, int o, ByteOrder ord) {
        var r = (int) NH_I.get(s, (long) o); if (ord != NATIVE) r = Integer.reverseBytes(r); return Float.intBitsToFloat(r); }
    private static double cGetDouble(MemorySegment s, int o, ByteOrder ord) {
        var r = (long) NH_L.get(s, (long) o); if (ord != NATIVE) r = Long.reverseBytes(r); return Double.longBitsToDouble(r); }

    private static void cPutShort(MemorySegment s, int o, short v, ByteOrder ord) { NH_S.set(s, (long) o, ord == NATIVE ? v : Short.reverseBytes(v)); }
    private static void cPutInt(MemorySegment s, int o, int v, ByteOrder ord) { NH_I.set(s, (long) o, ord == NATIVE ? v : Integer.reverseBytes(v)); }
    private static void cPutLong(MemorySegment s, int o, long v, ByteOrder ord) { NH_L.set(s, (long) o, ord == NATIVE ? v : Long.reverseBytes(v)); }
    private static void cPutFloat(MemorySegment s, int o, float v, ByteOrder ord) {
        var r = Float.floatToRawIntBits(v); if (ord != NATIVE) r = Integer.reverseBytes(r); NH_I.set(s, (long) o, r); }
    private static void cPutDouble(MemorySegment s, int o, double v, ByteOrder ord) {
        var r = Double.doubleToRawLongBits(v); if (ord != NATIVE) r = Long.reverseBytes(r); NH_L.set(s, (long) o, r); }

    // Strategy D: byte-by-byte get(JAVA_BYTE, ...) with manual shift/combine.

    private static short dGetShort(MemorySegment s, int o, ByteOrder ord) {
        var b0 = s.get(JAVA_BYTE, o) & 0xFF;
        var b1 = s.get(JAVA_BYTE, o + 1) & 0xFF;
        return ord == ByteOrder.BIG_ENDIAN ? (short) ((b0 << 8) | b1) : (short) ((b1 << 8) | b0);
    }

    private static int dGetInt(MemorySegment s, int o, ByteOrder ord) {
        var b0 = s.get(JAVA_BYTE, o) & 0xFF;
        var b1 = s.get(JAVA_BYTE, o + 1) & 0xFF;
        var b2 = s.get(JAVA_BYTE, o + 2) & 0xFF;
        var b3 = s.get(JAVA_BYTE, o + 3) & 0xFF;
        return ord == ByteOrder.BIG_ENDIAN
                ? (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
                : (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    private static long dGetLong(MemorySegment s, int o, ByteOrder ord) {
        long result = 0;
        if (ord == ByteOrder.BIG_ENDIAN) {
            for (var i = 0; i < 8; i++) {
                result = (result << 8) | (s.get(JAVA_BYTE, o + i) & 0xFFL);
            }
        } else {
            for (var i = 7; i >= 0; i--) {
                result = (result << 8) | (s.get(JAVA_BYTE, o + i) & 0xFFL);
            }
        }
        return result;
    }

    private static float dGetFloat(MemorySegment s, int o, ByteOrder ord) { return Float.intBitsToFloat(dGetInt(s, o, ord)); }
    private static double dGetDouble(MemorySegment s, int o, ByteOrder ord) { return Double.longBitsToDouble(dGetLong(s, o, ord)); }

    private static void dPutShort(MemorySegment s, int o, short v, ByteOrder ord) {
        var iv = v & 0xFFFF;
        if (ord == ByteOrder.BIG_ENDIAN) {
            s.set(JAVA_BYTE, o, (byte) (iv >>> 8));
            s.set(JAVA_BYTE, o + 1, (byte) iv);
        } else {
            s.set(JAVA_BYTE, o, (byte) iv);
            s.set(JAVA_BYTE, o + 1, (byte) (iv >>> 8));
        }
    }

    private static void dPutInt(MemorySegment s, int o, int v, ByteOrder ord) {
        if (ord == ByteOrder.BIG_ENDIAN) {
            for (var i = 0; i < 4; i++) {
                s.set(JAVA_BYTE, o + i, (byte) (v >>> (24 - 8 * i)));
            }
        } else {
            for (var i = 0; i < 4; i++) {
                s.set(JAVA_BYTE, o + i, (byte) (v >>> (8 * i)));
            }
        }
    }

    private static void dPutLong(MemorySegment s, int o, long v, ByteOrder ord) {
        if (ord == ByteOrder.BIG_ENDIAN) {
            for (var i = 0; i < 8; i++) {
                s.set(JAVA_BYTE, o + i, (byte) (v >>> (56 - 8 * i)));
            }
        } else {
            for (var i = 0; i < 8; i++) {
                s.set(JAVA_BYTE, o + i, (byte) (v >>> (8 * i)));
            }
        }
    }

    private static void dPutFloat(MemorySegment s, int o, float v, ByteOrder ord) { dPutInt(s, o, Float.floatToRawIntBits(v), ord); }
    private static void dPutDouble(MemorySegment s, int o, double v, ByteOrder ord) { dPutLong(s, o, Double.doubleToRawLongBits(v), ord); }

    // Strategy E: copy the field bytes to the heap, then reuse the existing DataUtils byte[] accessor (baseline).

    private static short eGetShort(MemorySegment s, int o, ByteOrder ord) { var t = new byte[2]; MemorySegment.copy(s, JAVA_BYTE, o, t, 0, 2); return DataUtils.getShort(t, 0, ord); }
    private static int eGetInt(MemorySegment s, int o, ByteOrder ord) { var t = new byte[4]; MemorySegment.copy(s, JAVA_BYTE, o, t, 0, 4); return DataUtils.getInt(t, 0, ord); }
    private static long eGetLong(MemorySegment s, int o, ByteOrder ord) { var t = new byte[8]; MemorySegment.copy(s, JAVA_BYTE, o, t, 0, 8); return DataUtils.getLong(t, 0, ord); }
    private static float eGetFloat(MemorySegment s, int o, ByteOrder ord) { var t = new byte[4]; MemorySegment.copy(s, JAVA_BYTE, o, t, 0, 4); return DataUtils.getFloat(t, 0, ord); }
    private static double eGetDouble(MemorySegment s, int o, ByteOrder ord) { var t = new byte[8]; MemorySegment.copy(s, JAVA_BYTE, o, t, 0, 8); return DataUtils.getDouble(t, 0, ord); }

    private static void ePutShort(MemorySegment s, int o, short v, ByteOrder ord) { var t = new byte[2]; DataUtils.putShort(t, 0, v, ord); MemorySegment.copy(t, 0, s, JAVA_BYTE, o, 2); }
    private static void ePutInt(MemorySegment s, int o, int v, ByteOrder ord) { var t = new byte[4]; DataUtils.putInt(t, 0, v, ord); MemorySegment.copy(t, 0, s, JAVA_BYTE, o, 4); }
    private static void ePutLong(MemorySegment s, int o, long v, ByteOrder ord) { var t = new byte[8]; DataUtils.putLong(t, 0, v, ord); MemorySegment.copy(t, 0, s, JAVA_BYTE, o, 8); }
    private static void ePutFloat(MemorySegment s, int o, float v, ByteOrder ord) { var t = new byte[4]; DataUtils.putFloat(t, 0, v, ord); MemorySegment.copy(t, 0, s, JAVA_BYTE, o, 4); }
    private static void ePutDouble(MemorySegment s, int o, double v, ByteOrder ord) { var t = new byte[8]; DataUtils.putDouble(t, 0, v, ord); MemorySegment.copy(t, 0, s, JAVA_BYTE, o, 8); }

    /**
     * Asserts every strategy agrees bit-for-bit with the corresponding {@link DataUtils} {@code byte[]}
     * accessor on the reference bytes, for both the read and the write direction, before any timing runs.
     */
    private void verify() {
        var vs = MemorySegment.ofArray(refBytes.clone());
        switch (kind) {
            case SHORT -> {
                var exp = DataUtils.getShort(refBytes, offset, byteOrder);
                check(aGetShort(vs, offset, byteOrder) == exp, "A get short");
                check(bGetShort(vs, offset, byteOrder) == exp, "B get short");
                check(cGetShort(vs, offset, byteOrder) == exp, "C get short");
                check(dGetShort(vs, offset, byteOrder) == exp, "D get short");
                check(eGetShort(vs, offset, byteOrder) == exp, "E get short");
                var pe = refBytes.clone();
                DataUtils.putShort(pe, offset, sVal, byteOrder);
                checkPut(s -> aPutShort(s, offset, sVal, byteOrder), pe, "A put short");
                checkPut(s -> bPutShort(s, offset, sVal, byteOrder), pe, "B put short");
                checkPut(s -> cPutShort(s, offset, sVal, byteOrder), pe, "C put short");
                checkPut(s -> dPutShort(s, offset, sVal, byteOrder), pe, "D put short");
                checkPut(s -> ePutShort(s, offset, sVal, byteOrder), pe, "E put short");
            }
            case INT -> {
                var exp = DataUtils.getInt(refBytes, offset, byteOrder);
                check(aGetInt(vs, offset, byteOrder) == exp, "A get int");
                check(bGetInt(vs, offset, byteOrder) == exp, "B get int");
                check(cGetInt(vs, offset, byteOrder) == exp, "C get int");
                check(dGetInt(vs, offset, byteOrder) == exp, "D get int");
                check(eGetInt(vs, offset, byteOrder) == exp, "E get int");
                var pe = refBytes.clone();
                DataUtils.putInt(pe, offset, iVal, byteOrder);
                checkPut(s -> aPutInt(s, offset, iVal, byteOrder), pe, "A put int");
                checkPut(s -> bPutInt(s, offset, iVal, byteOrder), pe, "B put int");
                checkPut(s -> cPutInt(s, offset, iVal, byteOrder), pe, "C put int");
                checkPut(s -> dPutInt(s, offset, iVal, byteOrder), pe, "D put int");
                checkPut(s -> ePutInt(s, offset, iVal, byteOrder), pe, "E put int");
            }
            case LONG -> {
                var exp = DataUtils.getLong(refBytes, offset, byteOrder);
                check(aGetLong(vs, offset, byteOrder) == exp, "A get long");
                check(bGetLong(vs, offset, byteOrder) == exp, "B get long");
                check(cGetLong(vs, offset, byteOrder) == exp, "C get long");
                check(dGetLong(vs, offset, byteOrder) == exp, "D get long");
                check(eGetLong(vs, offset, byteOrder) == exp, "E get long");
                var pe = refBytes.clone();
                DataUtils.putLong(pe, offset, lVal, byteOrder);
                checkPut(s -> aPutLong(s, offset, lVal, byteOrder), pe, "A put long");
                checkPut(s -> bPutLong(s, offset, lVal, byteOrder), pe, "B put long");
                checkPut(s -> cPutLong(s, offset, lVal, byteOrder), pe, "C put long");
                checkPut(s -> dPutLong(s, offset, lVal, byteOrder), pe, "D put long");
                checkPut(s -> ePutLong(s, offset, lVal, byteOrder), pe, "E put long");
            }
            case FLOAT -> {
                var expBits = Float.floatToRawIntBits(DataUtils.getFloat(refBytes, offset, byteOrder));
                check(Float.floatToRawIntBits(aGetFloat(vs, offset, byteOrder)) == expBits, "A get float");
                check(Float.floatToRawIntBits(bGetFloat(vs, offset, byteOrder)) == expBits, "B get float");
                check(Float.floatToRawIntBits(cGetFloat(vs, offset, byteOrder)) == expBits, "C get float");
                check(Float.floatToRawIntBits(dGetFloat(vs, offset, byteOrder)) == expBits, "D get float");
                check(Float.floatToRawIntBits(eGetFloat(vs, offset, byteOrder)) == expBits, "E get float");
                var pe = refBytes.clone();
                DataUtils.putFloat(pe, offset, fVal, byteOrder);
                checkPut(s -> aPutFloat(s, offset, fVal, byteOrder), pe, "A put float");
                checkPut(s -> bPutFloat(s, offset, fVal, byteOrder), pe, "B put float");
                checkPut(s -> cPutFloat(s, offset, fVal, byteOrder), pe, "C put float");
                checkPut(s -> dPutFloat(s, offset, fVal, byteOrder), pe, "D put float");
                checkPut(s -> ePutFloat(s, offset, fVal, byteOrder), pe, "E put float");
            }
            case DOUBLE -> {
                var expBits = Double.doubleToRawLongBits(DataUtils.getDouble(refBytes, offset, byteOrder));
                check(Double.doubleToRawLongBits(aGetDouble(vs, offset, byteOrder)) == expBits, "A get double");
                check(Double.doubleToRawLongBits(bGetDouble(vs, offset, byteOrder)) == expBits, "B get double");
                check(Double.doubleToRawLongBits(cGetDouble(vs, offset, byteOrder)) == expBits, "C get double");
                check(Double.doubleToRawLongBits(dGetDouble(vs, offset, byteOrder)) == expBits, "D get double");
                check(Double.doubleToRawLongBits(eGetDouble(vs, offset, byteOrder)) == expBits, "E get double");
                var pe = refBytes.clone();
                DataUtils.putDouble(pe, offset, dVal, byteOrder);
                checkPut(s -> aPutDouble(s, offset, dVal, byteOrder), pe, "A put double");
                checkPut(s -> bPutDouble(s, offset, dVal, byteOrder), pe, "B put double");
                checkPut(s -> cPutDouble(s, offset, dVal, byteOrder), pe, "C put double");
                checkPut(s -> dPutDouble(s, offset, dVal, byteOrder), pe, "D put double");
                checkPut(s -> ePutDouble(s, offset, dVal, byteOrder), pe, "E put double");
            }
        }
    }

    /**
     * Replays a put strategy against a fresh heap copy of the reference bytes and asserts the resulting
     * bytes are identical to what the {@link DataUtils} {@code byte[]} accessor produced.
     */
    private void checkPut(SegOp put, byte[] expected, String name) {
        var got = refBytes.clone();
        put.run(MemorySegment.ofArray(got));
        if (!Arrays.equals(got, expected)) {
            throw new IllegalStateException("Strategy " + name + " diverges from DataUtils: " + Arrays.toString(got) + " != " + Arrays.toString(expected));
        }
    }

    /**
     * Throws when a strategy disagrees with the {@link DataUtils} reference.
     */
    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new IllegalStateException("Strategy " + name + " diverges from DataUtils");
        }
    }
}
