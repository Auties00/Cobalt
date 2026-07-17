package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.UGCBotAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Persists the user-generated-content bot definition carried by a
 * {@code ugc_bot} mutation so it survives until WhatsApp Web ships a real
 * consumer.
 *
 * <p>The sync dispatcher would route incoming {@code ugc_bot} mutations here if
 * the server ever emits one. The handler captures the raw protobuf
 * {@code definition} bytes into
 * {@link LinkedWhatsAppBusinessStore#setUserCreatedBotDefinition(byte[])}
 * so downstream code can pick them up once the matching WA Web sync module
 * ships.
 *
 * @implNote
 * This implementation is forward-looking. The {@code UGC_BOT} protobuf is
 * declared in {@code WAWebProtobufSyncAction.pb} at action index 43 with action
 * name {@code "ugc_bot"} and is routed to {@code REGULAR_HIGH} by the
 * {@code getCollectionForAction} switch, but the current WA Web bundle ships no
 * {@code WAWebUGCBotSync} module. Cobalt's handler ingests the mutation today so
 * the wire payload is not lost when the server starts emitting it.
 */
public final class UGCBotHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link UGCBotHandler}.
     */
    private static final System.Logger LOGGER = Log.get(UGCBotHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    public UGCBotHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return UGCBotAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#REGULAR_HIGH} as declared
     * by the {@code e===c.UGC_BOT||e===c.STATUS_PRIVACY?u.REGULAR_HIGH} branch in
     * WA Web's {@code WAWebProtobufSyncAction.pb} {@code getCollectionForAction}
     * resolver.
     */
    @Override
    public SyncPatchType collectionName() {
        return UGCBotAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@code 1}, the lowest valid mutation version,
     * because WA Web ships no {@code WAWebUGCBotSync} module and therefore exposes
     * no {@code getVersion()} value to copy.
     */
    @Override
    public int version() {
        return UGCBotAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A non-{@link SyncdOperation#SET} operation is reported as
     * {@link MutationApplicationResult#unsupported()}, a missing
     * {@link UGCBotAction#definition()} is reported as malformed, and the raw
     * definition bytes are otherwise persisted via
     * {@link LinkedWhatsAppBusinessStore#setUserCreatedBotDefinition(byte[])}.
     *
     * @implNote
     * This implementation follows the canonical single-payload shape of sibling
     * handlers because WA Web ships no concrete handler to mirror.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof UGCBotAction action)
                || action.definition().isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ugc bot: malformed mutation, missing definition");
            return MutationApplicationResult.malformed();
        }

        client.store().businessStore().setUserCreatedBotDefinition(action.definition().get());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "ugc bot: persisted definition, size={0}", action.definition().get().length);
        return MutationApplicationResult.success();
    }
}
