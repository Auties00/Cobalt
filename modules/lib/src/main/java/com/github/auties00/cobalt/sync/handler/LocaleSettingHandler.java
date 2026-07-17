package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedLocaleChangedListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.LocaleSetting;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Applies the {@code setting_locale} app-state sync action that propagates the
 * user's preferred display locale across linked devices.
 *
 * <p>The action carries a BCP-47 string fanned out across the
 * {@link SyncPatchType#CRITICAL_BLOCK} collection so every companion surface
 * re-renders in the same locale. The mutation index has no variable parts and
 * is always
 * {@snippet :
 *     ["setting_locale"]
 * }
 *
 * @implNote
 * This implementation persists the locale into
 * {@link LinkedWhatsAppAccountStore#setLocale(String)} and
 * notifies every registered
 * {@link LinkedWhatsAppClientListener#onLocaleChanged(LinkedWhatsAppClient, String, String)}
 * on its own virtual thread, since Cobalt has no UI layer to delegate to.
 */
@WhatsAppWebModule(moduleName = "WAWebLocaleSettingSync")
public final class LocaleSettingHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link LocaleSettingHandler}.
     */
    private static final System.Logger LOGGER = Log.get(LocaleSettingHandler.class);

    /**
     * Constructs a new singleton {@link LocaleSettingHandler}.
     */
    @WhatsAppWebExport(moduleName = "WAWebLocaleSettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public LocaleSettingHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLocaleSettingSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return LocaleSetting.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLocaleSettingSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return LocaleSetting.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLocaleSettingSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return LocaleSetting.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects non-{@link SyncdOperation#SET} operations as
     * {@link MutationApplicationResult#unsupported()}, an absent action payload
     * as {@link MutationApplicationResult#malformed()}, and a {@code null}
     * locale as {@link MutationApplicationResult#skipped()}. Otherwise the new
     * locale is committed to the store and fanned out to listeners.
     *
     * @implNote
     * This implementation dispatches each listener notification on its own
     * virtual thread so a slow listener never blocks the sync pipeline.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebLocaleSettingSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof LocaleSetting setting)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "locale setting mutation has malformed action value");
            return MutationApplicationResult.malformed();
        }

        var newLocale = setting.locale().orElse(null);
        if (newLocale == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "locale setting mutation skipped, no locale carried");
            return MutationApplicationResult.skipped();
        }

        var oldLocale = client.store().accountStore().locale().orElse(null);
        client.store().accountStore().setLocale(newLocale);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "locale changed {0} -> {1}", oldLocale, newLocale);
        for (var listener : client.store().listeners()) {
            if (listener instanceof LinkedLocaleChangedListener typed) {
                Thread.startVirtualThread(() -> typed.onLocaleChanged(client, oldLocale, newLocale));
            }
        }

        return MutationApplicationResult.success();
    }

}
