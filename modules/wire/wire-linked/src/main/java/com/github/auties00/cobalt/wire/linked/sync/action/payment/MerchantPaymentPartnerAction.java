package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.util.Objects;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Synchronizes the activation state of a merchant payment partner linked to
 * the user's account.
 *
 * <p>WhatsApp allows business users to integrate with external payment
 * gateways in specific countries. This action records whether a given
 * merchant partner is currently active for the user, together with the
 * country of the integration and, when present, the gateway name and
 * credential identifier that uniquely describe the partner instance.
 *
 * <p>The action belongs to the {@link SyncPatchType#REGULAR_LOW} collection,
 * meaning it is part of the low-priority regular sync stream rather than the
 * critical-path data stream.
 */
@ProtobufMessage(name = "SyncActionValue.MerchantPaymentPartnerAction")
public final class MerchantPaymentPartnerAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify merchant payment
     * partner updates within the sync-action protocol.
     */
    public static final String ACTION_NAME = "merchant_payment_partner";

    /**
     * Canonical action version advertised by WhatsApp for entries of this
     * action type.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Sync collection this action is stored in, as defined by the WhatsApp
     * app-state sync protocol.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

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
     * Activation state of the merchant payment partner integration.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Status status;

    /**
     * ISO country code of the region in which the partner is active.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String country;

    /**
     * Name of the payment gateway associated with the partner, if available.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String gatewayName;

    /**
     * Opaque identifier of the merchant credential, if available.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String credentialId;


    /**
     * Constructs a new merchant payment partner action with the provided
     * fields.
     *
     * @param status the activation state, non-{@code null}
     * @param country the ISO country code of the integration, non-{@code null}
     * @param gatewayName the gateway name, may be {@code null}
     * @param credentialId the credential identifier, may be {@code null}
     * @throws NullPointerException if {@code status} or {@code country} is
     *         {@code null}
     */
    MerchantPaymentPartnerAction(Status status, String country, String gatewayName, String credentialId) {
        this.status = Objects.requireNonNull(status);
        this.country = Objects.requireNonNull(country);
        this.gatewayName = gatewayName;
        this.credentialId = credentialId;
    }

    /**
     * Returns the activation state of the merchant payment partner.
     *
     * @return the current {@link Status}
     */
    public Status status() {
        return status;
    }

    /**
     * Returns the ISO country code of the region in which the partner is
     * active.
     *
     * @return the country code
     */
    public String country() {
        return country;
    }

    /**
     * Returns the gateway name associated with the merchant partner.
     *
     * @return an {@link Optional} containing the gateway name, or empty if
     *         none has been set
     */
    public Optional<String> gatewayName() {
        return Optional.ofNullable(gatewayName);
    }

    /**
     * Returns the opaque credential identifier associated with the merchant
     * partner.
     *
     * @return an {@link Optional} containing the credential identifier, or
     *         empty if none has been set
     */
    public Optional<String> credentialId() {
        return Optional.ofNullable(credentialId);
    }

    /**
     * Sets the activation state of the merchant payment partner.
     *
     * @param status the new {@link Status}
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the ISO country code of the region in which the partner is active.
     *
     * @param country the new country code
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Sets the gateway name associated with the merchant partner.
     *
     * @param gatewayName the new gateway name, may be {@code null}
     */
    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    /**
     * Sets the opaque credential identifier associated with the merchant
     * partner.
     *
     * @param credentialId the new credential identifier, may be {@code null}
     */
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    /**
     * Activation state of a merchant payment partner integration.
     */
    @ProtobufEnum(name = "SyncActionValue.MerchantPaymentPartnerAction.Status")
    public static enum Status {
        /**
         * Indicates that the merchant payment partner is currently enabled
         * for the user.
         */
        ACTIVE(0),

        /**
         * Indicates that the merchant payment partner is currently disabled
         * for the user.
         */
        INACTIVE(1);

        /**
         * Constructs a new enum constant with the provided protobuf index.
         *
         * @param index the protobuf wire index
         */
        Status(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Wire index used to serialize this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
