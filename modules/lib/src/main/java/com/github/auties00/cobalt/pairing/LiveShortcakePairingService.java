package com.github.auties00.cobalt.pairing;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientPasskeyAuthenticator;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.device.identity.CompanionCommitmentBuilder;
import com.github.auties00.cobalt.wire.linked.device.identity.CompanionEphemeralIdentityBuilder;
import com.github.auties00.cobalt.wire.linked.device.identity.CompanionEphemeralIdentitySpec;
import com.github.auties00.cobalt.wire.linked.device.identity.PrimaryEphemeralIdentity;
import com.github.auties00.cobalt.wire.linked.device.identity.PrimaryEphemeralIdentitySpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.DevicePlatformType;
import com.github.auties00.cobalt.wire.linked.device.pairing.EncryptedPairingRequestBuilder;
import com.github.auties00.cobalt.wire.linked.device.pairing.EncryptedPairingRequestSpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.PairingRequestBuilder;
import com.github.auties00.cobalt.wire.linked.device.pairing.PairingRequestSpec;
import com.github.auties00.cobalt.wire.linked.device.pairing.ProloguePayloadBuilder;
import com.github.auties00.cobalt.wire.linked.device.pairing.ProloguePayloadSpec;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

/**
 * Live implementation of {@link ShortcakePairingService}.
 *
 * <p>Drives the companion side of WhatsApp's Shortcake passkey linking: it asserts the account
 * passkey through the configured {@link LinkedWhatsAppClientPasskeyAuthenticator}, ships the prologue
 * carrying that assertion, then on the primary's reply derives a short verification code and an
 * encryption key from a Curve25519 agreement and sends the AES-GCM-sealed pairing request.
 *
 * @implNote This is the passkey sibling of {@link LiveCompanionPairingService} and follows its
 * inline-{@link StanzaBuilder} and {@link Curve25519}/{@link KDF}/{@link Cipher} idioms. The companion
 * ephemeral public key is the raw 32-byte X25519 point {@code toEncodedPoint()} returns, which is what
 * the primary expects inside the {@code CompanionEphemeralIdentity}. The companion device type is fixed
 * to {@link DevicePlatformType#CHROME} (numeric {@value #DEVICE_TYPE_INDEX} in the key-derivation salt).
 * The ceremony is driven from {@link #start()}: the server answers the {@code passkey_request_options}
 * request directly (it does not require the {@code passkey_prologue_request} server prompt and returns
 * options with {@code rpId="whatsapp.com"}, an empty allow list, and {@code userVerification="required"}),
 * so the companion always initiates. The pairing-handoff-proof continuation that reuses a prior ADV
 * secret is not produced for a fresh link.
 */
public final class LiveShortcakePairingService implements ShortcakePairingService {
    /**
     * The logger for {@link LiveShortcakePairingService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveShortcakePairingService.class);

    /**
     * Holds the pairing-code Crockford-style base32 alphabet, skipping ambiguous symbols.
     *
     * @implNote This implementation copies the literal from
     * {@code WAWebAltDeviceLinkingBase32Encode} verbatim, matching the encoder the Shortcake
     * verification code shares with the pairing-code flow.
     */
    private static final char[] CROCKFORD_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTVWXYZ".toCharArray();

    /**
     * Holds the HKDF {@code info} label deriving the AES-GCM key from the ephemeral shared secret.
     */
    private static final String ENCRYPTION_KEY_INFO = "Pairing Information Encryption Key";

    /**
     * Holds the length in bytes of the short authentication string before base32 encoding.
     */
    private static final int SAS_LENGTH = 5;

    /**
     * Holds the length in bytes of the companion nonce committed to and later revealed.
     */
    private static final int NONCE_LENGTH = 32;

    /**
     * Holds the length in bytes of the AES-GCM nonce sealing the pairing request.
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * Holds the length in bits of the AES-GCM authentication tag.
     */
    private static final int GCM_TAG_BITS = 128;

    /**
     * Holds the length in bytes of the HKDF-derived AES-GCM key.
     */
    private static final int ENCRYPTION_KEY_LENGTH = 32;

    /**
     * Holds the companion device platform advertised in the ephemeral identity.
     */
    private static final DevicePlatformType DEVICE_TYPE = DevicePlatformType.CHROME;

