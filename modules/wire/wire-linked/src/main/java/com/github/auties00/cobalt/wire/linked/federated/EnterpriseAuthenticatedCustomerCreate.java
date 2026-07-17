package com.github.auties00.cobalt.wire.linked.federated;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;

/**
 * Input model for
 * {@code LinkedWhatsAppClient.createEnterpriseAuthenticatedCustomer}. Carries
 * every parameter required by the
 * {@code WASmaxWaffleGenerateWAEntACUserRPC} mutation: the RSA-encrypted
 * onboarding payload, the issuance timestamp, and the four disclosure
 * identifiers that identify the consent the customer accepted.
 *
 * <p>Every field is required — the relay rejects the mutation if any
 * disclosure attribute is missing.
 */
@ProtobufMessage
public final class EnterpriseAuthenticatedCustomerCreate {
    /**
     * RSA-encrypted onboarding payload carrying the customer's identity
     * material.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final FederatedRsaEncryption encryption;

    /**
     * Issuance timestamp routed onto the wire as {@code unix_seconds}
     * via {@link InstantSecondsMixin}. Defaults to {@link Instant#now()}
     * when omitted by the caller.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    final Instant timestamp;

    /**
     * Numeric identifier of the disclosure the customer accepted.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int disclosureId;

    /**
     * Version string of the accepted disclosure.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String disclosureVersion;

    /**
     * Language code of the accepted disclosure (e.g. {@code "en"}).
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String disclosureLg;

    /**
     * Locale/country code of the accepted disclosure (e.g. {@code "US"}).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String disclosureLc;

    /**
     * Constructs a new {@code EnterpriseAuthenticatedCustomerCreate}.
     *
     * @param encryption        the RSA-encrypted onboarding payload
     * @param timestamp         the issuance timestamp, or {@code null} to
     *                          default to {@link Instant#now()}
     * @param disclosureId      the disclosure identifier
     * @param disclosureVersion the disclosure version string
     * @param disclosureLg      the disclosure language code
     * @param disclosureLc      the disclosure locale/country code
     * @throws NullPointerException if any non-{@code timestamp} reference
     *                              argument is {@code null}
     */
    EnterpriseAuthenticatedCustomerCreate(FederatedRsaEncryption encryption, Instant timestamp,
                                          int disclosureId, String disclosureVersion,
                                          String disclosureLg, String disclosureLc) {
        this.encryption = Objects.requireNonNull(encryption, "encryption cannot be null");
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
        this.disclosureId = disclosureId;
        this.disclosureVersion = Objects.requireNonNull(disclosureVersion, "disclosureVersion cannot be null");
        this.disclosureLg = Objects.requireNonNull(disclosureLg, "disclosureLg cannot be null");
        this.disclosureLc = Objects.requireNonNull(disclosureLc, "disclosureLc cannot be null");
    }

    /**
     * Returns the RSA-encrypted onboarding payload.
     *
     * @return the encryption payload, never {@code null}
     */
    public FederatedRsaEncryption encryption() {
        return encryption;
    }

    /**
     * Returns the issuance timestamp. Defaults to {@link Instant#now()}
     * when the caller did not supply one.
     *
     * @return the timestamp, never {@code null}
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the disclosure identifier.
     *
     * @return the disclosure id
     */
    public int disclosureId() {
        return disclosureId;
    }

    /**
     * Returns the disclosure version string.
     *
     * @return the disclosure version, never {@code null}
     */
    public String disclosureVersion() {
        return disclosureVersion;
    }

    /**
     * Returns the disclosure language code.
     *
     * @return the disclosure language code, never {@code null}
     */
    public String disclosureLg() {
        return disclosureLg;
    }

    /**
     * Returns the disclosure locale/country code.
     *
     * @return the disclosure locale code, never {@code null}
     */
    public String disclosureLc() {
        return disclosureLc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EnterpriseAuthenticatedCustomerCreate) obj;
        return Objects.equals(encryption, that.encryption) &&
                Objects.equals(timestamp, that.timestamp) &&
                disclosureId == that.disclosureId &&
                Objects.equals(disclosureVersion, that.disclosureVersion) &&
                Objects.equals(disclosureLg, that.disclosureLg) &&
                Objects.equals(disclosureLc, that.disclosureLc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryption, timestamp, disclosureId, disclosureVersion, disclosureLg, disclosureLc);
    }

    @Override
    public String toString() {
        return "EnterpriseAuthenticatedCustomerCreate[" +
                "encryption=" + encryption + ", " +
                "timestamp=" + timestamp + ", " +
                "disclosureId=" + disclosureId + ", " +
                "disclosureVersion=" + disclosureVersion + ", " +
                "disclosureLg=" + disclosureLg + ", " +
                "disclosureLc=" + disclosureLc + ']';
    }
}
