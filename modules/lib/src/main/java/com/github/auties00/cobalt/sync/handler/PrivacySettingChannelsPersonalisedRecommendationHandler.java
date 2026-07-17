package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingChannelsPersonalisedRecommendationAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code setting_channels_personalised_recommendation_optout}
 * app-state action that distributes the user's opt-out preference for
 * personalised channel recommendations across linked devices.
 *
 * <p>Persists a single {@code isUserOptedOut} flag on
 * {@link LinkedWhatsAppStore} so the Channels
 * personalised recommendation surface honours the opt-out uniformly across
 * devices. Only {@link SyncdOperation#SET} is accepted; any other operation is
 * reported as {@link MutationApplicationResult#unsupported()} and a
 * wrong-typed value as {@link MutationApplicationResult#malformed()}.
 *
 * @implNote
 * This implementation has no WA Web counterpart: the
 * {@code SyncActionValue.PrivacySettingChannelsPersonalisedRecommendationAction}
 * protobuf (action index 64) is declared in
 * {@code WAWebProtobufSyncAction.pb} but no
 * {@code WAWebPrivacySettingChannelsPersonalisedRecommendationSync} module
 * ships and the action is absent from {@code WASyncdConst.Actions} and from
 * {@code WAWebCollectionHandlerActions.ActionHandlers}, so WA Web silently
 * drops any incoming mutation. The {@link SyncPatchType#REGULAR} collection is
 * taken directly from the inline router in
 * {@code WAWebProtobufSyncAction.pb}; the single-{@code SET}, single-boolean
 * apply path is inferred from sibling boolean-flag privacy handlers (for
 * example {@link DisableLinkPreviewsHandler}).
 */
public final class PrivacySettingChannelsPersonalisedRecommendationHandler implements WebAppStateActionHandler {
    /**
     * Holds the shared singleton instance used by the sync handler registry.
     *
     * @implNote
     * This implementation initialises the singleton eagerly because the
     * constructor is private and stateless.
     */
    public static final PrivacySettingChannelsPersonalisedRecommendationHandler INSTANCE =
            new PrivacySettingChannelsPersonalisedRecommendationHandler();

    /**
     * The logger for {@link PrivacySettingChannelsPersonalisedRecommendationHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PrivacySettingChannelsPersonalisedRecommendationHandler.class);

    /**
     * Constructs the singleton handler instance.
     *
     * <p>Private to enforce singleton access via {@link #INSTANCE}.
     *
     * @implNote
     * This implementation is stateless; the constructor exists only so the
     * static {@link #INSTANCE} field can be initialised once.
     */
    private PrivacySettingChannelsPersonalisedRecommendationHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return PrivacySettingChannelsPersonalisedRecommendationAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR} as declared by
     * the inline router in {@code WAWebProtobufSyncAction.pb}.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.REGULAR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return PrivacySettingChannelsPersonalisedRecommendationAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation accepts only {@link SyncdOperation#SET}: the action
     * carries a single boolean opt-out flag and there is no semantic for
     * {@code REMOVE}. A wrong-typed value surfaces as
     * {@link MutationApplicationResult#malformed()}; on success the resolved
     * boolean is written via
     * {@code LinkedWhatsAppStore.setChannelsPersonalisedRecommendationOptOut}. The
     * {@link PrivacySettingChannelsPersonalisedRecommendationAction#isUserOptedOut()}
     * accessor coalesces a missing flag to {@code false} per the project
     * nullable boolean rule.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrivacySettingChannelsPersonalisedRecommendationAction action)) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "channels personalised recommendation mutation malformed: missing action value");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setChannelsPersonalisedRecommendationOptOut(action.isUserOptedOut());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "channels personalised recommendation: opt-out set to {0}", action.isUserOptedOut());
        return MutationApplicationResult.success();
    }
}
