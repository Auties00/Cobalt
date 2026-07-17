package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Records the user's acceptance of a payments-related terms of service notice
 * and synchronizes it across linked devices.
 *
 * <p>WhatsApp presents region-specific legal notices before enabling payments
 * features (for example the Brazilian PIX privacy policy). When the user
 * acknowledges such a notice on one device, this action is emitted so that
 * all companion devices observe the same acceptance state and do not prompt
 * the user again.
 *
 * <p>The action belongs to the {@link SyncPatchType#REGULAR_LOW} collection,
 * meaning it is part of the low-priority regular sync stream rather than the
 * critical-path data stream.
 */
@ProtobufMessage(name = "SyncActionValue.PaymentTosAction")
public final class PaymentTosAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify payment terms of
     * service updates within the sync-action protocol.
     */
    public static final String ACTION_NAME = "payment_tos";

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
     * Identifies which payment notice the acceptance state refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PaymentNotice paymentNotice;

    /**
     * Whether the notice has been accepted by the user.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean accepted;


    /**
     * Constructs a new payment terms of service action with the provided
     * fields.
     *
     * @param paymentNotice the payment notice identifier, non-{@code null}
     * @param accepted the acceptance flag, non-{@code null}
     * @throws NullPointerException if {@code paymentNotice} or {@code accepted}
     *         is {@code null}
     */
    PaymentTosAction(PaymentNotice paymentNotice, Boolean accepted) {
        this.paymentNotice = Objects.requireNonNull(paymentNotice);
        this.accepted = Objects.requireNonNull(accepted);
    }

    /**
     * Returns the payment notice identifier this action refers to.
     *
     * @return the {@link PaymentNotice}
     */
    public PaymentNotice paymentNotice() {
        return paymentNotice;
    }

    /**
     * Returns whether the user has accepted the referenced payment notice.
     *
     * @return {@code true} if the notice has been accepted, {@code false}
     *         otherwise
     */
    public Boolean accepted() {
        return accepted;
    }

    /**
     * Sets the payment notice identifier this action refers to.
     *
     * @param paymentNotice the new {@link PaymentNotice}
     */
    public void setPaymentNotice(PaymentNotice paymentNotice) {
        this.paymentNotice = paymentNotice;
    }

    /**
     * Sets the acceptance state of the referenced payment notice.
     *
     * @param accepted the new acceptance flag
     */
    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * Identifies a specific payment-related legal notice that can be
     * accepted by the user.
     */
    @ProtobufEnum(name = "SyncActionValue.PaymentTosAction.PaymentNotice")
    public static enum PaymentNotice {
        /**
         * Brazilian PIX payment privacy policy notice.
         */
        BR_PAY_PRIVACY_POLICY(0);

        /**
         * Constructs a new enum constant with the provided protobuf index.
         *
         * @param index the protobuf wire index
         */
        PaymentNotice(@ProtobufEnumIndex int index) {
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