    /**
     * Holds the numeric device-type value folded into the key-derivation salt.
     *
     * @implNote This implementation uses {@code 1} to match {@link DevicePlatformType#CHROME}'s
     * protobuf index, so the salt the companion computes matches the one the primary derives from the
     * received ephemeral identity.
     */
    private static final int DEVICE_TYPE_INDEX = 1;

    /**
     * Holds the {@link LinkedWhatsAppClient} used to reach the store and dispatch the ceremony IQs.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the configured web verification handler; only a passkey handler enables this service.
     */
    private final LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler;

    /**
     * Holds the authenticator that asserts the account passkey, or {@code null} when none is set.
     */
    private final LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator;

    /**
     * Holds the mutex serialising every transition of {@link #stage} and the cached ceremony fields.
     */
    private final Object lock;

    /**
     * Holds the current ceremony stage.
     */
    private ShortcakePairingStage stage;

    /**
     * Holds the companion's ephemeral Curve25519 keypair for the current ceremony.
     */
    private SignalIdentityKeyPair companionEphemeralKeyPair;

    /**
     * Holds the 32-byte companion nonce committed to in the prologue and revealed afterwards.
     */
    private byte[] companionNonce;

    /**
     * Holds the server-issued companion reference echoed through the ceremony.
     */
    private String ref;

    /**
     * Constructs a service tied to the given client, handler, and authenticator.
     *
     * @param whatsapp               the WhatsApp client; never {@code null}
     * @param webVerificationHandler the verification handler, possibly a non-passkey variant
     * @param passkeyAuthenticator   the passkey authenticator, or {@code null} when none is configured
     * @throws NullPointerException if {@code whatsapp} is {@code null}
     */
    public LiveShortcakePairingService(LinkedWhatsAppClient whatsapp,
                                       LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler,
                                       LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp must not be null");
        this.webVerificationHandler = webVerificationHandler;
        this.passkeyAuthenticator = passkeyAuthenticator;
        this.lock = new Object();
        this.stage = ShortcakePairingStage.NOT_STARTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return webVerificationHandler instanceof LinkedWhatsAppClientVerificationHandler.Web.Passkey
                && passkeyAuthenticator != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws GeneralSecurityException {
        synchronized (lock) {
            if (!isEnabled()) {
                throw new IllegalStateException("Shortcake passkey linking is not configured");
            }
            if (stage != ShortcakePairingStage.NOT_STARTED) {
                return;
            }

            clearLocked();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting shortcake passkey pairing");
            var optionsBytes = requestPasskeyRequestOptions();
            var assertion = passkeyAuthenticator.assertCredential(
                    LinkedWhatsAppClientPasskeyAuthenticator.Request.ofShortcakeOptions(
                            optionsBytes, LinkedWhatsAppClientPasskeyAuthenticator.Request.WEB_ORIGIN));

            ref = sendGetRef();
            companionEphemeralKeyPair = SignalIdentityKeyPair.random();
            companionNonce = DataUtils.randomByteArray(NONCE_LENGTH);

            var companionEphemeralIdentity = new CompanionEphemeralIdentityBuilder()
                    .publicKey(companionEphemeralKeyPair.publicKey().toEncodedPoint())
                    .deviceType(DEVICE_TYPE)
                    .ref(ref)
                    .build();
            var companionEphemeralIdentityBytes = CompanionEphemeralIdentitySpec.encode(companionEphemeralIdentity);
            var commitmentHash = MessageDigest.getInstance("SHA-256")
                    .digest(DataUtils.concatByteArrays(companionEphemeralIdentityBytes, companionNonce));
            var commitment = new CompanionCommitmentBuilder()
                    .hash(commitmentHash)
                    .build();
            var prologuePayload = new ProloguePayloadBuilder()
                    .companionEphemeralIdentity(companionEphemeralIdentityBytes)
                    .commitment(commitment)
                    .build();

            sendSetPasskeyPrologue(assertion, ProloguePayloadSpec.encode(prologuePayload));
            stage = ShortcakePairingStage.WAITING_FOR_PRIMARY_IDENTITY;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "shortcake prologue sent, ref={0}", new LogRedactable.Token(ref));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePrimaryEphemeralIdentity(byte[] primaryEphemeralIdentity) throws GeneralSecurityException {
        synchronized (lock) {
            if (stage != ShortcakePairingStage.WAITING_FOR_PRIMARY_IDENTITY) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "crsc_continuation received out of order, stage {0}", stage);
                throw new IllegalStateException("crsc_continuation received while in stage " + stage);
            }
            if (companionEphemeralKeyPair == null || companionNonce == null || ref == null) {
                throw new GeneralSecurityException("Cached Shortcake prologue is missing");
            }

            var primaryIdentity = decodePrimaryEphemeralIdentity(primaryEphemeralIdentity);
            var primaryPublicKey = primaryIdentity.publicKey()
                    .orElseThrow(() -> new GeneralSecurityException("PrimaryEphemeralIdentity has no public key"));
            var primaryNonce = primaryIdentity.nonce()
                    .orElseThrow(() -> new GeneralSecurityException("PrimaryEphemeralIdentity has no nonce"));
            if (primaryPublicKey.length != NONCE_LENGTH) {
                throw new GeneralSecurityException("PrimaryEphemeralIdentity public key must be 32 bytes");
            }

            sendSetCompanionNonce(companionNonce);

            var verificationCode = deriveVerificationCode(companionNonce, primaryPublicKey, primaryNonce);
            var encryptionKey = deriveEncryptionKey(primaryPublicKey);

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "shortcake verification code derived: {0}", new LogRedactable.Code(verificationCode));
            var passkeyHandler = (LinkedWhatsAppClientVerificationHandler.Web.Passkey) webVerificationHandler;
            passkeyHandler.handle(verificationCode);
            if (!passkeyHandler.confirmVerificationCode(verificationCode)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "shortcake verification code was not confirmed");
                clearLocked();
                throw new GeneralSecurityException("Shortcake verification code was not confirmed");
            }

