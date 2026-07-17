package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Represents a single key/value metadata entry attached to a
 * {@link CustomPaymentMethod}.
 *
 * <p>Payment integrations may need to carry auxiliary information alongside a
 * credential (for example a display label, a masked account number, or an
 * integrator-specific routing hint). Each of those pieces of information is
 * modeled as a free-form string key paired with a string value, and several
 * such entries can be grouped inside a single custom payment method.
 */
@ProtobufMessage(name = "SyncActionValue.CustomPaymentMethodMetadata")
public final class CustomPaymentMethodMetadata implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify custom payment
     * method metadata entries within the sync-action protocol.
     */
    public static final String ACTION_NAME = "custom_payment_method_metadata";

    /**
     * Canonical action version advertised by WhatsApp for entries of this
     * action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name associated with this action.
     *
     * @return the action name defined by {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version associated with this action.
     *
     * @return the action version defined by {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Identifier of the metadata entry, interpreted by the payment
     * integration.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String key;

    /**
     * Value associated with the metadata key.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String value;


    /**
     * Constructs a new metadata entry with the provided key and value.
     *
     * @param key the metadata key, non-{@code null}
     * @param value the metadata value, non-{@code null}
     * @throws NullPointerException if {@code key} or {@code value} is
     *         {@code null}
     */
    CustomPaymentMethodMetadata(String key, String value) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Returns the key of this metadata entry.
     *
     * @return the metadata key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the value of this metadata entry.
     *
     * @return the metadata value
     */
    public String value() {
        return value;
    }

    /**
     * Sets the key of this metadata entry.
     *
     * @param key the new metadata key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the value of this metadata entry.
     *
     * @param value the new metadata value
     */
    public void setValue(String value) {
        this.value = value;
    }
}
