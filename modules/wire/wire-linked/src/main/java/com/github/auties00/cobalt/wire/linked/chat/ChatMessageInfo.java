package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.linked.message.*;
import com.github.auties00.cobalt.wire.core.message.*;
import com.github.auties00.cobalt.wire.linked.message.addon.*;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupHistoryBundleInfo;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupHistoryIndividualMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.media.MediaData;
import com.github.auties00.cobalt.wire.linked.message.event.EventAdditionalMetadata;
import com.github.auties00.cobalt.wire.linked.message.event.EventResponse;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessageAdditionalMetadata;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusMentionMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusPSA;
import com.github.auties00.cobalt.wire.linked.message.MessageCitation;
import com.github.auties00.cobalt.wire.linked.message.MessageReportingTokenInfo;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.payment.PaymentInfo;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Represents a complete WhatsApp message as stored and synchronized in chats.
 *
 * <p>This is the primary message information model (protobuf wire name
 * {@code WebMessageInfo}) that carries both the encrypted message content and all
 * associated metadata: delivery status, sender identity, timestamps, ephemeral
 * settings, reactions, receipts, poll updates, pin/keep states, bot metadata,
 * and system stub information.
 *
 * <p>{@code ChatMessageInfo} is used for messages in regular chats (one-to-one
 * conversations, groups, and communities). For newsletter messages, see
 * {@link LinkedMessageInfo} and its other
 * permitted implementations.
 *
 * <p>Messages are identified by a {@link MessageKey} containing the chat JID,
 * a unique message ID string, and a direction flag ({@code fromMe}). The actual
 * message content is wrapped in a {@link LinkedMessageContainer}.
 *
 * @see MessageKey
 * @see LinkedMessageContainer
 * @see Chat
 */
@ProtobufMessage(name = "WebMessageInfo")
public final class ChatMessageInfo implements LinkedMessageInfo {
    /**
     * The key that uniquely identifies this message. Contains the chat JID,
     * message ID, and the {@code fromMe} flag indicating whether the current
     * user sent this message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The container holding the actual message content. May contain any
     * WhatsApp message type (text, image, video, document, protocol message,
     * etc.) as a oneof variant.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer message;

    /**
     * The timestamp (in epoch seconds) at which this message was sent or
     * received.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * The delivery status of this message (pending, sent to server, delivered
     * to recipient, read, or played for media messages).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    MessageStatus status;

    /**
     * The JID of the message sender. For outgoing messages this is the
     * current user's JID; for group messages this identifies which
     * participant sent the message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid senderJid;

    /**
     * The client-to-server timestamp (in epoch seconds), representing when
     * the client submitted the message to the server. May differ from
     * {@link #timestamp} if there was a delay in server processing.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant c2STimestamp;

    /**
     * Whether this message should be ignored during processing. Ignored
     * messages are typically protocol-level messages that are not displayed
     * in the chat UI.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    Boolean ignore;

    /**
     * Whether this message has been starred (bookmarked) by the user.
     * Starred messages can be quickly found via the starred messages view.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
    Boolean starred;

    /**
     * Whether this message was sent via a broadcast list. Broadcast messages
     * are delivered individually to each recipient but originate from a
     * single send action.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    Boolean broadcast;

    /**
     * The push name (display name) of the sender at the time the message was
     * sent. This is the name the sender has configured in their WhatsApp
     * profile.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    String pushName;

    /**
     * The SHA-256 hash of the encrypted media ciphertext, used to verify
     * media integrity and deduplicate media uploads.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    byte[] mediaCiphertextSha256;

    /**
     * Whether this message was sent using multicast delivery (to multiple
     * recipients in a single server request rather than individual sends).
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    Boolean multicast;

    /**
     * Whether the message text contains a URL. Used by the client to decide
     * whether to generate a link preview.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    Boolean urlText;

    /**
     * Whether the message text contains a phone number. Used by the client
     * to highlight phone numbers as tappable links.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    Boolean urlNumber;

    /**
     * The stub type of this message, if it is a system notification message
     * rather than a user-sent message. Stub messages represent events like
     * group creation, participant changes, encryption notifications, and
     * many other system events.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.ENUM)
    StubType stubType;

    /**
     * Whether to clear the media associated with this message. Set when
     * media has been deleted or when the message is being revoked.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    Boolean clearMedia;

    /**
     * Parameters associated with a stub (system) message. For example, a
     * group participant change stub includes the JID of the affected
     * participant as a parameter.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    List<String> stubParameters;

    /**
     * The duration in seconds for media messages (audio, video) or call
     * messages. Represents the length of the media content.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.UINT32)
    Integer duration;

    /**
     * The list of business labels applied to this message. Labels are a
     * WhatsApp Business feature that allows categorizing conversations
     * and messages.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.STRING)
    List<String> labels;

    /**
     * Payment information associated with this message, if it contains or
     * references a payment transaction.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    PaymentInfo paymentInfo;

    /**
     * The final location snapshot of a live location sharing message. When
     * live location sharing ends, this field stores the last known location.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    LiveLocationMessage finalLiveLocation;

    /**
     * Payment information from a quoted (replied-to) message that contained
     * a payment transaction.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    PaymentInfo quotedPaymentInfo;

    /**
     * The timestamp (in epoch seconds) at which the ephemeral deletion timer
     * started for this message. After the configured ephemeral duration
     * elapses from this timestamp, the message is automatically deleted.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant ephemeralStartTimestamp;

    /**
     * The ephemeral duration in seconds for this specific message. May differ
     * from the chat-level ephemeral setting if the setting was changed after
     * the message was sent.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.UINT32)
    Integer ephemeralDuration;

    /**
     * Whether the ephemeral setting transitioned from off to on when this
     * message was sent. Used to display a system notification about the
     * change.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    Boolean ephemeralOffToOn;

    /**
     * Whether this message's ephemeral timer is out of sync with the current
     * chat-level setting, indicating a mismatch that may need resolution.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    Boolean ephemeralOutOfSync;

    /**
     * The privacy status of a business account message, indicating whether
     * the message was processed with end-to-end encryption, via a Business
     * Solution Provider (BSP), or through Facebook hosting.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.ENUM)
    BizPrivacyStatus bizPrivacyStatus;

    /**
     * The verified business name of the sender, if the sender is a verified
     * WhatsApp Business account.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    String verifiedBizName;

    /**
     * Additional media data associated with this message, such as media
     * file size, dimensions, or processing metadata.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    MediaData mediaData;

    /**
     * Information about a group or contact photo change event. Contains
     * the old and new photo data when a photo change notification is
     * received.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    PhotoChange photoChange;

    /**
     * The list of delivery and read receipts for this message. Each receipt
     * tracks when the message was delivered to and read by a specific
     * recipient.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    List<MessageReceipt> receipts;

    /**
     * The list of emoji reactions to this message. Each reaction includes
     * the reactor's JID, the emoji used, and a timestamp.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    List<Reaction> reactions;

    /**
     * Media data for the sticker in a quoted (replied-to) message, allowing
     * the client to render a thumbnail of the quoted sticker.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    MediaData quotedStickerData;

    /**
     * Raw protobuf data for a message type that this client version does not
     * yet understand. Preserved for forward compatibility so that future
     * updates can parse the content.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.BYTES)
    byte[] futureproofData;

    /**
     * Public Service Announcement (PSA) metadata for status messages,
     * containing information about official WhatsApp announcements shown
     * in the status tab.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    StatusPSA statusPsa;

    /**
     * The list of poll vote updates received for this poll message. Each
     * record contains a voter's encrypted vote and the timestamp.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    List<PollVoteRecord> pollUpdates;

    /**
     * Additional metadata for poll messages, such as whether the poll
     * results are visible and when voting was closed.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    PollAdditionalMetadata pollAdditionalMetadata;

    /**
     * The identifier of the customer service agent who sent or is assigned
     * to this message, in the context of WhatsApp Business agent
     * conversations.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.STRING)
    String agentId;

    /**
     * Whether this status update has already been viewed by the current
     * user.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    Boolean statusAlreadyViewed;

    /**
     * The per-message encryption secret used for view-once messages and
     * other secret-based message types.
     */
    @ProtobufProperty(index = 49, type = ProtobufType.BYTES)
    byte[] messageSecret;

    /**
     * The "keep in chat" state for this message, indicating whether an
     * ephemeral message has been kept (preserved beyond the disappearing
     * timer) for all participants.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    KeepInChat keepInChat;

    /**
     * The JID of the original self-authored user when this message was
     * sent from a different identity (for example, after a phone number
     * change or account migration).
     */
    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    Jid originalSelfAuthorUserJidString;

