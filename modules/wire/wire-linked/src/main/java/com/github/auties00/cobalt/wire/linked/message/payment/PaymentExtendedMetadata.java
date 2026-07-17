package com.github.auties00.cobalt.wire.linked.message.payment;

import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents additional metadata attached to a payment-related message.
 *
 * <p>Carries contextual information about a payment message that does
 * not fit into the primary payment fields, such as a numeric type tag
 * used by the WhatsApp payments backend and the name of the platform
 * that originated the payment. This metadata helps clients render
 * platform-specific payment UI and route payment events to the correct
 * processor.
 */
@ProtobufMessage(name = "Message.PaymentExtendedMetadata")
public final class PaymentExtendedMetadata implements Message {
    /**
     * The numeric type discriminator for this payment metadata.
     *
     * <p>An unsigned integer tag used by the WhatsApp payments
     * backend to classify the kind of payment this metadata refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer type;

    /**
     * The name of the platform that originated the payment.
     *
     * <p>Identifies the payment platform or provider that produced
     * the associated payment message, for example a specific regional
     * payment service.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String platform;


    /**
     * Constructs a new payment extended metadata with the given type
     * tag and platform name.
     *
     * @param type     the numeric type discriminator, may be {@code null}
     * @param platform the originating platform name, may be {@code null}
     */
    PaymentExtendedMetadata(Integer type, String platform) {
        this.type = type;
        this.platform = platform;
    }

    /**
     * Returns the numeric type discriminator.
     *
     * @return an {@link OptionalInt} containing the type tag, or
     *         {@link OptionalInt#empty()} if not set
     */
    public OptionalInt type() {
        return type == null ? OptionalInt.empty() : OptionalInt.of(type);
    }

    /**
     * Returns the originating platform name.
     *
     * @return an {@link Optional} containing the platform name, or
     *         {@link Optional#empty()} if not set
     */
    public Optional<String> platform() {
        return Optional.ofNullable(platform);
    }

    /**
     * Sets the numeric type discriminator.
     *
     * @param type the type tag, may be {@code null}
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * Sets the originating platform name.
     *
     * @param platform the platform name, may be {@code null}
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
