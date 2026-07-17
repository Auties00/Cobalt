package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.WamoUserIdentifierAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Persists the server-issued WAMO (WhatsApp Newsletter Subscription) user identifier when the
 * primary device announces it via a {@code generated_wui} mutation.
 *
 * <p>The sync dispatcher would route incoming {@code generated_wui} mutations here if the server
 * ever emits one. The identifier is an opaque token used to tag the local user inside WA's
 * paid-newsletter subscription pipeline; the handler stores it on
 * {@link LinkedWhatsAppSettingsStore#setNewsletterSubscriptionUserIdentifier(String)}
 * so subsequent newsletter subscription operations can include it.
 *
 * @implNote
 * This implementation is forward-looking. The {@code WamoUserIdentifierAction} protobuf is declared
 * at action index 52, action name {@code "generated_wui"}, collection
 * {@link SyncPatchType#CRITICAL_BLOCK}, but the WA Web bundle ships no sync module and the action is
 * absent from WA Web's collection-handler action table. WA Web's only other reference to
 * {@code "generated_wui"} tracks the action while logging MAC inconsistencies without ever applying
 * it. Cobalt's handler captures the identifier today so it is not lost once a real consumer ships.
 */
public final class WamoUserIdentifierHandler implements WebAppStateActionHandler {

    /**
     * The logger for {@link WamoUserIdentifierHandler}.
     */
    private static final System.Logger LOGGER = Log.get(WamoUserIdentifierHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single instance per client.
     */
    public WamoUserIdentifierHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionName() {
        return WamoUserIdentifierAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link SyncPatchType#CRITICAL_BLOCK} as declared by WA Web's
     * {@code WAMO_USER_IDENTIFIER_ACTION -> CRITICAL_BLOCK} collection-router branch.
     */
    @Override
    public SyncPatchType collectionName() {
        return SyncPatchType.CRITICAL_BLOCK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int version() {
        return WamoUserIdentifierAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Non-{@link SyncdOperation#SET} operations are {@link MutationApplicationResult#unsupported()}.
     * A missing or blank {@link WamoUserIdentifierAction#identifier()} is
     * {@link MutationApplicationResult#malformed()}, and the resolved string is written to
     * {@link LinkedWhatsAppSettingsStore#setNewsletterSubscriptionUserIdentifier(String)}.
     *
     * @implNote
     * This implementation follows the canonical SET-only single-string shape used by sibling
     * identifier-token handlers because WA Web ships no concrete handler to mirror. The blank-string
     * filter is Cobalt-defensive: an empty WAMO identifier carries no meaningful update.
     */
    @Override
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wamo user identifier: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof WamoUserIdentifierAction action)
                || action.identifier().isEmpty()
                || action.identifier().get().isBlank()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "wamo user identifier: malformed mutation, missing or blank identifier");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setNewsletterSubscriptionUserIdentifier(action.identifier().get());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wamo user identifier: stored newsletter subscription user identifier");
        return MutationApplicationResult.success();
    }
}
