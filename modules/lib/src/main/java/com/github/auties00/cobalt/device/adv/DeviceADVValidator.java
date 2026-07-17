package com.github.auties00.cobalt.device.adv;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.DeviceConstants;
import com.github.auties00.cobalt.exception.linked.WhatsAppAdvValidationException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.identity.*;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.SignalProtocolAddress;
import com.github.auties00.libsignal.key.SignalIdentityKey;
import it.auties.protobuf.exception.ProtobufDeserializationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates Account Device Verification (ADV) signatures binding companion device identities to the
 * primary WhatsApp account.
 *
 * <p>This is the single entry point for every ADV signature check Cobalt performs: pairing (local
 * device identity from a {@code <device-identity>} pair-success stanza, via
 * {@link #extractAndValidateLocalSignedDeviceIdentity(Stanza)}), prekey fetches (remote companion
 * device identity from a {@code <device-identity>} prekey stanza, via
 * {@link #extractAndValidateRemoteSignedDeviceIdentity(Jid, Stanza, byte[], boolean)}), and signed
 * key-index list verification during device-list synchronisation and ADV change notifications (via
 * {@link #decodeSignedKeyIndexBytes(Jid, byte[])} and {@link #verifySKeyIndexWithAccSigKey(byte[])}).
 * It covers both standard end-to-end encrypted accounts (E2EE) and hosted business-coexistence
 * accounts (HOSTED). The crypto is Curve25519 signatures over messages prefixed with two-byte
 * domain-separation headers; the headers themselves come from {@code WAWebAdvSignatureConstants}.
 */
@WhatsAppWebModule(moduleName = "WAWebAdvSignatureApi")
@WhatsAppWebModule(moduleName = "WAWebHandleAdvDeviceNotificationUtils")
@WhatsAppWebModule(moduleName = "WAWebAdvSignatureConstants")
public final class DeviceADVValidator {
    /**
     * The logger for {@link DeviceADVValidator}.
     */
    private static final System.Logger LOGGER = Log.get(DeviceADVValidator.class);

    /**
     * Holds the domain-separation header {@code [6, 0]} for the E2EE device-identity account
     * signature.
     *
     * <p>The header is prefixed to the signed message during the account-signature check in
     * {@link #extractAndValidateLocalSignedDeviceIdentity(Stanza)} and
     * {@link #extractAndValidateRemoteSignedDeviceIdentity(Jid, Stanza, byte[], boolean)} for
     * non-hosted accounts.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureConstants",
            exports = "ADV_PREFIX_DEVICE_IDENTITY_ACCOUNT_SIGNATURE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final byte[] E2EE_ACCOUNT_SIGNATURE_HEADER = {6, 0};

    /**
     * Holds the domain-separation header {@code [6, 1]} for the E2EE device-identity device
     * signature.
     *
     * <p>The header is used both to verify remote E2EE device signatures and to generate the local
     * device signature during pairing.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureConstants",
            exports = "ADV_PREFIX_DEVICE_IDENTITY_DEVICE_SIGNATURE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final byte[] E2EE_DEVICE_SIGNATURE_HEADER = {6, 1};

    /**
     * Holds the domain-separation header {@code [6, 2]} for signed key-index lists.
     *
     * <p>The header is prefixed to the signed message during the key-index-list signature check in
     * {@link #verifyAndBuildResult}.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureConstants",
            exports = "ADV_PREFIX_KEY_INDEX_LIST_ACCOUNT_SIGNATURE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final byte[] KEY_INDEX_LIST_SIGNATURE_HEADER = {6, 2};

    /**
     * Holds the domain-separation header {@code [6, 5]} for the HOSTED device-identity account
     * signature.
     *
     * <p>The header is selected over {@link #E2EE_ACCOUNT_SIGNATURE_HEADER} when the inner
     * {@code ADVDeviceIdentity.deviceType} is HOSTED and the {@code adv_accept_hosted_devices} AB
     * prop is on.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureConstants",
            exports = "ADV_HOSTED_PREFIX_DEVICE_IDENTITY_ACCOUNT_SIGNATURE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final byte[] HOSTED_ACCOUNT_SIGNATURE_HEADER = {6, 5};

    /**
     * Holds the domain-separation header {@code [6, 6]} for the HOSTED device-identity device
     * signature.
     *
     * <p>The header is used only to verify remote HOSTED devices, where the device-signature header
     * is driven by the JID's hosted flag rather than the protobuf. WA Web always generates the local
     * device signature with {@link #E2EE_DEVICE_SIGNATURE_HEADER}, so this header has no
     * generate-side counterpart.
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureConstants",
            exports = "ADV_HOSTED_PREFIX_DEVICE_IDENTITY_DEVICE_SIGNATURE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final byte[] HOSTED_DEVICE_SIGNATURE_HEADER = {6, 6};

    /**
     * Holds the store providing the local identity key pair, the ADV secret key, and stored peer
     * identity keys.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Holds the AB props service used to gate hosted-device behaviour.
     */
    private final ABPropsService abProps;

    /**
     * Constructs an ADV validator bound to the given store and AB props service.
     *
     * @param store   the store
     * @param abProps the AB props service
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = "validateADVwithIdentityKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public DeviceADVValidator(LinkedWhatsAppStore store, ABPropsService abProps) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.abProps = Objects.requireNonNull(abProps, "abProps cannot be null");
    }

    /**
     * Returns whether the hosted business-coexistence path is enabled.
     *
     * <p>The value reflects the {@code adv_accept_hosted_devices} AB prop. Every code path that
     * selects between E2EE and HOSTED headers consults this gate first.
     *
     * @return {@code true} when {@code adv_accept_hosted_devices} is set
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCoexGatingUtils",
            exports = "bizHostedDevicesEnabled",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isBizHostedDevicesEnabled() {
        return abProps.getBool(ABProp.ADV_ACCEPT_HOSTED_DEVICES, false);
    }

    /**
     * Extracts and validates the local device identity from a pairing response, then generates the
     * device signature.
     *
     * <p>This is called once during pairing, when the {@code <pair-success>} envelope carries the
     * {@code <device-identity>} stanza. It verifies the HMAC over the device identity using the
     * locally-stored {@code advSecretKey}, verifies the account signature using the device
     * identity's embedded account signature key, and signs the bundle with the local Signal identity
     * to produce the device signature returned in the result.
     *
     * @implNote
     * This implementation selects the account-signature header from the inner
     * {@code ADVDeviceIdentity.deviceType} (gated by the hosted-devices feature flag) and from the
     * SMB-versus-consumer platform check on the outer HMAC input. WA Web always uses the E2EE header
     * when generating the local device signature; the HOSTED device-signature header only ever
     * participates in verification of remote devices.
     *
     * @param deviceIdentityStanza the {@code <device-identity>} pairing stanza
     * @return the validated signed device identity, carrying the freshly generated device signature
     * @throws WhatsAppAdvValidationException if HMAC or signature validation fails
     * @throws IllegalStateException          if required store values are missing
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = {"verifyDeviceIdentityAccountSignature", "generateDeviceSignature"},
            adaptation = WhatsAppAdaptation.DIRECT)
    public ADVSignedDeviceIdentity extractAndValidateLocalSignedDeviceIdentity(Stanza deviceIdentityStanza) {
        Objects.requireNonNull(deviceIdentityStanza, "deviceIdentityStanza cannot be null");

        var localJid = store.accountStore().jid()
                .orElseThrow(() -> new IllegalStateException("Local JID not set in store"));
        var advSecretKey = store.signalStore().advSecretKey()
                .orElseThrow(() -> new IllegalStateException("ADV secret key not set in store"));
        var localIdentityKeyPair = store.signalStore().identityKeyPair();

        if (advSecretKey.length != 32) {
            throw new IllegalArgumentException("advSecretKey must be 32 bytes, got " + advSecretKey.length);
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "validating local device identity for {0}", localJid);

        try {
            var deviceIdentityHmacBytes = deviceIdentityStanza.getChild("device-identity")
                    .orElseThrow(() -> new WhatsAppAdvValidationException.MissingDeviceIdentity(localJid))
                    .toContentBytes()
                    .orElseThrow(() -> new WhatsAppAdvValidationException.EmptyDeviceIdentity(localJid));

            var deviceIdentityHmac = ADVSignedDeviceIdentityHMACSpec.decode(deviceIdentityHmacBytes);
            var details = deviceIdentityHmac.details()
                    .orElseThrow(() -> new NullPointerException("details cannot be null"));
            var hmac = deviceIdentityHmac.hmac()
                    .orElseThrow(() -> new NullPointerException("hmac cannot be null"));

            byte[] hmacInput;
            var outerEncryptionType = deviceIdentityHmac.accountType()
                    .orElse(ADVEncryptionType.E2EE);
            var platform = store.accountStore().device().platform();
            var isSMB = platform == ClientPlatformType.ANDROID_BUSINESS || platform == ClientPlatformType.IOS_BUSINESS;
            if (isSMB && outerEncryptionType == ADVEncryptionType.HOSTED) {
                hmacInput = DataUtils.concatByteArrays(HOSTED_ACCOUNT_SIGNATURE_HEADER, details);
            } else {
                hmacInput = details;
            }

            var mac = Mac.getInstance("HmacSHA256");
            var secretKey = new SecretKeySpec(advSecretKey, "HmacSHA256");
            mac.init(secretKey);
            var computedHmac = mac.doFinal(hmacInput);
            if (!Arrays.equals(hmac, computedHmac)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "adv hmac validation failed for {0}", localJid);
                throw new WhatsAppAdvValidationException.HmacValidationFailed(localJid);
            }

            var deviceIdentity = ADVSignedDeviceIdentitySpec.decode(details);
            var deviceIdentityDetails = deviceIdentity.details()
                    .orElseThrow(() -> new NullPointerException("details cannot be null"));
            var deviceIdentityAccountSignatureKey = deviceIdentity.accountSignatureKey()
                    .orElseThrow(() -> new NullPointerException("accountSignatureKey cannot be null"));
            var deviceIdentityAccountSignature = deviceIdentity.accountSignature()
                    .orElseThrow(() -> new NullPointerException("AccountSignature cannot be null"));

            var accountSignatureHeader = E2EE_ACCOUNT_SIGNATURE_HEADER;
            if (isBizHostedDevicesEnabled()) {
                try {
                    var innerDeviceIdentity = ADVDeviceIdentitySpec.decode(deviceIdentityDetails);
                    var advEncryptionType = innerDeviceIdentity.deviceType()
                            .orElse(ADVEncryptionType.E2EE);
                    if (advEncryptionType == ADVEncryptionType.HOSTED) {
                        accountSignatureHeader = HOSTED_ACCOUNT_SIGNATURE_HEADER;
                    }
                } catch (ProtobufDeserializationException e) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "failed to decode inner device identity for {0}, falling back to e2ee header", localJid);
                    }
                }
            }

            var localIdentityKey = localIdentityKeyPair.publicKey().toEncodedPoint();
            var message = DataUtils.concatByteArrays(accountSignatureHeader, deviceIdentityDetails, localIdentityKey);
            if (!Curve25519.verifySignature(deviceIdentityAccountSignatureKey, message, deviceIdentityAccountSignature)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "adv account signature validation failed for {0}", localJid);
                throw new WhatsAppAdvValidationException.AccountSignatureFailed(localJid);
            }

            var deviceSignatureMessage = DataUtils.concatByteArrays(
                    E2EE_DEVICE_SIGNATURE_HEADER,
                    deviceIdentityDetails,
                    localIdentityKey,
                    deviceIdentityAccountSignatureKey
            );
            var deviceSignature = Curve25519.sign(localIdentityKeyPair.privateKey().toEncodedPoint(), deviceSignatureMessage);

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "validated local device identity for {0}", localJid);

            return new ADVSignedDeviceIdentityBuilder()
                    .details(deviceIdentityDetails)
                    .accountSignatureKey(deviceIdentityAccountSignatureKey)
                    .accountSignature(deviceIdentityAccountSignature)
                    .deviceSignature(deviceSignature)
                    .build();
        } catch (GeneralSecurityException exception) {
            throw new WhatsAppAdvValidationException.CryptoError(localJid, exception);
        }
    }

    /**
     * Extracts and validates a remote device identity from a prekey response.
     *
     * <p>This is called by the Signal session-creation path when the prekey bundle for a companion
     * device includes a {@code <device-identity>} child. It verifies the account signature and the
     * device signature against the claimed identity key, returning the validated identity for the
     * caller to persist. It returns empty when validation is unnecessary: when the device is the
     * primary (which never carries an ADV envelope), or when the device's stored identity already
     * matches the claimed one.
     *
     * @implNote
     * This implementation derives the account-signature header from the inner
     * {@code ADVDeviceIdentity.deviceType} and the device-signature header from the JID's hosted
     * flag; both selections are gated by the hosted-devices feature flag. The protobuf
     * {@code accountSignatureKey} takes precedence, falling back to the stored user identity key
     * when the field is missing or empty.
     *
     * @param remoteJid          the remote device JID
     * @param remoteIdentityStanza the remote device identity stanza
     * @param remoteIdentityKey  the remote device's claimed identity key (32 bytes)
     * @param isHostedFromJid    whether the remote JID indicates a hosted device
     * @return the validated signed device identity, or empty when validation is unnecessary or the
     *         device's identity is unchanged
     * @throws WhatsAppAdvValidationException if signature validation fails
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = "validateADVwithIdentityKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<ADVSignedDeviceIdentity> extractAndValidateRemoteSignedDeviceIdentity(
            Jid remoteJid,
            Stanza remoteIdentityStanza,
            byte[] remoteIdentityKey,
            boolean isHostedFromJid
    ) {
        Objects.requireNonNull(remoteJid, "remoteJid cannot be null");
        Objects.requireNonNull(remoteIdentityStanza, "remoteIdentityStanza cannot be null");
        Objects.requireNonNull(remoteIdentityKey, "remoteIdentityKey cannot be null");

        if (!requiresValidation(remoteJid)) {
            return Optional.empty();
        }

        var storedDeviceIdentityKey = findStoredDeviceIdentityKey(remoteJid);
        if (storedDeviceIdentityKey.isPresent() && Arrays.equals(storedDeviceIdentityKey.get(), remoteIdentityKey)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "remote device identity unchanged for {0}, skipping validation", remoteJid);
            return Optional.empty();
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "validating remote device identity for {0}, hosted={1}", remoteJid, isHostedFromJid);

        var storedUserIdentityKey = findStoredUserIdentityKey(remoteJid);

        var remoteIdentityBytes = remoteIdentityStanza.getChild("device-identity")
                .orElseThrow(() -> new WhatsAppAdvValidationException.MissingDeviceIdentity(remoteJid))
                .toContentBytes()
                .orElseThrow(() -> new WhatsAppAdvValidationException.EmptyDeviceIdentity(remoteJid));
        var remoteIdentity = ADVSignedDeviceIdentitySpec.decode(remoteIdentityBytes);

        var remoteIdentityDetails = remoteIdentity.details()
                .orElseThrow(() -> new NullPointerException("details cannot be null"));

        var accountSignatureHeader = E2EE_ACCOUNT_SIGNATURE_HEADER;
        if (isBizHostedDevicesEnabled()) {
            try {
                var decodedDeviceIdentity = ADVDeviceIdentitySpec.decode(remoteIdentityDetails);
                var deviceType = decodedDeviceIdentity.deviceType()
                        .orElse(ADVEncryptionType.E2EE);
                if (deviceType == ADVEncryptionType.HOSTED) {
                    accountSignatureHeader = HOSTED_ACCOUNT_SIGNATURE_HEADER;
                }
            } catch (ProtobufDeserializationException e) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "failed to decode remote device identity for {0}, falling back to e2ee header", remoteJid);
                }
            }
        }

        var deviceSignatureHeader = E2EE_DEVICE_SIGNATURE_HEADER;
        if (isBizHostedDevicesEnabled() && isHostedFromJid) {
            deviceSignatureHeader = HOSTED_DEVICE_SIGNATURE_HEADER;
        }

        var remoteIdentityAccountSignatureKey = remoteIdentity.accountSignatureKey()
                .orElse(null);
        if (remoteIdentityAccountSignatureKey == null) {
            remoteIdentityAccountSignatureKey = storedUserIdentityKey.orElse(null);
        }
        if (isBizHostedDevicesEnabled()
                && remoteIdentityAccountSignatureKey != null
                && remoteIdentityAccountSignatureKey.length == 0) {
            remoteIdentityAccountSignatureKey = storedUserIdentityKey.orElse(null);
        }

        if (remoteIdentityAccountSignatureKey == null || remoteIdentityAccountSignatureKey.length == 0) {
            return Optional.empty();
        }

        var remoteIdentityAccountSignature = remoteIdentity.accountSignature()
                .orElseThrow(() -> new NullPointerException("accountSignature cannot be null"));
        var accountMessage = DataUtils.concatByteArrays(accountSignatureHeader, remoteIdentityDetails, remoteIdentityKey);
        if (!Curve25519.verifySignature(remoteIdentityAccountSignatureKey, accountMessage, remoteIdentityAccountSignature)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "remote adv account signature validation failed for {0}", remoteJid);
            throw new WhatsAppAdvValidationException.AccountSignatureFailed(remoteJid);
        }

        var remoteIdentityDeviceSignature = remoteIdentity.deviceSignature()
                .orElseThrow(() -> new NullPointerException("deviceSignature cannot be null"));
        var deviceMessage = DataUtils.concatByteArrays(deviceSignatureHeader, remoteIdentityDetails, remoteIdentityKey, remoteIdentityAccountSignatureKey);
        if (!Curve25519.verifySignature(remoteIdentityKey, deviceMessage, remoteIdentityDeviceSignature)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "remote adv device signature validation failed for {0}", remoteJid);
            throw new WhatsAppAdvValidationException.DeviceSignatureFailed(remoteJid);
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "validated remote device identity for {0}", remoteJid);

        return Optional.of(remoteIdentity);
    }

    /**
     * Decodes and verifies a signed key-index list against the user's local primary identity key.
     *
     * <p>This is the standard E2EE path used for normal accounts. The verification key comes from
     * the local Signal store (device 0 of {@code userJid}), and any embedded
     * {@code accountSignatureKey} on the wire is ignored; the server commonly omits that field
     * because peers verify against the identity they already trust from their Signal session with
     * the primary device. The returned result therefore carries an empty
     * {@code accountSignatureKey}.
     *
     * @param userJid             the user JID whose primary identity is the verification key
     * @param signedKeyIndexBytes the raw signed key-index list bytes
     * @return the validated key-index list data, or empty when no local primary identity is known or
     *         validation fails
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "decodeSignedKeyIndexBytes",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<ValidatedKeyIndexListResult> decodeSignedKeyIndexBytes(Jid userJid, byte[] signedKeyIndexBytes) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        Objects.requireNonNull(signedKeyIndexBytes, "signedKeyIndexBytes cannot be null");

        var localPrimaryIdentity = findStoredUserIdentityKey(userJid).orElse(null);
        if (localPrimaryIdentity == null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "no local primary identity for {0}, skipping key-index list validation", userJid);
            }
            return Optional.empty();
        }

        return decodeAndVerifySignedKeyIndexList(signedKeyIndexBytes, localPrimaryIdentity, null);
    }

    /**
     * Decodes and verifies a signed key-index list against the embedded account-signature key.
     *
     * <p>This is the hosted business-coexistence path: the verification key must be present in the
     * outer protobuf {@code accountSignatureKey} field, and the result is empty when it is missing
     * or empty. It is used when peers may not yet have a Signal session with the primary phone (and
     * therefore cannot resolve a stored identity) but still need to trust a freshly-received
     * key-index list. The returned result carries the embedded key so callers can persist it as the
     * primary identity.
     *
     * @param signedKeyIndexBytes the raw signed key-index list bytes
     * @return the validated key-index list data, or empty when validation fails
     * @throws NullPointerException if {@code signedKeyIndexBytes} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<ValidatedKeyIndexListResult> verifySKeyIndexWithAccSigKey(byte[] signedKeyIndexBytes) {
        Objects.requireNonNull(signedKeyIndexBytes, "signedKeyIndexBytes cannot be null");

        try {
            var signedKeyIndexList = ADVSignedKeyIndexListSpec.decode(signedKeyIndexBytes);
            var accountSignatureKey = signedKeyIndexList.accountSignatureKey()
                    .filter(key -> key.length > 0)
                    .orElse(null);
            if (accountSignatureKey == null) {
                return Optional.empty();
            }
            return verifyAndBuildResult(signedKeyIndexList, accountSignatureKey, accountSignatureKey);
        } catch (ProtobufDeserializationException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to decode signed key-index list envelope", e);
            return Optional.empty();
        }
    }

    /**
     * Decodes the outer {@code ADVSignedKeyIndexList} envelope and dispatches to
     * {@link #verifyAndBuildResult} for signature verification and inner decoding.
     *
     * <p>This is the shared shim between the standard E2EE and hosted-business entry points; it is
     * factored out so the entry points differ only in how they choose the verification key.
     *
     * @param signedKeyIndexBytes       the raw signed key-index list bytes
     * @param verificationKey           the public key used to verify the signature
     * @param resultAccountSignatureKey the key embedded in the returned result, or {@code null} for
     *                                  the standard path that has no embedded key to forward
     * @return the validated key-index list data, or empty when validation fails
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "decodeSignedKeyIndexBytes",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<ValidatedKeyIndexListResult> decodeAndVerifySignedKeyIndexList(
            byte[] signedKeyIndexBytes,
            byte[] verificationKey,
            byte[] resultAccountSignatureKey
    ) {
        try {
            var signedKeyIndexList = ADVSignedKeyIndexListSpec.decode(signedKeyIndexBytes);
            return verifyAndBuildResult(signedKeyIndexList, verificationKey, resultAccountSignatureKey);
        } catch (ProtobufDeserializationException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to decode signed key-index list envelope", e);
            return Optional.empty();
        }
    }

    /**
     * Verifies the account signature on a parsed {@code ADVSignedKeyIndexList}, decodes the inner
     * {@code ADVKeyIndexList}, and packages the result.
     *
     * <p>This internal helper is shared by both entry points and is never called from outside the
     * validator. It returns empty when the envelope lacks details or a signature, when the signature
     * fails to verify, or when the inner list lacks a raw id or timestamp.
     *
     * @param signedKeyIndexList        the parsed outer envelope
     * @param verificationKey           the public key used to verify the signature
     * @param resultAccountSignatureKey the key embedded in the returned result, or {@code null} for
     *                                  the standard path
     * @return the validated key-index list data, or empty when any check fails
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "decodeSignedKeyIndexBytes",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<ValidatedKeyIndexListResult> verifyAndBuildResult(
            ADVSignedKeyIndexList signedKeyIndexList,
            byte[] verificationKey,
            byte[] resultAccountSignatureKey
    ) {
        var details = signedKeyIndexList.details().orElse(null);
        if (details == null) {
            return Optional.empty();
        }

        var accountSignature = signedKeyIndexList.accountSignature()
                .filter(sig -> sig.length > 0)
                .orElse(null);
        if (accountSignature == null) {
            return Optional.empty();
        }

        var message = DataUtils.concatByteArrays(KEY_INDEX_LIST_SIGNATURE_HEADER, details);
        if (!Curve25519.verifySignature(verificationKey, message, accountSignature)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "key-index list account signature verification failed");
            return Optional.empty();
        }

        try {
            var keyIndexList = ADVKeyIndexListSpec.decode(details);
            var keyIndexListRawId = keyIndexList.rawId();
            var keyIndexListTimestamp = keyIndexList.timestamp();
            if (keyIndexListRawId.isEmpty() || keyIndexListTimestamp.isEmpty()) {
                return Optional.empty();
            }

            var keyIndexListValidIndexesSet = new LinkedHashSet<>(keyIndexList.validIndexes());
            var keyIndexListCurrentIndex = keyIndexList.currentIndex().orElse(0);
            var keyIndexListAccountType = keyIndexList.accountType().orElse(ADVEncryptionType.E2EE);

            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "verified key-index list rawId={0}, currentIndex={1}, validIndexes={2}, accountType={3}",
                        keyIndexListRawId.getAsInt(), keyIndexListCurrentIndex, keyIndexListValidIndexesSet.size(), keyIndexListAccountType);
            }

            return Optional.of(new ValidatedKeyIndexListResult(
                    keyIndexListRawId.getAsInt(),
                    keyIndexListTimestamp.get(),
                    keyIndexListValidIndexesSet,
                    keyIndexListCurrentIndex,
                    keyIndexListAccountType,
                    resultAccountSignatureKey
            ));
        } catch (ProtobufDeserializationException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to decode inner key-index list", e);
            return Optional.empty();
        }
    }

    /**
     * Returns the stored identity key for the exact device.
     *
     * <p>This lookup is used by
     * {@link #extractAndValidateRemoteSignedDeviceIdentity(Jid, Stanza, byte[], boolean)} to
     * short-circuit re-validation when the device's stored identity already matches the claimed one.
     *
     * @param deviceJid the device JID, including the device number
     * @return the identity key bytes, or empty when no key is stored
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = "validateADVwithIdentityKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<byte[]> findStoredDeviceIdentityKey(Jid deviceJid) {
        if (deviceJid == null) {
            return Optional.empty();
        }
        var address = new SignalProtocolAddress(deviceJid.user(), deviceJid.device());
        return store.signalStore().findIdentityByAddress(address)
                .map(SignalIdentityKey::toEncodedPoint);
    }

    /**
     * Returns the stored identity key for the user's primary device.
     *
     * <p>This key serves as the verification key for the standard signed-key-index-list path (where
     * the wire does not carry an embedded account signature key), and as a fallback for
     * {@code accountSignatureKey} in
     * {@link #extractAndValidateRemoteSignedDeviceIdentity(Jid, Stanza, byte[], boolean)} when the
     * protobuf field is missing or empty. The device number on {@code jid} is ignored.
     *
     * @param jid the JID; the device number is ignored
     * @return the identity key bytes, or empty when no key is stored
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = "validateADVwithIdentityKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<byte[]> findStoredUserIdentityKey(Jid jid) {
        if (jid == null) {
            return Optional.empty();
        }
        var address = new SignalProtocolAddress(jid.user(), 0);
        return store.signalStore().findIdentityByAddress(address)
                .map(SignalIdentityKey::toEncodedPoint);
    }

    /**
     * Returns whether the JID points to a device that requires ADV validation.
     *
     * <p>Primary devices ({@code device == }{@link DeviceConstants#PRIMARY_DEVICE_ID}) never carry
     * an ADV envelope and skip validation entirely; only companion devices have a signed device
     * identity to verify.
     *
     * @param jid the JID to test
     * @return {@code true} for companion devices
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvSignatureApi",
            exports = "validateADVwithIdentityKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean requiresValidation(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        return jid.device() != DeviceConstants.PRIMARY_DEVICE_ID;
    }
}
