package com.github.auties00.cobalt.device.adv;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * Holds the decoded payload of a signed key-index list after cryptographic validation.
 *
 * <p>An instance is produced by {@link DeviceADVValidator#decodeSignedKeyIndexBytes} and
 * {@link DeviceADVValidator#verifySKeyIndexWithAccSigKey} once the account signature has been
 * verified and the inner protobuf decoded. It carries the fields a downstream device-list applier
 * needs to reason about companion-device validity without re-decoding the protobuf or re-verifying
 * the signature: the {@link #validIndexes() validIndexes} set, the {@link #currentIndex()
 * currentIndex} counter, the {@link #accountType() accountType} flag distinguishing standard
 * end-to-end encrypted accounts from hosted business-coexistence accounts, and the
 * {@link #timestamp() timestamp} / {@link #rawId() rawId} pair that detects identity rotation.
 *
 * @see DeviceADVValidator#decodeSignedKeyIndexBytes(com.github.auties00.cobalt.wire.core.jid.Jid, byte[])
 * @see DeviceADVValidator#verifySKeyIndexWithAccSigKey(byte[])
 */
@WhatsAppWebModule(moduleName = "WAWebHandleAdvDeviceNotificationUtils")
public final class ValidatedKeyIndexListResult {
    /**
     * Holds the raw identity id lifted from the inner {@code ADVKeyIndexList.rawId} field.
     */
    private final long rawId;

    /**
     * Holds the snapshot timestamp lifted from the inner {@code ADVKeyIndexList.timestamp} field.
     */
    private final Instant timestamp;

    /**
     * Holds the set of key indexes the account currently considers valid, in encounter order.
     */
    private final SequencedSet<Integer> validIndexes;

    /**
     * Holds the current key index counter.
     */
    private final int currentIndex;

    /**
     * Holds the account encryption type, defaulting to {@link ADVEncryptionType#E2EE} when the
     * inner protobuf omits it.
     */
    private final ADVEncryptionType accountType;

    /**
     * Holds the 32-byte account signature key lifted from the outer signed wrapper.
     *
     * <p>The value is {@code null} when the standard end-to-end encrypted path verified against the
     * locally-stored primary identity and therefore has no embedded key to forward; it is non-null
     * only on the hosted business-coexistence path.
     */
    private final byte[] accountSignatureKey;

    /**
     * Constructs a validated key-index list result from the already-verified fields.
     *
     * <p>The instance is built by {@link DeviceADVValidator}'s verify-and-build helper after the
     * signature has been verified and the inner protobuf decoded.
     *
     * @param rawId               the raw identity id
     * @param timestamp           the key-index list timestamp
     * @param validIndexes        the set of valid key indexes
     * @param currentIndex        the current key index counter
     * @param accountType         the account encryption type
     * @param accountSignatureKey the 32-byte account signature key, or {@code null} when the
     *                            standard end-to-end encrypted path verified against the
     *                            locally-stored primary identity
     * @throws NullPointerException if any of {@code timestamp}, {@code validIndexes} or
     *                              {@code accountType} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public ValidatedKeyIndexListResult(
            long rawId,
            Instant timestamp,
            SequencedSet<Integer> validIndexes,
            int currentIndex,
            ADVEncryptionType accountType,
            byte[] accountSignatureKey
    ) {
        this.rawId = rawId;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.validIndexes = Objects.requireNonNull(validIndexes, "validIndexes cannot be null");
        this.currentIndex = currentIndex;
        this.accountType = Objects.requireNonNull(accountType, "accountType cannot be null");
        this.accountSignatureKey = accountSignatureKey;
    }

    /**
     * Returns the raw identity id from the key-index list.
     *
     * <p>The device-list applier compares this value against the cached device list's {@code rawId};
     * a mismatch is interpreted as an identity rotation and clears the cached record.
     *
     * @return the raw identity id
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long rawId() {
        return rawId;
    }

    /**
     * Returns the snapshot timestamp from the key-index list.
     *
     * <p>The device-list applier compares this timestamp against the cached snapshot timestamp and
     * drops the slot when it goes backwards; the value also feeds the expected-timestamp tracking
     * that gates the recurring ADV check.
     *
     * @return the timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the set of key indexes the account currently considers valid.
     *
     * <p>The device-list applier filters the cached device list down to companions whose key index
     * still appears in this set, evicting companions whose index is no longer valid.
     *
     * @return an unmodifiable view of the valid indexes, in encounter order
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public SequencedSet<Integer> validIndexes() {
        return Collections.unmodifiableSequencedSet(validIndexes);
    }

    /**
     * Returns the current key index counter.
     *
     * <p>The device-list applier uses this counter as the cutoff for accepting cached companion
     * entries: an entry whose own key index exceeds the counter is retained because it represents a
     * rotation not yet reflected in the signed list.
     *
     * @return the current index
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int currentIndex() {
        return currentIndex;
    }

    /**
     * Returns the account encryption type, either {@link ADVEncryptionType#E2EE} or
     * {@link ADVEncryptionType#HOSTED}.
     *
     * <p>The value drives the hosted business-coexistence branch in the device-list applier: a
     * {@link ADVEncryptionType#HOSTED} type marks the user as a hosted business account and triggers
     * cache invalidation when the cached and incoming types disagree.
     *
     * @return the account type
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public ADVEncryptionType accountType() {
        return accountType;
    }

    /**
     * Returns the 32-byte account signature key from the outer signed wrapper.
     *
     * <p>The key is present only on the hosted business-coexistence path
     * ({@link DeviceADVValidator#verifySKeyIndexWithAccSigKey(byte[])}); the standard end-to-end
     * encrypted path verifies against the locally-stored primary identity and leaves it empty. When
     * present, callers persist the key as the primary identity for the user.
     *
     * @return the account signature key, or empty when not provided
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
            exports = "verifySKeyIndexWithAccSigKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<byte[]> accountSignatureKey() {
        return Optional.ofNullable(accountSignatureKey);
    }
}
