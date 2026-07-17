package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppChatStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Helpers shared across the sync-action handlers for building and parsing
 * mutation indices and for moving between JSON index tuples and
 * {@link MessageKey} objects.
 *
 * <p>This utility consolidates the WA Web index modules
 * {@code WAWebSyncdIndexUtils} (orphan-friendly sentinels, message-key
 * round-trip), {@code WAWebSyncdActionUtils} (index serialization,
 * {@code [remote, id, fromMe, participant]} message-key index segments,
 * mutation builder), and {@code WAWebSyncdUtils} (the
 * {@code constructMsgKeySegmentsFromMsgKey} / {@code extractParticipantForSync}
 * pair).
 *
 * @implNote
 * The {@code WAWebSyncdResolveMessages.resolveMessagesForMutations} batch
 * pre-pass that consumes these helpers in WA Web has no Cobalt analogue;
 * Cobalt's handlers resolve their messages inline via
 * {@link LinkedWhatsAppChatStore#findChatByJid(com.github.auties00.cobalt.wire.core.jid.JidProvider)}
 * so the AB-prop driven chunked-vs-sync branch and the IDB existence probe are
 * not replicated.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdIndexUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdActionUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdResolveMessages")
@WhatsAppWebModule(moduleName = "WAWebSyncdUtils")
public final class SyncdIndexUtils {
    /**
     * The slot in a parsed index array that carries the action name.
     *
     * <p>The index array is conventionally laid out as
     * {@code [actionName, ...actionSpecificArgs]} so the zeroth slot is always
     * the action.
     */
    public static final int MUTATION_NAME_INDEX = 0;

    /**
     * The logger for {@link SyncdIndexUtils}.
     */
    private static final System.Logger LOGGER = Log.get(SyncdIndexUtils.class);

    /**
     * Hides the constructor of this utility class.
     *
     * <p>The class only exposes {@code static} helpers; instantiation is
     * pointless.
     */
    private SyncdIndexUtils() {
    }

    /**
     * Serializes an action name and its trailing arguments into the JSON-encoded
     * mutation index used by every sync handler.
     *
     * <p>The variadic argument list is prepended with the action name then
     * serialized; callers prefer this over hand-rolling the JSON string so that
     * future changes to the index format remain a single edit.
     *
     * @param actionName the sync action name (e.g. {@code "archive"}, {@code "pin_v1"})
     * @param indexArgs  the action-specific index arguments, may be empty but not {@code null}
     * @return the JSON-encoded index string
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "buildIndex", adaptation = WhatsAppAdaptation.DIRECT)
    public static String buildIndex(String actionName, String... indexArgs) {
        var parts = new Object[indexArgs.length + 1];
        parts[0] = actionName;
        System.arraycopy(indexArgs, 0, parts, 1, indexArgs.length);
        return JSON.toJSONString(Arrays.asList(parts));
    }

    /**
     * Parses a JSON-encoded mutation index back into its component array.
     *
     * <p>Returns {@code null} for missing, unparseable, or empty indices so
     * callers can take the malformed branch without wrapping the call in a
     * {@code try/catch}. The collection name is used only as a diagnostic tag in
     * the log message.
     *
     * @param collectionName the collection the mutation belongs to (diagnostic only)
     * @param index          the JSON-encoded index string
     * @return the parsed array, or {@code null} if missing, unparseable, or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "parseIndex", adaptation = WhatsAppAdaptation.DIRECT)
    public static JSONArray parseIndex(String collectionName, String index) {
        try {
            var parsed = JSON.parseArray(index);
            if (parsed == null || parsed.size() < 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "invalid empty index for collection={0}", collectionName);
                return null;
            }
            return parsed;
        } catch (Throwable throwable) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "invalid index for collection={0}", collectionName);
            return null;
        }
    }

    /**
     * Extracts the action name slot from a JSON-encoded mutation index.
     *
     * <p>Parses the index through {@link #parseIndex(String, String)} and returns
     * the {@link #MUTATION_NAME_INDEX} slot, or {@code null} when the parse fails.
     *
     * @param collectionName the collection the mutation belongs to (diagnostic only)
     * @param index          the JSON-encoded index string
     * @return the action name, or {@code null} if the index is invalid
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "getMutationNameFromIndex", adaptation = WhatsAppAdaptation.DIRECT)
    public static String getMutationNameFromIndex(String collectionName, String index) {
        var parsed = parseIndex(collectionName, index);
        if (parsed == null) {
            return null;
        }
        return parsed.getString(MUTATION_NAME_INDEX);
    }

    /**
     * Builds the {@code [remoteJid, id, fromMe, participant]} index tuple for
     * message-oriented sync mutations.
     *
     * <p>Targets a single message (star, delete-for-me, mark-as-read on one
     * message). The fourth slot is forced to the literal {@code "0"} when no
     * dedicated participant exists, preserving the four-arity invariant.
     *
     * @param remoteJid   the chat JID (must not be {@code null})
     * @param id          the message id
     * @param fromMe      whether the message was sent by the current user
     * @param participant the participant JID for incoming group messages, or {@code null} otherwise
     * @return the four-element index tuple
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdActionUtils", exports = "buildMessageKey", adaptation = WhatsAppAdaptation.DIRECT)
    public static List<String> buildMessageKey(Jid remoteJid, String id, boolean fromMe, Jid participant) {
        var fromMeStr = fromMe ? "1" : "0";
        var participantStr = participant != null && !fromMe
                ? participant.toString()
                : "0";
        return List.of(
                remoteJid.toString(),
                id,
                fromMeStr,
                participantStr
        );
    }

    /**
     * Builds the four-segment message-key tuple from a {@link ChatMessageInfo}.
     *
     * <p>Computes the sync-action index segments for callers that already hold a
     * full message wrapper, delegating to
     * {@link #constructMsgKeySegmentsFromMsgKey(MessageKey)} on the embedded key.
     *
     * @param info the chat message whose key is being encoded
     * @return the four-element segment list {@code [remote, id, fromMe, participant]}
     * @throws NullPointerException if {@code info} is {@code null}
     * @see #constructMsgKeySegmentsFromMsgKey(MessageKey)
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdUtils", exports = "constructMsgKeySegments", adaptation = WhatsAppAdaptation.DIRECT)
    public static List<String> constructMsgKeySegments(ChatMessageInfo info) {
        Objects.requireNonNull(info, "info cannot be null");
        return constructMsgKeySegmentsFromMsgKey(info.key());
    }

    /**
     * Builds the four-segment message-key tuple from a raw {@link MessageKey}.
     *
     * <p>Differs from {@link #buildMessageKey(Jid, String, boolean, Jid)} in that
     * callers do not need to pre-decompose the key; the participant predicate is
     * applied via {@link #extractParticipantForSync(MessageKey)}.
     *
     * @implNote
     * Remote JID serialization uses the {@code legacy:true} form via
     * {@link #toLegacyJidString(Jid)} so historical {@code c.us} keys remap onto
     * the canonical {@code s.whatsapp.net} wire representation.
     *
     * @param key the message key to encode
     * @return the four-element segment list {@code [remote, id, fromMe, participant]}
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} has no {@code parentJid} or no {@code id}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdUtils", exports = "constructMsgKeySegmentsFromMsgKey", adaptation = WhatsAppAdaptation.DIRECT)
    public static List<String> constructMsgKeySegmentsFromMsgKey(MessageKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        var remoteJid = key.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("key must carry a parentJid"));
        var id = key.id()
                .orElseThrow(() -> new IllegalArgumentException("key must carry an id"));
        var participantSegment = extractParticipantForSync(key);
        return List.of(
                toLegacyJidString(remoteJid),
                id,
                key.fromMe() ? "1" : "0",
                participantSegment
        );
    }

    /**
     * Computes the participant segment of a message-key sync tuple.
     *
     * <p>The participant slot is emitted only when the message has a dedicated
     * sender JID, the remote JID is multi-participant (group, broadcast,
     * newsletter), and the message was not sent by the current user; any other
     * combination collapses to the literal {@code "0"} so the index tuple keeps a
     * fixed arity.
     *
     * @implNote
     * Cobalt reads the raw sender JID through {@link #rawSenderJid(MessageKey)}
     * rather than through {@link MessageKey#senderJid()} because the latter falls
     * back to the parent JID when no explicit sender was stored, which would
     * wrongly emit the chat JID as the participant.
     *
     * @param key the message key whose participant segment is required
     * @return the serialized participant JID, or {@code "0"} when the predicate is not met
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} has no {@code parentJid}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdUtils", exports = "extractParticipantForSync", adaptation = WhatsAppAdaptation.ADAPTED)
    public static String extractParticipantForSync(MessageKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        var remoteJid = key.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("key must carry a parentJid"));
        var rawParticipant = rawSenderJid(key);
        if (rawParticipant != null
                && !isUserJid(remoteJid)
                && !key.fromMe()) {
            return toLegacyJidString(rawParticipant);
        }
        return "0";
    }

    /**
     * Returns the explicit sender JID on a {@link MessageKey}, ignoring the
     * parent-JID fallback.
     *
     * <p>Recovers WA Web's "no dedicated participant" sentinel for
     * {@link #extractParticipantForSync(MessageKey)}.
     *
     * @implNote
     * This implementation discriminates the fallback case by JID equality: when
     * {@link MessageKey#senderJid()} equals {@link MessageKey#parentJid()} the
     * helper assumes the fallback was applied and returns {@code null}.
     *
     * @param key the message key whose raw sender is required
     * @return the raw sender JID, or {@code null} when none was stored
     */
    private static Jid rawSenderJid(MessageKey key) {
        var parent = key.parentJid().orElse(null);
        var sender = key.senderJid().orElse(null);
        if (sender == null) {
            return null;
        }
        if (parent != null && parent.equals(sender)) {
            return null;
        }
        return sender;
    }

    /**
     * Serializes a {@link Jid} in the WA Web {@code legacy:true} form.
     *
     * <p>Sync-action indices are written in the legacy form so historical
     * {@code c.us} keys remain stable across the {@code c.us -> s.whatsapp.net}
     * server domain transition.
     *
     * @implNote
     * This implementation rewrites only the trailing {@code @c.us} server to
     * {@code @s.whatsapp.net}; for any other server the JID's default
     * {@link Jid#toString()} is identical to its legacy form so it is passed
     * through unchanged.
     *
     * @param jid the JID to serialize
     * @return the JID in legacy-wire form
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWid", exports = "toString", adaptation = WhatsAppAdaptation.ADAPTED)
    private static String toLegacyJidString(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        if (!jid.server().equals(JidServer.legacyUser())) {
            return jid.toString();
        }
        var serialized = jid.toString();
        var atIndex = serialized.lastIndexOf('@');
        if (atIndex < 0) {
            return JidServer.user().toString();
        }
        return serialized.substring(0, atIndex + 1) + JidServer.user();
    }

    /**
     * Returns the serialized message-key DB id with the participant segment
     * stripped when the message is an outgoing group/broadcast message.
     *
     * <p>Used to compare local keys against pre-fetched DB id lists via a
     * prefix-match against the WA Web MsgKey serialization. The trailing
     * underscore-separated segment is stripped only when {@code fromMe} is
     * {@code true} and the remote JID is not a user JID.
     *
     * @implNote
     * Cobalt's {@link MessageKey#senderJid()} parent-JID fallback forces this
     * helper to use {@link #serializeMessageKey(MessageKey)} for the full
     * serialization.
     *
     * @param key the message key to convert
     * @return the DB id string with the participant segment removed when applicable
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdIndexUtils", exports = "msgKeyToDbIdWithoutFromMeParticipant", adaptation = WhatsAppAdaptation.ADAPTED)
    static String msgKeyToDbIdWithoutFromMeParticipant(MessageKey key) {
        var serialized = serializeMessageKey(key);
        var remoteJid = key.parentJid().orElse(null);
        if (!key.fromMe() || remoteJid == null || isUserJid(remoteJid)) {
            return serialized;
        }
        var lastUnderscore = serialized.lastIndexOf('_');
        if (lastUnderscore < 0) {
            return serialized;
        }
        return serialized.substring(0, lastUnderscore);
    }

    /**
     * Rebuilds a {@link MessageKey} from the four index parts of a
     * message-oriented sync action.
     *
     * <p>Validates the remote JID is a wid and parses it; for non-user and
     * non-newsletter chats it resolves the participant from either the explicit
     * slot (when {@code fromMe} is {@code "0"}) or from the current user's JID
     * (when {@code fromMe} is {@code "1"}). Invalid inputs return
     * {@link Optional#empty()}. Used by handlers that need to surface a
     * malformed-or-orphan mutation back to the dispatcher in {@link MessageKey}
     * form (e.g. {@link StarMessageHandler} returning a populated
     * {@link MutationApplicationResult#orphan(String, String)}).
     *
     * @param store       the {@link LinkedWhatsAppStore} consulted for the current user's JID
     * @param remote      the chat JID string
     * @param id          the message id string
     * @param fromMe      the {@code fromMe} flag as {@code "0"} or {@code "1"}
     * @param participant the participant JID string, may be {@code "0"} or empty
     * @return the resolved {@link MessageKey}, or empty when the input is invalid
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdIndexUtils", exports = "syncKeyToMsgKey", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<MessageKey> syncKeyToMsgKey(LinkedWhatsAppStore store, String remote, String id, String fromMe, String participant) {
        if (remote == null || remote.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "syncKeyToMsgKey: invalid remote value");
            return Optional.empty();
        }

        Jid remoteJid;
        try {
            remoteJid = Jid.of(remote);
        } catch (Exception e) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "syncKeyToMsgKey: invalid remote value={0}", new LogRedactable.User(remote));
            return Optional.empty();
        }

        Jid participantJid = null;
        var isUser = isUserJid(remoteJid);
        var isNewsletter = remoteJid.hasNewsletterServer();
        if (!isUser && !isNewsletter) {
            if ("1".equals(fromMe)) {
                participantJid = store.accountStore().jid().orElse(null);
            } else {
                if (participant == null || participant.isEmpty()) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "syncKeyToMsgKey: invalid participant value");
                    return Optional.empty();
                }
                try {
                    participantJid = Jid.of(participant);
                } catch (Exception e) {
                    if (Log.WARNING)
                        LOGGER.log(Level.WARNING, "syncKeyToMsgKey: invalid participant value={0}", new LogRedactable.User(participant));
                    return Optional.empty();
                }
            }
        }

        var key = new MessageKeyBuilder()
                .fromMe("1".equals(fromMe))
                .parentJid(remoteJid)
                .id(id)
                .senderJid(participantJid)
                .build();
        return Optional.of(key);
    }

    /**
     * Extracts a {@link MessageKey} from a star-action index.
     *
     * <p>Requires the parsed array to have at least five elements and delegates to
     * {@link #syncKeyToMsgKey(LinkedWhatsAppStore, String, String, String, String)} on
     * slots {@code [1..4]}, returning empty when the input is malformed. Suits
     * callers that already hold the raw JSON-encoded star-action index and want the
     * rebuilt message key without parsing the JSON themselves.
     *
     * @param store the {@link LinkedWhatsAppStore} consulted for the current user's JID
     * @param index the JSON-encoded star-action index string
     * @return the resolved {@link MessageKey}, or empty when the input is malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdIndexUtils", exports = "getMsgKeyFromStarActionIndex", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<MessageKey> getMsgKeyFromStarActionIndex(LinkedWhatsAppStore store, String index) {
        var parsed = JSON.parseArray(index);
        if (parsed == null || parsed.size() < 5) {
            if (Log.WARNING)
                LOGGER.log(Level.WARNING, "star action index malformed, cannot create MsgKey");
            return Optional.empty();
        }
        var result = syncKeyToMsgKey(
                store,
                parsed.getString(1),
                parsed.getString(2),
                parsed.getString(3),
                parsed.getString(4)
        );
        if (result.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "star index malformed, MsgKey failed");
        }
        return result;
    }

    /**
     * Returns the {@code MALFORMED} sentinel for an invalid action index.
     *
     * <p>Centralizes the "invalid index" report and logs the collection and action
     * for local debugging.
     *
     * @implNote
     * This implementation skips WA Web's
     * {@code WAWebSyncdMetrics.uploadMdCriticalEventMetric(ACTION_INVALID_INDEX_DATA, ...)}
     * call because Cobalt does not replicate WAM telemetry.
     *
     * @param collectionName the collection name for diagnostic context
     * @param actionName     the action name for diagnostic context
     * @return the {@code MALFORMED} {@link MutationApplicationResult}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdIndexUtils", exports = "malformedActionIndex", adaptation = WhatsAppAdaptation.ADAPTED)
    static MutationApplicationResult malformedActionIndex(String collectionName, String actionName) {
        if (Log.DEBUG)
            LOGGER.log(Level.DEBUG, "malformed action index: collection={0}, action={1}", collectionName, actionName);
        return MutationApplicationResult.malformed();
    }

    /**
     * Returns the {@code MALFORMED} sentinel for an invalid action value.
     *
     * <p>Used when the mutation index is well-formed but the decoded action body is
     * missing required fields; distinguished from
     * {@link #malformedActionIndex(String, String)} only by the absence of the WAM
     * metric upload in WA Web.
     *
     * @param collectionName the collection name for diagnostic context
     * @return the {@code MALFORMED} {@link MutationApplicationResult}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdIndexUtils", exports = "malformedActionValue", adaptation = WhatsAppAdaptation.DIRECT)
    static MutationApplicationResult malformedActionValue(String collectionName) {
        return MutationApplicationResult.malformed();
    }

    /**
     * Serializes a {@link MessageKey} into the WA Web MsgKey string form.
     *
     * <p>Reproduces WA Web's {@code fromMe_remote_id[_participant]} layout: the
     * participant segment is appended only when the remote JID is non-user and
     * non-newsletter. Used by {@link #msgKeyToDbIdWithoutFromMeParticipant(MessageKey)}
     * and by the {@link StarMessageHandler} orphan branch to produce a stable
     * identifier that round-trips through WA Web's MsgKey format.
     *
     * @implNote
     * Appending the participant only for non-user, non-newsletter remotes dodges
     * Cobalt's {@link MessageKey#senderJid()} parent-JID fallback.
     *
     * @param key the message key to serialize
     * @return the WA Web-compatible serialized string
     */
    static String serializeMessageKey(MessageKey key) {
        var sb = new StringBuilder();
        sb.append(key.fromMe());
        sb.append('_');
        var remoteJid = key.parentJid().orElse(null);
        sb.append(remoteJid != null ? remoteJid.toString() : "");
        sb.append('_');
        sb.append(key.id().orElse(""));
        if (remoteJid != null && !isUserJid(remoteJid) && !remoteJid.hasNewsletterServer()) {
            key.senderJid().ifPresent(sender -> {
                sb.append('_');
                sb.append(sender);
            });
        }
        return sb.toString();
    }

    /**
     * Reports whether the given JID matches WA Web's {@code Wid.isUser} predicate.
     *
     * <p>Used by the index utilities to decide whether a remote JID has a dedicated
     * participant slot; covered servers are {@code c.us} / {@code s.whatsapp.net},
     * {@code lid}, {@code bot}, {@code hosted}, and {@code hosted.lid}.
     *
     * @implNote
     * This implementation expands the predicate beyond {@link Jid#hasUserServer()}
     * because the latter only covers the standard and legacy user domains, missing
     * the LID / bot / hosted variants WA Web treats as user JIDs.
     *
     * @param jid the JID to test
     * @return {@code true} if the JID belongs to a user-category server
     */
    private static boolean isUserJid(Jid jid) {
        return jid.hasUserServer()
                || jid.hasLidServer()
                || jid.hasBotServer()
                || jid.hasHostedServer()
                || jid.hasHostedLidServer();
    }
}
