package com.github.auties00.cobalt.registration.push.fcm;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevicePushClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link LinkedWhatsAppClientDevicePushClient} that receives WhatsApp's silent verification push over Firebase Cloud Messaging
 * while pretending to be the {@code com.whatsapp} (or {@code com.whatsapp.w4b}) Android app.
 *
 * <p>Owns three single-responsibility collaborators and orchestrates the lifecycle around them:
 * <ul>
 *   <li>{@link FcmRegistration} runs the three-step Android registration handshake on demand.</li>
 *   <li>{@link FcmMcsConnection} owns the long-lived MCS TLS stream and reconnect loop.</li>
 *   <li>{@link FcmPushCode} is the single-value sync primitive that hands the verification code back to
 *       {@link #getPushCode()}.</li>
 * </ul>
 *
 * <p>Persistent state lives entirely in the {@link FcmSession} the caller owns via {@link #getSession()} and
 * {@link #loadSession(FcmSession)}. The client never touches the file system. A typical first-time use:
 *
 * {@snippet :
 *   try (var fcm = FcmClient.newSession()) {
 *     fcm.authenticate(device);                // device.platform() picks WA personal vs business
 *     String pushToken = fcm.getPushToken();   // hand to /v2/exist
 *     FcmSession saved = fcm.getSession();     // persist this somewhere
 *     var code = fcm.getPushCode();         // blocks for the push
 * }
 * }
 *
 * <p>Subsequent runs skip the registration:
 *
 * {@snippet :
 *   try (var fcm = FcmClient.loadSession(saved)) {
 *     var code = fcm.getPushCode();
 * }
 * }
 */
public final class FcmClient implements LinkedWhatsAppClientDevicePushClient, AutoCloseable {
    /**
     * The logger for {@link FcmClient}.
     */
    private static final System.Logger LOGGER = Log.get(FcmClient.class);

    /**
     * Cached unmodifiable set of supported platforms.
     *
     * <p>Returned by {@link #supportedPlatforms()}; covers the personal and business Android variants
     * ({@link ClientPlatformType#ANDROID} and {@link ClientPlatformType#ANDROID_BUSINESS}).
     */
    private static final Set<ClientPlatformType> SUPPORTED_PLATFORMS =
            Set.of(ClientPlatformType.ANDROID, ClientPlatformType.ANDROID_BUSINESS);

    /**
     * Lifecycle states of the client.
     *
     * <p>The state machine transitions as follows:
     * <ul>
     *   <li>{@link #UNAUTHENTICATED} moves to {@link #AUTHENTICATING} when
     *       {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} is invoked.</li>
     *   <li>{@link #AUTHENTICATING} moves to {@link #AUTHENTICATED} on success or back to {@link #UNAUTHENTICATED} on
     *       failure.</li>
     *   <li>Any state moves to {@link #CLOSED} when {@link FcmClient#close()} is invoked.</li>
     * </ul>
     */
    private enum State {
        /**
         * No session bound.
         *
         * <p>Only {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} and {@link FcmClient#close()} are valid in this
         * state; every read-only accessor throws {@link IllegalStateException}.
         */
        UNAUTHENTICATED,
        /**
         * {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} is currently running.
         *
         * <p>Concurrent callers see this state and throw rather than racing on the registration pipeline.
         */
        AUTHENTICATING,
        /**
         * Registration succeeded.
         *
         * <p>The MCS listener is running and the read-only accessors are usable.
         */
        AUTHENTICATED,
        /**
         * {@link FcmClient#close()} has been invoked.
         *
         * <p>Every accessor throws and the listener thread is being torn down; further state transitions are not
         * permitted.
         */
        CLOSED
    }

    /**
     * Pre-built three-step registration helper bound to the configured proxy.
     *
     * <p>Stateless beyond its dependencies, so it can be reused across retries.
     */
    private final FcmRegistration registration;

    /**
     * Single-value sync primitive that holds the verification code once it arrives over MCS.
     *
     * <p>Shared with {@link #connection} so the MCS reader thread can hand the code straight to any
     * {@link #getPushCode()} caller.
     */
    private final FcmPushCode pushCode;

    /**
     * Lifecycle state.
     *
     * @implNote
     * This implementation transitions the state via {@link AtomicReference#compareAndSet(Object, Object)} so
     * concurrent {@link #authenticate(LinkedWhatsAppClientDevice)} callers see a consistent view and only one wins the race.
     */
    private final AtomicReference<State> state;

    /**
     * Session bound during {@link #authenticate(LinkedWhatsAppClientDevice)} or {@link #loadSession(FcmSession, URI)}.
     *
     * <p>{@code null} until authentication succeeds. Reset to {@code null} on auth failure so a retry sees a clean
     * slate.
     */
    private volatile FcmSession session;

    /**
     * MCS connection owning the long-lived TLS stream.
     *
     * <p>{@code null} until authentication completes; closed and dereferenced by {@link #close()}.
     */
    private volatile FcmMcsConnection connection;

    /**
     * Builds the I/O collaborators only.
     *
     * <p>Public callers go through the static factories; this constructor never touches the network.
     *
     * @param proxy proxy URI, or {@code null} for direct
     */
    private FcmClient(URI proxy) {
        this.registration = new FcmRegistration(proxy);
        this.pushCode = new FcmPushCode();
        this.state = new AtomicReference<>(State.UNAUTHENTICATED);
    }

    /**
     * Creates a fresh, unauthenticated client that dials Google directly.
     *
     * <p>The caller must call {@link #authenticate(LinkedWhatsAppClientDevice)} before any of the read-only accessors become
     * usable.
     *
     * @return a new unauthenticated client
     */
    public static FcmClient newSession() {
        return new FcmClient(null);
    }

    /**
     * Creates a fresh, unauthenticated client routed through {@code proxy}.
     *
     * <p>The proxy is forwarded to the underlying {@link java.net.http.HttpClient} the registration helper builds; the
     * MCS TLS stream itself does not honour the proxy.
     *
     * @param proxy proxy URI ({@code http(s)://...}, {@code socks://...}), or {@code null} for direct
     * @return a new unauthenticated client
     */
    public static FcmClient newSession(URI proxy) {
        return new FcmClient(proxy);
    }

    /**
     * Restores a client from a previously captured {@link FcmSession} and starts the background MCS listener.
     *
     * <p>Re-runs the FIS step if the cached auth token has expired. The returned client is already in
     * {@link State#AUTHENTICATED}, so {@link #authenticate(LinkedWhatsAppClientDevice)} would throw.
     *
     * @param session the session previously obtained from {@link #getSession()}
     * @return a restored, listening client
     * @throws IOException          if re-registration fails
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static FcmClient loadSession(FcmSession session) throws IOException {
        return loadSession(session, null);
    }

    /**
     * Restores a client from a previously captured {@link FcmSession}, routed through {@code proxy}.
     *
     * <p>Re-runs the FIS step if the cached auth token has expired and starts the background MCS listener. The
     * returned client is already in {@link State#AUTHENTICATED}.
     *
     * @param session the session previously obtained from {@link #getSession()}
     * @param proxy   proxy URI, or {@code null} for direct
     * @return a restored, listening client
     * @throws IOException          if re-registration fails
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static FcmClient loadSession(FcmSession session, URI proxy) throws IOException {
        Objects.requireNonNull(session, "session");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loading fcm session");
        var client = new FcmClient(proxy);
        client.session = session;
        client.registration.ensureCredentials(session);
        client.state.set(State.AUTHENTICATED);
        client.connection = new FcmMcsConnection(session, client.pushCode);
        client.connection.start();
        if (Log.INFO) LOGGER.log(Level.INFO, "fcm client authenticated from saved session");
        return client;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the cached two-element set {@code {ANDROID, ANDROID_BUSINESS}} because the FCM/MCS
     * pipeline is Android-specific.
     */
    @Override
    public Set<ClientPlatformType> supportedPlatforms() {
        return SUPPORTED_PLATFORMS;
    }

    /**
     * Selects the {@link FcmConfig} matching {@code device.platform()}, runs the three-step Android registration, and
     * starts the background MCS listener.
     *
     * <p>{@link FcmConfig#WHATSAPP_PERSONAL} is chosen for {@link ClientPlatformType#ANDROID} and
     * {@link FcmConfig#WHATSAPP_BUSINESS} for {@link ClientPlatformType#ANDROID_BUSINESS}. Only the first concurrent
     * caller actually runs the registration; any other caller observing {@link State#AUTHENTICATING} or
     * {@link State#AUTHENTICATED} throws {@link IllegalStateException}. On failure the state reverts to
     * {@link State#UNAUTHENTICATED} so the caller may retry.
     *
     * @implNote
     * This implementation guards the lifecycle with an {@link AtomicReference#compareAndSet(Object, Object)} on the
     * state, runs the registration via {@link FcmRegistration}, and starts the listener via
     * {@link FcmMcsConnection#start()}.
     *
     * @param device the device whose platform selects the WA config
     * @throws IllegalArgumentException if {@code device.platform()} is neither {@code ANDROID} nor
     *                                  {@code ANDROID_BUSINESS}
     * @throws IllegalStateException    if the client is already authenticating, authenticated, or closed
     * @throws UncheckedIOException     wrapping any HTTP or protocol failure
     */
    @Override
    public void authenticate(LinkedWhatsAppClientDevice device) {
        Objects.requireNonNull(device, "device");
        var platform = device.platform();
        var config = switch (platform) {
            case ANDROID -> FcmConfig.WHATSAPP_PERSONAL;
            case ANDROID_BUSINESS -> FcmConfig.WHATSAPP_BUSINESS;
            default -> throw new IllegalArgumentException(
                    "FcmClient.authenticate requires ANDROID or ANDROID_BUSINESS, got " + platform);
        };
        if (!state.compareAndSet(State.UNAUTHENTICATED, State.AUTHENTICATING)) {
            throw new IllegalStateException("FcmClient already " + state.get());
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "authenticating fcm client (platform={0})", platform);
        try {
            this.session = FcmSession.newSession(config);
            registration.ensureCredentials(session);
            this.connection = new FcmMcsConnection(session, pushCode);
            this.connection.start();
            state.set(State.AUTHENTICATED);
            if (Log.INFO) LOGGER.log(Level.INFO, "fcm client authenticated (platform={0})", platform);
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "fcm authentication failed", e);
            rollbackAuthentication();
            throw new UncheckedIOException(e);
        } catch (RuntimeException | Error e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "fcm authentication failed unexpectedly", e);
            rollbackAuthentication();
            throw e;
        }
    }

    /**
     * Wipes the partial state left by a failed {@link #authenticate(LinkedWhatsAppClientDevice)} attempt and reverts the lifecycle
     * back to {@link State#UNAUTHENTICATED}.
     *
     * <p>Lets the caller retry authentication after a transient HTTP failure without leaking a half-built
     * {@link #session} or {@link #connection}.
     */
    private void rollbackAuthentication() {
        this.session = null;
        this.connection = null;
        state.set(State.UNAUTHENTICATED);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fcm client state -> UNAUTHENTICATED");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reports {@code true} only when the {@link AtomicReference} lifecycle state is exactly
     * {@link State#AUTHENTICATED}; both {@link State#AUTHENTICATING} and {@link State#CLOSED} return {@code false}.
     */
    @Override
    public boolean isAuthenticated() {
        return state.get() == State.AUTHENTICATED;
    }

    /**
     * Returns the live {@link FcmSession} backing this client.
     *
     * <p>Callers may pass it back to {@link #loadSession(FcmSession)} on a future run, or hand it to the protobuf
     * codec for byte-level persistence. The returned object is mutated by the MCS reader thread as persistent ids
     * arrive; callers that serialise the session concurrently with an active MCS connection should snapshot
     * {@link FcmSession#persistentIds()} first.
     *
     * @return the live session
     * @throws IllegalStateException if the client is not authenticated
     */
    public FcmSession getSession() {
        requireAuthenticated();
        return session;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation forwards to {@link FcmSession#fcmToken()} after asserting the client is in
     * {@link State#AUTHENTICATED}; the value is the FCM registration token established during the register3 step.
     *
     * @throws IllegalStateException if the client is not authenticated
     */
    @Override
    public String getPushToken() {
        requireAuthenticated();
        return session.fcmToken();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until the silent FCM data push WhatsApp's server emits in response to {@code /v2/exist} arrives, or
     * until {@link #close()} is invoked. Safe to call from multiple threads concurrently; every caller sees the same
     * delivered code, since the WhatsApp registration flow only ever sends one per session. The returned value is the
     * code carried in {@code app_data.registration_code}.
     *
     * @implNote
     * This implementation blocks on {@link FcmPushCode#waitForCode()} and translates its checked exceptions into the
     * unchecked failures documented below.
     *
     * @throws UncheckedIOException  if the client has been closed before a code was delivered
     * @throws RuntimeException      if the caller is interrupted while waiting; the interrupt flag is restored before
     *                               the exception is thrown
     * @throws IllegalStateException if the client is not authenticated
     */
    @Override
    public String getPushCode() {
        requireAuthenticated();
        try {
            var code = pushCode.waitForCode();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fcm push code received");
            return code;
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "fcm push code wait failed", e);
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "fcm push code wait interrupted", e);
            throw new RuntimeException("FcmClient.getPushCode interrupted", e);
        }
    }

    /**
     * Throws {@link IllegalStateException} unless the client is in {@link State#AUTHENTICATED}.
     *
     * <p>Guards the accessors that depend on a populated {@link #session} and a running MCS listener.
     *
     * @throws IllegalStateException if the lifecycle state is anything other than {@link State#AUTHENTICATED}
     */
    private void requireAuthenticated() {
        var current = state.get();
        if (current != State.AUTHENTICATED) {
            throw new IllegalStateException(
                    "FcmClient must be authenticated first; current state=" + current);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stops the background MCS reader and heartbeat threads via {@link FcmMcsConnection#close()},
     * tears down the TLS socket, transitions the lifecycle to {@link State#CLOSED}, and unblocks every pending
     * {@link #getPushCode()} caller via {@link FcmPushCode#close()}. Idempotent; a second call after
     * {@link State#CLOSED} returns immediately.
     */
    @Override
    public void close() {
        var prev = state.getAndSet(State.CLOSED);
        if (prev == State.CLOSED) {
            return;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "closing fcm client");
        var c = connection;
        if (c != null) {
            c.close();
        }
        pushCode.close();
    }
}
