package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Synchronizes the user's customer payment identifier across linked devices.
 *
 * <p>WhatsApp assigns a customer payment identifier (commonly referred to as
 * the {@code cpi}) that uniquely identifies the user within the payments
 * platform. This action propagates that identifier through the app-state
 * sync stream so that every companion device knows which identifier is
 * currently associated with the account.
 *
 * <p>The action belongs to the {@link SyncPatchType#REGULAR_LOW} collection,
 * meaning it is part of the low-priority regular sync stream rather than the
 * critical-path data stream.
 */
@ProtobufMessage(name = "SyncActionValue.PaymentInfoAction")
public final class PaymentInfoAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify payment information
     * updates within the sync-action protocol.
     */
    public static final String ACTION_NAME = "payment_info";

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
     * Customer payment identifier assigned to the user by the WhatsApp
     * payments platform.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String cpi;


    /**
     * Constructs a new payment information action carrying the provided
     * customer payment identifier.
     *
     * @param cpi the customer payment identifier, may be {@code null}
     */
    PaymentInfoAction(String cpi) {
        this.cpi = cpi;
    }

    /**
     * Returns the customer payment identifier carried by this action.
     *
     * @return an {@link Optional} containing the identifier, or empty if no
     *         identifier has been set
     */
    public Optional<String> cpi() {
        return Optional.ofNullable(cpi);
    }

    /**
     * Sets the customer payment identifier carried by this action.
     *
     * @param cpi the new customer payment identifier, may be {@code null}
     */
    public void setCpi(String cpi) {
        this.cpi = cpi;
    }
}
