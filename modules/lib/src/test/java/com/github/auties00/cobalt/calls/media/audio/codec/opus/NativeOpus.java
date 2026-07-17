package com.github.auties00.cobalt.calls.media.audio.codec.opus;

/**
 * Test-only probe deciding whether the libopus-backed native codec is loadable in the running
 * environment, so the Opus round-trip suites can skip cleanly on a host without the {@code cobalt-native}
 * binary instead of failing.
 *
 * <p>The probe attempts to open and immediately close a minimal {@link OpusAudioCodec}. Loading the
 * binary happens in the codec class's static initializer, so an absent or incompatible library surfaces
 * as an {@link UnsatisfiedLinkError}, an {@link ExceptionInInitializerError}, or a
 * {@link NoClassDefFoundError} (a second touch of a class whose initializer already failed); any of these
 * means the native layer is unavailable. The result is computed once and cached.
 */
final class NativeOpus {
    /**
     * Whether a native Opus codec could be opened in this environment, computed once at class load.
     */
    private static final boolean AVAILABLE = probe();

    private NativeOpus() {
    }

    /**
     * Returns whether the native Opus codec is loadable here.
     *
     * @return {@code true} if a codec opened successfully during the one-shot probe
     */
    static boolean available() {
        return AVAILABLE;
    }

    /**
     * Opens and closes a throwaway 48 kHz mono codec, reporting success as native availability.
     *
     * @return {@code true} if the codec opened without a linkage or initializer error
     */
    private static boolean probe() {
        try {
            var params = OpusCodecParams.forSampleRate(48_000, 1, OpusApplication.VOIP);
            var codec = new OpusAudioCodec(params);
            codec.close();
            return true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
