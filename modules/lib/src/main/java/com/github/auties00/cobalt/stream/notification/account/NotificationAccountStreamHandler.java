package com.github.auties00.cobalt.stream.notification.account;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedAboutChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedContactBlockedListener;
import com.github.auties00.cobalt.listener.linked.LinkedProfilePictureChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedTosNoticesChangedListener;
import com.github.auties00.cobalt.listener.linked.LinkedContactTextStatusListener;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatusBuilder;
import com.github.auties00.cobalt.wire.linked.device.sync.PendingDeviceSync;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncContext;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Handles {@code type="account_sync"} notifications carrying server-side mutations to the authenticated
 * account's own settings.
 *
 * <p>Each notification carries exactly one typed child element that selects the side-effect:
 * {@code status} (about refresh), {@code text_status} (text-status upsert), {@code privacy} (app-state
 * pull), {@code devices} (inline device-list update), {@code blocklist} (blocklist reconciliation),
 * {@code picture} (profile-picture refresh), {@code disappearing_mode} (global ephemeral-timer change),
 * {@code tos} (TOS notice acknowledgement), {@code notice} (single accepted-notice record), {@code user}
 * (AI-availability flag), and {@code biz_opt_out_list} (business opt-out reconciliation). The protocol
 * ACK is always sent in the {@code finally} block of {@link #handle(Stanza)} regardless of mutation
 * success.</p>
 *
 * @implNote This implementation surfaces every mutation through the typed Cobalt store API and fires
 * listener callbacks, whereas WA Web fans the same parsed data out to its frontend pipeline. Cobalt ACKs
 * unconditionally, matching the WA Web ack-promise return path which constructs the ack stanza before
 * dispatching to the per-type branch.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleAccountSyncNotification")
final class NotificationAccountStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * Logs warnings about unhandled notifications and debug messages about ignored child elements.
     */
    private static final System.Logger LOGGER = Log.get(NotificationAccountStreamHandler.class);

    /**
     * Holds the {@code PDFN_ACCEPTED} notice-stage value used by the {@code notice} child path to
     * distinguish accepted policy disclosures from pending ones.
     *
     * <p>A {@code <notice>} stanza whose {@code stage} attribute equals this value is recorded as an
     * accepted notice id; any other stage is ignored.</p>
     *
     * @implNote This implementation hard-codes the literal {@code "5"} taken from WA Web's
     * {@code WAWebPDFNTypes.NOTICE_STAGES.PDFN_ACCEPTED}.
     */
    private static final String PDFN_ACCEPTED_STAGE = "5";

    /**
     * Holds the client used for store reads, queries (about, picture, blocklist), stanza sends, and
     * listener notifications.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the device service used by the {@code devices} child path to apply the inline device list
     * against the authenticated user's own device cache.
     */
    private final DeviceService deviceService;

    /**
     * Holds the ack sender used to ship the post-processing
     * {@code <ack class="notification" type="account_sync"/>} stanza.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with the shared client, device service, and ack sender.
     *
     * @param whatsapp      the non-{@code null} client used for store and network access
     * @param deviceService the non-{@code null} device service used for device-list mutations
     * @param ackSender     the non-{@code null} ack sender used for the protocol-level {@code <ack>} response
     */
    NotificationAccountStreamHandler(LinkedWhatsAppClient whatsapp, DeviceService deviceService, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService cannot be null");
        this.ackSender = Objects.requireNonNull(ackSender, "ackSender cannot be null");
    }

    /**
     * Validates the stanza shape, dispatches to {@link #handleNotification(Stanza)}, logs any thrown
     * exception, and sends the protocol-level ACK unless the branch deferred it.
     *
     * <p>Stanzas whose description is not {@code notification} or whose {@code type} is not
     * {@code account_sync} are dropped without ACK. A valid stanza is ACKed even when handling throws,
     * except when {@link #handleNotification(Stanza)} reports that the ACK was deferred (the
     * {@code devices} branch during resume-from-restart, where the ACK rides on the queued device sync
     * and is shipped once that sync completes).</p>
     *
     * @implNote This implementation swallows {@link Throwable} from the mutation branch and logs it,
     * whereas WA Web rejects the underlying job promise so the orchestrator can NACK the stanza. Cobalt
     * ACKs in the {@code finally} block because the WA Web ack stanza is constructed at the top of the
     * wrapper before dispatching to the per-type branch; the one exception is the deferred-ack path.
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification") || !stanza.hasAttribute("type", "account_sync")) {
            return;
        }

        var deferAck = false;
        try {
            deferAck = handleNotification(stanza);
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "cannot handle account_sync notification " + stanza.getAttributeAsString("id", "<missing>"),
                    throwable);
        } finally {
            if (!deferAck) {
                sendNotificationAck(stanza);
            }
        }
    }

    /**
     * Selects the per-type branch by iterating the notification children until a recognised tag is
     * found, then delegates to the matching helper.
     *
     * <p>Iterates the children in order and dispatches on the first recognised description, so a stanza
     * never triggers more than one branch. When no child matches any known tag, the stanza is logged at
     * {@code DEBUG} and otherwise ignored.</p>
     *
     * @param stanza the {@code <notification>} stanza
     * @return {@code true} when the matched branch deferred the protocol ack (the {@code devices}
     *         branch when it queues a device sync during resume-from-restart), {@code false} otherwise
     */
    private boolean handleNotification(Stanza stanza) {
        for (var child : stanza.children()) {
            switch (child.description()) {
                case "status" -> {
                    refreshOwnAbout();
                    return false;
                }
                case "text_status" -> {
                    handleTextStatusNotification(stanza, child);
                    return false;
                }
                case "privacy" -> {
                    whatsapp.pullWebAppState(SyncPatchType.REGULAR_LOW, SyncPatchType.REGULAR);
                    return false;
                }
                case "devices" -> {
                    return refreshOwnDevices(stanza);
                }
                case "blocklist" -> {
                    applyBlocklistUsernames(child);
                    whatsapp.refreshBlockList();
                    return false;
                }
                case "picture" -> {
                    refreshOwnPicture();
                    return false;
                }
                case "disappearing_mode" -> {
                    handleDisappearingModeNotification(stanza, child);
                    return false;
                }
                case "tos" -> {
                    handleTosNotification(child);
                    return false;
                }
                case "notice" -> {
                    handleNoticeNotification(child);
                    return false;
                }
                case "user" -> {
                    handleUserNotification(child);
                    return false;
                }
                case "biz_opt_out_list" -> {
                    handleBizOptOutListNotification(child);
                    return false;
                }
                default -> {
                }
            }
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                "ignoring unrecognized account_sync notification {0}",
                stanza.getAttributeAsString("id", "<missing>"));
        return false;
    }

    /**
     * Refreshes the authenticated user's about text by querying the server and notifying listeners when
     * the value changed.
     *
     * <p>Triggered by a {@code <status/>} child, meaning the user changed their about on another device.
     * Queries the current about for the local account, compares it against the stored value, and on a
     * change writes the new value to the store and fires {@link LinkedAboutChangedListener#onAboutChanged}.</p>
     *
     * @implNote This implementation compares the stored about against the queried value before writing,
     * avoiding a redundant listener fire when the server pushes a value Cobalt already has; WA Web fires
     * the frontend event unconditionally when the queried status is non-empty.
     */
    void refreshOwnAbout() {
        var self = whatsapp.store().accountStore().jid().orElse(null);
        if (self == null) {
            return;
        }

        var oldAbout = whatsapp.store().accountStore().selfTextStatus().flatMap(ContactTextStatus::text).orElse("");
        var newAbout = whatsapp.queryAbout(self).orElse("");
        if (Objects.equals(oldAbout, newAbout)) {
            return;
        }

        var newStatus = new ContactTextStatusBuilder()
                .text(newAbout)
                .build();
        whatsapp.store().accountStore().setSelfTextStatus(newStatus);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "own about text changed for {0}", self);
        fireListeners(LinkedAboutChangedListener.class, listener -> listener.onAboutChanged(whatsapp, oldAbout, newAbout));
    }

    /**
     * Applies a text-status change carried inside the {@code <text_status>} child to the local store for
     * the originating user.
     *
     * <p>When the stanza's {@code action} is {@code "modify"} the change applies to the authenticated
     * user via {@link #updateOwnTextStatus(Stanza)}; otherwise it applies to the user named in the
     * stanza's {@code from} attribute (falling back to the local account), reading the inline
     * {@code text}, {@code emoji}, {@code ephemeral_duration_sec}, and {@code last_update_time} fields.
     * Either path drives {@link LinkedContactTextStatusListener#onContactTextStatus} for the changed contact.</p>
     *
     * @implNote This implementation collapses the WA Web {@code action === "modify"} path (which
     * re-queries the text status from the server) into the same stanza-driven update used for the
     * non-modify case, because Cobalt does not maintain a dedicated text-status server query.
     *
     * @param stanza           the {@code <notification>} stanza
     * @param textStatusStanza the {@code <text_status>} child carrying the new values
     */
    private void handleTextStatusNotification(Stanza stanza, Stanza textStatusStanza) {
        var action = textStatusStanza.getAttributeAsString("action", null);
        if ("modify".equals(action)) {
            updateOwnTextStatus(textStatusStanza);
        } else {
            var from = getUserJid(stanza, "from");
            var self = from != null ? from : whatsapp.store().accountStore().jid().map(Jid::toUserJid).orElse(null);
            if (self == null) {
                return;
            }

            var text = textStatusStanza.getAttributeAsString("text", null);
            var emoji = textStatusStanza.getChild("emoji")
                    .flatMap(emojiNode -> emojiNode.getAttributeAsString("content"))
                    .orElse(null);
            var ephemeralDuration = textStatusStanza.getAttributeAsInt("ephemeral_duration_sec", (Integer) null);
            var lastUpdateTimeStr = textStatusStanza.getAttributeAsString("last_update_time", null);
            var lastUpdateTime = lastUpdateTimeStr != null ? Instant.ofEpochSecond(Long.parseLong(lastUpdateTimeStr)) : null;
            upsertContactTextStatus(
                    self,
                    text,
                    emoji,
                    ephemeralDuration,
                    lastUpdateTime
            );
        }
    }

    /**
     * Applies the {@code action="modify"} text-status path by reading the stanza's inline fields and
     * writing them to the authenticated user's text-status record.
     *
     * <p>Reads the same {@code text}, {@code emoji}, {@code ephemeral_duration_sec}, and
     * {@code last_update_time} attributes the non-modify path reads, but targets the local account only.</p>
     *
     * @implNote This implementation duplicates the stanza-attribute parsing rather than delegating to
     * {@link #handleTextStatusNotification(Stanza, Stanza)} because the {@code from} resolution differs (self
     * only) and the parent path also fans out to the from-derived JID.
     *
     * @param textStatusStanza the {@code <text_status>} child stanza
     */
    private void updateOwnTextStatus(Stanza textStatusStanza) {
        var self = whatsapp.store().accountStore().jid().orElse(null);
        if (self == null) {
            return;
        }

        var text = textStatusStanza.getAttributeAsString("text", null);
        var emoji = textStatusStanza.getChild("emoji")
                .flatMap(emojiNode -> emojiNode.getAttributeAsString("content"))
                .orElse(null);
        var ephemeralDuration = textStatusStanza.getAttributeAsInt("ephemeral_duration_sec", (Integer) null);
        var lastUpdateTimeStr = textStatusStanza.getAttributeAsString("last_update_time", null);
        var lastUpdateTime = lastUpdateTimeStr != null ? Instant.ofEpochSecond(Long.parseLong(lastUpdateTimeStr)) : null;
        upsertContactTextStatus(
                self.toUserJid(),
                text,
                emoji,
                ephemeralDuration,
                lastUpdateTime
        );
    }

    /**
     * Reconciles the authenticated user's cached device lists with the inline list carried by the
     * notification's {@code <devices>} child.
     *
     * <p>Reproduces WA Web's {@code WAWebHandleAccountSyncNotification} {@code DEVICES} case. While the
     * resume-from-restart sequence is still running ({@link LinkedWhatsAppStore#isResumeFromRestartComplete()}
     * is {@code false}) the inline payload is not trusted: the sender is stashed as a
     * {@link PendingDeviceSync} carrying this notification's deferred ack, so the authoritative device
     * list is fetched by a USync replay once the socket reaches steady state and the ack is shipped only
     * after that replay succeeds, mirroring WA Web's {@code OfflinePendingDeviceCache.addOfflinePendingDevice}.
     * A payload with no {@code <device>} children triggers a full self USync via
     * {@link DeviceService#syncMyDeviceList()}, matching WA Web's {@code getDevices("notification")}.
     * Otherwise a self sender is fanned out to both the phone-number and LID identities (WA Web's
     * {@code y()} helper) and each record is rebuilt through
     * {@link DeviceService#refreshOwnDeviceList(Jid, Stanza)}, which validates the embedded
     * {@code <key-index-list>} so the record carries the authoritative ADV fingerprint. This drives
     * Signal-session establishment for new companion devices and cleanup for removed ones.</p>
     *
     * @implNote This implementation collapses WA Web's secondary
     * {@code isResumeOnSocketDisconnectInProgress()} branch into the single
     * {@link LinkedWhatsAppStore#isResumeFromRestartComplete()} gate, because Cobalt has no browser-tab
     * concept and therefore no distinct open-tab resume state. It also drops WA Web's
     * {@code cleanupCampaignsWithInvalidDevices} follow-up, as Cobalt has no business-broadcast campaign
     * store. The deferred ack rides on the persisted {@link PendingDeviceSync} rather than WA Web's
     * separate {@code pendingAcks} debounce buffer, so it survives a restart and is shipped by
     * {@link DeviceService#retryPendingSyncs()} once the device fetch completes; a crash before then
     * leaves the notification unacknowledged so the server replays it.
     *
     * @param stanza the {@code <notification>} stanza carrying the {@code devices} child and {@code from} attribute
     * @return {@code true} when the ack was deferred (queued during resume), {@code false} when the ack
     *         should be sent immediately by {@link #handle(Stanza)}
     */
    private boolean refreshOwnDevices(Stanza stanza) {
        var rawFrom = stanza.getAttributeAsJid("from").orElse(null);
        if (rawFrom == null) {
            return false;
        }
        var from = rawFrom.toUserJid();

        var store = whatsapp.store();
        if (!store.connectionStore().isResumeFromRestartComplete()) {
            var notificationId = stanza.getAttributeAsString("id", null);
            if (notificationId == null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "queuing pending device sync for {0} without deferred ack, resume incomplete", from);
                store.syncStore().addPendingDeviceSync(PendingDeviceSync.of(List.of(from), UsyncContext.NOTIFICATION.wireValue()));
                return false;
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "queuing pending device sync for {0} with deferred ack, notification {1}", from, notificationId);
            store.syncStore().addPendingDeviceSync(PendingDeviceSync.ofDeferredAck(
                    List.of(from), UsyncContext.NOTIFICATION.wireValue(), notificationId, rawFrom));
            return true;
        }

        var devicesChild = stanza.getChild("devices").orElse(null);
        if (devicesChild == null || devicesChild.getChildren("device").isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "devices child empty for {0}, triggering full device list sync", from);
            deviceService.syncMyDeviceList();
            return false;
        }

        for (var wid : resolveSelfWids(from)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "refreshing own device list for {0}", wid);
            deviceService.refreshOwnDeviceList(wid, devicesChild);
        }
        return false;
    }

    /**
     * Fans a self account JID out to both the phone-number and LID self identities.
     *
     * <p>When {@code from} is one of this account's own identities, returns the phone-number user JID
     * and the LID user JID (whichever are present) so the caller rebuilds a device-list record under
     * each, matching the dual record WA Web keeps for {@code createDeviceListPK(PN)} and
     * {@code createDeviceListPK(LID)}. When {@code from} is not a self identity the single resolved JID
     * is returned unchanged, and a self account with neither identity resolvable falls back to
     * {@code from} alone.</p>
     *
     * @implNote This implementation mirrors WA Web's {@code y()} helper, which expands a me-account
     * wid into the {@code [PN, LID]} pair and otherwise logs a {@code wid-is-not-self} error and
     * processes the lone wid; Cobalt keeps the expansion but omits the diagnostic log.
     *
     * @param from the notification sender resolved to a user JID
     * @return the self identities to rebuild, or the single input JID when it is not a self account
     */
    private List<Jid> resolveSelfWids(Jid from) {
        var store = whatsapp.store();
        var phoneJid = store.accountStore().jid().map(Jid::toUserJid).orElse(null);
        var lidJid = store.accountStore().lid().map(Jid::toUserJid).orElse(null);
        if (!from.equals(phoneJid) && !from.equals(lidJid)) {
            return List.of(from);
        }
        var wids = new ArrayList<Jid>(2);
        if (phoneJid != null) {
            wids.add(phoneJid);
        }
        if (lidJid != null) {
            wids.add(lidJid);
        }
        return wids.isEmpty() ? List.of(from) : wids;
    }

    /**
     * Writes the {@code username} field of every {@code <item>} child of a {@code <blocklist>}
     * notification to the matching contact record.
     *
     * <p>Iterates each {@code <item jid=... username=.../>} entry, resolves the contact (creating a new
     * record when none exists for the JID), and stores the username so it can display next to the
     * blocked contact's row.</p>
     *
     * @implNote This implementation applies the usernames unconditionally, whereas WA Web reaches this
     * path only when its username-display gating prop is enabled; the Cobalt store has no equivalent
     * gating prop wired here.
     *
     * @param blocklistStanza the {@code <blocklist>} child carrying {@code <item jid=... username=.../>} entries
     */
    private void applyBlocklistUsernames(Stanza blocklistStanza) {
        for (var item : blocklistStanza.getChildren("item")) {
            var username = item.getAttributeAsString("username", null);
            if (username == null) {
                continue;
            }
            var userJid = item.getAttributeAsJid("jid")
                    .map(Jid::toUserJid)
                    .orElse(null);
            if (userJid == null) {
                continue;
            }
            var contact = whatsapp.store().contactStore().findContactByJid(userJid)
                    .orElseGet(() -> whatsapp.store().contactStore().addNewContact(userJid));
            contact.setUsername(username);
            whatsapp.store().contactStore().addContact(contact);
        }
    }

    /**
     * Refreshes the authenticated user's profile-picture URI by querying the server and firing
     * {@link LinkedProfilePictureChangedListener#onProfilePictureChanged} when the URI changed.
     *
     * <p>Triggered when the user updates their profile picture on another paired device. Compares the
     * queried URI against the stored URI and writes plus fires the listener only on a change.</p>
     *
     * @implNote This implementation short-circuits when the queried URI equals the stored URI, whereas WA
     * Web always dispatches the frontend event.
     */
    private void refreshOwnPicture() {
        var self = whatsapp.store().accountStore().jid().orElse(null);
        if (self == null) {
            return;
        }

        var oldPicture = whatsapp.store().accountStore().profilePicture().orElse(null);
        var newPicture = whatsapp.queryPicture(self).orElse(null);
        if (Objects.equals(oldPicture, newPicture)) {
            return;
        }

        whatsapp.store().accountStore().setProfilePicture(newPicture);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "own profile picture changed for {0}", self);
        fireListeners(LinkedProfilePictureChangedListener.class, listener -> listener.onProfilePictureChanged(whatsapp, self));
    }

    /**
     * Applies the global "new chats" ephemeral timer encoded inside the {@code <disappearing_mode>}
     * child of an {@code account_sync} notification.
     *
     * <p>Sets the default ephemeral duration applied to chats created after this point; existing chats
     * retain their per-chat setting. The inline {@code duration} and {@code t} attributes are read only
     * when the child carries no {@code action} attribute.</p>
     *
     * @implNote This implementation consumes only the inline {@code duration} and {@code t} attributes;
     * WA Web's {@code modify} action would re-query the server for the disappearing mode. Cobalt has no
     * equivalent query, so the {@code modify} branch falls through and waits for the next full app-state
     * sync to converge.
     *
     * @param stanza             the {@code <notification>} stanza
     * @param disappearingMode the {@code <disappearing_mode>} child
     */
    private void handleDisappearingModeNotification(Stanza stanza, Stanza disappearingMode) {
        var action = disappearingMode.getAttributeAsString("action", null);
        Integer duration;
        Integer settingTimestamp;
        if (action != null) {
            duration = null;
            settingTimestamp = null;
        } else {
            duration = disappearingMode.getAttributeAsInt("duration", (Integer) null);
            settingTimestamp = disappearingMode.getAttributeAsInt("t", (Integer) null);
        }

        if ("modify".equals(action)) {
            // TODO: implement a disappearing-mode server query; today the modify branch waits for the next full app-state sync to converge.
            var selfJid = getUserJid(stanza, "from");
            if (selfJid != null && Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "deferring disappearing_mode modify for {0} to next app-state sync",
                        selfJid);
            }
        }

        if (duration != null) {
            whatsapp.store().settingsStore().setNewChatsEphemeralTimer(ChatEphemeralTimer.of(duration));
        }
    }

    /**
     * Records every {@code <notice id=... state=.../>} entry under a {@code <tos>} child whose state is
     * anything other than {@code "false"} as an accepted notice id.
     *
     * <p>Collects the accepted ids, merges them into the stored notice-id set, and writes the union back.
     * The accepted ids gate UI elements such as banner dismissal and settings copy.</p>
     *
     * @param tosStanza the {@code <tos>} child containing one or more {@code <notice/>} entries
     */
    private void handleTosNotification(Stanza tosStanza) {
        var notices = new HashSet<String>();
        for (var notice : tosStanza.getChildren("notice")) {
            var state = notice.getAttributeAsString("state", null);
            var accepted = !"false".equals(state);
            var id = notice.getAttributeAsString("id", null);
            if (id != null && accepted) {
                notices.add(id);
            }
        }
        if (!notices.isEmpty()) {
            var currentNotices = new HashSet<>(whatsapp.store().settingsStore().acknowledgedTosNotices());
            currentNotices.addAll(notices);
            whatsapp.store().settingsStore().setAcknowledgedTosNotices(currentNotices);
            var snapshot = Set.copyOf(currentNotices);
            fireListeners(LinkedTosNoticesChangedListener.class, listener -> listener.onTosNoticesChanged(whatsapp, snapshot));
        }
    }

    /**
     * Records a single {@code <notice/>} child whose stage equals {@link #PDFN_ACCEPTED_STAGE} as an
     * accepted notice id.
     *
     * <p>The standalone {@code <notice/>} stanza arrives independently of the {@code <tos/>} batch path
     * and carries the additional {@code stage}, {@code version}, and {@code t} fields; all four of
     * {@code id}, {@code stage}, {@code version}, and {@code t} must be present or the stanza is ignored.</p>
     *
     * @implNote This implementation persists only the notice id when the stage equals
     * {@link #PDFN_ACCEPTED_STAGE}; WA Web also stores the policy version, but Cobalt's store keeps only
     * the accepted-id set.
     *
     * @param noticeStanza the {@code <notice/>} child stanza
     */
    private void handleNoticeNotification(Stanza noticeStanza) {
        var noticeId = noticeStanza.getAttributeAsString("id", null);
        var noticeStage = noticeStanza.getAttributeAsString("stage", null);
        var noticeVersion = noticeStanza.getAttributeAsString("version", null);
        var noticeTimestamp = noticeStanza.getAttributeAsInt("t", (Integer) null);

        if (noticeId == null || noticeId.isEmpty() || noticeStage == null || noticeVersion == null || noticeTimestamp == null) {
            return;
        }

        var accepted = PDFN_ACCEPTED_STAGE.equals(noticeStage);

        if (accepted) {
            var currentNotices = new HashSet<>(whatsapp.store().settingsStore().acknowledgedTosNotices());
            currentNotices.add(noticeId);
            whatsapp.store().settingsStore().setAcknowledgedTosNotices(currentNotices);
            var snapshot = Set.copyOf(currentNotices);
            fireListeners(LinkedTosNoticesChangedListener.class, listener -> listener.onTosNoticesChanged(whatsapp, snapshot));
        }
    }

    /**
     * Writes the {@code isAiAvailable} boolean derived from the {@code <user state="AI available"/>}
     * child to the store.
     *
     * <p>Gates the in-chat Meta AI affordances when the server-side eligibility flips on.</p>
     *
     * @implNote This implementation persists the flag, whereas WA Web only logs the receipt and relies on
     * the next AB-prop sync to surface the value to the UI.
     *
     * @param userStanza the {@code <user/>} child stanza
     */
    private void handleUserNotification(Stanza userStanza) {
        var isAiAvailable = "AI available".equals(userStanza.getAttributeAsString("state", null));
        whatsapp.store().businessStore().setAiAvailable(isAiAvailable);
    }

    /**
     * Reconciles the local business-opt-out blocklist against the {@code <biz_opt_out_list>} child.
     *
     * <p>Applies each {@code <item action=... biz_jid=.../>} entry to the matching contact's blocked
     * flag and updates the stored hash on success, firing {@link LinkedContactBlockedListener#onContactBlocked}
     * for any contact whose flag changed. The reconciliation is skipped entirely when {@code prev_dhash}
     * does not match the stored hash.</p>
     *
     * @implNote This implementation skips the full opt-out list refresh on a {@code prev_dhash} mismatch
     * and leaves the stored hash untouched (mismatch implies do-not-persist). WA Web fires a full
     * server-side refresh in that case; Cobalt has no equivalent batch endpoint and relies on the next
     * app-state sync to converge.
     *
     * @param optOutListStanza the {@code <biz_opt_out_list>} child carrying {@code dhash}, {@code prev_dhash}, and item children
     */
    private void handleBizOptOutListNotification(Stanza optOutListStanza) {
        var dhash = optOutListStanza.getAttributeAsString("dhash", null);
        var prevDhash = optOutListStanza.getAttributeAsString("prev_dhash", null);
        var storedHash = whatsapp.store().businessStore().businessOptOutListHash().orElse(null);

        if (!Objects.equals(storedHash, prevDhash)) {
            // TODO: trigger a full opt-out list refresh when prev_dhash mismatches; today Cobalt waits for the next app-state sync to converge.
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "biz_opt_out_list prev_dhash mismatch, skipping reconciliation");
            return;
        }

        if (dhash != null) {
            for (var item : optOutListStanza.children()) {
                var action = item.getAttributeAsString("action", null);
                var bizJid = item.getAttributeAsJid("biz_jid", null);
                if (action == null || bizJid == null) {
                    continue;
                }
                var isBlocked = "block".equals(action);
                var userJid = bizJid.toUserJid();
                var contact = whatsapp.store().contactStore().findContactByJid(userJid)
                        .orElseGet(() -> whatsapp.store().contactStore().addNewContact(userJid));
                if (contact.blocked() != isBlocked) {
                    contact.setBlocked(isBlocked);
                    whatsapp.store().contactStore().addContact(contact);
                    fireListeners(LinkedContactBlockedListener.class, listener -> listener.onContactBlocked(whatsapp, userJid));
                }
            }
            whatsapp.store().businessStore().setBusinessOptOutListHash(dhash);
        }
    }

    /**
     * Reads a JID-valued attribute and reduces it to its user form (server-only, no device, no agent).
     *
     * <p>Used by the per-child branches that need to address a user record rather than a device record.</p>
     *
     * @param stanza the stanza to read from
     * @param key  the attribute name
     * @return the parsed user JID, or {@code null} if the attribute is absent or unparsable
     */
    private Jid getUserJid(Stanza stanza, String key) {
        return stanza.getAttributeAsJid(key)
                .map(Jid::toUserJid)
                .orElse(null);
    }

    /**
     * Fans the given callback out to every registered listener of the given
     * type on its own virtual thread.
     *
     * <p>Each listener runs on a fresh virtual thread so a slow listener cannot block the notification
     * stream.</p>
     *
     * @param type     the per-event listener interface to dispatch against
     * @param consumer the callback to invoke against each matching listener
     * @param <L>      the per-event listener interface
     */
    private <L extends WhatsAppListener> void fireListeners(Class<L> type, Consumer<L> consumer) {
        for (var listener : whatsapp.store().listeners()) {
            if (type.isInstance(listener)) {
                var typed = type.cast(listener);
                Thread.startVirtualThread(() -> consumer.accept(typed));
            }
        }
    }

    /**
     * Creates or merges a {@link ContactTextStatus} record for the given contact and fires the change to
     * listeners.
     *
     * <p>Loads the existing record for the canonical user JID (or a fresh one when none exists), overwrites
     * its text, emoji, ephemeral duration, and last-update time, stores the result, and notifies listeners
     * via {@link #notifyContactTextStatusChanged(Jid, ContactTextStatus)}.</p>
     *
     * @param contactJid               the JID of the contact whose record is being updated
     * @param text                     the new about text, or {@code null} to clear
     * @param emoji                    the new emoji, or {@code null} to clear
     * @param ephemeralDurationSeconds the per-text ephemeral duration in seconds, or {@code null}
     * @param lastUpdateTime           the server-reported last update time, or {@code null}
     * @return the merged {@link ContactTextStatus} now stored in the local store
     */
    private ContactTextStatus upsertContactTextStatus(
            Jid contactJid,
            String text,
            String emoji,
            Integer ephemeralDurationSeconds,
            Instant lastUpdateTime
    ) {
        var canonicalJid = contactJid.toUserJid();
        var current = whatsapp.store().contactStore().findContactTextStatus(canonicalJid)
                .orElseGet(() -> new ContactTextStatusBuilder().build());
        current.setText(text);
        current.setEmoji(emoji);
        current.setEphemeralDurationSeconds(ephemeralDurationSeconds);
        current.setLastUpdateTime(lastUpdateTime);
        whatsapp.store().contactStore().addContactTextStatus(canonicalJid, current);
        notifyContactTextStatusChanged(canonicalJid, current);
        return current;
    }

    /**
     * Fires {@link LinkedContactTextStatusListener#onContactTextStatus} on every registered listener, each on its
     * own virtual thread.
     *
     * @param contactJid the JID whose text status changed
     * @param status     the updated text-status record
     */
    private void notifyContactTextStatusChanged(Jid contactJid, ContactTextStatus status) {
        fireListeners(LinkedContactTextStatusListener.class, listener -> listener.onContactTextStatus(whatsapp, contactJid, status));
    }

    /**
     * Sends the {@code <ack class="notification" type="account_sync"/>} stanza required by the server to
     * mark this notification as delivered.
     *
     * <p>The ack is fire-and-forget; a closed socket surfaces as a
     * {@link com.github.auties00.cobalt.exception.linked.WhatsAppSessionException.Closed} which the surrounding
     * {@link #handle(Stanza)} {@code finally} block already isolates from the mutation path.</p>
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza).type("account_sync").send();
    }
}
