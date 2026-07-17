package com.github.auties00.cobalt.calls.platform;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.foreign.MemorySegment;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Defines the boundary across which the WhatsApp voip engine calls down into its host environment.
 *
 * <p>The portable voip engine is host agnostic: every facility it cannot implement itself, from
 * sending a signaling stanza to drawing a decoded video frame, it reaches through a fixed set of
 * downcalls that the embedder supplies. On WhatsApp Web that embedder is the browser, and the
 * downcalls are Emscripten {@code env.*_js_sync} WASM imports backed by JavaScript. In Cobalt the
 * embedder is the JVM, the engine layer runs in process as Java, and the same downcalls collapse to the
 * ordinary methods of this interface; there is no marshalling boundary, only a plain method call on a
 * virtual thread. This interface is therefore the complete inventory of what the engine asks of its
 * host: the WhatsApp specific egress paths (signaling and media datagrams), the platform primitives
 * (cryptographically strong randomness, name resolution, a writable directory, the bandwidth
 * estimation model path), the privacy gate ({@link #isKnownContact(Jid)}), the rendered video sink,
 * the host audio processing capability query, and the two callback channels through which the engine
 * reports structured logs and typed call events back up to the application.
 *
 * <p>Each method corresponds to exactly one engine to host downcall, and that mapping is recorded on
 * the implementing methods. The audio and video capture and playback driver control downcalls
 * ({@code call_init/start/stop_capture/playback_js_sync} and the camera and screen capture variants)
 * are deliberately absent here: they drive stateful device endpoints with their own initialized and
 * active lifecycles and so live on dedicated driver contracts rather than on this flat callback
 * surface. What remains on this interface are the stateless, fire and forget downcalls plus the small
 * set of pure value queries.
 *
 * <p>This is the seam an embedder never calls directly: the engine layer invokes it, and the embedder
 * instead supplies the collaborators (the client, the contact store, a video sink) that the
 * implementation wires these downcalls onto. It is the contract documenting what the call engine needs
 * from its host, not an application API. It sits alongside the other call engine seams the lifecycle
 * controller is constructed over, so a test can substitute a fake host and drive the controller without
 * a live client; the production implementation is {@link LiveVoipHostApi}. Methods carry no recovery
 * contract of their own; an implementation that cannot satisfy a downcall (for example a datagram send
 * that fails because the socket is closed) reports it the way the wire path expects, and the engine
 * observes the result through the method return rather than through a thrown exception.
 *
 * @see CallEventType
 * @see LiveVoipHostApi
 */
public interface VoipHostApi {
    /**
     * Sends a single call signaling stanza on the host's signaling transport.
     *
     * <p>The engine produces one fully built {@link Stanza} per signaling action (an offer, an accept, a
     * transport update, a terminate, and so on) and hands it to this downcall for synchronous egress.
     * The host serializes the stanza onto its binary XMPP socket and returns once the stanza has been
     * accepted for sending; the engine treats the call as fire and forget at this layer, because the
     * matching server acknowledgement arrives back through the inbound signaling path rather than as a
     * return value here. A stanza whose transport cannot accept it (a disconnected socket) is dropped at
     * the host with the failure surfaced through the host's own logging rather than thrown back into the
     * engine.
     *
     * @param stanza the signaling stanza to send, already built by the engine signaling layer
     */
    void sendSignaling(Stanza stanza);

    /**
     * Sends one outbound media datagram to the given destination as an unreliable best effort packet.
     *
     * <p>This is the fallback egress for the media data plane: the engine reaches it when the shared
     * outbound packet ring is uninitialized or a frame does not fit the ring, in which case it asks the
     * host to perform a direct datagram send. The host writes the payload to its UDP transport (a relay
     * socket or a peer to peer socket selected by the destination) and returns the number of bytes the
     * transport accepted, which is the payload length on success and a value that is zero or negative
     * when the send could not be performed.
     *
     * @param payload     the datagram bytes to send
     * @param destination the address to send to
     * @return the number of bytes accepted by the transport, or a value that is zero or negative on failure
     */
    int sendDatagram(byte[] payload, SocketAddress destination);

    /**
     * Resolves a host name to its network addresses for the transport layer.
     *
     * <p>The engine's ICE and relay setup resolves STUN and TURN host names through this downcall before
     * it opens a transport. The host performs the lookup against the platform resolver and returns every
     * address the name maps to, in resolver order; a name that does not resolve yields an empty list so
     * the engine can fall through to the next candidate rather than fail the whole transport setup.
     *
     * @param hostName the host name to resolve
     * @return the resolved addresses in resolver order, or an empty list when the name does not resolve
     */
    List<InetAddress> resolveHost(String hostName);

    /**
     * Returns the requested number of cryptographically strong random bytes.
     *
     * <p>The engine draws all key material, nonces, and transaction identifiers that must be
     * unpredictable from this downcall rather than from any internal generator, so that the strength of
     * the host's entropy source backs them. Key sized draws request 32 bytes at a time, but the
     * count is a parameter and the host honours any length that is not negative.
     *
     * @param length the number of random bytes to produce
     * @return a freshly allocated array of exactly {@code length} cryptographically strong random bytes
     * @throws IllegalArgumentException if {@code length} is negative
     */
    byte[] randomBytes(int length);

    /**
     * Returns the on disk path of the bandwidth estimation model for the requested model type.
     *
     * <p>The engine's machine learning bandwidth estimator loads a model file per model type from disk
     * by asking the host where it lives. A host that bundles the model for the requested type returns its
     * path; a host that does not ship the model returns an empty result, which the engine treats as the
     * model being unavailable, silently falling back to its fallback estimator.
     *
     * @param modelType the engine's model type selector
     * @return the path to the model file, or empty when no model is available for the type
     */
    Optional<Path> bweMlModelPath(int modelType);

    /**
     * Reports whether the given participant is a known contact of the local account.
     *
     * <p>This is the engine's privacy gate for an incoming participant: several behaviours (how an
     * unknown caller is surfaced, which features are offered) branch on whether the peer is in the local
     * address book. The host consults its contact store and returns the answer; a peer that is not known,
     * or that cannot be classified because the address book has not loaded, is reported as not known so
     * the engine defaults to the more conservative handling of an unknown caller.
     *
     * @param participant the participant identity to classify
     * @return {@code true} if the participant is a known contact, {@code false} otherwise
     */
    boolean isKnownContact(Jid participant);

    /**
     * Hands a decoded video frame to the host's rendering sink for display.
     *
     * <p>Once the engine has decoded a participant's video it calls down with the finished frame so the
     * host can present it. The frame is described by its source participant, dimensions, up to two native
     * plane buffers, a rotation, a {@link RenderedVideoFrame.Format pixel format}, a presentation
     * timestamp, and a mirroring flag, all carried by {@link RenderedVideoFrame}. The host must consume
     * (copy or upload) the plane buffers before this method returns, because their backing memory is
     * owned by the engine and is not valid afterwards.
     *
     * @param frame the decoded frame to render
     */
    void renderVideoFrame(RenderedVideoFrame frame);

    /**
     * Returns the host's audio processing capability so the engine can avoid double processing.
     *
     * <p>Some hosts apply acoustic echo cancellation, noise suppression, and automatic gain control to
     * captured audio before the engine ever sees it; if so, the engine must not run its own copy of those
     * stages. The engine asks for the host's capability through this downcall and configures its
     * processing chain accordingly. The returned value is the engine's capability code; when the host has
     * not yet determined its capability it returns the code the engine treats as a safe default (it does
     * not assume upstream processing it cannot confirm).
     *
     * @return the host audio processing capability code
     */
    int browserAudioProcessingStatus();

    /**
     * Routes one structured log record from the engine to the host's logging facility.
     *
     * <p>The engine emits its diagnostics through this single sink rather than writing to any stream
     * directly, so the host controls where call engine logs land and at what severity. The host maps the
     * engine's level onto its own logger and forwards the message.
     *
     * @param level   the severity of the record
     * @param message the log message
     */
    void log(System.Logger.Level level, String message);

    /**
     * Delivers one typed in call event from the engine to the host application.
     *
     * <p>Every state change inside the engine that the application may care about (a call state
     * transition, a mute or video or screen share change, a reaction, a waiting room or call link update,
     * and so on) funnels through this single egress, selected by its {@link CallEventType} and carrying an
     * opaque serialized payload whose byte layout depends on the event type. The host decodes the payload
     * for the events it surfaces and fans them out to the application's listeners; an event the host does
     * not surface is dropped. The engine has already decided whether to emit before calling down, so an
     * event reaching this method is one intended for the host rather than an id used only internally.
     *
     * @param eventType the kind of event being reported
     * @param payload   the serialized event payload, whose layout is determined by {@code eventType}
     */
    void onCallEvent(CallEventType eventType, byte[] payload);

    /**
     * Carries one decoded video frame from the engine across {@link VoipHostApi#renderVideoFrame}.
     *
     * <p>This record packages the arguments the engine's render downcall passes for a single frame so the
     * host can present it. The {@code plane0} and {@code plane1} segments point at the frame's pixel data
     * in engine owned memory and are valid only for the duration of the
     * {@link VoipHostApi#renderVideoFrame} call; the {@link Format} records which pixel layout those
     * planes use, {@code rotation} the clockwise display rotation in degrees, {@code timestampSeconds} the
     * presentation time on the engine's media clock, and {@code mirrored} whether the frame should be
     * horizontally flipped for display (as a self view from a front camera typically is).
     *
     * @param participant      the participant whose video this frame belongs to
     * @param width            the frame width in pixels
     * @param height           the frame height in pixels
     * @param plane0           the first pixel plane, valid only during the render call
     * @param plane1           the second pixel plane, valid only during the render call
     * @param rotation         the clockwise display rotation in degrees
     * @param format           the pixel layout of the planes
     * @param timestampSeconds the presentation timestamp on the media clock, in seconds
     * @param mirrored         whether the frame should be horizontally flipped for display
     */
    record RenderedVideoFrame(
            Jid participant,
            int width,
            int height,
            MemorySegment plane0,
            MemorySegment plane1,
            int rotation,
            Format format,
            double timestampSeconds,
            boolean mirrored
    ) {
        /**
         * Enumerates the pixel layouts a {@link RenderedVideoFrame} can carry across the render seam.
         *
         * <p>These are the formats the engine's render downcall produces for a decoded frame, each tagged
         * with the integer format code the native boundary uses for it so the host can resolve a raw code
         * back to a constant through {@link #ofCode(int)}. The set spans the planar and packed layouts a
         * sink may receive: the planar {@link #I420} the codecs decode into, the packed {@link #RGB3} and
         * {@link #RGBA} byte orders, and {@link #H264} for the pass through path where an encoded access
         * unit reaches the sink unchanged. An unrecognized code maps to {@link #UNKNOWN}.
         */
        public enum Format {
            /**
             * Denotes an unrecognized or unset pixel layout.
             *
             * <p>A frame tagged with this format carries a layout the host cannot interpret and is
             * dropped rather than rendered. It is the result {@link #ofCode(int)} returns for any code
             * outside the known set.
             */
            UNKNOWN(0xffffffff),

            /**
             * Denotes I420 planar 4:2:0, the full resolution luma plane followed by the two
             * half resolution chroma planes.
             *
             * <p>This is the layout the call video codecs decode into, so it is the common case on the
             * render path.
             */
            I420(1),

            /**
             * Denotes packed 24 bit RGB, three bytes per pixel in red, green, blue order with no padding.
             */
            RGB3(2),

            /**
             * Denotes packed 32 bit RGBA, four bytes per pixel in red, green, blue, alpha order.
             */
            RGBA(3),

            /**
             * Denotes an H.264 access unit delivered to the sink unchanged.
             *
             * <p>A frame tagged with this format has not been decoded to raw pixels; it reaches the sink
             * on the pass through path where the host, rather than the engine, performs the final decode.
             */
            H264(100);

            /**
             * Caches the constant array so the {@link #ofCode(int)} decode scan run once per frame does
             * not pay the defensive clone cost of {@link #values()} on every rendered frame.
             */
            private static final Format[] VALUES = values();

            /**
             * Resolves a native format code to its constant, backing {@link #ofCode(int)}.
             *
             * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves
             * to its constant in constant time rather than by scanning {@link #VALUES}. A code with no
             * entry falls back to {@link #UNKNOWN} in {@link #ofCode(int)}.
             */
            private static final Map<Integer, Format> BY_CODE;

            static {
                var byCode = new HashMap<Integer, Format>();
                for (var format : VALUES) {
                    if (byCode.put(format.code, format) != null) {
                        throw new AssertionError("Conflict");
                    }
                }
                BY_CODE = Map.copyOf(byCode);
            }

            /**
             * Holds the integer format code the native render boundary uses for this layout.
             */
            private final int code;

            /**
             * Constructs a format constant bound to its native format code.
             *
             * @param code the integer format code the native boundary uses for this layout
             */
            Format(int code) {
                this.code = code;
            }

            /**
             * Returns the integer format code the native render boundary uses for this layout.
             *
             * @return the native format code
             */
            public int code() {
                return code;
            }

            /**
             * Resolves a native format code to its constant.
             *
             * <p>Returns the constant whose {@link #code()} equals the argument, or {@link #UNKNOWN} when
             * no constant matches, so a caller decoding a raw code from the engine boundary never fails on
             * an unexpected value and instead drops the frame.
             *
             * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
             * scanning {@link #VALUES}.
             * @param code the native format code to resolve
             * @return the matching constant, or {@link #UNKNOWN} when none matches
             */
            public static Format ofCode(int code) {
                var format = BY_CODE.get(code);
                return format != null ? format : UNKNOWN;
            }
        }
    }
}
