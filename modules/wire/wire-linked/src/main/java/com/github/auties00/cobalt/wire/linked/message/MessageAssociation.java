package com.github.auties00.cobalt.wire.linked.message;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Links a message to another parent message as part of a multi-message
 * feature, such as a media album or a status reshare.
 *
 * <p>Several WhatsApp features surface as a single logical unit in the UI
 * but are actually composed of multiple underlying messages. A media album
 * sent to a chat, for example, is delivered as several individual image
 * or video messages plus an {@link MessageAssociation} on each one that
 * points back to the parent album message and records the child's position
 * within the album.
 *
 * <p>A {@code MessageAssociation} carries:
 * <ul>
 *   <li>{@link #associationType()} describing which multi-message feature
 *       this message participates in</li>
 *   <li>{@link #parentMessageKey()} identifying the parent message</li>
 *   <li>{@link #messageIndex()} recording the zero-based position of this
 *       child within the parent's ordered children</li>
 * </ul>
 */
@ProtobufMessage(name = "MessageAssociation")
public final class MessageAssociation {
    /**
     * The type of multi-message relationship this association represents.
     *
     * <p>See {@link AssociationType} for the complete set of supported
     * relationships.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    AssociationType associationType;

    /**
     * The {@link MessageKey} of the parent message that anchors the
     * group to which this message belongs.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageKey parentMessageKey;

    /**
     * The zero-based position of this message within the ordered list
     * of its parent's children.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer messageIndex;


    /**
     * Constructs a new {@code MessageAssociation}.
     *
     * <p>The constructor is package-private; use
     * {@code MessageAssociationBuilder} to instantiate new associations.
     *
     * @param associationType  the type of multi-message relationship
     * @param parentMessageKey the key of the parent message
     * @param messageIndex     the zero-based position within the parent
     */
    MessageAssociation(AssociationType associationType, MessageKey parentMessageKey, Integer messageIndex) {
        this.associationType = associationType;
        this.parentMessageKey = parentMessageKey;
        this.messageIndex = messageIndex;
    }

    /**
     * Returns the type of multi-message relationship this association
     * describes.
     *
     * @return an {@link Optional} holding the {@link AssociationType},
     *         or empty if none was set
     */
    public Optional<AssociationType> associationType() {
        return Optional.ofNullable(associationType);
    }

    /**
     * Returns the {@link MessageKey} of the parent message.
     *
     * @return an {@link Optional} holding the parent key,
     *         or empty if none was set
     */
    public Optional<MessageKey> parentMessageKey() {
        return Optional.ofNullable(parentMessageKey);
    }

    /**
     * Returns the zero-based position of this message within its
     * parent's ordered children.
     *
     * @return an {@link OptionalInt} holding the index,
     *         or empty if none was set
     */
    public OptionalInt messageIndex() {
        return messageIndex == null ? OptionalInt.empty() : OptionalInt.of(messageIndex);
    }

    /**
     * Updates the type of multi-message relationship.
     *
     * @param associationType the new association type, or {@code null}
     *                        to clear
     */
    public void setAssociationType(AssociationType associationType) {
        this.associationType = associationType;
    }

    /**
     * Updates the parent message key.
     *
     * @param parentMessageKey the new parent key, or {@code null}
     *                         to clear
     */
    public void setParentMessageKey(MessageKey parentMessageKey) {
        this.parentMessageKey = parentMessageKey;
    }

    /**
     * Updates the zero-based position of this message within its
     * parent's ordered children.
     *
     * @param messageIndex the new index, or {@code null} to clear
     */
    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    /**
     * Enumerates the kinds of multi-message relationships supported
     * by WhatsApp.
     *
     * <p>Each constant identifies a specific feature that aggregates
     * multiple underlying messages under a common parent.
     */
    @ProtobufEnum(name = "MessageAssociation.AssociationType")
    public static enum AssociationType {
        /**
         * Default value used when the association type is unrecognised
         * by the current client.
         */
        UNKNOWN(0),

        /**
         * A collection of media messages displayed together as a single
         * album in the chat UI.
         */
        MEDIA_ALBUM(1),

        /**
         * A message produced by a bot plugin invocation; the child
         * message is the plugin output associated with the user's prompt.
         */
        BOT_PLUGIN(2),

        /**
         * An image attached as the cover of a calendar event.
         */
        EVENT_COVER_IMAGE(3),

        /**
         * A poll embedded within a status update.
         */
        STATUS_POLL(4),

        /**
         * A video uploaded twice (standard quality and HD) so clients
         * can choose the appropriate rendition.
         */
        HD_VIDEO_DUAL_UPLOAD(5),

        /**
         * A status reshared to another platform such as Instagram or
         * Facebook.
         */
        STATUS_EXTERNAL_RESHARE(6),

        /**
         * A poll embedded within a media message (album or single item).
         */
        MEDIA_POLL(7),

        /**
         * A contribution to an Add Yours status chain.
         */
        STATUS_ADD_YOURS(8),

        /**
         * A notification raised by a status update, such as a reaction
         * or reply.
         */
        STATUS_NOTIFICATION(9),

        /**
         * An image uploaded twice (standard quality and HD) so clients
         * can choose the appropriate rendition.
         */
        HD_IMAGE_DUAL_UPLOAD(10),

        /**
         * A sticker placed on top of another piece of content as an
         * annotation.
         */
        STICKER_ANNOTATION(11),

        /**
         * A motion photo pairing a still image with a short video clip.
         */
        MOTION_PHOTO(12),

        /**
         * A link action attached to a status update.
         */
        STATUS_LINK_ACTION(13),

        /**
         * A "view all replies" grouping inside a threaded reply chain.
         */
        VIEW_ALL_REPLIES(14),

        /**
         * An AI-imagine contribution to an Add Yours status chain.
         */
        STATUS_ADD_YOURS_AI_IMAGINE(15),

        /**
         * A question prompt attached to a status update.
         */
        STATUS_QUESTION(16),

        /**
         * A Diwali-themed contribution to an Add Yours status chain.
         */
        STATUS_ADD_YOURS_DIWALI(17),

        /**
         * A reaction applied to a status update.
         */
        STATUS_REACTION(18),

        /**
         * A video uploaded twice using the H.265/HEVC codec so clients
         * can choose the appropriate rendition.
         */
        HEVC_VIDEO_DUAL_UPLOAD(19);

        /**
         * Constructs a new enum constant with the given protobuf wire
         * index.
         *
         * @param index the protobuf wire index for this constant
         */
        AssociationType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index identifying this constant on the wire.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the non-negative wire index
         */
        public int index() {
            return this.index;
        }
    }
}
