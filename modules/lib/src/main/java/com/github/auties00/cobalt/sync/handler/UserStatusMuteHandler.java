package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataEditBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.UserStatusMuteAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.UserStatusMuteActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.StatusMuteEvent;
import com.github.auties00.cobalt.wire.wam.event.StatusMuteEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MuteAction;
import com.github.auties00.cobalt.wire.wam.type.MuteOrigin;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mirrors the per-contact or per-group "mute status updates" preference across linked devices.
 *
 * <p>The sync dispatcher routes incoming {@code userStatusMute} mutations here whenever the user
 * mutes or unmutes another contact's or group's status posts on a linked device. The handler
 * writes the new value to the matching
 * {@link Contact#setStatusMuted(boolean)} record or to
 * {@link GroupMetadata#statusMuted()} on
 * {@link LinkedWhatsAppStore}. Mutations referencing an unknown contact
 * or group are reported as {@link MutationApplicationResult#orphan(String, String)} so a later
 * contact or group-metadata sync can retry them.
 */
@WhatsAppWebModule(moduleName = "WAWebUserStatusMuteSync")
public final class UserStatusMuteHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link UserStatusMuteHandler}.
     */
    private static final System.Logger LOGGER = Log.get(UserStatusMuteHandler.class);

    /**
     * Holds the WAM telemetry service used to commit the {@link StatusMuteEvent} when a
     * status-mute mutation is successfully applied.
     */
    private final WamService wamService;

    /**
     * Constructs the handler bound to the given WAM telemetry service.
     *
     * <p>The handler holds no per-mutation state beyond the injected {@link WamService};
     * Cobalt's sync registry holds a single instance per client.
     *
     * @implNote
     * WA Web reaches the {@code WAWebStatusMuteWamEvent} singleton directly from the
     * status-mute UI action; this implementation injects {@link WamService} so the
     * {@link StatusMuteEvent} emission on the mutation-apply path is testable.
     *
     * @param wamService the WAM telemetry service used to commit the status-mute event
     */
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public UserStatusMuteHandler(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return UserStatusMuteAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return UserStatusMuteAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return UserStatusMuteAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@link SyncdOperation#SET} mutations are applied; any other operation is
     * {@link MutationApplicationResult#unsupported()}. The second index slot must parse as a valid
     * {@link Jid} and the decoded value must be a {@link UserStatusMuteAction}, otherwise the
     * mutation is reported as malformed. Group JIDs route through
     * {@link LinkedWhatsAppChatStore#applyGroupMetadataEdit(Jid, com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataEdit)};
     * user JIDs route through {@link Contact#setStatusMuted(boolean)}
     * on the resolved contact. An unknown group or contact surfaces as
     * {@link MutationApplicationResult#orphan(String, String)} with the model type
     * {@code "UserStatusMute"} so the dispatcher can retry once the missing entity arrives via a
     * separate sync.
     *
     * @implNote
     * This implementation drops WA Web's newsletter-status branch because Cobalt has no newsletter
     * status surface, and omits WA Web's warning counters and the {@code updateContactsStatusMute}
     * frontend hop as UI concerns, while committing the {@link StatusMuteEvent} telemetry on each
     * successful apply through
     * {@link #emitStatusMuteEvent(LinkedWhatsAppClient, Jid, boolean, Contact)}. The
     * {@link UserStatusMuteAction#muted()} accessor
     * coalesces a missing nullable boolean to {@code false}, so a mutation carrying {@code muted}
     * unset is applied as an unmute rather than reported as malformed, relaxing WA Web's explicit
     * value-present check.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        if (mutation.operation() != SyncdOperation.SET) {
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var widString = indexArray.getString(1);
        if (widString == null || widString.isEmpty()) {
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        Jid wid;
        try {
            wid = Jid.of(widString);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "user status mute: invalid wid={0}", new LogRedactable.User(widString));
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof UserStatusMuteAction action)) {
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (wid.hasServer(JidServer.groupOrCommunity())) {
            var edit = new GroupMetadataEditBuilder()
                    .group(wid)
                    .statusMuted(action.muted())
                    .build();
            var updated = client.store().chatStore().applyGroupMetadataEdit(wid, edit);
            if (updated.isEmpty()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "user status mute: orphan group={0}", wid);
                return MutationApplicationResult.orphan(widString, "UserStatusMute");
            }
            emitStatusMuteEvent(client, wid, action.muted(), null);
            if (Log.DEBUG)
                LOGGER.log(Level.DEBUG, "user status mute: set muted={0} for group={1}", action.muted(), wid);
            return MutationApplicationResult.success();
        }

        var contact = client.store().contactStore().findContactByJid(wid);
        if (contact.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "user status mute: orphan contact={0}", wid);
            return MutationApplicationResult.orphan(widString, "UserStatusMute");
        }

        contact.get().setStatusMuted(action.muted());
        emitStatusMuteEvent(client, wid, action.muted(), contact.get());
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "user status mute: set muted={0} for contact={1}", action.muted(), wid);
        return MutationApplicationResult.success();
    }

    /**
     * Builds the pending {@link SyncPendingMutation} that mutes or unmutes another user's or
     * group's status updates.
     *
     * <p>Invoked by Cobalt's outgoing-mutation factory when the embedder toggles {@code statusMuted}
     * on a contact or group; the returned pending mutation is queued through the regular sync
     * pipeline so the change propagates to the user's other devices. The index is
     * {@code ["userStatusMute", wid]} in legacy serialization, the value carries the {@code muted}
     * boolean, and the operation is {@link SyncdOperation#SET}.
     *
     * @implNote
     * This implementation reduces WA Web's {@code buildPendingMutation} call to direct
     * {@link DecryptedMutation.Trusted} and {@link SyncPendingMutation} construction because the
     * trusted mutation already carries the encoded value.
     *
     * @param wid       the user or group JID whose status mute state is being updated
     * @param muted     {@code true} to mute status updates, {@code false} to unmute
     * @param timestamp the mutation timestamp
     * @return the pending mutation ready for the outgoing sync pipeline
     */
    @WhatsAppWebExport(moduleName = "WAWebUserStatusMuteSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPendingMutation getMutationForStatusMute(Jid wid, boolean muted, Instant timestamp) {
        var action = new UserStatusMuteActionBuilder()
                .muted(muted)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .userStatusMuteAction(action)
                .build();
        var index = JSON.toJSONString(List.of(actionName(), wid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                SyncdOperation.SET,
                timestamp,
                version()
        );
        return new SyncPendingMutation(mutation, 0);
    }

    /**
     * Commits the {@link StatusMuteEvent} (WAM id 2978) for a status-mute mutation that
     * was just applied to the store.
     *
     * <p>Populates the event from the mutation subject: {@link MuteAction#MUTE} or
     * {@link MuteAction#UNMUTE} from {@code muted}, {@link StatusCategory#GROUP_STATUS}
     * or {@link StatusCategory#REGULAR_STATUS} from the JID server, and the
     * {@link StatusPosterContactType} resolved from the poster contact. The status,
     * viewer, and updates-tab session identifiers are ephemeral UI-session values a
     * headless client cannot observe, so they are fabricated as random positive
     * identifiers; WA Web derives {@code updatesTabSessionId} from the same value as
     * {@code statusSessionId}, which this method preserves. The SMB-only
     * {@code isPosterBiz} and {@code isPosterInAddressBook} fields are set only when the
     * running account is a verified business account, mirroring WA Web's
     * {@code Conn.isSMB} gate.
     *
     * @implNote
     * WA Web emits {@code WAWebStatusMuteWamEvent} from the status-mute UI action
     * ({@code WAWebLogStatusMute.logStatusMute}) on the acting device; Cobalt has no such
     * UI layer, so this implementation emits at the point the mute state flips in the
     * store, which is the single live chokepoint for the feature. The
     * {@link MuteOrigin#STATUS_LIST} origin is assumed because the Updates-tab status
     * list is the canonical place a poster is muted; the exact origin is not recoverable
     * from the app-state mutation. The unified session identifier is rendered as a
     * decimal string of a random positive value, matching the numeric shape WA Web's
     * {@code UnifiedSessionManager} produces.
     *
     * @param client the client whose store supplies the account business flag
     * @param wid    the muted contact or group JID
     * @param muted  {@code true} for a mute, {@code false} for an unmute
     * @param poster the resolved poster contact, or {@code null} for a group subject
     */
    private void emitStatusMuteEvent(LinkedWhatsAppClient client, Jid wid, boolean muted, Contact poster) {
        var random = ThreadLocalRandom.current();
        var statusSessionId = random.nextLong(1L, 1L << 53);
        var statusViewerSessionId = random.nextLong(1L, 1L << 53);
        var unifiedSessionId = Long.toUnsignedString(random.nextLong(1L, 1L << 53));
        var isGroup = wid.hasServer(JidServer.groupOrCommunity());
        var builder = new StatusMuteEventBuilder()
                .muteAction(muted ? MuteAction.MUTE : MuteAction.UNMUTE)
                .muteOrigin(MuteOrigin.STATUS_LIST)
                .statusCategory(isGroup ? StatusCategory.GROUP_STATUS : StatusCategory.REGULAR_STATUS)
                .statusPosterContactType(resolvePosterContactType(isGroup, poster))
                .statusSessionId(statusSessionId)
                .statusViewerSessionId(statusViewerSessionId)
                .updatesTabSessionId(statusSessionId)
                .unifiedSessionId(unifiedSessionId);
        if (client.store().accountStore().verifiedName().isPresent()) {
            builder.isPosterInAddressBook(poster != null && poster.fullName().isPresent())
                    .isPosterBiz(poster != null && poster.hostedOnFacebook());
        }
        wamService.commit(builder.build());
    }

    /**
     * Resolves the {@link StatusPosterContactType} for a muted status poster.
     *
     * <p>Returns {@link StatusPosterContactType#CONTACT} when the poster is a saved
     * address-book contact (evidenced by a resolved full name) and
     * {@link StatusPosterContactType#UNKNOWN} for a group subject or an unsaved poster.
     *
     * @implNote
     * This implementation collapses WA Web's {@code TRUSTED_GROUP_MEMBER} branch to
     * {@link StatusPosterContactType#UNKNOWN} because Cobalt's
     * {@link GroupMetadata} carries no
     * group-trust flag, and reads address-book membership from {@link Contact#fullName()}
     * presence in place of WA Web's {@code getIsMyContact} check.
     *
     * @param isGroup whether the muted subject is a group
     * @param poster  the resolved poster contact, or {@code null} for a group subject
     * @return the poster contact-type classification
     */
    private static StatusPosterContactType resolvePosterContactType(boolean isGroup, Contact poster) {
        if (isGroup) {
            return StatusPosterContactType.UNKNOWN;
        }
        return poster != null && poster.fullName().isPresent()
                ? StatusPosterContactType.CONTACT
                : StatusPosterContactType.UNKNOWN;
    }
}
