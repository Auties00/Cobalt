package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.exception.ADVValidationException;
import com.github.auties00.cobalt.model.auth.*;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Validator for ADV (Authenticated Device Verification) signatures.
 * ADV is used to validate companion device identities to prevent MITM attacks.
 * <p>
 * For companion devices (device != 0), the prekey response must contain a device-identity
 * node with a SignedDeviceIdentity protobuf. The signature chain is validated to ensure
 * the identity key belongs to a legitimate device linked to the account.
 */
public final class ADVValidator {
    /**
     * Header bytes prepended to messages before account signature verification.
     * Used for verifying that the account signature is valid.
     */
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};

    /**
     * Header bytes prepended to messages before device signature verification/creation.
     * Used for both verifying remote device signatures and creating local device signatures.
     */
    private static final byte[] DEVICE_SIGNATURE_HEADER = {6, 1};

    private ADVValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Extracts and validates the device identity from a prekey response node.
     * For companion devices, this validates the ADV signature chain.
     *
     * @param jid               the device JID
     * @param userNode          the prekey response node
     * @param deviceIdentityKey the device's claimed identity key (32 bytes)
     * @throws ADVValidationException if validation is required but fails
     */
    public static void validatePreKeyResponse(Jid jid, Node userNode, byte[] deviceIdentityKey) {
        Objects.requireNonNull(jid, "jid cannot be null");
        Objects.requireNonNull(deviceIdentityKey, "Identity key required for ADV validation");

        if (!requiresValidation(jid)) {
            return;
        }

        var deviceIdentityNode = userNode.getChild("device-identity");
        if (deviceIdentityNode.isEmpty()) {
            throw new ADVValidationException(jid, ADVValidationException.Type.MISSING_DEVICE_IDENTITY);
        }

        var identityBytes = deviceIdentityNode.flatMap(Node::toContentBytes)
                .orElseThrow(() -> new ADVValidationException(jid, ADVValidationException.Type.EMPTY_DEVICE_IDENTITY));
        var signedIdentity = SignedDeviceIdentitySpec.decode(identityBytes);
        validateSignedDeviceIdentity(jid, signedIdentity, deviceIdentityKey);
    }

    /**
     * Validates a SignedDeviceIdentity against the provided identity key.
     *
     * @param jid               the device JID
     * @param identity          the signed device identity
     * @param deviceIdentityKey the device's claimed identity key (32 bytes)
     * @throws ADVValidationException if validation fails
     */
    public static void validateSignedDeviceIdentity(Jid jid, SignedDeviceIdentity identity, byte[] deviceIdentityKey) {
        Objects.requireNonNull(identity, "SignedDeviceIdentity required");
        Objects.requireNonNull(identity.details(), "Identity details required");
        Objects.requireNonNull(identity.accountSignatureKey(), "Account signature key required");
        Objects.requireNonNull(identity.accountSignature(), "Account signature required");
        Objects.requireNonNull(identity.deviceSignature(), "Device signature required");
        Objects.requireNonNull(deviceIdentityKey, "Device identity key required");

        var accountMessage = SecureBytes.concat(ACCOUNT_SIGNATURE_HEADER, identity.details());
        if (!Curve25519.verifySignature(identity.accountSignatureKey(), accountMessage, identity.accountSignature())) {
            throw new ADVValidationException(jid, ADVValidationException.Type.ACCOUNT_SIGNATURE_FAILED);
        }

        var deviceMessage = SecureBytes.concat(DEVICE_SIGNATURE_HEADER, identity.details(), deviceIdentityKey);
        if (!Curve25519.verifySignature(identity.accountSignatureKey(), deviceMessage, identity.deviceSignature())) {
            throw new ADVValidationException(jid, ADVValidationException.Type.DEVICE_SIGNATURE_FAILED);
        }
    }

    /**
     * Verifies the account signature during device pairing.
     * This is used when linking a companion device to verify the primary device's identity.
     *
     * @param identity           the signed device identity from the primary device
     * @param localIdentityKey   the local device's identity public key (32 bytes)
     * @return true if the account signature is valid
     */
    public static boolean verifyAccountSignatureForPairing(SignedDeviceIdentity identity, byte[] localIdentityKey) {
        Objects.requireNonNull(identity, "SignedDeviceIdentity required");
        Objects.requireNonNull(identity.details(), "Identity details required");
        Objects.requireNonNull(identity.accountSignatureKey(), "Account signature key required");
        Objects.requireNonNull(identity.accountSignature(), "Account signature required");
        Objects.requireNonNull(localIdentityKey, "Local identity key required");

        var message = SecureBytes.concat(
                ACCOUNT_SIGNATURE_HEADER,
                identity.details(),
                localIdentityKey
        );

        return Curve25519.verifySignature(
                identity.accountSignatureKey(),
                message,
                identity.accountSignature()
        );
    }

    /**
     * Creates a device signature for completing the pairing process.
     * This is used by companion devices to sign their identity with the account's signature key.
     *
     * @param identity           the signed device identity from the primary device
     * @param localIdentityKey   the local device's identity public key (32 bytes)
     * @param localPrivateKey    the local device's identity private key (32 bytes)
     * @return the device signature bytes
     */
    public static byte[] createDeviceSignature(SignedDeviceIdentity identity, byte[] localIdentityKey, byte[] localPrivateKey) {
        Objects.requireNonNull(identity, "SignedDeviceIdentity required");
        Objects.requireNonNull(identity.details(), "Identity details required");
        Objects.requireNonNull(identity.accountSignatureKey(), "Account signature key required");
        Objects.requireNonNull(localIdentityKey, "Local identity key required");
        Objects.requireNonNull(localPrivateKey, "Local private key required");

        var message = SecureBytes.concat(
                DEVICE_SIGNATURE_HEADER,
                identity.details(),
                localIdentityKey,
                identity.accountSignatureKey()
        );

        return Curve25519.sign(localPrivateKey, message);
    }

    /**
     * Creates a complete SignedDeviceIdentity with a device signature for the pairing process.
     * This combines the account's identity with the local device's signature.
     *
     * @param identity           the signed device identity from the primary device
     * @param localIdentityKey   the local device's identity public key (32 bytes)
     * @param localPrivateKey    the local device's identity private key (32 bytes)
     * @return a new SignedDeviceIdentity with the device signature
     */
    public static SignedDeviceIdentity createSignedIdentityForPairing(SignedDeviceIdentity identity, byte[] localIdentityKey, byte[] localPrivateKey) {
        var deviceSignature = createDeviceSignature(identity, localIdentityKey, localPrivateKey);
        return new SignedDeviceIdentityBuilder()
                .details(identity.details())
                .accountSignatureKey(identity.accountSignatureKey())
                .accountSignature(identity.accountSignature())
                .deviceSignature(deviceSignature)
                .build();
    }

    /**
     * Extracts and validates the device identity from an incoming node given the companion key pair.
     *
     * @param jid the JID of the device being validated
     * @param companionKeyPair the non-null companion key pair
     * @param node the non-null node that contains the device identity
     * @return the validated signed device identity
     * @throws ADVValidationException if the device identity is missing, empty, or HMAC validation fails
     */
    public static SignedDeviceIdentity extractAndValidateDeviceIdentity(Jid jid, SignalIdentityKeyPair companionKeyPair, Node node) {
        Objects.requireNonNull(companionKeyPair, "companionKeyPair cannot be null");
        Objects.requireNonNull(node, "node cannot be null");

        var deviceIdentityBytes = node.getChild("device-identity")
                .flatMap(Node::toContentBytes)
                .orElseThrow(() -> new ADVValidationException(jid, ADVValidationException.Type.MISSING_DEVICE_IDENTITY));

        var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentityBytes);
        var advSign = getAdvSign(jid, companionKeyPair, advIdentity);
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            throw new ADVValidationException(jid, ADVValidationException.Type.HMAC_VALIDATION_FAILED);
        }

        return SignedDeviceIdentitySpec.decode(advIdentity.details());
    }

    private static byte[] getAdvSign(Jid jid, SignalIdentityKeyPair companionKeyPair, SignedDeviceIdentityHMAC advIdentity) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            var companionKey = companionKeyPair.publicKey().toEncodedPoint();
            var companionSecretKey = new SecretKeySpec(companionKey, "HmacSHA256");
            mac.init(companionSecretKey);
            return mac.doFinal(advIdentity.details());
        } catch (GeneralSecurityException exception) {
            throw new ADVValidationException(jid, ADVValidationException.Type.CRYPTO_ERROR, exception);
        }
    }

    /**
     * Checks if a device requires ADV validation.
     * Companion devices (device ID != 0) require ADV validation.
     *
     * @param jid the device JID
     * @return true if ADV validation is required
     */
    private static boolean requiresValidation(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        return jid.hasDevice();
    }
}
