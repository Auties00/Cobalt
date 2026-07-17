package com.github.auties00.cobalt.wire.linked.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Map;
import java.util.Optional;

/**
 * Result of registering a Brazilian-payments custom payment method
 * (cash on delivery or PIX key) on a WhatsApp Business account.
 *
 * <p>The Brazilian payments surface lets a small business register
 * non-card payment methods so they appear as checkout options inside a
 * commerce conversation. The relay assigns each registration an opaque
 * {@linkplain #credentialId() credential identifier} the client uses
 * to refer back to the method, and echoes the canonical method type
 * plus the optional flow ({@code "p2p"} for peer-to-peer or
 * {@code "p2m"} for peer-to-merchant), the BR country marker, the
 * creation timestamp the relay observed, the per-flow eligibility
 * flags and the metadata key-value pairs the client originally
 * supplied.
 */
@ProtobufMessage(name = "BrazilCustomPaymentMethod")
public final class BrazilCustomPaymentMethod {
    /**
     * Relay-assigned credential identifier for this registration.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String credentialId;

    /**
     * Echoed custom-payment-method type. Either {@code "pay_on_delivery"}
     * or {@code "pix_key"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String customPaymentMethodType;

    /**
     * Optional echoed country attribute (always {@code "BR"} when
     * present).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String country;

    /**
     * Optional creation timestamp echoed by the relay.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String created;

    /**
     * Optional flow attribute. Either {@code "p2p"} or {@code "p2m"}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String flow;

    /**
     * Optional peer-to-peer eligibility flag ({@code "0"} or
     * {@code "1"}).
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String p2pEligible;

    /**
     * Optional peer-to-merchant eligibility flag ({@code "0"} or
     * {@code "1"}).
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String p2mEligible;

    /**
     * Echoed metadata pairs the client supplied at registration time.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    Map<String, String> metadata;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param credentialId            the relay-assigned credential id
     * @param customPaymentMethodType the echoed method type
     * @param country                 the optional echoed country marker
     * @param created                 the optional echoed creation
     *                                timestamp
     * @param flow                    the optional flow attribute
     * @param p2pEligible             the optional P2P-eligible flag
     * @param p2mEligible             the optional P2M-eligible flag
     * @param metadata                the echoed metadata pairs
     */
    BrazilCustomPaymentMethod(String credentialId, String customPaymentMethodType,
                              String country, String created, String flow,
                              String p2pEligible, String p2mEligible,
                              Map<String, String> metadata) {
        this.credentialId = credentialId;
        this.customPaymentMethodType = customPaymentMethodType;
        this.country = country;
        this.created = created;
        this.flow = flow;
        this.p2pEligible = p2pEligible;
        this.p2mEligible = p2mEligible;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Returns the relay-assigned credential identifier.
     *
     * @return the identifier; never {@code null} for a parsed
     *         registration
     */
    public String credentialId() {
        return credentialId;
    }

    /**
     * Returns the echoed custom-payment-method type.
     *
     * @return the type; never {@code null} for a parsed registration
     */
    public String customPaymentMethodType() {
        return customPaymentMethodType;
    }

    /**
     * Returns the optional echoed country marker.
     *
     * @return an {@link Optional} carrying the marker, or empty when
     *         the relay omitted it
     */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }

    /**
     * Returns the optional echoed creation timestamp.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when
     *         the relay omitted it
     */
    public Optional<String> created() {
        return Optional.ofNullable(created);
    }

    /**
     * Returns the optional flow attribute.
     *
     * @return an {@link Optional} carrying the flow, or empty when the
     *         relay omitted it
     */
    public Optional<String> flow() {
        return Optional.ofNullable(flow);
    }

    /**
     * Returns the optional peer-to-peer eligibility flag.
     *
     * @return an {@link Optional} carrying the flag, or empty when the
     *         relay omitted it
     */
    public Optional<String> p2pEligible() {
        return Optional.ofNullable(p2pEligible);
    }

    /**
     * Returns the optional peer-to-merchant eligibility flag.
     *
     * @return an {@link Optional} carrying the flag, or empty when the
     *         relay omitted it
     */
    public Optional<String> p2mEligible() {
        return Optional.ofNullable(p2mEligible);
    }

    /**
     * Returns the echoed metadata pairs.
     *
     * @return an unmodifiable map; never {@code null}
     */
    public Map<String, String> metadata() {
        return metadata;
    }
}
