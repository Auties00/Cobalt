package com.github.auties00.cobalt.wire.linked.sync.action.business;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A sync action recording a single customer's click-to-WhatsApp (CTWA) data-sharing preference.
 *
 * <p>When a user clicks on a CTWA ad, the business is allowed to associate the resulting
 * conversation with advertising data if the customer has opted in. This action captures the
 * per-customer opt-in flag so that every linked business device sees the same preference.
 * Records are keyed by the customer's account LID JID through
 * {@link CtwaPerCustomerDataSharingActionArgs}.
 *
 * <p>This action is transported in the {@code REGULAR_HIGH} sync collection because the
 * preference affects outbound advertising attribution and must propagate with minimal delay.
 */
@ProtobufMessage(name = "SyncActionValue.CtwaPerCustomerDataSharingAction")
public final class CtwaPerCustomerDataSharingAction implements SyncAction<CtwaPerCustomerDataSharingActionArgs> {
    /**
     * The canonical action name used when encoding this action inside a sync patch index.
     */
    public static final String ACTION_NAME = "ctwaPerCustomerDataSharing";

    /**
     * The action version negotiated with the server for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that carries this action between devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version for this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Whether the customer has opted in to sharing CTWA (click-to-WhatsApp) attribution data
     * with the business.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isCtwaPerCustomerDataSharingEnabled;


    /**
     * Creates a data-sharing action with the supplied opt-in flag.
     *
     * @param isCtwaPerCustomerDataSharingEnabled {@code true} if the customer has opted in,
     *                                            {@code false} or {@code null} otherwise
     */
    CtwaPerCustomerDataSharingAction(Boolean isCtwaPerCustomerDataSharingEnabled) {
        this.isCtwaPerCustomerDataSharingEnabled = isCtwaPerCustomerDataSharingEnabled;
    }

    /**
     * Returns whether the customer has opted in to CTWA data sharing.
     *
     * @return {@code true} if data sharing is enabled, {@code false} otherwise
     */
    public boolean isCtwaPerCustomerDataSharingEnabled() {
        return isCtwaPerCustomerDataSharingEnabled != null && isCtwaPerCustomerDataSharingEnabled;
    }

    /**
     * Updates the CTWA data-sharing opt-in flag.
     *
     * @param isCtwaPerCustomerDataSharingEnabled {@code true} to mark the customer as opted in,
     *                                            {@code false} or {@code null} otherwise
     */
    public void setCtwaPerCustomerDataSharingEnabled(Boolean isCtwaPerCustomerDataSharingEnabled) {
        this.isCtwaPerCustomerDataSharingEnabled = isCtwaPerCustomerDataSharingEnabled;
    }


}
