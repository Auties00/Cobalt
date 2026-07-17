package com.github.auties00.cobalt.wire.linked.message.status;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Server-generated notification delivered to a status author when someone
 * interacts with their status through an "Add Yours", reshare, or
 * question-answer reshare flow.
 *
 * <p>This message pairs the key of the user's interaction with the key of
 * the original status so that the author's client can connect the two and
 * render the correct in-app notification. The {@link StatusNotificationType}
 * indicates which interaction flow the notification corresponds to.
 *
 * @see MessageKey
 * @see StatusNotificationType
 */
@ProtobufMessage(name = "Message.StatusNotificationMessage")
public final class StatusNotificationMessage implements Message {
    /**
     * Key of the responding status or message that triggered this
     * notification.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey responseMessageKey;

    /**
     * Key of the original status that was interacted with.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    MessageKey originalMessageKey;

    /**
     * Interaction flow that triggered this notification.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    StatusNotificationType type;

    /**
     * Constructs a new {@code StatusNotificationMessage} with the supplied
     * fields.
     *
     * @param responseMessageKey the key of the responding message, or {@code null} if absent
     * @param originalMessageKey the key of the original status, or {@code null} if absent
     * @param type               the interaction flow, or {@code null} if absent
     */
    StatusNotificationMessage(MessageKey responseMessageKey, MessageKey originalMessageKey, StatusNotificationType type) {
        this.responseMessageKey = responseMessageKey;
        this.originalMessageKey = originalMessageKey;
        this.type = type;
    }

    /**
     * Returns the key of the responding message that triggered this
     * notification.
     *
     * @return the response message key, or {@code Optional.empty()} if absent
     */
    public Optional<MessageKey> responseMessageKey() {
        return Optional.ofNullable(responseMessageKey);
    }

    /**
     * Returns the key of the original status that was interacted with.
     *
     * @return the original message key, or {@code Optional.empty()} if absent
     */
    public Optional<MessageKey> originalMessageKey() {
        return Optional.ofNullable(originalMessageKey);
    }

    /**
     * Returns the interaction flow that triggered this notification.
     *
     * @return the notification type, or {@code Optional.empty()} if absent
     */
    public Optional<StatusNotificationType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the key of the responding message.
     *
     * @param responseMessageKey the response message key, or {@code null} to clear
     */
    public void setResponseMessageKey(MessageKey responseMessageKey) {
        this.responseMessageKey = responseMessageKey;
    }

    /**
     * Sets the key of the original status that was interacted with.
     *
     * @param originalMessageKey the original message key, or {@code null} to clear
     */
    public void setOriginalMessageKey(MessageKey originalMessageKey) {
        this.originalMessageKey = originalMessageKey;
    }

    /**
     * Sets the interaction flow that triggered this notification.
     *
     * @param type the notification type, or {@code null} to clear
     */
    public void setType(StatusNotificationType type) {
        this.type = type;
    }

    /**
     * Enumerates the status-interaction flows that can produce a
     * {@link StatusNotificationMessage}.
     */
    @ProtobufEnum(name = "Message.StatusNotificationMessage.StatusNotificationType")
    public static enum StatusNotificationType {
        /**
         * Notification kind was not recognised or is not set.
         */
        UNKNOWN(0),
        /**
         * Notification was produced by someone responding to an "Add Yours"
         * status template.
         */
        STATUS_ADD_YOURS(1),
        /**
         * Notification was produced by someone resharing the status.
         */
        STATUS_RESHARE(2),
        /**
         * Notification was produced by someone answering a question posted
         * on the status and resharing the answer.
         */
        STATUS_QUESTION_ANSWER_RESHARE(3);

        /**
         * Constructs a new {@code StatusNotificationType} enum constant.
         *
         * @param index the protobuf wire index
         */
        StatusNotificationType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index used to serialise this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
