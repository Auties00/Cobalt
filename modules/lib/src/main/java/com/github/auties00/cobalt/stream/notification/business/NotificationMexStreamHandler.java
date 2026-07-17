package com.github.auties00.cobalt.stream.notification.business;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.ack.NackReason;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientPasskeyAuthenticator;
import com.github.auties00.cobalt.exception.linked.WhatsAppIntegrityChallengeException;
import com.github.auties00.cobalt.listener.linked.LinkedContactTextStatusListener;
import com.github.auties00.cobalt.listener.linked.LinkedIntegrityChallengeListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.integrity.IntegrityChallenge;
import com.github.auties00.cobalt.wire.stanza.mex.json.misc.IntegrityChallengeResponseMexRequest;
import com.github.auties00.cobalt.wire.stanza.mex.json.misc.IntegrityChallengeResponseMexResponse;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatusBuilder;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterViewerRole;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.MessageCappingEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.MessageCappingActionType;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Handles {@code type="mex"} notifications carrying server-pushed Meta Exchange GraphQL subscription updates.
 *
 * <p>Dispatched by {@link NotificationBusinessDispatcher}. The {@code <update op_name="..."/>} child carries a JSON
 * payload, and the {@code op_name} attribute selects the per-operation handler: newsletter mutations, text-status
 * updates, group property updates, community-owner updates, username changes, and LID changes. Every notification is
 * acknowledged at the transport layer: a successful dispatch sends a positive {@code <ack type="mex"/>}, while a fatal
 * extension error, a {@code null} {@code data} member, a known-but-unsupported operation, or any handler failure sends
 * an {@code <ack type="mex" error="487"/>} ({@link NackReason#PARSING_ERROR}); an operation with no registered handler
 * raises {@link MissingMexNotificationHandlerException} and sends an {@code <ack type="mex" error="488"/>}
 * ({@link NackReason#UNRECOGNIZED_STANZA}). The transport ack is mandatory: a notification left unacknowledged makes the
 * server eventually close the stream with a {@code <stream:error>} that names the outstanding id.
 *
 * @implNote
 * This implementation collapses WhatsApp Web's per-operation Meta Exchange handlers into one Cobalt handler that walks
 * the JSON payload directly, because the {@code <update>} content is plain JSON rather than a SMAX-encoded sub-stanza
 * and a typed parser would add no validation beyond a key check.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMexNotification")
final class NotificationMexStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link NotificationMexStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationMexStreamHandler.class);

    /**
     * Names the operations WhatsApp Web recognises but Cobalt has no consumer for.
     *
     * <p>Hitting one of these emits a warning log; hitting any other unknown op emits an error log so the operation can
     * be sampled for further inspection.
     */
    private static final Set<String> KNOWN_UNSUPPORTED_OPS = Set.of(
            "NotificationLinkedProfilesUpdatesSideSub",
            "NotificationAgeCollection",
            "NotificationLinkedProfilesUpdates"
    );

    /**
     * Reads the store, runs server queries (newsletter metadata, push name, about), and sends acks.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Migrates a contact's phone-number-to-LID mapping atomically across the store on a LID-change event.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * Ships the post-processing {@code <ack class="notification" type="mex"/>} stanza on success.
     */
    private final AckSender ackSender;

    /**
     * Answers passkey integrity challenges, or {@code null} when no authenticator is configured.
     */
    private final LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator;

    /**
     * Commits the {@link MessageCappingEventBuilder} telemetry when a new-chat message-capping info notification
     * arrives.
     */
    private final WamService wamService;

    /**
     * Constructs the handler with shared dependencies.
     *
     * <p>Called once by {@link NotificationBusinessDispatcher}.
     *
     * @param whatsapp            the client used for store reads, server queries, and acks
     * @param lidMigrationService the LID migration service used by {@link #handleLidChange(JSONObject)}
     * @param ackSender           the ack sender used for the success ack
     * @param passkeyAuthenticator the passkey authenticator used to answer integrity challenges, or
     *                            {@code null} when none is configured
     * @param wamService          the telemetry service used by {@link #handleMessageCappingInfo(JSONObject)}
     */
    NotificationMexStreamHandler(LinkedWhatsAppClient whatsapp, LidMigrationService lidMigrationService, AckSender ackSender, LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator, WamService wamService) {
        this.whatsapp = whatsapp;
        this.lidMigrationService = lidMigrationService;
        this.ackSender = ackSender;
        this.passkeyAuthenticator = passkeyAuthenticator;
        this.wamService = wamService;
    }

    /**
     * Validates the stanza shape and delegates to {@link #handleNotification(Stanza)}.
     *
     * <p>Stanzas whose description is not {@code notification} or whose {@code type} is not {@code mex} are silently
     * dropped.
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification") || !stanza.hasAttribute("type", "mex")) {
            return;
        }

        handleNotification(stanza);
    }

    /**
     * Parses the {@code <update>} child's JSON payload, validates it for fatal extension errors, and dispatches to the
     * per-operation handler.
     *
     * <p>A successful dispatch sends a positive ack. An operation among {@link #KNOWN_UNSUPPORTED_OPS} is warned about
     * and nacked with {@link NackReason#PARSING_ERROR}; an operation with no registered handler at all is logged at
     * error level and nacked with {@link NackReason#UNRECOGNIZED_STANZA}; any other failure is warned about and nacked
     * with {@link NackReason#PARSING_ERROR}. The transport ack is always sent so the server never closes the stream on
     * an outstanding notification.
     *
     * @param stanza the {@code <notification>} stanza
     */
    private void handleNotification(Stanza stanza) {
        var updateNode = stanza.getChild("update").orElse(null);
        if (updateNode == null) {
            return;
        }

        var stanzaId = stanza.getAttributeAsString("id", null);
        var stanzaFrom = stanza.getAttributeAsJid("from", null);
        var operationName = updateNode.getAttributeAsString("op_name", "");
        var payload = parsePayload(updateNode);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "[mex] handling notification id={0} op={1}", stanzaId, operationName);
        }

        try {
            dispatch(operationName, stanzaId, stanzaFrom, payload);
        } catch (MissingMexNotificationHandlerException e) {
            if (KNOWN_UNSUPPORTED_OPS.contains(e.operationName())) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "[mex] handleMexNotification: {0} unsupported, nack", e.operationName());
                }
                sendNotificationNack(stanzaId, stanzaFrom, NackReason.PARSING_ERROR);
            } else {
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR,
                            "[mex] handleMexNotification: {0} unknown op, nack", e.operationName());
                }
                sendNotificationNack(stanzaId, stanzaFrom, NackReason.UNRECOGNIZED_STANZA);
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "cannot handle mex notification id=" + (stanzaId != null ? stanzaId : "<missing>"),
                        throwable);
            }
            sendNotificationNack(stanzaId, stanzaFrom, NackReason.PARSING_ERROR);
        }
    }

    /**
     * Routes the operation to its handler, validates the payload, and sends the ack on success.
     *
     * <p>The payload is rejected with a {@link NackReason#PARSING_ERROR} ack when it carries a fatal extension error or
     * a {@code null} {@code data} member. The recognised op names map to newsletter, text-status, group,
     * community-owner, and username/LID handlers; the integrity-challenge op is answered with the configured passkey
     * authenticator; the message-capping-info op commits capping telemetry through
     * {@link #handleMessageCappingInfo(JSONObject)}; remaining UI-only ops (brigading, limit sharing, reachout
     * timelock) are debug-logged because Cobalt has no equivalent UI surface; any other op name raises
     * {@link MissingMexNotificationHandlerException}.
     *
     * @param operationName the Meta Exchange op name from the {@code op_name} attribute
     * @param stanzaId      the stanza id used in the ack
     * @param stanzaFrom    the {@code from} JID used in the ack
     * @param payload       the parsed JSON payload
     * @throws MissingMexNotificationHandlerException when no handler exists for {@code operationName}
     */
    private void dispatch(String operationName, String stanzaId, Jid stanzaFrom, JSONObject payload) {
        var errors = payload.getJSONArray("errors");
        if (hasFatalExtensionError(errors)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "[mex] fatal extension error in mex notification for operation {0}", operationName);
            }
            sendNotificationNack(stanzaId, stanzaFrom, NackReason.PARSING_ERROR);
            return;
        }

        var data = payload.get("data");
        if (data == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "[mex] null data in parsed json for operation {0}", operationName);
            }
            sendNotificationNack(stanzaId, stanzaFrom, NackReason.PARSING_ERROR);
            return;
        }

        switch (operationName) {
            case "MexNotificationEvent" -> {
            }
            case "NotificationNewsletterUserSettingChange",
                 "NotificationNewsletterJoin",
                 "NotificationNewsletterLeave",
                 "NotificationNewsletterStateChange",
                 "NotificationNewsletterAdminMetadataUpdate",
                 "NotificationNewsletterOwnerUpdate",
                 "NotificationNewsletterUpdate",
                 "NotificationNewsletterAdminPromote",
                 "NotificationNewsletterAdminDemote",
                 "NotificationNewsletterAdminInviteRevoke",
                 "NotificationNewsletterWamoSubStatusChange",
                 "NewsletterResponseStateUpdate",
                 "NotificationNewsletterBlockUser",
                 "NotificationNewsletterPaidPartnershipUpdate",
                 "NotificationNewsletterMilestone" -> handleNewsletterOperation(operationName, payload);
            case "TextStatusUpdateNotification",
                 "TextStatusUpdateNotificationSideSub" -> handleTextStatusOperation(payload);
            case "NotificationGroupPropertyUpdate",
                 "NotificationGroupHiddenPropertyUpdate",
                 "NotificationGroupSafetyCheckPropertyUpdate",
                 "NotificationGroupMemberLinkPropertyUpdate",
                 "NotificationGroupMemberShareGroupHistoryModePropertyUpdate" ->
                    refreshGroups(payload);
            case "NotificationCommunityOwnerUpdate" ->
                    refreshGroups(payload);
            case "UsernameSetNotification",
                 "UsernameDeleteNotification",
                 "UsernameUpdateNotification",
                 "AccountSyncUsernameNotification",
                 "LidChangeNotification" -> handleUserOperation(operationName, payload);
            case "NotificationUserBrigadingUpdate" -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "ignoring brigading update mex operation (UI-only feature)");
                }
            }
            case "NotificationGroupLimitSharingPropertyUpdate" -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "ignoring limit sharing update mex operation (UI-only feature)");
                }
            }
            case "NotificationUserReachoutTimelockUpdate" -> {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "ignoring reachout timelock update mex operation (UI-only feature)");
                }
            }
            case "NotificationIntegrityChallengeRequest" -> handleIntegrityChallenge(payload);
            case "MessageCappingInfoNotification" -> handleMessageCappingInfo(payload);
            default -> throw new MissingMexNotificationHandlerException(operationName);
        }

        sendNotificationAck(stanzaId, stanzaFrom);
    }

    /**
     * Parses the {@code <update>} child's text content as a JSON object.
     *
     * <p>Returns an empty {@link JSONObject} on blank content or parse failure so that the downstream fatal-error
     * validation always operates on a non-{@code null} object.
     *
     * @param updateStanza the {@code <update>} child of the notification
     * @return the parsed JSON object, never {@code null}
     */
    private JSONObject parsePayload(Stanza updateStanza) {
        var content = updateStanza.toContentString().orElse(null);
        if (content == null || content.isBlank()) {
            return new JSONObject();
        }

        try {
            var parsed = JSON.parse(content);
            if (parsed instanceof JSONObject jsonObject) {
                return jsonObject;
            }
        } catch (Throwable throwable) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "cannot parse mex JSON payload", throwable);
            }
        }

        return new JSONObject();
    }

    /**
     * Returns whether the {@code errors} array contains a fatal extension error.
     *
     * <p>An entry is fatal when its {@code extensions.is_summary} is {@code true} or its
     * {@code extensions.error_code} is non-{@code null}.
     *
     * @implNote
     * This implementation returns {@code true} for any non-empty {@code errors} array even when no entry passes the
     * {@code is_summary} or {@code error_code} check, treating an unrecognised error as fatal.
     *
     * @param errors the {@code errors} JSON array, or {@code null}
     * @return {@code true} when a fatal extension error is present; {@code false} otherwise
     */
    private boolean hasFatalExtensionError(JSONArray errors) {
        if (errors == null || errors.isEmpty()) {
            return false;
        }

        for (var i = 0; i < errors.size(); i++) {
            var error = errors.getJSONObject(i);
            if (error == null) {
                continue;
            }
            var extensions = error.getJSONObject("extensions");
            if (extensions == null) {
                continue;
            }
            if (Boolean.TRUE.equals(extensions.getBoolean("is_summary"))) {
                return true;
            }
            if (extensions.get("error_code") != null) {
                return true;
            }
        }

        return true;
    }

    /**
     * Routes newsletter Meta Exchange operations to per-op helpers.
     *
     * <p>Most newsletter ops resolve to a metadata refresh through {@link #refreshNewsletters(JSONObject)}. The
     * {@code NotificationNewsletterLeave} op removes the newsletter from the store, and
     * {@code NotificationNewsletterStateChange} branches through {@link #handleNewsletterStateChange(JSONObject)}.
     *
     * @param operationName the Meta Exchange op name
     * @param payload       the parsed JSON payload
     */
    private void handleNewsletterOperation(String operationName, JSONObject payload) {
        switch (operationName) {
            case "NotificationNewsletterLeave" -> {
                var newsletterJid = parseNewsletterId(payload, "xwa2_notify_newsletter_on_leave", "id");
                if (newsletterJid != null) {
                    removeNewsletter(newsletterJid);
                }
            }
            case "NotificationNewsletterStateChange" -> handleNewsletterStateChange(payload);
            default -> refreshNewsletters(payload);
        }
    }

    /**
     * Applies a newsletter state-change event by removing, terminating, or refreshing the newsletter depending on the
     * new state.
     *
     * <p>A {@code DELETED} state with {@code is_requestor=true} removes the newsletter locally; a {@code DELETED} state
     * without {@code is_requestor} marks it terminated so the UI can show the "this newsletter no longer exists"
     * tombstone; any other state refreshes the metadata. A payload without the
     * {@code xwa2_notify_newsletter_on_state_change} envelope falls back to a full newsletter refresh.
     *
     * @param payload the parsed JSON payload
     */
    private void handleNewsletterStateChange(JSONObject payload) {
        var root = payload.getJSONObject("xwa2_notify_newsletter_on_state_change");
        if (root == null) {
            refreshNewsletters(payload);
            return;
        }

        var newsletterJid = parseNewsletterId(root, "id");
        if (newsletterJid == null) {
            return;
        }

        var state = root.getJSONObject("state");
        var stateType = state != null ? state.getString("type") : null;
        if ("DELETED".equals(stateType) && Boolean.TRUE.equals(root.getBoolean("is_requestor"))) {
            removeNewsletter(newsletterJid);
            return;
        }

        if ("DELETED".equals(stateType)) {
            markTerminatedNewsletter(newsletterJid);
            return;
        }

        refreshNewsletter(newsletterJid);
    }

    /**
     * Re-queries each user or LID JID in the payload's about text and writes the result to the contact text-status
     * record.
     *
     * <p>Only JIDs on the user or LID server are processed; a failure on one JID is debug-logged and the loop
     * continues.
     *
     * @param payload the parsed JSON payload
     */
    private void handleTextStatusOperation(JSONObject payload) {
        for (var jid : collectJids(payload)) {
            if (!jid.hasUserServer() && !jid.hasLidServer()) {
                continue;
            }

            try {
                var statusText = whatsapp.queryAbout(jid).orElse(null);
                upsertContactTextStatus(jid.toUserJid(), statusText, null, null, null);
            } catch (Throwable throwable) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "cannot refresh text-status data for " + new LogRedactable.User(jid.toString()),
                            throwable);
                }
            }
        }
    }

    /**
     * Routes username and LID-change Meta Exchange operations to per-op helpers.
     *
     * <p>Username operations split into set, delete, update (side-sub), account-sync, and LID change. Each path applies
     * the change to both the contact record and the chat record so the username displays consistently across surfaces.
     *
     * @param operationName the Meta Exchange op name
     * @param payload       the parsed JSON payload
     */
    private void handleUserOperation(String operationName, JSONObject payload) {
        switch (operationName) {
            case "UsernameSetNotification" -> handleUsernameSet(payload);
            case "UsernameDeleteNotification" -> handleUsernameDelete(payload);
            case "UsernameUpdateNotification" -> refreshUsers(payload);
            case "AccountSyncUsernameNotification" -> handleUsernameAccountSync(payload);
            case "LidChangeNotification" -> handleLidChange(payload);
            default -> refreshUsers(payload);
        }
    }

    /**
     * Writes the new username from a {@code xwa2_notify_username_on_change} payload to the contact keyed by LID.
     *
     * @param payload the parsed JSON payload
     */
    private void handleUsernameSet(JSONObject payload) {
        var root = payload.getJSONObject("xwa2_notify_username_on_change");
        if (root == null) {
            return;
        }

        var lid = parseJid(root.getString("lid")).orElse(null);
        var username = root.getString("username");
        if (lid == null) {
            return;
        }

        updateUsername(lid.toUserJid(), username);
    }

    /**
     * Clears the username on the contact keyed by LID from a {@code xwa2_notify_username_delete} payload.
     *
     * @param payload the parsed JSON payload
     */
    private void handleUsernameDelete(JSONObject payload) {
        var root = payload.getJSONObject("xwa2_notify_username_delete");
        if (root == null) {
            return;
        }

        var lid = parseJid(root.getString("lid")).orElse(null);
        if (lid == null) {
            return;
        }

        updateUsername(lid.toUserJid(), null);
    }

    /**
     * Applies the username carried by the {@code xwa2_notify_wa_user} account-sync envelope to the contact keyed by
     * LID.
     *
     * @param payload the parsed JSON payload
     */
    private void handleUsernameAccountSync(JSONObject payload) {
        var root = payload.getJSONObject("xwa2_notify_wa_user");
        if (root == null) {
            return;
        }

        var lid = parseJid(root.getString("lid_jid")).orElse(null);
        if (lid == null) {
            return;
        }

        var usernameInfo = root.getJSONObject("username_info");
        var username = usernameInfo != null ? usernameInfo.getString("username") : null;
        updateUsername(lid.toUserJid(), username);
    }

    /**
     * Migrates a contact's LID from {@code old} to {@code new} across the store using
     * {@link LidMigrationService#changeLid(Jid, Jid, Jid)}.
     *
     * <p>The phone JID is resolved by first consulting the LID-to-phone mapping, then a contact record, then a chat
     * record. When a phone JID is found the change is delegated to the migration service. When none of the three match,
     * the LID is rewritten in place on both the contact and chat records as a fallback.
     *
     * @implNote
     * This implementation does not call the typed
     * {@link com.github.auties00.cobalt.wire.stanza.mex.json.user.LidChangeNotificationMexResponse} parser because the
     * notification body is inline JSON rather than the IQ-wrapped envelope that parser expects; the {@code old} and
     * {@code new} keys under {@code xwa2_notify_lid_change} are read directly.
     *
     * @param payload the parsed JSON payload
     */
    private void handleLidChange(JSONObject payload) {
        var root = payload.getJSONObject("xwa2_notify_lid_change");
        if (root == null) {
            return;
        }

        var oldLid = parseJid(root.getString("old")).orElse(null);
        var newLid = parseJid(root.getString("new")).orElse(null);
        if (oldLid == null || newLid == null) {
            return;
        }

        var phoneJid = whatsapp.store().contactStore().findPhoneByLid(oldLid.toUserJid()).orElse(null);
        if (phoneJid == null) {
            phoneJid = whatsapp.store().contactStore().findContactByJid(oldLid)
                    .map(Contact::jid)
                    .orElse(null);
        }
        if (phoneJid == null) {
            phoneJid = whatsapp.store().chatStore().findChatByJid(oldLid)
                    .flatMap(Chat::phoneNumberJid)
                    .orElse(null);
        }

        if (phoneJid != null) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "migrating lid {0} -> {1} for phone {2}", oldLid, newLid, phoneJid);
            }
            lidMigrationService.changeLid(phoneJid, newLid.toUserJid(), oldLid.toUserJid());
            return;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "rewriting lid {0} -> {1} in place, no phone mapping found", oldLid, newLid);
        }
        whatsapp.store().contactStore().findContactByJid(oldLid)
                .ifPresent(contact -> contact.setLid(newLid.toUserJid()));
        whatsapp.store().chatStore().findChatByJid(oldLid)
                .ifPresent(chat -> chat.setLid(newLid.toUserJid()));
    }

    /**
     * Answers a server-pushed integrity checkpoint.
     *
     * <p>Parses the challenge, notifies {@link LinkedIntegrityChallengeListener} observers, and then
     * attempts to satisfy it on a separate virtual thread so the transport ack is not blocked by the
     * potentially slow assertion ceremony. A {@code PASSKEY} challenge is answered with the configured
     * {@link LinkedWhatsAppClientPasskeyAuthenticator}; any other type, a missing authenticator, a
     * server rejection, or a ceremony failure logs the session out.
     *
     * @param payload the parsed JSON payload
     */
    private void handleIntegrityChallenge(JSONObject payload) {
        var data = payload.getJSONObject("data");
        var root = data != null ? data.getJSONObject("xwa2_notify_integrity_challenge") : null;
        if (root == null) {
            root = payload.getJSONObject("xwa2_notify_integrity_challenge");
        }
        if (root == null) {
            return;
        }

        var challenge = parseIntegrityChallenge(root);
        if (challenge == null) {
            return;
        }

        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedIntegrityChallengeListener typed) {
                Thread.startVirtualThread(() -> typed.onIntegrityChallenge(whatsapp, challenge));
            }
        }

        Thread.startVirtualThread(() -> satisfyIntegrityChallenge(challenge));
    }

    /**
     * Decodes the {@code xwa2_notify_integrity_challenge} envelope into a typed challenge.
     *
     * @param root the {@code xwa2_notify_integrity_challenge} JSON object
     * @return the decoded challenge, or {@code null} when the required fields are absent or the type
     *         is unknown
     */
    private IntegrityChallenge parseIntegrityChallenge(JSONObject root) {
        var type = root.getString("challenge_type");
        if ("PASSKEY".equals(type)) {
            var passkeyChallenge = root.getJSONObject("passkey_challenge");
            var base64 = passkeyChallenge != null ? passkeyChallenge.getString("challenge_base64") : null;
            if (base64 == null) {
                return null;
            }
            return new IntegrityChallenge(IntegrityChallenge.Type.PASSKEY,
                    Base64.getUrlDecoder().decode(base64), null, null);
        }
        if ("CAPTCHA".equals(type)) {
            var captchaChallenge = root.getJSONObject("captcha_challenge");
            var siteKey = captchaChallenge != null ? captchaChallenge.getString("site_key") : null;
            var challengeUrl = captchaChallenge != null ? captchaChallenge.getString("challenge_url") : null;
            return new IntegrityChallenge(IntegrityChallenge.Type.CAPTCHA, null, siteKey, challengeUrl);
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "unknown integrity challenge type {0}", type);
        }
        return null;
    }

    /**
     * Satisfies a passkey integrity challenge or logs the session out when it cannot.
     *
     * <p>Runs on its own virtual thread. When a {@link LinkedWhatsAppClientPasskeyAuthenticator} is
     * configured and the challenge is a passkey challenge, it asserts the credential, submits the
     * assertion through {@link IntegrityChallengeResponseMexRequest}, and treats anything other than a
     * truthy {@code success} as a rejection. A missing authenticator, an unsupported challenge type, a
     * rejection, or a thrown error logs the session out.
     *
     * @param challenge the challenge to satisfy
     */
    private void satisfyIntegrityChallenge(IntegrityChallenge challenge) {
        var authenticator = passkeyAuthenticator;
        if (challenge.type() != IntegrityChallenge.Type.PASSKEY || authenticator == null) {
            whatsapp.handleFailure(new WhatsAppIntegrityChallengeException(authenticator == null
                    ? "No passkey authenticator is configured to answer the integrity checkpoint"
                    : "Cannot answer integrity challenge of type " + challenge.type()));
            return;
        }

        try {
            var request = new LinkedWhatsAppClientPasskeyAuthenticator.Request(
                    LinkedWhatsAppClientPasskeyAuthenticator.Request.WHATSAPP_RP_ID,
                    challenge.passkeyChallenge().orElseThrow(),
                    new byte[0][],
                    LinkedWhatsAppClientPasskeyAuthenticator.UserVerification.PREFERRED,
                    Duration.ofMinutes(10),
                    "whatsapp-challenge".getBytes(StandardCharsets.UTF_8),
                    LinkedWhatsAppClientPasskeyAuthenticator.Request.WEB_ORIGIN);
            var assertion = authenticator.assertCredential(request);
            var signedChallenge = buildAssertionJson(assertion).getBytes(StandardCharsets.UTF_8);
            var response = whatsapp.sendNode(new IntegrityChallengeResponseMexRequest(
                    signedChallenge, assertion.prfOutput() != null));
            var accepted = IntegrityChallengeResponseMexResponse.of(response)
                    .flatMap(IntegrityChallengeResponseMexResponse::success)
                    .orElse(false);
            if (!accepted) {
                whatsapp.handleFailure(new WhatsAppIntegrityChallengeException(
                        "Server rejected the integrity challenge response"));
            }
        } catch (Throwable throwable) {
            whatsapp.handleFailure(new WhatsAppIntegrityChallengeException(
                    "Cannot satisfy the integrity checkpoint", throwable));
        }
    }

    /**
     * Serialises a WebAuthn assertion into the JSON shape the integrity submit mutation expects.
     *
     * <p>Every binary field is URL-safe base64 encoded without padding, {@code user_handle} becomes an
     * empty string when absent, and {@code prf_output} is emitted as {@code null} when the PRF was not
     * evaluated, matching the genuine client's assertion envelope.
     *
     * @param assertion the assertion to serialise
     * @return the assertion JSON string
     */
    private String buildAssertionJson(LinkedWhatsAppClientPasskeyAuthenticator.Assertion assertion) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var json = new JSONObject();
        json.put("credential_id", encoder.encodeToString(assertion.credentialId()));
        json.put("raw_id", encoder.encodeToString(assertion.credentialId()));
        json.put("type", "public-key");
        json.put("authenticator_data", encoder.encodeToString(assertion.authenticatorData()));
        json.put("client_data_json", encoder.encodeToString(assertion.clientDataJson()));
        json.put("signature", encoder.encodeToString(assertion.signature()));
        json.put("user_handle", assertion.userHandle() != null
                ? encoder.encodeToString(assertion.userHandle()) : "");
        json.put("prf_output", assertion.prfOutput() != null
                ? encoder.encodeToString(assertion.prfOutput()) : null);
        return JSON.toJSONString(json, JSONWriter.Feature.WriteNulls);
    }

    /**
     * Commits capping telemetry when a new-chat message-capping info notification arrives.
     *
     * <p>The server pushes the account's current messaging quota under the
     * {@code xwa2_notify_new_chat_messages_capping_info_update} envelope. Cobalt has no capping UI and does not persist
     * the quota state, so the notification is acknowledged and recorded as a single received-event: a
     * {@link MessageCappingActionType#API} action targeting {@code capping_notification_received} whose
     * {@code extraAttributes} carries the raw capping-info object under a {@code capping_info} key, matching the shape
     * WhatsApp Web serialises.
     *
     * @implNote
     * This implementation emits only the received-event; WhatsApp Web additionally logs a
     * {@link MessageCappingActionType#DEBUG} {@code capping_info_not_saved_on_client} event when the incoming quota is
     * staler than the client's persisted copy, which Cobalt cannot reproduce because it keeps no client-side capping
     * record to compare against.
     *
     * @param payload the parsed JSON payload
     */
    @WhatsAppWebExport(moduleName = "WAWebNewChatMessageCappingNotificationHandler", exports = "mexHandleNewChatMessageCappingNotification", adaptation = WhatsAppAdaptation.ADAPTED)
    private void handleMessageCappingInfo(JSONObject payload) {
        var cappingInfo = extractCappingInfo(payload);
        var extraAttributes = new JSONObject();
        if (cappingInfo != null) {
            extraAttributes.put("capping_info", cappingInfo);
        }
        wamService.commit(new MessageCappingEventBuilder()
                .messageCappingActionType(MessageCappingActionType.API)
                .userActionTarget("capping_notification_received")
                .extraAttributes(JSON.toJSONString(extraAttributes))
                .build());
    }

    /**
     * Reads the {@code xwa2_notify_new_chat_messages_capping_info_update} envelope from the notification payload.
     *
     * <p>The envelope sits under the {@code data} member the server wraps every Meta Exchange notification in; a
     * top-level copy is accepted as a fallback for payloads delivered without the {@code data} wrapper.
     *
     * @param payload the parsed JSON payload
     * @return the capping-info object, or {@code null} when absent
     */
    private JSONObject extractCappingInfo(JSONObject payload) {
        var data = payload.getJSONObject("data");
        var cappingInfo = data != null
                ? data.getJSONObject("xwa2_notify_new_chat_messages_capping_info_update")
                : null;
        if (cappingInfo == null) {
            cappingInfo = payload.getJSONObject("xwa2_notify_new_chat_messages_capping_info_update");
        }
        return cappingInfo;
    }

    /**
     * Re-queries the metadata for every group or community JID found in the payload.
     *
     * <p>Used by every {@code NotificationGroup*PropertyUpdate} op and by {@code NotificationCommunityOwnerUpdate}. A
     * failure on one JID is debug-logged and the loop continues.
     *
     * @param payload the parsed JSON payload
     */
    private void refreshGroups(JSONObject payload) {
        for (var jid : collectJids(payload)) {
            if (!jid.hasGroupOrCommunityServer()) {
                continue;
            }

            try {
                whatsapp.queryChatMetadata(jid);
            } catch (Throwable throwable) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "cannot refresh group metadata for " + new LogRedactable.User(jid.toString()),
                            throwable);
                }
            }
        }
    }

    /**
     * Refreshes every newsletter JID found in the payload.
     *
     * <p>Used by the catch-all newsletter branch when no specific handler matches the op name.
     *
     * @param payload the parsed JSON payload
     */
    private void refreshNewsletters(JSONObject payload) {
        for (var jid : collectJids(payload)) {
            if (!jid.hasNewsletterServer()) {
                continue;
            }

            refreshNewsletter(jid);
        }
    }

    /**
     * Refreshes the push name for every user, LID, or bot JID found in the payload.
     *
     * <p>Used by the {@code UsernameUpdateNotification} side-sub variant and by the default user-op branch. A contact
     * record is created when none exists; a failure on one JID is debug-logged and the loop continues.
     *
     * @param payload the parsed JSON payload
     */
    private void refreshUsers(JSONObject payload) {
        for (var jid : collectJids(payload)) {
            if (!jid.hasUserServer() && !jid.hasLidServer() && !jid.hasBotServer()) {
                continue;
            }

            var userJid = jid.toUserJid();
            var contact = whatsapp.store().contactStore().findContactByJid(userJid)
                    .orElseGet(() -> whatsapp.store().contactStore().addNewContact(userJid));
            try {
                whatsapp.queryName(userJid).ifPresent(contact::setChosenName);
            } catch (Throwable throwable) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG,
                            "cannot refresh username metadata for " + new LogRedactable.User(userJid.toString()),
                            throwable);
                }
            }
        }
    }

    /**
     * Writes the username on the contact and its associated chat, preferring the phone-number-keyed record when a
     * LID-to-phone mapping exists.
     *
     * @param lidJid   the LID JID of the contact
     * @param username the new username, or {@code null} to clear
     */
    private void updateUsername(Jid lidJid, String username) {
        var phoneJid = whatsapp.store().contactStore().findPhoneByLid(lidJid).orElse(null);
        var contact = whatsapp.store().contactStore().findContactByJid(lidJid)
                .or(() -> phoneJid != null ? whatsapp.store().contactStore().findContactByJid(phoneJid) : Optional.empty())
                .orElseGet(() -> whatsapp.store().contactStore().addNewContact(phoneJid != null ? phoneJid : lidJid));
        contact.setLid(lidJid);
        contact.setUsername(username);
        whatsapp.store().contactStore().addContact(contact);

        whatsapp.store().chatStore().findChatByJid(lidJid)
                .or(() -> phoneJid != null ? whatsapp.store().chatStore().findChatByJid(phoneJid) : Optional.empty())
                .ifPresent(chat -> {
                    chat.setLid(lidJid);
                    chat.setUsername(username);
                });
    }

    /**
     * Reads a newsletter JID nested under {@code payload[rootKey][idKey]}.
     *
     * @param payload the JSON payload to descend
     * @param rootKey the outer key (envelope name)
     * @param idKey   the inner key naming the id field
     * @return the parsed newsletter JID, or {@code null} when missing or unparsable
     */
    private Jid parseNewsletterId(JSONObject payload, String rootKey, String idKey) {
        var root = payload.getJSONObject(rootKey);
        if (root == null) {
            return null;
        }
        return parseNewsletterId(root, idKey);
    }

    /**
     * Reads a newsletter id from the given JSON object, appending the {@code @newsletter} server when the value is
     * bare.
     *
     * <p>Accepts both forms the server emits: fully-qualified {@code 123@newsletter} and bare {@code 123}.
     *
     * @param payload the JSON object to read from
     * @param idKey   the id key
     * @return the parsed newsletter JID, or {@code null} when missing or blank
     */
    private Jid parseNewsletterId(JSONObject payload, String idKey) {
        var id = payload.getString(idKey);
        if (id == null || id.isBlank()) {
            return null;
        }

        if (id.contains("@")) {
            return parseJid(id).orElse(null);
        }

        return parseJid(id + "@newsletter").orElse(null);
    }

    /**
     * Recursively walks a JSON value and collects every string that parses as a {@link Jid}.
     *
     * <p>Used by {@link #refreshGroups(JSONObject)}, {@link #refreshNewsletters(JSONObject)},
     * {@link #refreshUsers(JSONObject)}, and {@link #handleTextStatusOperation(JSONObject)} to extract the affected ids
     * without coupling to specific envelope shapes.
     *
     * @param value the JSON value to scan (an object, array, or string)
     * @return an insertion-ordered set of parsed JIDs
     */
    private Set<Jid> collectJids(Object value) {
        var result = new LinkedHashSet<Jid>();
        collectJids(value, result);
        return result;
    }

    /**
     * Accumulates JIDs into the supplied set as part of the recursive walk started by {@link #collectJids(Object)}.
     *
     * @param value  the current JSON value
     * @param result the accumulator set
     */
    private void collectJids(Object value, Set<Jid> result) {
        if (value == null) {
            return;
        }

        if (value instanceof String stringValue) {
            parseJid(stringValue).ifPresent(result::add);
            return;
        }

        if (value instanceof JSONObject jsonObject) {
            jsonObject.forEach((ignored, nestedValue) -> collectJids(nestedValue, result));
            return;
        }

        if (value instanceof JSONArray jsonArray) {
            jsonArray.forEach(item -> collectJids(item, result));
        }
    }

    /**
     * Parses a string as a {@link Jid}, returning {@link Optional#empty()} for blank or {@code @}-less values.
     *
     * @param value the string to parse
     * @return the parsed JID, or empty
     */
    private Optional<Jid> parseJid(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            return Optional.empty();
        }

        try {
            return Optional.of(Jid.of(value));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    /**
     * Creates or merges a {@link ContactTextStatus} record and fires the change to listeners.
     *
     * @param contactJid               the contact JID
     * @param text                     the new text, or {@code null}
     * @param emoji                    the new emoji, or {@code null}
     * @param ephemeralDurationSeconds the new ephemeral duration in seconds, or {@code null}
     * @param lastUpdateTime           the new last-update time, or {@code null}
     * @return the merged text-status record
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
     * Fires {@link LinkedWhatsAppClientListener#onContactTextStatus(LinkedWhatsAppClient, Jid, ContactTextStatus)}
     * on every registered listener on its own virtual thread.
     *
     * @param contactJid the JID whose text status changed
     * @param status     the updated text-status record
     */
    private void notifyContactTextStatusChanged(Jid contactJid, ContactTextStatus status) {
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedContactTextStatusListener typed) {
                Thread.startVirtualThread(() -> typed.onContactTextStatus(whatsapp, contactJid, status));
            }
        }
    }

    /**
     * Returns the newsletter with the given JID, creating a blank record when none exists.
     *
     * @param newsletterJid the newsletter JID
     * @return the matching {@link Newsletter}
     */
    private Newsletter ensureNewsletter(Jid newsletterJid) {
        return whatsapp.store().chatStore().findNewsletterByJid(newsletterJid)
                .orElseGet(() -> whatsapp.store().chatStore().addNewNewsletter(newsletterJid));
    }

    /**
     * Refreshes a newsletter's metadata by re-querying it from the server and applying the result fields.
     *
     * <p>The existing viewer role (subscriber, admin, and so on) is preserved across the refresh by passing it as the
     * query parameter, so a server response that omits the role-specific fields does not downgrade the local record. A
     * {@code null} server response leaves the local record untouched.
     *
     * @param newsletterJid the newsletter JID to refresh
     */
    private void refreshNewsletter(Jid newsletterJid) {
        var newsletter = ensureNewsletter(newsletterJid);
        var role = newsletter.viewerMetadata()
                .map(viewerMetadata -> viewerMetadata.role())
                .filter(existingRole -> existingRole != NewsletterViewerRole.UNKNOWN)
                .orElse(NewsletterViewerRole.GUEST);
        var refreshed = whatsapp.queryNewsletter(newsletterJid, role).orElse(null);
        if (refreshed == null) {
            return;
        }

        newsletter.setState(refreshed.state().orElse(null));
        newsletter.setMetadata(refreshed.metadata().orElse(null));
        newsletter.setViewerMetadata(refreshed.viewerMetadata().orElse(null));
        newsletter.setUnreadMessagesCount(refreshed.unreadMessagesCount());
        newsletter.setTimestamp(refreshed.timestamp().orElse(null));
    }

    /**
     * Marks the newsletter as terminated by flipping the {@code terminated} flag on the metadata.
     *
     * <p>Used when a {@code DELETED} state arrives without {@code is_requestor=true}, meaning the owner deleted the
     * newsletter; the local record is retained so the UI can show a tombstone.
     *
     * @param newsletterJid the newsletter JID to mark as terminated
     */
    private void markTerminatedNewsletter(Jid newsletterJid) {
        var newsletter = ensureNewsletter(newsletterJid);
        var metadata = newsletter.metadata().orElse(null);
        if (metadata == null) {
            metadata = new NewsletterMetadataBuilder().build();
            newsletter.setMetadata(metadata);
        }
        metadata.setTerminated(true);
    }

    /**
     * Removes the newsletter from the local store.
     *
     * <p>Used when the user leaves the newsletter (the {@code NotificationNewsletterLeave} op) and when a
     * {@code DELETED} state arrives with {@code is_requestor=true}.
     *
     * @param newsletterJid the newsletter JID to remove
     */
    private void removeNewsletter(Jid newsletterJid) {
        whatsapp.store().chatStore().removeNewsletter(newsletterJid);
    }

    /**
     * Sends the positive {@code <ack class="notification" type="mex"/>} stanza.
     *
     * <p>Fire-and-forget. Invoked only on successful dispatch. The ack is suppressed when either the stanza id or the
     * {@code from} JID is missing.
     *
     * @param stanzaId   the stanza id
     * @param stanzaFrom the {@code from} JID
     */
    private void sendNotificationAck(String stanzaId, Jid stanzaFrom) {
        if (stanzaId == null || stanzaFrom == null) {
            return;
        }
        var synthetic = new StanzaBuilder()
                .description("notification")
                .attribute("id", stanzaId)
                .attribute("from", stanzaFrom)
                .build();
        ackSender.ack(AckClass.NOTIFICATION, synthetic).type("mex").send();
    }

    /**
     * Sends an {@code <ack class="notification" type="mex" error="N"/>} nack stanza.
     *
     * <p>Fire-and-forget. Invoked on every dispatch failure (fatal extension error, {@code null} data, unsupported or
     * unknown operation, or a handler exception). The nack is suppressed when either the stanza id or the {@code from}
     * JID is missing. The transport ack is required regardless of outcome: leaving a notification unacknowledged makes
     * the server close the stream with a {@code <stream:error>} that names the outstanding id.
     *
     * @param stanzaId   the stanza id
     * @param stanzaFrom the {@code from} JID
     * @param reason     the {@link NackReason} stamped into the {@code error} attribute
     */
    private void sendNotificationNack(String stanzaId, Jid stanzaFrom, NackReason reason) {
        if (stanzaId == null || stanzaFrom == null) {
            return;
        }
        var synthetic = new StanzaBuilder()
                .description("notification")
                .attribute("id", stanzaId)
                .attribute("from", stanzaFrom)
                .build();
        ackSender.ack(AckClass.NOTIFICATION, synthetic).type("mex").error(reason).send();
    }

    /**
     * Signals that {@link #dispatch(String, String, Jid, JSONObject)} encountered an op name no Cobalt handler knows.
     *
     * <p>Caught inside {@link #handleNotification(Stanza)} and turned into an {@code <ack error=...>} nack: a
     * known-but-unsupported op nacks with {@link NackReason#PARSING_ERROR}, any other unknown op with
     * {@link NackReason#UNRECOGNIZED_STANZA}.
     */
    private static final class MissingMexNotificationHandlerException extends RuntimeException {
        /**
         * Holds the op name that had no registered handler.
         */
        private final String operationName;

        /**
         * Constructs the exception for the given op name.
         *
         * @param operationName the op name with no handler
         */
        MissingMexNotificationHandlerException(String operationName) {
            super("MissingMEXNotificationHandler: " + operationName);
            this.operationName = operationName;
        }

        /**
         * Returns the op name that triggered this exception.
         *
         * <p>Read by {@link NotificationMexStreamHandler#handleNotification(Stanza)} to choose between the
         * {@link NotificationMexStreamHandler#KNOWN_UNSUPPORTED_OPS} warning path and the unknown-op error path.
         *
         * @return the op name
         */
        String operationName() {
            return operationName;
        }

        /**
         * {@inheritDoc}
         *
         * @return the {@code "MissingMEXNotificationHandler: <op>"} text
         */
        @Override
        public String toString() {
            return "MissingMEXNotificationHandler: " + operationName;
        }
    }
}
