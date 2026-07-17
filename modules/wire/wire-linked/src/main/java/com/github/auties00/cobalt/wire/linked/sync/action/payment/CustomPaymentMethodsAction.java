package com.github.auties00.cobalt.wire.linked.sync.action.payment;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Synchronizes the full list of the user's custom payment methods across
 * linked devices.
 *
 * <p>This action carries the complete collection of
 * {@link CustomPaymentMethod} entries that the user has registered. Whenever
 * the list is modified on one device (for example when a credential is added
 * or removed), WhatsApp broadcasts the updated collection through this sync
 * action so that every companion device observes the same set of methods.
 *
 * <p>The action belongs to the {@link SyncPatchType#REGULAR_LOW} collection,
 * meaning it is part of the low-priority regular sync stream rather than the
 * critical-path data stream.
 */
@ProtobufMessage(name = "SyncActionValue.CustomPaymentMethodsAction")
public final class CustomPaymentMethodsAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used by WhatsApp to identify custom payment
     * methods updates within the sync-action protocol.
     */
    public static final String ACTION_NAME = "custom_payment_methods";

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
     * Full list of registered custom payment methods.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<CustomPaymentMethod> customPaymentMethods;


    /**
     * Constructs a new custom payment methods action carrying the provided
     * list.
     *
     * @param customPaymentMethods the list of payment methods to synchronize,
     *        may be {@code null}
     */
    CustomPaymentMethodsAction(List<CustomPaymentMethod> customPaymentMethods) {
        this.customPaymentMethods = customPaymentMethods;
    }

    /**
     * Returns the list of custom payment methods carried by this action.
     *
     * <p>The returned list is unmodifiable. If no methods have been set, an
     * empty list is returned rather than {@code null}.
     *
     * @return an unmodifiable list of custom payment methods, never
     *         {@code null}
     */
    public List<CustomPaymentMethod> customPaymentMethods() {
        return customPaymentMethods == null ? List.of() : Collections.unmodifiableList(customPaymentMethods);
    }

    /**
     * Sets the list of custom payment methods carried by this action.
     *
     * @param customPaymentMethods the new list of payment methods, may be
     *        {@code null}
     */
    public void setCustomPaymentMethods(List<CustomPaymentMethod> customPaymentMethods) {
        this.customPaymentMethods = customPaymentMethods;
    }
}
