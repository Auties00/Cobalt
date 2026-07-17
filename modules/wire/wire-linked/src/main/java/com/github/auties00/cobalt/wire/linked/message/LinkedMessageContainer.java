package com.github.auties00.cobalt.wire.linked.message;
import com.github.auties00.cobalt.wire.core.message.MessageContainer;

import com.github.auties00.cobalt.wire.linked.message.bot.AIRichResponseMessage;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.linked.message.call.*;
import com.github.auties00.cobalt.wire.linked.message.commerce.*;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventMessage;
import com.github.auties00.cobalt.wire.linked.message.group.GroupInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.group.SenderKeyDistributionMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateButtonReplyMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterAdminInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterFollowerInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.payment.*;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollResultSnapshotMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.security.PlaceholderMessage;
import com.github.auties00.cobalt.wire.linked.message.security.SecretEncMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusNotificationMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusQuestionAnswerMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusQuotedMessage;
import com.github.auties00.cobalt.wire.linked.message.status.StatusStickerInteractionMessage;
import com.github.auties00.cobalt.wire.linked.message.system.*;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryBundle;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryNotice;
import com.github.auties00.cobalt.wire.linked.message.text.*;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Discriminated union holding exactly one WhatsApp message payload of any
 * known type, together with a small number of transport-level side-channel
 * fields.
 *
 * <p>Every message transmitted on WhatsApp is carried inside a container
 * of this shape. Although the type declares many fields (one for each
 * kind of payload the protocol can deliver), at most one payload field is
 * populated in any given instance: the populated field determines the
 * kind of content the recipient actually sees (text, image, poll, call,
 * protocol action, and so on).
 *
 * <p>Three additional fields may coexist alongside the payload without
 * counting as content on their own:
 * <ul>
 * <li>{@link #messageContextInfo()} carries per-message transport
 *     metadata such as the sender's device list, message encryption
 *     secrets, and bot metadata.</li>
 * <li>{@link #senderKeyDistributionMessage()} piggybacks a group
 *     Signal sender-key distribution onto the message so that new
 *     participants can decrypt subsequent group messages.</li>
 * <li>{@link #fastRatchetKeySenderKeyDistributionMessage()} carries
 *     the fast-ratchet variant of the same sender-key distribution.</li>
 * </ul>
 *
 * <h2>FutureProofMessage wrappers</h2>
 *
 * <p>A subset of fields is typed as {@link FutureProofMessage} rather
 * than as a concrete payload. These are forward-compatibility envelopes
 * containing a nested {@code LinkedMessageContainer}, which in turn holds the
 * actual content. The indirection lets older clients that do not
 * recognise a new payload type forward the raw protobuf bytes untouched
 * so that other participants can still display them.
 *
 * <p>Ephemeral (disappearing) messages, view-once media, message edits,
 * documents with captions, and several status features all use this
 * wrapping. {@link #content()} recursively unwraps every envelope and
 * returns the innermost {@link Message}.
 *
 * <h2>DeviceSentMessage</h2>
 *
 * <p>When the logged-in user sends a message from one of their linked
 * devices, WhatsApp distributes a copy to every other device owned by
 * the same user, wrapped inside a {@link DeviceSentMessage}.
 * {@link #content()} transparently unwraps this layer so callers always
 * receive the original payload regardless of how it arrived.
 *
 * <h2>Versioned payloads</h2>
 *
 * <p>Several message kinds occupy multiple protobuf field indices to
 * maintain backwards compatibility across client generations. Poll
 * creation messages for example occupy five different indices and
 * view-once media three. Cobalt maps every version to the same Java
 * type. The factory methods (such as {@link #of(Message)} and
 * {@link #ofViewOnce(Message)}) and the instance converters (such as
 * {@link #toViewOnce()} and {@link #toGroupStatus()}) always produce
 * the newest version; {@link #content()} transparently reads any
 * version.
 *
 * <h2>Usage</h2>
 *
 * <p>Create a container from any supported message:
 * <pre>{@code
 *     LinkedMessageContainer container = LinkedMessageContainer.of(myImageMessage);
 *     LinkedMessageContainer text = LinkedMessageContainer.of("Hello, world!");
 *     LinkedMessageContainer ephemeral = LinkedMessageContainer.ofEphemeral(myTextMessage);
 * }</pre>
 *
 * <p>Retrieve the innermost payload, unwrapping every envelope:
 * <pre>{@code
 *     Message msg = container.content();
 *     Optional<ContextualMessage> ctx = container.contextualContent();
 * }</pre>
 */
@ProtobufMessage(name = "Message")
public final class LinkedMessageContainer implements MessageContainer {
    private static final LinkedMessageContainer EMPTY = new LinkedMessageContainerBuilder().build();

    /**
     * A plain text message with no context info or formatting.
     *
     * <p>This is the legacy plain-text field. Prefer
     * {@link #extendedTextMessage} for rich text with link previews,
     * mentions, and context metadata.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String conversation;

    /**
     * Group Signal encryption key distribution data.
     *
     * <p>This is a side-channel field that can coexist with the actual
     * message payload. It distributes sender keys to group participants.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    SenderKeyDistributionMessage senderKeyDistributionMessage;

    /**
     * An image message with optional caption, thumbnail, and media metadata.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ImageMessage imageMessage;

    /**
     * A single contact card (vCard) message.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    ContactMessage contactMessage;

    /**
     * A static location pin message.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    LocationMessage locationMessage;

    /**
     * A rich text message supporting link previews, mentions,
     * formatting, and context info.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    ExtendedTextMessage extendedTextMessage;

    /**
     * A document file message with optional caption.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    DocumentMessage documentMessage;

    /**
     * An audio message (voice note or audio file).
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    AudioMessage audioMessage;

    /**
     * A video message (or GIF, depending on the {@code gifPlayback} flag).
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    VideoMessage videoMessage;

    /**
     * A call offer/initiation message.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    CallOfferMessage call;

    /**
     * An internal chat protocol control message.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    ChatProtocolMessage chat;

    /**
     * A protocol-level message for revocations, ephemeral settings,
     * history sync notifications, app state key sharing, message edits,
     * and other system-level operations.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    ProtocolMessage protocolMessage;

    /**
     * A multi-contact card message containing multiple vCards.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    ContactsArrayMessage contactsArrayMessage;

    /**
     * A highly structured message used for business message templates
     * with localizable parameters.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    HighlyStructuredMessage highlyStructuredMessage;

    /**
     * Fast ratchet variant of the sender key distribution.
     *
     * <p>Like {@link #senderKeyDistributionMessage}, this is a
     * side-channel field that can coexist with the actual payload.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage;

    /**
     * A send-payment message.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    SendPaymentMessage sendPaymentMessage;

    /**
     * A live location sharing message.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    LiveLocationMessage liveLocationMessage;

    /**
     * A request-payment message.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    RequestPaymentMessage requestPaymentMessage;

    /**
     * A decline-payment-request message.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    DeclinePaymentRequestMessage declinePaymentRequestMessage;

    /**
     * A cancel-payment-request message.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    CancelPaymentRequestMessage cancelPaymentRequestMessage;

    /**
     * A business template message.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.MESSAGE)
    TemplateMessage templateMessage;

    /**
     * A sticker message.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    StickerMessage stickerMessage;

    /**
     * A group invite link message.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.MESSAGE)
    GroupInviteMessage groupInviteMessage;

    /**
     * A reply to a template button.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    TemplateButtonReplyMessage templateButtonReplyMessage;

    /**
     * A product catalog message.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    ProductMessage productMessage;

    /**
     * A message mirrored to linked companion devices.
     *
     * <p>When a user sends a message from one device, WhatsApp distributes
     * it to the user's other linked devices wrapped in this envelope. The
     * inner {@link DeviceSentMessage#message()} contains the original
     * message.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    DeviceSentMessage deviceSentMessage;

    /**
     * Per-message transport metadata including device list information,
     * encryption secrets, bot metadata, and limit-sharing data.
     *
     * <p>This is a side-channel field that coexists with the actual
     * message payload. It is not a message type and is excluded from
     * {@link #content()} resolution.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.MESSAGE)
    ChatMessageContextInfo messageContextInfo;

    /**
     * A selection list message.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.MESSAGE)
    ListMessage listMessage;

    /**
     * A view-once media message (V1), wrapped in a
     * {@link FutureProofMessage} envelope for forward compatibility.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.MESSAGE)
    FutureProofMessage viewOnceMessage;

    /**
     * A payment order message.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    OrderMessage orderMessage;

    /**
     * A response to a selection list message.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    ListResponseMessage listResponseMessage;

    /**
     * A disappearing (ephemeral) message, wrapped in a
     * {@link FutureProofMessage} envelope.
     *
     * <p>The inner container holds the actual message that will
     * auto-delete after the chat's ephemeral timer expires.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    FutureProofMessage ephemeralMessage;

    /**
     * A payment invoice message.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    InvoiceMessage invoiceMessage;

    /**
     * A buttons message (deprecated in favor of interactive messages).
     */
    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    ButtonsMessage buttonsMessage;

    /**
     * A response to a buttons message.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.MESSAGE)
    ButtonsResponseMessage buttonsResponseMessage;

    /**
     * A payment invite message.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    PaymentInviteMessage paymentInviteMessage;

    /**
     * An interactive message (native flows, CTAs, carousels).
     */
    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    InteractiveMessage interactiveMessage;

    /**
     * A reaction (emoji) to another message.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    ReactionMessage reactionMessage;

    /**
     * A sticker sync remove-my-receipt message.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.MESSAGE)
    StickerSyncRMRMessage stickerSyncRmrMessage;

    /**
     * A response to an interactive message.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.MESSAGE)
    InteractiveResponseMessage interactiveResponseMessage;

    /**
     * A poll creation message (V1).
     */
    @ProtobufProperty(index = 49, type = ProtobufType.MESSAGE)
    PollCreationMessage pollCreationMessage;

    /**
     * A poll vote/update message.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    PollUpdateMessage pollUpdateMessage;

    /**
     * A keep-in-chat (bookmark/save) action message.
     */
    @ProtobufProperty(index = 51, type = ProtobufType.MESSAGE)
    KeepInChatMessage keepInChatMessage;

    /**
     * A document message with a separate caption, wrapped in a
     * {@link FutureProofMessage} envelope for forward compatibility.
     */
    @ProtobufProperty(index = 53, type = ProtobufType.MESSAGE)
    FutureProofMessage documentWithCaptionMessage;

    /**
     * A request for the recipient's phone number.
     */
    @ProtobufProperty(index = 54, type = ProtobufType.MESSAGE)
    RequestPhoneNumberMessage requestPhoneNumberMessage;

    /**
     * A view-once media message (V2), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 55, type = ProtobufType.MESSAGE)
    FutureProofMessage viewOnceMessageV2;

    /**
     * An E2E-encrypted reaction targeting another message.
     *
     * <p>Carries {@code targetMessageKey}, {@code encPayload}, and
     * {@code encIv} so the server cannot read the reaction content.
     */
    @ProtobufProperty(index = 56, type = ProtobufType.MESSAGE)
    EncReactionMessage encReactionMessage;

    /**
     * An edited message, wrapped in a {@link FutureProofMessage}
     * envelope. The inner container holds the new message content.
     */
    @ProtobufProperty(index = 58, type = ProtobufType.MESSAGE)
    FutureProofMessage editedMessage;

    /**
     * A view-once media message (V2 extension), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 59, type = ProtobufType.MESSAGE)
    FutureProofMessage viewOnceMessageV2Extension;

    /**
     * A poll creation message (V2).
     */
    @ProtobufProperty(index = 60, type = ProtobufType.MESSAGE)
    PollCreationMessage pollCreationMessageV2;

    /**
     * A scheduled call creation message.
     */
    @ProtobufProperty(index = 61, type = ProtobufType.MESSAGE)
    ScheduledCallCreationMessage scheduledCallCreationMessage;

    /**
     * A group-mention message, wrapped in a {@link FutureProofMessage}
     * envelope. Has the highest unwrapping priority in WhatsApp Web.
     */
    @ProtobufProperty(index = 62, type = ProtobufType.MESSAGE)
    FutureProofMessage groupMentionedMessage;

    /**
     * A pin/unpin message action in a chat.
     */
    @ProtobufProperty(index = 63, type = ProtobufType.MESSAGE)
    PinInChatMessage pinInChatMessage;

    /**
     * A poll creation message (V3).
     */
    @ProtobufProperty(index = 64, type = ProtobufType.MESSAGE)
    PollCreationMessage pollCreationMessageV3;

    /**
     * An edit to a previously scheduled call.
     */
    @ProtobufProperty(index = 65, type = ProtobufType.MESSAGE)
    ScheduledCallEditMessage scheduledCallEditMessage;

    /**
     * A push-to-talk video (video note / circular video).
     *
     * <p>Reuses the {@link VideoMessage} type at a different protobuf
     * field index (66 vs 9) to distinguish circular video notes from
     * regular video messages.
     */
    @ProtobufProperty(index = 66, type = ProtobufType.MESSAGE)
    VideoMessage ptvMessage;

    /**
     * A bot invocation message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 67, type = ProtobufType.MESSAGE)
    FutureProofMessage botInvokeMessage;

    /**
     * A call log entry message.
     */
    @ProtobufProperty(index = 69, type = ProtobufType.MESSAGE)
    CallLogMessage callLogMesssage;

    /**
     * A history sync bundle containing encrypted message history data.
     */
    @ProtobufProperty(index = 70, type = ProtobufType.MESSAGE)
    MessageHistoryBundle messageHistoryBundle;

    /**
     * An E2E-encrypted comment targeting another message.
     */
    @ProtobufProperty(index = 71, type = ProtobufType.MESSAGE)
    EncCommentMessage encCommentMessage;

    /**
     * A business call (BCall) message.
     */
    @ProtobufProperty(index = 72, type = ProtobufType.MESSAGE)
    BCallMessage bcallMessage;

    /**
     * An animated Lottie sticker message, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 74, type = ProtobufType.MESSAGE)
    FutureProofMessage lottieStickerMessage;

    /**
     * A calendar event message.
     */
    @ProtobufProperty(index = 75, type = ProtobufType.MESSAGE)
    EventMessage eventMessage;

    /**
     * An E2E-encrypted event response (RSVP) message.
     */
    @ProtobufProperty(index = 76, type = ProtobufType.MESSAGE)
    EncEventResponseMessage encEventResponseMessage;

    /**
     * A comment on a message (channel/community context).
     */
    @ProtobufProperty(index = 77, type = ProtobufType.MESSAGE)
    CommentMessage commentMessage;

    /**
     * A newsletter (channel) admin invite message.
     */
    @ProtobufProperty(index = 78, type = ProtobufType.MESSAGE)
    NewsletterAdminInviteMessage newsletterAdminInviteMessage;

    /**
     * A placeholder for a message that cannot be displayed by the
     * current client version.
     */
    @ProtobufProperty(index = 80, type = ProtobufType.MESSAGE)
    PlaceholderMessage placeholderMessage;

    /**
     * A secret-encrypted message carrying an E2E-encrypted payload
     * targeting another message (used for event edits).
     */
    @ProtobufProperty(index = 82, type = ProtobufType.MESSAGE)
    SecretEncMessage secretEncryptedMessage;

    /**
     * An album message grouping multiple media messages together.
     */
    @ProtobufProperty(index = 83, type = ProtobufType.MESSAGE)
    AlbumMessage albumMessage;

    /**
     * An event cover image, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 85, type = ProtobufType.MESSAGE)
    FutureProofMessage eventCoverImage;

    /**
     * A sticker pack message.
     */
    @ProtobufProperty(index = 86, type = ProtobufType.MESSAGE)
    StickerPackMessage stickerPackMessage;

    /**
     * A status mention message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 87, type = ProtobufType.MESSAGE)
    FutureProofMessage statusMentionMessage;

    /**
     * A snapshot of poll results (V1).
     */
    @ProtobufProperty(index = 88, type = ProtobufType.MESSAGE)
    PollResultSnapshotMessage pollResultSnapshotMessage;

    /**
     * A poll creation option image, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 90, type = ProtobufType.MESSAGE)
    FutureProofMessage pollCreationOptionImageMessage;

    /**
     * An associated child message, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 91, type = ProtobufType.MESSAGE)
    FutureProofMessage associatedChildMessage;

    /**
     * A group status mention message, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 92, type = ProtobufType.MESSAGE)
    FutureProofMessage groupStatusMentionMessage;

    /**
     * A poll creation message (V4), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 93, type = ProtobufType.MESSAGE)
    FutureProofMessage pollCreationMessageV4;

    /**
     * A status "Add Yours" sticker/prompt, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 95, type = ProtobufType.MESSAGE)
    FutureProofMessage statusAddYours;

    /**
     * A group status message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 96, type = ProtobufType.MESSAGE)
    FutureProofMessage groupStatusMessage;

    /**
     * A rich response from the WhatsApp AI bot, composed of typed
     * sub-message fragments (text, code, tables, images, etc.).
     */
    @ProtobufProperty(index = 97, type = ProtobufType.MESSAGE)
    AIRichResponseMessage richResponseMessage;

    /**
     * A status notification message (e.g. reaction or interaction
     * notification on a status update).
     */
    @ProtobufProperty(index = 98, type = ProtobufType.MESSAGE)
    StatusNotificationMessage statusNotificationMessage;

    /**
     * A limit-sharing message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 99, type = ProtobufType.MESSAGE)
    FutureProofMessage limitSharingMessage;

    /**
     * A bot task message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 100, type = ProtobufType.MESSAGE)
    FutureProofMessage botTaskMessage;

    /**
     * A question message (AI/bot context), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 101, type = ProtobufType.MESSAGE)
    FutureProofMessage questionMessage;

    /**
     * A message history notice with sync metadata.
     */
    @ProtobufProperty(index = 102, type = ProtobufType.MESSAGE)
    MessageHistoryNotice messageHistoryNotice;

    /**
     * A group status message (V2), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 103, type = ProtobufType.MESSAGE)
    FutureProofMessage groupStatusMessageV2;

    /**
     * A bot-forwarded message, wrapped in a {@link FutureProofMessage}
     * envelope.
     */
    @ProtobufProperty(index = 104, type = ProtobufType.MESSAGE)
    FutureProofMessage botForwardedMessage;

    /**
     * A status question-and-answer message.
     */
    @ProtobufProperty(index = 105, type = ProtobufType.MESSAGE)
    StatusQuestionAnswerMessage statusQuestionAnswerMessage;

    /**
     * A question reply message (AI/bot context), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 106, type = ProtobufType.MESSAGE)
    FutureProofMessage questionReplyMessage;

    /**
     * A question response message (AI/bot context).
     */
    @ProtobufProperty(index = 107, type = ProtobufType.MESSAGE)
    QuestionResponseMessage questionResponseMessage;

    /**
     * A quoted status message.
     */
    @ProtobufProperty(index = 109, type = ProtobufType.MESSAGE)
    StatusQuotedMessage statusQuotedMessage;

    /**
     * A sticker interaction on a status update.
     */
    @ProtobufProperty(index = 110, type = ProtobufType.MESSAGE)
    StatusStickerInteractionMessage statusStickerInteractionMessage;

    /**
     * A poll creation message (V5).
     */
    @ProtobufProperty(index = 111, type = ProtobufType.MESSAGE)
    PollCreationMessage pollCreationMessageV5;

    /**
     * A newsletter (channel) follower invite message (V2).
     */
    @ProtobufProperty(index = 113, type = ProtobufType.MESSAGE)
    NewsletterFollowerInviteMessage newsletterFollowerInviteMessageV2;

    /**
     * A snapshot of poll results (V3).
     */
    @ProtobufProperty(index = 115, type = ProtobufType.MESSAGE)
    PollResultSnapshotMessage pollResultSnapshotMessageV3;

    /**
     * A newsletter admin profile message, wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 116, type = ProtobufType.MESSAGE)
    FutureProofMessage newsletterAdminProfileMessage;

    /**
     * A newsletter admin profile message (V2), wrapped in a
     * {@link FutureProofMessage} envelope.
     */
    @ProtobufProperty(index = 117, type = ProtobufType.MESSAGE)
    FutureProofMessage newsletterAdminProfileMessageV2;


    /**
     * Constructs a new {@code LinkedMessageContainer} with every payload and
     * side-channel field explicitly specified.
     *
     * <p>The constructor is package-private and is intended to be
     * invoked by {@code LinkedMessageContainerBuilder} or by protobuf
     * deserialisation. Client code should prefer the static factories
     * {@link #of(Message)}, {@link #of(String)}, or the
     * {@code of*} wrappers to build containers with the correct
     * version and envelope for the target feature.
     *
     * <p>At most one payload field is expected to be non-{@code null}:
     * populating more than one is valid on the wire but ambiguous and
     * should be avoided. Any combination of the three side-channel
     * fields may coexist with the payload.
     */
    LinkedMessageContainer(String conversation, SenderKeyDistributionMessage senderKeyDistributionMessage, ImageMessage imageMessage, ContactMessage contactMessage, LocationMessage locationMessage, ExtendedTextMessage extendedTextMessage, DocumentMessage documentMessage, AudioMessage audioMessage, VideoMessage videoMessage, CallOfferMessage call, ChatProtocolMessage chat, ProtocolMessage protocolMessage, ContactsArrayMessage contactsArrayMessage, HighlyStructuredMessage highlyStructuredMessage, SenderKeyDistributionMessage fastRatchetKeySenderKeyDistributionMessage, SendPaymentMessage sendPaymentMessage, LiveLocationMessage liveLocationMessage, RequestPaymentMessage requestPaymentMessage, DeclinePaymentRequestMessage declinePaymentRequestMessage, CancelPaymentRequestMessage cancelPaymentRequestMessage, TemplateMessage templateMessage, StickerMessage stickerMessage, GroupInviteMessage groupInviteMessage, TemplateButtonReplyMessage templateButtonReplyMessage, ProductMessage productMessage, DeviceSentMessage deviceSentMessage, ChatMessageContextInfo messageContextInfo, ListMessage listMessage, FutureProofMessage viewOnceMessage, OrderMessage orderMessage, ListResponseMessage listResponseMessage, FutureProofMessage ephemeralMessage, InvoiceMessage invoiceMessage, ButtonsMessage buttonsMessage, ButtonsResponseMessage buttonsResponseMessage, PaymentInviteMessage paymentInviteMessage, InteractiveMessage interactiveMessage, ReactionMessage reactionMessage, StickerSyncRMRMessage stickerSyncRmrMessage, InteractiveResponseMessage interactiveResponseMessage, PollCreationMessage pollCreationMessage, PollUpdateMessage pollUpdateMessage, KeepInChatMessage keepInChatMessage, FutureProofMessage documentWithCaptionMessage, RequestPhoneNumberMessage requestPhoneNumberMessage, FutureProofMessage viewOnceMessageV2, EncReactionMessage encReactionMessage, FutureProofMessage editedMessage, FutureProofMessage viewOnceMessageV2Extension, PollCreationMessage pollCreationMessageV2, ScheduledCallCreationMessage scheduledCallCreationMessage, FutureProofMessage groupMentionedMessage, PinInChatMessage pinInChatMessage, PollCreationMessage pollCreationMessageV3, ScheduledCallEditMessage scheduledCallEditMessage, VideoMessage ptvMessage, FutureProofMessage botInvokeMessage, CallLogMessage callLogMesssage, MessageHistoryBundle messageHistoryBundle, EncCommentMessage encCommentMessage, BCallMessage bcallMessage, FutureProofMessage lottieStickerMessage, EventMessage eventMessage, EncEventResponseMessage encEventResponseMessage, CommentMessage commentMessage, NewsletterAdminInviteMessage newsletterAdminInviteMessage, PlaceholderMessage placeholderMessage, SecretEncMessage secretEncryptedMessage, AlbumMessage albumMessage, FutureProofMessage eventCoverImage, StickerPackMessage stickerPackMessage, FutureProofMessage statusMentionMessage, PollResultSnapshotMessage pollResultSnapshotMessage, FutureProofMessage pollCreationOptionImageMessage, FutureProofMessage associatedChildMessage, FutureProofMessage groupStatusMentionMessage, FutureProofMessage pollCreationMessageV4, FutureProofMessage statusAddYours, FutureProofMessage groupStatusMessage, AIRichResponseMessage richResponseMessage, StatusNotificationMessage statusNotificationMessage, FutureProofMessage limitSharingMessage, FutureProofMessage botTaskMessage, FutureProofMessage questionMessage, MessageHistoryNotice messageHistoryNotice, FutureProofMessage groupStatusMessageV2, FutureProofMessage botForwardedMessage, StatusQuestionAnswerMessage statusQuestionAnswerMessage, FutureProofMessage questionReplyMessage, QuestionResponseMessage questionResponseMessage, StatusQuotedMessage statusQuotedMessage, StatusStickerInteractionMessage statusStickerInteractionMessage, PollCreationMessage pollCreationMessageV5, NewsletterFollowerInviteMessage newsletterFollowerInviteMessageV2, PollResultSnapshotMessage pollResultSnapshotMessageV3, FutureProofMessage newsletterAdminProfileMessage, FutureProofMessage newsletterAdminProfileMessageV2) {
        this.conversation = conversation;
        this.senderKeyDistributionMessage = senderKeyDistributionMessage;
        this.imageMessage = imageMessage;
        this.contactMessage = contactMessage;
        this.locationMessage = locationMessage;
        this.extendedTextMessage = extendedTextMessage;
        this.documentMessage = documentMessage;
        this.audioMessage = audioMessage;
        this.videoMessage = videoMessage;
        this.call = call;
        this.chat = chat;
        this.protocolMessage = protocolMessage;
        this.contactsArrayMessage = contactsArrayMessage;
        this.highlyStructuredMessage = highlyStructuredMessage;
        this.fastRatchetKeySenderKeyDistributionMessage = fastRatchetKeySenderKeyDistributionMessage;
        this.sendPaymentMessage = sendPaymentMessage;
        this.liveLocationMessage = liveLocationMessage;
        this.requestPaymentMessage = requestPaymentMessage;
        this.declinePaymentRequestMessage = declinePaymentRequestMessage;
        this.cancelPaymentRequestMessage = cancelPaymentRequestMessage;
        this.templateMessage = templateMessage;
        this.stickerMessage = stickerMessage;
        this.groupInviteMessage = groupInviteMessage;
        this.templateButtonReplyMessage = templateButtonReplyMessage;
        this.productMessage = productMessage;
        this.deviceSentMessage = deviceSentMessage;
        this.messageContextInfo = messageContextInfo;
        this.listMessage = listMessage;
        this.viewOnceMessage = viewOnceMessage;
        this.orderMessage = orderMessage;
        this.listResponseMessage = listResponseMessage;
        this.ephemeralMessage = ephemeralMessage;
        this.invoiceMessage = invoiceMessage;
        this.buttonsMessage = buttonsMessage;
        this.buttonsResponseMessage = buttonsResponseMessage;
        this.paymentInviteMessage = paymentInviteMessage;
        this.interactiveMessage = interactiveMessage;
        this.reactionMessage = reactionMessage;
        this.stickerSyncRmrMessage = stickerSyncRmrMessage;
        this.interactiveResponseMessage = interactiveResponseMessage;
        this.pollCreationMessage = pollCreationMessage;
        this.pollUpdateMessage = pollUpdateMessage;
        this.keepInChatMessage = keepInChatMessage;
        this.documentWithCaptionMessage = documentWithCaptionMessage;
        this.requestPhoneNumberMessage = requestPhoneNumberMessage;
        this.viewOnceMessageV2 = viewOnceMessageV2;
        this.encReactionMessage = encReactionMessage;
        this.editedMessage = editedMessage;
        this.viewOnceMessageV2Extension = viewOnceMessageV2Extension;
        this.pollCreationMessageV2 = pollCreationMessageV2;
        this.scheduledCallCreationMessage = scheduledCallCreationMessage;
        this.groupMentionedMessage = groupMentionedMessage;
        this.pinInChatMessage = pinInChatMessage;
        this.pollCreationMessageV3 = pollCreationMessageV3;
        this.scheduledCallEditMessage = scheduledCallEditMessage;
        this.ptvMessage = ptvMessage;
        this.botInvokeMessage = botInvokeMessage;
        this.callLogMesssage = callLogMesssage;
        this.messageHistoryBundle = messageHistoryBundle;
        this.encCommentMessage = encCommentMessage;
        this.bcallMessage = bcallMessage;
        this.lottieStickerMessage = lottieStickerMessage;
        this.eventMessage = eventMessage;
        this.encEventResponseMessage = encEventResponseMessage;
        this.commentMessage = commentMessage;
        this.newsletterAdminInviteMessage = newsletterAdminInviteMessage;
        this.placeholderMessage = placeholderMessage;
        this.secretEncryptedMessage = secretEncryptedMessage;
        this.albumMessage = albumMessage;
        this.eventCoverImage = eventCoverImage;
        this.stickerPackMessage = stickerPackMessage;
        this.statusMentionMessage = statusMentionMessage;
        this.pollResultSnapshotMessage = pollResultSnapshotMessage;
        this.pollCreationOptionImageMessage = pollCreationOptionImageMessage;
        this.associatedChildMessage = associatedChildMessage;
        this.groupStatusMentionMessage = groupStatusMentionMessage;
        this.pollCreationMessageV4 = pollCreationMessageV4;
        this.statusAddYours = statusAddYours;
        this.groupStatusMessage = groupStatusMessage;
        this.richResponseMessage = richResponseMessage;
        this.statusNotificationMessage = statusNotificationMessage;
        this.limitSharingMessage = limitSharingMessage;
        this.botTaskMessage = botTaskMessage;
        this.questionMessage = questionMessage;
        this.messageHistoryNotice = messageHistoryNotice;
        this.groupStatusMessageV2 = groupStatusMessageV2;
        this.botForwardedMessage = botForwardedMessage;
        this.statusQuestionAnswerMessage = statusQuestionAnswerMessage;
        this.questionReplyMessage = questionReplyMessage;
        this.questionResponseMessage = questionResponseMessage;
        this.statusQuotedMessage = statusQuotedMessage;
        this.statusStickerInteractionMessage = statusStickerInteractionMessage;
        this.pollCreationMessageV5 = pollCreationMessageV5;
        this.newsletterFollowerInviteMessageV2 = newsletterFollowerInviteMessageV2;
        this.pollResultSnapshotMessageV3 = pollResultSnapshotMessageV3;
        this.newsletterAdminProfileMessage = newsletterAdminProfileMessage;
        this.newsletterAdminProfileMessageV2 = newsletterAdminProfileMessageV2;
    }

    /**
     * Returns an empty message container with no fields populated.
     *
     * @return a non-null empty container
     */
    public static LinkedMessageContainer empty() {
        return EMPTY;
    }

    /**
     * Constructs a new container wrapping the given text as a plain
     * {@code conversation} message (protobuf field 1). For rich text with
     * context info, link previews, or formatting, construct an
     * {@link ExtendedTextMessage} and pass it to {@link #of(Message)}.
     *
     * @param text the plain text content
     * @return a non-null container
     */
    public static LinkedMessageContainer of(String text) {
        return new LinkedMessageContainerBuilder()
                .conversation(text)
                .build();
    }

    /**
     * Constructs a new container wrapping the given message in its
     * corresponding protobuf field. The correct field is selected via
     * pattern matching on the message's concrete type.
     *
     * <p>Message types that do not correspond to any top-level
     * {@code LinkedMessageContainer} field (such as sub-messages embedded in
     * {@link ProtocolMessage}) are silently ignored, producing an empty
     * container.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer of(T message) {
        var builder = new LinkedMessageContainerBuilder();
        switch (message) {
            case ImageMessage m -> builder.imageMessage(m);
            case ContactMessage m -> builder.contactMessage(m);
            case LocationMessage m -> builder.locationMessage(m);
            case ExtendedTextMessage m -> builder.extendedTextMessage(m);
            case DocumentMessage m -> builder.documentMessage(m);
            case AudioMessage m -> builder.audioMessage(m);
            case VideoMessage m -> builder.videoMessage(m);
            case StickerMessage m -> builder.stickerMessage(m);
            case StickerPackMessage m -> builder.stickerPackMessage(m);
            case CallOfferMessage m -> builder.call(m);
            case ChatProtocolMessage m -> builder.chat(m);
            case ProtocolMessage m -> builder.protocolMessage(m);
            case ContactsArrayMessage m -> builder.contactsArrayMessage(m);
            case HighlyStructuredMessage m -> builder.highlyStructuredMessage(m);
            case SenderKeyDistributionMessage m -> builder.senderKeyDistributionMessage(m);
            case SendPaymentMessage m -> builder.sendPaymentMessage(m);
            case LiveLocationMessage m -> builder.liveLocationMessage(m);
            case RequestPaymentMessage m -> builder.requestPaymentMessage(m);
            case DeclinePaymentRequestMessage m -> builder.declinePaymentRequestMessage(m);
            case CancelPaymentRequestMessage m -> builder.cancelPaymentRequestMessage(m);
            case TemplateMessage m -> builder.templateMessage(m);
            case GroupInviteMessage m -> builder.groupInviteMessage(m);
            case TemplateButtonReplyMessage m -> builder.templateButtonReplyMessage(m);
            case ProductMessage m -> builder.productMessage(m);
            case DeviceSentMessage m -> builder.deviceSentMessage(m);
            case ListMessage m -> builder.listMessage(m);
            case OrderMessage m -> builder.orderMessage(m);
            case ListResponseMessage m -> builder.listResponseMessage(m);
            case InvoiceMessage m -> builder.invoiceMessage(m);
            case ButtonsMessage m -> builder.buttonsMessage(m);
            case ButtonsResponseMessage m -> builder.buttonsResponseMessage(m);
            case PaymentInviteMessage m -> builder.paymentInviteMessage(m);
            case InteractiveMessage m -> builder.interactiveMessage(m);
            case ReactionMessage m -> builder.reactionMessage(m);
            case StickerSyncRMRMessage m -> builder.stickerSyncRmrMessage(m);
            case InteractiveResponseMessage m -> builder.interactiveResponseMessage(m);
            case PollCreationMessage m -> builder.pollCreationMessageV5(m);
            case PollUpdateMessage m -> builder.pollUpdateMessage(m);
            case KeepInChatMessage m -> builder.keepInChatMessage(m);
            case RequestPhoneNumberMessage m -> builder.requestPhoneNumberMessage(m);
            case EncReactionMessage m -> builder.encReactionMessage(m);
            case ScheduledCallCreationMessage m -> builder.scheduledCallCreationMessage(m);
            case PinInChatMessage m -> builder.pinInChatMessage(m);
            case ScheduledCallEditMessage m -> builder.scheduledCallEditMessage(m);
            case CallLogMessage m -> builder.callLogMesssage(m);
            case MessageHistoryBundle m -> builder.messageHistoryBundle(m);
            case EncCommentMessage m -> builder.encCommentMessage(m);
            case BCallMessage m -> builder.bcallMessage(m);
            case EventMessage m -> builder.eventMessage(m);
            case EncEventResponseMessage m -> builder.encEventResponseMessage(m);
            case CommentMessage m -> builder.commentMessage(m);
            case NewsletterAdminInviteMessage m -> builder.newsletterAdminInviteMessage(m);
            case NewsletterFollowerInviteMessage m -> builder.newsletterFollowerInviteMessageV2(m);
            case PlaceholderMessage m -> builder.placeholderMessage(m);
            case SecretEncMessage m -> builder.secretEncryptedMessage(m);
            case AlbumMessage m -> builder.albumMessage(m);
            case PollResultSnapshotMessage m -> builder.pollResultSnapshotMessageV3(m);
            case StatusNotificationMessage m -> builder.statusNotificationMessage(m);
            case MessageHistoryNotice m -> builder.messageHistoryNotice(m);
            case StatusQuestionAnswerMessage m -> builder.statusQuestionAnswerMessage(m);
            case QuestionResponseMessage m -> builder.questionResponseMessage(m);
            case StatusQuotedMessage m -> builder.statusQuotedMessage(m);
            case StatusStickerInteractionMessage m -> builder.statusStickerInteractionMessage(m);
            default -> {}
        }
        return builder.build();
    }

    /**
     * Wraps the given message inside an ephemeral (disappearing)
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to make ephemeral
     * @param <T>     the compile-time message type
     * @return a non-null ephemeral container
     */
    public static <T extends Message> LinkedMessageContainer ofEphemeral(T message) {
        return new LinkedMessageContainerBuilder()
                .ephemeralMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a view-once
     * {@link FutureProofMessage} envelope.
     *
     * <p>Internally uses the newest view-once format.
     *
     * @param message the message to make view-once
     * @param <T>     the compile-time message type
     * @return a non-null view-once container
     */
    public static <T extends Message> LinkedMessageContainer ofViewOnce(T message) {
        return new LinkedMessageContainerBuilder()
                .viewOnceMessageV2Extension(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside an edited
     * {@link FutureProofMessage} envelope.
     *
     * @param message the edited message content
     * @param <T>     the compile-time message type
     * @return a non-null edited container
     */
    public static <T extends Message> LinkedMessageContainer ofEdited(T message) {
        return new LinkedMessageContainerBuilder()
                .editedMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a document-with-caption
     * {@link FutureProofMessage} envelope.
     *
     * @param message the document message with caption
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofDocumentWithCaption(T message) {
        return new LinkedMessageContainerBuilder()
                .documentWithCaptionMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a group-mentioned
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofGroupMentioned(T message) {
        return new LinkedMessageContainerBuilder()
                .groupMentionedMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a bot-invoke
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofBotInvoke(T message) {
        return new LinkedMessageContainerBuilder()
                .botInvokeMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a Lottie sticker
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofLottieSticker(T message) {
        return new LinkedMessageContainerBuilder()
                .lottieStickerMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside an event cover image
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofEventCoverImage(T message) {
        return new LinkedMessageContainerBuilder()
                .eventCoverImage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a status mention
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofStatusMention(T message) {
        return new LinkedMessageContainerBuilder()
                .statusMentionMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a poll creation option image
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofPollCreationOptionImage(T message) {
        return new LinkedMessageContainerBuilder()
                .pollCreationOptionImageMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside an associated child
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofAssociatedChild(T message) {
        return new LinkedMessageContainerBuilder()
                .associatedChildMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a group status mention
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofGroupStatusMention(T message) {
        return new LinkedMessageContainerBuilder()
                .groupStatusMentionMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a status "Add Yours"
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofStatusAddYours(T message) {
        return new LinkedMessageContainerBuilder()
                .statusAddYours(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a group status
     * {@link FutureProofMessage} envelope.
     *
     * <p>Internally uses the newest group status format.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null group status container
     */
    public static <T extends Message> LinkedMessageContainer ofGroupStatus(T message) {
        return new LinkedMessageContainerBuilder()
                .groupStatusMessageV2(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a limit-sharing
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofLimitSharing(T message) {
        return new LinkedMessageContainerBuilder()
                .limitSharingMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a bot task
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofBotTask(T message) {
        return new LinkedMessageContainerBuilder()
                .botTaskMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a question
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofQuestion(T message) {
        return new LinkedMessageContainerBuilder()
                .questionMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a bot-forwarded
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofBotForwarded(T message) {
        return new LinkedMessageContainerBuilder()
                .botForwardedMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a question reply
     * {@link FutureProofMessage} envelope.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null container
     */
    public static <T extends Message> LinkedMessageContainer ofQuestionReply(T message) {
        return new LinkedMessageContainerBuilder()
                .questionReplyMessage(wrapFutureProof(message))
                .build();
    }

    /**
     * Wraps the given message inside a newsletter admin profile
     * {@link FutureProofMessage} envelope.
     *
     * <p>Internally uses the newest newsletter admin profile format.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-null newsletter admin profile container
     */
    public static <T extends Message> LinkedMessageContainer ofNewsletterAdminProfile(T message) {
        return new LinkedMessageContainerBuilder()
                .newsletterAdminProfileMessageV2(wrapFutureProof(message))
                .build();
    }

    /**
     * Builds a {@link FutureProofMessage} envelope that contains a
     * single-payload {@link LinkedMessageContainer} wrapping the given
     * message.
     *
     * @param message the message to wrap
     * @param <T>     the compile-time message type
     * @return a non-{@code null} envelope containing the message
     */
    private static <T extends Message> FutureProofMessage wrapFutureProof(T message) {
        return new FutureProofMessageBuilder()
                .messageContainer(LinkedMessageContainer.of(message))
                .build();
    }

    /**
     * Returns the first populated message inside this container,
     * recursively unwrapping all {@link FutureProofMessage} and
     * {@link DeviceSentMessage} envelopes.
     *
     * <p>Side-channel fields ({@link #messageContextInfo()},
     * {@link #senderKeyDistributionMessage()}, and
     * {@link #fastRatchetKeySenderKeyDistributionMessage()}) are not
     * considered payload and are skipped.
     *
     * <p>If no payload field is populated, returns an empty
     * {@code Message}.
     *
     * @return a {@code Message} describing the innermost {@link Message}
     */
    public Message content() {
        // FutureProofMessage wrappers, in WhatsApp priority order first
        if (groupMentionedMessage != null) return unboxFutureProof(groupMentionedMessage).content();
        if (documentWithCaptionMessage != null) return unboxFutureProof(documentWithCaptionMessage).content();
        if (viewOnceMessage != null) return unboxFutureProof(viewOnceMessage).content();
        if (viewOnceMessageV2 != null) return unboxFutureProof(viewOnceMessageV2).content();
        if (viewOnceMessageV2Extension != null) return unboxFutureProof(viewOnceMessageV2Extension).content();
        if (ephemeralMessage != null) return unboxFutureProof(ephemeralMessage).content();
        if (editedMessage != null) return unboxFutureProof(editedMessage).content();
        if (botInvokeMessage != null) return unboxFutureProof(botInvokeMessage).content();
        // Remaining FutureProofMessage wrappers in field index order
        if (lottieStickerMessage != null) return unboxFutureProof(lottieStickerMessage).content();
        if (eventCoverImage != null) return unboxFutureProof(eventCoverImage).content();
        if (statusMentionMessage != null) return unboxFutureProof(statusMentionMessage).content();
        if (pollCreationOptionImageMessage != null) return unboxFutureProof(pollCreationOptionImageMessage).content();
        if (associatedChildMessage != null) return unboxFutureProof(associatedChildMessage).content();
        if (groupStatusMentionMessage != null) return unboxFutureProof(groupStatusMentionMessage).content();
        if (pollCreationMessageV4 != null) return unboxFutureProof(pollCreationMessageV4).content();
        if (statusAddYours != null) return unboxFutureProof(statusAddYours).content();
        if (groupStatusMessage != null) return unboxFutureProof(groupStatusMessage).content();
        if (limitSharingMessage != null) return unboxFutureProof(limitSharingMessage).content();
        if (botTaskMessage != null) return unboxFutureProof(botTaskMessage).content();
        if (questionMessage != null) return unboxFutureProof(questionMessage).content();
        if (groupStatusMessageV2 != null) return unboxFutureProof(groupStatusMessageV2).content();
        if (botForwardedMessage != null) return unboxFutureProof(botForwardedMessage).content();
        if (questionReplyMessage != null) return unboxFutureProof(questionReplyMessage).content();
        if (newsletterAdminProfileMessage != null) return unboxFutureProof(newsletterAdminProfileMessage).content();
        if (newsletterAdminProfileMessageV2 != null) return unboxFutureProof(newsletterAdminProfileMessageV2).content();
        // DeviceSentMessage wrapping
        if (deviceSentMessage != null && deviceSentMessage.message().isPresent()) return deviceSentMessage.message().get().content();
        // Direct message fields in protobuf index order
        if (conversation != null) return new ExtendedTextMessageBuilder().text(conversation).build();
        if (imageMessage != null) return imageMessage;
        if (contactMessage != null) return contactMessage;
        if (locationMessage != null) return locationMessage;
        if (extendedTextMessage != null) return extendedTextMessage;
        if (documentMessage != null) return documentMessage;
        if (audioMessage != null) return audioMessage;
        if (videoMessage != null) return videoMessage;
        if (call != null) return call;
        if (chat != null) return chat;
        if (protocolMessage != null) return protocolMessage;
        if (contactsArrayMessage != null) return contactsArrayMessage;
        if (highlyStructuredMessage != null) return highlyStructuredMessage;
        if (sendPaymentMessage != null) return sendPaymentMessage;
        if (liveLocationMessage != null) return liveLocationMessage;
        if (requestPaymentMessage != null) return requestPaymentMessage;
        if (declinePaymentRequestMessage != null) return declinePaymentRequestMessage;
        if (cancelPaymentRequestMessage != null) return cancelPaymentRequestMessage;
        if (templateMessage != null) return templateMessage;
        if (stickerMessage != null) return stickerMessage;
        if (groupInviteMessage != null) return groupInviteMessage;
        if (templateButtonReplyMessage != null) return templateButtonReplyMessage;
        if (productMessage != null) return productMessage;
        if (listMessage != null) return listMessage;
        if (orderMessage != null) return orderMessage;
        if (listResponseMessage != null) return listResponseMessage;
        if (invoiceMessage != null) return invoiceMessage;
        if (buttonsMessage != null) return buttonsMessage;
        if (buttonsResponseMessage != null) return buttonsResponseMessage;
        if (paymentInviteMessage != null) return paymentInviteMessage;
        if (interactiveMessage != null) return interactiveMessage;
        if (reactionMessage != null) return reactionMessage;
        if (stickerSyncRmrMessage != null) return stickerSyncRmrMessage;
        if (interactiveResponseMessage != null) return interactiveResponseMessage;
        if (pollCreationMessage != null) return pollCreationMessage;
        if (pollUpdateMessage != null) return pollUpdateMessage;
        if (keepInChatMessage != null) return keepInChatMessage;
        if (requestPhoneNumberMessage != null) return requestPhoneNumberMessage;
        if (encReactionMessage != null) return encReactionMessage;
        if (pollCreationMessageV2 != null) return pollCreationMessageV2;
        if (scheduledCallCreationMessage != null) return scheduledCallCreationMessage;
        if (pinInChatMessage != null) return pinInChatMessage;
        if (pollCreationMessageV3 != null) return pollCreationMessageV3;
        if (scheduledCallEditMessage != null) return scheduledCallEditMessage;
        if (ptvMessage != null) return ptvMessage;
        if (callLogMesssage != null) return callLogMesssage;
        if (messageHistoryBundle != null) return messageHistoryBundle;
        if (encCommentMessage != null) return encCommentMessage;
        if (bcallMessage != null) return bcallMessage;
        if (eventMessage != null) return eventMessage;
        if (encEventResponseMessage != null) return encEventResponseMessage;
        if (commentMessage != null) return commentMessage;
        if (newsletterAdminInviteMessage != null) return newsletterAdminInviteMessage;
        if (placeholderMessage != null) return placeholderMessage;
        if (secretEncryptedMessage != null) return secretEncryptedMessage;
        if (albumMessage != null) return albumMessage;
        if (stickerPackMessage != null) return stickerPackMessage;
        if (pollResultSnapshotMessage != null) return pollResultSnapshotMessage;
        if (richResponseMessage != null) return richResponseMessage;
        if (statusNotificationMessage != null) return statusNotificationMessage;
        if (messageHistoryNotice != null) return messageHistoryNotice;
        if (statusQuestionAnswerMessage != null) return statusQuestionAnswerMessage;
        if (questionResponseMessage != null) return questionResponseMessage;
        if (statusQuotedMessage != null) return statusQuotedMessage;
        if (statusStickerInteractionMessage != null) return statusStickerInteractionMessage;
        if (pollCreationMessageV5 != null) return pollCreationMessageV5;
        if (newsletterFollowerInviteMessageV2 != null) return newsletterFollowerInviteMessageV2;
        if (pollResultSnapshotMessageV3 != null) return pollResultSnapshotMessageV3;
        return EmptyMessage.INSTANCE;
    }

    /**
     * Returns the first populated {@link ContextualMessage} inside this
     * container, unwrapping all envelopes. If the innermost message does
     * not implement {@code ContextualMessage}, returns an empty
     * {@code Optional}.
     *
     * @return an {@code Optional} describing the {@link ContextualMessage}
     */
    public Optional<ContextualMessage> contextualContent() {
        return content() instanceof ContextualMessage contextualMessage
                ? Optional.of(contextualMessage)
                : Optional.empty();
    }

    /**
     * Returns the {@link FutureProofMessageType} identifying which wrapper
     * field is populated in this container, or {@link FutureProofMessageType#NONE}
     * if no wrapper is present.
     *
     * @return the wrapper type, never {@code null}
     */
    public FutureProofMessageType futureProofContentType() {
        if (groupMentionedMessage != null) return FutureProofMessageType.GROUP_MENTIONED;
        if (documentWithCaptionMessage != null) return FutureProofMessageType.DOCUMENT_WITH_CAPTION;
        if (viewOnceMessage != null || viewOnceMessageV2 != null || viewOnceMessageV2Extension != null) return FutureProofMessageType.VIEW_ONCE;
        if (ephemeralMessage != null) return FutureProofMessageType.EPHEMERAL;
        if (editedMessage != null) return FutureProofMessageType.EDITED;
        if (botInvokeMessage != null) return FutureProofMessageType.BOT_INVOKE;
        if (lottieStickerMessage != null) return FutureProofMessageType.LOTTIE_STICKER;
        if (eventCoverImage != null) return FutureProofMessageType.EVENT_COVER_IMAGE;
        if (statusMentionMessage != null) return FutureProofMessageType.STATUS_MENTION;
        if (pollCreationOptionImageMessage != null) return FutureProofMessageType.POLL_CREATION_OPTION_IMAGE;
        if (associatedChildMessage != null) return FutureProofMessageType.ASSOCIATED_CHILD;
        if (groupStatusMentionMessage != null) return FutureProofMessageType.GROUP_STATUS_MENTION;
        if (pollCreationMessageV4 != null) return FutureProofMessageType.POLL_CREATION;
        if (statusAddYours != null) return FutureProofMessageType.STATUS_ADD_YOURS;
        if (groupStatusMessage != null || groupStatusMessageV2 != null) return FutureProofMessageType.GROUP_STATUS;
        if (limitSharingMessage != null) return FutureProofMessageType.LIMIT_SHARING;
        if (botTaskMessage != null) return FutureProofMessageType.BOT_TASK;
        if (questionMessage != null) return FutureProofMessageType.QUESTION;
        if (botForwardedMessage != null) return FutureProofMessageType.BOT_FORWARDED;
        if (questionReplyMessage != null) return FutureProofMessageType.QUESTION_REPLY;
        if (newsletterAdminProfileMessage != null || newsletterAdminProfileMessageV2 != null) return FutureProofMessageType.NEWSLETTER_ADMIN_PROFILE;
        if (deviceSentMessage != null) return FutureProofMessageType.DEVICE_SENT;
        return FutureProofMessageType.NONE;
    }

    /**
     * Extracts the nested {@link LinkedMessageContainer} carried inside a
     * {@link FutureProofMessage} envelope, returning
     * {@link #empty()} when the envelope carries no container.
     *
     * @param wrapper the envelope to unwrap
     * @return the inner container, or {@link #empty()} if the
     *         envelope is empty
     */
    private static LinkedMessageContainer unboxFutureProof(FutureProofMessage wrapper) {
        return wrapper.message().orElseGet(LinkedMessageContainer::empty);
    }

    /**
     * Returns {@code true} if this container holds no payload message.
     *
     * <p>Side-channel metadata ({@link #messageContextInfo()},
     * {@link #senderKeyDistributionMessage()},
     * {@link #fastRatchetKeySenderKeyDistributionMessage()}) alone does
     * not count as content.
     *
     * @return {@code true} if this container is empty
     */
    public boolean isEmpty() {
        return content() == EmptyMessage.INSTANCE;
    }

    /**
     * Returns a human-readable string representation of this container,
     * delegating to the innermost message's {@code toString}.
     *
     * @return a non-null string
     */
    @Override
    public String toString() {
        var message = content();
        if(message == EmptyMessage.INSTANCE) {
            return "[Empty]";
        }

        return message.toString();
    }

    /**
     * Returns the per-message transport metadata, if present.
     *
     * <p>This is a side-channel field that coexists with the message
     * payload. It carries device list information, encryption secrets,
     * bot metadata, and limit-sharing data.
     *
     * @return an {@code Optional} describing the {@link ChatMessageContextInfo}
     */
    public Optional<ChatMessageContextInfo> messageContextInfo() {
        return Optional.ofNullable(messageContextInfo);
    }

    /**
     * Returns the sender key distribution message, if present.
     *
     * <p>This is a side-channel field that coexists with the message
     * payload. It distributes group Signal encryption sender keys to
     * group participants.
     *
     * @return an {@code Optional} describing the {@link SenderKeyDistributionMessage}
     */
    public Optional<SenderKeyDistributionMessage> senderKeyDistributionMessage() {
        return Optional.ofNullable(senderKeyDistributionMessage);
    }

    /**
     * Returns the raw {@link DeviceSentMessage} envelope at this level, if
     * present, without recursing into its wrapped inner container.
     *
     * <p>Companion accessor to the {@link FutureProofMessageType#DEVICE_SENT}
     * value returned by {@link #futureProofContentType()}: the enum identifies
     * the wrapper type and this accessor exposes the wrapper instance, so
     * callers that need the envelope's {@link DeviceSentMessage#destinationJid()}
     * or its raw inner container (for the receiver's outer-vs-inner
     * {@link ChatMessageContextInfo} merge) can read them without going
     * through {@link #content()}, which transparently unwraps the envelope
     * and returns the inner payload.
     *
     * @return an {@code Optional} describing the {@link DeviceSentMessage}
     */
    public Optional<DeviceSentMessage> deviceSentMessage() {
        return Optional.ofNullable(deviceSentMessage);
    }

    /**
     * Returns the fast ratchet key sender key distribution message, if present.
     *
     * <p>This is a side-channel field that coexists with the message
     * payload. It distributes fast ratchet sender keys for group
     * encryption.
     *
     * @return an {@code Optional} describing the {@link SenderKeyDistributionMessage}
     */
    public Optional<SenderKeyDistributionMessage> fastRatchetKeySenderKeyDistributionMessage() {
        return Optional.ofNullable(fastRatchetKeySenderKeyDistributionMessage);
    }

    /**
     * Returns a new container with the given per-message transport
     * metadata, preserving the current message and all other sidecar
     * fields.
     *
     * @param value the message context info to attach, or {@code null}
     *              to clear
     * @return a new container with the updated value
     */
    public LinkedMessageContainer withMessageContextInfo(ChatMessageContextInfo value) {
        return replaceSideChannel(value, senderKeyDistributionMessage, fastRatchetKeySenderKeyDistributionMessage);
    }

    /**
     * Returns a new container with the given sender key distribution
     * message, preserving the current message and all other sidecar
     * fields.
     *
     * @param value the sender key distribution message to attach, or
     *              {@code null} to clear
     * @return a new container with the updated value
     */
    public LinkedMessageContainer withSenderKeyDistributionMessage(SenderKeyDistributionMessage value) {
        return replaceSideChannel(messageContextInfo, value, fastRatchetKeySenderKeyDistributionMessage);
    }

    /**
     * Returns a new container with the given fast ratchet key sender key
     * distribution message, preserving the current message and all other
     * sidecar fields.
     *
     * @param value the fast ratchet key sender key distribution message
     *              to attach, or {@code null} to clear
     * @return a new container with the updated value
     */
    public LinkedMessageContainer withFastRatchetKeySenderKeyDistributionMessage(SenderKeyDistributionMessage value) {
        return replaceSideChannel(messageContextInfo, senderKeyDistributionMessage, value);
    }

    /**
     * Converts this container into a view-once message.
     * If already view-once (any version), returns {@code this}.
     *
     * <p>Internally uses the newest view-once format.
     *
     * @return a non-null view-once container
     */
    public LinkedMessageContainer toViewOnce() {
        if (viewOnceMessage != null || viewOnceMessageV2 != null || viewOnceMessageV2Extension != null) return this;
        return replacePayload(b -> b.viewOnceMessageV2Extension(wrapInner()));
    }

    /**
     * Converts this container into an ephemeral (disappearing) message.
     * If already ephemeral, returns {@code this}.
     *
     * @return a non-null ephemeral container
     */
    public LinkedMessageContainer toEphemeral() {
        if (ephemeralMessage != null) return this;
        return replacePayload(b -> b.ephemeralMessage(wrapInner()));
    }

    /**
     * Converts this container into a document-with-caption message.
     * If already document-with-caption, returns {@code this}.
     *
     * @return a non-null document-with-caption container
     */
    public LinkedMessageContainer toDocumentWithCaption() {
        if (documentWithCaptionMessage != null) return this;
        return replacePayload(b -> b.documentWithCaptionMessage(wrapInner()));
    }

    /**
     * Converts this container into an edited message.
     * If already an edit, returns {@code this}.
     *
     * @return a non-null edited container
     */
    public LinkedMessageContainer toEdited() {
        if (editedMessage != null) return this;
        return replacePayload(b -> b.editedMessage(wrapInner()));
    }

    /**
     * Converts this container into a group-mentioned message.
     * If already group-mentioned, returns {@code this}.
     *
     * @return a non-null group-mentioned container
     */
    public LinkedMessageContainer toGroupMentioned() {
        if (groupMentionedMessage != null) return this;
        return replacePayload(b -> b.groupMentionedMessage(wrapInner()));
    }

    /**
     * Converts this container into a bot-invoke message.
     * If already bot-invoke, returns {@code this}.
     *
     * @return a non-null bot-invoke container
     */
    public LinkedMessageContainer toBotInvoke() {
        if (botInvokeMessage != null) return this;
        return replacePayload(b -> b.botInvokeMessage(wrapInner()));
    }

    /**
     * Converts this container into a Lottie sticker message.
     * If already a Lottie sticker, returns {@code this}.
     *
     * @return a non-null Lottie sticker container
     */
    public LinkedMessageContainer toLottieSticker() {
        if (lottieStickerMessage != null) return this;
        return replacePayload(b -> b.lottieStickerMessage(wrapInner()));
    }

    /**
     * Converts this container into an event cover image message.
     * If already an event cover image, returns {@code this}.
     *
     * @return a non-null event cover image container
     */
    public LinkedMessageContainer toEventCoverImage() {
        if (eventCoverImage != null) return this;
        return replacePayload(b -> b.eventCoverImage(wrapInner()));
    }

    /**
     * Converts this container into a status mention message.
     * If already a status mention, returns {@code this}.
     *
     * @return a non-null status mention container
     */
    public LinkedMessageContainer toStatusMention() {
        if (statusMentionMessage != null) return this;
        return replacePayload(b -> b.statusMentionMessage(wrapInner()));
    }

    /**
     * Converts this container into a poll creation option image message.
     * If already a poll creation option image, returns {@code this}.
     *
     * @return a non-null poll creation option image container
     */
    public LinkedMessageContainer toPollCreationOptionImage() {
        if (pollCreationOptionImageMessage != null) return this;
        return replacePayload(b -> b.pollCreationOptionImageMessage(wrapInner()));
    }

    /**
     * Converts this container into an associated child message.
     * If already an associated child, returns {@code this}.
     *
     * @return a non-null associated child container
     */
    public LinkedMessageContainer toAssociatedChild() {
        if (associatedChildMessage != null) return this;
        return replacePayload(b -> b.associatedChildMessage(wrapInner()));
    }

    /**
     * Converts this container into a group status mention message.
     * If already a group status mention, returns {@code this}.
     *
     * @return a non-null group status mention container
     */
    public LinkedMessageContainer toGroupStatusMention() {
        if (groupStatusMentionMessage != null) return this;
        return replacePayload(b -> b.groupStatusMentionMessage(wrapInner()));
    }

    /**
     * Converts this container into a status "Add Yours" message.
     * If already a status add-yours, returns {@code this}.
     *
     * @return a non-null status add-yours container
     */
    public LinkedMessageContainer toStatusAddYours() {
        if (statusAddYours != null) return this;
        return replacePayload(b -> b.statusAddYours(wrapInner()));
    }

    /**
     * Converts this container into a group status message.
     * If already a group status (any version), returns {@code this}.
     *
     * <p>Internally uses the newest group status format.
     *
     * @return a non-null group status container
     */
    public LinkedMessageContainer toGroupStatus() {
        if (groupStatusMessage != null || groupStatusMessageV2 != null) return this;
        return replacePayload(b -> b.groupStatusMessageV2(wrapInner()));
    }

    /**
     * Converts this container into a limit-sharing message.
     * If already a limit-sharing, returns {@code this}.
     *
     * @return a non-null limit-sharing container
     */
    public LinkedMessageContainer toLimitSharing() {
        if (limitSharingMessage != null) return this;
        return replacePayload(b -> b.limitSharingMessage(wrapInner()));
    }

    /**
     * Converts this container into a bot task message.
     * If already a bot task, returns {@code this}.
     *
     * @return a non-null bot task container
     */
    public LinkedMessageContainer toBotTask() {
        if (botTaskMessage != null) return this;
        return replacePayload(b -> b.botTaskMessage(wrapInner()));
    }

    /**
     * Converts this container into a question message.
     * If already a question, returns {@code this}.
     *
     * @return a non-null question container
     */
    public LinkedMessageContainer toQuestion() {
        if (questionMessage != null) return this;
        return replacePayload(b -> b.questionMessage(wrapInner()));
    }

    /**
     * Converts this container into a bot-forwarded message.
     * If already bot-forwarded, returns {@code this}.
     *
     * @return a non-null bot-forwarded container
     */
    public LinkedMessageContainer toBotForwarded() {
        if (botForwardedMessage != null) return this;
        return replacePayload(b -> b.botForwardedMessage(wrapInner()));
    }

    /**
     * Converts this container into a question reply message.
     * If already a question reply, returns {@code this}.
     *
     * @return a non-null question reply container
     */
    public LinkedMessageContainer toQuestionReply() {
        if (questionReplyMessage != null) return this;
        return replacePayload(b -> b.questionReplyMessage(wrapInner()));
    }

    /**
     * Converts this container into a newsletter admin profile message.
     * If already a newsletter admin profile (any version), returns
     * {@code this}.
     *
     * <p>Internally uses the newest newsletter admin profile format.
     *
     * @return a non-null newsletter admin profile container
     */
    public LinkedMessageContainer toNewsletterAdminProfile() {
        if (newsletterAdminProfileMessage != null || newsletterAdminProfileMessageV2 != null) return this;
        return replacePayload(b -> b.newsletterAdminProfileMessageV2(wrapInner()));
    }

    /**
     * Builds a {@link FutureProofMessage} envelope whose inner
     * container carries the payload currently held by this container.
     *
     * @return a non-{@code null} envelope
     */
    private FutureProofMessage wrapInner() {
        return new FutureProofMessageBuilder()
                .messageContainer(LinkedMessageContainer.of(content()))
                .build();
    }

    /**
     * Returns a new container whose payload is set by {@code payloadSetter}
     * and whose three side-channel fields are copied from this container.
     *
     * @param payloadSetter the callback that populates the new payload
     *                      field on the builder
     * @return the new container with the replaced payload
     */
    private LinkedMessageContainer replacePayload(Consumer<LinkedMessageContainerBuilder> payloadSetter) {
        var builder = new LinkedMessageContainerBuilder();
        payloadSetter.accept(builder);
        return builder
                .messageContextInfo(messageContextInfo)
                .senderKeyDistributionMessage(senderKeyDistributionMessage)
                .fastRatchetKeySenderKeyDistributionMessage(fastRatchetKeySenderKeyDistributionMessage)
                .build();
    }

    /**
     * Returns a copy of this container with the payload preserved and the
     * three side-channel fields replaced by the supplied values.
     *
     * @param contextInfo    the new {@code messageContextInfo}
     * @param senderKey      the new {@code senderKeyDistributionMessage}
     * @param fastRatchetKey the new {@code fastRatchetKeySenderKeyDistributionMessage}
     * @return the copy with replaced side-channel fields
     */
    private LinkedMessageContainer replaceSideChannel(
            ChatMessageContextInfo contextInfo,
            SenderKeyDistributionMessage senderKey,
            SenderKeyDistributionMessage fastRatchetKey
    ) {
        var builder = new LinkedMessageContainerBuilder();
        copyPayloadInto(builder);
        return builder
                .messageContextInfo(contextInfo)
                .senderKeyDistributionMessage(senderKey)
                .fastRatchetKeySenderKeyDistributionMessage(fastRatchetKey)
                .build();
    }

    /**
     * Copies every payload field of this container onto the supplied
     * builder, leaving the three side-channel fields untouched.
     *
     * @param builder the target builder
     */
    @SuppressWarnings("unused")
    private void copyPayloadInto(LinkedMessageContainerBuilder builder) {
        if (conversation != null) builder.conversation(conversation);
        if (imageMessage != null) builder.imageMessage(imageMessage);
        if (contactMessage != null) builder.contactMessage(contactMessage);
        if (locationMessage != null) builder.locationMessage(locationMessage);
        if (extendedTextMessage != null) builder.extendedTextMessage(extendedTextMessage);
        if (documentMessage != null) builder.documentMessage(documentMessage);
        if (audioMessage != null) builder.audioMessage(audioMessage);
        if (videoMessage != null) builder.videoMessage(videoMessage);
        if (call != null) builder.call(call);
        if (chat != null) builder.chat(chat);
        if (protocolMessage != null) builder.protocolMessage(protocolMessage);
        if (contactsArrayMessage != null) builder.contactsArrayMessage(contactsArrayMessage);
        if (highlyStructuredMessage != null) builder.highlyStructuredMessage(highlyStructuredMessage);
        if (sendPaymentMessage != null) builder.sendPaymentMessage(sendPaymentMessage);
        if (liveLocationMessage != null) builder.liveLocationMessage(liveLocationMessage);
        if (requestPaymentMessage != null) builder.requestPaymentMessage(requestPaymentMessage);
        if (declinePaymentRequestMessage != null) builder.declinePaymentRequestMessage(declinePaymentRequestMessage);
        if (cancelPaymentRequestMessage != null) builder.cancelPaymentRequestMessage(cancelPaymentRequestMessage);
        if (templateMessage != null) builder.templateMessage(templateMessage);
        if (stickerMessage != null) builder.stickerMessage(stickerMessage);
        if (groupInviteMessage != null) builder.groupInviteMessage(groupInviteMessage);
        if (templateButtonReplyMessage != null) builder.templateButtonReplyMessage(templateButtonReplyMessage);
        if (productMessage != null) builder.productMessage(productMessage);
        if (deviceSentMessage != null) builder.deviceSentMessage(deviceSentMessage);
        if (listMessage != null) builder.listMessage(listMessage);
        if (viewOnceMessage != null) builder.viewOnceMessage(viewOnceMessage);
        if (orderMessage != null) builder.orderMessage(orderMessage);
        if (listResponseMessage != null) builder.listResponseMessage(listResponseMessage);
        if (ephemeralMessage != null) builder.ephemeralMessage(ephemeralMessage);
        if (invoiceMessage != null) builder.invoiceMessage(invoiceMessage);
        if (buttonsMessage != null) builder.buttonsMessage(buttonsMessage);
        if (buttonsResponseMessage != null) builder.buttonsResponseMessage(buttonsResponseMessage);
        if (paymentInviteMessage != null) builder.paymentInviteMessage(paymentInviteMessage);
        if (interactiveMessage != null) builder.interactiveMessage(interactiveMessage);
        if (reactionMessage != null) builder.reactionMessage(reactionMessage);
        if (stickerSyncRmrMessage != null) builder.stickerSyncRmrMessage(stickerSyncRmrMessage);
        if (interactiveResponseMessage != null) builder.interactiveResponseMessage(interactiveResponseMessage);
        if (pollCreationMessage != null) builder.pollCreationMessage(pollCreationMessage);
        if (pollUpdateMessage != null) builder.pollUpdateMessage(pollUpdateMessage);
        if (keepInChatMessage != null) builder.keepInChatMessage(keepInChatMessage);
        if (documentWithCaptionMessage != null) builder.documentWithCaptionMessage(documentWithCaptionMessage);
        if (requestPhoneNumberMessage != null) builder.requestPhoneNumberMessage(requestPhoneNumberMessage);
        if (viewOnceMessageV2 != null) builder.viewOnceMessageV2(viewOnceMessageV2);
        if (encReactionMessage != null) builder.encReactionMessage(encReactionMessage);
        if (editedMessage != null) builder.editedMessage(editedMessage);
        if (viewOnceMessageV2Extension != null) builder.viewOnceMessageV2Extension(viewOnceMessageV2Extension);
        if (pollCreationMessageV2 != null) builder.pollCreationMessageV2(pollCreationMessageV2);
        if (scheduledCallCreationMessage != null) builder.scheduledCallCreationMessage(scheduledCallCreationMessage);
        if (groupMentionedMessage != null) builder.groupMentionedMessage(groupMentionedMessage);
        if (pinInChatMessage != null) builder.pinInChatMessage(pinInChatMessage);
        if (pollCreationMessageV3 != null) builder.pollCreationMessageV3(pollCreationMessageV3);
        if (scheduledCallEditMessage != null) builder.scheduledCallEditMessage(scheduledCallEditMessage);
        if (ptvMessage != null) builder.ptvMessage(ptvMessage);
        if (botInvokeMessage != null) builder.botInvokeMessage(botInvokeMessage);
        if (callLogMesssage != null) builder.callLogMesssage(callLogMesssage);
        if (messageHistoryBundle != null) builder.messageHistoryBundle(messageHistoryBundle);
        if (encCommentMessage != null) builder.encCommentMessage(encCommentMessage);
        if (bcallMessage != null) builder.bcallMessage(bcallMessage);
        if (lottieStickerMessage != null) builder.lottieStickerMessage(lottieStickerMessage);
        if (eventMessage != null) builder.eventMessage(eventMessage);
        if (encEventResponseMessage != null) builder.encEventResponseMessage(encEventResponseMessage);
        if (commentMessage != null) builder.commentMessage(commentMessage);
        if (newsletterAdminInviteMessage != null) builder.newsletterAdminInviteMessage(newsletterAdminInviteMessage);
        if (placeholderMessage != null) builder.placeholderMessage(placeholderMessage);
        if (secretEncryptedMessage != null) builder.secretEncryptedMessage(secretEncryptedMessage);
        if (albumMessage != null) builder.albumMessage(albumMessage);
        if (eventCoverImage != null) builder.eventCoverImage(eventCoverImage);
        if (stickerPackMessage != null) builder.stickerPackMessage(stickerPackMessage);
        if (statusMentionMessage != null) builder.statusMentionMessage(statusMentionMessage);
        if (pollResultSnapshotMessage != null) builder.pollResultSnapshotMessage(pollResultSnapshotMessage);
        if (pollCreationOptionImageMessage != null) builder.pollCreationOptionImageMessage(pollCreationOptionImageMessage);
        if (associatedChildMessage != null) builder.associatedChildMessage(associatedChildMessage);
        if (groupStatusMentionMessage != null) builder.groupStatusMentionMessage(groupStatusMentionMessage);
        if (pollCreationMessageV4 != null) builder.pollCreationMessageV4(pollCreationMessageV4);
        if (statusAddYours != null) builder.statusAddYours(statusAddYours);
        if (groupStatusMessage != null) builder.groupStatusMessage(groupStatusMessage);
        if (richResponseMessage != null) builder.richResponseMessage(richResponseMessage);
        if (statusNotificationMessage != null) builder.statusNotificationMessage(statusNotificationMessage);
        if (limitSharingMessage != null) builder.limitSharingMessage(limitSharingMessage);
        if (botTaskMessage != null) builder.botTaskMessage(botTaskMessage);
        if (questionMessage != null) builder.questionMessage(questionMessage);
        if (messageHistoryNotice != null) builder.messageHistoryNotice(messageHistoryNotice);
        if (groupStatusMessageV2 != null) builder.groupStatusMessageV2(groupStatusMessageV2);
        if (botForwardedMessage != null) builder.botForwardedMessage(botForwardedMessage);
        if (statusQuestionAnswerMessage != null) builder.statusQuestionAnswerMessage(statusQuestionAnswerMessage);
        if (questionReplyMessage != null) builder.questionReplyMessage(questionReplyMessage);
        if (questionResponseMessage != null) builder.questionResponseMessage(questionResponseMessage);
        if (statusQuotedMessage != null) builder.statusQuotedMessage(statusQuotedMessage);
        if (statusStickerInteractionMessage != null) builder.statusStickerInteractionMessage(statusStickerInteractionMessage);
        if (pollCreationMessageV5 != null) builder.pollCreationMessageV5(pollCreationMessageV5);
        if (newsletterFollowerInviteMessageV2 != null) builder.newsletterFollowerInviteMessageV2(newsletterFollowerInviteMessageV2);
        if (pollResultSnapshotMessageV3 != null) builder.pollResultSnapshotMessageV3(pollResultSnapshotMessageV3);
        if (newsletterAdminProfileMessage != null) builder.newsletterAdminProfileMessage(newsletterAdminProfileMessage);
        if (newsletterAdminProfileMessageV2 != null) builder.newsletterAdminProfileMessageV2(newsletterAdminProfileMessageV2);
    }
}
