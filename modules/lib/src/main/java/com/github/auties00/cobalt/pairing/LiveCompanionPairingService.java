package com.github.auties00.cobalt.pairing;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.*;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Live implementation of {@link CompanionPairingService} that drives the companion side of
 * WhatsApp's alt-device-linking (eight-character pairing-code) flow.
 *
 * <p>One instance is created per {@link LinkedWhatsAppClient} when the client
 * is configured with a
 * {@link LinkedWhatsAppClientVerificationHandler.Web.PairingCode} handler and
 * a phone number is set on the store. The service is shared between the
 * IQ stream handler that triggers {@code companion_hello} and the
 * notification stream handler that consumes the resulting
 * {@code primary_hello} and {@code refresh_code} notifications, and it
 * caches the generated pairing code, the companion ephemeral keypair,
 * the server-issued {@code link_code_pairing_ref}, and a generation
 * timestamp so the companion can validate the primary's response and
 * complete the handshake.
 *
 * <p>The flow lets the user attach a new device by typing an
 * eight-character code into their primary phone instead of scanning a
 * QR code. The lifecycle is:
 * <ol>
 *   <li>{@link #start} generates the code, ships
 *       {@code companion_hello}, and delivers the code to
 *       {@link LinkedWhatsAppClientVerificationHandler.Web#handle(String)}.</li>
 *   <li>The user types the code on their primary device.</li>
 *   <li>{@link #handlePrimaryHello} runs the {@code companion_finish}
 *       algorithm, ships the IQ, and persists the derived ADV master
 *       secret.</li>
 * </ol>
 *
 * @implNote
 * This implementation adapts WA Web's {@code WAWebAltDeviceLinkingApi}
 * module-level state into an instance-scoped service so multiple Cobalt
 * clients can run independent pairing flows. WA Web keeps the pairing
 * state in a singleton {@code PairingState} guarded by the browser's
 * single-threaded event loop; Cobalt runs stream handlers across
 * virtual threads, so this class serialises every state transition
 * under {@link #lock}. The QPL telemetry markers and the
 * {@code WAWebBackendApi.frontendFireAndForget} hooks emitted around
 * each phase by WA Web are dropped because Cobalt has no telemetry
 * pipeline and no front-end bus.
 */
@WhatsAppWebModule(moduleName = "WAWebAltDeviceLinkingApi")
@WhatsAppWebModule(moduleName = "WAWebAltDeviceLinkingIq")
@WhatsAppWebModule(moduleName = "WAWebAltDeviceLinkingAlgorithm")
@WhatsAppWebModule(moduleName = "WAWebAltDeviceLinkingBase32Encode")
@WhatsAppWebModule(moduleName = "WAWebCompanionRegClientUtils")
@WhatsAppWebModule(moduleName = "WAWebUserPrefsMultiDevice")
@WhatsAppWebModule(moduleName = "WACryptoHkdf")
public final class LiveCompanionPairingService implements CompanionPairingService {
    /**
     * The logger for {@link LiveCompanionPairingService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCompanionPairingService.class);

    /**
     * Holds the maximum age of a pairing code before
     * {@code primary_hello} is rejected.
     *
     * <p>This caps how long the user has to type the code on the
     * primary device.
     *
     * @implNote
     * This implementation uses 180 seconds to match the {@code I=180}
     * literal in {@code WAWebAltDeviceLinkingApi}.
     */
    private static final Duration CODE_MAX_AGE = Duration.ofSeconds(180);

    /**
     * Holds the maximum number of {@code primary_hello} notifications
     * tolerated per generated pairing code.
     *
     * <p>This bounds how many times the user can mistype the code on
     * the primary before the handshake aborts.
     *
     * @implNote
     * This implementation uses 3 to match the {@code T=3} literal in
     * {@code WAWebAltDeviceLinkingApi}.
     */
    private static final int MAX_PRIMARY_HELLO_ATTEMPTS = 3;

    /**
     * Holds the length in bytes of the PBKDF2 salt prefixing the
     * wrapped ephemeral public key.
     *
     * <p>This is used both when generating
     * {@code <link_code_pairing_wrapped_companion_ephemeral_pub>} and
     * when parsing the symmetric
     * {@code link_code_pairing_wrapped_primary_ephemeral_pub} delivered
     * by {@code primary_hello}.
     */
    private static final int PBKDF2_SALT_LENGTH = 32;

    /**
     * Holds the length in bytes of the AES-CTR initial counter
     * prefixing the wrapped ephemeral public key.
     *
     * @implNote
     * This implementation uses 16 to match the
     * {@code new Uint8Array(16)} in WA Web's
     * {@code WAWebAltDeviceLinkingAlgorithm.companionHelloInternal} and
     * the symmetric parser in {@code companionFinishInternal}.
     */
    private static final int AES_CTR_IV_LENGTH = 16;

    /**
     * Holds the length in bytes of the AES-GCM nonce used to encrypt
     * the final identity bundle.
     *
     * @implNote
     * This implementation uses 12 to match the WebCrypto default nonce
     * length and the {@code new Uint8Array(12)} in WA Web's
     * {@code companionFinishInternal}.
     */
    private static final int AES_GCM_IV_LENGTH = 12;

    /**
     * Holds the length in bytes of the Curve25519 public key component
     * of the ephemeral keypair the companion ships to the primary.
     *
     * <p>This is consulted only to validate the minimum size of
     * {@code link_code_pairing_wrapped_primary_ephemeral_pub}; the
     * actual ciphertext can be longer because AES-CTR preserves the
     * input length.
     */
    private static final int CURVE25519_PUBLIC_KEY_LENGTH = 32;

    /**
     * Holds the length in bytes of the HKDF output used both as the
     * AES-GCM bundle encryption key and as the derived ADV master
     * secret.
     *
     * @implNote
     * This implementation uses 32 to match the length argument WA Web
     * passes to {@code WACryptoHkdf.extractWithSaltAndExpand} for both
     * the {@code link_code_pairing_key_bundle_encryption_key} and the
     * {@code adv_secret}.
     */
    private static final int HKDF_OUTPUT_LENGTH = 32;

    /**
     * Holds the ASCII HKDF {@code info} label used when deriving the
     * AES-GCM bundle encryption key from the X25519 ephemeral shared
     * secret.
     */
    private static final String BUNDLE_ENCRYPTION_INFO = "link_code_pairing_key_bundle_encryption_key";

    /**
     * Holds the ASCII HKDF {@code info} label used when deriving the
     * ADV master secret from the concatenated X25519 shared secrets.
     */
    private static final String ADV_SECRET_INFO = "adv_secret";

    /**
     * Holds the pairing-code Crockford-style base32 alphabet.
     *
     * <p>The alphabet skips {@code 0}, {@code I}, {@code O}, and
     * {@code U} so the eight-character code is unambiguous when typed by
     * a human reading it off the companion display.
     *
     * @implNote
     * This implementation copies the literal
     * {@code "123456789ABCDEFGHJKLMNPQRSTVWXYZ"} from WA Web's
     * {@code WAWebAltDeviceLinkingBase32Encode} module verbatim into a
     * {@code char[]} for direct indexing.
     */
    private static final char[] CROCKFORD_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTVWXYZ".toCharArray();

    /**
     * Holds the number of PBKDF2-HMAC-SHA256 iterations applied to the
     * pairing code when deriving the AES-CTR key.
     *
     * @implNote
     * This implementation uses {@code 2 << 16} (131072) to reproduce WA
     * Web's {@code iterations: 2<<16} literal in
     * {@code WAWebAltDeviceLinkingAlgorithm.deriveKey}.
     */
    private static final int PBKDF2_ITERATIONS = 2 << 16;

    /**
     * Holds the length in bits of the AES-CTR key derived by PBKDF2
     * from the pairing code.
     *
     * @implNote
     * This implementation uses 256 to match WA Web's
     * {@code length: 256} parameter to
     * {@code crypto.subtle.deriveKey} for the {@code name: "AES-CTR"}
     * key.
     */
    private static final int PBKDF2_KEY_BITS = 256;

    /**
     * Holds the length in bits of the AES-GCM authentication tag
     * appended to the encrypted identity bundle.
     *
     * @implNote
     * This implementation uses 128 to match the WebCrypto default; WA
     * Web does not specify {@code tagLength} explicitly so the bundle
     * inherits the 128-bit tag.
     */
    private static final int GCM_TAG_BITS = 128;

    /**
     * Holds the {@link LinkedWhatsAppClient} used to reach the store and
     * dispatch the pairing IQs.
     *
     * <p>This is injected via the constructor and is never {@code null}
     * once construction has completed.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the web verification handler that receives the pairing code
     * after {@code companion_hello} returns.
     *
     * <p>Only the
     * {@link LinkedWhatsAppClientVerificationHandler.Web.PairingCode}
     * sub-type triggers this service; QR handlers leave the field
     * present but {@link #isEnabled} returns {@code false}.
     */
    private final LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler;

    /**
     * Holds the mutex serialising every transition of {@link #stage}
     * and every read or write of the cached handshake fields.
     *
     * @implNote
     * This implementation introduces an explicit lock because Cobalt
     * runs stream handlers across virtual threads, whereas WA Web
     * relies on the browser's single-threaded event loop and therefore
     * needs no synchronisation around its {@code PairingState}
     * singleton.
     */
    private final Object lock;

    /**
     * Holds the current handshake stage.
     *
     * <p>This is inspected by {@link #handlePrimaryHello} to decide
     * whether to regenerate the ADV secret and rerun the algorithm or
     * to reject the notification as out-of-order.
     */
    private CompanionPairingStage stage;

    /**
     * Holds the pairing code produced by the most recent
     * {@code companion_hello}.
     *
     * <p>This is cached because {@link #handlePrimaryHello} needs the
     * original code to re-derive the PBKDF2 AES-CTR key that unwraps the
     * primary's ephemeral public key.
     */
    private String pairingCode;

    /**
     * Holds the companion's ephemeral Curve25519 keypair for the
     * current pairing attempt.
     *
     * <p>The private half is needed to run X25519 against the primary's
     * decrypted ephemeral public key during {@code companion_finish}.
     */
    private SignalIdentityKeyPair companionEphemeralKeyPair;

    /**
     * Holds the server-issued {@code link_code_pairing_ref} returned by
     * the {@code companion_hello} IQ.
     *
     * <p>This is compared byte-for-byte against the ref carried by the
     * primary's notification to detect replays or concurrent pairing
     * attempts.
     */
    private byte[] cachedRef;

    /**
     * Holds the phone-number JID identifying the primary device the
     * user is linking against.
     *
     * <p>This is captured from
     * {@link LinkedWhatsAppAccountStore#phoneNumber}
     * at {@link #start} time and echoed into the {@code companion_hello}
     * and {@code companion_finish} IQs.
     */
    private Jid phoneJid;

    /**
     * Holds the wall-clock instant at which the pairing code was
     * generated.
     *
     * <p>This is compared against {@link Instant#now()} in
     * {@link #handlePrimaryHello} to reject codes older than
     * {@link #CODE_MAX_AGE}.
     */
    private Instant codeGenerationTs;

    /**
     * Holds the count of {@code primary_hello} notifications observed
     * against the current pairing code.
     *
     * <p>This is incremented on every call to
     * {@link #handlePrimaryHello(byte[], byte[], byte[])} and checked
     * against {@link #MAX_PRIMARY_HELLO_ATTEMPTS} before the algorithm
     * is allowed to run.
     */
    private int primaryHelloAttemptCount;

    /**
     * Constructs a service tied to the given client and verification
     * handler.
     *
     * <p>The same instance is shared between every handler that
     * participates in the alt-device-linking flow.
     *
     * @param whatsapp               the WhatsApp client; never
     *                               {@code null}
     * @param webVerificationHandler the verification handler, which may
     *                               be a QR or a pairing-code variant;
     *                               {@code null} disables external code
     *                               delivery
     * @throws NullPointerException if {@code whatsapp} is {@code null}
     */
    public LiveCompanionPairingService(LinkedWhatsAppClient whatsapp, LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp must not be null");
        this.webVerificationHandler = webVerificationHandler;
        this.lock = new Object();
        this.stage = CompanionPairingStage.NOT_STARTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String pairingCode() {
        synchronized (lock) {
            return pairingCode;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return webVerificationHandler instanceof LinkedWhatsAppClientVerificationHandler.Web.PairingCode
                && whatsapp.store().accountStore().phoneNumber().isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation differs from WA Web in two places. WA Web
     * generates the ADV secret eagerly inside
     * {@code initializeAltDeviceLinking} (via {@code setADVSecretKey()}
     * with no argument, which mints a fresh one); Cobalt defers
     * ADV-secret material generation until inside
     * {@code companionFinish}. WA Web logs QPL points
     * ({@code generate_code_start}, {@code send_companion_hello_end},
     * and others) and emits
     * {@code WAWebBackendApi.frontendFireAndForget} events; Cobalt skips
     * both because it has no QPL pipeline.
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingApi", exports = {"initializeAltDeviceLinking", "startAltLinkingFlow"}, adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void start() throws GeneralSecurityException {
        synchronized (lock) {
            if (!isEnabled()) {
                throw new IllegalStateException("Alt-device-linking is not configured");
            }

            clearLocked();
            stage = CompanionPairingStage.INITIALIZED;
            var phoneNumber = whatsapp.store().accountStore().phoneNumber().orElseThrow();
            phoneJid = Jid.of(phoneNumber);
            codeGenerationTs = Instant.now();

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting companion pairing for {0}", phoneJid);

            var hello = companionHello();
            pairingCode = hello.linkCodePairingSecret();
            companionEphemeralKeyPair = hello.companionEphemeralKeyPair();

            var ref = sendCompanionHello(phoneJid, hello.linkCodePairingWrappedCompanionEphemeralPub());
            if (ref == null || ref.length == 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "companion_hello returned no ref for {0}", phoneJid);
                clearLocked();
                throw new GeneralSecurityException("Server did not return a ref for companion_hello");
            }
            cachedRef = ref;
            stage = CompanionPairingStage.AFTER_SEND_COMPANION_HELLO;

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "companion pairing code generated: {0}", new LogRedactable.Code(pairingCode));

            if (webVerificationHandler != null) {
                webVerificationHandler.handle(pairingCode);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * The branch that re-writes {@link #cachedRef} only re-binds the
     * existing value to itself. WA Web's {@code refreshAltLinkingCode}
     * triggers a {@code link_device_events:refresh_alt_linking_code} UI
     * event; Cobalt has no UI bus and therefore intentionally drops the
     * event.
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingApi", exports = "refreshAltLinkingCode", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void handleRefreshCode(byte[] notificationRef) {
        synchronized (lock) {
            if (cachedRef == null || notificationRef == null) {
                return;
            }
            if (!Arrays.equals(cachedRef, notificationRef)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "refresh_code ref mismatch, ignoring");
                return;
            }
            cachedRef = notificationRef;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "refresh_code applied for {0}", phoneJid);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code handlePrimaryHelloInternal} except that Cobalt does not
     * catch and re-emit the {@code errorAltLinking} event WA Web fires
     * on failure; the exception is allowed to propagate to the stream
     * handler. Pairing-state mutations happen under {@link #lock} so a
     * second notification cannot observe a half-applied state, and a
     * repeat after completion regenerates the ADV secret up to
     * {@link #MAX_PRIMARY_HELLO_ATTEMPTS} times per code.
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingApi", exports = {"handlePrimaryHello", "handlePrimaryHelloInternal"}, adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public void handlePrimaryHello(byte[] wrappedPrimaryEphemeralPub, byte[] primaryIdentityPublic, byte[] notificationRef) throws GeneralSecurityException {
        synchronized (lock) {
            primaryHelloAttemptCount++;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "primary_hello received, attempt {0}, stage {1}", primaryHelloAttemptCount, stage);
            if (stage == CompanionPairingStage.AFTER_SEND_COMPANION_FINISH) {
                if (primaryHelloAttemptCount > MAX_PRIMARY_HELLO_ATTEMPTS) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "primary_hello attempts exceeded for {0}", phoneJid);
                    throw new GeneralSecurityException("Exceeded primary_hello attempts for the current pairing code");
                }
                regenerateAdvSecret();
                stage = CompanionPairingStage.AFTER_SEND_COMPANION_HELLO;
            }

            if (stage != CompanionPairingStage.AFTER_SEND_COMPANION_HELLO) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "primary_hello received out of order, stage {0}", stage);
                throw new IllegalStateException("primary_hello received while in stage " + stage);
            }

            if (cachedRef == null) {
                throw new GeneralSecurityException("No cached ref from companion_hello");
            }
            if (notificationRef == null || !Arrays.equals(cachedRef, notificationRef)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "primary_hello ref does not match cached ref for {0}", phoneJid);
                throw new GeneralSecurityException("primary_hello ref does not match cached ref");
            }
            if (companionEphemeralKeyPair == null || pairingCode == null) {
                throw new GeneralSecurityException("Cached companion hello is missing");
            }
            if (codeGenerationTs == null
                    || Duration.between(codeGenerationTs, Instant.now()).compareTo(CODE_MAX_AGE) > 0) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "pairing code expired for {0}", phoneJid);
                throw new GeneralSecurityException("Pairing code has expired");
            }

            var identityKeyPair = whatsapp.store().signalStore().identityKeyPair();
            var finish = companionFinish(
                    pairingCode,
                    wrappedPrimaryEphemeralPub,
                    primaryIdentityPublic,
                    companionEphemeralKeyPair,
                    identityKeyPair);

            whatsapp.store().signalStore().setAdvSecretKey(finish.advSecret());
            sendCompanionFinish(phoneJid, finish.linkCodePairingWrappedKeyBundle(), finish.companionIdentityPublic(), cachedRef);
            stage = CompanionPairingStage.AFTER_SEND_COMPANION_FINISH;
            if (Log.INFO) LOGGER.log(Level.INFO, "companion pairing completed for {0}", phoneJid);
        }
    }

    /**
     * Resets every field in the cached pairing state.
     *
     * <p>This is invoked at the top of {@link #start} so an aborted
     * previous attempt cannot bleed into a fresh one. The caller must
     * already hold {@link #lock}.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code PairingState.clear()}
     * but does not call
     * {@code WAWebAltDeviceLinkingQpl.clearCurrentMarker()} because
     * Cobalt has no QPL pipeline.
     */
    private void clearLocked() {
        stage = CompanionPairingStage.NOT_STARTED;
        pairingCode = null;
        companionEphemeralKeyPair = null;
        cachedRef = null;
        phoneJid = null;
        codeGenerationTs = null;
        primaryHelloAttemptCount = 0;
    }

    /**
     * Regenerates the store's ADV secret to recover from a repeat
     * {@code primary_hello} in the same code window.
     *
     * <p>This is invoked only from {@link #handlePrimaryHello} when a
     * second notification arrives while {@link #stage} is already
     * {@link CompanionPairingStage#AFTER_SEND_COMPANION_FINISH}.
     *
     * @implNote
     * This implementation mints 32 random bytes locally rather than
     * delegating to a key-derivation helper. WA Web routes the same
     * regeneration through
     * {@code WAWebAdvSignatureApi.generateADVSecretKey}, which
     * internally calls
     * {@code WAWebUserPrefsMultiDevice.setADVSecretKey} after building a
     * random buffer of identical size, so the wire-visible behaviour
     * matches.
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMultiDevice", exports = "setADVSecretKey", adaptation = WhatsAppAdaptation.ADAPTED)
    private void regenerateAdvSecret() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "regenerating adv secret after repeat primary_hello");
        var key = DataUtils.randomByteArray(32);
        whatsapp.store().signalStore().setAdvSecretKey(key);
    }

    /**
     * Distills an {@code <iq type="error">} response into a
     * {@link WhatsAppRegistrationException}.
     *
     * <p>This is a helper for {@link #sendCompanionHello} and
     * {@link #sendCompanionFinish}; it returns {@code null} when the
     * response is an ordinary {@code type="result"} stanza so the caller
     * can proceed.
     *
     * @implNote
     * This implementation reads the {@code <error>} child's
     * {@code code} (integer) and {@code text} (string) attributes and
     * folds them into a single human-readable message. It collapses WA
     * Web's {@code CompanionHelloError}/{@code CompanionFinishError}
     * pair onto Cobalt's single
     * {@link WhatsAppRegistrationException}.
     *
     * @param response the raw IQ response from the server
     * @param flow     a short label identifying the sub-flow, for
     *                 example {@code "companion_hello"} or
     *                 {@code "companion_finish"}
     * @return the exception to throw, or {@code null} when the response
     *         is not an error
     */
    private WhatsAppRegistrationException extractIqError(Stanza response, String flow) {
        if (response == null || !response.hasAttribute("type", "error")) {
            return null;
        }
        var error = response.getChild("error").orElse(null);
        var code = error == null ? -1 : error.getAttributeAsInt("code", -1);
        var text = error == null ? null : error.getAttributeAsString("text", null);
        if (Log.WARNING) LOGGER.log(Level.WARNING, "{0} rejected by server, code={1}", flow, code);
        return new WhatsAppRegistrationException(
                "alt pairing: " + flow + " rejected by server (code=" + code + ", text=" + text + ")");
    }

    /**
     * Builds and sends the {@code companion_hello} IQ.
     *
     * <p>This is a helper for {@link #start}. It carries the wrapped
     * companion ephemeral public key, the companion noise server-auth
     * public key, the platform id and display label, and the
     * always-zero link-code pairing nonce up to the WhatsApp relay.
     *
     * @implNote
     * This implementation issues the IQ at the {@link StanzaBuilder} level
     * rather than going through WA Web's
     * {@code WASmaxMdCompanionHelloRPC}-generated RPC. The
     * {@code link_code_pairing_nonce} element is always emitted with a
     * single zero byte even though it is declared {@code OPTIONAL_CHILD}
     * in the outbound mixin, matching WA Web's unconditional
     * {@code new Uint8Array(1)}. The {@code companion_platform_id} and
     * {@code companion_platform_display} children are written as raw
     * UTF-8 bytes rather than tokenised WAP labels so the relay accepts
     * arbitrary platform names. The
     * {@code should_show_push_notification} attribute is always
     * {@code "true"} on Cobalt, matching the {@code i:true} branch in WA
     * Web's {@code sendCompanionHello}.
     *
     * @param phoneJid                                    the primary
     *                                                    phone-number
     *                                                    JID being
     *                                                    linked against
     * @param linkCodePairingWrappedCompanionEphemeralPub the wrapped
     *                                                    companion
     *                                                    ephemeral
     *                                                    public key
     * @return the raw {@code link_code_pairing_ref} bytes, or
     *         {@code null} if the response omits the ref
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingIq", exports = "sendCompanionHello", adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] sendCompanionHello(Jid phoneJid, byte[] linkCodePairingWrappedCompanionEphemeralPub) {
        var wrappedChild = new StanzaBuilder()
                .description("link_code_pairing_wrapped_companion_ephemeral_pub")
                .content(linkCodePairingWrappedCompanionEphemeralPub)
                .build();

        var companionServerAuthKeyPub = new StanzaBuilder()
                .description("companion_server_auth_key_pub")
                .content(whatsapp.store().signalStore().noiseKeyPair().publicKey().toEncodedPoint())
                .build();

        var companionPlatformId = new StanzaBuilder()
                .description("companion_platform_id")
                .content(companionPlatformId().getBytes(StandardCharsets.UTF_8))
                .build();

        var companionPlatformDisplay = new StanzaBuilder()
                .description("companion_platform_display")
                .content(companionPlatformDisplay().getBytes(StandardCharsets.UTF_8))
                .build();

        var linkCodePairingNonce = new StanzaBuilder()
                .description("link_code_pairing_nonce")
                .content(new byte[]{0})
                .build();

        var linkCodeCompanionReg = new StanzaBuilder()
                .description("link_code_companion_reg")
                .attribute("jid", phoneJid)
                .attribute("stage", "companion_hello")
                .attribute("should_show_push_notification", "true")
                .content(wrappedChild, companionServerAuthKeyPub, companionPlatformId, companionPlatformDisplay, linkCodePairingNonce)
                .build();

        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "md")
                .content(linkCodeCompanionReg);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending companion_hello iq to {0}", phoneJid);
        var response = whatsapp.sendNode(iq);
        var error = extractIqError(response, "companion_hello");
        if (error != null) {
            throw error;
        }
        return extractRef(response);
    }

    /**
     * Builds and sends the {@code companion_finish} IQ.
     *
     * <p>This is a helper for {@link #handlePrimaryHello}. It carries
     * the encrypted identity bundle, the companion's long-term identity
     * public key, and the server-issued ref the relay uses to correlate
     * this IQ with the earlier {@code companion_hello}.
     *
     * @implNote
     * This implementation builds the stanza at the {@link StanzaBuilder}
     * level rather than through WA Web's
     * {@code WASmaxMdCompanionFinishRPC}. The element ordering (wrapped
     * bundle, companion identity, pairing ref) matches the order WA Web
     * declares in {@code WAWebAltDeviceLinkingIq.sendCompanionFinish}.
     *
     * @param phoneJid                        the primary phone-number
     *                                        JID being linked against
     * @param linkCodePairingWrappedKeyBundle the encrypted identity
     *                                        bundle
     * @param companionIdentityPublic         the companion's long-term
     *                                        identity public key
     * @param ref                             the server-issued ref
     *                                        echoed into the IQ
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingIq", exports = "sendCompanionFinish", adaptation = WhatsAppAdaptation.DIRECT)
    private void sendCompanionFinish(Jid phoneJid, byte[] linkCodePairingWrappedKeyBundle, byte[] companionIdentityPublic, byte[] ref) {
        var wrappedKeyBundle = new StanzaBuilder()
                .description("link_code_pairing_wrapped_key_bundle")
                .content(linkCodePairingWrappedKeyBundle)
                .build();

        var companionIdentity = new StanzaBuilder()
                .description("companion_identity_public")
                .content(companionIdentityPublic)
                .build();

        var pairingRef = new StanzaBuilder()
                .description("link_code_pairing_ref")
                .content(ref)
                .build();

        var linkCodeCompanionReg = new StanzaBuilder()
                .description("link_code_companion_reg")
                .attribute("jid", phoneJid)
                .attribute("stage", "companion_finish")
                .content(wrappedKeyBundle, companionIdentity, pairingRef)
                .build();

        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "md")
                .content(linkCodeCompanionReg);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending companion_finish iq to {0}", phoneJid);
        var response = whatsapp.sendNode(iq);
        var error = extractIqError(response, "companion_finish");
        if (error != null) {
            throw error;
        }
    }

    /**
     * Returns the {@code companion_platform_id} string identifying the
     * companion device to the primary.
     *
     * <p>The string is sent inside {@code companion_hello} so the
     * primary's confirmation dialog can name the device the user is
     * about to authorize. Cobalt does not run inside a browser, so the
     * mapping is approximate: the companion impersonates the client
     * whose user agent it advertises.
     *
     * @implNote
     * This implementation diverges from WA Web's
     * {@code WAWebCompanionRegClientUtils.DEVICE_PLATFORM} table in one
     * place. WA Web maps Edge running on Windows to the {@code EDGE = 2}
     * branch, but Cobalt deliberately answers {@code "8"} ({@code UWP})
     * for {@link LinkedWhatsAppClient}s whose configured device platform is
     * Windows, matching the wire shape the native UWP client sends.
     * MacOS maps to {@code "6"} ({@code SAFARI}), and every other
     * configuration falls back to {@code "1"} ({@code CHROME}).
     *
     * @return the decimal-string platform identifier
     */
    @WhatsAppWebExport(moduleName = "WAWebCompanionRegClientUtils", exports = "DEVICE_PLATFORM", adaptation = WhatsAppAdaptation.ADAPTED)
    private String companionPlatformId() {
        return switch (whatsapp.store().accountStore().device().platform()) {
            case WINDOWS -> "8";
            case MACOS -> "6";
            default -> "1";
        };
    }

    /**
     * Returns the {@code companion_platform_display} label shown in the
     * primary's confirmation UI.
     *
     * <p>The label appears under the platform icon when the primary
     * device asks the user to confirm the link. The browser portion of
     * the label must agree with the platform id returned by
     * {@link #companionPlatformId} so the primary device sees a
     * self-consistent identification.
     *
     * @implNote
     * This implementation mirrors WA Web's
     * {@code l.name+" ("+l.os+")"} from
     * {@code WAWebAltDeviceLinkingIq.sendCompanionHello} but reads
     * {@code os.name} via {@link System#getProperty(String, String)}
     * when the configured platform is neither Windows nor macOS, because
     * Cobalt has no equivalent of WA Web's {@code WAWebBrowserInfo}.
     *
     * @return the human-readable {@code "Browser (OS)"} label
     */
    private String companionPlatformDisplay() {
        return switch (whatsapp.store().accountStore().device().platform()) {
            case WINDOWS -> "Edge (Windows)";
            case MACOS -> "Safari (macOS)";
            default -> {
                var raw = System.getProperty("os.name", "Unknown");
                var lower = raw.toLowerCase();
                String os;
                if (lower.contains("mac")) {
                    os = "macOS";
                } else if (lower.contains("win")) {
                    os = "Windows";
                } else if (lower.contains("linux")) {
                    os = "Linux";
                } else {
                    os = raw;
                }
                yield "Chrome (" + os + ")";
            }
        };
    }

    /**
     * Extracts the {@code <link_code_pairing_ref>} content from the
     * {@code companion_hello} response.
     *
     * <p>This is a helper for {@link #sendCompanionHello}. It warns
     * through {@link System.Logger} when the ref is absent so a
     * malformed server response shows up in the diagnostic log even
     * though the caller raises a generic error.
     *
     * @param response the IQ result stanza, or {@code null}
     * @return the raw ref bytes, or {@code null} if the response or the
     *         ref child is absent
     */
    private byte[] extractRef(Stanza response) {
        if (response == null) {
            return null;
        }
        var ref = response.getChild("link_code_companion_reg")
                .flatMap(node -> node.getChild("link_code_pairing_ref"))
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        if (ref == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "companion_hello response missing ref: {0}", response);
        }
        return ref;
    }

    /**
     * Generates a fresh pairing code, ephemeral keypair, salt, and
     * counter, then wraps the public key under the PBKDF2-derived
     * AES-CTR key.
     *
     * <p>The wire-format contract is the concatenation
     * {@code salt || counter || AES-CTR(ephemeralPub)} carried inside
     * {@code <link_code_pairing_wrapped_companion_ephemeral_pub>}.
     *
     * @implNote
     * This implementation matches WA Web's {@code companionHelloInternal}
     * bit for bit on the wire but does not return WA Web's
     * {@code linkCodeKey} field: WA Web caches the imported PBKDF2 key so
     * {@code companionFinish} can reuse it, while Cobalt re-derives the
     * key from the pairing code and the primary's salt when needed, so
     * the cached key would have no consumer.
     *
     * @return a populated {@link CompanionPairingCompanionHello}
     * @throws GeneralSecurityException if the JCE provider rejects any
     *                                  step
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingAlgorithm", exports = {"companionHello", "companionHelloInternal"}, adaptation = WhatsAppAdaptation.DIRECT)
    private static CompanionPairingCompanionHello companionHello() throws GeneralSecurityException {
        var pairingCode = bytesToCrockford(DataUtils.randomByteArray(5));
        var ephemeralKeyPair = SignalIdentityKeyPair.random();
        var salt = DataUtils.randomByteArray(PBKDF2_SALT_LENGTH);
        var counter = DataUtils.randomByteArray(AES_CTR_IV_LENGTH);

        var aesKey = derivePairingKey(pairingCode, salt);
        var cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(counter));
        var ciphertext = cipher.doFinal(ephemeralKeyPair.publicKey().toEncodedPoint());

        var wrapped = DataUtils.concatByteArrays(salt, counter, ciphertext);
        return new CompanionPairingCompanionHello(pairingCode, wrapped, ephemeralKeyPair);
    }

    /**
     * Encodes exactly five bytes (40 bits) as an eight-character
     * Crockford-style base32 string over {@link #CROCKFORD_ALPHABET}.
     *
     * <p>The output is the human-readable pairing code shown to the
     * user. The fixed 5-byte input maps cleanly to eight 5-bit Crockford
     * symbols with no leftover bits.
     *
     * @implNote
     * This implementation specialises WA Web's arbitrary-length
     * {@code bytesToCrockford} loop (a {@link ByteBuffer}-equivalent
     * {@code DataView} walk with a trailing-bits branch) to
     * straight-line bit extractions, since the only call site passes
     * exactly five random bytes. The output is bit-for-bit identical to
     * WA Web's for any 5-byte input.
     *
     * @param input the 5-byte buffer to encode
     * @return an 8-character Crockford-base32 string
     * @throws IllegalArgumentException if {@code input.length != 5}
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingBase32Encode", exports = "bytesToCrockford", adaptation = WhatsAppAdaptation.ADAPTED)
    private static String bytesToCrockford(byte[] input) {
        if (input.length != 5) {
            throw new IllegalArgumentException("Crockford encoder expects exactly 5 bytes, got " + input.length);
        }

        var b0 = input[0] & 0xFF;
        var b1 = input[1] & 0xFF;
        var b2 = input[2] & 0xFF;
        var b3 = input[3] & 0xFF;
        var b4 = input[4] & 0xFF;

        var out = new char[8];
        out[0] = CROCKFORD_ALPHABET[(b0 >>> 3) & 0x1F];
        out[1] = CROCKFORD_ALPHABET[((b0 << 2) | (b1 >>> 6)) & 0x1F];
        out[2] = CROCKFORD_ALPHABET[(b1 >>> 1) & 0x1F];
        out[3] = CROCKFORD_ALPHABET[((b1 << 4) | (b2 >>> 4)) & 0x1F];
        out[4] = CROCKFORD_ALPHABET[((b2 << 1) | (b3 >>> 7)) & 0x1F];
        out[5] = CROCKFORD_ALPHABET[(b3 >>> 2) & 0x1F];
        out[6] = CROCKFORD_ALPHABET[((b3 << 3) | (b4 >>> 5)) & 0x1F];
        out[7] = CROCKFORD_ALPHABET[b4 & 0x1F];
        return new String(out);
    }

    /**
     * Completes the handshake after a {@code primary_hello}
     * notification.
     *
     * <p>This is a helper for {@link #handlePrimaryHello}. It re-derives
     * the PBKDF2 key from the pairing code and the primary's salt,
     * decrypts the primary's ephemeral public key, runs two X25519
     * agreements (ephemeral and identity), and wraps the companion
     * identity bundle under an HKDF-derived AES-GCM key.
     *
     * @implNote
     * This implementation matches WA Web's {@code companionFinishInternal}
     * bit for bit on the wire format but folds in the random generation
     * that WA Web routes through a separate {@code companionFinish}
     * wrapper (the {@code advSecretMaterialSalt}, {@code bundleHkdfSalt},
     * and {@code gcmIv} buffers WA Web fills from
     * {@code crypto.getRandomValues}). The HKDF-Extract-then-Expand
     * sequence is invoked through {@link KDF} with
     * {@link HKDFParameterSpec}, the JDK API equivalent of WA Web's
     * {@code WACryptoHkdf.extractWithSaltAndExpand}. AES-GCM is driven
     * with a sequence of
     * {@link Cipher#update(byte[], int, int, byte[], int)} calls so the
     * three plaintext segments (companion identity, primary identity,
     * ADV secret material salt) feed into one authentication tag in the
     * same order WA Web concatenates them.
     *
     * @param pairingCode                the pairing code the user typed
     *                                   on the primary
     * @param wrappedPrimaryEphemeralPub the salt {@code ||} counter
     *                                   {@code ||} ciphertext payload
     *                                   from {@code primary_hello}
     * @param primaryIdentityPublic      the primary's long-term identity
     *                                   public key
     * @param companionEphemeralKeyPair  the companion keypair produced
     *                                   by {@link #companionHello}
     * @param companionIdentityKeyPair   the companion's long-term
     *                                   identity keypair
     * @return a populated {@link CompanionPairingCompanionFinish}
     * @throws GeneralSecurityException if any argument is malformed or
     *                                  the JCE provider rejects a step
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingAlgorithm", exports = {"companionFinish", "companionFinishInternal"}, adaptation = WhatsAppAdaptation.DIRECT)
    private static CompanionPairingCompanionFinish companionFinish(
            String pairingCode,
            byte[] wrappedPrimaryEphemeralPub,
            byte[] primaryIdentityPublic,
            SignalIdentityKeyPair companionEphemeralKeyPair,
            SignalIdentityKeyPair companionIdentityKeyPair
    ) throws GeneralSecurityException {
        Objects.requireNonNull(pairingCode, "pairingCode");
        Objects.requireNonNull(wrappedPrimaryEphemeralPub, "wrappedPrimaryEphemeralPub");
        Objects.requireNonNull(primaryIdentityPublic, "primaryIdentityPublic");
        Objects.requireNonNull(companionEphemeralKeyPair, "companionEphemeralKeyPair");
        Objects.requireNonNull(companionIdentityKeyPair, "companionIdentityKeyPair");

        if (wrappedPrimaryEphemeralPub.length < PBKDF2_SALT_LENGTH + AES_CTR_IV_LENGTH + CURVE25519_PUBLIC_KEY_LENGTH) {
            throw new GeneralSecurityException(
                    "link_code_pairing_wrapped_primary_ephemeral_pub is truncated: " + wrappedPrimaryEphemeralPub.length + " bytes");
        }

        var kdf = KDF.getInstance("HKDF-SHA256");

        var buffer = ByteBuffer.wrap(wrappedPrimaryEphemeralPub);
        var primarySalt = new byte[PBKDF2_SALT_LENGTH];
        buffer.get(primarySalt);
        var primaryCounter = new byte[AES_CTR_IV_LENGTH];
        buffer.get(primaryCounter);
        var primaryCiphertext = new byte[buffer.remaining()];
        buffer.get(primaryCiphertext);

        var primaryAesKey = derivePairingKey(pairingCode, primarySalt);
        var aesCtrCipher = Cipher.getInstance("AES/CTR/NoPadding");
        aesCtrCipher.init(Cipher.DECRYPT_MODE, primaryAesKey, new IvParameterSpec(primaryCounter));
        var primaryEphemeralPublic = aesCtrCipher.doFinal(primaryCiphertext);
        if (primaryEphemeralPublic.length == 0) {
            throw new GeneralSecurityException("Decrypted primary ephemeral public key is empty");
        }

        var ephemeralShared = Curve25519.sharedKey(
                companionEphemeralKeyPair.privateKey().toEncodedPoint(), primaryEphemeralPublic);

        var advSecretMaterialSalt = DataUtils.randomByteArray(HKDF_OUTPUT_LENGTH);
        var bundleHkdfSalt = DataUtils.randomByteArray(HKDF_OUTPUT_LENGTH);
        var gcmIv = DataUtils.randomByteArray(AES_GCM_IV_LENGTH);

        var bundleKeyParams = HKDFParameterSpec.ofExtract()
                .addIKM(ephemeralShared)
                .addSalt(bundleHkdfSalt)
                .thenExpand(BUNDLE_ENCRYPTION_INFO.getBytes(StandardCharsets.US_ASCII), HKDF_OUTPUT_LENGTH);
        var bundleKey = kdf.deriveData(bundleKeyParams);

        var companionIdentityPublic = companionIdentityKeyPair.publicKey().toEncodedPoint();
        var aesGcmCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesGcmCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bundleKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, gcmIv));
        var keyBundlePlaintextLength = companionIdentityPublic.length + primaryIdentityPublic.length + advSecretMaterialSalt.length;
        var keyBundleCiphertext = new byte[aesGcmCipher.getOutputSize(keyBundlePlaintextLength)];
        var keyBundleCiphertextOffset = 0;
        keyBundleCiphertextOffset += aesGcmCipher.update(companionIdentityPublic, 0, companionIdentityPublic.length, keyBundleCiphertext, keyBundleCiphertextOffset);
        keyBundleCiphertextOffset += aesGcmCipher.update(primaryIdentityPublic, 0, primaryIdentityPublic.length, keyBundleCiphertext, keyBundleCiphertextOffset);
        keyBundleCiphertextOffset += aesGcmCipher.update(advSecretMaterialSalt, 0, advSecretMaterialSalt.length, keyBundleCiphertext, keyBundleCiphertextOffset);
        aesGcmCipher.doFinal(keyBundleCiphertext, keyBundleCiphertextOffset);

        var wrappedKeyBundle = new byte[bundleHkdfSalt.length + gcmIv.length + keyBundleCiphertext.length];
        var wrappedKeyBundleOffset = 0;
        System.arraycopy(bundleHkdfSalt, 0, wrappedKeyBundle, wrappedKeyBundleOffset, bundleHkdfSalt.length);
        wrappedKeyBundleOffset += bundleHkdfSalt.length;
        System.arraycopy(gcmIv, 0, wrappedKeyBundle, wrappedKeyBundleOffset, gcmIv.length);
        wrappedKeyBundleOffset += gcmIv.length;
        System.arraycopy(keyBundleCiphertext, 0, wrappedKeyBundle, wrappedKeyBundleOffset, keyBundleCiphertext.length);

        var identityShared = Curve25519.sharedKey(
                companionIdentityKeyPair.privateKey().toEncodedPoint(), primaryIdentityPublic);

        var advSecretParams = HKDFParameterSpec.ofExtract()
                .addIKM(ephemeralShared)
                .addIKM(identityShared)
                .addIKM(advSecretMaterialSalt)
                .thenExpand(ADV_SECRET_INFO.getBytes(StandardCharsets.US_ASCII), HKDF_OUTPUT_LENGTH);
        var advSecret = kdf.deriveData(advSecretParams);

        return new CompanionPairingCompanionFinish(wrappedKeyBundle, advSecret, companionIdentityPublic);
    }

    /**
     * Derives a 256-bit AES-CTR key from the pairing code via
     * PBKDF2-HMAC-SHA256.
     *
     * <p>This is a helper called from both {@link #companionHello}
     * (encrypting the companion ephemeral public key) and
     * {@link #companionFinish} (decrypting the primary ephemeral public
     * key). The pairing code is the password and the caller-supplied
     * salt is the same {@link #PBKDF2_SALT_LENGTH} byte buffer that
     * prefixes the wire payload.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code WAWebAltDeviceLinkingAlgorithm.deriveKey} parameter for
     * parameter: SHA-256 HMAC, {@value #PBKDF2_ITERATIONS} iterations,
     * {@value #PBKDF2_KEY_BITS}-bit output. The result is rewrapped as a
     * {@link SecretKeySpec} so it can drive an
     * {@code "AES/CTR/NoPadding"} cipher directly.
     *
     * @param pairingCode the eight-character pairing code used as the
     *                    password
     * @param salt        the per-side PBKDF2 salt
     * @return a {@link SecretKey} suitable for {@code AES/CTR/NoPadding}
     * @throws GeneralSecurityException if the JCE provider rejects the
     *                                  parameters
     */
    @WhatsAppWebExport(moduleName = "WAWebAltDeviceLinkingAlgorithm", exports = "deriveKey", adaptation = WhatsAppAdaptation.DIRECT)
    private static SecretKey derivePairingKey(String pairingCode, byte[] salt) throws GeneralSecurityException {
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var spec = new PBEKeySpec(pairingCode.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
        var raw = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(raw, "AES");
    }
}
