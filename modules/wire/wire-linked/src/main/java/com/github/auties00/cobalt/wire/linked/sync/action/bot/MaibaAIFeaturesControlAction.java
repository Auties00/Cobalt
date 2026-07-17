package com.github.auties00.cobalt.wire.linked.sync.action.bot;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that propagates the user's Meta AI ("Maiba") feature
 * preference across every linked device.
 *
 * <p>This action carries a single enum value indicating whether Meta AI
 * features are enabled, enabled with learning consent, or fully disabled for
 * the account. When the user changes this setting on one device, the
 * preference is broadcast through the high-priority regular sync collection
 * so that companion devices immediately reflect the new state.
 *
 * <p>The associated args type is {@link SyncActionEmptyArgs} because the
 * preference is global to the account and is not scoped to any particular
 * chat, thread, or bot.
 */
@ProtobufMessage(name = "SyncActionValue.MaibaAIFeaturesControlAction")
public final class MaibaAIFeaturesControlAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the wire.
     */
    public static final String ACTION_NAME = "maiba_ai_features_control";

    /**
     * The canonical action version negotiated by the WhatsApp sync protocol
     * for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync patch collection that stores this action on the server.
     *
     * <p>Meta AI preferences are placed in the high-priority regular
     * collection so that toggling the feature propagates quickly to all
     * linked devices.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name used to identify this sync action.
     *
     * @return the action name string
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical action version negotiated for this sync action.
     *
     * @return the action version integer
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The Meta AI feature status carried by this action, or {@code null} if
     * unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    MaibaAIFeatureStatus aiFeatureStatus;


    /**
     * Constructs a new {@code MaibaAIFeaturesControlAction} carrying the
     * supplied feature status.
     *
     * @param aiFeatureStatus the Meta AI feature status to persist, or
     *                        {@code null} if the field is unset
     */
    MaibaAIFeaturesControlAction(MaibaAIFeatureStatus aiFeatureStatus) {
        this.aiFeatureStatus = aiFeatureStatus;
    }

    /**
     * Returns the Meta AI feature status carried by this action.
     *
     * @return an {@link Optional} containing the feature status, or
     *         {@link Optional#empty()} if the field was not set
     */
    public Optional<MaibaAIFeatureStatus> aiFeatureStatus() {
        return Optional.ofNullable(aiFeatureStatus);
    }

    /**
     * Updates the Meta AI feature status carried by this action.
     *
     * @param aiFeatureStatus the new feature status, or {@code null} to
     *                        clear the field
     */
    public void setAiFeatureStatus(MaibaAIFeatureStatus aiFeatureStatus) {
        this.aiFeatureStatus = aiFeatureStatus;
    }

    /**
     * Enumerates the possible values of the user's Meta AI ("Maiba") feature
     * preference as stored and synchronised by WhatsApp.
     */
    @ProtobufEnum(name = "SyncActionValue.MaibaAIFeaturesControlAction.MaibaAIFeatureStatus")
    public enum MaibaAIFeatureStatus {
        /**
         * Meta AI features are enabled for the account but the user has not
         * granted the additional consent required to contribute interactions
         * to Meta AI model learning.
         */
        ENABLED(0),
        /**
         * Meta AI features are enabled and the user has consented to having
         * their interactions used for Meta AI model learning.
         */
        ENABLED_HAS_LEARNING(1),
        /**
         * Meta AI features are fully disabled for the account.
         */
        DISABLED(2);

        /**
         * Constructs a new feature status constant associated with the given
         * protobuf wire index.
         *
         * @param index the integer index assigned to this constant on the
         *              wire
         */
        MaibaAIFeatureStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The integer index assigned to this status on the protobuf wire.
         */
        final int index;

        /**
         * Returns the integer index that represents this status on the
         * protobuf wire.
         *
         * @return the protobuf wire index of this status
         */
        public int index() {
            return this.index;
        }
    }
}
