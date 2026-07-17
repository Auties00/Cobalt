package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingRelayAllCalls;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;

/**
 * Mirrors the "Always relay calls" privacy setting across linked devices.
 *
 * <p>The sync dispatcher routes incoming {@code setting_relayAllCalls} mutations here whenever the
 * user toggles "Always relay calls" on another linked device (typical trigger: Privacy Settings,
 * Advanced, Protect IP Address in Calls). The handler writes the boolean preference onto
 * {@link LinkedWhatsAppSettingsStore#setRelayAllCalls(boolean)} so Cobalt's VoIP
 * layer routes subsequent calls through the WA relay instead of letting the peer learn the local IP
 * address.
 */
@WhatsAppWebModule(moduleName = "WAWebVoipRelayAllCallsSettingSync")
public final class VoipRelayAllCallsHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link VoipRelayAllCallsHandler}.
     */
    private static final System.Logger LOGGER = Log.get(VoipRelayAllCallsHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public VoipRelayAllCallsHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return PrivacySettingRelayAllCalls.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return PrivacySettingRelayAllCalls.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return PrivacySettingRelayAllCalls.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The mutation must be a {@link SyncdOperation#SET} whose decoded value is a
     * {@link PrivacySettingRelayAllCalls}; any other operation is
     * {@link MutationApplicationResult#unsupported()} and any other value is
     * {@link MutationApplicationResult#malformed()}. The boolean is written into
     * {@link LinkedWhatsAppSettingsStore#setRelayAllCalls(boolean)}.
     *
     * @implNote
     * This implementation replaces WA Web's {@code setRelayAllCallsToUserPrefs} backend shell hop
     * with a direct store write. The {@link PrivacySettingRelayAllCalls#isEnabled()} accessor
     * coalesces a null Boolean to {@code false}, so WA Web's null short-circuit (which skips the
     * persist call but still reports success) is relaxed to a direct write of {@code false}. The
     * warning counters and the outer try/catch-to-failed wrapper are dropped per Cobalt's pluggable
     * error model: thrown exceptions surface to the configured
     * {@link WhatsAppLinkedClientErrorHandler}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebVoipRelayAllCallsSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof PrivacySettingRelayAllCalls action)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "voip relay all calls: malformed mutation");
            return MutationApplicationResult.malformed();
        }

        client.store().settingsStore().setRelayAllCalls(action.isEnabled());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "voip relay all calls: set enabled={0}", action.isEnabled());
        return MutationApplicationResult.success();
    }

}
