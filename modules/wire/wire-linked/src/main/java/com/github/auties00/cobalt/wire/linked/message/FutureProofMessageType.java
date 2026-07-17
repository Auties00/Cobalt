package com.github.auties00.cobalt.wire.linked.message;

import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessage;
import com.github.auties00.cobalt.wire.linked.message.system.FutureProofMessage;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Identifies which forward-compatibility envelope is wrapping the payload
 * of a {@link LinkedMessageContainer}, if any.
 *
 * <p>WhatsApp introduces new message types frequently. To keep older
 * clients from failing when they encounter an unknown type, the protocol
 * wraps certain messages inside {@link FutureProofMessage} envelopes or
 * the companion-distribution {@link DeviceSentMessage} envelope. A client
 * that cannot decode the inner payload can still forward the raw
 * protobuf bytes untouched.
 *
 * <p>This enum is returned by {@link LinkedMessageContainer#futureProofContentType()}
 * and tells the caller which wrapper field holds the actual content,
 * allowing feature-specific handling (for example, rendering ephemeral
 * messages differently from regular ones) without manually checking
 * every wrapper field.
 */
@ProtobufEnum(name = "FutureProofMessageType")
public enum FutureProofMessageType {
    /**
     * No wrapper is present; the container holds a direct message field.
     *
     * <p>This is the default state for normal text, media, and other
     * top-level messages that do not require forward-compatibility
     * wrapping.
     */
    NONE(0),

    /**
     * A view-once media message.
     *
     * <p>Covers all three protobuf versions of view-once: V1, V2, and
     * V2 extension. View-once media self-destructs after being opened
     * by the recipient.
     */
    VIEW_ONCE(1),

    /**
     * A disappearing (ephemeral) message.
     *
     * <p>The message auto-deletes for every participant once the chat's
     * ephemeral timer elapses after it was sent.
     */
    EPHEMERAL(2),

    /**
     * A document message that carries a caption alongside the file.
     *
     * <p>Wrapped separately from regular documents so older clients can
     * still display the file while newer clients render the caption.
     */
    DOCUMENT_WITH_CAPTION(3),

    /**
     * An edit applied to a previously sent message.
     *
     * <p>The inner container holds the new content that replaces the
     * original message body in the recipient's view.
     */
    EDITED(4),

    /**
     * A group-mention message.
     *
     * <p>Used when a user mentions an entire group or a subset of group
     * members. Has the highest unwrapping priority on WhatsApp Web
     * because these messages can wrap any other type.
     */
    GROUP_MENTIONED(5),

    /**
     * A bot invocation message.
     *
     * <p>Sent when the user interacts with a WhatsApp AI bot; the wrapped
     * payload contains the prompt or action being invoked.
     */
    BOT_INVOKE(6),

    /**
     * An animated Lottie sticker message.
     *
     * <p>Wraps a sticker that uses the Lottie vector animation format
     * rather than a static image.
     */
    LOTTIE_STICKER(7),

    /**
     * An event cover image attached to a calendar event.
     */
    EVENT_COVER_IMAGE(8),

    /**
     * A status mention message.
     *
     * <p>Sent when a user's status update explicitly references another
     * user's status.
     */
    STATUS_MENTION(9),

    /**
     * A media image used as an option inside a poll creation message.
     */
    POLL_CREATION_OPTION_IMAGE(10),

    /**
     * An associated child message linked to a parent message.
     *
     * <p>Used for message association features such as media albums,
     * HD dual uploads, and motion photos where multiple messages share
     * a common parent.
     */
    ASSOCIATED_CHILD(11),

    /**
     * A group status mention message.
     */
    GROUP_STATUS_MENTION(12),

    /**
     * A poll creation message wrapped for forward compatibility.
     *
     * <p>Corresponds to the V4 poll creation field.
     */
    POLL_CREATION(13),

    /**
     * A status Add Yours sticker or prompt.
     *
     * <p>Invites other users to contribute to a shared status chain
     * started by the sender.
     */
    STATUS_ADD_YOURS(14),

    /**
     * A group status message.
     *
     * <p>Covers both V1 and V2 group-status variants. Group statuses
     * broadcast updates to group members rather than individual
     * contacts.
     */
    GROUP_STATUS(15),

    /**
     * A limit sharing message.
     *
     * <p>Indicates that a message has an upper bound on how far it may
     * be forwarded.
     */
    LIMIT_SHARING(16),

    /**
     * A bot task message.
     *
     * <p>Represents a task the AI bot is executing on behalf of the user.
     */
    BOT_TASK(17),

    /**
     * A question message in an AI or bot context.
     *
     * <p>Typically a standalone question sent to the AI bot that expects
     * a textual or structured response.
     */
    QUESTION(18),

    /**
     * A message that has been forwarded by the AI bot.
     */
    BOT_FORWARDED(19),

    /**
     * A question reply in an AI or bot context.
     *
     * <p>Represents the follow-up reply to a previously asked question.
     */
    QUESTION_REPLY(20),

    /**
     * A newsletter (channel) admin profile message.
     *
     * <p>Covers both V1 and V2 variants and carries the public profile
     * metadata shown to newsletter followers.
     */
    NEWSLETTER_ADMIN_PROFILE(21),

    /**
     * A message mirrored from another linked device of the same user.
     *
     * <p>When a user sends a message from one device, WhatsApp wraps it
     * in a {@link DeviceSentMessage} and distributes it to the user's
     * other linked devices so they can display the same message locally.
     */
    DEVICE_SENT(22);

    /**
     * Constructs a new enum constant with the given protobuf wire index.
     *
     * @param index the protobuf wire index associated with this constant
     */
    FutureProofMessageType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The protobuf wire index representing this constant on the wire.
     */
    final int index;

    /**
     * Returns the protobuf wire index representing this constant.
     *
     * @return the non-negative wire index
     */
    public int index() {
        return this.index;
    }
}
