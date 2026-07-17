package com.github.auties00.cobalt.socket.websocket;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Masks and unmasks a region of a byte array in place using the RFC 6455
 * XOR masking algorithm, and selects the concrete implementation to use
 * for the lifetime of the process.
 *
 * <p>A single operation serves both directions: the XOR mask is its own
 * inverse, so {@link WebSocketFrameOutputStream} uses it to mask outbound
 * payloads and {@link WebSocketFrameInputStream} uses the same call to
 * unmask inbound ones. The two permitted subclasses differ only in
 * throughput strategy, never in result: {@link ScalarWebSocketMasker} is
 * always available, while {@link VectorWebSocketMasker} adds a SIMD bulk
 * loop and references the {@code jdk.incubator.vector} incubator module.
 *
 * <p>That incubator module is resolved into the run-time image only when
 * the launcher is given {@code --add-modules jdk.incubator.vector}. The
 * {@code new VectorWebSocketMasker()} in {@link #lookup()} is named
 * directly rather than through reflection: naming the class only forces
 * the verifier to <em>load</em> it (to confirm it is a subtype of this
 * class), which never touches the incubator types named in its field and
 * method descriptors. Those types are <em>linked</em> only when the
 * constructor actually runs, which {@link #lookup()} gates behind a check
 * that the module is present. When the module is absent the
 * always-available scalar masker is used, so a process launched without
 * the flag runs correctly rather than failing with a
 * {@link NoClassDefFoundError}.
 */
 sealed abstract class WebSocketMasker
        permits ScalarWebSocketMasker, VectorWebSocketMasker {
    /**
     * The logger for {@link WebSocketMasker}.
     */
    private static final System.Logger LOGGER = Log.get(WebSocketMasker.class);

    /**
     * Holds the name of the incubator module that
     * {@link VectorWebSocketMasker} depends on.
     */
    private static final String VECTOR_MODULE_NAME = "jdk.incubator.vector";

    /**
     * Holds the system property GraalVM sets inside a native image, used
     * to skip the Vector API where it offers no SIMD speedup.
     */
    private static final String NATIVE_IMAGE_PROPERTY = "org.graalvm.nativeimage.imagecode";

    /**
     * Holds the masker chosen for the lifetime of the process, read
     * directly by the two WebSocket streams.
     */
    static final WebSocketMasker INSTANCE = lookup();

    /**
     * Constructs the masker base.
     *
     * <p>The constructor is package-private so that the sealed hierarchy
     * stays closed to this package; the two permitted subclasses carry no
     * state and invoke it implicitly.
     */
    WebSocketMasker() {

    }

    /**
     * Resolves the masker, preferring {@link VectorWebSocketMasker} when
     * the Vector API is both available and worth using and falling back to
     * {@link ScalarWebSocketMasker} otherwise.
     *
     * @implNote This implementation rules out the SIMD path in three
     * stages, short-circuiting to scalar as soon as any stage fails:
     * <ul>
     *   <li>GraalVM native image is detected through the
     *       {@value #NATIVE_IMAGE_PROPERTY} system property (set to
     *       {@code "buildtime"} during image construction and
     *       {@code "runtime"} in the built image, absent on a stock JVM).
     *       Native image always provides the incubator module but executes
     *       the Vector API as a per-lane scalar emulation, so the SIMD path
     *       there is strictly slower than {@link ScalarWebSocketMasker} and
     *       is skipped.</li>
     *   <li>The module graph is queried for {@value #VECTOR_MODULE_NAME};
     *       an absent module means the flag was not passed.</li>
     *   <li>When present, an explicit {@code addReads} edge is added (a
     *       {@code requires static} dependency does not always yield a
     *       run-time read edge) and {@link VectorWebSocketMasker} is
     *       constructed directly. {@link Throwable} is caught around the
     *       construction so the Vector API's own initialisation failure (or
     *       any {@link LinkageError} surfacing as the constructor links the
     *       incubator types) degrades to scalar.</li>
     * </ul>
     *
     * @return the masker to use, never {@code null}
     */
    private static WebSocketMasker lookup() {
        if (System.getProperty(NATIVE_IMAGE_PROPERTY) != null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "selected scalar websocket masker: native image detected");
            }
            return new ScalarWebSocketMasker();
        }

        var module = WebSocketMasker.class.getModule();
        var layer = Optional.ofNullable(module.getLayer())
                .orElse(ModuleLayer.boot());
        var vectorModule = layer.findModule(VECTOR_MODULE_NAME);
        if (vectorModule.isEmpty()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "selected scalar websocket masker: {0} module not present", VECTOR_MODULE_NAME);
            }
            return new ScalarWebSocketMasker();
        }
        module.addReads(vectorModule.get());

        try {
            var masker = new VectorWebSocketMasker();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "selected vector websocket masker");
            }
            return masker;
        } catch (Throwable e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "vector websocket masker initialization failed, falling back to scalar", e);
            }
            return new ScalarWebSocketMasker();
        }
    }

    /**
     * Applies the WebSocket XOR mask to a region of {@code array} in place.
     *
     * @implSpec Implementations must XOR the payload byte at each index
     * {@code i} in {@code [0, length)} with
     * {@link WebSocketFrameConstants#maskByte(int, int)} evaluated at
     * {@code maskKey} and {@code maskOffset + i}, mutating only the
     * half-open range {@code [offset, offset + length)} and touching no
     * byte outside it. The outcome must not depend on how a frame's
     * payload is split across calls, so a caller may mask one frame with
     * repeated calls that advance {@code maskOffset} by the number of
     * payload bytes already consumed.
     * @param array      the byte array to mask in place
     * @param offset     the index of the first byte to mask
     * @param length     the number of bytes to mask
     * @param maskKey    the four-byte masking key
     * @param maskOffset the starting position in the four-byte mask cycle
     */
    abstract void applyMask(byte[] array, int offset, int length, int maskKey, int maskOffset);
}
