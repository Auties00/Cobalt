package com.github.auties00.cobalt.util.ffmpeg;

import com.github.auties00.cobalt.util.NativeLibLoader;

import java.lang.foreign.Arena;

/**
 * Loads the FFmpeg shared libraries that the jextract-generated {@code Ffmpeg} bindings dispatch through.
 *
 * <p>The generated bindings resolve symbols via
 * {@code SymbolLookup.loaderLookup().or(defaultLookup())}, so they observe any library already
 * brought into the JVM through {@link System#load(String)}. Every FFmpeg library, the codec
 * implementations it wraps, and the rest of Cobalt's native dependencies are linked into one
 * combined {@code cobalt-native} library, so a single {@link NativeLibLoader#load(String, Arena)}
 * walks the loader's resolution order (classpath bundle, then on-disk cache, then network
 * download), finishes with a {@code System.load(absolutePath)}, and makes every subsequent
 * {@code Ffmpeg.*} call resolve through the loader's symbol table.
 *
 * <p>Loading is idempotent and lazy: the first call triggers the work and later calls return
 * immediately, since {@link NativeLibLoader} caches by library name and this class guards on a
 * one-shot flag.
 */
public final class FFmpegLoader {
    /**
     * Tracks whether the libraries have been loaded, flipping to {@code true} after the first
     * successful boot.
     *
     * @implNote This implementation is {@code volatile} so that the double-checked guard in
     * {@link #ensureLoaded()} observes the flip without holding the class monitor on the fast
     * path.
     */
    private static volatile boolean loaded;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always, since the class exposes only static members
     */
    private FFmpegLoader() {
        throw new AssertionError("FFmpegLoader is not instantiable");
    }

    /**
     * Ensures every FFmpeg shared library referenced by the bindings is loaded into the JVM.
     *
     * <p>The first invocation loads all required libraries in dependency order and marks the
     * loader booted; concurrent and later invocations return without repeating the work. Callers
     * invoke this before issuing their first {@code Ffmpeg.*} call.
     *
     * @throws UnsatisfiedLinkError if any required library cannot be resolved for this platform
     *                              classifier
     */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (FFmpegLoader.class) {
            if (loaded) {
                return;
            }
            NativeLibLoader.load("cobalt-native", Arena.global());
            loaded = true;
        }
    }

    /**
     * Returns whether the FFmpeg libraries have been loaded into this JVM.
     *
     * @return {@code true} once {@link #ensureLoaded()} has completed successfully
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
