package com.github.auties00.cobalt.calls.media.audio.codec.mlow.dsp;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Single precision real and complex fast Fourier transform used by the MLow speech codec for its LPC
 * spectral analysis. The arithmetic reproduces the four wide SSE variant of the PFFFT library (Julien
 * Pommier's PFFFT, an SSE adaptation of NETLIB FFTPACK version 4) float rounding for float rounding.
 *
 * <p>The MLow encoder estimates the short term linear prediction spectrum of every frame by taking a
 * 512 point real forward FFT of the windowed signal, squaring the magnitude, and running a brute force DCT
 * to recover the autocorrelation. Because that autocorrelation feeds the line spectral frequency quantizer,
 * the FFT output must be bit reproducible against the reference encoder down to the float least significant
 * bit; a single differing rounding can flip an LSF index and desynchronize the decoder. For that reason this
 * reproduces the exact SIMD code path rather than the slower scalar fallback.
 *
 * <p>Vector width. The four wide SSE variant with {@link #SIMD_SZ} equal to {@code 4} is reproduced: the
 * four lane vector is modeled as four consecutive {@code float} lanes, and every vector operation (add,
 * subtract, multiply, fused multiply add, interleave, uninterleave, 4x4 transpose, high and low swap,
 * complex multiply, complex multiply by conjugate) is transcribed lane by lane in the same evaluation order,
 * so the intermediate single precision roundings match the SSE registers exactly. The scalar fallback with
 * {@link #SIMD_SZ} equal to {@code 1} is a mathematically equivalent but not bit identical sequence of
 * butterflies; measured against the SSE path on a 512 point real frame it diverges by up to {@code 8e-5}
 * relative, outside the tolerance that keeps the LSF quantizer in lock step.
 *
 * <p>Transform layout. A real transform of length {@code N} treats its {@code N} input floats as
 * {@code N / 4} vectors and runs a length {@code N / 4} core transform over them, then a finalize pass
 * combines the four interleaved sub transforms. The unordered forward output is the z domain layout that is
 * most efficient to transform back; {@link #transformOrdered(float[], float[], float[], boolean)}
 * additionally reorders it into canonical interleaved complex order. In that canonical order the real
 * transform output packs the two purely real bins together: {@code out[0]} is {@code F(0)} (DC) and
 * {@code out[1]} is {@code F(N/2)} (Nyquist), then {@code out[2*k]} and {@code out[2*k+1]} are the real and
 * imaginary parts of {@code F(k)} for {@code k} in {@code [1, N/2)}. This is the form the MLow LPC analysis
 * consumes. Transforms are unscaled: a forward followed by a backward multiplies the signal by {@code N}.
 *
 * <p>Size restrictions. A real transform requires {@code N} to be a positive multiple of
 * {@code 2 * SIMD_SZ * SIMD_SZ == 32}; a complex transform requires a multiple of
 * {@code SIMD_SZ * SIMD_SZ == 16}. After dividing by {@code SIMD_SZ} the remaining length must factor into
 * {@code 2}, {@code 3}, {@code 4}, and {@code 5}. The MLow encoder uses only the {@code N == 512} real
 * forward transform; the complex path and the backward direction are provided for completeness and validated
 * by the forward then backward identity.
 *
 * <p>A {@link Pffft} instance precomputes and holds the read only twiddle factors for one transform size and
 * type; it is immutable after construction and may be shared across threads. The {@code work} scratch buffer
 * passed to a transform is caller owned and is the only mutable state touched per call, so concurrent
 * transforms must each supply their own scratch (or {@code null} to allocate one internally).
 *
 * @implNote This implementation reproduces the four wide SSE arithmetic with {@link #SIMD_SZ} equal to
 * {@code 4}. The radix 2, 3, 4, and 5 forward and backward butterflies, the complex passes, the real and
 * complex finalize and preprocess 4x4 blocks, and the z reorder shuffles keep their reference loop and
 * indexing structure so the buffer ping pong (which of the two work buffers ends up holding the result)
 * resolves identically. Each twiddle angle is computed in {@code float} before the {@code cos}/{@code sin}
 * call, matching the reference narrowing that makes the tables bit identical and keeps the downstream line
 * spectral frequency quantization indices in lock step. Vector lanes are laid out so that vector index
 * {@code v} occupies float offsets {@code [4*v, 4*v+4)}, the same memory order an SSE register occupies,
 * which is what makes the byte level interleave and swap shuffles reproduce the SSE results.
 */
public final class Pffft {
    /**
     * The logger for {@link Pffft}.
     */
    private static final System.Logger LOGGER = Log.get(Pffft.class);

    /**
     * Number of {@code float} lanes in one SIMD vector on the SSE path.
     *
     * <p>The whole implementation is written for the four wide SSE register; the preprocess and finalize
     * stages hard code four lane shuffles, so this constant is effectively fixed at {@code 4} and is named
     * only for readability where a length is multiplied or divided by the lane count.
     */
    private static final int SIMD_SZ = 4;

    /**
     * Two times pi in {@code double} precision, used to build the twiddle factor angles.
     */
    private static final double M_2PI = 2.0 * Math.PI;

    /**
     * Square root of one half, {@code M_SQRT2 / 2}, the rotation constant of the real finalize stage.
     */
    private static final float S_HALF = (float) (Math.sqrt(2.0) / 2.0);

    /**
     * Square root of two, the rotation constant of the real preprocess stage.
     */
    private static final float S_SQRT2 = (float) Math.sqrt(2.0);

    /**
     * Marks a real valued transform.
     */
    public static final int REAL = 0;

    /**
     * Marks a complex valued transform.
     */
    public static final int COMPLEX = 1;

    /**
     * The transform length in {@code float} samples.
     */
    private final int n;

    /**
     * Number of complex SIMD vectors: {@code N/2/SIMD_SZ} for a real transform, {@code N/SIMD_SZ} for a
     * complex transform.
     */
    private final int ncvec;

    /**
     * The transform type, {@link #REAL} or {@link #COMPLEX}.
     */
    private final int transform;

    /**
     * The FFTPACK factorization descriptor.
     *
     * <p>Entry {@code 0} is the decomposed length {@code N/SIMD_SZ}, entry {@code 1} the number of factors, and
     * entries {@code 2..} the radix of each factor in application order.
     */
    private final int[] ifac;

    /**
     * The finalize and preprocess twiddle table applied per 4x4 block, of length {@code N/4*3} floats.
     */
    private final float[] e;

    /**
     * The FFTPACK twiddle table for the length {@code N/SIMD_SZ} core transform, of length {@code N/SIMD_SZ}
     * floats.
     */
    private final float[] twiddle;

    /**
     * Prepares a transform of the given length and type.
     *
     * <p>Validates that {@code n} satisfies the SIMD size constraints and the {@code 2}, {@code 3}, {@code 4},
     * {@code 5} factorization constraint, then precomputes the read only twiddle tables {@link #e} and
     * {@link #twiddle} and the factorization {@link #ifac}. The resulting instance is immutable and thread
     * safe.
     *
     * @param n         the transform length in {@code float} samples; for {@link #REAL} a positive multiple of
     *                  {@code 32}, for {@link #COMPLEX} a positive multiple of {@code 16}, and after dividing
     *                  by {@code 4} factorable into {@code 2}, {@code 3}, {@code 4}, {@code 5}
     * @param transform {@link #REAL} or {@link #COMPLEX}
     * @throws IllegalArgumentException if {@code n} violates the size or factorization constraints, or
     *                                  {@code transform} is neither {@link #REAL} nor {@link #COMPLEX}
     */
    public Pffft(int n, int transform) {
        if (transform != REAL && transform != COMPLEX) {
            throw new IllegalArgumentException("transform must be REAL or COMPLEX");
        }
        if (transform == REAL && (n <= 0 || n % (2 * SIMD_SZ * SIMD_SZ) != 0)) {
            throw new IllegalArgumentException("real transform length must be a positive multiple of 32: " + n);
        }
        if (transform == COMPLEX && (n <= 0 || n % (SIMD_SZ * SIMD_SZ) != 0)) {
            throw new IllegalArgumentException("complex transform length must be a positive multiple of 16: " + n);
        }
        this.n = n;
        this.transform = transform;
        this.ncvec = (transform == REAL ? n / 2 : n) / SIMD_SZ;
        this.e = new float[6 * ncvec];
        this.twiddle = new float[2 * ncvec];
        this.ifac = new int[15];

        for (var k = 0; k < ncvec; ++k) {
            var i = k / SIMD_SZ;
            var j = k % SIMD_SZ;
            for (var m = 0; m < SIMD_SZ - 1; ++m) {
                var a = (float) (-M_2PI * (m + 1) * k / n);
                e[(2 * (i * 3 + m) + 0) * SIMD_SZ + j] = (float) Math.cos(a);
                e[(2 * (i * 3 + m) + 1) * SIMD_SZ + j] = (float) Math.sin(a);
            }
        }
        if (transform == REAL) {
            rffti1(n / SIMD_SZ, twiddle, ifac);
        } else {
            cffti1(n / SIMD_SZ, twiddle, ifac);
        }

        var m = 1;
        for (var k = 0; k < ifac[1]; ++k) {
            m *= ifac[2 + k];
        }
        if (m != n / SIMD_SZ) {
            throw new IllegalArgumentException("length not decomposable with factors 2,3,4,5: " + n);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "pffft setup: n={0} transform={1}", n, transform == REAL ? "REAL" : "COMPLEX");
        }
    }

    /**
     * Returns the transform length in {@code float} samples.
     *
     * @return the value of {@code N} this setup was built for
     */
    public int length() {
        return n;
    }

    /**
     * Performs the transform leaving the output in FFTPACK z domain order.
     *
     * <p>The forward output is the layout most efficient to transform back or to convolve with; it is not the
     * canonical interleaved complex order. Use {@link #transformOrdered(float[], float[], float[], boolean)}
     * when the spectrum is consumed directly. Input and output may be the same array; if they alias, an extra
     * internal copy is made. The {@code work} buffer, when non-{@code null}, must hold at least
     * {@code 2 * Ncvec * SIMD_SZ} floats.
     *
     * @param input     the {@code N}-float input
     * @param output    the {@code N}-float output, possibly aliasing {@code input}
     * @param work      a scratch buffer of at least {@code 2 * Ncvec * SIMD_SZ} floats, or {@code null} to
     *                  allocate one internally
     * @param forward   {@code true} for the forward transform, {@code false} for the backward (inverse, unscaled)
     */
    public void transform(float[] input, float[] output, float[] work, boolean forward) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "pffft transform: n={0} forward={1} ordered=false", n, forward);
        transformInternal(input, output, work, forward, false);
    }

    /**
     * Performs the transform leaving the output in canonical interleaved complex order.
     *
     * <p>This is equivalent to {@link #transform(float[], float[], float[], boolean)} followed (forward) or
     * preceded (backward) by a z reorder. For a {@link #REAL} forward transform the output packs the two real
     * bins as {@code out[0] = F(0)} and {@code out[1] = F(N/2)}, then interleaved real and imaginary pairs for
     * the remaining bins. Input and output may alias. The {@code work} buffer, when non-{@code null}, must
     * hold at least {@code 2 * Ncvec * SIMD_SZ} floats.
     *
     * @param input     the {@code N}-float input
     * @param output    the {@code N}-float output, possibly aliasing {@code input}
     * @param work      a scratch buffer of at least {@code 2 * Ncvec * SIMD_SZ} floats, or {@code null} to
     *                  allocate one internally
     * @param forward   {@code true} for the forward transform, {@code false} for the backward (inverse, unscaled)
     */
    public void transformOrdered(float[] input, float[] output, float[] work, boolean forward) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "pffft transform: n={0} forward={1} ordered=true", n, forward);
        transformInternal(input, output, work, forward, true);
    }

    /**
     * Reorders a forward z domain spectrum into canonical interleaved complex order, or undoes that reorder.
     *
     * <p>Call with {@code forward == true} after a forward {@link #transform(float[], float[], float[], boolean)}
     * to get interleaved complex numbers; call with {@code forward == false} before a backward transform to put
     * an interleaved spectrum back into z domain order. Input and output must not alias.
     *
     * @param in      the {@code N}-float source spectrum
     * @param out     the {@code N}-float destination, distinct from {@code in}
     * @param forward {@code true} to reorder a forward result, {@code false} to prepare a backward input
     */
    public void zreorder(float[] in, float[] out, boolean forward) {
        int dk;
        if (transform == REAL) {
            dk = n / 32;
            if (forward) {
                for (var k = 0; k < dk; ++k) {
                    interleave2(in, (k * 8 + 0) * 4, in, (k * 8 + 1) * 4, out, (2 * (0 * dk + k) + 0) * 4, out, (2 * (0 * dk + k) + 1) * 4);
                    interleave2(in, (k * 8 + 4) * 4, in, (k * 8 + 5) * 4, out, (2 * (2 * dk + k) + 0) * 4, out, (2 * (2 * dk + k) + 1) * 4);
                }
                reversedCopy(dk, in, 2 * 4, 8, out, n / 2);
                reversedCopy(dk, in, 6 * 4, 8, out, n);
            } else {
                for (var k = 0; k < dk; ++k) {
                    uninterleave2(in, (2 * (0 * dk + k) + 0) * 4, in, (2 * (0 * dk + k) + 1) * 4, out, (k * 8 + 0) * 4, out, (k * 8 + 1) * 4);
                    uninterleave2(in, (2 * (2 * dk + k) + 0) * 4, in, (2 * (2 * dk + k) + 1) * 4, out, (k * 8 + 4) * 4, out, (k * 8 + 5) * 4);
                }
                unreversedCopy(dk, in, n / 4, out, n - 6 * SIMD_SZ, -8);
                unreversedCopy(dk, in, 3 * n / 4, out, n - 2 * SIMD_SZ, -8);
            }
        } else {
            if (forward) {
                for (var k = 0; k < ncvec; ++k) {
                    var kk = (k / 4) + (k % 4) * (ncvec / 4);
                    interleave2(in, (k * 2) * 4, in, (k * 2 + 1) * 4, out, (kk * 2) * 4, out, (kk * 2 + 1) * 4);
                }
            } else {
                for (var k = 0; k < ncvec; ++k) {
                    var kk = (k / 4) + (k % 4) * (ncvec / 4);
                    uninterleave2(in, (kk * 2) * 4, in, (kk * 2 + 1) * 4, out, (k * 2) * 4, out, (k * 2 + 1) * 4);
                }
            }
        }
    }

    /**
     * Runs the full transform with optional ordering.
     *
     * <p>Drives the core FFTPACK pass, the finalize or preprocess stage, and the optional z reorder, tracking
     * which of the two work buffers ends up holding the result through the buffer ping pong. A final copy
     * handles the aliasing case where {@code input == output} left the result in the scratch buffer.
     *
     * @param finput    the {@code N}-float input
     * @param foutput   the {@code N}-float output, possibly aliasing {@code finput}
     * @param scratch   a scratch buffer of at least {@code 2 * Ncvec * SIMD_SZ} floats, or {@code null}
     * @param forward   {@code true} for the forward transform, {@code false} for the backward
     * @param ordered   {@code true} to leave the output in canonical interleaved complex order
     */
    private void transformInternal(float[] finput, float[] foutput, float[] scratch, boolean forward, boolean ordered) {
        var nfOdd = (ifac[1] & 1) != 0;
        var scratchBuf = scratch != null ? scratch : new float[2 * ncvec * SIMD_SZ];

        var buff0 = foutput;
        var buff1 = scratchBuf;
        var ib = (nfOdd ^ ordered) ? 1 : 0;

        if (forward) {
            ib = ib == 0 ? 1 : 0;
            if (transform == REAL) {
                var r = rfftf1(ncvec * 2, finput, sel(buff0, buff1, ib), sel(buff0, buff1, 1 - ib), twiddle, ifac);
                ib = (r == buff0) ? 0 : 1;
                realFinalize(ncvec, sel(buff0, buff1, ib), sel(buff0, buff1, 1 - ib), e);
            } else {
                var tmp = sel(buff0, buff1, ib);
                for (var k = 0; k < ncvec; ++k) {
                    uninterleave2(finput, (k * 2) * 4, finput, (k * 2 + 1) * 4, tmp, (k * 2) * 4, tmp, (k * 2 + 1) * 4);
                }
                var r = cfftf1(ncvec, sel(buff0, buff1, ib), sel(buff0, buff1, 1 - ib), sel(buff0, buff1, ib), twiddle, ifac, -1);
                ib = (r == buff0) ? 0 : 1;
                cplxFinalize(ncvec, sel(buff0, buff1, ib), sel(buff0, buff1, 1 - ib), e);
            }
            if (ordered) {
                zreorder(sel(buff0, buff1, 1 - ib), sel(buff0, buff1, ib), true);
            } else {
                ib = 1 - ib;
            }
        } else {
            var vinput = finput;
            if (vinput == sel(buff0, buff1, ib)) {
                ib = 1 - ib;
            }
            if (ordered) {
                zreorder(vinput, sel(buff0, buff1, ib), false);
                vinput = sel(buff0, buff1, ib);
                ib = 1 - ib;
            }
            if (transform == REAL) {
                realPreprocess(ncvec, vinput, sel(buff0, buff1, ib), e);
                var r = rfftb1(ncvec * 2, sel(buff0, buff1, ib), buff0, buff1, twiddle, ifac);
                ib = (r == buff0) ? 0 : 1;
            } else {
                cplxPreprocess(ncvec, vinput, sel(buff0, buff1, ib), e);
                var r = cfftf1(ncvec, sel(buff0, buff1, ib), buff0, buff1, twiddle, ifac, +1);
                ib = (r == buff0) ? 0 : 1;
                var b = sel(buff0, buff1, ib);
                for (var k = 0; k < ncvec; ++k) {
                    interleave2(b, (k * 2) * 4, b, (k * 2 + 1) * 4, b, (k * 2) * 4, b, (k * 2 + 1) * 4);
                }
            }
        }

        var result = sel(buff0, buff1, ib);
        if (result != foutput) {
            for (var k = 0; k < ncvec; ++k) {
                var a0 = result[(2 * k) * 4 + 0];
                var a1 = result[(2 * k) * 4 + 1];
                var a2 = result[(2 * k) * 4 + 2];
                var a3 = result[(2 * k) * 4 + 3];
                var b0 = result[(2 * k + 1) * 4 + 0];
                var b1 = result[(2 * k + 1) * 4 + 1];
                var b2 = result[(2 * k + 1) * 4 + 2];
                var b3 = result[(2 * k + 1) * 4 + 3];
                foutput[(2 * k) * 4 + 0] = a0;
                foutput[(2 * k) * 4 + 1] = a1;
                foutput[(2 * k) * 4 + 2] = a2;
                foutput[(2 * k) * 4 + 3] = a3;
                foutput[(2 * k + 1) * 4 + 0] = b0;
                foutput[(2 * k + 1) * 4 + 1] = b1;
                foutput[(2 * k + 1) * 4 + 2] = b2;
                foutput[(2 * k + 1) * 4 + 3] = b3;
            }
        }
    }

    /**
     * Selects one of the two work buffers by index.
     *
     * @param b0 the buffer at index {@code 0} (the output array)
     * @param b1 the buffer at index {@code 1} (the scratch array)
     * @param ib the index, {@code 0} or {@code 1}
     * @return {@code b0} when {@code ib} is {@code 0}, otherwise {@code b1}
     */
    private static float[] sel(float[] b0, float[] b1, int ib) {
        return ib == 0 ? b0 : b1;
    }

    /**
     * Interleaves two four lane vectors at the given float offsets.
     *
     * <p>Given inputs {@code [a0 a1 a2 a3]} and {@code [b0 b1 b2 b3]}, writes {@code out1 = [a0 b0 a1 b1]} and
     * {@code out2 = [a2 b2 a3 b3]}. The two outputs are computed into temporaries first so the operation is
     * safe when an output aliases an input.
     *
     * @param in1 the first input array
     * @param oi1 the first input vector offset
     * @param in2 the second input array
     * @param oi2 the second input vector offset
     * @param o1  the first output array
     * @param oo1 the first output vector offset
     * @param o2  the second output array
     * @param oo2 the second output vector offset
     */
    private static void interleave2(float[] in1, int oi1, float[] in2, int oi2, float[] o1, int oo1, float[] o2, int oo2) {
        float t0 = in1[oi1], t1 = in2[oi2], t2 = in1[oi1 + 1], t3 = in2[oi2 + 1];
        float u0 = in1[oi1 + 2], u1 = in2[oi2 + 2], u2 = in1[oi1 + 3], u3 = in2[oi2 + 3];
        o1[oo1] = t0;
        o1[oo1 + 1] = t1;
        o1[oo1 + 2] = t2;
        o1[oo1 + 3] = t3;
        o2[oo2] = u0;
        o2[oo2 + 1] = u1;
        o2[oo2 + 2] = u2;
        o2[oo2 + 3] = u3;
    }

    /**
     * Uninterleaves two four lane vectors at the given float offsets.
     *
     * <p>Given inputs {@code [a0 a1 a2 a3]} and {@code [b0 b1 b2 b3]}, writes {@code out1 = [a0 a2 b0 b2]} and
     * {@code out2 = [a1 a3 b1 b3]}. Outputs are staged in temporaries so the operation is alias safe.
     *
     * @param in1 the first input array
     * @param oi1 the first input vector offset
     * @param in2 the second input array
     * @param oi2 the second input vector offset
     * @param o1  the first output array
     * @param oo1 the first output vector offset
     * @param o2  the second output array
     * @param oo2 the second output vector offset
     */
    private static void uninterleave2(float[] in1, int oi1, float[] in2, int oi2, float[] o1, int oo1, float[] o2, int oo2) {
        float t0 = in1[oi1], t1 = in1[oi1 + 2], t2 = in2[oi2], t3 = in2[oi2 + 2];
        float u0 = in1[oi1 + 1], u1 = in1[oi1 + 3], u2 = in2[oi2 + 1], u3 = in2[oi2 + 3];
        o1[oo1] = t0;
        o1[oo1 + 1] = t1;
        o1[oo1 + 2] = t2;
        o1[oo1 + 3] = t3;
        o2[oo2] = u0;
        o2[oo2 + 1] = u1;
        o2[oo2 + 2] = u2;
        o2[oo2 + 3] = u3;
    }

    /**
     * Builds the real transform twiddle table.
     *
     * <p>Decomposes {@code n} with the real forward factor preference {@code 4,2,3,5} and fills {@code wa} with
     * the cosine and sine twiddle factors for each factor stage. The angle {@code fi*argld} is formed in
     * {@code float} before the {@code cos}/{@code sin} call, so its rounding matches the reference tables.
     *
     * @param nn   the decomposed length {@code N/SIMD_SZ}
     * @param wa   the twiddle output of length {@code nn}
     * @param ifac the factorization descriptor to fill
     */
    private static void rffti1(int nn, float[] wa, int[] ifac) {
        var ntryh = new int[]{4, 2, 3, 5, 0};
        var nf = decompose(nn, ifac, ntryh);
        var argh = (float) (M_2PI / nn);
        var is = 0;
        var nfm1 = nf - 1;
        var l1 = 1;
        for (var k1 = 1; k1 <= nfm1; k1++) {
            var ip = ifac[k1 + 1];
            var ld = 0;
            var l2 = l1 * ip;
            var ido = nn / l2;
            var ipm = ip - 1;
            for (var j = 1; j <= ipm; ++j) {
                var i = is;
                var fi = 0;
                ld += l1;
                var argld = ld * argh;
                for (var ii = 3; ii <= ido; ii += 2) {
                    i += 2;
                    fi += 1;
                    wa[i - 2] = (float) Math.cos(fi * argld);
                    wa[i - 1] = (float) Math.sin(fi * argld);
                }
                is += ido;
            }
            l1 = l2;
        }
    }

    /**
     * Builds the complex transform twiddle table.
     *
     * <p>Decomposes {@code n} with the complex factor preference {@code 5,3,4,2} and fills {@code wa} with the
     * twiddle factors of each stage, including the leading unit twiddle and the {@code ip > 5} duplication
     * branch.
     *
     * @param nn   the decomposed length {@code N/SIMD_SZ}
     * @param wa   the twiddle output of length {@code 2 * nn}
     * @param ifac the factorization descriptor to fill
     */
    private static void cffti1(int nn, float[] wa, int[] ifac) {
        var ntryh = new int[]{5, 3, 4, 2, 0};
        var nf = decompose(nn, ifac, ntryh);
        var argh = (float) (M_2PI / nn);
        var i = 1;
        var l1 = 1;
        for (var k1 = 1; k1 <= nf; k1++) {
            var ip = ifac[k1 + 1];
            var ld = 0;
            var l2 = l1 * ip;
            var ido = nn / l2;
            var idot = ido + ido + 2;
            var ipm = ip - 1;
            for (var j = 1; j <= ipm; j++) {
                var i1 = i;
                var fi = 0;
                wa[i - 1] = 1;
                wa[i] = 0;
                ld += l1;
                var argld = ld * argh;
                for (var ii = 4; ii <= idot; ii += 2) {
                    i += 2;
                    fi += 1;
                    wa[i - 1] = (float) Math.cos(fi * argld);
                    wa[i] = (float) Math.sin(fi * argld);
                }
                if (ip > 5) {
                    wa[i1 - 1] = wa[i - 1];
                    wa[i1] = wa[i];
                }
            }
            l1 = l2;
        }
    }

    /**
     * Factorizes a length into the allowed radices.
     *
     * <p>Greedily peels factors from {@code ntryh} (terminated by a zero), recording each radix in
     * {@code ifac}. The {@code ntry == 2 && nf != 1} branch moves an extracted factor of two to the front of
     * the factor list, as FFTPACK does.
     *
     * @param nn    the length to factorize
     * @param ifac  the descriptor to fill ({@code ifac[0] = nn}, {@code ifac[1] = factor count}, then the
     *              factors)
     * @param ntryh the zero terminated radix preference order
     * @return the number of factors found
     */
    private static int decompose(int nn, int[] ifac, int[] ntryh) {
        int nl = nn, nf = 0;
        for (var j = 0; ntryh[j] != 0; ++j) {
            var ntry = ntryh[j];
            while (nl != 1) {
                var nq = nl / ntry;
                var nr = nl - ntry * nq;
                if (nr == 0) {
                    ifac[2 + nf++] = ntry;
                    nl = nq;
                    if (ntry == 2 && nf != 1) {
                        for (var i = 2; i <= nf; ++i) {
                            var ib = nf - i + 2;
                            ifac[ib + 1] = ifac[ib];
                        }
                        ifac[2] = 2;
                    }
                } else {
                    break;
                }
            }
        }
        ifac[0] = nn;
        ifac[1] = nf;
        return nf;
    }

    /**
     * Runs the real forward FFTPACK core.
     *
     * <p>Applies the factor passes from the last factor to the first, ping ponging between the two work
     * buffers, and returns the buffer that holds the result. The vector length {@code nn} counts four lane
     * vectors, so all offsets are scaled by four when indexing the flat float arrays.
     *
     * @param nn    the transform length in vectors
     * @param input the read only input buffer (vectors)
     * @param work1 the first work buffer
     * @param work2 the second work buffer
     * @param wa    the twiddle table
     * @param ifac  the factorization descriptor
     * @return whichever of {@code work1} or {@code work2} holds the result
     */
    private static float[] rfftf1(int nn, float[] input, float[] work1, float[] work2, float[] wa, int[] ifac) {
        var in = input;
        var out = (in == work2) ? work1 : work2;
        var nf = ifac[1];
        var l2 = nn;
        var iw = nn - 1;
        for (var k1 = 1; k1 <= nf; ++k1) {
            var kh = nf - k1;
            var ip = ifac[kh + 2];
            var l1 = l2 / ip;
            var ido = nn / l2;
            iw -= (ip - 1) * ido;
            switch (ip) {
                case 5 -> {
                    int ix2 = iw + ido, ix3 = ix2 + ido, ix4 = ix3 + ido;
                    radf5(ido, l1, in, out, wa, iw, ix2, ix3, ix4);
                }
                case 4 -> {
                    int ix2 = iw + ido, ix3 = ix2 + ido;
                    radf4(ido, l1, in, out, wa, iw, ix2, ix3);
                }
                case 3 -> {
                    var ix2 = iw + ido;
                    radf3(ido, l1, in, out, wa, iw, ix2);
                }
                case 2 -> radf2(ido, l1, in, out, wa, iw);
                default -> throw new IllegalStateException("unsupported radix " + ip);
            }
            l2 = l1;
            if (out == work2) {
                out = work1;
                in = work2;
            } else {
                out = work2;
                in = work1;
            }
        }
        return in;
    }

    /**
     * Runs the real backward FFTPACK core.
     *
     * <p>Applies the factor passes from the first factor to the last, ping ponging between the two work
     * buffers, and returns the buffer that holds the result.
     *
     * @param nn    the transform length in vectors
     * @param input the read only input buffer (vectors)
     * @param work1 the first work buffer
     * @param work2 the second work buffer
     * @param wa    the twiddle table
     * @param ifac  the factorization descriptor
     * @return whichever of {@code work1} or {@code work2} holds the result
     */
    private static float[] rfftb1(int nn, float[] input, float[] work1, float[] work2, float[] wa, int[] ifac) {
        var in = input;
        var out = (in == work2) ? work1 : work2;
        var nf = ifac[1];
        var l1 = 1;
        var iw = 0;
        for (var k1 = 1; k1 <= nf; k1++) {
            var ip = ifac[k1 + 1];
            var l2 = ip * l1;
            var ido = nn / l2;
            switch (ip) {
                case 5 -> {
                    int ix2 = iw + ido, ix3 = ix2 + ido, ix4 = ix3 + ido;
                    radb5(ido, l1, in, out, wa, iw, ix2, ix3, ix4);
                }
                case 4 -> {
                    int ix2 = iw + ido, ix3 = ix2 + ido;
                    radb4(ido, l1, in, out, wa, iw, ix2, ix3);
                }
                case 3 -> {
                    var ix2 = iw + ido;
                    radb3(ido, l1, in, out, wa, iw, ix2);
                }
                case 2 -> radb2(ido, l1, in, out, wa, iw);
                default -> throw new IllegalStateException("unsupported radix " + ip);
            }
            l1 = l2;
            iw += (ip - 1) * ido;
            if (out == work2) {
                out = work1;
                in = work2;
            } else {
                out = work2;
                in = work1;
            }
        }
        return in;
    }

    /**
     * Runs the complex FFTPACK core.
     *
     * <p>Applies the complex factor passes, ping ponging between the two work buffers, and returns the buffer
     * that holds the result. The {@code isign} selects forward ({@code -1}) or backward ({@code +1}).
     *
     * @param nn    the transform length in complex vectors
     * @param input the read only input buffer
     * @param work1 the first work buffer
     * @param work2 the second work buffer
     * @param wa    the twiddle table
     * @param ifac  the factorization descriptor
     * @param isign {@code -1} for forward, {@code +1} for backward
     * @return whichever of {@code work1} or {@code work2} holds the result
     */
    private static float[] cfftf1(int nn, float[] input, float[] work1, float[] work2, float[] wa, int[] ifac, int isign) {
        var in = input;
        var out = (in == work2) ? work1 : work2;
        var nf = ifac[1];
        var l1 = 1;
        var iw = 0;
        for (var k1 = 2; k1 <= nf + 1; k1++) {
            var ip = ifac[k1];
            var l2 = ip * l1;
            var ido = nn / l2;
            var idot = ido + ido;
            switch (ip) {
                case 5 -> {
                    int ix2 = iw + idot, ix3 = ix2 + idot, ix4 = ix3 + idot;
                    passf5(idot, l1, in, out, wa, iw, ix2, ix3, ix4, isign);
                }
                case 4 -> {
                    int ix2 = iw + idot, ix3 = ix2 + idot;
                    passf4(idot, l1, in, out, wa, iw, ix2, ix3, isign);
                }
                case 3 -> {
                    var ix2 = iw + idot;
                    passf3(idot, l1, in, out, wa, iw, ix2, isign);
                }
                case 2 -> passf2(idot, l1, in, out, wa, iw, isign);
                default -> throw new IllegalStateException("unsupported radix " + ip);
            }
            l1 = l2;
            iw += (ip - 1) * idot;
            if (out == work2) {
                out = work1;
                in = work2;
            } else {
                out = work2;
                in = work1;
            }
        }
        return in;
    }

    /**
     * Loads a vector from an array at a vector offset into a four lane register.
     *
     * @param r   the destination register
     * @param a   the source array
     * @param off the source vector offset in floats
     */
    private static void ld(float[] r, float[] a, int off) {
        r[0] = a[off];
        r[1] = a[off + 1];
        r[2] = a[off + 2];
        r[3] = a[off + 3];
    }

    /**
     * Stores a four lane register into an array at a vector offset.
     *
     * @param a   the destination array
     * @param off the destination vector offset in floats
     * @param r   the source register
     */
    private static void st(float[] a, int off, float[] r) {
        a[off] = r[0];
        a[off + 1] = r[1];
        a[off + 2] = r[2];
        a[off + 3] = r[3];
    }

    /**
     * Adds two registers lane by lane into a destination register.
     *
     * @param d the destination register
     * @param a the first operand register
     * @param b the second operand register
     */
    private static void radd(float[] d, float[] a, float[] b) {
        d[0] = a[0] + b[0];
        d[1] = a[1] + b[1];
        d[2] = a[2] + b[2];
        d[3] = a[3] + b[3];
    }

    /**
     * Subtracts one register from another lane by lane into a destination register.
     *
     * @param d the destination register
     * @param a the minuend register
     * @param b the subtrahend register
     */
    private static void rsub(float[] d, float[] a, float[] b) {
        d[0] = a[0] - b[0];
        d[1] = a[1] - b[1];
        d[2] = a[2] - b[2];
        d[3] = a[3] - b[3];
    }

    /**
     * Multiplies a register by a broadcast scalar into a destination register.
     *
     * @param d the destination register
     * @param f the scalar broadcast to all lanes
     * @param a the operand register
     */
    private static void rsvmul(float[] d, float f, float[] a) {
        d[0] = f * a[0];
        d[1] = f * a[1];
        d[2] = f * a[2];
        d[3] = f * a[3];
    }

    /**
     * Multiplies one register by another lane by lane into a destination register.
     *
     * @param d the destination register
     * @param a the first operand register
     * @param b the second operand register
     */
    private static void rmul(float[] d, float[] a, float[] b) {
        d[0] = a[0] * b[0];
        d[1] = a[1] * b[1];
        d[2] = a[2] * b[2];
        d[3] = a[3] * b[3];
    }

    /**
     * Fused multiply add of three registers into a destination register, computing {@code a * b + c} lane by
     * lane.
     *
     * @param d the destination register
     * @param a the first multiplicand register
     * @param b the second multiplicand register
     * @param c the addend register
     */
    private static void rmadd(float[] d, float[] a, float[] b, float[] c) {
        d[0] = a[0] * b[0] + c[0];
        d[1] = a[1] * b[1] + c[1];
        d[2] = a[2] * b[2] + c[2];
        d[3] = a[3] * b[3] + c[3];
    }

    /**
     * Broadcasts a scalar to all four lanes of a register.
     *
     * @param d the destination register
     * @param f the scalar
     */
    private static void ldps1(float[] d, float f) {
        d[0] = f;
        d[1] = f;
        d[2] = f;
        d[3] = f;
    }

    /**
     * Multiplies a complex register pair by a broadcast complex scalar in place.
     *
     * @param ar the real part register, overwritten with the product real part
     * @param ai the imaginary part register, overwritten with the product imaginary part
     * @param br the broadcast real twiddle
     * @param bi the broadcast imaginary twiddle
     */
    private static void rcplxMul(float[] ar, float[] ai, float br, float bi) {
        for (var l = 0; l < 4; l++) {
            float r = ar[l], i = ai[l];
            var tmp = r * bi;
            r = r * br;
            r = r - i * bi;
            i = i * br;
            i = i + tmp;
            ar[l] = r;
            ai[l] = i;
        }
    }

    /**
     * Multiplies a complex register pair by the conjugate of a broadcast complex scalar in place.
     *
     * @param ar the real part register, overwritten with the product real part
     * @param ai the imaginary part register, overwritten with the product imaginary part
     * @param br the broadcast real twiddle
     * @param bi the broadcast imaginary twiddle
     */
    private static void rcplxMulConj(float[] ar, float[] ai, float br, float bi) {
        for (var l = 0; l < 4; l++) {
            float r = ar[l], i = ai[l];
            var tmp = r * bi;
            r = r * br;
            r = r + i * bi;
            i = i * br;
            i = i - tmp;
            ar[l] = r;
            ai[l] = i;
        }
    }

    /**
     * Applies the radix 2 real forward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the twiddle base offset for this stage
     */
    private static void radf2(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1) {
        var minusOne = -1.0f;
        var l1ido = l1 * ido;
        float[] a = new float[4], b = new float[4], t = new float[4];
        for (var k = 0; k < l1ido; k += ido) {
            ld(a, cc, k * 4);
            ld(b, cc, (k + l1ido) * 4);
            radd(t, a, b);
            st(ch, (2 * k) * 4, t);
            rsub(t, a, b);
            st(ch, (2 * (k + ido) - 1) * 4, t);
        }
        if (ido < 2) {
            return;
        }
        if (ido != 2) {
            float[] tr2 = new float[4], ti2 = new float[4], br = new float[4], bi = new float[4];
            for (var k = 0; k < l1ido; k += ido) {
                for (var i = 2; i < ido; i += 2) {
                    ld(tr2, cc, (i - 1 + k + l1ido) * 4);
                    ld(ti2, cc, (i + k + l1ido) * 4);
                    ld(br, cc, (i - 1 + k) * 4);
                    ld(bi, cc, (i + k) * 4);
                    rcplxMulConj(tr2, ti2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                    radd(t, bi, ti2);
                    st(ch, (i + 2 * k) * 4, t);
                    rsub(t, ti2, bi);
                    st(ch, (2 * (k + ido) - i) * 4, t);
                    radd(t, br, tr2);
                    st(ch, (i - 1 + 2 * k) * 4, t);
                    rsub(t, br, tr2);
                    st(ch, (2 * (k + ido) - i - 1) * 4, t);
                }
            }
            if (ido % 2 == 1) {
                return;
            }
        }
        for (var k = 0; k < l1ido; k += ido) {
            ld(a, cc, (ido - 1 + k + l1ido) * 4);
            rsvmul(t, minusOne, a);
            st(ch, (2 * k + ido) * 4, t);
            ld(a, cc, (k + ido - 1) * 4);
            st(ch, (2 * k + ido - 1) * 4, a);
        }
    }

    /**
     * Applies the radix 3 real forward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     */
    private static void radf3(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2) {
        var taur = -0.5f;
        var taui = 0.866025403784439f;
        float[] cr2 = new float[4], dr2 = new float[4], di2 = new float[4], dr3 = new float[4], di3 = new float[4];
        float[] ci2 = new float[4], tr2 = new float[4], ti2 = new float[4], tr3 = new float[4], ti3 = new float[4];
        float[] tA = new float[4], tB = new float[4], tmp = new float[4];
        for (var k = 0; k < l1; k++) {
            ld(tA, cc, (k + l1) * ido * 4);
            ld(tB, cc, (k + 2 * l1) * ido * 4);
            radd(cr2, tA, tB);
            ld(tmp, cc, k * ido * 4);
            radd(tA, tmp, cr2);
            st(ch, 3 * k * ido * 4, tA);
            ld(tA, cc, (k + l1 * 2) * ido * 4);
            ld(tB, cc, (k + l1) * ido * 4);
            rsub(tmp, tA, tB);
            rsvmul(tA, taui, tmp);
            st(ch, (3 * k + 2) * ido * 4, tA);
            ld(tmp, cc, k * ido * 4);
            rsvmul(tA, taur, cr2);
            radd(tB, tmp, tA);
            st(ch, (ido - 1 + (3 * k + 1) * ido) * 4, tB);
        }
        if (ido == 1) {
            return;
        }
        for (var k = 0; k < l1; k++) {
            for (var i = 2; i < ido; i += 2) {
                var ic = ido - i;
                ld(dr2, cc, (i - 1 + (k + l1) * ido) * 4);
                ld(di2, cc, (i + (k + l1) * ido) * 4);
                rcplxMulConj(dr2, di2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                ld(dr3, cc, (i - 1 + (k + l1 * 2) * ido) * 4);
                ld(di3, cc, (i + (k + l1 * 2) * ido) * 4);
                rcplxMulConj(dr3, di3, wa[wa2 + i - 2], wa[wa2 + i - 1]);
                radd(cr2, dr2, dr3);
                radd(ci2, di2, di3);
                ld(tmp, cc, (i - 1 + k * ido) * 4);
                radd(tA, tmp, cr2);
                st(ch, (i - 1 + 3 * k * ido) * 4, tA);
                ld(tmp, cc, (i + k * ido) * 4);
                radd(tA, tmp, ci2);
                st(ch, (i + 3 * k * ido) * 4, tA);
                ld(tmp, cc, (i - 1 + k * ido) * 4);
                rsvmul(tA, taur, cr2);
                radd(tr2, tmp, tA);
                ld(tmp, cc, (i + k * ido) * 4);
                rsvmul(tA, taur, ci2);
                radd(ti2, tmp, tA);
                rsub(tmp, di2, di3);
                rsvmul(tr3, taui, tmp);
                rsub(tmp, dr3, dr2);
                rsvmul(ti3, taui, tmp);
                radd(tA, tr2, tr3);
                st(ch, (i - 1 + (3 * k + 2) * ido) * 4, tA);
                rsub(tA, tr2, tr3);
                st(ch, (ic - 1 + (3 * k + 1) * ido) * 4, tA);
                radd(tA, ti2, ti3);
                st(ch, (i + (3 * k + 2) * ido) * 4, tA);
                rsub(tA, ti3, ti2);
                st(ch, (ic + (3 * k + 1) * ido) * 4, tA);
            }
        }
    }

    /**
     * Applies the radix 4 real forward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     * @param wa3 the third twiddle base offset
     */
    private static void radf4(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3) {
        var minusHsqt2 = (float) -0.7071067811865475;
        var l1ido = l1 * ido;
        float[] a0 = new float[4], a1 = new float[4], a2 = new float[4], a3 = new float[4];
        float[] tr1 = new float[4], tr2 = new float[4], t = new float[4];
        {
            int ccp = 0, chp = 0;
            while (ccp < l1ido) {
                ld(a0, cc, (ccp + 0) * 4);
                ld(a1, cc, (ccp + l1ido) * 4);
                ld(a2, cc, (ccp + 2 * l1ido) * 4);
                ld(a3, cc, (ccp + 3 * l1ido) * 4);
                radd(tr1, a1, a3);
                radd(tr2, a0, a2);
                rsub(t, a0, a2);
                st(ch, (chp + 2 * ido - 1) * 4, t);
                rsub(t, a3, a1);
                st(ch, (chp + 2 * ido) * 4, t);
                radd(t, tr1, tr2);
                st(ch, (chp + 0) * 4, t);
                rsub(t, tr2, tr1);
                st(ch, (chp + 4 * ido - 1) * 4, t);
                ccp += ido;
                chp += 4 * ido;
            }
        }
        if (ido < 2) {
            return;
        }
        if (ido != 2) {
            float[] cr2 = new float[4], ci2 = new float[4], cr3 = new float[4], ci3 = new float[4];
            float[] cr4 = new float[4], ci4 = new float[4];
            float[] ti1 = new float[4], ti2 = new float[4], tr3 = new float[4], ti3 = new float[4], ti4 = new float[4], tr4 = new float[4];
            for (var k = 0; k < l1ido; k += ido) {
                var pc = 1 + k;
                for (var i = 2; i < ido; i += 2) {
                    var ic = ido - i;
                    ld(cr2, cc, (pc + 1 * l1ido + 0) * 4);
                    ld(ci2, cc, (pc + 1 * l1ido + 1) * 4);
                    rcplxMulConj(cr2, ci2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                    ld(cr3, cc, (pc + 2 * l1ido + 0) * 4);
                    ld(ci3, cc, (pc + 2 * l1ido + 1) * 4);
                    rcplxMulConj(cr3, ci3, wa[wa2 + i - 2], wa[wa2 + i - 1]);
                    ld(cr4, cc, (pc + 3 * l1ido) * 4);
                    ld(ci4, cc, (pc + 3 * l1ido + 1) * 4);
                    rcplxMulConj(cr4, ci4, wa[wa3 + i - 2], wa[wa3 + i - 1]);
                    radd(tr1, cr2, cr4);
                    rsub(tr4, cr4, cr2);
                    ld(t, cc, (pc + 0) * 4);
                    radd(tr2, t, cr3);
                    rsub(tr3, t, cr3);
                    radd(t, tr1, tr2);
                    st(ch, (i - 1 + 4 * k) * 4, t);
                    rsub(t, tr2, tr1);
                    st(ch, (ic - 1 + 4 * k + 3 * ido) * 4, t);
                    radd(ti1, ci2, ci4);
                    rsub(ti4, ci2, ci4);
                    radd(t, ti4, tr3);
                    st(ch, (i - 1 + 4 * k + 2 * ido) * 4, t);
                    rsub(t, tr3, ti4);
                    st(ch, (ic - 1 + 4 * k + 1 * ido) * 4, t);
                    ld(t, cc, (pc + 1) * 4);
                    radd(ti2, t, ci3);
                    rsub(ti3, t, ci3);
                    radd(t, ti1, ti2);
                    st(ch, (i + 4 * k) * 4, t);
                    rsub(t, ti1, ti2);
                    st(ch, (ic + 4 * k + 3 * ido) * 4, t);
                    radd(t, tr4, ti3);
                    st(ch, (i + 4 * k + 2 * ido) * 4, t);
                    rsub(t, tr4, ti3);
                    st(ch, (ic + 4 * k + 1 * ido) * 4, t);
                    pc += 2;
                }
            }
            if (ido % 2 == 1) {
                return;
            }
        }
        float[] aa = new float[4], bb = new float[4], cc4 = new float[4], dd = new float[4], ti1 = new float[4];
        for (var k = 0; k < l1ido; k += ido) {
            ld(aa, cc, (ido - 1 + k + l1ido) * 4);
            ld(bb, cc, (ido - 1 + k + 3 * l1ido) * 4);
            ld(cc4, cc, (ido - 1 + k) * 4);
            ld(dd, cc, (ido - 1 + k + 2 * l1ido) * 4);
            radd(t, aa, bb);
            rsvmul(ti1, minusHsqt2, t);
            rsub(t, bb, aa);
            rsvmul(tr1, minusHsqt2, t);
            radd(t, tr1, cc4);
            st(ch, (ido - 1 + 4 * k) * 4, t);
            rsub(t, cc4, tr1);
            st(ch, (ido - 1 + 4 * k + 2 * ido) * 4, t);
            rsub(t, ti1, dd);
            st(ch, (4 * k + 1 * ido) * 4, t);
            radd(t, ti1, dd);
            st(ch, (4 * k + 3 * ido) * 4, t);
        }
    }

    /**
     * Applies the radix 5 real forward butterfly.
     *
     * <p>The one based index helpers {@link #ccRef(int, int, int, int, int)} and
     * {@link #chRef(int, int, int, int)} address the input and output vectors; the base offsets
     * {@code ccOffset} and {@code chOffset} carry the leading adjustment so the index expressions stay one
     * based.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     * @param wa3 the third twiddle base offset
     * @param wa4 the fourth twiddle base offset
     */
    private static void radf5(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3, int wa4) {
        float tr11 = .309016994374947f, ti11 = .951056516295154f, tr12 = -.809016994374947f, ti12 = .587785252292473f;
        var chOffset = 1 + ido * 6;
        var ccOffset = 1 + ido * (1 + l1);
        float[] cr2 = new float[4], ci5 = new float[4], cr3 = new float[4], ci4 = new float[4];
        float[] cr4 = new float[4], cr5 = new float[4], ci2 = new float[4], ci3 = new float[4];
        float[] dr2 = new float[4], di2 = new float[4], dr3 = new float[4], di3 = new float[4];
        float[] dr4 = new float[4], di4 = new float[4], dr5 = new float[4], di5 = new float[4];
        float[] tr2 = new float[4], ti2 = new float[4], tr3 = new float[4], ti3 = new float[4];
        float[] tr4 = new float[4], ti4 = new float[4], tr5 = new float[4], ti5 = new float[4];
        float[] tA = new float[4], tB = new float[4], tmp = new float[4], one = new float[4];
        for (var k = 1; k <= l1; ++k) {
            ld(tA, cc, (ccRef(ido, l1, 1, k, 5) - ccOffset) * 4);
            ld(tB, cc, (ccRef(ido, l1, 1, k, 2) - ccOffset) * 4);
            radd(cr2, tA, tB);
            rsub(ci5, tA, tB);
            ld(tA, cc, (ccRef(ido, l1, 1, k, 4) - ccOffset) * 4);
            ld(tB, cc, (ccRef(ido, l1, 1, k, 3) - ccOffset) * 4);
            radd(cr3, tA, tB);
            rsub(ci4, tA, tB);
            ld(tmp, cc, (ccRef(ido, l1, 1, k, 1) - ccOffset) * 4);
            radd(tA, cr2, cr3);
            radd(one, tmp, tA);
            st(ch, (chRef(ido, 1, 1, k) - chOffset) * 4, one);
            rsvmul(tA, tr11, cr2);
            rsvmul(tB, tr12, cr3);
            radd(tA, tA, tB);
            radd(one, tmp, tA);
            st(ch, (chRef(ido, ido, 2, k) - chOffset) * 4, one);
            rsvmul(tA, ti11, ci5);
            rsvmul(tB, ti12, ci4);
            radd(one, tA, tB);
            st(ch, (chRef(ido, 1, 3, k) - chOffset) * 4, one);
            rsvmul(tA, tr12, cr2);
            rsvmul(tB, tr11, cr3);
            radd(tA, tA, tB);
            radd(one, tmp, tA);
            st(ch, (chRef(ido, ido, 4, k) - chOffset) * 4, one);
            rsvmul(tA, ti12, ci5);
            rsvmul(tB, ti11, ci4);
            rsub(one, tA, tB);
            st(ch, (chRef(ido, 1, 5, k) - chOffset) * 4, one);
        }
        if (ido == 1) {
            return;
        }
        var idp2 = ido + 2;
        for (var k = 1; k <= l1; ++k) {
            for (var i = 3; i <= ido; i += 2) {
                var ic = idp2 - i;
                ldps1(dr2, wa[wa1 + i - 3]);
                ldps1(di2, wa[wa1 + i - 2]);
                ldps1(dr3, wa[wa2 + i - 3]);
                ldps1(di3, wa[wa2 + i - 2]);
                ldps1(dr4, wa[wa3 + i - 3]);
                ldps1(di4, wa[wa3 + i - 2]);
                ldps1(dr5, wa[wa4 + i - 3]);
                ldps1(di5, wa[wa4 + i - 2]);
                cplxMulConjReg(dr2, di2, cc, (ccRef(ido, l1, i - 1, k, 2) - ccOffset) * 4, (ccRef(ido, l1, i, k, 2) - ccOffset) * 4);
                cplxMulConjReg(dr3, di3, cc, (ccRef(ido, l1, i - 1, k, 3) - ccOffset) * 4, (ccRef(ido, l1, i, k, 3) - ccOffset) * 4);
                cplxMulConjReg(dr4, di4, cc, (ccRef(ido, l1, i - 1, k, 4) - ccOffset) * 4, (ccRef(ido, l1, i, k, 4) - ccOffset) * 4);
                cplxMulConjReg(dr5, di5, cc, (ccRef(ido, l1, i - 1, k, 5) - ccOffset) * 4, (ccRef(ido, l1, i, k, 5) - ccOffset) * 4);
                radd(cr2, dr2, dr5);
                rsub(ci5, dr5, dr2);
                rsub(cr5, di2, di5);
                radd(ci2, di2, di5);
                radd(cr3, dr3, dr4);
                rsub(ci4, dr4, dr3);
                rsub(cr4, di3, di4);
                radd(ci3, di3, di4);
                ld(tmp, cc, (ccRef(ido, l1, i - 1, k, 1) - ccOffset) * 4);
                radd(tA, cr2, cr3);
                radd(one, tmp, tA);
                st(ch, (chRef(ido, i - 1, 1, k) - chOffset) * 4, one);
                ld(tmp, cc, (ccRef(ido, l1, i, k, 1) - ccOffset) * 4);
                radd(tA, ci2, ci3);
                rsub(one, tmp, tA);
                st(ch, (chRef(ido, i, 1, k) - chOffset) * 4, one);
                ld(tmp, cc, (ccRef(ido, l1, i - 1, k, 1) - ccOffset) * 4);
                rsvmul(tA, tr11, cr2);
                rsvmul(tB, tr12, cr3);
                radd(tA, tA, tB);
                radd(tr2, tmp, tA);
                ld(tmp, cc, (ccRef(ido, l1, i, k, 1) - ccOffset) * 4);
                rsvmul(tA, tr11, ci2);
                rsvmul(tB, tr12, ci3);
                radd(tA, tA, tB);
                rsub(ti2, tmp, tA);
                ld(tmp, cc, (ccRef(ido, l1, i - 1, k, 1) - ccOffset) * 4);
                rsvmul(tA, tr12, cr2);
                rsvmul(tB, tr11, cr3);
                radd(tA, tA, tB);
                radd(tr3, tmp, tA);
                ld(tmp, cc, (ccRef(ido, l1, i, k, 1) - ccOffset) * 4);
                rsvmul(tA, tr12, ci2);
                rsvmul(tB, tr11, ci3);
                radd(tA, tA, tB);
                rsub(ti3, tmp, tA);
                rsvmul(tA, ti11, cr5);
                rsvmul(tB, ti12, cr4);
                radd(tr5, tA, tB);
                rsvmul(tA, ti11, ci5);
                rsvmul(tB, ti12, ci4);
                radd(ti5, tA, tB);
                rsvmul(tA, ti12, cr5);
                rsvmul(tB, ti11, cr4);
                rsub(tr4, tA, tB);
                rsvmul(tA, ti12, ci5);
                rsvmul(tB, ti11, ci4);
                rsub(ti4, tA, tB);
                rsub(one, tr2, tr5);
                st(ch, (chRef(ido, i - 1, 3, k) - chOffset) * 4, one);
                radd(one, tr2, tr5);
                st(ch, (chRef(ido, ic - 1, 2, k) - chOffset) * 4, one);
                radd(one, ti2, ti5);
                st(ch, (chRef(ido, i, 3, k) - chOffset) * 4, one);
                rsub(one, ti5, ti2);
                st(ch, (chRef(ido, ic, 2, k) - chOffset) * 4, one);
                rsub(one, tr3, tr4);
                st(ch, (chRef(ido, i - 1, 5, k) - chOffset) * 4, one);
                radd(one, tr3, tr4);
                st(ch, (chRef(ido, ic - 1, 4, k) - chOffset) * 4, one);
                radd(one, ti3, ti4);
                st(ch, (chRef(ido, i, 5, k) - chOffset) * 4, one);
                rsub(one, ti4, ti3);
                st(ch, (chRef(ido, ic, 4, k) - chOffset) * 4, one);
            }
        }
    }

    /**
     * Computes the one based flat index {@code ((a3)*l1 + (a2))*ido + a1} of the radix 5 forward input
     * layout.
     *
     * @param ido the inner dimension
     * @param l1  the outer dimension
     * @param a1  the first index
     * @param a2  the second index
     * @param a3  the third index
     * @return the flat vector index before offset adjustment
     */
    private static int ccRef(int ido, int l1, int a1, int a2, int a3) {
        return ((a3) * l1 + (a2)) * ido + a1;
    }

    /**
     * Computes the one based flat index {@code ((a3)*5 + (a2))*ido + a1} of the radix 5 forward output
     * layout.
     *
     * @param ido the inner dimension
     * @param a1  the first index
     * @param a2  the second index
     * @param a3  the third index
     * @return the flat vector index before offset adjustment
     */
    private static int chRef(int ido, int a1, int a2, int a3) {
        return ((a3) * 5 + (a2)) * ido + a1;
    }

    /**
     * Multiplies a complex register pair by the conjugate of a complex value stored in an array, in place.
     *
     * <p>The register operands {@code rr}/{@code ri} are overwritten with the product of the register value
     * and the conjugate of the complex value loaded from {@code a} at the given real and imaginary offsets.
     *
     * @param rr the real register operand, overwritten with the result real part
     * @param ri the imaginary register operand, overwritten with the result imaginary part
     * @param a  the array holding the memory complex operand
     * @param obr the real memory offset
     * @param obi the imaginary memory offset
     */
    private static void cplxMulConjReg(float[] rr, float[] ri, float[] a, int obr, int obi) {
        for (var l = 0; l < 4; l++) {
            float ar = rr[l], ai = ri[l];
            float br = a[obr + l], bi = a[obi + l];
            var tmp = ar * bi;
            ar = ar * br;
            ar = ar + ai * bi;
            ai = ai * br;
            ai = ai - tmp;
            rr[l] = ar;
            ri[l] = ai;
        }
    }

    /**
     * Applies the radix 2 real backward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the twiddle base offset
     */
    private static void radb2(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1) {
        var minusTwo = -2.0f;
        var l1ido = l1 * ido;
        float[] a = new float[4], b = new float[4], c = new float[4], d = new float[4];
        float[] tr2 = new float[4], ti2 = new float[4], t = new float[4];
        for (var k = 0; k < l1ido; k += ido) {
            ld(a, cc, (2 * k) * 4);
            ld(b, cc, (2 * (k + ido) - 1) * 4);
            radd(t, a, b);
            st(ch, k * 4, t);
            rsub(t, a, b);
            st(ch, (k + l1ido) * 4, t);
        }
        if (ido < 2) {
            return;
        }
        if (ido != 2) {
            for (var k = 0; k < l1ido; k += ido) {
                for (var i = 2; i < ido; i += 2) {
                    ld(a, cc, (i - 1 + 2 * k) * 4);
                    ld(b, cc, (2 * (k + ido) - i - 1) * 4);
                    ld(c, cc, (i + 0 + 2 * k) * 4);
                    ld(d, cc, (2 * (k + ido) - i + 0) * 4);
                    radd(t, a, b);
                    st(ch, (i - 1 + k) * 4, t);
                    rsub(tr2, a, b);
                    rsub(t, c, d);
                    st(ch, (i + 0 + k) * 4, t);
                    radd(ti2, c, d);
                    rcplxMul(tr2, ti2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                    st(ch, (i - 1 + k + l1ido) * 4, tr2);
                    st(ch, (i + 0 + k + l1ido) * 4, ti2);
                }
            }
            if (ido % 2 == 1) {
                return;
            }
        }
        for (var k = 0; k < l1ido; k += ido) {
            ld(a, cc, (2 * k + ido - 1) * 4);
            ld(b, cc, (2 * k + ido) * 4);
            radd(t, a, a);
            st(ch, (k + ido - 1) * 4, t);
            rsvmul(t, minusTwo, b);
            st(ch, (k + ido - 1 + l1ido) * 4, t);
        }
    }

    /**
     * Applies the radix 3 real backward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     */
    private static void radb3(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2) {
        float taur = -0.5f, taui = 0.866025403784439f, taui2 = 0.866025403784439f * 2;
        float[] tr2 = new float[4], cr2 = new float[4], ci3 = new float[4], ci2 = new float[4];
        float[] cr3 = new float[4], ti2 = new float[4], dr2 = new float[4], dr3 = new float[4];
        float[] di2 = new float[4], di3 = new float[4], tA = new float[4], tB = new float[4], taurReg = new float[4];
        ldps1(taurReg, taur);
        for (var k = 0; k < l1; k++) {
            ld(tr2, cc, (ido - 1 + (3 * k + 1) * ido) * 4);
            radd(tr2, tr2, tr2);
            ld(tB, cc, 3 * k * ido * 4);
            rmadd(cr2, taurReg, tr2, tB);
            ld(tB, cc, 3 * k * ido * 4);
            radd(tA, tB, tr2);
            st(ch, k * ido * 4, tA);
            ld(tB, cc, (3 * k + 2) * ido * 4);
            rsvmul(ci3, taui2, tB);
            rsub(tA, cr2, ci3);
            st(ch, (k + l1) * ido * 4, tA);
            radd(tA, cr2, ci3);
            st(ch, (k + 2 * l1) * ido * 4, tA);
        }
        if (ido == 1) {
            return;
        }
        for (var k = 0; k < l1; k++) {
            for (var i = 2; i < ido; i += 2) {
                var ic = ido - i;
                ld(tA, cc, (i - 1 + (3 * k + 2) * ido) * 4);
                ld(tB, cc, (ic - 1 + (3 * k + 1) * ido) * 4);
                radd(tr2, tA, tB);
                ld(tB, cc, (i - 1 + 3 * k * ido) * 4);
                rmadd(cr2, taurReg, tr2, tB);
                ld(tB, cc, (i - 1 + 3 * k * ido) * 4);
                radd(tA, tB, tr2);
                st(ch, (i - 1 + k * ido) * 4, tA);
                ld(tA, cc, (i + (3 * k + 2) * ido) * 4);
                ld(tB, cc, (ic + (3 * k + 1) * ido) * 4);
                rsub(ti2, tA, tB);
                ld(tB, cc, (i + 3 * k * ido) * 4);
                rmadd(ci2, taurReg, ti2, tB);
                ld(tB, cc, (i + 3 * k * ido) * 4);
                radd(tA, tB, ti2);
                st(ch, (i + k * ido) * 4, tA);
                ld(tA, cc, (i - 1 + (3 * k + 2) * ido) * 4);
                ld(tB, cc, (ic - 1 + (3 * k + 1) * ido) * 4);
                rsub(tA, tA, tB);
                rsvmul(cr3, taui, tA);
                ld(tA, cc, (i + (3 * k + 2) * ido) * 4);
                ld(tB, cc, (ic + (3 * k + 1) * ido) * 4);
                radd(tA, tA, tB);
                rsvmul(ci3, taui, tA);
                rsub(dr2, cr2, ci3);
                radd(dr3, cr2, ci3);
                radd(di2, ci2, cr3);
                rsub(di3, ci2, cr3);
                rcplxMul(dr2, di2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                st(ch, (i - 1 + (k + l1) * ido) * 4, dr2);
                st(ch, (i + (k + l1) * ido) * 4, di2);
                rcplxMul(dr3, di3, wa[wa2 + i - 2], wa[wa2 + i - 1]);
                st(ch, (i - 1 + (k + 2 * l1) * ido) * 4, dr3);
                st(ch, (i + (k + 2 * l1) * ido) * 4, di3);
            }
        }
    }

    /**
     * Applies the radix 4 real backward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     * @param wa3 the third twiddle base offset
     */
    private static void radb4(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3) {
        var minusSqrt2 = (float) -1.414213562373095;
        var two = 2.0f;
        var l1ido = l1 * ido;
        float[] a = new float[4], b = new float[4], c = new float[4], d = new float[4];
        float[] tr1 = new float[4], tr2 = new float[4], tr3 = new float[4], tr4 = new float[4];
        float[] ti1 = new float[4], ti2 = new float[4], ti3 = new float[4], ti4 = new float[4];
        float[] cr2 = new float[4], cr3 = new float[4], cr4 = new float[4];
        float[] ci2 = new float[4], ci3 = new float[4], ci4 = new float[4], t = new float[4];
        {
            int ccp = 0, chp = 0;
            while (chp < l1ido) {
                ld(a, cc, (ccp + 0) * 4);
                ld(b, cc, (ccp + 4 * ido - 1) * 4);
                ld(c, cc, (ccp + 2 * ido) * 4);
                ld(d, cc, (ccp + 2 * ido - 1) * 4);
                rsvmul(tr3, two, d);
                radd(tr2, a, b);
                rsub(tr1, a, b);
                rsvmul(tr4, two, c);
                radd(t, tr2, tr3);
                st(ch, (chp + 0 * l1ido) * 4, t);
                rsub(t, tr2, tr3);
                st(ch, (chp + 2 * l1ido) * 4, t);
                rsub(t, tr1, tr4);
                st(ch, (chp + 1 * l1ido) * 4, t);
                radd(t, tr1, tr4);
                st(ch, (chp + 3 * l1ido) * 4, t);
                ccp += 4 * ido;
                chp += ido;
            }
        }
        if (ido < 2) {
            return;
        }
        if (ido != 2) {
            for (var k = 0; k < l1ido; k += ido) {
                var pc = -1 + 4 * k;
                var ph = k + 1;
                for (var i = 2; i < ido; i += 2) {
                    ld(a, cc, (pc + i) * 4);
                    ld(b, cc, (pc + 4 * ido - i) * 4);
                    rsub(tr1, a, b);
                    radd(tr2, a, b);
                    ld(a, cc, (pc + 2 * ido + i) * 4);
                    ld(b, cc, (pc + 2 * ido - i) * 4);
                    rsub(ti4, a, b);
                    radd(tr3, a, b);
                    radd(t, tr2, tr3);
                    st(ch, ph * 4, t);
                    rsub(cr3, tr2, tr3);
                    ld(a, cc, (pc + 2 * ido + i + 1) * 4);
                    ld(b, cc, (pc + 2 * ido - i + 1) * 4);
                    rsub(ti3, a, b);
                    radd(tr4, a, b);
                    rsub(cr2, tr1, tr4);
                    radd(cr4, tr1, tr4);
                    ld(a, cc, (pc + i + 1) * 4);
                    ld(b, cc, (pc + 4 * ido - i + 1) * 4);
                    radd(ti1, a, b);
                    rsub(ti2, a, b);
                    radd(t, ti2, ti3);
                    st(ch, (ph + 1) * 4, t);
                    ph += l1ido;
                    rsub(ci3, ti2, ti3);
                    radd(ci2, ti1, ti4);
                    rsub(ci4, ti1, ti4);
                    rcplxMul(cr2, ci2, wa[wa1 + i - 2], wa[wa1 + i - 1]);
                    st(ch, ph * 4, cr2);
                    st(ch, (ph + 1) * 4, ci2);
                    ph += l1ido;
                    rcplxMul(cr3, ci3, wa[wa2 + i - 2], wa[wa2 + i - 1]);
                    st(ch, ph * 4, cr3);
                    st(ch, (ph + 1) * 4, ci3);
                    ph += l1ido;
                    rcplxMul(cr4, ci4, wa[wa3 + i - 2], wa[wa3 + i - 1]);
                    st(ch, ph * 4, cr4);
                    st(ch, (ph + 1) * 4, ci4);
                    ph = ph - 3 * l1ido + 2;
                }
            }
            if (ido % 2 == 1) {
                return;
            }
        }
        for (var k = 0; k < l1ido; k += ido) {
            var i0 = 4 * k + ido;
            ld(c, cc, (i0 - 1) * 4);
            ld(d, cc, (i0 + 2 * ido - 1) * 4);
            ld(a, cc, (i0 + 0) * 4);
            ld(b, cc, (i0 + 2 * ido + 0) * 4);
            rsub(tr1, c, d);
            radd(tr2, c, d);
            radd(ti1, b, a);
            rsub(ti2, b, a);
            radd(t, tr2, tr2);
            st(ch, (ido - 1 + k + 0 * l1ido) * 4, t);
            rsub(t, ti1, tr1);
            rsvmul(t, minusSqrt2, t);
            st(ch, (ido - 1 + k + 1 * l1ido) * 4, t);
            radd(t, ti2, ti2);
            st(ch, (ido - 1 + k + 2 * l1ido) * 4, t);
            radd(t, ti1, tr1);
            rsvmul(t, minusSqrt2, t);
            st(ch, (ido - 1 + k + 3 * l1ido) * 4, t);
        }
    }

    /**
     * Applies the radix 5 real backward butterfly.
     *
     * @param ido the inner dimension in vectors
     * @param l1  the outer dimension
     * @param cc  the input vectors
     * @param ch  the output vectors
     * @param wa  the twiddle table
     * @param wa1 the first twiddle base offset
     * @param wa2 the second twiddle base offset
     * @param wa3 the third twiddle base offset
     * @param wa4 the fourth twiddle base offset
     */
    private static void radb5(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3, int wa4) {
        float tr11 = .309016994374947f, ti11 = .951056516295154f, tr12 = -.809016994374947f, ti12 = .587785252292473f;
        var chOffset = 1 + ido * (1 + l1);
        var ccOffset = 1 + ido * 6;
        float[] ti5 = new float[4], ti4 = new float[4], tr2 = new float[4], tr3 = new float[4];
        float[] ti2 = new float[4], ti3 = new float[4], tr5 = new float[4], tr4 = new float[4];
        float[] cr2 = new float[4], cr3 = new float[4], ci5 = new float[4], ci4 = new float[4];
        float[] ci2 = new float[4], ci3 = new float[4], cr5 = new float[4], cr4 = new float[4];
        float[] dr3 = new float[4], dr4 = new float[4], di3 = new float[4], di4 = new float[4];
        float[] dr5 = new float[4], dr2 = new float[4], di5 = new float[4], di2 = new float[4];
        float[] tA = new float[4], tB = new float[4], tmp = new float[4], one = new float[4];
        for (var k = 1; k <= l1; ++k) {
            ld(tA, cc, (ccRefB(ido, 1, 3, k) - ccOffset) * 4);
            radd(ti5, tA, tA);
            ld(tA, cc, (ccRefB(ido, 1, 5, k) - ccOffset) * 4);
            radd(ti4, tA, tA);
            ld(tA, cc, (ccRefB(ido, ido, 2, k) - ccOffset) * 4);
            radd(tr2, tA, tA);
            ld(tA, cc, (ccRefB(ido, ido, 4, k) - ccOffset) * 4);
            radd(tr3, tA, tA);
            ld(tmp, cc, (ccRefB(ido, 1, 1, k) - ccOffset) * 4);
            radd(tA, tr2, tr3);
            radd(one, tmp, tA);
            st(ch, (chRefB(ido, l1, 1, k, 1) - chOffset) * 4, one);
            rsvmul(tA, tr11, tr2);
            rsvmul(tB, tr12, tr3);
            radd(tA, tA, tB);
            radd(cr2, tmp, tA);
            rsvmul(tA, tr12, tr2);
            rsvmul(tB, tr11, tr3);
            radd(tA, tA, tB);
            radd(cr3, tmp, tA);
            rsvmul(tA, ti11, ti5);
            rsvmul(tB, ti12, ti4);
            radd(ci5, tA, tB);
            rsvmul(tA, ti12, ti5);
            rsvmul(tB, ti11, ti4);
            rsub(ci4, tA, tB);
            rsub(one, cr2, ci5);
            st(ch, (chRefB(ido, l1, 1, k, 2) - chOffset) * 4, one);
            rsub(one, cr3, ci4);
            st(ch, (chRefB(ido, l1, 1, k, 3) - chOffset) * 4, one);
            radd(one, cr3, ci4);
            st(ch, (chRefB(ido, l1, 1, k, 4) - chOffset) * 4, one);
            radd(one, cr2, ci5);
            st(ch, (chRefB(ido, l1, 1, k, 5) - chOffset) * 4, one);
        }
        if (ido == 1) {
            return;
        }
        var idp2 = ido + 2;
        for (var k = 1; k <= l1; ++k) {
            for (var i = 3; i <= ido; i += 2) {
                var ic = idp2 - i;
                ld(tA, cc, (ccRefB(ido, i, 3, k) - ccOffset) * 4);
                ld(tB, cc, (ccRefB(ido, ic, 2, k) - ccOffset) * 4);
                radd(ti5, tA, tB);
                rsub(ti2, tA, tB);
                ld(tA, cc, (ccRefB(ido, i, 5, k) - ccOffset) * 4);
                ld(tB, cc, (ccRefB(ido, ic, 4, k) - ccOffset) * 4);
                radd(ti4, tA, tB);
                rsub(ti3, tA, tB);
                ld(tA, cc, (ccRefB(ido, i - 1, 3, k) - ccOffset) * 4);
                ld(tB, cc, (ccRefB(ido, ic - 1, 2, k) - ccOffset) * 4);
                rsub(tr5, tA, tB);
                radd(tr2, tA, tB);
                ld(tA, cc, (ccRefB(ido, i - 1, 5, k) - ccOffset) * 4);
                ld(tB, cc, (ccRefB(ido, ic - 1, 4, k) - ccOffset) * 4);
                rsub(tr4, tA, tB);
                radd(tr3, tA, tB);
                ld(tmp, cc, (ccRefB(ido, i - 1, 1, k) - ccOffset) * 4);
                radd(tA, tr2, tr3);
                radd(one, tmp, tA);
                st(ch, (chRefB(ido, l1, i - 1, k, 1) - chOffset) * 4, one);
                ld(tmp, cc, (ccRefB(ido, i, 1, k) - ccOffset) * 4);
                radd(tA, ti2, ti3);
                radd(one, tmp, tA);
                st(ch, (chRefB(ido, l1, i, k, 1) - chOffset) * 4, one);
                ld(tmp, cc, (ccRefB(ido, i - 1, 1, k) - ccOffset) * 4);
                rsvmul(tA, tr11, tr2);
                rsvmul(tB, tr12, tr3);
                radd(tA, tA, tB);
                radd(cr2, tmp, tA);
                ld(tmp, cc, (ccRefB(ido, i, 1, k) - ccOffset) * 4);
                rsvmul(tA, tr11, ti2);
                rsvmul(tB, tr12, ti3);
                radd(tA, tA, tB);
                radd(ci2, tmp, tA);
                ld(tmp, cc, (ccRefB(ido, i - 1, 1, k) - ccOffset) * 4);
                rsvmul(tA, tr12, tr2);
                rsvmul(tB, tr11, tr3);
                radd(tA, tA, tB);
                radd(cr3, tmp, tA);
                ld(tmp, cc, (ccRefB(ido, i, 1, k) - ccOffset) * 4);
                rsvmul(tA, tr12, ti2);
                rsvmul(tB, tr11, ti3);
                radd(tA, tA, tB);
                radd(ci3, tmp, tA);
                rsvmul(tA, ti11, tr5);
                rsvmul(tB, ti12, tr4);
                radd(cr5, tA, tB);
                rsvmul(tA, ti11, ti5);
                rsvmul(tB, ti12, ti4);
                radd(ci5, tA, tB);
                rsvmul(tA, ti12, tr5);
                rsvmul(tB, ti11, tr4);
                rsub(cr4, tA, tB);
                rsvmul(tA, ti12, ti5);
                rsvmul(tB, ti11, ti4);
                rsub(ci4, tA, tB);
                rsub(dr3, cr3, ci4);
                radd(dr4, cr3, ci4);
                radd(di3, ci3, cr4);
                rsub(di4, ci3, cr4);
                radd(dr5, cr2, ci5);
                rsub(dr2, cr2, ci5);
                rsub(di5, ci2, cr5);
                radd(di2, ci2, cr5);
                rcplxMul(dr2, di2, wa[wa1 + i - 3], wa[wa1 + i - 2]);
                rcplxMul(dr3, di3, wa[wa2 + i - 3], wa[wa2 + i - 2]);
                rcplxMul(dr4, di4, wa[wa3 + i - 3], wa[wa3 + i - 2]);
                rcplxMul(dr5, di5, wa[wa4 + i - 3], wa[wa4 + i - 2]);
                st(ch, (chRefB(ido, l1, i - 1, k, 2) - chOffset) * 4, dr2);
                st(ch, (chRefB(ido, l1, i, k, 2) - chOffset) * 4, di2);
                st(ch, (chRefB(ido, l1, i - 1, k, 3) - chOffset) * 4, dr3);
                st(ch, (chRefB(ido, l1, i, k, 3) - chOffset) * 4, di3);
                st(ch, (chRefB(ido, l1, i - 1, k, 4) - chOffset) * 4, dr4);
                st(ch, (chRefB(ido, l1, i, k, 4) - chOffset) * 4, di4);
                st(ch, (chRefB(ido, l1, i - 1, k, 5) - chOffset) * 4, dr5);
                st(ch, (chRefB(ido, l1, i, k, 5) - chOffset) * 4, di5);
            }
        }
    }

    /**
     * Computes the one based flat index {@code ((a3)*5 + (a2))*ido + a1} of the radix 5 backward input
     * layout.
     *
     * @param ido the inner dimension
     * @param a1  the first index
     * @param a2  the second index
     * @param a3  the third index
     * @return the flat vector index before offset adjustment
     */
    private static int ccRefB(int ido, int a1, int a2, int a3) {
        return ((a3) * 5 + (a2)) * ido + a1;
    }

    /**
     * Computes the one based flat index {@code ((a3)*l1 + (a2))*ido + a1} of the radix 5 backward output
     * layout.
     *
     * @param ido the inner dimension
     * @param l1  the outer dimension
     * @param a1  the first index
     * @param a2  the second index
     * @param a3  the third index
     * @return the flat vector index before offset adjustment
     */
    private static int chRefB(int ido, int l1, int a1, int a2, int a3) {
        return ((a3) * l1 + (a2)) * ido + a1;
    }

    /**
     * Applies the radix 2 complex pass; {@code fsign} is {@code -1} forward, {@code +1} backward.
     *
     * @param ido   the inner dimension in vectors
     * @param l1    the outer dimension
     * @param cc    the input vectors
     * @param ch    the output vectors
     * @param wa    the twiddle table
     * @param wa1   the twiddle base offset
     * @param fsign the transform sign
     */
    private static void passf2(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int fsign) {
        var l1ido = l1 * ido;
        float[] a = new float[4], b = new float[4], t = new float[4];
        if (ido <= 2) {
            int ccp = 0, chp = 0;
            for (var k = 0; k < l1ido; k += ido) {
                ld(a, cc, (ccp + 0) * 4);
                ld(b, cc, (ccp + ido + 0) * 4);
                radd(t, a, b);
                st(ch, (chp + 0) * 4, t);
                rsub(t, a, b);
                st(ch, (chp + l1ido) * 4, t);
                ld(a, cc, (ccp + 1) * 4);
                ld(b, cc, (ccp + ido + 1) * 4);
                radd(t, a, b);
                st(ch, (chp + 1) * 4, t);
                rsub(t, a, b);
                st(ch, (chp + l1ido + 1) * 4, t);
                chp += ido;
                ccp += 2 * ido;
            }
        } else {
            float[] tr2 = new float[4], ti2 = new float[4];
            int ccp = 0, chp = 0;
            for (var k = 0; k < l1ido; k += ido) {
                for (var i = 0; i < ido - 1; i += 2) {
                    ld(a, cc, (ccp + i + 0) * 4);
                    ld(b, cc, (ccp + i + ido + 0) * 4);
                    rsub(tr2, a, b);
                    ld(a, cc, (ccp + i + 1) * 4);
                    ld(b, cc, (ccp + i + ido + 1) * 4);
                    rsub(ti2, a, b);
                    var wr = wa[wa1 + i];
                    var wi = fsign * wa[wa1 + i + 1];
                    ld(a, cc, (ccp + i + 0) * 4);
                    ld(b, cc, (ccp + i + ido + 0) * 4);
                    radd(t, a, b);
                    st(ch, (chp + i) * 4, t);
                    ld(a, cc, (ccp + i + 1) * 4);
                    ld(b, cc, (ccp + i + ido + 1) * 4);
                    radd(t, a, b);
                    st(ch, (chp + i + 1) * 4, t);
                    rcplxMul(tr2, ti2, wr, wi);
                    st(ch, (chp + i + l1ido) * 4, tr2);
                    st(ch, (chp + i + l1ido + 1) * 4, ti2);
                }
                chp += ido;
                ccp += 2 * ido;
            }
        }
    }

    /**
     * Applies the radix 3 complex pass.
     *
     * @param ido   the inner dimension in vectors
     * @param l1    the outer dimension
     * @param cc    the input vectors
     * @param ch    the output vectors
     * @param wa    the twiddle table
     * @param wa1   the first twiddle base offset
     * @param wa2   the second twiddle base offset
     * @param fsign the transform sign
     */
    private static void passf3(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int fsign) {
        var taur = -0.5f;
        var taui = 0.866025403784439f * fsign;
        var l1ido = l1 * ido;
        float[] tr2 = new float[4], cr2 = new float[4], ti2 = new float[4], ci2 = new float[4];
        float[] cr3 = new float[4], ci3 = new float[4], dr2 = new float[4], dr3 = new float[4];
        float[] di2 = new float[4], di3 = new float[4], a = new float[4], b = new float[4], t = new float[4];
        int ccp = 0, chp = 0;
        for (var k = 0; k < l1ido; k += ido) {
            for (var i = 0; i < ido - 1; i += 2) {
                ld(a, cc, (ccp + i + ido) * 4);
                ld(b, cc, (ccp + i + 2 * ido) * 4);
                radd(tr2, a, b);
                ld(a, cc, (ccp + i) * 4);
                rsvmul(t, taur, tr2);
                radd(cr2, a, t);
                ld(a, cc, (ccp + i) * 4);
                radd(t, a, tr2);
                st(ch, (chp + i) * 4, t);
                ld(a, cc, (ccp + i + ido + 1) * 4);
                ld(b, cc, (ccp + i + 2 * ido + 1) * 4);
                radd(ti2, a, b);
                ld(a, cc, (ccp + i + 1) * 4);
                rsvmul(t, taur, ti2);
                radd(ci2, a, t);
                ld(a, cc, (ccp + i + 1) * 4);
                radd(t, a, ti2);
                st(ch, (chp + i + 1) * 4, t);
                ld(a, cc, (ccp + i + ido) * 4);
                ld(b, cc, (ccp + i + 2 * ido) * 4);
                rsub(t, a, b);
                rsvmul(cr3, taui, t);
                ld(a, cc, (ccp + i + ido + 1) * 4);
                ld(b, cc, (ccp + i + 2 * ido + 1) * 4);
                rsub(t, a, b);
                rsvmul(ci3, taui, t);
                rsub(dr2, cr2, ci3);
                radd(dr3, cr2, ci3);
                radd(di2, ci2, cr3);
                rsub(di3, ci2, cr3);
                float wr1 = wa[wa1 + i], wi1 = fsign * wa[wa1 + i + 1];
                float wr2 = wa[wa2 + i], wi2 = fsign * wa[wa2 + i + 1];
                rcplxMul(dr2, di2, wr1, wi1);
                st(ch, (chp + i + l1ido) * 4, dr2);
                st(ch, (chp + i + l1ido + 1) * 4, di2);
                rcplxMul(dr3, di3, wr2, wi2);
                st(ch, (chp + i + 2 * l1ido) * 4, dr3);
                st(ch, (chp + i + 2 * l1ido + 1) * 4, di3);
            }
            ccp += 3 * ido;
            chp += ido;
        }
    }

    /**
     * Applies the radix 4 complex pass.
     *
     * @param ido   the inner dimension in vectors
     * @param l1    the outer dimension
     * @param cc    the input vectors
     * @param ch    the output vectors
     * @param wa    the twiddle table
     * @param wa1   the first twiddle base offset
     * @param wa2   the second twiddle base offset
     * @param wa3   the third twiddle base offset
     * @param fsign the transform sign
     */
    private static void passf4(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3, int fsign) {
        var l1ido = l1 * ido;
        float[] a = new float[4], b = new float[4], t = new float[4];
        float[] tr1 = new float[4], tr2 = new float[4], tr3 = new float[4], tr4 = new float[4];
        float[] ti1 = new float[4], ti2 = new float[4], ti3 = new float[4], ti4 = new float[4];
        float[] cr2 = new float[4], cr3 = new float[4], cr4 = new float[4];
        float[] ci2 = new float[4], ci3 = new float[4], ci4 = new float[4];
        if (ido == 2) {
            int ccp = 0, chp = 0;
            for (var k = 0; k < l1ido; k += ido) {
                ld(a, cc, (ccp + 0) * 4);
                ld(b, cc, (ccp + 2 * ido + 0) * 4);
                rsub(tr1, a, b);
                radd(tr2, a, b);
                ld(a, cc, (ccp + 1) * 4);
                ld(b, cc, (ccp + 2 * ido + 1) * 4);
                rsub(ti1, a, b);
                radd(ti2, a, b);
                ld(a, cc, (ccp + 1 * ido + 0) * 4);
                ld(b, cc, (ccp + 3 * ido + 0) * 4);
                rsub(t, a, b);
                rsvmul(ti4, fsign, t);
                ld(a, cc, (ccp + 3 * ido + 1) * 4);
                ld(b, cc, (ccp + 1 * ido + 1) * 4);
                rsub(t, a, b);
                rsvmul(tr4, fsign, t);
                ld(a, cc, (ccp + ido + 0) * 4);
                ld(b, cc, (ccp + 3 * ido + 0) * 4);
                radd(tr3, a, b);
                ld(a, cc, (ccp + ido + 1) * 4);
                ld(b, cc, (ccp + 3 * ido + 1) * 4);
                radd(ti3, a, b);
                radd(t, tr2, tr3);
                st(ch, (chp + 0 * l1ido + 0) * 4, t);
                radd(t, ti2, ti3);
                st(ch, (chp + 0 * l1ido + 1) * 4, t);
                radd(t, tr1, tr4);
                st(ch, (chp + 1 * l1ido + 0) * 4, t);
                radd(t, ti1, ti4);
                st(ch, (chp + 1 * l1ido + 1) * 4, t);
                rsub(t, tr2, tr3);
                st(ch, (chp + 2 * l1ido + 0) * 4, t);
                rsub(t, ti2, ti3);
                st(ch, (chp + 2 * l1ido + 1) * 4, t);
                rsub(t, tr1, tr4);
                st(ch, (chp + 3 * l1ido + 0) * 4, t);
                rsub(t, ti1, ti4);
                st(ch, (chp + 3 * l1ido + 1) * 4, t);
                chp += ido;
                ccp += 4 * ido;
            }
        } else {
            int ccp = 0, chp = 0;
            for (var k = 0; k < l1ido; k += ido) {
                for (var i = 0; i < ido - 1; i += 2) {
                    ld(a, cc, (ccp + i + 0) * 4);
                    ld(b, cc, (ccp + i + 2 * ido + 0) * 4);
                    rsub(tr1, a, b);
                    radd(tr2, a, b);
                    ld(a, cc, (ccp + i + 1) * 4);
                    ld(b, cc, (ccp + i + 2 * ido + 1) * 4);
                    rsub(ti1, a, b);
                    radd(ti2, a, b);
                    ld(a, cc, (ccp + i + 3 * ido + 1) * 4);
                    ld(b, cc, (ccp + i + 1 * ido + 1) * 4);
                    rsub(t, a, b);
                    rsvmul(tr4, fsign, t);
                    ld(a, cc, (ccp + i + 1 * ido + 0) * 4);
                    ld(b, cc, (ccp + i + 3 * ido + 0) * 4);
                    rsub(t, a, b);
                    rsvmul(ti4, fsign, t);
                    ld(a, cc, (ccp + i + ido + 0) * 4);
                    ld(b, cc, (ccp + i + 3 * ido + 0) * 4);
                    radd(tr3, a, b);
                    ld(a, cc, (ccp + i + ido + 1) * 4);
                    ld(b, cc, (ccp + i + 3 * ido + 1) * 4);
                    radd(ti3, a, b);
                    radd(t, tr2, tr3);
                    st(ch, (chp + i) * 4, t);
                    rsub(cr3, tr2, tr3);
                    radd(t, ti2, ti3);
                    st(ch, (chp + i + 1) * 4, t);
                    rsub(ci3, ti2, ti3);
                    radd(cr2, tr1, tr4);
                    rsub(cr4, tr1, tr4);
                    radd(ci2, ti1, ti4);
                    rsub(ci4, ti1, ti4);
                    float wr1 = wa[wa1 + i], wi1 = fsign * wa[wa1 + i + 1];
                    rcplxMul(cr2, ci2, wr1, wi1);
                    float wr2 = wa[wa2 + i], wi2 = fsign * wa[wa2 + i + 1];
                    st(ch, (chp + i + l1ido) * 4, cr2);
                    st(ch, (chp + i + l1ido + 1) * 4, ci2);
                    rcplxMul(cr3, ci3, wr2, wi2);
                    float wr3 = wa[wa3 + i], wi3 = fsign * wa[wa3 + i + 1];
                    st(ch, (chp + i + 2 * l1ido) * 4, cr3);
                    st(ch, (chp + i + 2 * l1ido + 1) * 4, ci3);
                    rcplxMul(cr4, ci4, wr3, wi3);
                    st(ch, (chp + i + 3 * l1ido) * 4, cr4);
                    st(ch, (chp + i + 3 * l1ido + 1) * 4, ci4);
                }
                chp += ido;
                ccp += 4 * ido;
            }
        }
    }

    /**
     * Applies the radix 5 complex pass.
     *
     * <p>Input vectors are addressed as {@code (a2-1)*ido + a1 + 1} and output vectors as
     * {@code (a3-1)*l1*ido + a1 + 1}, both relative to the base offsets {@code ccp} and {@code chp} that
     * advance once per outer index {@code k}.
     *
     * @param ido   the inner dimension in vectors
     * @param l1    the outer dimension
     * @param cc    the input vectors
     * @param ch    the output vectors
     * @param wa    the twiddle table
     * @param wa1   the first twiddle base offset
     * @param wa2   the second twiddle base offset
     * @param wa3   the third twiddle base offset
     * @param wa4   the fourth twiddle base offset
     * @param fsign the transform sign
     */
    private static void passf5(int ido, int l1, float[] cc, float[] ch, float[] wa, int wa1, int wa2, int wa3, int wa4, int fsign) {
        var tr11 = .309016994374947f;
        var ti11 = .951056516295154f * fsign;
        var tr12 = -.809016994374947f;
        var ti12 = .587785252292473f * fsign;
        float[] ti5 = new float[4], ti2 = new float[4], ti4 = new float[4], ti3 = new float[4];
        float[] tr5 = new float[4], tr2 = new float[4], tr4 = new float[4], tr3 = new float[4];
        float[] cr2 = new float[4], ci2 = new float[4], cr3 = new float[4], ci3 = new float[4];
        float[] cr5 = new float[4], ci5 = new float[4], cr4 = new float[4], ci4 = new float[4];
        float[] dr3 = new float[4], dr4 = new float[4], di3 = new float[4], di4 = new float[4];
        float[] dr5 = new float[4], dr2 = new float[4], di5 = new float[4], di2 = new float[4];
        float[] a = new float[4], b = new float[4], t = new float[4], u = new float[4];
        int ccp = 0, chp = 0;
        for (var k = 0; k < l1; ++k) {
            for (var i = 0; i < ido - 1; i += 2) {
                ld(a, cc, (ccp + (2 - 1) * ido + i + 1) * 4);
                ld(b, cc, (ccp + (5 - 1) * ido + i + 1) * 4);
                rsub(ti5, a, b);
                radd(ti2, a, b);
                ld(a, cc, (ccp + (3 - 1) * ido + i + 1) * 4);
                ld(b, cc, (ccp + (4 - 1) * ido + i + 1) * 4);
                rsub(ti4, a, b);
                radd(ti3, a, b);
                ld(a, cc, (ccp + (2 - 1) * ido + (i - 1) + 1) * 4);
                ld(b, cc, (ccp + (5 - 1) * ido + (i - 1) + 1) * 4);
                rsub(tr5, a, b);
                radd(tr2, a, b);
                ld(a, cc, (ccp + (3 - 1) * ido + (i - 1) + 1) * 4);
                ld(b, cc, (ccp + (4 - 1) * ido + (i - 1) + 1) * 4);
                rsub(tr4, a, b);
                radd(tr3, a, b);
                ld(a, cc, (ccp + (1 - 1) * ido + (i - 1) + 1) * 4);
                radd(t, tr2, tr3);
                radd(t, a, t);
                st(ch, (chp + (1 - 1) * l1 * ido + (i - 1) + 1) * 4, t);
                ld(a, cc, (ccp + (1 - 1) * ido + i + 1) * 4);
                radd(t, ti2, ti3);
                radd(t, a, t);
                st(ch, (chp + (1 - 1) * l1 * ido + i + 1) * 4, t);
                ld(a, cc, (ccp + (1 - 1) * ido + (i - 1) + 1) * 4);
                rsvmul(t, tr11, tr2);
                rsvmul(u, tr12, tr3);
                radd(t, t, u);
                radd(cr2, a, t);
                ld(a, cc, (ccp + (1 - 1) * ido + i + 1) * 4);
                rsvmul(t, tr11, ti2);
                rsvmul(u, tr12, ti3);
                radd(t, t, u);
                radd(ci2, a, t);
                ld(a, cc, (ccp + (1 - 1) * ido + (i - 1) + 1) * 4);
                rsvmul(t, tr12, tr2);
                rsvmul(u, tr11, tr3);
                radd(t, t, u);
                radd(cr3, a, t);
                ld(a, cc, (ccp + (1 - 1) * ido + i + 1) * 4);
                rsvmul(t, tr12, ti2);
                rsvmul(u, tr11, ti3);
                radd(t, t, u);
                radd(ci3, a, t);
                rsvmul(t, ti11, tr5);
                rsvmul(u, ti12, tr4);
                radd(cr5, t, u);
                rsvmul(t, ti11, ti5);
                rsvmul(u, ti12, ti4);
                radd(ci5, t, u);
                rsvmul(t, ti12, tr5);
                rsvmul(u, ti11, tr4);
                rsub(cr4, t, u);
                rsvmul(t, ti12, ti5);
                rsvmul(u, ti11, ti4);
                rsub(ci4, t, u);
                rsub(dr3, cr3, ci4);
                radd(dr4, cr3, ci4);
                radd(di3, ci3, cr4);
                rsub(di4, ci3, cr4);
                radd(dr5, cr2, ci5);
                rsub(dr2, cr2, ci5);
                rsub(di5, ci2, cr5);
                radd(di2, ci2, cr5);
                float wr1 = wa[wa1 + i], wi1 = fsign * wa[wa1 + i + 1];
                float wr2 = wa[wa2 + i], wi2 = fsign * wa[wa2 + i + 1];
                float wr3 = wa[wa3 + i], wi3 = fsign * wa[wa3 + i + 1];
                float wr4 = wa[wa4 + i], wi4 = fsign * wa[wa4 + i + 1];
                rcplxMul(dr2, di2, wr1, wi1);
                st(ch, (chp + (2 - 1) * l1 * ido + (i - 1) + 1) * 4, dr2);
                st(ch, (chp + (2 - 1) * l1 * ido + i + 1) * 4, di2);
                rcplxMul(dr3, di3, wr2, wi2);
                st(ch, (chp + (3 - 1) * l1 * ido + (i - 1) + 1) * 4, dr3);
                st(ch, (chp + (3 - 1) * l1 * ido + i + 1) * 4, di3);
                rcplxMul(dr4, di4, wr3, wi3);
                st(ch, (chp + (4 - 1) * l1 * ido + (i - 1) + 1) * 4, dr4);
                st(ch, (chp + (4 - 1) * l1 * ido + i + 1) * 4, di4);
                rcplxMul(dr5, di5, wr4, wi4);
                st(ch, (chp + (5 - 1) * l1 * ido + (i - 1) + 1) * 4, dr5);
                st(ch, (chp + (5 - 1) * l1 * ido + i + 1) * 4, di5);
            }
            ccp += 5 * ido;
            chp += ido;
        }
    }

    /**
     * Combines one 4x4 block of the real forward finalize stage.
     *
     * <p>The first two rows {@code r0}/{@code i0} come from the separate {@code in0}/{@code in1} operands (the
     * carry from the previous block, or zero for the first block); the remaining six rows are read from
     * {@code in}. After the two transposes, the per lane twiddle multiply, and the radix 4 combine, eight
     * vectors are written to {@code out}.
     *
     * @param in0  the array holding the {@code r0} carry vector
     * @param oin0 the offset of {@code r0}
     * @param in1  the array holding the {@code i0} carry vector
     * @param oin1 the offset of {@code i0}
     * @param in   the array holding the six remaining input vectors
     * @param oin  the offset of the first of the six
     * @param e    the twiddle array
     * @param oe   the twiddle base offset
     * @param out  the destination array
     * @param oout the destination base offset
     */
    private static void realFinalize4x4(float[] in0, int oin0, float[] in1, int oin1, float[] in, int oin, float[] e, int oe, float[] out, int oout) {
        float[] r0 = new float[4], i0 = new float[4], r1 = new float[4], i1 = new float[4];
        float[] r2 = new float[4], i2 = new float[4], r3 = new float[4], i3 = new float[4];
        float[] sr0 = new float[4], dr0 = new float[4], sr1 = new float[4], dr1 = new float[4];
        float[] si0 = new float[4], di0 = new float[4], si1 = new float[4], di1 = new float[4];
        ld(r0, in0, oin0);
        ld(i0, in1, oin1);
        ld(r1, in, oin + 0);
        ld(i1, in, oin + 4);
        ld(r2, in, oin + 8);
        ld(i2, in, oin + 12);
        ld(r3, in, oin + 16);
        ld(i3, in, oin + 20);
        transpose4Regs(r0, r1, r2, r3);
        transpose4Regs(i0, i1, i2, i3);
        rcplxMulReg(r1, i1, e, oe + 0, oe + 4);
        rcplxMulReg(r2, i2, e, oe + 8, oe + 12);
        rcplxMulReg(r3, i3, e, oe + 16, oe + 20);
        radd(sr0, r0, r2);
        rsub(dr0, r0, r2);
        radd(sr1, r1, r3);
        rsub(dr1, r3, r1);
        radd(si0, i0, i2);
        rsub(di0, i0, i2);
        radd(si1, i1, i3);
        rsub(di1, i3, i1);
        radd(r0, sr0, sr1);
        rsub(r3, sr0, sr1);
        radd(i0, si0, si1);
        rsub(i3, si1, si0);
        radd(r1, dr0, di1);
        rsub(r2, dr0, di1);
        rsub(i1, dr1, di0);
        radd(i2, dr1, di0);
        st(out, oout + 0, r0);
        st(out, oout + 4, i0);
        st(out, oout + 8, r1);
        st(out, oout + 12, i1);
        st(out, oout + 16, r2);
        st(out, oout + 20, i2);
        st(out, oout + 24, r3);
        st(out, oout + 28, i3);
    }

    /**
     * Finalizes the real forward transform.
     *
     * <p>Drives {@link #realFinalize4x4} once per 4x4 block, carrying the seventh vector of each block forward
     * to the next, and computes the eight scalar DC, Nyquist, and quarter frequency outputs of the first
     * block with the rotation constant {@link #S_HALF}.
     *
     * @param ncvecParam the number of complex SIMD vectors
     * @param in         the input vectors
     * @param out        the output vectors
     * @param e          the twiddle array
     */
    private static void realFinalize(int ncvecParam, float[] in, float[] out, float[] e) {
        var dk = ncvecParam / SIMD_SZ;
        var zero = new float[4];
        float[] cr = new float[4], ci = new float[4], save = new float[4], saveNext = new float[4];
        ld(cr, in, 0 * 4);
        ld(ci, in, (ncvecParam * 2 - 1) * 4);
        ld(save, in, 7 * 4);
        realFinalize4x4(zero, 0, zero, 0, in, 1 * 4, e, 0, out, 0);

        out[0 * 4 + 0] = (cr[0] + cr[2]) + (cr[1] + cr[3]);
        out[1 * 4 + 0] = (cr[0] + cr[2]) - (cr[1] + cr[3]);
        out[4 * 4 + 0] = (cr[0] - cr[2]);
        out[5 * 4 + 0] = (cr[3] - cr[1]);
        out[2 * 4 + 0] = ci[0] + S_HALF * (ci[1] - ci[3]);
        out[3 * 4 + 0] = -ci[2] - S_HALF * (ci[1] + ci[3]);
        out[6 * 4 + 0] = ci[0] - S_HALF * (ci[1] - ci[3]);
        out[7 * 4 + 0] = ci[2] - S_HALF * (ci[1] + ci[3]);

        for (var k = 1; k < dk; ++k) {
            ld(saveNext, in, (8 * k + 7) * 4);
            realFinalize4x4(save, 0, in, (8 * k + 0) * 4, in, (8 * k + 1) * 4, e, k * 6 * 4, out, k * 8 * 4);
            var tmp = save;
            save = saveNext;
            saveNext = tmp;
        }
    }

    /**
     * Preprocesses one 4x4 block of the real backward transform.
     *
     * <p>Reads eight input vectors, applies the inverse matrix combine, the conjugate per lane twiddle
     * multiply, and two transposes, then writes either eight vectors (when not the first block) or only the
     * last six (when {@code first} is set, since the first two are produced by the scalar tail).
     *
     * @param in    the input array
     * @param oin   the input base offset
     * @param e     the twiddle array
     * @param oe    the twiddle base offset
     * @param out   the output array
     * @param oout  the output base offset
     * @param first {@code true} for the first block, suppressing the first two output vectors
     */
    private static void realPreprocess4x4(float[] in, int oin, float[] e, int oe, float[] out, int oout, boolean first) {
        float[] r0 = new float[4], i0 = new float[4], r1 = new float[4], i1 = new float[4];
        float[] r2 = new float[4], i2 = new float[4], r3 = new float[4], i3 = new float[4];
        ld(r0, in, oin + 0);
        ld(i0, in, oin + 4);
        ld(r1, in, oin + 8);
        ld(i1, in, oin + 12);
        ld(r2, in, oin + 16);
        ld(i2, in, oin + 20);
        ld(r3, in, oin + 24);
        ld(i3, in, oin + 28);
        float[] sr0 = new float[4], dr0 = new float[4], sr1 = new float[4], dr1 = new float[4];
        float[] si0 = new float[4], di0 = new float[4], si1 = new float[4], di1 = new float[4];
        radd(sr0, r0, r3);
        rsub(dr0, r0, r3);
        radd(sr1, r1, r2);
        rsub(dr1, r1, r2);
        radd(si0, i0, i3);
        rsub(di0, i0, i3);
        radd(si1, i1, i2);
        rsub(di1, i1, i2);
        radd(r0, sr0, sr1);
        rsub(r2, sr0, sr1);
        rsub(r1, dr0, si1);
        radd(r3, dr0, si1);
        rsub(i0, di0, di1);
        radd(i2, di0, di1);
        rsub(i1, si0, dr1);
        radd(i3, si0, dr1);
        rcplxMulConjReg(r1, i1, e, oe + 0, oe + 4);
        rcplxMulConjReg(r2, i2, e, oe + 8, oe + 12);
        rcplxMulConjReg(r3, i3, e, oe + 16, oe + 20);
        transpose4Regs(r0, r1, r2, r3);
        transpose4Regs(i0, i1, i2, i3);
        var o = oout;
        if (!first) {
            st(out, o, r0);
            o += 4;
            st(out, o, i0);
            o += 4;
        }
        st(out, o, r1);
        o += 4;
        st(out, o, i1);
        o += 4;
        st(out, o, r2);
        o += 4;
        st(out, o, i2);
        o += 4;
        st(out, o, r3);
        o += 4;
        st(out, o, i3);
    }

    /**
     * Preprocesses the real backward transform.
     *
     * <p>Computes the eight scalar inputs of the first block with the rotation constant {@link #S_SQRT2},
     * drives {@link #realPreprocess4x4} once per block, and writes the two corner vectors. Because the first
     * block writes only six vectors, the output offsets carry a bias of one vector for the remaining blocks.
     *
     * @param ncvecParam the number of complex SIMD vectors
     * @param in         the input vectors
     * @param out        the output vectors
     * @param e          the twiddle array
     */
    private static void realPreprocess(int ncvecParam, float[] in, float[] out, float[] e) {
        var dk = ncvecParam / SIMD_SZ;
        float[] xr = new float[4], xi = new float[4];
        for (var k = 0; k < 4; ++k) {
            xr[k] = in[8 * k];
            xi[k] = in[8 * k + 4];
        }
        realPreprocess4x4(in, 0, e, 0, out, 1 * 4, true);
        for (var k = 1; k < dk; ++k) {
            realPreprocess4x4(in, 8 * k * 4, e, k * 6 * 4, out, (-1 + k * 8) * 4, false);
        }
        var u0 = 0;
        var uLast = (2 * ncvecParam - 1) * 4;
        out[u0 + 0] = (xr[0] + xi[0]) + 2 * xr[2];
        out[u0 + 1] = (xr[0] - xi[0]) - 2 * xi[2];
        out[u0 + 2] = (xr[0] + xi[0]) - 2 * xr[2];
        out[u0 + 3] = (xr[0] - xi[0]) + 2 * xi[2];
        out[uLast + 0] = 2 * (xr[1] + xr[3]);
        out[uLast + 1] = S_SQRT2 * (xr[1] - xr[3]) - S_SQRT2 * (xi[1] + xi[3]);
        out[uLast + 2] = 2 * (xi[3] - xi[1]);
        out[uLast + 3] = -S_SQRT2 * (xr[1] - xr[3]) - S_SQRT2 * (xi[1] + xi[3]);
    }

    /**
     * Finalizes the complex forward transform.
     *
     * @param ncvecParam the number of complex SIMD vectors
     * @param in         the input vectors
     * @param out        the output vectors
     * @param e          the twiddle array
     */
    private static void cplxFinalize(int ncvecParam, float[] in, float[] out, float[] e) {
        var dk = ncvecParam / SIMD_SZ;
        float[] r0 = new float[4], i0 = new float[4], r1 = new float[4], i1 = new float[4];
        float[] r2 = new float[4], i2 = new float[4], r3 = new float[4], i3 = new float[4];
        float[] sr0 = new float[4], dr0 = new float[4], sr1 = new float[4], dr1 = new float[4];
        float[] si0 = new float[4], di0 = new float[4], si1 = new float[4], di1 = new float[4];
        var o = 0;
        for (var k = 0; k < dk; ++k) {
            ld(r0, in, (8 * k + 0) * 4);
            ld(i0, in, (8 * k + 1) * 4);
            ld(r1, in, (8 * k + 2) * 4);
            ld(i1, in, (8 * k + 3) * 4);
            ld(r2, in, (8 * k + 4) * 4);
            ld(i2, in, (8 * k + 5) * 4);
            ld(r3, in, (8 * k + 6) * 4);
            ld(i3, in, (8 * k + 7) * 4);
            transpose4Regs(r0, r1, r2, r3);
            transpose4Regs(i0, i1, i2, i3);
            rcplxMulReg(r1, i1, e, k * 6 * 4 + 0, k * 6 * 4 + 4);
            rcplxMulReg(r2, i2, e, k * 6 * 4 + 8, k * 6 * 4 + 12);
            rcplxMulReg(r3, i3, e, k * 6 * 4 + 16, k * 6 * 4 + 20);
            radd(sr0, r0, r2);
            rsub(dr0, r0, r2);
            radd(sr1, r1, r3);
            rsub(dr1, r1, r3);
            radd(si0, i0, i2);
            rsub(di0, i0, i2);
            radd(si1, i1, i3);
            rsub(di1, i1, i3);
            radd(r0, sr0, sr1);
            radd(i0, si0, si1);
            radd(r1, dr0, di1);
            rsub(i1, di0, dr1);
            rsub(r2, sr0, sr1);
            rsub(i2, si0, si1);
            rsub(r3, dr0, di1);
            radd(i3, di0, dr1);
            st(out, o, r0);
            o += 4;
            st(out, o, i0);
            o += 4;
            st(out, o, r1);
            o += 4;
            st(out, o, i1);
            o += 4;
            st(out, o, r2);
            o += 4;
            st(out, o, i2);
            o += 4;
            st(out, o, r3);
            o += 4;
            st(out, o, i3);
            o += 4;
        }
    }

    /**
     * Preprocesses the complex backward transform.
     *
     * @param ncvecParam the number of complex SIMD vectors
     * @param in         the input vectors
     * @param out        the output vectors
     * @param e          the twiddle array
     */
    private static void cplxPreprocess(int ncvecParam, float[] in, float[] out, float[] e) {
        var dk = ncvecParam / SIMD_SZ;
        float[] r0 = new float[4], i0 = new float[4], r1 = new float[4], i1 = new float[4];
        float[] r2 = new float[4], i2 = new float[4], r3 = new float[4], i3 = new float[4];
        float[] sr0 = new float[4], dr0 = new float[4], sr1 = new float[4], dr1 = new float[4];
        float[] si0 = new float[4], di0 = new float[4], si1 = new float[4], di1 = new float[4];
        var o = 0;
        for (var k = 0; k < dk; ++k) {
            ld(r0, in, (8 * k + 0) * 4);
            ld(i0, in, (8 * k + 1) * 4);
            ld(r1, in, (8 * k + 2) * 4);
            ld(i1, in, (8 * k + 3) * 4);
            ld(r2, in, (8 * k + 4) * 4);
            ld(i2, in, (8 * k + 5) * 4);
            ld(r3, in, (8 * k + 6) * 4);
            ld(i3, in, (8 * k + 7) * 4);
            radd(sr0, r0, r2);
            rsub(dr0, r0, r2);
            radd(sr1, r1, r3);
            rsub(dr1, r1, r3);
            radd(si0, i0, i2);
            rsub(di0, i0, i2);
            radd(si1, i1, i3);
            rsub(di1, i1, i3);
            radd(r0, sr0, sr1);
            radd(i0, si0, si1);
            rsub(r1, dr0, di1);
            radd(i1, di0, dr1);
            rsub(r2, sr0, sr1);
            rsub(i2, si0, si1);
            radd(r3, dr0, di1);
            rsub(i3, di0, dr1);
            rcplxMulConjReg(r1, i1, e, k * 6 * 4 + 0, k * 6 * 4 + 4);
            rcplxMulConjReg(r2, i2, e, k * 6 * 4 + 8, k * 6 * 4 + 12);
            rcplxMulConjReg(r3, i3, e, k * 6 * 4 + 16, k * 6 * 4 + 20);
            transpose4Regs(r0, r1, r2, r3);
            transpose4Regs(i0, i1, i2, i3);
            st(out, o, r0);
            o += 4;
            st(out, o, i0);
            o += 4;
            st(out, o, r1);
            o += 4;
            st(out, o, i1);
            o += 4;
            st(out, o, r2);
            o += 4;
            st(out, o, i2);
            o += 4;
            st(out, o, r3);
            o += 4;
            st(out, o, i3);
            o += 4;
        }
    }

    /**
     * Transposes four registers as a 4x4 matrix in place.
     *
     * @param a register row 0, overwritten with column 0
     * @param b register row 1, overwritten with column 1
     * @param c register row 2, overwritten with column 2
     * @param d register row 3, overwritten with column 3
     */
    private static void transpose4Regs(float[] a, float[] b, float[] c, float[] d) {
        float a0 = a[0], a1 = a[1], a2 = a[2], a3 = a[3];
        float b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3];
        float c0 = c[0], c1 = c[1], c2 = c[2], c3 = c[3];
        float d0 = d[0], d1 = d[1], d2 = d[2], d3 = d[3];
        a[0] = a0;
        a[1] = b0;
        a[2] = c0;
        a[3] = d0;
        b[0] = a1;
        b[1] = b1;
        b[2] = c1;
        b[3] = d1;
        c[0] = a2;
        c[1] = b2;
        c[2] = c2;
        c[3] = d2;
        d[0] = a3;
        d[1] = b3;
        d[2] = c3;
        d[3] = d3;
    }

    /**
     * Multiplies a complex register pair by a per lane vector twiddle from {@code e} in place.
     *
     * @param ar  the real part register
     * @param ai  the imaginary part register
     * @param e   the twiddle array
     * @param obr the real twiddle offset
     * @param obi the imaginary twiddle offset
     */
    private static void rcplxMulReg(float[] ar, float[] ai, float[] e, int obr, int obi) {
        for (var l = 0; l < 4; l++) {
            float r = ar[l], i = ai[l];
            float br = e[obr + l], bi = e[obi + l];
            var tmp = r * bi;
            r = r * br;
            r = r - i * bi;
            i = i * br;
            i = i + tmp;
            ar[l] = r;
            ai[l] = i;
        }
    }

    /**
     * Multiplies a complex register pair by the conjugate of a per lane vector twiddle from {@code e} in
     * place.
     *
     * @param ar  the real part register
     * @param ai  the imaginary part register
     * @param e   the twiddle array
     * @param obr the real twiddle offset
     * @param obi the imaginary twiddle offset
     */
    private static void rcplxMulConjReg(float[] ar, float[] ai, float[] e, int obr, int obi) {
        for (var l = 0; l < 4; l++) {
            float r = ar[l], i = ai[l];
            float br = e[obr + l], bi = e[obi + l];
            var tmp = r * bi;
            r = r * br;
            r = r + i * bi;
            i = i * br;
            i = i - tmp;
            ar[l] = r;
            ai[l] = i;
        }
    }

    /**
     * Performs the reversed interleave copy of the real forward z reorder.
     *
     * <p>Walks {@code nn} blocks of the source with the given vector stride, interleaving each pair and
     * writing the high and low swapped result backward from the end of the output window.
     *
     * @param nn       the number of blocks
     * @param in       the source array
     * @param inBase   the source base float offset
     * @param inStride the source vector stride in vectors
     * @param out      the destination array
     * @param outVec   the one past the end destination vector index (the copy fills backward from here)
     */
    private static void reversedCopy(int nn, float[] in, int inBase, int inStride, float[] out, int outVec) {
        float[] g0 = new float[4], g1 = new float[4], h0 = new float[4], h1 = new float[4];
        var inp = inBase;
        var outp = outVec;
        interleave2Regs(in, inp, in, inp + 4, g0, g1);
        inp += inStride * 4;
        outp -= 4;
        vswaphlRegs(out, outp, g0, g1);
        for (var k = 1; k < nn; ++k) {
            interleave2Regs(in, inp, in, inp + 4, h0, h1);
            inp += inStride * 4;
            outp -= 4;
            vswaphlRegs(out, outp, g1, h0);
            outp -= 4;
            vswaphlRegs(out, outp, h0, h1);
            System.arraycopy(h1, 0, g1, 0, 4);
        }
        outp -= 4;
        vswaphlRegs(out, outp, g1, g0);
    }

    /**
     * Performs the unreversed uninterleave copy of the real backward z reorder.
     *
     * <p>{@code inBase} and {@code outBase} are float offsets into the flat arrays, not vector indices, so a
     * caller passes them already advanced by a float bias (for example {@code in + N/4} and
     * {@code out + N - 2*SIMD_SZ}); the per vector advance scales by four, as
     * {@link #reversedCopy(int, float[], int, int, float[], int)} does.
     *
     * @param nn        the number of blocks
     * @param in        the source array
     * @param inBase    the source base float offset
     * @param out       the destination array
     * @param outBase   the destination base float offset
     * @param outStride the destination vector stride (negative; the copy walks backward)
     */
    private static void unreversedCopy(int nn, float[] in, int inBase, float[] out, int outBase, int outStride) {
        float[] g0 = new float[4], g1 = new float[4], h0 = new float[4], h1 = new float[4];
        var inp = inBase;
        var outp = outBase;
        ld(g0, in, inp);
        System.arraycopy(g0, 0, g1, 0, 4);
        inp += 4;
        for (var k = 1; k < nn; ++k) {
            ld(h0, in, inp);
            inp += 4;
            ld(h1, in, inp);
            inp += 4;
            vswaphlRegsR(g1, g1, h0);
            vswaphlRegsR(h0, h0, h1);
            uninterleave2Regs(h0, g1, out, outp, out, outp + 4);
            outp += outStride * 4;
            System.arraycopy(h1, 0, g1, 0, 4);
        }
        ld(h0, in, inp);
        System.arraycopy(g0, 0, h1, 0, 4);
        vswaphlRegsR(g1, g1, h0);
        vswaphlRegsR(h0, h0, h1);
        uninterleave2Regs(h0, g1, out, outp, out, outp + 4);
    }

    /**
     * Interleaves two array vectors into two registers.
     *
     * @param in1 the first input array
     * @param oi1 the first input offset
     * @param in2 the second input array
     * @param oi2 the second input offset
     * @param o1  the first output register
     * @param o2  the second output register
     */
    private static void interleave2Regs(float[] in1, int oi1, float[] in2, int oi2, float[] o1, float[] o2) {
        float t0 = in1[oi1], t1 = in2[oi2], t2 = in1[oi1 + 1], t3 = in2[oi2 + 1];
        float u0 = in1[oi1 + 2], u1 = in2[oi2 + 2], u2 = in1[oi1 + 3], u3 = in2[oi2 + 3];
        o1[0] = t0;
        o1[1] = t1;
        o1[2] = t2;
        o1[3] = t3;
        o2[0] = u0;
        o2[1] = u1;
        o2[2] = u2;
        o2[3] = u3;
    }

    /**
     * Uninterleaves two registers into two array vectors.
     *
     * @param r1  the first input register
     * @param r2  the second input register
     * @param o1  the first output array
     * @param oo1 the first output offset
     * @param o2  the second output array
     * @param oo2 the second output offset
     */
    private static void uninterleave2Regs(float[] r1, float[] r2, float[] o1, int oo1, float[] o2, int oo2) {
        float t0 = r1[0], t1 = r1[2], t2 = r2[0], t3 = r2[2];
        float u0 = r1[1], u1 = r1[3], u2 = r2[1], u3 = r2[3];
        o1[oo1] = t0;
        o1[oo1 + 1] = t1;
        o1[oo1 + 2] = t2;
        o1[oo1 + 3] = t3;
        o2[oo2] = u0;
        o2[oo2 + 1] = u1;
        o2[oo2 + 2] = u2;
        o2[oo2 + 3] = u3;
    }

    /**
     * Combines the low lanes of one register with the high lanes of another and stores the result to an
     * array.
     *
     * @param out the destination array
     * @param oo  the destination offset
     * @param a   the register supplying the high lanes
     * @param b   the register supplying the low lanes
     */
    private static void vswaphlRegs(float[] out, int oo, float[] a, float[] b) {
        out[oo] = b[0];
        out[oo + 1] = b[1];
        out[oo + 2] = a[2];
        out[oo + 3] = a[3];
    }

    /**
     * Combines the low lanes of one register with the high lanes of another into a destination register.
     *
     * @param d the destination register
     * @param a the register supplying the high lanes
     * @param b the register supplying the low lanes
     */
    private static void vswaphlRegsR(float[] d, float[] a, float[] b) {
        float r0 = b[0], r1 = b[1], r2 = a[2], r3 = a[3];
        d[0] = r0;
        d[1] = r1;
        d[2] = r2;
        d[3] = r3;
    }
}
