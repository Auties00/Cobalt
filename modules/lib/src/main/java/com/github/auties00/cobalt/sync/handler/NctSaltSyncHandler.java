package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.NctSaltSyncAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import java.lang.System.Logger.Level;

/**
 * Applies the {@code nct_salt_sync} app-state action that distributes the
 * Notification Content Tokenizer (NCT) salt across linked devices.
 *
 * <p>This handler backs the privacy-preserving notification content pipeline:
 * the salt is consumed by NCT to tokenize the previewable content of incoming
 * notifications without leaking message text to the OS notification surface.
 * {@link SyncdOperation#REMOVE} clears the locally stored salt;
 * {@link SyncdOperation#SET} writes the new salt; any other operation is
 * reported as {@link MutationApplicationResult#unsupported()}. The mutation
 * index is the singleton
 * {@snippet :
 *     ["nct_salt_sync"]
 * }
 *
 * @implNote
 * This implementation persists the raw salt {@code byte[]} directly on the
 * typed Cobalt store, rather than the base64-encoded string WA Web stores in
 * its UserPrefs IndexedDB; the base64 round-trip is a JSON-only-storage
 * requirement of IndexedDB and has no equivalent in a typed in-memory store.
 * The per-batch WA Web log counters are dropped; per-mutation outcomes are
 * surfaced through {@link MutationApplicationResult}.
 */
@WhatsAppWebModule(moduleName = "WAWebNctSaltSync")
public final class NctSaltSyncHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link NctSaltSyncHandler}.
     *
     * @implNote
     * This implementation logs at {@link Level#DEBUG} for the success/clear
     * paths and {@link Level#WARNING} for the unsupported/missing-salt paths,
     * mirroring the WA Web log granularity even though the per-batch counters
     * are dropped.
     */
    private static final System.Logger LOGGER = Log.get(NctSaltSyncHandler.class);

    /**
     * Constructs a stateless {@link NctSaltSyncHandler} for registration in
     * the sync handler registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebNctSaltSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public NctSaltSyncHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNctSaltSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return NctSaltSyncAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNctSaltSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return NctSaltSyncAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNctSaltSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return NctSaltSyncAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The per-operation arms are:
     * <ul>
     *   <li>{@link SyncdOperation#REMOVE} clears the stored salt and returns
     *       {@link MutationApplicationResult#success()};</li>
     *   <li>any operation other than {@link SyncdOperation#SET} returns
     *       {@link MutationApplicationResult#unsupported()};</li>
     *   <li>a missing {@link NctSaltSyncAction#salt()} returns
     *       {@link MutationApplicationResult#malformed()} via
     *       {@link SyncdIndexUtils#malformedActionIndex(String, String)};</li>
     *   <li>the resolved {@code byte[]} salt is written via
     *       {@link LinkedWhatsAppStore#setNotificationContentTokenSalt(byte[])}.</li>
     * </ul>
     *
     * @implNote
     * This implementation elides the base64 encode/decode WA Web performs to
     * round-trip the salt through its UserPrefs IndexedDB; the typed Cobalt
     * store holds the raw bytes directly.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNctSaltSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.DIRECT)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() == SyncdOperation.REMOVE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "nct salt sync: removing stored salt");
            client.store().accountStore().setNotificationContentTokenSalt(null);
            return MutationApplicationResult.success();
        }

        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "nct salt sync: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        var salt = mutation.value().flatMap(sav -> sav.action())
                .filter(NctSaltSyncAction.class::isInstance)
                .map(NctSaltSyncAction.class::cast)
                .flatMap(NctSaltSyncAction::salt)
                .orElse(null);
        if (salt == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "nct salt sync mutation malformed: missing salt");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        client.store().accountStore().setNotificationContentTokenSalt(salt);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "nct salt sync: stored salt {0}", salt);
        return MutationApplicationResult.success();
    }
}
