package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.exception.linked.WhatsAppStreamException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.DevicePropsBuilder;
import com.github.auties00.cobalt.wire.linked.device.DevicePropsHistorySyncConfigBuilder;
import com.github.auties00.cobalt.wire.linked.device.DevicePropsSpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.*;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPayload.DevicePairingRegistrationData;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPayload.UserAgent;
import com.github.auties00.cobalt.wire.linked.signal.CertChainSpec;
import com.github.auties00.cobalt.wire.linked.signal.NoiseCertificateCertChainDetailsSpec;
import com.github.auties00.cobalt.wire.linked.signal.NoiseCertificateDetailsSpec;
import com.github.auties00.cobalt.wire.linked.signal.NoiseCertificateSpec;
import com.github.auties00.cobalt.socket.datagram.WhatsAppDatagramInputStream;
import com.github.auties00.cobalt.socket.datagram.WhatsAppDatagramOutputStream;
import com.github.auties00.cobalt.socket.tunnel.HttpTunnel;
import com.github.auties00.cobalt.socket.tunnel.SocksTunnel;
import com.github.auties00.cobalt.socket.websocket.WebSocketFrameInputStream;
import com.github.auties00.cobalt.socket.websocket.WebSocketFrameOutputStream;
import com.github.auties00.cobalt.socket.websocket.WebSocketUpgrade;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.binary.StanzaReader;
import com.github.auties00.cobalt.stanza.binary.StanzaSizer;
import com.github.auties00.cobalt.stanza.binary.StanzaTokens;
import com.github.auties00.cobalt.stanza.binary.StanzaWriter;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocket;
import javax.security.auth.DestroyFailedException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Top-level WhatsApp socket client that performs the Noise XX
 * handshake and exchanges {@code int24}-prefixed AES-GCM datagrams
 * with the WhatsApp server.
 *
 * <p>The class is sealed into two transport-shaped subtypes that map
 * to the two physical wires WhatsApp servers expose:
 * <ul>
 *   <li>{@link Tcp}: plain TCP to {@code g.whatsapp.net:443}; used by
 *       iOS, Android and the native macOS app (Mac Catalyst port of
 *       the iOS binary).</li>
 *   <li>{@link WebSocket}: TLS plus RFC 6455 WebSocket to
 *       {@code web.whatsapp.com:443}; used by the browser and the
 *       Windows Electron desktop app (which ships the same JS bundle
 *       as the browser).</li>
 * </ul>
 *
 * <p>Handshake shape (prologue, certificate verification and client
 * payload) is decided <em>orthogonally</em> from the transport, off
 * the {@code store.accountStore().device().platform()} value: {@code IOS},
 * {@code IOS_BUSINESS}, {@code ANDROID} and {@code ANDROID_BUSINESS}
 * use the mobile-style handshake (flat {@code NoiseCertificate},
 * mobile prologue, mobile {@code ClientPayload}); {@code WEB},
 * {@code WINDOWS} and {@code MACOS} use the web-style handshake
 * (two-level {@code CertChain}, web prologue, companion
 * {@code ClientPayload}). macOS therefore rides on the {@link Tcp}
 * transport but uses the web-style handshake, and the transport vs
 * handshake-shape distinction is what motivates keeping the two
 * concerns split.
 *
 * <p>Every subtype builds the same logical I/O pipeline as a chain
 * of standard {@code Filter*Stream} filters:
 * <pre>
 * Tcp       : raw Socket -> WhatsAppDatagram{In,Out}putStream
 * WebSocket : raw Socket -> SSLSocket -> WebSocketFrame{In,Out}putStream -> WhatsAppDatagram{In,Out}putStream
 * </pre>
 *
 * <p>During the Noise XX handshake the datagram streams are in their
 * pre-handshake passthrough mode (no AES-GCM) and the handshake
 * messages are framed directly through
 * {@link WhatsAppSocketHandshake#writeClientHandshake(HandshakeMessage)}
 * and {@link WhatsAppSocketHandshake#readServerHandshake()}. Once the
 * handshake derives the read and write keys they are installed on
 * the datagram streams via
 * {@link WhatsAppDatagramInputStream#setReadKey(SecretKey)} and
 * {@link WhatsAppDatagramOutputStream#setWriteKey(SecretKey)}, and
 * every subsequent {@link #sendNode(Stanza)} is encoded straight into
 * the wire by {@link StanzaWriter#toStream(OutputStream)}; the reader
 * thread mirrors the same chain by decoding one node per datagram frame
 * with {@link StanzaReader#fromStream(InputStream, int)}.
 *
 * <p>HTTP {@code CONNECT} and SOCKS4/4a/5/5h proxies are supported
 * on every form factor through {@link HttpTunnel} and
 * {@link SocksTunnel}; {@link WhatsAppClientProxy.Http.Secure}
 * additionally TLS-wraps the proxy hop before sending
 * {@code CONNECT}.
 */
@WhatsAppWebModule(moduleName = "WANoiseSocket")
@WhatsAppWebModule(moduleName = "WAFrameSocket")
public sealed abstract class WhatsAppSocketClient {

    /**
     * The logger for {@link WhatsAppSocketClient}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppSocketClient.class);

    /**
     * The 32-byte Ed25519 public key of the WhatsApp Noise root CA.
     *
     * <p>This is the trust anchor for the two-level certificate chain (root
     * then intermediate then leaf) that the server presents during the Noise
     * handshake. The intermediate must have {@code issuerSerial == 0} and be
     * signed by this key; the leaf must be signed by the intermediate and
     * its embedded key must match the server static key carried in the Noise
     * hello.
     */
    private static final byte[] NOISE_ROOT_CA_PUBLIC_KEY = HexFormat.of().parseHex(
            "142375574d0a587166aae71ebe516437c4a28b73e3695c6ce1f7f9545da8ee6b"
    );

    /**
     * The two-byte WhatsApp protocol identifier ({@code "WA"})
     * prepended to every handshake prologue.
     */
    private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);

    /**
     * The two-byte web version footer appended to
     * {@link #WHATSAPP_VERSION_HEADER} to form the web handshake
     * prologue.
     *
     * <p>This footer is used by every companion (browser, Windows Electron,
     * macOS native app).
     */
    private static final byte[] WEB_VERSION = new byte[]{6, StanzaTokens.DICTIONARY_VERSION};

    /**
     * The handshake prologue advertised by companion-device clients.
     */
    private static final byte[] WEB_PROLOGUE = DataUtils.concatByteArrays(WHATSAPP_VERSION_HEADER, WEB_VERSION);

    /**
     * The two-byte mobile version footer appended to
     * {@link #WHATSAPP_VERSION_HEADER} to form the mobile handshake
     * prologue.
     */
    private static final byte[] MOBILE_VERSION = new byte[]{5, StanzaTokens.DICTIONARY_VERSION};

    /**
     * The handshake prologue advertised by native mobile clients
     * (iOS, Android).
     */
    private static final byte[] MOBILE_PROLOGUE = DataUtils.concatByteArrays(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);

    /**
     * The number of bytes in the {@code int24} length prefix that
     * frames every datagram on the wire.
     */
    private static final int INT24_BYTE_SIZE = 3;

    /**
     * The defensive upper bound on a single Noise handshake message.
     */
    private static final int MAX_HANDSHAKE_MESSAGE_LENGTH = 0xFFFF;

    /**
     * The connect timeout in milliseconds for the raw TCP open.
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 30_000;

    /**
     * Builds a WhatsApp socket client tailored to the platform
     * recorded in {@code store}, with the default Chrome-fingerprinted
     * TLS configuration.
     *
     * <p>This is equivalent to
     * {@link #newCipheredSocketClient(LinkedWhatsAppStore, WhatsAppSslContextFactory)}
     * with {@link WhatsAppSslContextFactory#chrome()} and is the standard
     * entry point for embedders that do not need a custom truststore or
     * trust-all factory. {@code WEB} and {@code WINDOWS} platforms get the
     * {@link WebSocket} transport over {@code web.whatsapp.com:443};
     * {@code IOS}, {@code IOS_BUSINESS}, {@code ANDROID},
     * {@code ANDROID_BUSINESS} and {@code MACOS} platforms get the
     * {@link Tcp} transport over {@code g.whatsapp.net:443}.
     *
     * @param store the WhatsApp store carrying the platform, identity
     *              keys and proxy configuration
     * @return a socket client ready to
     *         {@link #connect(WhatsAppSocketListener)}
     */
    public static WhatsAppSocketClient newCipheredSocketClient(LinkedWhatsAppStore store) {
        return newCipheredSocketClient(store, WhatsAppSslContextFactory.chrome());
    }

    /**
     * Builds a WhatsApp socket client tailored to the platform
     * recorded in {@code store}, with the supplied SSL configuration.
     *
     * <p>This overload substitutes a trust-all factory in tests driving a
     * locally-signed proxy, or a custom-truststore factory for enterprise CA
     * pinning. The factory governs both the end-to-end TLS hop on
     * {@link WebSocket} and the optional TLS-to-proxy hop on every form
     * factor.
     *
     * @param store               the WhatsApp store
     * @param sslContextFactory   the SSL configuration applied to
     *                            every TLS hop on this connection
     * @return a socket client wired with the supplied SSL context
     *         factory
     * @throws NullPointerException if {@code store} or
     *                              {@code sslContextFactory} is
     *                              {@code null}
     */
    public static WhatsAppSocketClient newCipheredSocketClient(LinkedWhatsAppStore store, WhatsAppSslContextFactory sslContextFactory) {
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(sslContextFactory, "sslContextFactory cannot be null");
        var platform = store.accountStore().device().platform();
        return switch (platform) {
            case WEB, WINDOWS -> new WebSocket(store, sslContextFactory);
            default -> new Tcp(store, sslContextFactory);
        };
    }

    /**
     * The WhatsApp store, owning the identity keys and platform
     * metadata required to assemble the handshake payload.
     */
    final LinkedWhatsAppStore store;

    /**
     * The SSL configuration applied to every TLS hop on this
     * connection.
     *
     * <p>This covers both the optional TLS-to-proxy hop performed by
     * {@link #openTunneledSocket(InetSocketAddress)} and the end-to-end hop
     * on {@link WebSocket}. The same factory is used for both hops so a
     * caller-supplied {@link WhatsAppSslContextFactory} (typically a
     * trust-all factory in tests, or a custom-truststore factory for
     * enterprise CA pinning) applies consistently.
     */
    final WhatsAppSslContextFactory sslContextFactory;

    /**
     * The listener that receives deserialized nodes, errors and the
     * close event; set on every
     * {@link #connect(WhatsAppSocketListener)}.
     */
    private WhatsAppSocketListener listener;

    /**
     * The virtual thread that drains {@code int24}-framed datagrams
     * off the transport after the Noise handshake completes.
     */
    private volatile Thread readerThread;

    /**
     * The sticky flag set by {@link #disconnect()} so the reader loop
     * can distinguish an orderly local close from a server-side drop.
     */
    private volatile boolean closed;

    /**
     * The datagram input stream that strips the {@code int24} prefix
     * and decrypts inbound AES-GCM datagrams.
     *
     * <p>This lives on top of the subtype-specific transport stream chain
     * built in {@link #openTransport()}; subtypes assign this field directly
     * inside {@code openTransport}.
     */
    protected WhatsAppDatagramInputStream in;

    /**
     * The datagram output stream that encrypts outbound plaintext
     * with AES-GCM and writes the {@code int24}-prefixed ciphertext.
     *
     * <p>This lives on top of the subtype-specific transport stream chain
     * built in {@link #openTransport()}; subtypes assign this field directly
     * inside {@code openTransport}.
     */
    protected WhatsAppDatagramOutputStream out;

    /**
     * The AES write key derived from the Noise handshake.
     *
     * <p>It is retained so the package-private test accessor
     * {@link #writeKey()} can verify the handshake completed and so
     * {@link #disconnect()} can destroy the key material eagerly.
     */
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    private volatile SecretKeySpec writeKey;

    /**
     * The AES read key derived from the Noise handshake; retained for
     * the same reasons as {@link #writeKey}.
     */
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    private volatile SecretKeySpec readKey;

    /**
     * The mutex that serialises concurrent {@link #sendNode(Stanza)}
     * callers so the datagram output stream observes one complete
     * stanza-encode per acquired monitor, with no interleaving across
     * senders.
     */
    private final Object writeLock = new Object();

    /**
     * The wall-clock duration of the transport-open phase, captured
     * between the entry of {@link #connect(WhatsAppSocketListener)}
     * and the point at which the transport reports as open.
     *
     * <p>This is exposed via {@link #socketConnectDuration()} for telemetry
     * consumers.
     */
    private volatile Duration socketConnectDuration;

    /**
     * The wall-clock duration of the Noise XX handshake, captured
     * from the first byte of {@code ClientHello} until the read and
     * write keys have been derived.
     *
     * <p>This is exposed via {@link #authHandshakeDuration()} for telemetry
     * consumers.
     */
    private volatile Duration authHandshakeDuration;

    /**
     * Constructs the common base shared by every subtype.
     *
     * @param store             the WhatsApp store
     * @param sslContextFactory the SSL configuration applied to
     *                          every TLS hop on this connection
     */
    private WhatsAppSocketClient(LinkedWhatsAppStore store, WhatsAppSslContextFactory sslContextFactory) {
        this.store = store;
        this.sslContextFactory = sslContextFactory;
    }

    /**
     * Opens the underlying connection and performs the Noise XX
     * handshake, dispatching decoded nodes to {@code listener}.
     *
     * <p>This is the single entry point for application code: it combines
     * transport open, Noise handshake and reader-thread start under one call
     * so the caller has no separate ordering responsibility. The supplied
     * {@code listener} is bound for the lifetime of this connection;
     * subsequent reconnects need a fresh listener (or the same instance
     * reset by the caller).
     *
     * @param listener the callback that receives decoded nodes,
     *                 errors and the close event
     * @throws IOException if the connection or handshake fails
     */
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    public final void connect(WhatsAppSocketListener listener) throws IOException {
        Objects.requireNonNull(listener, "listener cannot be null");
        this.listener = listener;
        this.closed = false;

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "connecting socket client, platform={0}", store.accountStore().device().platform());
        var socketOpenStart = Instant.now();
        try {
            openTransport();
        } catch (IOException failure) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "socket transport open failed", failure);
            throw failure;
        }
        this.socketConnectDuration = Duration.between(socketOpenStart, Instant.now());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "socket transport open in {0}", socketConnectDuration);

        try {
            performNoiseHandshake();
        } catch (IOException failure) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "noise handshake failed", failure);
            throw failure;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "noise handshake complete in {0}", authHandshakeDuration);

        startReaderThread();
        if (Log.INFO) LOGGER.log(Level.INFO, "socket connected");
    }

    /**
     * Opens the platform-specific transport and assigns the
     * {@link #in} and {@link #out} fields with the freshly-built
     * datagram stream pair on top of it.
     *
     * @implSpec
     * Implementations must populate both {@link #in} and {@link #out}
     * before returning; the caller assumes the datagram streams are
     * ready for handshake traffic immediately after this method
     * returns.
     *
     * @throws IOException if the transport cannot be opened
     */
    abstract void openTransport() throws IOException;

    /**
     * Closes the transport opened by {@link #openTransport()}.
     *
     * @implSpec
     * Implementations should close idempotently and tolerate being
     * invoked on a transport that was never successfully opened.
     *
     * @throws IOException if the transport cannot be closed cleanly
     */
    abstract void closeTransport() throws IOException;

    /**
     * Returns whether the transport opened by
     * {@link #openTransport()} is currently open.
     *
     * @implSpec
     * Implementations must report {@code true} only while the underlying
     * connection can still carry traffic in both directions.
     *
     * @return {@code true} if connected, {@code false} otherwise
     */
    abstract boolean isTransportOpen();

    /**
     * Reports whether this client uses the web-style handshake shape
     * instead of the mobile-style handshake shape.
     *
     * <p>The selection is driven by the device platform and is independent
     * of the transport: {@code WEB}, {@code WINDOWS} and {@code MACOS} all
     * use the web shape (the macOS native app does even though it rides on
     * the {@link Tcp} transport), while {@code IOS} and {@code ANDROID} (and
     * their business variants) use the mobile shape.
     *
     * @return {@code true} for the web handshake shape (web
     *         prologue, two-level certificate chain, companion
     *         {@code ClientPayload}), {@code false} for the mobile
     *         handshake shape (mobile prologue, flat
     *         {@code NoiseCertificate}, mobile {@code ClientPayload})
     */
    private boolean isWebHandshakeShape() {
        return switch (store.accountStore().device().platform()) {
            case WEB, WINDOWS, MACOS -> true;
            case IOS, IOS_BUSINESS, ANDROID, ANDROID_BUSINESS -> false;
            default -> throw new IllegalStateException("Unexpected value: " + store.accountStore().device().platform());
        };
    }

    /**
     * Returns the handshake prologue for the current client.
     *
     * <p>A defensive {@code clone()} of the static prologue is returned so
     * the handshake can mutate the buffer (currently it does not, but the
     * contract preserves safety against future caller-side mutation).
     *
     * @return the prologue bytes
     */
    private byte[] handshakePrologue() {
        return isWebHandshakeShape() ? WEB_PROLOGUE.clone() : MOBILE_PROLOGUE.clone();
    }

    /**
     * Verifies the server certificate carried in the Noise
     * handshake.
     *
     * <p>Companion-device clients (web shape) receive a two-level chain
     * (intermediate plus leaf) verified against the WhatsApp root CA; native
     * mobile clients (mobile shape) receive a flat {@code NoiseCertificate}
     * verified against the same root.
     *
     * @param decryptedCertificate the decrypted certificate payload
     * @param serverStaticKey      the 32-byte server static public
     *                             key extracted from the Noise hello
     * @throws IOException if the certificate is malformed or invalid
     */
    private void verifyCertificateChain(byte[] decryptedCertificate, byte[] serverStaticKey) throws IOException {
        try {
            if (isWebHandshakeShape()) {
                verifyWebCertificateChain(decryptedCertificate, serverStaticKey);
            } else {
                verifyMobileCertificate(decryptedCertificate, serverStaticKey);
            }
        } catch (IOException failure) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "certificate chain verification failed", failure);
            throw failure;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "certificate chain verified");
    }

    /**
     * Verifies the two-level companion certificate chain
     * (intermediate plus leaf) against the embedded root CA.
     *
     * @param decryptedCertificate the decrypted certificate payload
     * @param serverStaticKey      the 32-byte server static key
     * @throws IOException if the chain is malformed or any signature
     *         fails to verify
     */
    private static void verifyWebCertificateChain(byte[] decryptedCertificate, byte[] serverStaticKey) throws IOException {
        var certChain = CertChainSpec.decode(decryptedCertificate);
        var intermediate = certChain.intermediate()
                .orElseThrow(() -> new IOException("Certificate chain missing intermediate certificate"));
        var leaf = certChain.leaf()
                .orElseThrow(() -> new IOException("Certificate chain missing leaf certificate"));

        var intermediateDetails = intermediate.details()
                .orElseThrow(() -> new IOException("Intermediate certificate missing details"));
        var intermediateSignature = intermediate.signature()
                .orElseThrow(() -> new IOException("Intermediate certificate missing signature"));
        var parsedIntermediate = NoiseCertificateCertChainDetailsSpec.decode(intermediateDetails);

        var intermediateIssuerSerial = parsedIntermediate.issuerSerial()
                .orElseThrow(() -> new IOException("Intermediate certificate missing issuerSerial"));
        if (intermediateIssuerSerial != 0) {
            throw new IOException("Intermediate certificate was not issued by root CA, issuerSerial: " + intermediateIssuerSerial);
        }

        if (!Curve25519.verifySignature(NOISE_ROOT_CA_PUBLIC_KEY, intermediateDetails, ensureSignatureSize(intermediateSignature))) {
            throw new IOException("Intermediate certificate has invalid signature");
        }

        var leafDetails = leaf.details()
                .orElseThrow(() -> new IOException("Leaf certificate missing details"));
        var leafSignature = leaf.signature()
                .orElseThrow(() -> new IOException("Leaf certificate missing signature"));
        var parsedLeaf = NoiseCertificateCertChainDetailsSpec.decode(leafDetails);

        var leafIssuerSerial = parsedLeaf.issuerSerial()
                .orElseThrow(() -> new IOException("Leaf certificate missing issuerSerial"));
        var intermediateSerial = parsedIntermediate.serial()
                .orElseThrow(() -> new IOException("Intermediate certificate missing serial"));
        if (leafIssuerSerial != intermediateSerial) {
            throw new IOException("Leaf certificate was not issued by intermediate");
        }

        var intermediateKey = parsedIntermediate.key()
                .orElseThrow(() -> new IOException("Intermediate certificate missing key"));
        if (!Curve25519.verifySignature(intermediateKey, leafDetails, ensureSignatureSize(leafSignature))) {
            throw new IOException("Leaf certificate has invalid signature");
        }

        var leafKey = parsedLeaf.key()
                .orElseThrow(() -> new IOException("Leaf certificate missing key"));
        if (!Arrays.equals(leafKey, serverStaticKey)) {
            throw new IOException("Leaf certificate key does not match handshake server static key");
        }
    }

    /**
     * Verifies the flat {@code NoiseCertificate} that the mobile
     * server returns instead of a two-level chain.
     *
     * @param decryptedCertificate the decrypted certificate payload
     * @param serverStaticKey      the 32-byte server static key
     * @throws IOException if any check fails
     */
    private static void verifyMobileCertificate(byte[] decryptedCertificate, byte[] serverStaticKey) throws IOException {
        var cert = NoiseCertificateSpec.decode(decryptedCertificate);
        var details = cert.details()
                .orElseThrow(() -> new IOException("NoiseCertificate missing details"));
        var signature = cert.signature()
                .orElseThrow(() -> new IOException("NoiseCertificate missing signature"));

        if (!Curve25519.verifySignature(NOISE_ROOT_CA_PUBLIC_KEY, details, ensureSignatureSize(signature))) {
            throw new IOException("NoiseCertificate has invalid signature");
        }

        var parsedDetails = NoiseCertificateDetailsSpec.decode(details);
        var key = parsedDetails.key()
                .orElseThrow(() -> new IOException("NoiseCertificate missing key"));
        if (!Arrays.equals(key, serverStaticKey)) {
            throw new IOException("NoiseCertificate key does not match handshake server static key");
        }
    }

    /**
     * Builds the serialized client payload sent inside the
     * {@code ClientFinish} message.
     *
     * @return the serialized client payload
     */
    private byte[] handshakePayload() {
        var payload = isWebHandshakeShape() ? webClientPayload() : mobileClientPayload();
        return ClientPayloadSpec.encode(payload);
    }

    /**
     * Builds the companion-device client payload (web handshake
     * shape).
     *
     * <p>This returns a reconnection payload when the store already carries a
     * JID, otherwise a fresh pairing payload that includes registration
     * data. Either way the {@code webInfo} sub-platform is derived from
     * {@code store.accountStore().device().platform()} via {@link #webSubPlatform()}.
     *
     * @return the client payload
     * @implNote The reconnection payload sets {@code lidDbMigrated = true}, exactly as a live
     * WhatsApp Web companion does. This flag tells the server the companion stores and addresses
     * by Linked Identity (LID); without it the server stamps the companion's outbound stanzas with
     * a phone-number device {@code from} (a {@code user:device@s.whatsapp.net} PhoneNumberDeviceJid).
     * The recipient's VoIP layer ({@code voipBridgeJidToDeviceJID}) rejects a companion caller in
     * that form and silently drops the call before it rings, whereas a primary (device {@code 0})
     * phone-number caller is accepted. It is independent of the account-wide 1:1 LID migration AB
     * prop, which gates per-chat addressing rather than the companion's own device identity.
     */
    private ClientPayload webClientPayload() {
        var agent = webUserAgent();
        var webInfo = new ClientPayloadWebInfoBuilder()
                .webSubPlatform(webSubPlatform())
                .build();
        var jid = store.accountStore().jid();
        if (jid.isPresent()) {
            return new ClientPayloadBuilder()
                    .connectType(ClientPayload.ConnectType.WIFI_UNKNOWN)
                    .connectReason(ClientPayload.ConnectReason.USER_ACTIVATED)
                    .userAgent(agent)
                    .webInfo(webInfo)
                    .username(Long.parseLong(jid.get().user()))
                    .passive(true)
                    .pull(true)
                    .device(jid.get().device())
                    .lidDbMigrated(true)
                    .build();
        }
        return new ClientPayloadBuilder()
                .connectType(ClientPayload.ConnectType.WIFI_UNKNOWN)
                .connectReason(ClientPayload.ConnectReason.USER_ACTIVATED)
                .userAgent(agent)
                .webInfo(webInfo)
                .devicePairingData(createRegisterData())
                .passive(false)
                .pull(false)
                .build();
    }

    /**
     * Builds the user agent advertised inside the companion
     * handshake payload (web handshake shape).
     *
     * <p>On the Windows hybrid shell the six-digit {@code windowsBuild} URL
     * parameter is copied into {@code appVersion.quaternary} by
     * {@code WhatsAppWindowsClientInfo}; when six characters long, the build
     * halves overwrite the mcc/mnc fields in the user agent. This method
     * mirrors only the mcc/mnc override here; the quaternary itself is
     * already set on the cached {@code ClientAppVersion}.
     *
     * @return the user agent value
     */
    private UserAgent webUserAgent() {
        var appVersion = store.accountStore().clientVersion();
        var mcc = "000";
        var mnc = "000";
        var devicePlatform = store.accountStore().device().platform();
        if (devicePlatform == ClientPlatformType.WINDOWS
            && appVersion.quaternary().isPresent()) {
            var buildStr = Integer.toString(appVersion.quaternary().getAsInt());
            if (buildStr.length() == 6) {
                mcc = buildStr.substring(0, 3);
                mnc = buildStr.substring(3, 6);
            }
        }
        return new ClientPayloadUserAgentBuilder()
                .platform(webUserAgentPlatform())
                .appVersion(appVersion)
                .mcc(mcc)
                .mnc(mnc)
                .releaseChannel(store.accountStore().releaseChannel())
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("US")
                .deviceType(ClientPayload.ClientType.PHONE)
                .deviceModelType(store.accountStore().device() instanceof LinkedWhatsAppClientDevice.Mobile mobile ? mobile.modelId() : null)
                .build();
    }

    /**
     * Returns the {@link ClientPlatformType} advertised in the user
     * agent for the web handshake shape.
     *
     * <p>The Windows hybrid shell keeps {@code UserAgent.platform == WEB} and
     * only distinguishes itself from a browser through the
     * {@code WebInfo.webSubPlatform} field; the macOS native app advertises
     * {@code MACOS}; the browser advertises {@code WEB}.
     *
     * @return the platform value advertised at handshake time
     */
    private ClientPlatformType webUserAgentPlatform() {
        return switch (store.accountStore().device().platform()) {
            case WINDOWS, WEB -> ClientPlatformType.WEB;
            case MACOS -> ClientPlatformType.MACOS;
            default -> throw new IllegalStateException(
                    "Unexpected web handshake platform: " + store.accountStore().device().platform());
        };
    }

    /**
     * Returns the {@code WebSubPlatform} that this client advertises
     * inside the {@code webInfo} block of the companion handshake.
     *
     * @return the sub-platform value advertised at handshake time
     */
    private ClientPayload.WebInfo.WebSubPlatform webSubPlatform() {
        return switch (store.accountStore().device().platform()) {
            case WEB -> ClientPayload.WebInfo.WebSubPlatform.WEB_BROWSER;
            case WINDOWS -> ClientPayload.WebInfo.WebSubPlatform.WIN_HYBRID;
            case MACOS -> ClientPayload.WebInfo.WebSubPlatform.DARWIN;
            default -> throw new IllegalStateException(
                    "Unexpected web handshake platform: " + store.accountStore().device().platform());
        };
    }

    /**
     * Creates the device-pairing registration data for a new
     * companion-device session, including the encoded device
     * properties.
     *
     * @return the registration data
     */
    private DevicePairingRegistrationData createRegisterData() {
        return new ClientPayloadDevicePairingRegistrationDataBuilder()
                .buildHash(store.accountStore().clientVersion().toHash())
                .eRegid(DataUtils.intToBytes(store.signalStore().registrationId(), 4))
                .eKeytype(DataUtils.intToBytes(SignalIdentityPublicKey.type(), 1))
                .eIdent(store.signalStore().identityKeyPair().publicKey().toEncodedPoint())
                .eSkeyId(DataUtils.intToBytes(store.signalStore().signedKeyPair().id(), 3))
                .eSkeyVal(store.signalStore().signedKeyPair().publicKey().toEncodedPoint())
                .eSkeySig(store.signalStore().signedKeyPair().signature())
                .deviceProps(createCompanionProps())
                .build();
    }

    /**
     * Creates and encodes the companion device properties.
     *
     * @return the encoded companion device properties
     */
    private byte[] createCompanionProps() {
        var sync = store.syncStore();
        var platform = store.accountStore().device().platform();
        var desktop = platform.isDesktop();
        var config = new DevicePropsHistorySyncConfigBuilder()
                .inlineInitialPayloadInE2EeMsg(true)
                .supportBotUserAgentChatHistory(true)
                .supportCagReactionsAndPolls(true)
                .supportRecentSyncChunkMessageCountTuning(true)
                .supportHostedGroupMsg(true)
                .supportBizHostedMsg(true)
                .supportFbidBotChatHistory(true)
                .supportMessageAssociation(true)
                .supportCallLogHistory(true)
                .supportGroupHistory(true);
        if (desktop) {
            config.onDemandReady(true)
                    .completeOnDemandReady(true);
        }
        if (sync.isFullHistorySyncRequired()) {
            sync.historyFullSyncDays().ifPresent(config::fullSyncDaysLimit);
        } else {
            var storageQuota = sync.historyStorageQuotaMb();
            if (storageQuota.isEmpty()) {
                storageQuota = estimateStorageQuotaMb();
            }
            storageQuota.ifPresent(config::storageQuotaMb);
        }
        sync.historyRecentSyncDays().ifPresent(config::recentSyncDaysLimit);
        sync.historyThumbnailSyncDays().ifPresent(config::thumbnailSyncDaysLimit);
        sync.historyMaxMessagesPerChat().ifPresent(config::initialSyncMaxMessagesPerChat);
        var platformType = switch (platform) {
            case IOS, IOS_BUSINESS -> DevicePlatformType.IOS_PHONE;
            case ANDROID, ANDROID_BUSINESS -> DevicePlatformType.ANDROID_PHONE;
            case WINDOWS -> DevicePlatformType.UWP;
            case MACOS -> DevicePlatformType.IOS_CATALYST;
            case WEB -> DevicePlatformType.CHROME;
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
        var os = switch (platform) {
            case IOS, IOS_BUSINESS -> "iOS";
            case ANDROID, ANDROID_BUSINESS -> "Android";
            case WINDOWS, WEB -> "Windows";
            case MACOS -> "Mac OS";
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
        var props = new DevicePropsBuilder()
                .os(os)
                .platformType(platformType)
                .requireFullSync(sync.isFullHistorySyncRequired())
                .historySyncConfig(config.build())
                .version(store.accountStore().clientVersion())
                .build();
        return DevicePropsSpec.encode(props);
    }

    /**
     * Estimates the storage budget in megabytes the companion advertises in
     * its {@code DeviceProps.historySyncConfig}.
     *
     * <p>Returns the usable space of the filesystem backing the host's home
     * directory, truncated to whole megabytes, or {@link OptionalInt#empty()}
     * when it cannot be determined.
     *
     * @return the storage budget in megabytes, or {@link OptionalInt#empty()}
     *
     * @implNote
     * This implementation mirrors WhatsApp Web, which derives
     * {@code storageQuotaMb} from {@code navigator.storage.estimate().quota}
     * truncated to megabytes and omits the field when the estimate is
     * unavailable. The usable space of the home filesystem is the closest
     * faithful analog in a non-browser runtime.
     */
    private OptionalInt estimateStorageQuotaMb() {
        try {
            var usableBytes = Files.getFileStore(Path.of(System.getProperty("user.home"))).getUsableSpace();
            if (usableBytes <= 0) {
                return OptionalInt.empty();
            }
            return OptionalInt.of((int) Math.min(Integer.MAX_VALUE, usableBytes / (1024L * 1024L)));
        } catch (Exception exception) {
            return OptionalInt.empty();
        }
    }

    /**
     * Builds the mobile client payload (mobile handshake shape).
     *
     * @return the client payload
     */
    private ClientPayload mobileClientPayload() {
        var agent = mobileUserAgent();
        var phoneNumber = store.accountStore()
                .phoneNumber()
                .orElseThrow(() -> new InternalError("Phone number was not set"));
        return new ClientPayloadBuilder()
                .username(phoneNumber)
                .passive(false)
                .pushName(store.accountStore().registered() ? store.accountStore().name().orElse(null) : null)
                .userAgent(agent)
                .shortConnect(true)
                .connectType(ClientPayload.ConnectType.WIFI_UNKNOWN)
                .connectReason(ClientPayload.ConnectReason.USER_ACTIVATED)
                .connectAttemptCount(0)
                .device(0)
                .oc(false)
                .build();
    }

    /**
     * Builds the mobile-specific user agent, including the device
     * manufacturer, model, OS version and FDID.
     *
     * @return the user agent value
     */
    private UserAgent mobileUserAgent() {
        return new ClientPayloadUserAgentBuilder()
                .platform(store.accountStore().device().platform())
                .appVersion(store.accountStore().clientVersion())
                .mcc("000")
                .mnc("000")
                .osVersion(store.accountStore().device().osDeviceAppVersion().toString())
                .manufacturer(store.accountStore().device().manufacturer())
                .device(store.accountStore().device().model().replaceAll("_", " "))
                .osBuildNumber(store.accountStore().device().osBuildNumber())
                .phoneId(store.signalStore().fdid().toString().toUpperCase())
                .releaseChannel(store.accountStore().releaseChannel())
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("US")
                .deviceType(ClientPayload.ClientType.PHONE)
                .deviceModelType(store.accountStore().device() instanceof LinkedWhatsAppClientDevice.Mobile mobile ? mobile.modelId() : null)
                .build();
    }

    /**
     * Returns the measured transport-open duration for the current
     * session.
     *
     * <p>This is useful for embedders that mirror WA Web's telemetry surface
     * or for local diagnostics.
     *
     * @return the duration, or {@code null} if the transport has not
     *         finished opening yet
     */
    public final Duration socketConnectDuration() {
        return socketConnectDuration;
    }

    /**
     * Returns the measured Noise handshake duration for the current
     * session.
     *
     * <p>This is useful for embedders that mirror WA Web's telemetry surface
     * or for local diagnostics.
     *
     * @return the duration, or {@code null} if the handshake has not
     *         finished yet
     */
    public final Duration authHandshakeDuration() {
        return authHandshakeDuration;
    }

    /**
     * Returns the AES write key derived by the Noise XX handshake.
     *
     * <p>This is a package-private hook for in-process tests that assert the
     * handshake completed; it is not exposed publicly because the key is
     * sensitive material destroyed by {@link #disconnect()}.
     *
     * @return the write key, or {@code null} if the handshake has
     *         not completed or has been torn down
     */
    final SecretKeySpec writeKey() {
        return writeKey;
    }

    /**
     * Returns the AES read key derived by the Noise XX handshake.
     *
     * <p>This is a package-private hook for in-process tests that assert the
     * handshake completed; it is not exposed publicly because the key is
     * sensitive material destroyed by {@link #disconnect()}.
     *
     * @return the read key, or {@code null} if the handshake has
     *         not completed or has been torn down
     */
    final SecretKeySpec readKey() {
        return readKey;
    }

    /**
     * Disconnects from the server and destroys the AES read and
     * write keys.
     *
     * <p>This is idempotent: it is safe to call on an already-closed client
     * or on one that never completed its handshake. A close requested this
     * way is silent: the reader thread exits without delivering
     * {@link WhatsAppSocketListener#onClose()}, since that callback signals an
     * unsolicited drop and must not fire against a caller-initiated teardown.
     *
     * @implNote
     * This implementation destroys the AES keys eagerly via
     * {@link SecretKeySpec#destroy()} so the secrets do not linger
     * in heap memory beyond the lifetime of the connection; WA Web
     * relies on the browser's garbage collector for the same effect.
     */
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    public final void disconnect() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "disconnecting socket client");
        this.closed = true;

        if (readKey != null) {
            try {
                readKey.destroy();
            } catch (DestroyFailedException _) {
            }
            readKey = null;
        }

        if (writeKey != null) {
            try {
                writeKey.destroy();
            } catch (DestroyFailedException _) {
            }
            writeKey = null;
        }

        try {
            closeTransport();
        } catch (IOException _) {
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "socket disconnected");
    }

    /**
     * Returns whether the underlying connection is currently open.
     *
     * <p>This combines the local {@link #closed} flag with the transport's
     * own open check so a server-side drop or a local {@link #disconnect()}
     * both surface as {@code false}.
     *
     * @return {@code true} if connected, {@code false} otherwise
     */
    public final boolean isConnected() {
        return !closed && isTransportOpen();
    }

    /**
     * Serialises a {@link Stanza} and sends it as one encrypted
     * datagram.
     *
     * <p>This is the standard send primitive for every outbound stanza: the
     * encoding is streamed straight through the datagram output stream's
     * cipher into the wire, with no intermediate buffer held by Cobalt
     * beyond the cipher's fixed-size chunk buffer. Concurrent callers are
     * serialised by {@link #writeLock} so each datagram is one atomic
     * operation from
     * {@link WhatsAppDatagramOutputStream#beginDatagram(byte[], int)} through
     * the closing {@link OutputStream#flush()}.
     *
     * @param stanza the stanza to send
     * @throws IOException if serialisation or the underlying write
     *         fails
     */
    @WhatsAppWebExport(moduleName = "WAFrameSocket", exports = "FrameSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    public final void sendNode(Stanza stanza) throws IOException {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending stanza {0}", stanza);
        synchronized (writeLock) {
            try {
                out.beginDatagram(null, StanzaSizer.sizeOf(stanza));
                try (var encoder = StanzaWriter.toStream(out)) {
                    encoder.writeStanza(stanza);
                }
            } catch (IOException failure) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to send stanza", failure);
                throw failure;
            }
        }
    }

    /**
     * Runs the full Noise XX handshake against the connected server.
     *
     * <p>This sends the {@code ClientHello}, processes the server's
     * {@code ServerHello}, verifies the certificate chain via
     * {@link #verifyCertificateChain(byte[], byte[])}, sends the
     * {@code ClientFinish} and finally splits the derived 64-byte key
     * material into the read and write AES keys, installing them on the
     * datagram streams so subsequent traffic is enciphered.
     *
     * @throws IOException if the handshake fails or any of its
     *         underlying I/O operations does
     */
    private void performNoiseHandshake() throws IOException {
        var handshakeStart = Instant.now();
        var ephemeralKeyPair = SignalIdentityKeyPair.random();
        var prologue = handshakePrologue();

        try (var handshake = new WhatsAppSocketHandshake(in, out, prologue)) {
            var clientHelloMessage = new HandshakeMessageBuilder()
                    .clientHello(new HandshakeMessageClientHelloBuilder()
                            .ephemeral(ephemeralKeyPair.publicKey().toEncodedPoint())
                            .build())
                    .build();
            handshake.updateHash(ephemeralKeyPair.publicKey().toEncodedPoint());
            handshake.writeClientHandshake(clientHelloMessage);

            var serverHello = handshake.readServerHandshake()
                    .serverHello()
                    .orElseThrow(() -> new IOException("Missing server hello"));

            var ephemeral = serverHello.ephemeral()
                    .orElseThrow(() -> new IOException("Missing server ephemeral key"));
            handshake.updateHash(ephemeral);

            var sharedEphemeral = Curve25519.sharedKey(
                    ephemeralKeyPair.privateKey().toEncodedPoint(), ephemeral
            );
            handshake.mixIntoKey(sharedEphemeral);

            var staticText = serverHello._static()
                    .orElseThrow(() -> new IOException("Missing server static key"));
            var decodedStaticText = handshake.cipher(staticText, false);

            var sharedStatic = Curve25519.sharedKey(
                    ephemeralKeyPair.privateKey().toEncodedPoint(), decodedStaticText
            );
            handshake.mixIntoKey(sharedStatic);

            var payload = serverHello.payload()
                    .orElseThrow(() -> new IOException("Missing server payload"));
            var decryptedCertificate = handshake.cipher(payload, false);

            verifyCertificateChain(decryptedCertificate, decodedStaticText);

            var noiseKeyPair = store.signalStore().noiseKeyPair();
            var encodedKey = handshake.cipher(noiseKeyPair.publicKey().toEncodedPoint(), true);
            var sharedPrivate = Curve25519.sharedKey(
                    noiseKeyPair.privateKey().toEncodedPoint(), ephemeral
            );
            handshake.mixIntoKey(sharedPrivate);

            var encodedPayload = handshake.cipher(handshakePayload(), true);
            var clientFinishMessage = new HandshakeMessageBuilder()
                    .clientFinish(new HandshakeMessageClientFinishBuilder()
                            ._static(encodedKey)
                            .payload(encodedPayload)
                            .build())
                    .build();
            handshake.writeClientHandshake(clientFinishMessage);

            var keys = handshake.finish();
            this.writeKey = new SecretKeySpec(keys, 0, 32, "AES");
            this.readKey = new SecretKeySpec(keys, 32, 32, "AES");
            out.setWriteKey(writeKey);
            in.setReadKey(readKey);

            this.authHandshakeDuration = Duration.between(handshakeStart, Instant.now());
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "noise handshake failed with unexpected exception", e);
            throw new IOException("Noise handshake failure", e);
        }
    }

    /**
     * Validates that a Curve25519 signature is exactly 64 bytes.
     *
     * @param signature the raw signature bytes
     * @return {@code signature}, returned for fluent chaining
     * @throws IOException if the signature is not exactly 64 bytes
     *         long
     */
    private static byte[] ensureSignatureSize(byte[] signature) throws IOException {
        if (signature.length != 64) {
            throw new IOException("Certificate signature has invalid length: " + signature.length + ", expected 64");
        }
        return signature;
    }

    /**
     * Spawns the reader virtual thread that drains datagrams from
     * the datagram input stream, decodes each one into a
     * {@link Stanza} and dispatches it to the application listener.
     */
    private void startReaderThread() {
        this.readerThread = Thread.ofVirtual()
                .name("cobalt-socket-reader")
                .start(this::runReaderLoop);
    }

    /**
     * Continuously decodes nodes from the datagram input stream
     * until the server signals end-of-stream, the transport is closed,
     * or a fatal error is observed.
     *
     * <p>Each iteration decodes exactly one datagram: its plaintext length is taken
     * from {@link WhatsAppDatagramInputStream#readDatagramLength()} and a single
     * {@link Stanza} is streamed off the datagram stream through
     * {@link StanzaReader#fromStream(InputStream, int)}, which bounds its reads to
     * that length through a fixed staging buffer so a multi-megabyte frame is never
     * materialised whole. Binding the decode to the datagram frame is what keeps the
     * reader in sync: the frame boundary is authoritative, so a stanza that decodes
     * to fewer bytes than the frame carries cannot bleed into the next stanza, and a
     * malformed frame cannot shift the cursor for the frames that follow. After the
     * decode, {@link StanzaReader#drain()} discards any bytes the decoder
     * left unread so the datagram cursor lands exactly on the next frame's length
     * prefix (and the transport finishes authenticating the frame it streamed).
     *
     * <p>Because decoding is frame-bounded, a malformed inbound stanza cannot corrupt
     * the frames that follow, so a {@link StanzaReader#decode()} failure is isolated
     * per frame rather than dropping the connection: the frame is drained and a
     * non-fatal {@link WhatsAppStreamException.MalformedNode} is surfaced through
     * {@link WhatsAppSocketListener#onError(WhatsAppException)} while the loop keeps
     * reading, mirroring WA Web's {@code WAComms.parseAndHandleStanza}, which logs a
     * stanza-parse failure and continues. A {@link WhatsAppSessionException.BadMac}
     * from the datagram layer is not a decode failure: it means the frame did not
     * authenticate, so it is re-raised as a fatal fault rather than isolated.
     *
     * <p>A {@code <xmlstreamend>} stanza is dispatched like any other stanza and
     * the loop keeps reading: WhatsApp ends a logical XML stream with
     * {@code <xmlstreamend>} without necessarily closing the transport. During a
     * clean server-initiated teardown (after a {@code <stream:error>}, a logout,
     * or a conflict) the server closes the socket right after, so the next
     * {@link WhatsAppDatagramInputStream#readDatagramLength()} observes a
     * frame-boundary end-of-stream and returns {@code -1}, which this loop treats
     * as an orderly close (it stops without surfacing an error). The server may
     * trail a few bytes that never form a whole datagram before closing; the
     * datagram layer drops such a partial tail and reports the same clean
     * {@code -1} length, so a server-initiated close never surfaces as a framing
     * error. During the
     * unregistered pairing phase the server instead ends one stream segment and
     * immediately pushes a fresh {@code <pair-device>} on the same socket to
     * rotate the QR refs or refresh the pairing code; treating
     * {@code <xmlstreamend>} as terminal here would tear down the socket, force a
     * full reconnect that regenerates the code, and trip the pairing
     * rate-limiter. Keeping the read loop alive mirrors WA Web's
     * {@code WAWebCommsHandleLoggedInStanza}, which logs {@code <xmlstreamend>}
     * and lets the underlying frame socket close drive the lifecycle.
     *
     * <p>A bad MAC tears down the connection through
     * {@link WhatsAppSessionException.BadMac}; every other unsolicited drop (an abrupt transport
     * reset or abort, the host network going down, or a corrupted frame) surfaces as
     * {@link WhatsAppSessionException.Closed}, which the client treats as a reconnect. Errors
     * observed after {@link #disconnect()} are suppressed so orderly shutdown does not spam the
     * listener with spurious failures.
     *
     * @implNote
     * This implementation runs on a dedicated virtual thread so the
     * carrier thread is never blocked on socket I/O; one reader is
     * spawned per {@link #connect(WhatsAppSocketListener)} call.
     */
    @WhatsAppWebExport(moduleName = "WAFrameSocket", exports = "FrameSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WANoiseSocket", exports = "NoiseSocket", adaptation = WhatsAppAdaptation.ADAPTED)
    private void runReaderLoop() {
        try {
            while (!closed && isTransportOpen()) {
                var length = in.readDatagramLength();
                if (length < 0) {
                    break;
                }
                Stanza stanza;
                try (var decoder = StanzaReader.fromStream(in, length)) {
                    try {
                        stanza = decoder.decode();
                    } catch (WhatsAppSessionException.BadMac fatal) {
                        if (Log.ERROR) LOGGER.log(Level.ERROR, "datagram authentication failed, tearing down session", fatal);
                        throw fatal;
                    } catch (IOException | RuntimeException malformed) {
                        decoder.drain();
                        if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to decode inbound stanza", malformed);
                        listener.onError(new WhatsAppStreamException.MalformedNode("Failed to decode inbound stanza", malformed));
                        continue;
                    }
                    decoder.drain();
                }
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received stanza {0}", stanza);
                listener.onNode(stanza);
            }
        } catch (Exception failure) {
            if (!closed) {
                var surfaced = classifyReaderFailure(failure);
                if (surfaced != null) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "socket reader loop terminated with error", failure);
                    listener.onError(surfaced);
                } else if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "socket reader loop reached orderly end of stream");
                }
                // A bad MAC leaves the Noise cipher unusable, so the socket is torn down here; every
                // other surfaced fault is reconnect-worthy and the client's failure handler drives
                // the teardown off the verdict.
                if (failure instanceof WhatsAppSessionException.BadMac) {
                    disconnect();
                }
            }
        } finally {
            if (!closed) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "socket reader loop closed, notifying listener");
                listener.onClose();
            }
        }
    }

    /**
     * Maps a fault caught by the reader loop to the {@link WhatsAppException} that should be
     * surfaced through {@link WhatsAppSocketListener#onError(WhatsAppException)}, or {@code null}
     * when the fault is an orderly end-of-stream that should fall through to
     * {@link WhatsAppSocketListener#onClose()} without an error.
     *
     * <p>The classification is verdict-driven: because the reader thread is started only after the
     * Noise handshake has established a session, every fault here is a mid-session event. An
     * {@link EOFException} maps to {@code null} (orderly close); a {@link WhatsAppSessionException.BadMac}
     * is passed through unchanged; every other fault (a transport drop such as a connection reset,
     * abort, or the host network going down, as well as a corrupted frame) is reconnect-worthy and
     * maps to {@link WhatsAppSessionException.Closed} rather than the initial-connection
     * {@code WhatsAppConnectionException} (which classifies as a terminal disconnect and would
     * strand the session).
     *
     * @param failure the exception caught by the reader loop
     * @return the exception to surface via {@code onError}, or {@code null} for an orderly close
     */
    static WhatsAppException classifyReaderFailure(Exception failure) {
        return switch (failure) {
            case EOFException _ -> null;
            case WhatsAppSessionException.BadMac badMac -> badMac;
            default -> new WhatsAppSessionException.Closed("Connection dropped", failure);
        };
    }

    /**
     * Returns the configured proxy for this client, if any.
     *
     * @return the proxy or {@code null} if none is configured
     */
    final WhatsAppClientProxy proxy() {
        return store.connectionStore().proxy().orElse(null);
    }

    /**
     * Opens a blocking {@link Socket} to the supplied endpoint,
     * routing via the configured proxy when present and dispatching
     * to the appropriate tunnel implementation in
     * {@code socket.tunnel}.
     *
     * <p>Three proxy shapes are supported:
     * <ul>
     *   <li>{@link WhatsAppClientProxy.Http.Plain} or
     *       {@link WhatsAppClientProxy.Http.Secure}, handled by
     *       {@link HttpTunnel}, which internally TLS-wraps the proxy
     *       hop for the secure variant before sending
     *       {@code CONNECT}.</li>
     *   <li>{@link WhatsAppClientProxy.Socks} (V4, V4a, V5 or V5h),
     *       handled by {@link SocksTunnel} which runs the
     *       protocol-specific handshake on the raw socket.</li>
     * </ul>
     * For a {@link WhatsAppClientProxy.Http.Secure} proxy the returned socket is
     * the TLS-wrapped {@link SSLSocket}; in all other cases the raw socket is
     * returned.
     *
     * @param target the final destination the tunnel should reach
     * @return the connected socket, already past the proxy handshake
     * @throws IOException if any step of the connect or tunnel fails
     */
    final Socket openTunneledSocket(InetSocketAddress target) throws IOException {
        var proxy = proxy();
        var firstHop = proxy == null
                ? target
                : new InetSocketAddress(proxy.host(), proxy.port());

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening tcp socket to {0}:{1}", firstHop.getHostString(), firstHop.getPort());
        var raw = new Socket();
        raw.setTcpNoDelay(true);
        raw.setKeepAlive(true);
        raw.connect(firstHop, CONNECT_TIMEOUT_MILLIS);

        if (proxy == null) {
            return raw;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tunneling through proxy to {0}:{1}", target.getHostString(), target.getPort());
        try {
            var tunneled = switch (proxy) {
                case WhatsAppClientProxy.Http http -> HttpTunnel.tunnel(raw,
                        target.getHostString(), target.getPort(), http, sslContextFactory);
                case WhatsAppClientProxy.Socks socks -> SocksTunnel.tunnel(raw,
                        target.getHostString(), target.getPort(), socks);
            };
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "proxy tunnel established to {0}:{1}", target.getHostString(), target.getPort());
            return tunneled;
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "proxy tunnel failed", e);
            try {
                raw.close();
            } catch (IOException _) {
            }
            throw e;
        }
    }

    /**
     * Raw-TCP socket client for {@code g.whatsapp.net:443}.
     *
     * <p>This connects via a blocking {@link Socket} (optionally tunneled
     * through an HTTP or SOCKS proxy via
     * {@link WhatsAppSocketClient#openTunneledSocket(InetSocketAddress)}) and
     * runs the Noise XX handshake directly over the socket. The end-to-end
     * hop carries no TLS because the Noise handshake is itself an
     * authenticated key exchange. It is used by every client whose device
     * platform routes to plain TCP: {@code IOS}, {@code IOS_BUSINESS},
     * {@code ANDROID}, {@code ANDROID_BUSINESS} and {@code MACOS} (the native
     * macOS desktop app is a Mac Catalyst port of the iOS binary and shares
     * the transport, although it diverges on the handshake shape).
     */
    static final class Tcp extends WhatsAppSocketClient {

        /**
         * The TCP endpoint for plain-TCP clients.
         */
        private static final InetSocketAddress TCP_ENDPOINT = new InetSocketAddress("g.whatsapp.net", 443);

        /**
         * The raw TCP socket, set by {@link #openTransport()}.
         */
        private Socket socket;

        /**
         * Constructs a plain-TCP socket client.
         *
         * <p>The {@code sslContextFactory} is only consulted when a
         * {@link WhatsAppClientProxy.Http.Secure} proxy is configured; the
         * end-to-end TCP hop does not use TLS so the factory is otherwise
         * unused.
         *
         * @param store             the WhatsApp store
         * @param sslContextFactory the SSL configuration applied
         *                          when tunnelling through a TLS
         *                          proxy
         */
        Tcp(LinkedWhatsAppStore store, WhatsAppSslContextFactory sslContextFactory) {
            super(store, sslContextFactory);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation opens the (possibly proxy-tunneled)
         * raw socket and wraps its streams in the datagram pair; no
         * TLS is involved on the end-to-end hop.
         */
        @Override
        void openTransport() throws IOException {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening tcp transport");
            this.socket = openTunneledSocket(TCP_ENDPOINT);
            this.in = new WhatsAppDatagramInputStream(socket.getInputStream());
            this.out = new WhatsAppDatagramOutputStream(socket.getOutputStream());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void closeTransport() throws IOException {
            if (socket != null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing tcp transport");
                socket.close();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isTransportOpen() {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }
    }

    /**
     * WebSocket-over-TLS socket client for
     * {@code web.whatsapp.com:443}.
     *
     * <p>This opens a raw {@link Socket}, optionally tunnels it through an
     * HTTP or SOCKS proxy via
     * {@link WhatsAppSocketClient#openTunneledSocket(InetSocketAddress)},
     * TLS-wraps it with the supplied {@link WhatsAppSslContextFactory},
     * drives the RFC 6455 upgrade handshake via {@link WebSocketUpgrade}, and
     * runs Noise XX over the resulting frame stream. WhatsApp datagrams ride
     * on top of the WebSocket frames as a continuous byte stream. It is used
     * by browser companions and by the Windows Electron desktop app, which is
     * a hybrid web/native shell that ships the same JS bundle as the browser
     * and reuses this transport.
     */
    static final class WebSocket extends WhatsAppSocketClient {

        /**
         * The TCP endpoint for WebSocket companions; the WebSocket
         * upgrade negotiates the {@code /ws/chat} path on top of
         * it.
         */
        private static final InetSocketAddress WEB_ENDPOINT = new InetSocketAddress("web.whatsapp.com", 443);

        /**
         * The Host header sent on the WebSocket upgrade request.
         */
        private static final String WEB_HOST = "web.whatsapp.com";

        /**
         * The endpoint path sent on the WebSocket upgrade request.
         */
        private static final String WEB_PATH = "/ws/chat";

        /**
         * The desktop Chrome User-Agent advertised on the upgrade
         * request so the server does not redirect the client to the
         * mobile landing page.
         */
        private static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

        /**
         * The TLS-wrapped socket, set by {@link #openTransport()}.
         */
        private SSLSocket socket;

        /**
         * Constructs a WebSocket-over-TLS socket client.
         *
         * @param store             the WhatsApp store
         * @param sslContextFactory the SSL configuration for the
         *                          WebSocket TLS hop
         */
        WebSocket(LinkedWhatsAppStore store, WhatsAppSslContextFactory sslContextFactory) {
            super(store, sslContextFactory);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation opens the (possibly proxy-tunneled)
         * raw socket, TLS-wraps it, performs the WebSocket upgrade
         * and stacks the WebSocket frame streams under the datagram
         * streams. Any leftover bytes the server piggybacked on the
         * upgrade response are passed to the input stream so the
         * first frame is not lost.
         */
        @Override
        void openTransport() throws IOException {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "opening websocket transport to {0}:{1}", WEB_HOST, WEB_ENDPOINT.getPort());
            var raw = openTunneledSocket(WEB_ENDPOINT);
            SSLSocket ssl = null;
            try {
                ssl = (SSLSocket) sslContextFactory.sslContext()
                        .getSocketFactory()
                        .createSocket(raw, WEB_HOST, WEB_ENDPOINT.getPort(), true);
                ssl.setSSLParameters(sslContextFactory.sslParameters());
                ssl.startHandshake();
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tls handshake complete, protocol={0}", ssl.getSession().getProtocol());

                var leftover = WebSocketUpgrade.upgrade(ssl, WEB_PATH, WEB_HOST,
                        WEB_ENDPOINT.getPort(), WEB_USER_AGENT);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "websocket upgrade complete");

                this.socket = ssl;
                var wsOut = new WebSocketFrameOutputStream(ssl.getOutputStream());
                var wsIn = new WebSocketFrameInputStream(ssl.getInputStream(), leftover, wsOut);
                this.in = new WhatsAppDatagramInputStream(wsIn);
                this.out = new WhatsAppDatagramOutputStream(wsOut);
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "websocket transport open failed", e);
                if (ssl != null) {
                    try {
                        ssl.close();
                    } catch (IOException _) {
                    }
                } else {
                    try {
                        raw.close();
                    } catch (IOException _) {
                    }
                }
                throw e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void closeTransport() throws IOException {
            if (socket != null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing websocket transport");
                socket.close();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isTransportOpen() {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }
    }
}
