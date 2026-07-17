package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySetting;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacySettingBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StatusPrivacyAction;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Updates the audience that may view the local user's status updates when
 * another linked device changes the status-privacy setting.
 *
 * <p>The sync dispatcher routes incoming {@code status_privacy} mutations here
 * whenever the user changes the status audience on another device. The handler
 * rewrites the {@link StatusPrivacySetting status-privacy setting} on
 * {@link LinkedWhatsAppStore} so subsequent status
 * posts use the new audience.
 */
@WhatsAppWebModule(moduleName = "WAWebStatusPrivacySettingSync")
public final class StatusPrivacyHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link StatusPrivacyHandler}.
     */
    private static final System.Logger LOGGER = Log.get(StatusPrivacyHandler.class);

    /**
     * Constructs the handler.
     *
     * <p>The handler is stateless; Cobalt's sync registry holds a single
     * instance per client.
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public StatusPrivacyHandler() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "getAction", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return StatusPrivacyAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "collectionName", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return StatusPrivacyAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "getVersion", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return StatusPrivacyAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The batch must contain exactly one mutation. A batch with anything other
     * than one mutation maps every entry to
     * {@link MutationApplicationResult#malformed()}. A non-{@link SyncdOperation#SET}
     * singleton is reported as {@link MutationApplicationResult#unsupported()};
     * otherwise the singleton is delegated to
     * {@link #applySetMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} inside a
     * {@code try/catch} that turns any exception into
     * {@link MutationApplicationResult#failed()}.
     *
     * @implNote
     * This implementation omits WA Web's {@code WALogger} messages around the count
     * check and the IDB write failure as telemetry.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MutationApplicationResult> applyMutationBatch(LinkedWhatsAppClient client, List<DecryptedMutation.Trusted> mutations) {
        if (mutations.size() != 1) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "status privacy: unexpected batch size={0}, expected 1", mutations.size());
            var malformed = new ArrayList<MutationApplicationResult>(mutations.size());
            for (var i = 0; i < mutations.size(); i++) {
                malformed.add(MutationApplicationResult.malformed());
            }
            return malformed;
        }

        var last = mutations.get(mutations.size() - 1);
        if (last.operation() != SyncdOperation.SET) {
            return List.of(MutationApplicationResult.unsupported());
        }

        try {
            return List.of(applySetMutation(client, last));
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "status privacy mutation batch failed", e);
            return List.of(MutationApplicationResult.failed());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is the single-mutation entry point used when the caller already
     * knows the batch contains exactly one mutation. It applies the same
     * {@link SyncdOperation#SET}-only filter and {@code try/catch} wrapper as
     * {@link #applyMutationBatch(LinkedWhatsAppClient, List)} and delegates to
     * {@link #applySetMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        try {
            return applySetMutation(client, mutation);
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "status privacy mutation failed", e);
            return MutationApplicationResult.failed();
        }
    }

    /**
     * Translates the decoded status-privacy mode into a
     * {@link StatusPrivacySetting} and writes it to the store.
     *
     * <p>Validates the value and dispatches on the distribution mode:
     * {@code CONTACTS} writes a {@link StatusPrivacyMode#CONTACTS} setting with an
     * empty JID list; {@code ALLOW_LIST} and {@code DENY_LIST} write a
     * {@link StatusPrivacyMode#WHITELIST} /
     * {@link StatusPrivacyMode#CONTACTS_EXCEPT} setting whose JID list
     * is the input {@link StatusPrivacyAction#userJid()} filtered through
     * {@link #filterUserJids(List)}; {@code CLOSE_FRIENDS} and {@code CUSTOM_LIST}
     * are accepted as no-ops. A missing value or mode is reported as malformed. The
     * caller is responsible for the {@link SyncdOperation#SET} check and the
     * surrounding {@code try/catch}.
     *
     * @implNote
     * This implementation drops WA Web's crossposting branch
     * ({@code WAWebCrosspostingBackendGatingUtils.crosspostSettingsSyncReceiverEnabled})
     * because Cobalt's {@link StatusPrivacyAction} model does not carry the
     * {@code shareToFB} / {@code shareToIG} fields.
     *
     * @param client   the {@link LinkedWhatsAppClient} whose store receives the entry
     * @param mutation the {@code SET} mutation to apply
     * @return the detailed application result
     */
    @WhatsAppWebExport(moduleName = "WAWebStatusPrivacySettingSync", exports = "applyMutations", adaptation = WhatsAppAdaptation.ADAPTED)
    private MutationApplicationResult applySetMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof StatusPrivacyAction action)) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var mode = action.mode().orElse(null);
        if (mode == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "status privacy: malformed mutation, missing mode");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        var userJid = action.userJid();

        StatusPrivacySetting setting = null;
        switch (mode) {
            case CONTACTS -> setting = new StatusPrivacySettingBuilder()
                    .mode(StatusPrivacyMode.CONTACTS)
                    .jids(List.of())
                    .build();
            case ALLOW_LIST -> setting = new StatusPrivacySettingBuilder()
                    .mode(StatusPrivacyMode.WHITELIST)
                    .jids(filterUserJids(userJid))
                    .build();
            case DENY_LIST -> setting = new StatusPrivacySettingBuilder()
                    .mode(StatusPrivacyMode.CONTACTS_EXCEPT)
                    .jids(filterUserJids(userJid))
                    .build();
            case CLOSE_FRIENDS, CUSTOM_LIST -> {
            }
        }

        if (setting != null) {
            client.store().settingsStore().setStatusPrivacy(setting);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "status privacy: set mode={0}", mode);
        }
        return MutationApplicationResult.success();
    }

    /**
     * Filters a JID list to those whose server is a user-server domain.
     *
     * <p>The status-privacy allow-list and deny-list payloads may contain any JID
     * type on the wire; this helper drops non-user JIDs so only user-addressable
     * contacts end up in the persisted privacy entry. {@code null} entries are
     * tolerated and skipped, and a {@code null} or empty input yields an empty
     * list.
     *
     * @implNote
     * WA Web's {@code createWid} would throw on a null input; Cobalt's iterator
     * skips them so the surrounding handler does not bubble up an unexpected
     * exception.
     *
     * @param jids the input JID list
     * @return an unmodifiable list of user-server JIDs in input order
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isUser", adaptation = WhatsAppAdaptation.ADAPTED)
    private static List<Jid> filterUserJids(List<Jid> jids) {
        if (jids == null || jids.isEmpty()) {
            return List.of();
        }
        var filtered = new ArrayList<Jid>(jids.size());
        for (var jid : jids) {
            if (jid == null) {
                continue;
            }
            if (isUserWid(jid)) {
                filtered.add(jid);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * Reports whether the given JID belongs to a user-server domain.
     *
     * <p>The JID's server must be one of {@code c.us}, {@code lid}, {@code bot},
     * {@code hosted}, or {@code hosted.lid}. Used by {@link #filterUserJids(List)}
     * to drop non-addressable contacts from a privacy list.
     *
     * @implNote
     * This implementation reads the {@link JidServer.Type} discriminator directly
     * because {@link Jid#hasUserServer()} alone only covers the standard and legacy
     * user domains, missing LID / hosted / bot.
     *
     * @param jid the JID to test
     * @return {@code true} if the JID's server is a user-server domain
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "isUser", adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isUserWid(Jid jid) {
        var type = jid.server().type();
        return type == JidServer.Type.USER
                || type == JidServer.Type.LEGACY_USER
                || type == JidServer.Type.LID
                || type == JidServer.Type.BOT
                || type == JidServer.Type.HOSTED
                || type == JidServer.Type.HOSTED_LID;
    }

}