    /**
     * The timestamp (in epoch seconds) at which this message was revoked
     * ("deleted for everyone"). Present only for revoked messages.
     */
    @ProtobufProperty(index = 52, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant revokeMessageTimestamp;

    /**
     * The "pin in chat" state for this message, indicating whether this
     * message has been pinned to the top of the conversation for all
     * participants.
     */
    @ProtobufProperty(index = 54, type = ProtobufType.MESSAGE)
    PinInChat pinInChat;

    /**
     * Premium message information, present when this message was sent as
     * a premium (paid) message or contains premium features.
     */
    @ProtobufProperty(index = 55, type = ProtobufType.MESSAGE)
    PremiumMessageInfo premiumMessageInfo;

    /**
     * Whether this message was sent by a first-party (Meta-operated) business
     * bot, as opposed to a third-party bot.
     */
    @ProtobufProperty(index = 56, type = ProtobufType.BOOL)
    Boolean is1PBizBotMessage;

    /**
     * Whether this message is part of a group history bundle that was shared
     * when a new member joined the group with history sharing enabled.
     */
    @ProtobufProperty(index = 57, type = ProtobufType.BOOL)
    Boolean isGroupHistoryMessage;

    /**
     * The JID of the user who invoked the bot that generated this message.
     * Present when a bot message was triggered by a specific user's
     * interaction.
     */
    @ProtobufProperty(index = 58, type = ProtobufType.STRING)
    Jid botMessageInvokerJid;

    /**
     * Comment thread metadata for this message, including the parent message
     * key and the total reply count.
     */
    @ProtobufProperty(index = 59, type = ProtobufType.MESSAGE)
    ChatCommentMetadata commentMetadata;

    /**
     * The list of event responses collected for this event message (for
     * example, RSVP responses to a group event).
     */
    @ProtobufProperty(index = 61, type = ProtobufType.MESSAGE)
    List<EventResponse> eventResponses;

    /**
     * Reporting token information for this message, used to generate tokens
     * that allow users to report messages without revealing encrypted content.
     */
    @ProtobufProperty(index = 62, type = ProtobufType.MESSAGE)
    MessageReportingTokenInfo reportingTokenInfo;

    /**
     * The server-assigned numeric identifier for this message in a newsletter
     * channel. Newsletter messages use sequential server IDs in addition to
     * the standard message key.
     */
    @ProtobufProperty(index = 63, type = ProtobufType.UINT64)
    Long newsletterServerId;

    /**
     * Additional metadata for event messages, such as event details and
     * configuration beyond what is in the event message content itself.
     */
    @ProtobufProperty(index = 64, type = ProtobufType.MESSAGE)
    EventAdditionalMetadata eventAdditionalMetadata;

    /**
     * Whether the current user is mentioned in this status update.
     */
    @ProtobufProperty(index = 65, type = ProtobufType.BOOL)
    Boolean isMentionedInStatus;

    /**
     * The list of JID strings for users who were mentioned in this status
     * update.
     */
    @ProtobufProperty(index = 66, type = ProtobufType.STRING)
    List<String> statusMentions;

    /**
     * The key of the target message that this message acts upon (for example,
     * when this message is an edit, reaction, or revocation targeting another
     * message).
     */
    @ProtobufProperty(index = 67, type = ProtobufType.MESSAGE)
    MessageKey targetMessageId;

    /**
     * The list of message add-ons attached to this message, such as pins,
     * keeps, and edits that have been applied as add-on operations.
     */
    @ProtobufProperty(index = 68, type = ProtobufType.MESSAGE)
    List<MessageAddOn> messageAddOns;

    /**
     * Information about a status mention message, containing the status
     * update content and the mention context.
     */
    @ProtobufProperty(index = 69, type = ProtobufType.MESSAGE)
    StatusMentionMessage statusMentionMessageInfo;

    /**
     * Whether this message was generated by the WhatsApp support AI
     * assistant.
     */
    @ProtobufProperty(index = 70, type = ProtobufType.BOOL)
    Boolean isSupportAiMessage;

    /**
     * The list of source identifiers for status mentions in this message.
     */
    @ProtobufProperty(index = 71, type = ProtobufType.STRING)
    List<String> statusMentionSources;

    /**
     * The list of citations provided by the support AI in its response,
     * referencing source material used to generate the answer.
     */
    @ProtobufProperty(index = 72, type = ProtobufType.MESSAGE)
    List<MessageCitation> supportAiCitations;

    /**
     * The target bot identifier for this message, used when routing a
     * message to a specific bot instance.
     */
    @ProtobufProperty(index = 73, type = ProtobufType.STRING)
    String botTargetId;

    /**
     * Information about an individual message within a group history sharing
     * bundle, including its original context.
     */
    @ProtobufProperty(index = 74, type = ProtobufType.MESSAGE)
    GroupHistoryIndividualMessageInfo groupHistoryIndividualMessageInfo;

    /**
     * Information about a group history sharing bundle as a whole, including
     * its processing state and the number of messages it contains.
     */
    @ProtobufProperty(index = 75, type = ProtobufType.MESSAGE)
    GroupHistoryBundleInfo groupHistoryBundleInfo;

    /**
     * Additional metadata for interactive messages (list messages, button
     * messages, product messages) beyond the message content itself.
     */
    @ProtobufProperty(index = 76, type = ProtobufType.MESSAGE)
    InteractiveMessageAdditionalMetadata interactiveMessageAdditionalMetadata;

    /**
     * Information about a quarantined message that was held back from
     * delivery due to content policy violations or other safety checks.
     */
    @ProtobufProperty(index = 77, type = ProtobufType.MESSAGE)
    QuarantinedMessage quarantinedMessage;

    /**
     * The count of non-JID mentions in this message. These are mentions
     * that reference entities other than specific users (for example,
     * everyone mentions or role mentions).
     */
    @ProtobufProperty(index = 78, type = ProtobufType.UINT32)
    Integer nonJidMentions;

    /**
     * Constructs a new {@code ChatMessageInfo} with all the specified field values.
     *
     * @param key                                the message key (must not be {@code null})
     * @param messageContainer                   the message content container
     * @param messageTimestamp                    the message timestamp
     * @param status                             the delivery status
     * @param senderJid                          the sender JID
     * @param c2STimestamp                       the client-to-server timestamp
     * @param ignore                             whether to ignore the message
     * @param starred                            whether the message is starred
     * @param broadcast                          whether sent via broadcast
     * @param pushName                           the sender's push name
     * @param mediaCiphertextSha256              the media ciphertext hash
     * @param multicast                          whether sent via multicast
     * @param urlText                            whether text contains a URL
     * @param urlNumber                          whether text contains a phone number
     * @param stubType                           the system stub type
     * @param clearMedia                         whether to clear media
     * @param stubParameters                     the stub parameters
     * @param duration                           the media duration
     * @param labels                             the business labels
     * @param paymentInfo                        the payment info
     * @param finalLiveLocation                  the final live location
     * @param quotedPaymentInfo                  the quoted payment info
     * @param ephemeralStartTimestamp             the ephemeral timer start
     * @param ephemeralDuration                  the ephemeral duration
     * @param ephemeralOffToOn                   whether ephemeral changed off to on
     * @param ephemeralOutOfSync                 whether ephemeral is out of sync
     * @param bizPrivacyStatus                   the business privacy status
     * @param verifiedBizName                    the verified business name
     * @param mediaData                          the media data
     * @param photoChange                        the photo change info
     * @param receipts                           the delivery receipts
     * @param reactions                          the reactions
     * @param quotedStickerData                  the quoted sticker data
     * @param futureproofData                    the futureproof data
     * @param statusPsa                          the status PSA
     * @param pollUpdates                        the poll updates
     * @param pollAdditionalMetadata             the poll metadata
     * @param agentId                            the agent ID
     * @param statusAlreadyViewed                whether status was viewed
     * @param messageSecret                      the message secret
     * @param keepInChat                         the keep in chat state
     * @param originalSelfAuthorUserJidString    the original author JID
     * @param revokeMessageTimestamp             the revoke timestamp
     * @param pinInChat                          the pin in chat state
     * @param premiumMessageInfo                 the premium message info
     * @param is1PBizBotMessage                  whether from first-party bot
     * @param isGroupHistoryMessage              whether part of group history
     * @param botMessageInvokerJid               the bot invoker JID
     * @param commentMetadata                    the comment metadata
     * @param eventResponses                     the event responses
     * @param reportingTokenInfo                 the reporting token info
     * @param newsletterServerId                 the newsletter server ID
     * @param eventAdditionalMetadata            the event metadata
     * @param isMentionedInStatus                whether mentioned in status
     * @param statusMentions                     the status mentions
     * @param targetMessageId                    the target message key
     * @param messageAddOns                      the message add-ons
     * @param statusMentionMessageInfo           the status mention message
     * @param isSupportAiMessage                 whether from support AI
     * @param statusMentionSources               the status mention sources
     * @param supportAiCitations                 the AI citations
     * @param botTargetId                        the bot target ID
     * @param groupHistoryIndividualMessageInfo  the group history individual info
     * @param groupHistoryBundleInfo             the group history bundle info
     * @param interactiveMessageAdditionalMetadata the interactive message metadata
     * @param quarantinedMessage                 the quarantined message info
     * @param nonJidMentions                     the non-JID mention count
     * @throws NullPointerException if {@code key} is {@code null}
     */
    ChatMessageInfo(MessageKey key, LinkedMessageContainer messageContainer, Instant messageTimestamp, MessageStatus status, Jid senderJid, Instant c2STimestamp, Boolean ignore, Boolean starred, Boolean broadcast, String pushName, byte[] mediaCiphertextSha256, Boolean multicast, Boolean urlText, Boolean urlNumber, StubType stubType, Boolean clearMedia, List<String> stubParameters, Integer duration, List<String> labels, PaymentInfo paymentInfo, LiveLocationMessage finalLiveLocation, PaymentInfo quotedPaymentInfo, Instant ephemeralStartTimestamp, Integer ephemeralDuration, Boolean ephemeralOffToOn, Boolean ephemeralOutOfSync, BizPrivacyStatus bizPrivacyStatus, String verifiedBizName, MediaData mediaData, PhotoChange photoChange, List<MessageReceipt> receipts, List<Reaction> reactions, MediaData quotedStickerData, byte[] futureproofData, StatusPSA statusPsa, List<PollVoteRecord> pollUpdates, PollAdditionalMetadata pollAdditionalMetadata, String agentId, Boolean statusAlreadyViewed, byte[] messageSecret, KeepInChat keepInChat, Jid originalSelfAuthorUserJidString, Instant revokeMessageTimestamp, PinInChat pinInChat, PremiumMessageInfo premiumMessageInfo, Boolean is1PBizBotMessage, Boolean isGroupHistoryMessage, Jid botMessageInvokerJid, ChatCommentMetadata commentMetadata, List<EventResponse> eventResponses, MessageReportingTokenInfo reportingTokenInfo, Long newsletterServerId, EventAdditionalMetadata eventAdditionalMetadata, Boolean isMentionedInStatus, List<String> statusMentions, MessageKey targetMessageId, List<MessageAddOn> messageAddOns, StatusMentionMessage statusMentionMessageInfo, Boolean isSupportAiMessage, List<String> statusMentionSources, List<MessageCitation> supportAiCitations, String botTargetId, GroupHistoryIndividualMessageInfo groupHistoryIndividualMessageInfo, GroupHistoryBundleInfo groupHistoryBundleInfo, InteractiveMessageAdditionalMetadata interactiveMessageAdditionalMetadata, QuarantinedMessage quarantinedMessage, Integer nonJidMentions) {
        this.key = Objects.requireNonNull(key);
        this.message = messageContainer;
        this.timestamp = messageTimestamp;
        this.status = status;
        this.senderJid = senderJid;
        if(key.senderJid().isEmpty()) {

        }
        this.c2STimestamp = c2STimestamp;
        this.ignore = ignore;
        this.starred = starred;
        this.broadcast = broadcast;
        this.pushName = pushName;
        this.mediaCiphertextSha256 = mediaCiphertextSha256;
        this.multicast = multicast;
        this.urlText = urlText;
        this.urlNumber = urlNumber;
        this.stubType = stubType;
        this.clearMedia = clearMedia;
        this.stubParameters = stubParameters;
        this.duration = duration;
        this.labels = labels;
        this.paymentInfo = paymentInfo;
        this.finalLiveLocation = finalLiveLocation;
        this.quotedPaymentInfo = quotedPaymentInfo;
        this.ephemeralStartTimestamp = ephemeralStartTimestamp;
        this.ephemeralDuration = ephemeralDuration;
        this.ephemeralOffToOn = ephemeralOffToOn;
        this.ephemeralOutOfSync = ephemeralOutOfSync;
        this.bizPrivacyStatus = bizPrivacyStatus;
        this.verifiedBizName = verifiedBizName;
        this.mediaData = mediaData;
        this.photoChange = photoChange;
        this.receipts = receipts;
        this.reactions = reactions;
        this.quotedStickerData = quotedStickerData;
        this.futureproofData = futureproofData;
        this.statusPsa = statusPsa;
        this.pollUpdates = pollUpdates;
        this.pollAdditionalMetadata = pollAdditionalMetadata;
        this.agentId = agentId;
        this.statusAlreadyViewed = statusAlreadyViewed;
        this.messageSecret = messageSecret;
        this.keepInChat = keepInChat;
        this.originalSelfAuthorUserJidString = originalSelfAuthorUserJidString;
        this.revokeMessageTimestamp = revokeMessageTimestamp;
        this.pinInChat = pinInChat;
        this.premiumMessageInfo = premiumMessageInfo;
        this.is1PBizBotMessage = is1PBizBotMessage;
        this.isGroupHistoryMessage = isGroupHistoryMessage;
        this.botMessageInvokerJid = botMessageInvokerJid;
        this.commentMetadata = commentMetadata;
        this.eventResponses = eventResponses;
        this.reportingTokenInfo = reportingTokenInfo;
        this.newsletterServerId = newsletterServerId;
        this.eventAdditionalMetadata = eventAdditionalMetadata;
        this.isMentionedInStatus = isMentionedInStatus;
        this.statusMentions = statusMentions;
        this.targetMessageId = targetMessageId;
        this.messageAddOns = messageAddOns;
        this.statusMentionMessageInfo = statusMentionMessageInfo;
        this.isSupportAiMessage = isSupportAiMessage;
        this.statusMentionSources = statusMentionSources;
        this.supportAiCitations = supportAiCitations;
        this.botTargetId = botTargetId;
        this.groupHistoryIndividualMessageInfo = groupHistoryIndividualMessageInfo;
        this.groupHistoryBundleInfo = groupHistoryBundleInfo;
        this.interactiveMessageAdditionalMetadata = interactiveMessageAdditionalMetadata;
        this.quarantinedMessage = quarantinedMessage;
        this.nonJidMentions = nonJidMentions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey key() {
        return key;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty {@link LinkedMessageContainer} if the message content has
     * not been set, rather than {@code null}.
     */
    @Override
    public LinkedMessageContainer message() {
        return message != null ? message : LinkedMessageContainer.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MessageStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the JID of the message sender.
     *
     * @return an {@link Optional} containing the sender JID, or empty if not
     *         available
     */
    public Optional<Jid> senderJid() {
        return Optional.ofNullable(senderJid);
    }

    /**
     * Returns the client-to-server timestamp for this message.
     *
     * @return an {@link Optional} containing the C2S timestamp, or empty if
     *         not available
     */
    public Optional<Instant> messageC2STimestamp() {
        return Optional.ofNullable(c2STimestamp);
    }

    /**
     * Returns whether this message should be ignored during processing.
     *
     * @return {@code true} if the message should be ignored
     */
    public boolean ignore() {
        return ignore != null && ignore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean starred() {
        return starred != null && starred;
    }

    /**
     * Returns whether this message was sent via a broadcast list.
     *
     * @return {@code true} if the message is a broadcast
     */
    public boolean broadcast() {
        return broadcast != null && broadcast;
    }

    /**
     * Returns the sender's push name at the time of sending.
     *
     * @return an {@link Optional} containing the push name, or empty if not
     *         available
     */
    public Optional<String> pushName() {
        return Optional.ofNullable(pushName);
    }

    /**
     * Returns the SHA-256 hash of the encrypted media ciphertext.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not
     *         a media message
     */
    public Optional<byte[]> mediaCiphertextSha256() {
        return Optional.ofNullable(mediaCiphertextSha256);
    }

    /**
     * Returns whether this message was sent using multicast delivery.
     *
     * @return {@code true} if multicast was used
     */
    public boolean multicast() {
        return multicast != null && multicast;
    }

    /**
     * Returns whether the message text contains a URL.
     *
     * @return {@code true} if the text contains a URL
     */
    public boolean urlText() {
        return urlText != null && urlText;
    }

    /**
     * Returns whether the message text contains a phone number.
     *
     * @return {@code true} if the text contains a phone number
     */
    public boolean urlNumber() {
        return urlNumber != null && urlNumber;
    }

    /**
     * Returns the system stub type of this message.
     *
     * @return an {@link Optional} containing the stub type, or empty if this
     *         is a regular user message
     */
    public Optional<StubType> messageStubType() {
        return Optional.ofNullable(stubType);
    }

    /**
     * Returns whether to clear the media associated with this message.
     *
     * @return {@code true} if media should be cleared
     */
    public boolean clearMedia() {
        return clearMedia != null && clearMedia;
    }

    /**
     * Returns the parameters for a system stub message.
     *
     * @return an unmodifiable list of stub parameters, never {@code null}
     */
    public List<String> messageStubParameters() {
        return stubParameters == null ? List.of() : Collections.unmodifiableList(stubParameters);
    }

    /**
     * Returns the duration of the media content in seconds.
     *
     * @return an {@link OptionalInt} containing the duration, or empty if not
     *         a media message
     */
    public OptionalInt duration() {
        return duration == null ? OptionalInt.empty() : OptionalInt.of(duration);
    }

    /**
     * Returns the business labels applied to this message.
     *
     * @return an unmodifiable list of label strings, never {@code null}
     */
    public List<String> labels() {
        return labels == null ? List.of() : Collections.unmodifiableList(labels);
    }

    /**
     * Returns the payment information for this message.
     *
     * @return an {@link Optional} containing the payment info, or empty if
     *         not a payment message
     */
    public Optional<PaymentInfo> paymentInfo() {
        return Optional.ofNullable(paymentInfo);
    }

    /**
     * Returns the final location of a live location sharing session.
     *
     * @return an {@link Optional} containing the final location, or empty if
     *         not applicable
     */
    public Optional<LiveLocationMessage> finalLiveLocation() {
        return Optional.ofNullable(finalLiveLocation);
    }

    /**
     * Returns the payment information from a quoted message.
     *
     * @return an {@link Optional} containing the quoted payment info, or
     *         empty if not present
     */
    public Optional<PaymentInfo> quotedPaymentInfo() {
        return Optional.ofNullable(quotedPaymentInfo);
    }

    /**
     * Returns the timestamp at which the ephemeral deletion timer started.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not
     *         an ephemeral message
     */
    public Optional<Instant> ephemeralStartTimestamp() {
        return Optional.ofNullable(ephemeralStartTimestamp);
    }

    /**
     * Returns the ephemeral duration in seconds for this message.
     *
     * @return an {@link OptionalInt} containing the duration, or empty if not
     *         an ephemeral message
     */
    public OptionalInt ephemeralDuration() {
        return ephemeralDuration == null ? OptionalInt.empty() : OptionalInt.of(ephemeralDuration);
    }

    /**
     * Returns whether the ephemeral setting transitioned from off to on.
     *
     * @return {@code true} if the transition occurred
     */
    public boolean ephemeralOffToOn() {
        return ephemeralOffToOn != null && ephemeralOffToOn;
    }

    /**
     * Returns whether this message's ephemeral timer is out of sync.
     *
     * @return {@code true} if there is an ephemeral sync mismatch
     */
    public boolean ephemeralOutOfSync() {
        return ephemeralOutOfSync != null && ephemeralOutOfSync;
    }

    /**
     * Returns the business privacy status for this message.
     *
     * @return an {@link Optional} containing the privacy status, or empty if
     *         not a business message
     */
    public Optional<BizPrivacyStatus> bizPrivacyStatus() {
        return Optional.ofNullable(bizPrivacyStatus);
    }

    /**
     * Returns the verified business name of the sender.
     *
     * @return an {@link Optional} containing the business name, or empty if
     *         the sender is not a verified business
     */
    public Optional<String> verifiedBizName() {
        return Optional.ofNullable(verifiedBizName);
    }

    /**
     * Returns additional media data for this message.
     *
     * @return an {@link Optional} containing the media data, or empty if not
     *         present
     */
    public Optional<MediaData> mediaData() {
        return Optional.ofNullable(mediaData);
    }

    /**
     * Returns the photo change information for this message.
     *
     * @return an {@link Optional} containing the photo change, or empty if
     *         not a photo change notification
     */
    public Optional<PhotoChange> photoChange() {
        return Optional.ofNullable(photoChange);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MessageReceipt> receipts() {
        return receipts == null ? List.of() : Collections.unmodifiableList(receipts);
    }

    /**
     * Returns the emoji reactions to this message.
     *
     * @return an unmodifiable list of reactions, never {@code null}
     */
    public List<Reaction> reactions() {
        return reactions == null ? List.of() : Collections.unmodifiableList(reactions);
    }

    /**
     * Returns the media data for a sticker in a quoted message.
     *
     * @return an {@link Optional} containing the sticker data, or empty if
     *         not present
     */
    public Optional<MediaData> quotedStickerData() {
        return Optional.ofNullable(quotedStickerData);
    }

    /**
     * Returns the raw futureproof data for unrecognized message types.
     *
     * @return an {@link Optional} containing the raw bytes, or empty if not
     *         present
     */
    public Optional<byte[]> futureproofData() {
        return Optional.ofNullable(futureproofData);
    }

    /**
     * Returns the PSA metadata for a status message.
     *
     * @return an {@link Optional} containing the status PSA, or empty if not
     *         a PSA status
     */
    public Optional<StatusPSA> statusPsa() {
        return Optional.ofNullable(statusPsa);
    }

    /**
     * Returns the poll vote updates for this poll message.
     *
     * @return an unmodifiable list of poll updates, never {@code null}
     */
    public List<PollVoteRecord> pollUpdates() {
        return pollUpdates == null ? List.of() : Collections.unmodifiableList(pollUpdates);
    }

    /**
     * Returns additional metadata for this poll message.
     *
     * @return an {@link Optional} containing the poll metadata, or empty if
     *         not a poll message
     */
    public Optional<PollAdditionalMetadata> pollAdditionalMetadata() {
        return Optional.ofNullable(pollAdditionalMetadata);
    }

    /**
     * Returns the business agent identifier for this message.
     *
     * @return an {@link Optional} containing the agent ID, or empty if not
     *         an agent conversation
     */
    public Optional<String> agentId() {
        return Optional.ofNullable(agentId);
    }

    /**
     * Returns whether this status update has been viewed by the current user.
     *
     * @return {@code true} if the status was already viewed
     */
    public boolean statusAlreadyViewed() {
        return statusAlreadyViewed != null && statusAlreadyViewed;
    }

    /**
     * Returns the per-message encryption secret.
     *
     * @return an {@link Optional} containing the secret bytes, or empty if
     *         not present
     */
    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    /**
     * Returns the "keep in chat" state for this ephemeral message.
     *
     * @return an {@link Optional} containing the keep state, or empty if not
     *         applicable
     */
    public Optional<KeepInChat> keepInChat() {
        return Optional.ofNullable(keepInChat);
    }

    /**
     * Returns the original self-authored user JID.
     *
     * @return an {@link Optional} containing the original JID, or empty if
     *         not applicable
     */
    public Optional<Jid> originalSelfAuthorUserJidString() {
        return Optional.ofNullable(originalSelfAuthorUserJidString);
    }

    /**
     * Returns the timestamp at which this message was revoked.
     *
     * @return an {@link Optional} containing the revoke timestamp, or empty
     *         if the message has not been revoked
     */
    public Optional<Instant> revokeMessageTimestamp() {
        return Optional.ofNullable(revokeMessageTimestamp);
    }

    /**
     * Returns the "pin in chat" state for this message.
     *
     * @return an {@link Optional} containing the pin state, or empty if the
     *         message is not pinned
     */
    public Optional<PinInChat> pinInChat() {
        return Optional.ofNullable(pinInChat);
    }

    /**
     * Returns the premium message information.
     *
     * @return an {@link Optional} containing the premium info, or empty if
     *         not a premium message
     */
    public Optional<PremiumMessageInfo> premiumMessageInfo() {
        return Optional.ofNullable(premiumMessageInfo);
    }

    /**
     * Returns whether this message is from a first-party business bot.
     *
     * @return {@code true} if from a first-party bot
     */
    public boolean is1PBizBotMessage() {
        return is1PBizBotMessage != null && is1PBizBotMessage;
    }

    /**
     * Returns whether this message is part of a group history sharing bundle.
     *
     * @return {@code true} if this is a group history message
     */
    public boolean isGroupHistoryMessage() {
        return isGroupHistoryMessage != null && isGroupHistoryMessage;
    }

    /**
     * Returns the JID of the user who invoked the bot.
     *
     * @return an {@link Optional} containing the invoker JID, or empty if
     *         not a bot message
     */
    public Optional<Jid> botMessageInvokerJid() {
        return Optional.ofNullable(botMessageInvokerJid);
    }

    /**
     * Returns the comment thread metadata for this message.
     *
     * @return an {@link Optional} containing the comment metadata, or empty
     *         if not part of a comment thread
     */
    public Optional<ChatCommentMetadata> commentMetadata() {
        return Optional.ofNullable(commentMetadata);
    }

    /**
     * Returns the event responses for this event message.
     *
     * @return an unmodifiable list of event responses, never {@code null}
     */
    public List<EventResponse> eventResponses() {
        return eventResponses == null ? List.of() : Collections.unmodifiableList(eventResponses);
    }

    /**
     * Returns the reporting token information for this message.
     *
     * @return an {@link Optional} containing the reporting info, or empty if
     *         not present
     */
    public Optional<MessageReportingTokenInfo> reportingTokenInfo() {
        return Optional.ofNullable(reportingTokenInfo);
    }

    /**
     * Returns the newsletter server-assigned message identifier.
     *
     * @return an {@link OptionalLong} containing the server ID, or empty if
     *         not a newsletter message
     */
    public OptionalLong newsletterServerId() {
        return newsletterServerId == null ? OptionalLong.empty() : OptionalLong.of(newsletterServerId);
    }

    /**
     * Returns additional metadata for an event message.
     *
     * @return an {@link Optional} containing the event metadata, or empty if
     *         not an event message
     */
    public Optional<EventAdditionalMetadata> eventAdditionalMetadata() {
        return Optional.ofNullable(eventAdditionalMetadata);
    }

    /**
     * Returns whether the current user is mentioned in this status update.
     *
     * @return {@code true} if mentioned in status
     */
    public boolean isMentionedInStatus() {
        return isMentionedInStatus != null && isMentionedInStatus;
    }

    /**
     * Returns the list of users mentioned in this status update.
     *
     * @return an unmodifiable list of mention JID strings, never {@code null}
     */
    public List<String> statusMentions() {
        return statusMentions == null ? List.of() : Collections.unmodifiableList(statusMentions);
    }

    /**
     * Returns the key of the target message this message acts upon.
     *
     * @return an {@link Optional} containing the target message key, or empty
     *         if not applicable
     */
    public Optional<MessageKey> targetMessageId() {
        return Optional.ofNullable(targetMessageId);
    }

    /**
     * Returns the message add-ons attached to this message.
     *
     * @return an unmodifiable list of add-ons, never {@code null}
     */
    public List<MessageAddOn> messageAddOns() {
        return messageAddOns == null ? List.of() : Collections.unmodifiableList(messageAddOns);
    }

    /**
     * Returns the status mention message information.
     *
     * @return an {@link Optional} containing the status mention message, or
     *         empty if not present
     */
    public Optional<StatusMentionMessage> statusMentionMessageInfo() {
        return Optional.ofNullable(statusMentionMessageInfo);
    }

    /**
     * Returns whether this message was generated by the support AI.
     *
     * @return {@code true} if from the support AI
     */
    public boolean isSupportAiMessage() {
        return isSupportAiMessage != null && isSupportAiMessage;
    }

    /**
     * Returns the source identifiers for status mentions.
     *
     * @return an unmodifiable list of source identifiers, never {@code null}
     */
    public List<String> statusMentionSources() {
        return statusMentionSources == null ? List.of() : Collections.unmodifiableList(statusMentionSources);
    }

    /**
     * Returns the support AI citations for this message.
     *
     * @return an unmodifiable list of citations, never {@code null}
     */
    public List<MessageCitation> supportAiCitations() {
        return supportAiCitations == null ? List.of() : Collections.unmodifiableList(supportAiCitations);
    }

    /**
     * Returns the target bot identifier for this message.
     *
     * @return an {@link Optional} containing the bot target ID, or empty if
     *         not present
     */
    public Optional<String> botTargetId() {
        return Optional.ofNullable(botTargetId);
    }

    /**
     * Returns the group history individual message information.
     *
     * @return an {@link Optional} containing the individual info, or empty if
     *         not part of a group history bundle
     */
    public Optional<GroupHistoryIndividualMessageInfo> groupHistoryIndividualMessageInfo() {
        return Optional.ofNullable(groupHistoryIndividualMessageInfo);
    }

    /**
     * Returns the group history bundle information.
     *
     * @return an {@link Optional} containing the bundle info, or empty if not
     *         a group history bundle
     */
    public Optional<GroupHistoryBundleInfo> groupHistoryBundleInfo() {
        return Optional.ofNullable(groupHistoryBundleInfo);
    }

    /**
     * Returns the interactive message additional metadata.
     *
     * @return an {@link Optional} containing the metadata, or empty if not an
     *         interactive message
     */
    public Optional<InteractiveMessageAdditionalMetadata> interactiveMessageAdditionalMetadata() {
        return Optional.ofNullable(interactiveMessageAdditionalMetadata);
    }

    /**
     * Returns the quarantined message information.
     *
     * @return an {@link Optional} containing the quarantine info, or empty if
     *         the message was not quarantined
     */
    public Optional<QuarantinedMessage> quarantinedMessage() {
        return Optional.ofNullable(quarantinedMessage);
    }

    /**
     * Returns the count of non-JID mentions in this message.
     *
     * @return an {@link OptionalInt} containing the count, or empty if not set
     */
    public OptionalInt nonJidMentions() {
        return nonJidMentions == null ? OptionalInt.empty() : OptionalInt.of(nonJidMentions);
    }

    /**
     * Sets the message key.
     *
     * @param key the new message key
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the message content container.
     *
     * @param messageContainer the new message container
     */
    public void setMessage(LinkedMessageContainer messageContainer) {
        this.message = messageContainer;
    }

    /**
     * Sets the message timestamp.
     *
     * @param timestamp the new timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the message delivery status.
     *
     * @param status the new status
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Sets the sender JID.
     *
     * @param senderJid the new sender JID
     */
    public void setSenderJid(Jid senderJid) {
        this.senderJid = senderJid;
    }

    /**
     * Sets the client-to-server timestamp.
     *
     * @param c2STimestamp the new C2S timestamp
     */
    public void setC2STimestamp(Instant c2STimestamp) {
        this.c2STimestamp = c2STimestamp;
    }

    /**
     * Sets whether this message should be ignored.
     *
     * @param ignore {@code true} to ignore, or {@code null} to clear
     */
    public void setIgnore(Boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Sets whether this message is starred.
     *
     * @param starred {@code true} to star, or {@code null} to clear
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
    }

    /**
     * Sets whether this message was sent via broadcast.
     *
     * @param broadcast {@code true} for broadcast, or {@code null} to clear
     */
    public void setBroadcast(Boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * Sets the sender's push name.
     *
     * @param pushName the push name, or {@code null} to clear
     */
    public void setPushName(String pushName) {
        this.pushName = pushName;
    }

    /**
     * Sets the media ciphertext SHA-256 hash.
     *
     * @param mediaCiphertextSha256 the hash bytes, or {@code null} to clear
     */
    public void setMediaCiphertextSha256(byte[] mediaCiphertextSha256) {
        this.mediaCiphertextSha256 = mediaCiphertextSha256;
    }

    /**
     * Sets whether this message was multicast.
     *
     * @param multicast {@code true} for multicast, or {@code null} to clear
     */
    public void setMulticast(Boolean multicast) {
        this.multicast = multicast;
    }

    /**
     * Sets whether the message text contains a URL.
     *
     * @param urlText {@code true} if contains URL, or {@code null} to clear
     */
    public void setUrlText(Boolean urlText) {
        this.urlText = urlText;
    }

    /**
     * Sets whether the message text contains a phone number.
     *
     * @param urlNumber {@code true} if contains number, or {@code null} to clear
     */
    public void setUrlNumber(Boolean urlNumber) {
        this.urlNumber = urlNumber;
    }

    /**
     * Sets the system stub type.
     *
     * @param stubType the stub type, or {@code null} to clear
     */
    public void setStubType(StubType stubType) {
        this.stubType = stubType;
    }

    /**
     * Sets whether to clear the media.
     *
     * @param clearMedia {@code true} to clear, or {@code null}
     */
    public void setClearMedia(Boolean clearMedia) {
        this.clearMedia = clearMedia;
    }

    /**
     * Sets the stub parameters.
     *
     * @param stubParameters the parameters, or {@code null} to clear
     */
    public void setStubParameters(List<String> stubParameters) {
        this.stubParameters = stubParameters;
    }

    /**
     * Sets the media duration in seconds.
     *
     * @param duration the duration, or {@code null} to clear
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Sets the business labels.
     *
     * @param labels the labels, or {@code null} to clear
     */
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    /**
     * Sets the payment information.
     *
     * @param paymentInfo the payment info, or {@code null} to clear
     */
    public void setPaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    /**
     * Sets the final live location.
     *
     * @param finalLiveLocation the final location, or {@code null} to clear
     */
    public void setFinalLiveLocation(LiveLocationMessage finalLiveLocation) {
        this.finalLiveLocation = finalLiveLocation;
    }

    /**
     * Sets the quoted payment information.
     *
     * @param quotedPaymentInfo the payment info, or {@code null} to clear
     */
    public void setQuotedPaymentInfo(PaymentInfo quotedPaymentInfo) {
        this.quotedPaymentInfo = quotedPaymentInfo;
    }

    /**
     * Sets the ephemeral timer start timestamp.
     *
     * @param ephemeralStartTimestamp the timestamp, or {@code null} to clear
     */
    public void setEphemeralStartTimestamp(Instant ephemeralStartTimestamp) {
        this.ephemeralStartTimestamp = ephemeralStartTimestamp;
    }

    /**
     * Sets the ephemeral duration in seconds.
     *
     * @param ephemeralDuration the duration, or {@code null} to clear
     */
    public void setEphemeralDuration(Integer ephemeralDuration) {
        this.ephemeralDuration = ephemeralDuration;
    }

    /**
     * Sets whether the ephemeral setting transitioned off to on.
     *
     * @param ephemeralOffToOn {@code true} if transitioned, or {@code null}
     */
    public void setEphemeralOffToOn(Boolean ephemeralOffToOn) {
        this.ephemeralOffToOn = ephemeralOffToOn;
    }

    /**
     * Sets whether the ephemeral timer is out of sync.
     *
     * @param ephemeralOutOfSync {@code true} if out of sync, or {@code null}
     */
    public void setEphemeralOutOfSync(Boolean ephemeralOutOfSync) {
        this.ephemeralOutOfSync = ephemeralOutOfSync;
    }

    /**
     * Sets the business privacy status.
     *
     * @param bizPrivacyStatus the status, or {@code null} to clear
     */
    public void setBizPrivacyStatus(BizPrivacyStatus bizPrivacyStatus) {
        this.bizPrivacyStatus = bizPrivacyStatus;
    }

    /**
     * Sets the verified business name.
     *
     * @param verifiedBizName the business name, or {@code null} to clear
     */
    public void setVerifiedBizName(String verifiedBizName) {
        this.verifiedBizName = verifiedBizName;
    }

    /**
     * Sets the media data.
     *
     * @param mediaData the media data, or {@code null} to clear
     */
    public void setMediaData(MediaData mediaData) {
        this.mediaData = mediaData;
    }

    /**
     * Sets the photo change information.
     *
     * @param photoChange the photo change, or {@code null} to clear
     */
    public void setPhotoChange(PhotoChange photoChange) {
        this.photoChange = photoChange;
    }

    /**
     * Sets the delivery receipts.
     *
     * @param messageReceipt the receipt list, or {@code null} to clear
     */
    public void setReceipts(List<MessageReceipt> messageReceipt) {
        this.receipts = messageReceipt;
    }

    /**
     * Sets the reactions.
     *
     * @param reactions the reaction list, or {@code null} to clear
     */
    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions;
    }

    /**
     * Sets the quoted sticker media data.
     *
     * @param quotedStickerData the sticker data, or {@code null} to clear
     */
    public void setQuotedStickerData(MediaData quotedStickerData) {
        this.quotedStickerData = quotedStickerData;
    }

    /**
     * Sets the futureproof data.
     *
     * @param futureproofData the raw bytes, or {@code null} to clear
     */
    public void setFutureproofData(byte[] futureproofData) {
        this.futureproofData = futureproofData;
    }

    /**
     * Sets the status PSA metadata.
     *
     * @param statusPsa the PSA metadata, or {@code null} to clear
     */
    public void setStatusPsa(StatusPSA statusPsa) {
        this.statusPsa = statusPsa;
    }

    /**
     * Sets the poll vote updates.
     *
     * @param pollUpdates the poll updates, or {@code null} to clear
     */
    public void setPollUpdates(List<PollVoteRecord> pollUpdates) {
        this.pollUpdates = pollUpdates;
    }

    /**
     * Sets the poll additional metadata.
     *
     * @param pollAdditionalMetadata the metadata, or {@code null} to clear
     */
    public void setPollAdditionalMetadata(PollAdditionalMetadata pollAdditionalMetadata) {
        this.pollAdditionalMetadata = pollAdditionalMetadata;
    }

    /**
     * Sets the business agent identifier.
     *
     * @param agentId the agent ID, or {@code null} to clear
     */
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    /**
     * Sets whether this status update has been viewed.
     *
     * @param statusAlreadyViewed {@code true} if viewed, or {@code null}
     */
    public void setStatusAlreadyViewed(Boolean statusAlreadyViewed) {
        this.statusAlreadyViewed = statusAlreadyViewed;
    }

    /**
     * Sets the per-message encryption secret.
     *
     * @param messageSecret the secret bytes, or {@code null} to clear
     */
    public void setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
    }

    /**
     * Sets the "keep in chat" state.
     *
     * @param keepInChat the keep state, or {@code null} to clear
     */
    public void setKeepInChat(KeepInChat keepInChat) {
        this.keepInChat = keepInChat;
    }

    /**
     * Sets the original self-authored user JID.
     *
     * @param originalSelfAuthorUserJidString the JID, or {@code null} to clear
     */
    public void setOriginalSelfAuthorUserJidString(Jid originalSelfAuthorUserJidString) {
        this.originalSelfAuthorUserJidString = originalSelfAuthorUserJidString;
    }

    /**
     * Sets the revoke timestamp.
     *
     * @param revokeMessageTimestamp the timestamp, or {@code null} to clear
     */
    public void setRevokeMessageTimestamp(Instant revokeMessageTimestamp) {
        this.revokeMessageTimestamp = revokeMessageTimestamp;
    }

    /**
     * Sets the "pin in chat" state.
     *
     * @param pinInChat the pin state, or {@code null} to clear
     */
    public void setPinInChat(PinInChat pinInChat) {
        this.pinInChat = pinInChat;
    }

    /**
     * Sets the premium message information.
     *
     * @param premiumMessageInfo the premium info, or {@code null} to clear
     */
    public void setPremiumMessageInfo(PremiumMessageInfo premiumMessageInfo) {
        this.premiumMessageInfo = premiumMessageInfo;
    }

    /**
     * Sets whether this message is from a first-party business bot.
     *
     * @param is1PBizBotMessage {@code true} if from first-party bot, or
     *                          {@code null} to clear
     */
    public void set1PBizBotMessage(Boolean is1PBizBotMessage) {
        this.is1PBizBotMessage = is1PBizBotMessage;
    }

    /**
     * Sets whether this is a group history message.
     *
     * @param isGroupHistoryMessage {@code true} if group history, or
     *                              {@code null} to clear
     */
    public void setGroupHistoryMessage(Boolean isGroupHistoryMessage) {
        this.isGroupHistoryMessage = isGroupHistoryMessage;
    }

    /**
     * Sets the bot message invoker JID.
     *
     * @param botMessageInvokerJid the invoker JID, or {@code null} to clear
     */
    public void setBotMessageInvokerJid(Jid botMessageInvokerJid) {
        this.botMessageInvokerJid = botMessageInvokerJid;
    }

    /**
     * Sets the comment thread metadata.
     *
     * @param commentMetadata the metadata, or {@code null} to clear
     */
    public void setCommentMetadata(ChatCommentMetadata commentMetadata) {
        this.commentMetadata = commentMetadata;
    }

    /**
     * Sets the event responses.
     *
     * @param eventResponses the responses, or {@code null} to clear
     */
    public void setEventResponses(List<EventResponse> eventResponses) {
        this.eventResponses = eventResponses;
    }

    /**
     * Sets the reporting token information.
     *
     * @param reportingTokenInfo the token info, or {@code null} to clear
     */
    public void setReportingTokenInfo(MessageReportingTokenInfo reportingTokenInfo) {
        this.reportingTokenInfo = reportingTokenInfo;
    }

    /**
     * Sets the newsletter server message identifier.
     *
     * @param newsletterServerId the server ID, or {@code null} to clear
     */
    public void setNewsletterServerId(Long newsletterServerId) {
        this.newsletterServerId = newsletterServerId;
    }

    /**
     * Sets the event additional metadata.
     *
     * @param eventAdditionalMetadata the metadata, or {@code null} to clear
     */
    public void setEventAdditionalMetadata(EventAdditionalMetadata eventAdditionalMetadata) {
        this.eventAdditionalMetadata = eventAdditionalMetadata;
    }

    /**
     * Sets whether the current user is mentioned in this status.
     *
     * @param isMentionedInStatus {@code true} if mentioned, or {@code null}
     */
    public void setMentionedInStatus(Boolean isMentionedInStatus) {
        this.isMentionedInStatus = isMentionedInStatus;
    }

    /**
     * Sets the status mentions.
     *
     * @param statusMentions the mention list, or {@code null} to clear
     */
    public void setStatusMentions(List<String> statusMentions) {
        this.statusMentions = statusMentions;
    }

    /**
     * Sets the target message key.
     *
     * @param targetMessageId the target key, or {@code null} to clear
     */
    public void setTargetMessageId(MessageKey targetMessageId) {
        this.targetMessageId = targetMessageId;
    }

    /**
     * Sets the message add-ons.
     *
     * @param messageAddOns the add-on list, or {@code null} to clear
     */
    public void setMessageAddOns(List<MessageAddOn> messageAddOns) {
        this.messageAddOns = messageAddOns;
    }

    /**
     * Sets the status mention message information.
     *
     * @param statusMentionMessageInfo the status mention message, or
     *                                 {@code null} to clear
     */
    public void setStatusMentionMessageInfo(StatusMentionMessage statusMentionMessageInfo) {
        this.statusMentionMessageInfo = statusMentionMessageInfo;
    }

    /**
     * Sets whether this message is from the support AI.
     *
     * @param isSupportAiMessage {@code true} if from support AI, or
     *                           {@code null} to clear
     */
    public void setSupportAiMessage(Boolean isSupportAiMessage) {
        this.isSupportAiMessage = isSupportAiMessage;
    }

    /**
     * Sets the status mention sources.
     *
     * @param statusMentionSources the source list, or {@code null} to clear
     */
    public void setStatusMentionSources(List<String> statusMentionSources) {
        this.statusMentionSources = statusMentionSources;
    }

    /**
     * Sets the support AI citations.
     *
     * @param supportAiCitations the citation list, or {@code null} to clear
     */
    public void setSupportAiCitations(List<MessageCitation> supportAiCitations) {
        this.supportAiCitations = supportAiCitations;
    }

    /**
     * Sets the bot target identifier.
     *
     * @param botTargetId the target ID, or {@code null} to clear
     */
    public void setBotTargetId(String botTargetId) {
        this.botTargetId = botTargetId;
    }

    /**
     * Sets the group history individual message information.
     *
     * @param groupHistoryIndividualMessageInfo the info, or {@code null} to clear
     */
    public void setGroupHistoryIndividualMessageInfo(GroupHistoryIndividualMessageInfo groupHistoryIndividualMessageInfo) {
        this.groupHistoryIndividualMessageInfo = groupHistoryIndividualMessageInfo;
    }

    /**
     * Sets the group history bundle information.
     *
     * @param groupHistoryBundleInfo the bundle info, or {@code null} to clear
     */
    public void setGroupHistoryBundleInfo(GroupHistoryBundleInfo groupHistoryBundleInfo) {
        this.groupHistoryBundleInfo = groupHistoryBundleInfo;
    }

    /**
     * Sets the interactive message additional metadata.
     *
     * @param interactiveMessageAdditionalMetadata the metadata, or {@code null}
     *                                             to clear
     */
    public void setInteractiveMessageAdditionalMetadata(InteractiveMessageAdditionalMetadata interactiveMessageAdditionalMetadata) {
        this.interactiveMessageAdditionalMetadata = interactiveMessageAdditionalMetadata;
    }

    /**
     * Sets the quarantined message information.
     *
     * @param quarantinedMessage the quarantine info, or {@code null} to clear
     */
    public void setQuarantinedMessage(QuarantinedMessage quarantinedMessage) {
        this.quarantinedMessage = quarantinedMessage;
    }

    /**
     * Sets the non-JID mention count.
     *
     * @param nonJidMentions the count, or {@code null} to clear
     */
    public void setNonJidMentions(Integer nonJidMentions) {
        this.nonJidMentions = nonJidMentions;
    }

    /**
     * Represents the privacy status of a WhatsApp Business message, indicating
     * the data-handling tier for the message content.
     *
     * <p>WhatsApp Business conversations can be processed through different
     * infrastructure tiers with varying privacy guarantees: fully end-to-end
     * encrypted (E2EE), hosted on Facebook infrastructure, routed through a
     * Business Solution Provider (BSP), or a combination thereof.
     */
    @ProtobufEnum(name = "WebMessageInfo.BizPrivacyStatus")
    public static enum BizPrivacyStatus {
        /**
         * The message is fully end-to-end encrypted with no third-party access.
         */
        E2EE(0),

        /**
         * The message is hosted on Facebook infrastructure.
         */
        FB(2),

        /**
         * The message is routed through a Business Solution Provider.
         */
        BSP(1),

        /**
         * The message is routed through both a Business Solution Provider and
         * Facebook infrastructure.
         */
        BSP_AND_FB(3);

        /**
         * Constructs a {@code BizPrivacyStatus} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        BizPrivacyStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this privacy status.
         *
         * @return the integer index used for wire serialization
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Identifies the type of system notification (stub) message in a WhatsApp chat.
     *
     * <p>Stub messages are not user-authored content. Instead, they represent
     * system events such as group membership changes, encryption status
     * transitions, missed calls, payment status updates, community operations,
     * and many other protocol-level or administrative notifications. The
     * WhatsApp client renders each stub type as a centered informational bubble
     * in the chat.
     */
    @ProtobufEnum(name = "WebMessageInfo.StubType")
    public static enum StubType {
        /** Unknown or unrecognized stub type. */
        UNKNOWN(0),
        /** A message was revoked ("deleted for everyone"). */
        REVOKE(1),
        /** A ciphertext message that has not yet been decrypted. */
        CIPHERTEXT(2),
        /** A futureproof message type not supported by this client version. */
        FUTUREPROOF(3),
        /** Identity key transition from non-verified state. */
        NON_VERIFIED_TRANSITION(4),
        /** Identity key transition from unverified state. */
        UNVERIFIED_TRANSITION(5),
        /** Identity key transition to verified state. */
        VERIFIED_TRANSITION(6),
        /** Verified identity at low-unknown trust level. */
        VERIFIED_LOW_UNKNOWN(7),
        /** Verified identity at high trust level. */
        VERIFIED_HIGH(8),
        /** Initial verified identity at unknown trust level. */
        VERIFIED_INITIAL_UNKNOWN(9),
        /** Initial verified identity at low trust level. */
        VERIFIED_INITIAL_LOW(10),
        /** Initial verified identity at high trust level. */
        VERIFIED_INITIAL_HIGH(11),
        /** Identity trust transition from any level to none. */
        VERIFIED_TRANSITION_ANY_TO_NONE(12),
        /** Identity trust transition from any level to high. */
        VERIFIED_TRANSITION_ANY_TO_HIGH(13),
        /** Identity trust transition from high to low. */
        VERIFIED_TRANSITION_HIGH_TO_LOW(14),
        /** Identity trust transition from high to unknown. */
        VERIFIED_TRANSITION_HIGH_TO_UNKNOWN(15),
        /** Identity trust transition from unknown to low. */
        VERIFIED_TRANSITION_UNKNOWN_TO_LOW(16),
        /** Identity trust transition from low to unknown. */
        VERIFIED_TRANSITION_LOW_TO_UNKNOWN(17),
        /** Identity trust transition from none to low. */
        VERIFIED_TRANSITION_NONE_TO_LOW(18),
        /** Identity trust transition from none to unknown. */
        VERIFIED_TRANSITION_NONE_TO_UNKNOWN(19),
        /** A new group was created. */
        GROUP_CREATE(20),
        /** The group subject was changed. */
        GROUP_CHANGE_SUBJECT(21),
        /** The group icon was changed. */
        GROUP_CHANGE_ICON(22),
        /** The group invite link was changed. */
        GROUP_CHANGE_INVITE_LINK(23),
        /** The group description was changed. */
        GROUP_CHANGE_DESCRIPTION(24),
        /** The group restrict setting was changed. */
        GROUP_CHANGE_RESTRICT(25),
        /** The group announce setting was changed. */
        GROUP_CHANGE_ANNOUNCE(26),
        /** A participant was added to the group. */
        GROUP_PARTICIPANT_ADD(27),
        /** A participant was removed from the group. */
        GROUP_PARTICIPANT_REMOVE(28),
        /** A participant was promoted to admin. */
        GROUP_PARTICIPANT_PROMOTE(29),
        /** A participant was demoted from admin. */
        GROUP_PARTICIPANT_DEMOTE(30),
        /** A participant was invited to the group. */
        GROUP_PARTICIPANT_INVITE(31),
        /** A participant left the group. */
        GROUP_PARTICIPANT_LEAVE(32),
        /** A participant changed their phone number. */
        GROUP_PARTICIPANT_CHANGE_NUMBER(33),
        /** A broadcast list was created. */
        BROADCAST_CREATE(34),
        /** A recipient was added to a broadcast list. */
        BROADCAST_ADD(35),
        /** A recipient was removed from a broadcast list. */
        BROADCAST_REMOVE(36),
        /** A generic system notification. */
        GENERIC_NOTIFICATION(37),
        /** A contact's encryption identity changed. */
        E2E_IDENTITY_CHANGED(38),
        /** Messages are now encrypted with this contact. */
        E2E_ENCRYPTED(39),
        /** A voice call was missed. */
        CALL_MISSED_VOICE(40),
        /** A video call was missed. */
        CALL_MISSED_VIDEO(41),
        /** A contact changed their phone number (individual chat). */
        INDIVIDUAL_CHANGE_NUMBER(42),
        /** A group was deleted. */
        GROUP_DELETE(43),
        /** A message bounced in an announcement-only group. */
        GROUP_ANNOUNCE_MODE_MESSAGE_BOUNCE(44),
        /** A group voice call was missed. */
        CALL_MISSED_GROUP_VOICE(45),
        /** A group video call was missed. */
        CALL_MISSED_GROUP_VIDEO(46),
        /** A payment ciphertext message. */
        PAYMENT_CIPHERTEXT(47),
        /** A futureproof payment message. */
        PAYMENT_FUTUREPROOF(48),
        /** A payment transaction failed. */
        PAYMENT_TRANSACTION_STATUS_UPDATE_FAILED(49),
        /** A payment transaction was refunded. */
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUNDED(50),
        /** A payment refund attempt failed. */
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUND_FAILED(51),
        /** The payment receiver needs to complete setup. */
        PAYMENT_TRANSACTION_STATUS_RECEIVER_PENDING_SETUP(52),
        /** The payment succeeded after an initial hiccup. */
        PAYMENT_TRANSACTION_STATUS_RECEIVER_SUCCESS_AFTER_HICCUP(53),
        /** A reminder to set up a payment account. */
        PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER(54),
        /** A reminder to send a payment. */
        PAYMENT_ACTION_SEND_PAYMENT_REMINDER(55),
        /** An invitation to send a payment. */
        PAYMENT_ACTION_SEND_PAYMENT_INVITATION(56),
        /** A payment request was declined. */
        PAYMENT_ACTION_REQUEST_DECLINED(57),
        /** A payment request expired. */
        PAYMENT_ACTION_REQUEST_EXPIRED(58),
        /** A payment request was cancelled. */
        PAYMENT_ACTION_REQUEST_CANCELLED(59),
        /** Business verification transition from top tier to bottom tier. */
        BIZ_VERIFIED_TRANSITION_TOP_TO_BOTTOM(60),
        /** Business verification transition from bottom tier to top tier. */
        BIZ_VERIFIED_TRANSITION_BOTTOM_TO_TOP(61),
        /** Business intro message (top section). */
        BIZ_INTRO_TOP(62),
        /** Business intro message (bottom section). */
        BIZ_INTRO_BOTTOM(63),
        /** A business changed its name. */
        BIZ_NAME_CHANGE(64),
        /** A business moved to the consumer WhatsApp app. */
        BIZ_MOVE_TO_CONSUMER_APP(65),
        /** Business two-tier migration notification (top). */
        BIZ_TWO_TIER_MIGRATION_TOP(66),
        /** Business two-tier migration notification (bottom). */
        BIZ_TWO_TIER_MIGRATION_BOTTOM(67),
        /** The message is oversized. */
        OVERSIZED(68),
        /** The group changed its frequently forwarded message setting. */
        GROUP_CHANGE_NO_FREQUENTLY_FORWARDED(69),
        /** A group V4 add invite was sent. */
        GROUP_V4_ADD_INVITE_SENT(70),
        /** A participant requested to join the group. */
        GROUP_PARTICIPANT_ADD_REQUEST_JOIN(71),
        /** The ephemeral (disappearing) messages setting was changed. */
        CHANGE_EPHEMERAL_SETTING(72),
        /** A contact's device list changed. */
        E2E_DEVICE_CHANGED(73),
        /** A view-once message was opened. */
        VIEWED_ONCE(74),
        /** Messages with this contact are now end-to-end encrypted. */
        E2E_ENCRYPTED_NOW(75),
        /** Blue message: BSP FB to BSP premise transition. */
        BLUE_MSG_BSP_FB_TO_BSP_PREMISE(76),
        /** Blue message: BSP FB to self FB transition. */
        BLUE_MSG_BSP_FB_TO_SELF_FB(77),
        /** Blue message: BSP FB to self premise transition. */
        BLUE_MSG_BSP_FB_TO_SELF_PREMISE(78),
        /** Blue message: BSP FB unverified. */
        BLUE_MSG_BSP_FB_UNVERIFIED(79),
        /** Blue message: BSP FB unverified to self premise verified. */
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(80),
        /** Blue message: BSP FB verified. */
        BLUE_MSG_BSP_FB_VERIFIED(81),
        /** Blue message: BSP FB verified to self premise unverified. */
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(82),
        /** Blue message: BSP premise to self premise. */
        BLUE_MSG_BSP_PREMISE_TO_SELF_PREMISE(83),
        /** Blue message: BSP premise unverified. */
        BLUE_MSG_BSP_PREMISE_UNVERIFIED(84),
        /** Blue message: BSP premise unverified to self premise verified. */
        BLUE_MSG_BSP_PREMISE_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(85),
        /** Blue message: BSP premise verified. */
        BLUE_MSG_BSP_PREMISE_VERIFIED(86),
        /** Blue message: BSP premise verified to self premise unverified. */
        BLUE_MSG_BSP_PREMISE_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(87),
        /** Blue message: consumer to BSP FB unverified. */
        BLUE_MSG_CONSUMER_TO_BSP_FB_UNVERIFIED(88),
        /** Blue message: consumer to BSP premise unverified. */
        BLUE_MSG_CONSUMER_TO_BSP_PREMISE_UNVERIFIED(89),
        /** Blue message: consumer to self FB unverified. */
        BLUE_MSG_CONSUMER_TO_SELF_FB_UNVERIFIED(90),
        /** Blue message: consumer to self premise unverified. */
        BLUE_MSG_CONSUMER_TO_SELF_PREMISE_UNVERIFIED(91),
        /** Blue message: self FB to BSP premise. */
        BLUE_MSG_SELF_FB_TO_BSP_PREMISE(92),
        /** Blue message: self FB to self premise. */
        BLUE_MSG_SELF_FB_TO_SELF_PREMISE(93),
        /** Blue message: self FB unverified. */
        BLUE_MSG_SELF_FB_UNVERIFIED(94),
        /** Blue message: self FB unverified to self premise verified. */
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(95),
        /** Blue message: self FB verified. */
        BLUE_MSG_SELF_FB_VERIFIED(96),
        /** Blue message: self FB verified to self premise unverified. */
        BLUE_MSG_SELF_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(97),
        /** Blue message: self premise to BSP premise. */
        BLUE_MSG_SELF_PREMISE_TO_BSP_PREMISE(98),
        /** Blue message: self premise unverified. */
        BLUE_MSG_SELF_PREMISE_UNVERIFIED(99),
        /** Blue message: self premise verified. */
        BLUE_MSG_SELF_PREMISE_VERIFIED(100),
        /** Blue message: transition to BSP FB. */
        BLUE_MSG_TO_BSP_FB(101),
        /** Blue message: transition to consumer. */
        BLUE_MSG_TO_CONSUMER(102),
        /** Blue message: transition to self FB. */
        BLUE_MSG_TO_SELF_FB(103),
        /** Blue message: unverified to BSP FB verified. */
        BLUE_MSG_UNVERIFIED_TO_BSP_FB_VERIFIED(104),
        /** Blue message: unverified to BSP premise verified. */
        BLUE_MSG_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(105),
        /** Blue message: unverified to self FB verified. */
        BLUE_MSG_UNVERIFIED_TO_SELF_FB_VERIFIED(106),
        /** Blue message: unverified to verified. */
        BLUE_MSG_UNVERIFIED_TO_VERIFIED(107),
        /** Blue message: verified to BSP FB unverified. */
        BLUE_MSG_VERIFIED_TO_BSP_FB_UNVERIFIED(108),
        /** Blue message: verified to BSP premise unverified. */
        BLUE_MSG_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(109),
        /** Blue message: verified to self FB unverified. */
        BLUE_MSG_VERIFIED_TO_SELF_FB_UNVERIFIED(110),
        /** Blue message: verified to unverified. */
        BLUE_MSG_VERIFIED_TO_UNVERIFIED(111),
        /** Blue message: BSP FB unverified to BSP premise verified. */
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(112),
        /** Blue message: BSP FB unverified to self FB verified. */
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_FB_VERIFIED(113),
        /** Blue message: BSP FB verified to BSP premise unverified. */
        BLUE_MSG_BSP_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(114),
        /** Blue message: BSP FB verified to self FB unverified. */
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_FB_UNVERIFIED(115),
        /** Blue message: self FB unverified to BSP premise verified. */
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(116),
        /** Blue message: self FB verified to BSP premise unverified. */
        BLUE_MSG_SELF_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(117),
        /** The contact's encryption identity is unavailable. */
        E2E_IDENTITY_UNAVAILABLE(118),
        /** A group is being created (in progress). */
        GROUP_CREATING(119),
        /** Group creation failed. */
        GROUP_CREATE_FAILED(120),
        /** A message bounced in the group. */
        GROUP_BOUNCED(121),
        /** A contact was blocked. */
        BLOCK_CONTACT(122),
        /** An ephemeral setting was not applied. */
        EPHEMERAL_SETTING_NOT_APPLIED(123),
        /** Message sync failed. */
        SYNC_FAILED(124),
        /** Messages are syncing. */
        SYNCING(125),
        /** Business privacy mode initialized with Facebook hosting. */
        BIZ_PRIVACY_MODE_INIT_FB(126),
        /** Business privacy mode initialized with BSP hosting. */
        BIZ_PRIVACY_MODE_INIT_BSP(127),
        /** Business privacy mode transitioned to Facebook hosting. */
        BIZ_PRIVACY_MODE_TO_FB(128),
        /** Business privacy mode transitioned to BSP hosting. */
        BIZ_PRIVACY_MODE_TO_BSP(129),
        /** The disappearing messages mode changed. */
        DISAPPEARING_MODE(130),
        /** Failed to fetch end-to-end encryption device information. */
        E2E_DEVICE_FETCH_FAILED(131),
        /** An admin revoked a message. */
        ADMIN_REVOKE(132),
        /** The group invite link growth is locked. */
        GROUP_INVITE_LINK_GROWTH_LOCKED(133),
        /** A community linked a parent group. */
        COMMUNITY_LINK_PARENT_GROUP(134),
        /** A community linked a sibling group. */
        COMMUNITY_LINK_SIBLING_GROUP(135),
        /** A community linked a sub-group. */
        COMMUNITY_LINK_SUB_GROUP(136),
        /** A community unlinked a parent group. */
        COMMUNITY_UNLINK_PARENT_GROUP(137),
        /** A community unlinked a sibling group. */
        COMMUNITY_UNLINK_SIBLING_GROUP(138),
        /** A community unlinked a sub-group. */
        COMMUNITY_UNLINK_SUB_GROUP(139),
        /** A participant accepted a group invite. */
        GROUP_PARTICIPANT_ACCEPT(140),
        /** A participant joined via a linked group. */
        GROUP_PARTICIPANT_LINKED_GROUP_JOIN(141),
        /** A community was created. */
        COMMUNITY_CREATE(142),
        /** A disappearing message was kept in the chat. */
        EPHEMERAL_KEEP_IN_CHAT(143),
        /** A membership join approval request was submitted. */
        GROUP_MEMBERSHIP_JOIN_APPROVAL_REQUEST(144),
        /** The group membership approval mode was changed. */
        GROUP_MEMBERSHIP_JOIN_APPROVAL_MODE(145),
        /** A parent group was unlinked due to integrity checks. */
        INTEGRITY_UNLINK_PARENT_GROUP(146),
        /** A community participant was promoted to admin. */
        COMMUNITY_PARTICIPANT_PROMOTE(147),
        /** A community participant was demoted from admin. */
        COMMUNITY_PARTICIPANT_DEMOTE(148),
        /** A community parent group was deleted. */
        COMMUNITY_PARENT_GROUP_DELETED(149),
        /** A community linked a parent group with membership approval. */
        COMMUNITY_LINK_PARENT_GROUP_MEMBERSHIP_APPROVAL(150),
        /** A participant joined both the group and its parent community. */
        GROUP_PARTICIPANT_JOINED_GROUP_AND_PARENT_GROUP(151),
        /** A masked (hidden) chat thread was created. */
        MASKED_THREAD_CREATED(152),
        /** A masked chat thread was revealed. */
        MASKED_THREAD_UNMASKED(153),
        /** A business chat was assigned to an agent. */
        BIZ_CHAT_ASSIGNMENT(154),
        /** A chat PSA (public service announcement) was shown. */
        CHAT_PSA(155),
        /** A poll was created. */
        CHAT_POLL_CREATION_MESSAGE(156),
        /** A CAG (community announcement group) masked thread was created. */
        CAG_MASKED_THREAD_CREATED(157),
        /** A community parent group subject was changed. */
        COMMUNITY_PARENT_GROUP_SUBJECT_CHANGED(158),
        /** A CAG invite auto-add occurred. */
        CAG_INVITE_AUTO_ADD(159),
        /** A business chat assignment was unassigned. */
        BIZ_CHAT_ASSIGNMENT_UNASSIGN(160),
        /** A CAG invite auto-join occurred. */
        CAG_INVITE_AUTO_JOINED(161),
        /** A scheduled call start message. */
        SCHEDULED_CALL_START_MESSAGE(162),
        /** A rich community invite. */
        COMMUNITY_INVITE_RICH(163),
        /** A rich community invite with auto-add. */
        COMMUNITY_INVITE_AUTO_ADD_RICH(164),
        /** A rich sub-group invite. */
        SUB_GROUP_INVITE_RICH(165),
        /** A rich sub-group participant add. */
        SUB_GROUP_PARTICIPANT_ADD_RICH(166),
        /** A rich community linked parent group notification. */
        COMMUNITY_LINK_PARENT_GROUP_RICH(167),
        /** A rich community participant add. */
        COMMUNITY_PARTICIPANT_ADD_RICH(168),
        /** A voice call from an unknown caller was silenced. */
        SILENCED_UNKNOWN_CALLER_AUDIO(169),
        /** A video call from an unknown caller was silenced. */
        SILENCED_UNKNOWN_CALLER_VIDEO(170),
        /** The group member add mode was changed. */
        GROUP_MEMBER_ADD_MODE(171),
        /** A non-admin join approval request. */
        GROUP_MEMBERSHIP_JOIN_APPROVAL_REQUEST_NON_ADMIN_ADD(172),
        /** A community description was changed. */
        COMMUNITY_CHANGE_DESCRIPTION(173),
        /** A sender invite notification. */
        SENDER_INVITE(174),
        /** A receiver invite notification. */
        RECEIVER_INVITE(175),
        /** The community allowed member-added groups setting was changed. */
        COMMUNITY_ALLOW_MEMBER_ADDED_GROUPS(176),
        /** A message was pinned in the chat. */
        PINNED_MESSAGE_IN_CHAT(177),
        /** Payment invite setup for the inviter. */
        PAYMENT_INVITE_SETUP_INVITER(178),
        /** Payment invite setup for receive-only invitee. */
        PAYMENT_INVITE_SETUP_INVITEE_RECEIVE_ONLY(179),
        /** Payment invite setup for send-and-receive invitee. */
        PAYMENT_INVITE_SETUP_INVITEE_SEND_AND_RECEIVE(180),
        /** A linked group call started. */
        LINKED_GROUP_CALL_START(181),
        /** Report-to-admin was enabled. */
        REPORT_TO_ADMIN_ENABLED_STATUS(182),
        /** An empty sub-group was created. */
        EMPTY_SUBGROUP_CREATE(183),
        /** A scheduled call was cancelled. */
        SCHEDULED_CALL_CANCEL(184),
        /** A sub-group admin triggered an auto-add (rich notification). */
        SUBGROUP_ADMIN_TRIGGERED_AUTO_ADD_RICH(185),
        /** The group recent history sharing setting was changed. */
        GROUP_CHANGE_RECENT_HISTORY_SHARING(186),
        /** A paid message with a server campaign ID. */
        PAID_MESSAGE_SERVER_CAMPAIGN_ID(187),
        /** A general chat was created within a community. */
        GENERAL_CHAT_CREATE(188),
        /** A member was added to a general chat. */
        GENERAL_CHAT_ADD(189),
        /** The general chat auto-add was disabled. */
        GENERAL_CHAT_AUTO_ADD_DISABLED(190),
        /** A suggested sub-group announcement. */
        SUGGESTED_SUBGROUP_ANNOUNCE(191),
        /** First-party business bot messaging was enabled. */
        BIZ_BOT_1P_MESSAGING_ENABLED(192),
        /** A username was changed. */
        CHANGE_USERNAME(193),
        /** Business coexistence privacy initialized (self). */
        BIZ_COEX_PRIVACY_INIT_SELF(194),
        /** Business coexistence privacy transitioned (self). */
        BIZ_COEX_PRIVACY_TRANSITION_SELF(195),
        /** Support AI education message. */
        SUPPORT_AI_EDUCATION(196),
        /** Third-party business bot messaging was enabled. */
        BIZ_BOT_3P_MESSAGING_ENABLED(197),
        /** A reminder was set up. */
        REMINDER_SETUP_MESSAGE(198),
        /** A reminder was sent. */
        REMINDER_SENT_MESSAGE(199),
        /** A reminder was cancelled. */
        REMINDER_CANCEL_MESSAGE(200),
        /** Business coexistence privacy initialized. */
        BIZ_COEX_PRIVACY_INIT(201),
        /** Business coexistence privacy transitioned. */
        BIZ_COEX_PRIVACY_TRANSITION(202),
        /** A group was deactivated. */
        GROUP_DEACTIVATED(203),
        /** A community sibling group was deactivated. */
        COMMUNITY_DEACTIVATE_SIBLING_GROUP(204),
        /** An event was updated. */
        EVENT_UPDATED(205),
        /** An event was canceled. */
        EVENT_CANCELED(206),
        /** The community owner was updated. */
        COMMUNITY_OWNER_UPDATED(207),
        /** A community sub-group visibility was set to hidden. */
        COMMUNITY_SUB_GROUP_VISIBILITY_HIDDEN(208),
        /** A CAPI group non-E2EE system message. */
        CAPI_GROUP_NE2EE_SYSTEM_MESSAGE(209),
        /** A status mention notification. */
        STATUS_MENTION(210),
        /** A user controls system message. */
        USER_CONTROLS_SYSTEM_MESSAGE(211),
        /** A support system message. */
        SUPPORT_SYSTEM_MESSAGE(212),
        /** A LID (Linked Identity) change notification. */
        CHANGE_LID(213),
        /** A business customer opted in to third-party data sharing. */
        BIZ_CUSTOMER_3PD_DATA_SHARING_OPT_IN_MESSAGE(214),
        /** A business customer opted out of third-party data sharing. */
        BIZ_CUSTOMER_3PD_DATA_SHARING_OPT_OUT_MESSAGE(215),
        /** The limit sharing setting was changed. */
        CHANGE_LIMIT_SHARING(216),
        /** The group member link mode was changed. */
        GROUP_MEMBER_LINK_MODE(217),
        /** A business automatically labeled a chat. */
        BIZ_AUTOMATICALLY_LABELED_CHAT_SYSTEM_MESSAGE(218),
        /** Phone number hiding chat deprecated message. */
        PHONE_NUMBER_HIDING_CHAT_DEPRECATED_MESSAGE(219),
        /** A message was quarantined. */
        QUARANTINED_MESSAGE(220),
        /** The group member share group history mode was changed. */
        GROUP_MEMBER_SHARE_GROUP_HISTORY_MODE(221),
        /** An open bot was added to the group. */
        GROUP_OPEN_BOT_ADDED(222),
        /** A TEE (Trusted Execution Environment) bot was added to the group. */
        GROUP_TEE_BOT_ADDED(223);

        /**
         * Constructs a {@code StubType} with the specified protobuf index.
         *
         * @param index the protobuf enum index
         */
        StubType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this stub type.
         *
         * @return the integer index used for wire serialization
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Represents the "keep in chat" state for an ephemeral message, indicating
     * whether the message has been preserved beyond the disappearing messages
     * timer.
     *
     * <p>When a user "keeps" a disappearing message, the server records this
     * action along with timestamps and the device that performed it. The kept
     * message persists even after the ephemeral timer expires, until it is
     * explicitly un-kept.
     */
    @ProtobufMessage(name = "KeepInChat")
    public static final class KeepInChat {
        /**
         * The type of keep action (keep for all, undo keep for all, or unknown).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        ChatKeepType keepType;

        /**
         * The server timestamp (in epoch seconds) at which the keep action
         * was recorded.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
        Instant serverTimestamp;

        /**
         * The key of the message that was kept or un-kept.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        MessageKey key;

        /**
         * The JID of the device that performed the keep action.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Jid deviceJid;

        /**
         * The client timestamp (in epoch milliseconds) at which the keep
         * action was performed on the device.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
        Instant clientTimestampMs;

        /**
         * The server timestamp (in epoch milliseconds) at which the keep
         * action was recorded. This is a higher-precision variant of
         * {@link #serverTimestamp}.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
        Instant serverTimestampMs;

        /**
         * Constructs a new {@code KeepInChat} with the specified values.
         *
         * @param keepType          the keep action type
         * @param serverTimestamp   the server timestamp in seconds
         * @param key               the kept message key
         * @param deviceJid         the device that performed the action
         * @param clientTimestampMs the client timestamp in milliseconds
         * @param serverTimestampMs the server timestamp in milliseconds
         */
        KeepInChat(ChatKeepType keepType, Instant serverTimestamp, MessageKey key, Jid deviceJid, Instant clientTimestampMs, Instant serverTimestampMs) {
            this.keepType = keepType;
            this.serverTimestamp = serverTimestamp;
            this.key = key;
            this.deviceJid = deviceJid;
            this.clientTimestampMs = clientTimestampMs;
            this.serverTimestampMs = serverTimestampMs;
        }

        /**
         * Returns the type of keep action.
         *
         * @return an {@link Optional} containing the keep type, or empty if
         *         not set
         */
        public Optional<ChatKeepType> keepType() {
            return Optional.ofNullable(keepType);
        }

        /**
         * Returns the server timestamp in epoch seconds.
         *
         * @return an {@link Optional} containing the timestamp, or empty if
         *         not set
         */
        public Optional<Instant> serverTimestamp() {
            return Optional.ofNullable(serverTimestamp);
        }

        /**
         * Returns the key of the kept message.
         *
         * @return an {@link Optional} containing the message key, or empty if
         *         not set
         */
        public Optional<MessageKey> key() {
            return Optional.ofNullable(key);
        }

        /**
         * Returns the JID of the device that performed the action.
         *
         * @return an {@link Optional} containing the device JID, or empty if
         *         not set
         */
        public Optional<Jid> deviceJid() {
            return Optional.ofNullable(deviceJid);
        }

        /**
         * Returns the client timestamp in epoch milliseconds.
         *
         * @return an {@link Optional} containing the timestamp, or empty if
         *         not set
         */
        public Optional<Instant> clientTimestampMs() {
            return Optional.ofNullable(clientTimestampMs);
        }

        /**
         * Returns the server timestamp in epoch milliseconds.
         *
         * @return an {@link Optional} containing the timestamp, or empty if
         *         not set
         */
        public Optional<Instant> serverTimestampMs() {
            return Optional.ofNullable(serverTimestampMs);
        }

        /**
         * Sets the keep action type.
         *
         * @param keepType the keep type, or {@code null} to clear
         */
        public void setKeepType(ChatKeepType keepType) {
            this.keepType = keepType;
    }

        /**
         * Sets the server timestamp in seconds.
         *
         * @param serverTimestamp the timestamp, or {@code null} to clear
         */
        public void setServerTimestamp(Instant serverTimestamp) {
            this.serverTimestamp = serverTimestamp;
    }

        /**
         * Sets the kept message key.
         *
         * @param key the message key, or {@code null} to clear
         */
        public void setKey(MessageKey key) {
            this.key = key;
    }

        /**
         * Sets the device JID that performed the action.
         *
         * @param deviceJid the device JID, or {@code null} to clear
         */
        public void setDeviceJid(Jid deviceJid) {
            this.deviceJid = deviceJid;
    }

        /**
         * Sets the client timestamp in milliseconds.
         *
         * @param clientTimestampMs the timestamp, or {@code null} to clear
         */
        public void setClientTimestampMs(Instant clientTimestampMs) {
            this.clientTimestampMs = clientTimestampMs;
    }

        /**
         * Sets the server timestamp in milliseconds.
         *
         * @param serverTimestampMs the timestamp, or {@code null} to clear
         */
        public void setServerTimestampMs(Instant serverTimestampMs) {
            this.serverTimestampMs = serverTimestampMs;
    }
    }

    /**
     * Represents a change in the group or contact profile photo, carrying
     * both the old and new photo data.
     *
     * <p>This protobuf message (wire name {@code PhotoChange}) is attached to
     * system notification messages that inform participants about profile photo
     * updates.
     */
    @ProtobufMessage(name = "PhotoChange")
    public static final class PhotoChange {
        /**
         * The raw bytes of the previous profile photo, before the change.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] oldPhoto;

        /**
         * The raw bytes of the new profile photo, after the change.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] newPhoto;

        /**
         * The server-assigned identifier for the new photo.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        Integer newPhotoId;

        /**
         * Constructs a new {@code PhotoChange} with the specified values.
         *
         * @param oldPhoto   the old photo bytes, or {@code null}
         * @param newPhoto   the new photo bytes, or {@code null}
         * @param newPhotoId the new photo identifier, or {@code null}
         */
        PhotoChange(byte[] oldPhoto, byte[] newPhoto, Integer newPhotoId) {
            this.oldPhoto = oldPhoto;
            this.newPhoto = newPhoto;
            this.newPhotoId = newPhotoId;
        }

        /**
         * Returns the old profile photo data.
         *
         * @return an {@link Optional} containing the old photo bytes, or empty
         *         if not available
         */
        public Optional<byte[]> oldPhoto() {
            return Optional.ofNullable(oldPhoto);
        }

        /**
         * Returns the new profile photo data.
         *
         * @return an {@link Optional} containing the new photo bytes, or empty
         *         if not available
         */
        public Optional<byte[]> newPhoto() {
            return Optional.ofNullable(newPhoto);
        }

        /**
         * Returns the identifier of the new photo.
         *
         * @return an {@link OptionalInt} containing the photo ID, or empty if
         *         not set
         */
        public OptionalInt newPhotoId() {
            return newPhotoId == null ? OptionalInt.empty() : OptionalInt.of(newPhotoId);
        }

        /**
         * Sets the old photo data.
         *
         * @param oldPhoto the old photo bytes, or {@code null} to clear
         */
        public void setOldPhoto(byte[] oldPhoto) {
            this.oldPhoto = oldPhoto;
    }

        /**
         * Sets the new photo data.
         *
         * @param newPhoto the new photo bytes, or {@code null} to clear
         */
        public void setNewPhoto(byte[] newPhoto) {
            this.newPhoto = newPhoto;
    }

        /**
         * Sets the new photo identifier.
         *
         * @param newPhotoId the photo ID, or {@code null} to clear
         */
        public void setNewPhotoId(Integer newPhotoId) {
            this.newPhotoId = newPhotoId;
    }
    }

    /**
     * Represents the state of a message that has been pinned or unpinned in a
     * WhatsApp chat.
     *
     * <p>Pinned messages are displayed prominently at the top of the conversation
     * or in a dedicated pinned messages section. This protobuf message (wire name
     * {@code PinInChat}) records the pin action type, the pinned message key,
     * and associated timestamps.
     */
    @ProtobufMessage(name = "PinInChat")
    public static final class PinInChat {
        /**
         * The pin action type (pin for all, unpin for all, or unknown).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        Type type;

        /**
         * The key of the message that was pinned or unpinned.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        MessageKey key;

        /**
         * The client-side timestamp (in epoch milliseconds) at which the pin
         * action was performed.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
        Instant senderTimestampMs;

        /**
         * The server-side timestamp (in epoch milliseconds) at which the pin
         * action was recorded.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
        Instant serverTimestampMs;

        /**
         * Context information about this pin action when treated as a message
         * add-on.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        MessageAddOnContextInfo messageAddOnContextInfo;

        /**
         * Constructs a new {@code PinInChat} with the specified values.
         *
         * @param type                      the pin action type
         * @param key                       the pinned message key
         * @param senderTimestampMs         the client timestamp
         * @param serverTimestampMs         the server timestamp
         * @param messageAddOnContextInfo   the add-on context info
         */
        PinInChat(Type type, MessageKey key, Instant senderTimestampMs, Instant serverTimestampMs, MessageAddOnContextInfo messageAddOnContextInfo) {
            this.type = type;
            this.key = key;
            this.senderTimestampMs = senderTimestampMs;
            this.serverTimestampMs = serverTimestampMs;
            this.messageAddOnContextInfo = messageAddOnContextInfo;
        }

        /**
         * Returns the pin action type.
         *
         * @return an {@link Optional} containing the type, or empty if not set
         */
        public Optional<Type> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Returns the key of the pinned or unpinned message.
         *
         * @return an {@link Optional} containing the message key, or empty if
         *         not set
         */
        public Optional<MessageKey> key() {
            return Optional.ofNullable(key);
        }

        /**
         * Returns the client timestamp of the pin action.
         *
         * @return an {@link Optional} containing the timestamp, or empty if
         *         not set
         */
        public Optional<Instant> senderTimestampMs() {
            return Optional.ofNullable(senderTimestampMs);
        }

        /**
         * Returns the server timestamp of the pin action.
         *
         * @return an {@link Optional} containing the timestamp, or empty if
         *         not set
         */
        public Optional<Instant> serverTimestampMs() {
            return Optional.ofNullable(serverTimestampMs);
        }

        /**
         * Returns the message add-on context information.
         *
         * @return an {@link Optional} containing the context info, or empty if
         *         not present
         */
        public Optional<MessageAddOnContextInfo> messageAddOnContextInfo() {
            return Optional.ofNullable(messageAddOnContextInfo);
        }

        /**
         * Sets the pin action type.
         *
         * @param type the type, or {@code null} to clear
         */
        public void setType(Type type) {
            this.type = type;
    }

        /**
         * Sets the pinned message key.
         *
         * @param key the message key, or {@code null} to clear
         */
        public void setKey(MessageKey key) {
            this.key = key;
    }

        /**
         * Sets the client timestamp of the pin action.
         *
         * @param senderTimestampMs the timestamp, or {@code null} to clear
         */
        public void setSenderTimestampMs(Instant senderTimestampMs) {
            this.senderTimestampMs = senderTimestampMs;
    }

        /**
         * Sets the server timestamp of the pin action.
         *
         * @param serverTimestampMs the timestamp, or {@code null} to clear
         */
        public void setServerTimestampMs(Instant serverTimestampMs) {
            this.serverTimestampMs = serverTimestampMs;
    }

        /**
         * Sets the message add-on context information.
         *
         * @param messageAddOnContextInfo the context info, or {@code null}
         *                                to clear
         */
        public void setMessageAddOnContextInfo(MessageAddOnContextInfo messageAddOnContextInfo) {
            this.messageAddOnContextInfo = messageAddOnContextInfo;
    }

        /**
         * Identifies the type of pin action performed on a message in a chat.
         */
        @ProtobufEnum(name = "PinInChat.Type")
        public static enum Type {
            /**
             * The pin type is unknown or unspecified.
             */
            UNKNOWN_TYPE(0),

            /**
             * The message was pinned for all participants in the chat.
             */
            PIN_FOR_ALL(1),

            /**
             * The message was unpinned for all participants in the chat.
             */
            UNPIN_FOR_ALL(2);

            /**
             * Constructs a {@code Type} with the specified protobuf index.
             *
             * @param index the protobuf enum index
             */
            Type(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf-assigned index for this enum constant.
             */
            final int index;

            /**
             * Returns the protobuf index of this pin type.
             *
             * @return the integer index used for wire serialization
             */
            public int index() {
                return this.index;
            }
        }
    }
}
