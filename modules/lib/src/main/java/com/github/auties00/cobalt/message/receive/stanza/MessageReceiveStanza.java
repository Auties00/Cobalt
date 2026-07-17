package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The fully parsed, pre-decryption representation of an incoming
 * {@code <message>} stanza.
 *
 * <p>An instance is produced by {@link MessageReceiveStanzaParser#parse} for
 * every message the socket delivers. It captures the entire structural
 * extract: the {@code id} / {@code t} pair, the {@code from} /
 * {@code participant} addressing plus the full LID/PN/username migration
 * triplets, every {@code <enc>} ciphertext, the bot / business / payment /
 * reporting children, the broadcast contact list, the {@code <hsm>} tags, and
 * every {@code <meta>} attribute that influences downstream routing or
 * rendering. Decryption, dedup, sender-key processing, receipt emission, and UI
 * dispatch all consume this record instead of re-scanning the raw stanza.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
@WhatsAppWebModule(moduleName = "WAWebHandleMsgCommon")
public final class MessageReceiveStanza {
    /**
     * The {@code edit} attribute value that means "no edit", used when the
     * attribute is absent from the stanza.
     *
     * <p>Compare {@link #editAttribute()} against this constant to detect the
     * default no-edit case before branching on the other {@code EDIT_*} values.
     *
     * @implNote
     * This implementation uses {@code 0} as the absent-attribute sentinel
     * because the parser passes {@code 0} as the default to its
     * {@code getAttributeAsInt("edit", 0)} read; the upstream no-edit sentinel
     * is {@code -1} and never appears on the wire either. Both are "no edit",
     * neither is observable inside a real stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static final int EDIT_NONE = 0;

    /**
     * The {@code edit} attribute value that marks the stanza as an in-place
     * message edit of a previously sent message.
     *
     * <p>The {@code target_id} attribute (see {@link #targetId()}) identifies
     * the original message being edited.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int EDIT_MESSAGE = 1;

    /**
     * The {@code edit} attribute value that marks the stanza as a pin-in-chat
     * operation.
     *
     * <p>Pins or unpins the message referenced by {@link #targetId()} inside the
     * chat; the inner protobuf carries the action.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int EDIT_PIN = 2;

    /**
     * The {@code edit} attribute value that marks the stanza as a sender-side
     * revoke.
     *
     * <p>Drives the "deleted for everyone" UI when the original sender revokes
     * one of their own messages.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int EDIT_SENDER_REVOKE = 7;

    /**
     * The {@code edit} attribute value that marks the stanza as a group-admin
     * revoke.
     *
     * <p>Drives the "deleted by admin" UI inside groups; the {@code target_id}
     * attribute identifies the revoked message.
     */
    @WhatsAppWebExport(moduleName = "WAWebAck", exports = "EDIT_ATTR",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int EDIT_ADMIN_REVOKE = 8;

    /**
     * The {@code context_source} attribute value identifying a stanza that was
     * sent in response to a channel invitation.
     *
     * <p>Compare {@link #contextSource()} against this constant when filtering
     * stanzas that originated from the channels-onboarding surface.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgCommon", exports = "CONTEXT_SOURCE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String CONTEXT_SOURCE_CHANNELS_INVITATION = "channels_invitation";

    /**
     * The stanza's {@code id} attribute.
     */
    private final String id;

    /**
     * The message timestamp parsed from the {@code t} attribute.
     */
    private final Instant timestamp;

    /**
     * The chat JID derived from the {@code from} attribute; the user for 1:1
     * chats, the group for group messages, the broadcast or status JID for
     * broadcast messages.
     */
    private final Jid chatJid;

    /**
     * The actual sender's device JID; equal to {@link #chatJid} for 1:1
     * messages, the {@code participant} attribute for group/broadcast/status
     * messages.
     */
    private final Jid senderJid;

    /**
     * The raw {@code participant} attribute when present, identifying the
     * sender's device inside a group or broadcast.
     */
    private final Jid participant;

    /**
     * The addressing-derived message classification.
     */
    private final MessageType messageType;

    /**
     * The {@code edit} attribute value; defaults to {@link #EDIT_NONE} when the
     * attribute is absent.
     */
    private final int editAttribute;

    /**
     * The sender's push name carried by the {@code notify} attribute.
     */
    private final String pushName;

    /**
     * The {@code category} attribute; the only defined value is {@code "peer"}
     * for peer-protocol messages.
     */
    private final String category;

    /**
     * The {@code offline} attribute, present on messages the server delivers
     * after the client comes back online.
     */
    private final String offline;

    /**
     * The {@code addressing_mode} attribute on group messages, either
     * {@code "pn"} (phone-number addressing) or {@code "lid"} (LID addressing).
     */
    private final String addressingMode;

    /**
     * {@code true} when the stanza has an {@code <hsm>} child, indicating a
     * highly-structured (business template) message.
     */
    private final boolean isHsm;

    /**
     * The {@code count} attribute, when present.
     */
    private final Integer count;

    /**
     * The sender's phone-number JID from the {@code sender_pn} attribute,
     * present on LID-addressed groups to carry the PN mapping.
     */
    private final Jid senderPn;

    /**
     * The sender's LID from the {@code sender_lid} attribute.
     */
    private final Jid senderLid;

    /**
     * The peer JID from the plain {@code recipient} attribute, present on
     * self-authored 1:1 echoes to name the conversation the message was
     * addressed to; distinct from the {@code from} attribute, which on such an
     * echo is the local account.
     */
    private final Jid recipient;

    /**
     * The recipient's phone-number JID from the {@code recipient_pn} attribute,
     * present on peer-protocol messages.
     */
    private final Jid recipientPn;

    /**
     * The recipient's LID from the {@code recipient_lid} attribute.
     */
    private final Jid recipientLid;

    /**
     * The peer recipient's phone-number JID from the {@code peer_recipient_pn}
     * attribute, present on peer-broadcast messages.
     */
    private final Jid peerRecipientPn;

    /**
     * The peer recipient's LID from the {@code peer_recipient_lid} attribute.
     */
    private final Jid peerRecipientLid;

    /**
     * The peer recipient's username from the {@code peer_recipient_username}
     * attribute.
     */
    private final String peerRecipientUsername;

    /**
     * The most recent LID known by the server for the recipient, from the
     * {@code recipient_latest_lid} attribute.
     */
    private final Jid recipientLatestLid;

    /**
     * The recipient's username from the {@code recipient_username} attribute.
     */
    private final String recipientUsername;

    /**
     * The group participant's phone-number JID from the {@code participant_pn}
     * attribute.
     */
    private final Jid participantPn;

    /**
     * The group participant's LID from the {@code participant_lid} attribute.
     */
    private final Jid participantLid;

    /**
     * The group participant's username from the {@code participant_username}
     * attribute.
     */
    private final String participantUsername;

    /**
     * The sender's username from the {@code username} attribute.
     */
    private final String username;

    /**
     * The sender's display name from the {@code display_name} attribute.
     */
    private final String displayName;

    /**
     * The {@code type} attribute of the stanza; defined values are
     * {@code "text"}, {@code "media"}, {@code "medianotify"}, {@code "pay"},
     * {@code "poll"}, {@code "reaction"}, and {@code "event"}.
     */
    private final String stanzaType;

    /**
     * {@code true} when the stanza has an {@code <unavailable>} child,
     * indicating a fanout placeholder for which the payload is missing.
     */
    private final boolean unavailable;

    /**
     * {@code true} when the {@code <unavailable>} child carries
     * {@code hosted="true"}, marking a hosted-device fanout placeholder.
     */
    private final boolean hostedUnavailable;

    /**
     * {@code true} when the {@code <unavailable>} child carries
     * {@code type="view_once"}, marking a view-once fanout placeholder.
     */
    private final boolean viewOnceUnavailable;

    /**
     * The {@code polltype} attribute on the {@code <meta>} child; defined values
     * are {@code "creation"}, {@code "quiz_creation"}, {@code "vote"},
     * {@code "result_snapshot"}, and {@code "edit"}.
     */
    private final String pollType;

    /**
     * The {@code event_type} attribute on the {@code <meta>} child, populated
     * only when the stanza type is {@code "event"}; defined values are
     * {@code "creation"}, {@code "response"}, and {@code "edit"}.
     */
    private final String eventType;

    /**
     * The {@code origin} attribute on the {@code <meta>} child; the only defined
     * value is {@code "ctwa"} for click-to-WhatsApp ad-originated conversations.
     */
    private final String origin;

    /**
     * {@code true} when the stanza has a {@code <url_number>} child.
     */
    private final boolean urlNumber;

    /**
     * {@code true} when the stanza has a {@code <url_text>} child.
     */
    private final boolean urlText;

    /**
     * {@code true} when the {@code <meta>} child carries
     * {@code status_mentioned="true"}, marking a status post that mentions the
     * local user.
     */
    private final boolean statusMentioned;

    /**
     * The {@code appdata} attribute on the {@code <meta>} child; defined values
     * are {@code "default"}, {@code "member_tag"}, and {@code "group_history"}.
     */
    private final String appdata;

    /**
     * The {@code biz_source} attribute on the {@code <meta>} child.
     */
    private final String bizSource;

    /**
     * The {@code thread_msg_id} attribute on the {@code <meta>} child,
     * referencing the parent of a comment thread.
     */
    private final String threadMsgId;

    /**
     * The {@code thread_msg_sender_jid} attribute on the {@code <meta>} child.
     */
    private final Jid threadMsgSenderJid;

    /**
     * The {@code target_id} attribute on the {@code <meta>} child; used by addon
     * messages (reactions, poll votes, edits, revokes) to point at the parent
     * message.
     */
    private final String targetId;

    /**
     * The {@code target_sender_jid} attribute on the {@code <meta>} child.
     */
    private final Jid targetSenderJid;

    /**
     * The {@code target_chat_jid} attribute on the {@code <meta>} child.
     */
    private final Jid targetChatJid;

    /**
     * The {@code target_chat_jid_lid} attribute on the {@code <meta>} child.
     */
    private final Jid targetChatJidLid;

    /**
     * {@code true} when the {@code <meta>} child carries {@code capi="true"}.
     */
    private final boolean capi;

    /**
     * The {@code context_source} attribute on the {@code <meta>} child; compare
     * against {@link #CONTEXT_SOURCE_CHANNELS_INVITATION}.
     */
    private final String contextSource;

    /**
     * The sender's country code from the {@code sender_country_code} attribute
     * on the {@code <meta>} child.
     */
    private final String senderCountryCode;

    /**
     * Every {@code <enc>} child parsed into a payload record.
     */
    private final List<MessageReceiveEncryptedPayload> encs;

    /**
     * The raw bytes of the {@code <device-identity>} child, fed to ADV
     * validation for companion devices.
     */
    private final byte[] deviceIdentity;

    /**
     * The {@code <bot>} child parsed into a typed record, when present.
     */
    private final MessageReceiveBotInfo botInfo;

    /**
     * The business metadata parsed from the stanza attributes and the
     * {@code <biz>} child, when any business field is present.
     */
    private final MessageReceiveBizInfo bizInfo;

    /**
     * The {@code <reporting>} child parsed into a typed record, when present.
     */
    private final MessageReceiveReportingInfo reportingInfo;

    /**
     * The broadcast contact list parsed from the {@code <participants>} child;
     * populated for {@link MessageType#PEER_BROADCAST} and
     * {@link MessageType#DIRECT_PEER_STATUS} stanzas.
     */
    private final List<MessageReceiveBroadcastParticipant> bclParticipants;

    /**
     * The {@code <pay>} or {@code <transaction>} child parsed into a typed
     * record, when present.
     */
    private final MessageReceivePaymentInfo paymentInfo;

    /**
     * The raw bytes of the {@code <rcat>} child, used for content-binding
     * verification.
     */
    private final byte[] rcat;

    /**
     * The stanza-level {@code eph_setting} attribute, present on
     * {@link MessageType#OTHER_BROADCAST} messages.
     */
    private final String ephSetting;

    /**
     * The {@code tag} attribute on the {@code <hsm>} child.
     */
    private final String hsmTag;

    /**
     * The {@code category} attribute on the {@code <hsm>} child.
     */
    private final String hsmCategory;

    /**
     * Constructs a fully populated stanza record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * <p>The constructor remains public so the parser in the same package and
     * the test fixtures can build instances.
     *
     * @param id                    the stanza identifier
     * @param timestamp             the message timestamp
     * @param chatJid               the chat JID from the {@code from} attribute
     * @param senderJid             the sender's device JID
     * @param participant           the raw participant attribute, or {@code null}
     * @param messageType           the addressing-derived classification
     * @param editAttribute         the edit-attribute integer
     * @param pushName              the sender push name, or {@code null}
     * @param category              the message category, or {@code null}
     * @param offline               the offline attribute, or {@code null}
     * @param addressingMode        the group addressing mode, or {@code null}
     * @param isHsm                 whether the stanza carries an {@code <hsm>} child
     * @param count                 the count attribute, or {@code null}
     * @param senderPn              the sender phone-number JID, or {@code null}
     * @param senderLid             the sender LID, or {@code null}
     * @param recipient             the plain recipient JID, or {@code null}
     * @param recipientPn           the recipient phone-number JID, or {@code null}
     * @param recipientLid          the recipient LID, or {@code null}
     * @param peerRecipientPn       the peer recipient phone-number JID, or {@code null}
     * @param peerRecipientLid      the peer recipient LID, or {@code null}
     * @param peerRecipientUsername the peer recipient username, or {@code null}
     * @param recipientLatestLid    the latest recipient LID, or {@code null}
     * @param recipientUsername     the recipient username, or {@code null}
     * @param participantPn         the participant phone-number JID, or {@code null}
     * @param participantLid        the participant LID, or {@code null}
     * @param participantUsername   the participant username, or {@code null}
     * @param username              the sender username, or {@code null}
     * @param displayName           the sender display name, or {@code null}
     * @param stanzaType            the {@code type} attribute
     * @param unavailable           whether the stanza is an unavailable placeholder
     * @param hostedUnavailable     whether the placeholder is hosted
     * @param viewOnceUnavailable   whether the placeholder is view-once
     * @param pollType              the poll type, or {@code null}
     * @param eventType             the event type, or {@code null}
     * @param origin                the origin attribute, or {@code null}
     * @param urlNumber             whether a {@code <url_number>} child is present
     * @param urlText               whether a {@code <url_text>} child is present
     * @param statusMentioned       whether the status mentions the local user
     * @param appdata               the appdata attribute, or {@code null}
     * @param bizSource             the biz_source attribute, or {@code null}
     * @param threadMsgId           the thread parent id, or {@code null}
     * @param threadMsgSenderJid    the thread parent sender JID, or {@code null}
     * @param targetId              the addon target id, or {@code null}
     * @param targetSenderJid       the addon target sender JID, or {@code null}
     * @param targetChatJid         the addon target chat JID, or {@code null}
     * @param targetChatJidLid      the addon target chat LID, or {@code null}
     * @param capi                  whether the {@code capi} attribute is {@code "true"}
     * @param contextSource         the context_source value, or {@code null}
     * @param senderCountryCode     the sender country code, or {@code null}
     * @param encs                  the parsed encrypted payloads (defensively copied)
     * @param deviceIdentity        the device identity bytes, or {@code null}
     * @param botInfo               the parsed bot record, or {@code null}
     * @param bizInfo               the parsed business record, or {@code null}
     * @param reportingInfo         the parsed reporting record, or {@code null}
     * @param bclParticipants       the broadcast contact list (defensively copied; may be {@code null} to denote empty)
     * @param paymentInfo           the parsed payment record, or {@code null}
     * @param ephSetting            the stanza-level ephemeral setting, or {@code null}
     * @param rcat                  the rcat content bytes, or {@code null}
     * @param hsmTag                the HSM tag, or {@code null}
     * @param hsmCategory           the HSM category, or {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceiveStanza(
            String id,
            Instant timestamp,
            Jid chatJid,
            Jid senderJid,
            Jid participant,
            MessageType messageType,
            int editAttribute,
            String pushName,
            String category,
            String offline,
            String addressingMode,
            boolean isHsm,
            Integer count,
            Jid senderPn,
            Jid senderLid,
            Jid recipient,
            Jid recipientPn,
            Jid recipientLid,
            Jid peerRecipientPn,
            Jid peerRecipientLid,
            String peerRecipientUsername,
            Jid recipientLatestLid,
            String recipientUsername,
            Jid participantPn,
            Jid participantLid,
            String participantUsername,
            String username,
            String displayName,
            String stanzaType,
            boolean unavailable,
            boolean hostedUnavailable,
            boolean viewOnceUnavailable,
            String pollType,
            String eventType,
            String origin,
            boolean urlNumber,
            boolean urlText,
            boolean statusMentioned,
            String appdata,
            String bizSource,
            String threadMsgId,
            Jid threadMsgSenderJid,
            String targetId,
            Jid targetSenderJid,
            Jid targetChatJid,
            Jid targetChatJidLid,
            boolean capi,
            String contextSource,
            String senderCountryCode,
            List<MessageReceiveEncryptedPayload> encs,
            byte[] deviceIdentity,
            MessageReceiveBotInfo botInfo,
            MessageReceiveBizInfo bizInfo,
            MessageReceiveReportingInfo reportingInfo,
            List<MessageReceiveBroadcastParticipant> bclParticipants,
            MessageReceivePaymentInfo paymentInfo,
            String ephSetting,
            byte[] rcat,
            String hsmTag,
            String hsmCategory
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid");
        this.senderJid = Objects.requireNonNull(senderJid, "senderJid");
        this.participant = participant;
        this.messageType = Objects.requireNonNull(messageType, "messageType");
        this.editAttribute = editAttribute;
        this.pushName = pushName;
        this.category = category;
        this.offline = offline;
        this.addressingMode = addressingMode;
        this.isHsm = isHsm;
        this.count = count;
        this.senderPn = senderPn;
        this.senderLid = senderLid;
        this.recipient = recipient;
        this.recipientPn = recipientPn;
        this.recipientLid = recipientLid;
        this.peerRecipientPn = peerRecipientPn;
        this.peerRecipientLid = peerRecipientLid;
        this.peerRecipientUsername = peerRecipientUsername;
        this.recipientLatestLid = recipientLatestLid;
        this.recipientUsername = recipientUsername;
        this.participantPn = participantPn;
        this.participantLid = participantLid;
        this.participantUsername = participantUsername;
        this.username = username;
        this.displayName = displayName;
        this.stanzaType = Objects.requireNonNull(stanzaType, "stanzaType");
        this.unavailable = unavailable;
        this.hostedUnavailable = hostedUnavailable;
        this.viewOnceUnavailable = viewOnceUnavailable;
        this.pollType = pollType;
        this.eventType = eventType;
        this.origin = origin;
        this.urlNumber = urlNumber;
        this.urlText = urlText;
        this.statusMentioned = statusMentioned;
        this.appdata = appdata;
        this.bizSource = bizSource;
        this.threadMsgId = threadMsgId;
        this.threadMsgSenderJid = threadMsgSenderJid;
        this.targetId = targetId;
        this.targetSenderJid = targetSenderJid;
        this.targetChatJid = targetChatJid;
        this.targetChatJidLid = targetChatJidLid;
        this.capi = capi;
        this.contextSource = contextSource;
        this.senderCountryCode = senderCountryCode;
        this.encs = List.copyOf(Objects.requireNonNull(encs, "encs"));
        this.deviceIdentity = deviceIdentity;
        this.botInfo = botInfo;
        this.bizInfo = bizInfo;
        this.reportingInfo = reportingInfo;
        this.bclParticipants = bclParticipants != null ? List.copyOf(bclParticipants) : List.of();
        this.paymentInfo = paymentInfo;
        this.ephSetting = ephSetting;
        this.rcat = rcat;
        this.hsmTag = hsmTag;
        this.hsmCategory = hsmCategory;
    }

    /**
     * Returns the stanza's {@code id} attribute.
     *
     * <p>The canonical message identifier; reused as the persisted message key
     * and as the value matched by retry receipts and addon {@code target_id}
     * references.
     *
     * @return the stanza identifier
     */
    public String id() { return id; }

    /**
     * Returns the message timestamp parsed from the {@code t} attribute.
     *
     * <p>The server's authoritative receive timestamp; used for chat-ordering
     * and for binding the reporting token to a specific delivery.
     *
     * @return the message timestamp
     */
    public Instant timestamp() { return timestamp; }

    /**
     * Returns the chat JID derived from the {@code from} attribute.
     *
     * <p>The conversation key under which the message is stored: a user JID for
     * 1:1, a group JID for groups, a broadcast or status JID for broadcasts.
     *
     * @return the chat JID
     */
    public Jid chatJid() { return chatJid; }

    /**
     * Returns the actual sender's device JID.
     *
     * <p>For 1:1 messages this equals {@link #chatJid()}; for group, broadcast,
     * and status messages it is the {@code participant} JID. Used to select the
     * Signal session that decrypts each {@code <enc>} payload.
     *
     * @return the sender's device JID
     */
    public Jid senderJid() { return senderJid; }

    /**
     * Returns the raw {@code participant} attribute, when present.
     *
     * <p>Distinct from {@link #senderJid()} in that it stays empty for 1:1
     * messages; useful when downstream code needs to know whether the attribute
     * was on the wire.
     *
     * @return an {@link Optional} wrapping the participant JID
     */
    public Optional<Jid> participant() { return Optional.ofNullable(participant); }

    /**
     * Returns the addressing-derived classification.
     *
     * <p>The single discriminator the receive pipeline branches on; see
     * {@link MessageType} for what each value implies.
     *
     * @return the classified message type
     */
    public MessageType messageType() { return messageType; }

    /**
     * Returns the {@code edit} attribute value.
     *
     * <p>Compare against {@link #EDIT_NONE}, {@link #EDIT_MESSAGE},
     * {@link #EDIT_PIN}, {@link #EDIT_SENDER_REVOKE}, or
     * {@link #EDIT_ADMIN_REVOKE} to dispatch the appropriate edit-handling path.
     *
     * @return the edit attribute integer
     */
    public int editAttribute() { return editAttribute; }

    /**
     * Returns the sender's push name from the {@code notify} attribute, when
     * present.
     *
     * <p>Surfaces the contact's display name as set in their WhatsApp profile;
     * used to update the local contacts cache opportunistically.
     *
     * @return an {@link Optional} wrapping the push name
     */
    public Optional<String> pushName() { return Optional.ofNullable(pushName); }

    /**
     * Returns the {@code category} attribute, when present.
     *
     * <p>Compare against {@code "peer"} (or use {@link #isPeer()}) to detect a
     * peer-protocol message.
     *
     * @return an {@link Optional} wrapping the category string
     */
    public Optional<String> category() { return Optional.ofNullable(category); }

    /**
     * Returns the {@code offline} attribute, when present.
     *
     * <p>Set by the server on stanzas it delivers after a reconnect to flag them
     * as offline-queued; pairs with {@link #isOffline()}.
     *
     * @return an {@link Optional} wrapping the offline value
     */
    public Optional<String> offline() { return Optional.ofNullable(offline); }

    /**
     * Returns the group {@code addressing_mode} attribute, when present.
     *
     * <p>Either {@code "pn"} or {@code "lid"}; lets the receive pipeline pick the
     * right Signal protocol address for the participant in LID-migrating groups.
     *
     * @return an {@link Optional} wrapping the addressing mode
     */
    public Optional<String> addressingMode() { return Optional.ofNullable(addressingMode); }

    /**
     * Returns whether the stanza carried an {@code <hsm>} child.
     *
     * <p>Indicates the message is a highly-structured (business template)
     * message; pair with {@link #hsmTag()} and {@link #hsmCategory()} for the
     * template metadata.
     *
     * @return {@code true} if the HSM child is present
     */
    public boolean isHsm() { return isHsm; }

    /**
     * Returns the {@code count} attribute, when present.
     *
     * <p>Carries server-side reporting of how many recipients a fanout was
     * addressed to.
     *
     * @return an {@link Optional} wrapping the count value
     */
    public Optional<Integer> count() { return Optional.ofNullable(count); }

    /**
     * Returns the sender's phone-number JID, when present.
     *
     * <p>Part of the LID-to-PN migration mapping the server attaches on
     * LID-addressed groups so the recipient can resolve the sender's PN
     * identity.
     *
     * @return an {@link Optional} wrapping the sender PN JID
     */
    public Optional<Jid> senderPn() { return Optional.ofNullable(senderPn); }

    /**
     * Returns the sender's LID, when present.
     *
     * <p>Pairs with {@link #senderPn()} for the LID/PN mapping.
     *
     * @return an {@link Optional} wrapping the sender LID
     */
    public Optional<Jid> senderLid() { return Optional.ofNullable(senderLid); }

    /**
     * Returns the plain {@code recipient} attribute, when present.
     *
     * <p>Carried on self-authored 1:1 echoes (fanned out to the user's own
     * devices) to name the peer the message was addressed to, distinct from the
     * {@code from} attribute which on such an echo is the local account. The
     * receipt pipeline places this value on the {@code recipient} attribute of
     * delivery and retry receipts; {@link #recipientPn()} and
     * {@link #recipientLid()} carry the addressing-mode mapping forms.
     *
     * @return an {@link Optional} wrapping the recipient JID
     */
    public Optional<Jid> recipient() { return Optional.ofNullable(recipient); }

    /**
     * Returns the recipient's phone-number JID, when present.
     *
     * <p>Present on peer-protocol messages where the server identifies the
     * intended recipient by PN.
     *
     * @return an {@link Optional} wrapping the recipient PN JID
     */
    public Optional<Jid> recipientPn() { return Optional.ofNullable(recipientPn); }

    /**
     * Returns the recipient's LID, when present.
     *
     * <p>Pairs with {@link #recipientPn()} for the LID/PN mapping.
     *
     * @return an {@link Optional} wrapping the recipient LID
     */
    public Optional<Jid> recipientLid() { return Optional.ofNullable(recipientLid); }

    /**
     * Returns the peer recipient's phone-number JID, when present.
     *
     * <p>Present on peer-broadcast messages to identify the peer device whose
     * broadcast the local user mirrors.
     *
     * @return an {@link Optional} wrapping the peer recipient PN JID
     */
    public Optional<Jid> peerRecipientPn() { return Optional.ofNullable(peerRecipientPn); }

    /**
     * Returns the peer recipient's LID, when present.
     *
     * <p>Pairs with {@link #peerRecipientPn()} for the LID/PN mapping.
     *
     * @return an {@link Optional} wrapping the peer recipient LID
     */
    public Optional<Jid> peerRecipientLid() { return Optional.ofNullable(peerRecipientLid); }

    /**
     * Returns the peer recipient's username, when present.
     *
     * <p>Populated only when the username-display gate is on.
     *
     * @return an {@link Optional} wrapping the peer recipient username
     */
    public Optional<String> peerRecipientUsername() { return Optional.ofNullable(peerRecipientUsername); }

    /**
     * Returns the freshest LID the server knows for the recipient, when present.
     *
     * <p>Lets the receive pipeline learn an updated LID for the recipient without
     * an explicit query.
     *
     * @return an {@link Optional} wrapping the latest recipient LID
     */
    public Optional<Jid> recipientLatestLid() { return Optional.ofNullable(recipientLatestLid); }

    /**
     * Returns the recipient's username, when present.
     *
     * <p>Mirrors {@link #peerRecipientUsername()} but for the standard recipient
     * field.
     *
     * @return an {@link Optional} wrapping the recipient username
     */
    public Optional<String> recipientUsername() { return Optional.ofNullable(recipientUsername); }

    /**
     * Returns the group participant's phone-number JID, when present.
     *
     * <p>Part of the LID/PN migration mapping for LID-addressed groups; its
     * absence on a LID-addressed group message is an error condition.
     *
     * @return an {@link Optional} wrapping the participant PN JID
     */
    public Optional<Jid> participantPn() { return Optional.ofNullable(participantPn); }

    /**
     * Returns the group participant's LID, when present.
     *
     * <p>Pairs with {@link #participantPn()} for the LID/PN mapping.
     *
     * @return an {@link Optional} wrapping the participant LID
     */
    public Optional<Jid> participantLid() { return Optional.ofNullable(participantLid); }

    /**
     * Returns the group participant's username, when present.
     *
     * <p>Populated only when the username-display gate is on; surfaces the group
     * member's public username.
     *
     * @return an {@link Optional} wrapping the participant username
     */
    public Optional<String> participantUsername() { return Optional.ofNullable(participantUsername); }

    /**
     * Returns the sender's username, when present.
     *
     * <p>Used to update the contact cache opportunistically; mirrors
     * {@link #pushName()} but for the username-display surface.
     *
     * @return an {@link Optional} wrapping the sender username
     */
    public Optional<String> username() { return Optional.ofNullable(username); }

    /**
     * Returns the sender's display name, when present.
     *
     * <p>Used as a fallback display label on LID groups where the participant
     * carries no PN mapping.
     *
     * @return an {@link Optional} wrapping the sender display name
     */
    public Optional<String> displayName() { return Optional.ofNullable(displayName); }

    /**
     * Returns the stanza's {@code type} attribute.
     *
     * <p>Downstream code branches on this string to pick between text, media,
     * poll, event, reaction, and pay handling.
     *
     * @return the stanza type
     */
    public String stanzaType() { return stanzaType; }

    /**
     * Returns whether this stanza is an unavailable fanout placeholder.
     *
     * <p>The server emits these placeholders when the per-recipient ciphertext
     * is missing; the receive pipeline treats them as "decryption deferred" and
     * waits for the actual fanout.
     *
     * @return {@code true} if an {@code <unavailable>} child is present
     */
    public boolean isUnavailable() { return unavailable; }

    /**
     * Returns whether the unavailable placeholder is tagged as hosted.
     *
     * <p>Only meaningful when {@link #isUnavailable()} is {@code true}; flags
     * placeholders that target a hosted companion device rather than a device the
     * local user controls.
     *
     * @return {@code true} if hosted
     */
    public boolean isHostedUnavailable() { return hostedUnavailable; }

    /**
     * Returns whether the unavailable placeholder is a view-once message.
     *
     * <p>Only meaningful when {@link #isUnavailable()} is {@code true}; flags
     * placeholders that will resolve into a view-once media payload.
     *
     * @return {@code true} if view-once
     */
    public boolean isViewOnceUnavailable() { return viewOnceUnavailable; }

    /**
     * Returns the {@code polltype} attribute on the {@code <meta>} child, when
     * present.
     *
     * <p>Only populated when {@link #stanzaType()} is {@code "poll"}.
     *
     * @return an {@link Optional} wrapping the poll type
     */
    public Optional<String> pollType() { return Optional.ofNullable(pollType); }

    /**
     * Returns the {@code event_type} attribute on the {@code <meta>} child, when
     * present.
     *
     * <p>Only populated when {@link #stanzaType()} is {@code "event"}.
     *
     * @return an {@link Optional} wrapping the event type
     */
    public Optional<String> eventType() { return Optional.ofNullable(eventType); }

    /**
     * Returns the {@code origin} attribute on the {@code <meta>} child, when
     * present.
     *
     * <p>The only defined value is {@code "ctwa"} (click-to-WhatsApp ads); used
     * by the chat-header pipeline to badge ad-originated conversations.
     *
     * @return an {@link Optional} wrapping the origin
     */
    public Optional<String> origin() { return Optional.ofNullable(origin); }

    /**
     * Returns whether the stanza had a {@code <url_number>} child.
     *
     * <p>Indicates the message references a URL-encoded phone number; the receive
     * pipeline routes such messages to the link-preview path.
     *
     * @return {@code true} if present
     */
    public boolean urlNumber() { return urlNumber; }

    /**
     * Returns whether the stanza had a {@code <url_text>} child.
     *
     * <p>Companion to {@link #urlNumber()} for URL-encoded text payloads.
     *
     * @return {@code true} if present
     */
    public boolean urlText() { return urlText; }

    /**
     * Returns whether the {@code <meta>} child carried
     * {@code status_mentioned="true"}.
     *
     * <p>Marks status posts that mention the local user so the status feed can
     * surface a notification.
     *
     * @return {@code true} if mentioned
     */
    public boolean statusMentioned() { return statusMentioned; }

    /**
     * Returns the {@code appdata} attribute on the {@code <meta>} child, when
     * present.
     *
     * <p>Defined values are {@code "default"}, {@code "member_tag"}, and
     * {@code "group_history"}; drives content-categorization for the inbox.
     *
     * @return an {@link Optional} wrapping the appdata value
     */
    public Optional<String> appdata() { return Optional.ofNullable(appdata); }

    /**
     * Returns the {@code biz_source} attribute on the {@code <meta>} child, when
     * present.
     *
     * <p>Carries the WhatsApp Business attribution source so the chat-header
     * pipeline can render the right business badge.
     *
     * @return an {@link Optional} wrapping the biz source
     */
    public Optional<String> bizSource() { return Optional.ofNullable(bizSource); }

    /**
     * Returns the {@code thread_msg_id} attribute, when present.
     *
     * <p>References the parent message of a comment thread; pair with
     * {@link #threadMsgSenderJid()} to identify the parent fully.
     *
     * @return an {@link Optional} wrapping the thread parent id
     */
    public Optional<String> threadMsgId() { return Optional.ofNullable(threadMsgId); }

    /**
     * Returns the {@code thread_msg_sender_jid} attribute, when present.
     *
     * <p>The author of the comment-thread parent; companion to
     * {@link #threadMsgId()}.
     *
     * @return an {@link Optional} wrapping the thread parent sender JID
     */
    public Optional<Jid> threadMsgSenderJid() { return Optional.ofNullable(threadMsgSenderJid); }

    /**
     * Returns the {@code target_id} attribute, when present.
     *
     * <p>Identifies the parent message for addon payloads such as reactions, poll
     * votes, edits, and revokes; the addon dispatcher reads this to locate the
     * message being acted on.
     *
     * @return an {@link Optional} wrapping the target id
     */
    public Optional<String> targetId() { return Optional.ofNullable(targetId); }

    /**
     * Returns the {@code target_sender_jid} attribute, when present.
     *
     * <p>The author of the addon-parent message; companion to {@link #targetId()}.
     *
     * @return an {@link Optional} wrapping the target sender JID
     */
    public Optional<Jid> targetSenderJid() { return Optional.ofNullable(targetSenderJid); }

    /**
     * Returns the {@code target_chat_jid} attribute, when present.
     *
     * <p>The chat in which the addon-parent message lives; used for bot replies
     * that arrive on a different chat than the original question.
     *
     * @return an {@link Optional} wrapping the target chat JID
     */
    public Optional<Jid> targetChatJid() { return Optional.ofNullable(targetChatJid); }

    /**
     * Returns the {@code target_chat_jid_lid} attribute, when present.
     *
     * <p>The LID form of {@link #targetChatJid()}, preferred over the PN form
     * when both are present.
     *
     * @return an {@link Optional} wrapping the target chat LID
     */
    public Optional<Jid> targetChatJidLid() { return Optional.ofNullable(targetChatJidLid); }

    /**
     * Returns whether the {@code <meta>} child carried {@code capi="true"}.
     *
     * <p>Identifies stanzas originated through the WhatsApp Cloud API; surfaces to
     * the UI as a "sent via Cloud API" indicator.
     *
     * @return {@code true} if {@code capi} was set to {@code "true"}
     */
    public boolean isCapi() { return capi; }

    /**
     * Returns the {@code context_source} attribute, when present.
     *
     * <p>Compare against {@link #CONTEXT_SOURCE_CHANNELS_INVITATION} to detect
     * channels-invitation-originated stanzas.
     *
     * @return an {@link Optional} wrapping the context source
     */
    public Optional<String> contextSource() { return Optional.ofNullable(contextSource); }

    /**
     * Returns the sender's country code, when present.
     *
     * <p>Used by the safety pipeline to flag cross-region messaging anomalies.
     *
     * @return an {@link Optional} wrapping the sender country code
     */
    public Optional<String> senderCountryCode() { return Optional.ofNullable(senderCountryCode); }

    /**
     * Returns every {@code <enc>} child parsed into a payload record.
     *
     * <p>Always non-{@code null}, defensively copied at construction time so it
     * cannot be mutated externally; iterate to dispatch each ciphertext to its
     * Signal cipher.
     *
     * @return the encrypted payloads
     */
    public List<MessageReceiveEncryptedPayload> encs() { return encs; }

    /**
     * Returns the raw {@code <device-identity>} bytes, when present.
     *
     * <p>Fed to the ADV validator to verify that a companion device's signed
     * identity matches the one the server attests.
     *
     * @return an {@link Optional} wrapping the device identity bytes
     */
    public Optional<byte[]> deviceIdentity() { return Optional.ofNullable(deviceIdentity); }

    /**
     * Returns the parsed {@code <bot>} child, when present.
     *
     * <p>Populated for Meta AI and 1P/3P business bot replies; consumed by the
     * rich-response stitcher to assemble streaming chunks.
     *
     * @return an {@link Optional} wrapping the bot info
     */
    public Optional<MessageReceiveBotInfo> botInfo() { return Optional.ofNullable(botInfo); }

    /**
     * Returns the parsed business metadata, when present.
     *
     * <p>Populated when the stanza carries any business attribute or a
     * {@code <biz>} child; consumed by the business-chat rendering path.
     *
     * @return an {@link Optional} wrapping the biz info
     */
    public Optional<MessageReceiveBizInfo> bizInfo() { return Optional.ofNullable(bizInfo); }

    /**
     * Returns the parsed reporting token, when present.
     *
     * <p>Populated only when the reporting-token-receive gate is on; stored with
     * the message so a later abuse report can include the token.
     *
     * @return an {@link Optional} wrapping the reporting info
     */
    public Optional<MessageReceiveReportingInfo> reportingInfo() { return Optional.ofNullable(reportingInfo); }

    /**
     * Returns the broadcast contact list, defensively copied at construction
     * time.
     *
     * <p>Populated only for {@link MessageType#PEER_BROADCAST} and
     * {@link MessageType#DIRECT_PEER_STATUS} messages; empty otherwise. The list
     * lets the receiving device mirror the recipient list that the primary device
     * used.
     *
     * @return the broadcast contact list
     */
    public List<MessageReceiveBroadcastParticipant> bclParticipants() { return bclParticipants; }

    /**
     * Returns the parsed payment metadata, when present.
     *
     * <p>Populated when the stanza carries a {@code <pay>} or
     * {@code <transaction>} child; consumed by the WhatsApp Pay rendering path.
     *
     * @return an {@link Optional} wrapping the payment info
     */
    public Optional<MessageReceivePaymentInfo> paymentInfo() { return Optional.ofNullable(paymentInfo); }

    /**
     * Returns the stanza-level {@code eph_setting} attribute, when present.
     *
     * <p>Carried by {@link MessageType#OTHER_BROADCAST} messages to convey the
     * per-recipient ephemeral setting active for this broadcast delivery.
     *
     * @return an {@link Optional} wrapping the ephemeral setting
     */
    public Optional<String> ephSetting() { return Optional.ofNullable(ephSetting); }

    /**
     * Returns the raw {@code <rcat>} content bytes, when present.
     *
     * <p>Used by the protobuf decoder for content-binding verification on
     * specific message types; opaque to the receive pipeline.
     *
     * @return an {@link Optional} wrapping the rcat bytes
     */
    public Optional<byte[]> rcat() { return Optional.ofNullable(rcat); }

    /**
     * Returns the {@code tag} attribute on the {@code <hsm>} child, when present.
     *
     * <p>Identifies the highly-structured-message template applied to this
     * stanza; consumed by the business-template renderer.
     *
     * @return an {@link Optional} wrapping the HSM tag
     */
    public Optional<String> hsmTag() { return Optional.ofNullable(hsmTag); }

    /**
     * Returns the {@code category} attribute on the {@code <hsm>} child, when
     * present.
     *
     * <p>Identifies the HSM category (utility, marketing, authentication, and
     * similar); consumed alongside {@link #hsmTag()} by the business-template
     * renderer.
     *
     * @return an {@link Optional} wrapping the HSM category
     */
    public Optional<String> hsmCategory() { return Optional.ofNullable(hsmCategory); }

    /**
     * Returns the retry count from the first encrypted payload, when non-zero.
     *
     * <p>The receive pipeline emits a {@code retry} receipt with this count when
     * a previously failed decryption finally succeeds; returns empty when no
     * retry attempt has been made yet.
     *
     * @implNote
     * This implementation reads only the first payload's count because the
     * retry-receipt path reports a stanza-level retry count per delivery, not per
     * ciphertext.
     *
     * @return an {@link OptionalInt} wrapping the retry count
     */
    public OptionalInt retryCount() {
        if (encs.isEmpty()) {
            return OptionalInt.empty();
        }
        var count = encs.getFirst().retryCount();
        return count > 0 ? OptionalInt.of(count) : OptionalInt.empty();
    }

    /**
     * Returns whether any encrypted payload carried {@code decrypt-fail="hide"}.
     *
     * <p>Used by the dedup layer to suppress the usual decryption-failure
     * placeholder when the sender requested silent drop semantics; common on
     * sender-key distribution messages.
     *
     * @return {@code true} when at least one payload hides failures
     */
    public boolean hasHideFailPayload() {
        return encs.stream().anyMatch(MessageReceiveEncryptedPayload::hideFail);
    }

    /**
     * Returns whether the message was queued offline before delivery.
     *
     * <p>Pairs with {@link #offline()}; collapses the "is the attribute present"
     * check into a primitive boolean.
     *
     * @return {@code true} when the {@code offline} attribute was set
     */
    public boolean isOffline() {
        return offline != null;
    }

    /**
     * Returns whether the stanza is a peer-protocol message.
     *
     * <p>Equivalent to {@code "peer".equals(category)}; peer-protocol messages
     * are routed away from chat storage and into the peer handler.
     *
     * @return {@code true} when the category is {@code "peer"}
     */
    public boolean isPeer() {
        return "peer".equals(category);
    }

    /**
     * Returns whether every encrypted payload uses direct (non-SKMSG)
     * encryption.
     *
     * <p>Pivots the broadcast/status classification between peer-broadcast and
     * other-broadcast as well as between direct-peer-status and other-status.
     *
     * @return {@code true} when no payload is a sender-key message
     */
    public boolean isDirect() {
        return encs.stream().noneMatch(enc ->
                enc.e2eType().isSenderKeyMessage());
    }

    /**
     * Returns whether the sender is a companion device.
     *
     * <p>A device id of zero identifies the primary device; any other value is a
     * companion. Used by the receive pipeline to gate companion-only code paths.
     *
     * @return {@code true} when the sender's device id is non-zero
     */
    public boolean isCompanionDevice() {
        return senderJid.device() != 0;
    }

    /**
     * Returns whether any encrypted payload was a retry.
     *
     * <p>Companion to {@link #retryCount()}; signals whether the receive pipeline
     * should treat this delivery as a retry attempt at all.
     *
     * @return {@code true} when at least one payload's retry count is non-zero
     */
    public boolean isRetry() {
        return encs.stream().anyMatch(enc -> enc.retryCount() > 0);
    }
}