            var pairingRequest = buildPairingRequest();
            var encryptedPairingRequest = encryptPairingRequest(pairingRequest, encryptionKey);
            sendSetEncryptedPairingRequest(encryptedPairingRequest);
            stage = ShortcakePairingStage.WAITING_FOR_PAIRING_COMPLETION;
            if (Log.INFO) LOGGER.log(Level.INFO, "shortcake passkey pairing completed");
        }
    }

    /**
     * Resets every field in the cached ceremony state. The caller must hold {@link #lock}.
     */
    private void clearLocked() {
        stage = ShortcakePairingStage.NOT_STARTED;
        companionEphemeralKeyPair = null;
        companionNonce = null;
        ref = null;
    }

    /**
     * Sends the {@code GetPasskeyRequestOptions} IQ and returns the opaque WebAuthn options bytes.
     *
     * @return the raw {@code passkey_request_options} bytes
     * @throws GeneralSecurityException if the server omits the options or returns an error
     */
    private byte[] requestPasskeyRequestOptions() throws GeneralSecurityException {
        var request = new StanzaBuilder()
                .description("passkey_request_options")
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .attribute("xmlns", "md")
                .content(request);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending get_passkey_request_options iq");
        var response = whatsapp.sendNode(iq);
        throwIfError(response, "get_passkey_request_options");
        return response.getChild("passkey_request_options")
                .flatMap(Stanza::toContentBytes)
                .orElseThrow(() -> new GeneralSecurityException("Server did not return passkey_request_options"));
    }

    /**
     * Sends the {@code GetRef} IQ and returns the companion reference string.
     *
     * @return the UTF-8 decoded reference string
     * @throws GeneralSecurityException if the server omits the reference or returns an error
     */
    private String sendGetRef() throws GeneralSecurityException {
        var request = new StanzaBuilder()
                .description("ref")
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "get")
                .attribute("xmlns", "md")
                .content(request);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending get_ref iq");
        var response = whatsapp.sendNode(iq);
        throwIfError(response, "get_ref");
        var refBytes = response.getChild("ref")
                .flatMap(Stanza::toContentBytes)
                .orElseThrow(() -> new GeneralSecurityException("Server did not return a ref"));
        return new String(refBytes, StandardCharsets.UTF_8);
    }

    /**
     * Sends the {@code SetPasskeyPrologue} IQ carrying the assertion and prologue payload.
     *
     * @param assertion       the WebAuthn assertion answering the request options
     * @param prologuePayload the serialised {@code ProloguePayload}
     * @throws GeneralSecurityException if the server returns an error
     */
    private void sendSetPasskeyPrologue(LinkedWhatsAppClientPasskeyAuthenticator.Assertion assertion,
                                        byte[] prologuePayload) throws GeneralSecurityException {
        var credentialId = new StanzaBuilder()
                .description("credential_id")
                .content(assertion.credentialId())
                .build();
        var webauthnAssertion = new StanzaBuilder()
                .description("webauthn_assertion")
                .content(buildAssertionJson(assertion).getBytes(StandardCharsets.UTF_8))
                .build();
        var prologue = new StanzaBuilder()
                .description("prologue_payload")
                .content(prologuePayload)
                .build();
        var passkeyPrologue = new StanzaBuilder()
                .description("passkey_prologue")
                .content(credentialId, webauthnAssertion, prologue)
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "md")
                .content(passkeyPrologue);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending set_passkey_prologue iq");
        var response = whatsapp.sendNode(iq);
        throwIfError(response, "set_passkey_prologue");
    }

    /**
     * Sends the {@code SetCompanionNonce} IQ revealing the committed nonce.
     *
     * @param nonce the companion nonce
     * @throws GeneralSecurityException if the server returns an error
     */
    private void sendSetCompanionNonce(byte[] nonce) throws GeneralSecurityException {
        var companionNonceNode = new StanzaBuilder()
                .description("companion_nonce")
                .content(nonce)
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "md")
                .content(companionNonceNode);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending set_companion_nonce iq");
        var response = whatsapp.sendNode(iq);
        throwIfError(response, "set_companion_nonce");
    }

    /**
     * Sends the {@code SetEncryptedPairingRequest} IQ carrying the sealed pairing request.
     *
     * @param encryptedPairingRequest the serialised {@code EncryptedPairingRequest}
     * @throws GeneralSecurityException if the server returns an error
     */
    private void sendSetEncryptedPairingRequest(byte[] encryptedPairingRequest) throws GeneralSecurityException {
        var encryptedNode = new StanzaBuilder()
                .description("encrypted_pairing_request")
                .content(encryptedPairingRequest)
                .build();
        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .attribute("xmlns", "md")
                .content(encryptedNode);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending set_encrypted_pairing_request iq");
        var response = whatsapp.sendNode(iq);
        throwIfError(response, "set_encrypted_pairing_request");
    }

    /**
     * Decodes a serialised {@code PrimaryEphemeralIdentity}.
     *
     * @param bytes the serialised identity
     * @return the decoded identity
     */
    private PrimaryEphemeralIdentity decodePrimaryEphemeralIdentity(byte[] bytes) {
        return PrimaryEphemeralIdentitySpec.decode(bytes);
    }

    /**
     * Derives the eight-character verification code from the nonces and the primary public key.
     *
     * <p>The short authentication string is the first {@value #SAS_LENGTH} bytes of the primary nonce
     * XORed with the SHA-256 of the companion nonce concatenated with the primary public key, then
     * Crockford-base32 encoded.
     *
     * @param companionNonce   the companion nonce
     * @param primaryPublicKey the primary's ephemeral public key
     * @param primaryNonce     the primary nonce
     * @return the eight-character verification code
     * @throws GeneralSecurityException if SHA-256 is unavailable
     */
    private String deriveVerificationCode(byte[] companionNonce, byte[] primaryPublicKey, byte[] primaryNonce)
            throws GeneralSecurityException {
        var digest = MessageDigest.getInstance("SHA-256")
                .digest(DataUtils.concatByteArrays(companionNonce, primaryPublicKey));
        var sas = new byte[SAS_LENGTH];
        for (var i = 0; i < SAS_LENGTH; i++) {
            sas[i] = (byte) (primaryNonce[i] ^ digest[i]);
        }
        return bytesToCrockford(sas);
    }

    /**
     * Derives the AES-GCM pairing key from the Curve25519 agreement.
     *
     * @param primaryPublicKey the primary's ephemeral public key
     * @return the 32-byte encryption key
     * @throws GeneralSecurityException if the JCE provider rejects a step
     */
    private byte[] deriveEncryptionKey(byte[] primaryPublicKey) throws GeneralSecurityException {
        var sharedSecret = Curve25519.sharedKey(
                companionEphemeralKeyPair.privateKey().toEncodedPoint(), primaryPublicKey);
        var salt = ("Companion Pairing " + DEVICE_TYPE_INDEX + " with ref " + ref)
                .getBytes(StandardCharsets.UTF_8);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(sharedSecret)
                .addSalt(salt)
                .thenExpand(ENCRYPTION_KEY_INFO.getBytes(StandardCharsets.US_ASCII), ENCRYPTION_KEY_LENGTH);
        return KDF.getInstance("HKDF-SHA256").deriveData(params);
    }

    /**
     * Builds the serialised {@code PairingRequest} from the local Noise, identity, and ADV material.
     *
     * @return the serialised pairing request
     */
    private byte[] buildPairingRequest() {
        var signalStore = whatsapp.store().signalStore();
        var advSecret = signalStore.advSecretKey().orElseGet(() -> {
            var generated = DataUtils.randomByteArray(NONCE_LENGTH);
            signalStore.setAdvSecretKey(generated);
            return generated;
        });
        var pairingRequest = new PairingRequestBuilder()
                .companionPublicKey(signalStore.noiseKeyPair().publicKey().toEncodedPoint())
                .companionIdentityKey(signalStore.identityKeyPair().publicKey().toEncodedPoint())
                .advSecret(advSecret)
                .build();
        return PairingRequestSpec.encode(pairingRequest);
    }

    /**
     * Seals the pairing request under the derived encryption key with a fresh AES-GCM nonce.
     *
     * @param pairingRequest the serialised pairing request
     * @param encryptionKey  the derived AES-GCM key
     * @return the serialised {@code EncryptedPairingRequest}
     * @throws GeneralSecurityException if the JCE provider rejects a step
     */
    private byte[] encryptPairingRequest(byte[] pairingRequest, byte[] encryptionKey)
            throws GeneralSecurityException {
        var iv = DataUtils.randomByteArray(GCM_IV_LENGTH);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        var ciphertext = cipher.doFinal(pairingRequest);
        var encrypted = new EncryptedPairingRequestBuilder()
                .encryptedPayload(ciphertext)
                .iv(iv)
                .build();
        return EncryptedPairingRequestSpec.encode(encrypted);
    }

    /**
     * Serialises a WebAuthn assertion into the JSON shape the prologue's {@code webauthn_assertion}
     * element carries.
     *
     * @param assertion the assertion to serialise
     * @return the assertion JSON string
     */
    private String buildAssertionJson(LinkedWhatsAppClientPasskeyAuthenticator.Assertion assertion) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var response = new JSONObject();
        response.put("clientDataJSON", encoder.encodeToString(assertion.clientDataJson()));
        response.put("authenticatorData", encoder.encodeToString(assertion.authenticatorData()));
        response.put("signature", encoder.encodeToString(assertion.signature()));
        response.put("userHandle", assertion.userHandle() != null
                ? encoder.encodeToString(assertion.userHandle()) : null);
        var json = new JSONObject();
        json.put("id", encoder.encodeToString(assertion.credentialId()));
        json.put("rawId", encoder.encodeToString(assertion.credentialId()));
        json.put("type", "public-key");
        json.put("response", response);
        return JSON.toJSONString(json, JSONWriter.Feature.WriteNulls);
    }

    /**
     * Throws a {@link WhatsAppRegistrationException} when the IQ response is an error.
     *
     * @param response the IQ response
     * @param flow     a short label identifying the sub-flow
     * @throws WhatsAppRegistrationException if the response is an {@code <iq type="error">}
     */
    private void throwIfError(Stanza response, String flow) {
        if (response != null && response.hasAttribute("type", "error")) {
            var error = response.getChild("error").orElse(null);
            var code = error == null ? -1 : error.getAttributeAsInt("code", -1);
            var text = error == null ? null : error.getAttributeAsString("text", null);
            if (Log.WARNING) LOGGER.log(Level.WARNING, "{0} rejected by server, code={1}", flow, code);
            throw new WhatsAppRegistrationException(
                    "shortcake: " + flow + " rejected by server (code=" + code + ", text=" + text + ")");
        }
    }

    /**
     * Encodes exactly five bytes as an eight-character Crockford-style base32 string.
     *
     * @param input the five-byte short authentication string
     * @return the eight-character verification code
     */
    private static String bytesToCrockford(byte[] input) {
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
}
