package com.github.auties00.cobalt.calls.media.audio.codec.mlow.bwe;

/**
 * Holds the per subframe LSF interpolation factor rows shared by the low band and the high band.
 *
 * <p>The high band LSF interpolation reuses the same per subframe interpolation factor rows the low band
 * uses, selected by the frame's interpolation index and its subframe count. This holder supplies that factor
 * row to the high band interpolator so it threads the same blend weights the low band decode applied. Only
 * the rows for normally coded frames are needed here, because the high band is engaged only on those frames
 * and never on silence insertion (SID) frames.
 *
 * @implNote This implementation holds the interpolation tables as literal blend weights. The single subframe
 * factor is the scalar {@code 0.95f} carried as a one element row; the two subframe and four subframe tables
 * carry the blend weight rows indexed by the frame interpolation index. The comfort noise rows are omitted
 * because the high band is not synthesized for silence insertion frames.
 */
final class MlowLsfInterpolTables {
    /**
     * The single subframe interpolation factor, carried as a one element row.
     */
    private static final float[] INTERPOL_1 = {0.95f};

    /**
     * The two subframe interpolation factor rows, indexed by the frame interpolation index.
     */
    private static final float[][] INTERPOL_2 = {
            {0.75f, 1.0f},
            {0.4f, 0.95f}
    };

    /**
     * The four subframe interpolation factor rows, indexed by the frame interpolation index.
     */
    private static final float[][] INTERPOL_4 = {
            {0.55f, 0.88f, 1.0f, 1.0f},
            {0.3f, 0.65f, 0.95f, 1.0f}
    };

    /**
     * Prevents instantiation of this table holder.
     */
    private MlowLsfInterpolTables() {
    }

    /**
     * Returns the per subframe interpolation factor row for a frame.
     *
     * <p>Selects the table by the subframe count and the row by {@code interpolIdx}. The single subframe frame
     * has one fixed row regardless of the index.
     *
     * @param interpolIdx  the LSF interpolation index of the frame
     * @param numSubframes the subframe count of the frame; 1, 2, or 4
     * @return the interpolation factor row, holding {@code numSubframes} entries
     * @throws IllegalArgumentException if {@code numSubframes} is not 1, 2, or 4
     */
    static float[] factors(int interpolIdx, int numSubframes) {
        return switch (numSubframes) {
            case 1 -> INTERPOL_1;
            case 2 -> INTERPOL_2[interpolIdx];
            case 4 -> INTERPOL_4[interpolIdx];
            default -> throw new IllegalArgumentException("invalid subframe count " + numSubframes);
        };
    }
}
