package com.github.auties00.cobalt.device.icdc;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Holds the Identity Change Detection Consistency data derived from a single
 * user's device list.
 *
 * <p>An instance is produced by
 * {@link IcdcComputer#compute(com.github.auties00.cobalt.wire.core.jid.Jid)} and
 * consumed by the outbound message encoder to populate the
 * {@code deviceListMetadata} field of {@code messageContextInfo} on every
 * outgoing multi-device message. It carries the four values the recipient
 * compares against its own view of the participant's device list: the truncated
 * SHA-256 hash of companion identity keys ({@link #keyHash()}), the device-list
 * snapshot timestamp ({@link #timestamp()}), the indexes of the devices included
 * in the hash ({@link #keyIndexes()}, omitted when every device was included),
 * and the hosted account type when applicable ({@link #accountType()}).
 */
@WhatsAppWebModule(moduleName = "WAWebIdentityIcdcApi")
public final class IcdcResult {

    /**
     * Holds the truncated SHA-256 hash of the sorted, concatenated identity
     * keys, or {@code null} when the user has no companion devices.
     */
    private final byte[] keyHash;

    /**
     * Holds the device-list snapshot timestamp, or {@code null} when the user
     * has no companion devices and the timestamp is older than
     * {@link IcdcComputer}'s recent threshold.
     */
    private final Instant timestamp;

    /**
     * Holds the indexes of devices whose identity keys were successfully
     * retrieved, or {@code null} when every device was included.
     */
    private final List<Integer> keyIndexes;

    /**
     * Holds the hosted account encryption type, or {@code null} for non-hosted
     * accounts.
     */
    private final ADVEncryptionType accountType;

    /**
     * Constructs a new ICDC result from the supplied components.
     *
     * <p>This constructor is package-private and is invoked only by
     * {@link IcdcComputer#computeFromDeviceList} and, on the test classpath, by
     * {@code TestIcdcResults}.
     *
     * @param keyHash     the truncated identity key hash, or {@code null}
     * @param timestamp   the device-list snapshot timestamp, or {@code null}
     * @param keyIndexes  the indexes of included devices, or {@code null}
     * @param accountType the hosted account encryption type, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi",
            exports = "getICDCMetaFromDeviceRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    IcdcResult(
            byte[] keyHash,
            Instant timestamp,
            List<Integer> keyIndexes,
            ADVEncryptionType accountType
    ) {
        this.keyHash = keyHash;
        this.timestamp = timestamp;
        this.keyIndexes = keyIndexes;
        this.accountType = accountType;
    }

    /**
     * Returns the truncated SHA-256 hash of the participant's identity keys.
     *
     * <p>The result is empty when the user has no companion devices, since
     * there are no identity keys to hash. It is present otherwise, even when
     * only a subset of identity keys could be resolved locally; in that case
     * {@link #keyIndexes()} signals which subset contributed to the hash.
     *
     * @return the key hash, or empty when no companion devices were found
     */
    @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi",
            exports = "getICDCMetaFromDeviceRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<byte[]> keyHash() {
        return Optional.ofNullable(keyHash);
    }

    /**
     * Returns the device-list snapshot timestamp.
     *
     * <p>The result is present when the user has companion devices, or when the
     * timestamp falls within {@link IcdcComputer}'s 720-hour recent window. It
     * is empty for a primary-only list whose snapshot timestamp is stale.
     *
     * @return the timestamp, or empty when stale and no companion devices
     */
    @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi",
            exports = "getICDCMetaFromDeviceRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the indexes of devices whose identity keys were included in the
     * hash.
     *
     * <p>The list is empty when every device in the device list was included,
     * so the indexes do not need to be transmitted. It is populated when some
     * companion devices had no locally-cached identity key and were omitted
     * from the hash.
     *
     * @return an unmodifiable list of key indexes, or an empty list when every
     *         device was included
     */
    @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi",
            exports = "getICDCMetaFromDeviceRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public List<Integer> keyIndexes() {
        return keyIndexes != null
                ? Collections.unmodifiableList(keyIndexes)
                : List.of();
    }

    /**
     * Returns the hosted account encryption type for this participant.
     *
     * <p>The result is empty for non-hosted accounts and whenever the
     * {@code adv_accept_hosted_devices} AB prop is off.
     *
     * @implNote This implementation collapses WhatsApp Web's
     * {@code senderAccountType} and {@code receiverAccountType} pair onto a
     * single field; the caller decides which role this result represents based
     * on which side of the message it is encoding for.
     *
     * @return the account type, or empty when the account is not hosted
     */
    @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi",
            exports = "getICDCMetaFromDeviceRecord",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<ADVEncryptionType> accountType() {
        return Optional.ofNullable(accountType);
    }

    /**
     * Returns a diagnostic string representation suitable for logs.
     *
     * <p>The representation lists the timestamp, key indexes, and account type
     * directly, and summarises the hash by its byte length rather than its
     * contents.
     *
     * @implNote This implementation reports only the hash length, never the
     * hash bytes, to keep log lines short and to avoid leaking the bytes to any
     * log sink that might persist them.
     *
     * @return the diagnostic string
     */
    @Override
    public String toString() {
        return "IcdcResult[" +
                "keyHash=" + (keyHash != null ? keyHash.length + " bytes" : "null") +
                ", timestamp=" + timestamp +
                ", keyIndexes=" + keyIndexes +
                ", accountType=" + accountType +
                ']';
    }
}
