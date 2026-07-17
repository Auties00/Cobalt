package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.commerce.OrderMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.ProductMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.system.PinInChatMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;

/**
 * Classifies messages into the WAM {@link MediaType}, {@link MessageType},
 * {@link MessageChatType}, and {@link E2eDeviceType} enumerations consumed by
 * the WAM send and receive metric emitters.
 *
 * <p>These are pure, stateless functions: each derives a telemetry
 * bucketing dimension from a {@link LinkedMessageContainer}, a
 * {@link ChatMessageInfo}, a {@link Jid}, or a parser-level stanza type,
 * with no dependency on session state. They are split out of
 * {@link WamService} (which owns the stateful batching and upload
 * pipeline) so that the message-send and message-receive surfaces can tag
 * their events without holding a reference to the running service.
 */
@WhatsAppWebModule(moduleName = "WAWebWamMsgUtils")
public final class WamMsgUtils {
    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private WamMsgUtils() {
        throw new AssertionError();
    }

    /**
     * Returns the WAM {@link MediaType} classification for the
     * payload carried by the given {@link ChatMessageInfo}.
     *
     * <p>This resolves the wrapped {@link LinkedMessageContainer} and forwards to
     * {@link #getWamMediaType(LinkedMessageContainer)}; the WAM send and receive
     * metric loggers use it to tag every event with the payload shape so
     * the WA backend can bucket telemetry per media kind.
     * {@link MediaType#NONE} is returned for {@code null} and for
     * unclassified types.
     *
     * @param info the chat message info whose payload is being
     *             classified; may be {@code null}
     * @return the WAM media-type classification for the resolved
     *         payload
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MediaType getWamMediaType(ChatMessageInfo info) {
        return info == null ? MediaType.NONE : getWamMediaType(info.message());
    }

    /**
     * Returns the WAM {@link MediaType} classification for the
     * resolved content of the given {@link LinkedMessageContainer}.
     *
     * <p>This mirrors WA Web's {@code getWamMediaType} switch on
     * {@code e.type}: every branch of the upstream string switch maps to
     * an {@code instanceof} arm here, with the GIF and PTT
     * sub-classifications driven off the per-message
     * {@link VideoMessage#gifPlayback()} and {@link AudioMessage#ptt()}
     * booleans rather than secondary type strings.
     *
     * @implNote
     * This implementation does not surface the
     * {@code BUTTON_MESSAGE} or {@code BUTTON_RESPONSE_MESSAGE} arms
     * (legacy reply-button surfaces), the {@code PUSH_TO_VIDEO}
     * variant of {@code ptv}, the {@code FUTURE} catch-all for
     * unrecognised type strings, the catalog-link sub-classifications
     * of plain {@code chat} payloads, or the
     * {@code POLL_RESULT_SNAPSHOT} / {@code KEEP} / {@code UNKEEP} /
     * {@code EPHEMERAL_SYNC_RESPONSE} ancillary entries; these are
     * never produced by Cobalt's send and receive surfaces.
     *
     * @param container the container whose resolved content is being
     *                  classified; {@code null} yields
     *                  {@link MediaType#NONE}
     * @return the WAM media-type classification, defaulting to
     *         {@link MediaType#NONE} for unrecognised or unclassified
     *         types
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMediaType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MediaType getWamMediaType(LinkedMessageContainer container) {
        if (container == null) {
            return MediaType.NONE;
        }
        var content = container.content();
        return switch (content) {
            case ImageMessage ignored -> MediaType.PHOTO;
            case VideoMessage video -> video.gifPlayback() ? MediaType.GIF : MediaType.VIDEO;
            case AudioMessage audio -> audio.ptt() ? MediaType.PTT : MediaType.AUDIO;
            case DocumentMessage ignored -> MediaType.DOCUMENT;
            case StickerMessage ignored -> MediaType.STICKER;
            case StickerPackMessage ignored -> MediaType.STICKER_PACK;
            case ReactionMessage ignored -> MediaType.REACTION;
            case EncReactionMessage ignored -> MediaType.REACTION;
            case PollCreationMessage ignored -> MediaType.POLL_CREATE;
            case PollUpdateMessage ignored -> MediaType.POLL_VOTE;
            case ContactMessage ignored -> MediaType.CONTACT;
            case ContactsArrayMessage ignored -> MediaType.CONTACT_ARRAY;
            case LocationMessage ignored -> MediaType.LOCATION;
            case LiveLocationMessage ignored -> MediaType.LIVE_LOCATION;
            case ProductMessage ignored -> MediaType.PRODUCT_IMAGE;
            case ListMessage ignored -> MediaType.LIST;
            case ListResponseMessage ignored -> MediaType.LIST_REPLY;
            case OrderMessage ignored -> MediaType.ORDER;
            case EventResponseMessage ignored -> MediaType.EVENT_RESPOND;
            case EncEventResponseMessage ignored -> MediaType.EVENT_RESPOND;
            case EventMessage ignored -> MediaType.EVENT_CREATE;
            case AlbumMessage ignored -> MediaType.MEDIA_ALBUM;
            case PinInChatMessage ignored -> MediaType.PIN_IN_CHAT;
            case ExtendedTextMessage ignored -> MediaType.TEXT;
            case null, default -> MediaType.NONE;
        };
    }

    /**
     * Returns the WAM {@link MessageType} classification derived
     * from the chat JID carried by the given
     * {@link ChatMessageInfo}.
     *
     * <p>This mirrors WA Web's {@code WAWebWamMsgUtils.getWamMessageType}:
     * it resolves the parent JID from {@code info.key()} and delegates to
     * {@link #getWamMessageType(Jid)}. {@link MessageType#STATUS} is
     * disambiguated before {@link MessageType#BROADCAST} because status
     * messages live on the broadcast server but must not be reported as
     * generic broadcasts.
     *
     * @param info the chat message info whose destination is being
     *             classified; {@code null} yields
     *             {@link MessageType#INDIVIDUAL}
     * @return the WAM message-type classification, defaulting to
     *         {@link MessageType#INDIVIDUAL}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMessageType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MessageType getWamMessageType(ChatMessageInfo info) {
        if (info == null) {
            return MessageType.INDIVIDUAL;
        }
        var parent = info.key().parentJid().orElse(null);
        if (parent == null) {
            return MessageType.INDIVIDUAL;
        }
        return getWamMessageType(parent);
    }

    /**
     * Returns the WAM {@link MessageType} classification derived
     * from the server component of the given chat JID.
     *
     * <p>This is the JID-level fan-out of
     * {@link #getWamMessageType(ChatMessageInfo)}, exposed separately
     * because Cobalt's send pipeline reaches the classification before the
     * {@link ChatMessageInfo} wrapper has been constructed. It mirrors WA
     * Web's {@code getWamMessageType} server cascade:
     * {@code isStatusBroadcast} first, then {@code isGroup}, then
     * {@code isBroadcast}, then {@code isNewsletter}, falling through to
     * {@link MessageType#INDIVIDUAL} for user, LID, hosted, and bot
     * servers.
     *
     * @param chatJid the chat JID whose server is being classified;
     *                {@code null} yields {@link MessageType#INDIVIDUAL}
     * @return the WAM message-type classification, defaulting to
     *         {@link MessageType#INDIVIDUAL}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamMessageType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MessageType getWamMessageType(Jid chatJid) {
        if (chatJid == null) {
            return MessageType.INDIVIDUAL;
        }
        if (chatJid.isStatusBroadcastAccount()) {
            return MessageType.STATUS;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return MessageType.GROUP;
        }
        if (chatJid.hasBroadcastServer()) {
            return MessageType.BROADCAST;
        }
        if (chatJid.hasNewsletterServer()) {
            return MessageType.CHANNEL;
        }
        return MessageType.INDIVIDUAL;
    }

    /**
     * Returns the WAM {@link MessageChatType} classification derived from the
     * server component of the given chat JID.
     *
     * <p>This mirrors the cascaded ternary in
     * {@code WAWebGetMessageChatTypeFromWid.getMessageChatTypeFromWid}
     * exactly, dispatching on the WA Web {@code Wid} predicates in the same
     * order:
     * <ol>
     *     <li>{@code isUser()} (user/legacy-user/LID/bot/hosted/hosted.lid
     *         domains) maps to {@link MessageChatType#INDIVIDUAL};</li>
     *     <li>{@code isGroup()} ({@code g.us} domain) maps to
     *         {@link MessageChatType#GROUP};</li>
     *     <li>{@code isBroadcast()} ({@code broadcast} domain) maps to
     *         {@link MessageChatType#BROADCAST};</li>
     *     <li>{@code isStatus()} ({@code status@broadcast}) maps to
     *         {@link MessageChatType#STATUS};</li>
     *     <li>{@code isNewsletter()} ({@code newsletter} domain) maps to
     *         {@link MessageChatType#CHANNEL};</li>
     *     <li>anything else maps to {@link MessageChatType#OTHER}.</li>
     * </ol>
     *
     * @param chatJid the chat JID to classify; must not be {@code null}
     * @return the corresponding {@link MessageChatType}; never {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetMessageChatTypeFromWid",
            exports = "getMessageChatTypeFromWid",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static MessageChatType getWamChatType(Jid chatJid) {
        if (chatJid.hasUserServer()
                || chatJid.hasLidServer()
                || chatJid.hasBotServer()
                || chatJid.hasHostedServer()
                || chatJid.hasHostedLidServer()) {
            return MessageChatType.INDIVIDUAL;
        }
        if (chatJid.hasGroupOrCommunityServer()) {
            return MessageChatType.GROUP;
        }
        if (chatJid.hasBroadcastServer()) {
            return MessageChatType.BROADCAST;
        }
        // (unreachable: isBroadcast above already catches status@broadcast; kept for
        // structural parity with the JS ternary)
        if (chatJid.isStatusBroadcastAccount()) {
            return MessageChatType.STATUS;
        }
        if (chatJid.hasNewsletterServer()) {
            return MessageChatType.CHANNEL;
        }
        return MessageChatType.OTHER;
    }

    /**
     * Returns the WAM {@link MessageType} classification derived
     * from the stanza-level
     * {@link com.github.auties00.cobalt.message.receive.stanza.MessageType}
     * produced during parsing.
     *
     * <p>This mirrors WA Web's
     * {@code WAWebWamMsgUtils.getMessageTypeFromMsgInfoType}: the upstream
     * switch is on the {@code msgInfo.type} string ({@code chat},
     * {@code group}, {@code peer_broadcast}, {@code other_broadcast},
     * {@code direct_peer_status}, {@code other_status}). Cobalt classifies
     * the incoming stanza once during parsing into the enum form, so this
     * helper performs the equivalent collapse onto the WAM message-type
     * enum directly without re-stringifying.
     *
     * @param stanzaType the parser-level message type;
     *                   {@code null} yields {@link MessageType#INDIVIDUAL}
     * @return the WAM message-type classification, defaulting to
     *         {@link MessageType#INDIVIDUAL}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getMessageTypeFromMsgInfoType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static MessageType getWamMessageTypeFromStanzaType(
            com.github.auties00.cobalt.message.receive.stanza.MessageType stanzaType
    ) {
        if (stanzaType == null) {
            return MessageType.INDIVIDUAL;
        }
        return switch (stanzaType) {
            case CHAT, PEER_CHAT -> MessageType.INDIVIDUAL;
            case GROUP -> MessageType.GROUP;
            case PEER_BROADCAST, OTHER_BROADCAST -> MessageType.BROADCAST;
            case DIRECT_PEER_STATUS, OTHER_STATUS -> MessageType.STATUS;
        };
    }

    /**
     * Returns the WAM {@link E2eDeviceType} classification of the
     * sender JID relative to the bound account.
     *
     * <p>This mirrors WA Web's {@code WAWebWamMsgUtils.getWamE2eSenderType}:
     * a two-axis classification (self-vs-other on the user component of
     * the JID, primary-vs-companion on the device component, hosted-vs-not
     * on the server component). The {@code instanceof Wid} guard at the top
     * of the WA Web function maps here to a server-family check
     * ({@link Jid#hasUserServer()}, {@link Jid#hasLidServer()},
     * {@link Jid#hasHostedServer()}, {@link Jid#hasHostedLidServer()});
     * foreign-server JIDs return {@code null} so callers omit the property
     * from the WAM event.
     *
     * @param senderJid the sender's full device JID;
     *                  {@code null} yields {@code null}
     * @param selfJid   the logged-in account's primary JID;
     *                  {@code null} when the account is not yet bound
     * @return the WAM classification, or {@code null} when the
     *         sender is not a user / LID / hosted-LID JID
     */
    @WhatsAppWebExport(moduleName = "WAWebWamMsgUtils", exports = "getWamE2eSenderType",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static E2eDeviceType getWamE2eSenderType(Jid senderJid, Jid selfJid) {
        if (senderJid == null) {
            return null;
        }
        if (!senderJid.hasUserServer() && !senderJid.hasLidServer()
                && !senderJid.hasHostedServer() && !senderJid.hasHostedLidServer()) {
            return null;
        }
        var isMe = selfJid != null
                && selfJid.toUserJid().equals(senderJid.toUserJid());
        var isCompanion = senderJid.hasDevice();
        var isHosted = senderJid.hasHostedServer() || senderJid.hasHostedLidServer();
        if (isMe) {
            if (isCompanion) {
                return isHosted ? E2eDeviceType.MY_HOSTED_COMPANION : E2eDeviceType.MY_COMPANION;
            }
            return E2eDeviceType.MY_PRIMARY;
        }
        if (isCompanion) {
            return isHosted ? E2eDeviceType.OTHER_HOSTED_COMPANION : E2eDeviceType.OTHER_COMPANION;
        }
        return E2eDeviceType.OTHER_PRIMARY;
    }
}
