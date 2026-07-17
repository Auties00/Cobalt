package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Represents a sync action that replicates the authoritative snapshot of a
 * WhatsApp Business account's paid subscriptions and feature flags.
 *
 * <p>This action carries the full server state for business monetisation: the
 * catalog of active or historical {@link SubscriptionInfo} entries and the
 * current {@link PaidFeature} flag table. On {@code SET} the receiver replaces
 * its locally cached subscription state and feature flag table with the lists
 * carried in this message, and on {@code REMOVE} it clears that local state
 * entirely. The mutation is singleton, so the sync index is composed solely of
 * {@link #ACTION_NAME} with no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.SubscriptionsSyncV2Action")
public final class SubscriptionsSyncV2Action implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "subscriptions_sync_v2";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Repeated list of {@link SubscriptionInfo} entries describing every paid
     * subscription known to the server for this business account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<SubscriptionInfo> subscriptions;

    /**
     * Repeated list of {@link PaidFeature} entries describing every paid
     * business feature flag known to the server for this account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<PaidFeature> paidFeatures;

    /**
     * Constructs a new {@code SubscriptionsSyncV2Action} from raw protobuf field
     * values.
     *
     * @param subscriptions the subscriptions list, possibly {@code null}
     * @param paidFeatures  the paid features list, possibly {@code null}
     */
    SubscriptionsSyncV2Action(List<SubscriptionInfo> subscriptions, List<PaidFeature> paidFeatures) {
        this.subscriptions = subscriptions;
        this.paidFeatures = paidFeatures;
    }

    /**
     * Returns the canonical action name for every {@code SubscriptionsSyncV2Action}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code SubscriptionsSyncV2Action}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the unmodifiable list of {@link SubscriptionInfo} entries carried
     * by this action.
     *
     * <p>An empty list is returned when no subscription entries were present on
     * the wire; the result is never {@code null}.
     *
     * @return the unmodifiable subscriptions list
     */
    public List<SubscriptionInfo> subscriptions() {
        return subscriptions == null ? List.of() : Collections.unmodifiableList(subscriptions);
    }

    /**
     * Returns the unmodifiable list of {@link PaidFeature} entries carried by
     * this action.
     *
     * <p>An empty list is returned when no paid feature entries were present on
     * the wire; the result is never {@code null}.
     *
     * @return the unmodifiable paid features list
     */
    public List<PaidFeature> paidFeatures() {
        return paidFeatures == null ? List.of() : Collections.unmodifiableList(paidFeatures);
    }

    /**
     * Sets the list of {@link SubscriptionInfo} entries.
     *
     * @param subscriptions the new subscriptions list, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionsSyncV2Action setSubscriptions(List<SubscriptionInfo> subscriptions) {
        this.subscriptions = subscriptions;
        return this;
    }

    /**
     * Sets the list of {@link PaidFeature} entries.
     *
     * @param paidFeatures the new paid features list, or {@code null} to clear
     * @return this instance for method chaining
     */
    public SubscriptionsSyncV2Action setPaidFeatures(List<PaidFeature> paidFeatures) {
        this.paidFeatures = paidFeatures;
        return this;
    }
}
