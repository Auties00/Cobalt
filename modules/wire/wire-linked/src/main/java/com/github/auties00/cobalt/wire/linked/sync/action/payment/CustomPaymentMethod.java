package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single custom payment method that the user has registered for
 * sending or receiving payments through WhatsApp.
 *
 * <p>A custom payment method describes an external payment credential (for
 * example a bank card or wallet) that is not natively managed by WhatsApp but
 * is referenced through an opaque credential identifier. It includes the
 * country in which the credential is valid, a type descriptor, and an
 * arbitrary list of key/value metadata entries that the payment integration
 * may use for presentation or routing.
 *
 * <p>Instances of this type are typically grouped inside a
 * {@link CustomPaymentMethodsAction}, which synchronizes the full list of
 * registered methods across the user's linked devices.
 */
@ProtobufMessage(name = "SyncActionValue.CustomPaymentMethod")
public final class CustomPaymentMethod implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify a custom payment
     * method entry within the sync-action protocol.
     */
    public static final String ACTION_NAME = "custom_payment_method";

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
     * Opaque credential identifier that uniquely identifies this payment
     * method within the user's account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String credentialId;

    /**
     * ISO country code for the region in which the credential is valid.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String country;

    /**
     * Textual type descriptor of the payment method (for example a card or
     * wallet category defined by the integrator).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String type;

    /**
     * Additional key/value entries describing the payment method, used by
     * payment integrations for presentation and routing.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    List<CustomPaymentMethodMetadata> metadata;


    /**
     * Constructs a new custom payment method with the provided fields.
     *
     * @param credentialId the opaque credential identifier, non-{@code null}
     * @param country the ISO country code for the credential, non-{@code null}
     * @param type the payment method type descriptor, non-{@code null}
     * @param metadata the associated metadata entries, may be {@code null}
     * @throws NullPointerException if {@code credentialId}, {@code country} or
     *         {@code type} is {@code null}
     */
    CustomPaymentMethod(String credentialId, String country, String type, List<CustomPaymentMethodMetadata> metadata) {
        this.credentialId = Objects.requireNonNull(credentialId);
        this.country = Objects.requireNonNull(country);
        this.type = Objects.requireNonNull(type);
        this.metadata = metadata;
    }

    /**
     * Returns the opaque credential identifier of this payment method.
     *
     * @return the credential identifier
     */
    public String credentialId() {
        return credentialId;
    }

    /**
     * Returns the ISO country code under which the credential is registered.
     *
     * @return the country code
     */
    public String country() {
        return country;
    }

    /**
     * Returns the textual type descriptor of this payment method.
     *
     * @return the type descriptor
     */
    public String type() {
        return type;
    }

    /**
     * Returns the metadata entries associated with this payment method.
     *
     * <p>The returned list is unmodifiable. If no metadata has been set, an
     * empty list is returned rather than {@code null}.
     *
     * @return an unmodifiable list of metadata entries, never {@code null}
     */
    public List<CustomPaymentMethodMetadata> metadata() {
        return metadata == null ? List.of() : Collections.unmodifiableList(metadata);
    }

    /**
     * Sets the opaque credential identifier of this payment method.
     *
     * @param credentialId the new credential identifier
     */
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    /**
     * Sets the ISO country code under which the credential is registered.
     *
     * @param country the new country code
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Sets the textual type descriptor of this payment method.
     *
     * @param type the new type descriptor
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets the metadata entries associated with this payment method.
     *
     * @param metadata the new list of metadata entries, may be {@code null}
     */
    public void setMetadata(List<CustomPaymentMethodMetadata> metadata) {
        this.metadata = metadata;
    }
}
