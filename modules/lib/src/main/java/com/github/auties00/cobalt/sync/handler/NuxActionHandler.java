package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.preference.OnboardingHintStateBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.device.NuxAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code nux} app-state action that records acknowledgement of New
 * User Experience prompts (onboarding hints, first-run dialogs).
 *
 * <p>This handler backs the per-account NUX surface: when the user dismisses a
 * one-shot tip on any device the resulting acknowledgement fans out across the
 * {@link SyncPatchType#REGULAR_LOW} collection so the same tip does not
 * reappear on the other paired devices. The mutation index keys each entry by
 * the NUX key, formatted as
 * {@snippet :
 *     ["nux", nuxKey]
 * }
 * There is no AB-prop gate: WA Web registers the handler unconditionally.
 *
 * @implNote
 * This implementation collapses WA Web's two-tier NUX preference layout into a
 * single map keyed by NUX key on
 * {@link LinkedWhatsAppStore}; the timestamp WA Web
 * carries alongside the acknowledgement is not preserved because no Cobalt
 * consumer reads it. A missing {@link NuxAction} on the value defaults
 * {@code acknowledged} to {@code false} and still returns
 * {@link MutationApplicationResult#success()} (the per-batch WA Web log
 * counters are dropped).
 */
@WhatsAppWebModule(moduleName = "WAWebNuxSync")
public final class NuxActionHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link NuxActionHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NuxActionHandler.class);

    /**
     * Constructs a stateless {@link NuxActionHandler} for registration in the
     * sync handler registry.
     */
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public NuxActionHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return NuxAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return NuxAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return NuxAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} is accepted. A missing or non-string
     * index slot surfaces as
     * {@link SyncdIndexUtils#malformedActionIndex(String, String)}; a missing
     * {@link NuxAction} coalesces {@link NuxAction#acknowledged()} to
     * {@code false} and STILL writes the hint state. The resolved key and
     * dismissed flag are persisted via
     * {@link LinkedWhatsAppSettingsStore#putOnboardingHintState(com.github.auties00.cobalt.wire.linked.preference.OnboardingHintState)}.
     *
     * @implNote
     * This implementation drops the WA Web timestamp on the NUX data because
     * Cobalt's typed store has no consumer for it.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebNuxSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "nux action: unsupported operation {0}", mutation.operation());
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "nux action mutation malformed: missing nux key index");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var nuxKey = indexArray.getString(1);
        if (nuxKey == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "nux action mutation malformed: null nux key");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var nuxAction = mutation.value().flatMap(sav -> sav.action()).orElse(null);
        var acknowledged = nuxAction instanceof NuxAction action && action.acknowledged();

        client.store().settingsStore().putOnboardingHintState(new OnboardingHintStateBuilder().hintId(nuxKey).dismissed(acknowledged).build());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "nux action: hint {0} dismissed={1}", nuxKey, acknowledged);

        return MutationApplicationResult.success();
    }

}
