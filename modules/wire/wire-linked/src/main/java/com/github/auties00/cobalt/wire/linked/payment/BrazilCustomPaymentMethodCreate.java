package com.github.auties00.cobalt.wire.linked.payment;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.createBrazilCustomPaymentMethod} —
 * registers a Brazil-specific custom payment method against the user's
 * account.
 *
 * <p>All fields are optional — the relay accepts any non-{@code null}
 * subset and uses the missing slots as "leave unchanged" hints. The
 * caller is expected to set whichever combination the relay's BRA
 * payments contract requires.
 *
 * <p>{@link #metadata} is exposed as an ordered list of typed
 * {@link BrazilCustomPaymentMethodMetadataEntry} entries rather than a
 * {@code Map}, so the public surface stays Map-free.
 */
@ProtobufMessage
public final class BrazilCustomPaymentMethodCreate {
    /**
     * Account-device identifier this custom payment method is bound to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String accountDeviceId;

    /**
     * Custom payment-method type discriminator (e.g. {@code "pix"}).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String customPaymentMethodType;

    /**
     * Payload that updates the credential's mutable attributes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String customPaymentMethodUpdate;

    /**
     * Flow discriminator routing the request through the right
     * onboarding state machine.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String customPaymentMethodFlow;

    /**
     * Metadata key/value entries forwarded to the relay verbatim.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<BrazilCustomPaymentMethodMetadataEntry> metadata;

    /**
     * Constructs a new {@code BrazilCustomPaymentMethodCreate}.
     *
     * @param accountDeviceId            the optional account-device id
     * @param customPaymentMethodType    the optional method type
     * @param customPaymentMethodUpdate  the optional update payload
     * @param customPaymentMethodFlow    the optional flow discriminator
     * @param metadata                   the optional metadata entries
     */
    BrazilCustomPaymentMethodCreate(String accountDeviceId, String customPaymentMethodType,
                                    String customPaymentMethodUpdate, String customPaymentMethodFlow,
                                    List<BrazilCustomPaymentMethodMetadataEntry> metadata) {
        this.accountDeviceId = accountDeviceId;
        this.customPaymentMethodType = customPaymentMethodType;
        this.customPaymentMethodUpdate = customPaymentMethodUpdate;
        this.customPaymentMethodFlow = customPaymentMethodFlow;
        this.metadata = metadata;
    }

    /**
     * Returns the account-device id.
     *
     * @return an {@link Optional} carrying the id, or empty when unset
     */
    public Optional<String> accountDeviceId() {
        return Optional.ofNullable(accountDeviceId);
    }

    /**
     * Returns the custom payment-method type.
     *
     * @return an {@link Optional} carrying the type, or empty when unset
     */
    public Optional<String> customPaymentMethodType() {
        return Optional.ofNullable(customPaymentMethodType);
    }

    /**
     * Returns the update payload.
     *
     * @return an {@link Optional} carrying the payload, or empty when unset
     */
    public Optional<String> customPaymentMethodUpdate() {
        return Optional.ofNullable(customPaymentMethodUpdate);
    }

    /**
     * Returns the flow discriminator.
     *
     * @return an {@link Optional} carrying the discriminator, or empty when unset
     */
    public Optional<String> customPaymentMethodFlow() {
        return Optional.ofNullable(customPaymentMethodFlow);
    }

    /**
     * Returns the metadata entries.
     *
     * @return the metadata entries; never {@code null}
     */
    public List<BrazilCustomPaymentMethodMetadataEntry> metadata() {
        return metadata == null ? List.of() : metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BrazilCustomPaymentMethodCreate) obj;
        return Objects.equals(accountDeviceId, that.accountDeviceId) &&
                Objects.equals(customPaymentMethodType, that.customPaymentMethodType) &&
                Objects.equals(customPaymentMethodUpdate, that.customPaymentMethodUpdate) &&
                Objects.equals(customPaymentMethodFlow, that.customPaymentMethodFlow) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountDeviceId, customPaymentMethodType, customPaymentMethodUpdate,
                customPaymentMethodFlow, metadata);
    }

    @Override
    public String toString() {
        return "BrazilCustomPaymentMethodCreate[" +
                "accountDeviceId=" + accountDeviceId + ", " +
                "customPaymentMethodType=" + customPaymentMethodType + ", " +
                "customPaymentMethodUpdate=" + customPaymentMethodUpdate + ", " +
                "customPaymentMethodFlow=" + customPaymentMethodFlow + ", " +
                "metadata=" + metadata + ']';
    }
}
