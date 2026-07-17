package com.github.auties00.cobalt.wire.linked.message.system;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A system message emitted by the WhatsApp Business Cloud API when the control
 * over a conversation is transferred between the business owner and an agent
 * integration.
 *
 * <p>The Cloud API supports a handoff protocol that allows different systems
 * (for example a chatbot and a human agent console) to temporarily take or
 * release control of a conversation. When such a transfer happens the server
 * delivers this notification so that every participating endpoint can update
 * its local view of which system is currently responsible for responding to
 * the consumer.
 *
 * <p>The notification carries the new {@link CloudAPIThreadControl} status,
 * the consumer's identity (both LID and phone number), an optional textual
 * description intended for display, and a flag that allows the receiving
 * endpoint to suppress surfacing the event to the end user.
 */
@ProtobufMessage(name = "Message.CloudAPIThreadControlNotification")
public final class CloudAPIThreadControlNotification implements Message {
    /**
     * The new control state for the conversation.
     *
     * <p>Indicates whether control was passed away from the current endpoint
     * or taken by it. May be {@code null} when the server does not set the
     * status explicitly.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    CloudAPIThreadControl status;

    /**
     * The timestamp, in milliseconds, at which the sender issued the control
     * change notification.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderNotificationTimestampMs;

    /**
     * The Linked Identifier (LID) of the consumer whose conversation control
     * is being transferred.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String consumerLid;

    /**
     * The phone number of the consumer whose conversation control is being
     * transferred, in E.164 format when available.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String consumerPhoneNumber;

    /**
     * Optional display content that describes the handoff to the end user.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    CloudAPIThreadControlNotificationContent notificationContent;

    /**
     * Whether the receiving endpoint should suppress surfacing this
     * notification to the end user.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean shouldSuppressNotification;


    /**
     * Constructs a new Cloud API thread control notification.
     *
     * @param status                         the new control state, may be {@code null}
     * @param senderNotificationTimestampMs  the sender notification timestamp, may be {@code null}
     * @param consumerLid                    the consumer LID, may be {@code null}
     * @param consumerPhoneNumber            the consumer phone number, may be {@code null}
     * @param notificationContent            the display content for the handoff, may be {@code null}
     * @param shouldSuppressNotification     whether to suppress user notifications, may be {@code null}
     */
    CloudAPIThreadControlNotification(CloudAPIThreadControl status, Instant senderNotificationTimestampMs, String consumerLid, String consumerPhoneNumber, CloudAPIThreadControlNotificationContent notificationContent, Boolean shouldSuppressNotification) {
        this.status = status;
        this.senderNotificationTimestampMs = senderNotificationTimestampMs;
        this.consumerLid = consumerLid;
        this.consumerPhoneNumber = consumerPhoneNumber;
        this.notificationContent = notificationContent;
        this.shouldSuppressNotification = shouldSuppressNotification;
    }

    /**
     * Returns the new control state for the conversation.
     *
     * @return an {@link Optional} containing the control state, or
     *         {@link Optional#empty()} if no status is set
     */
    public Optional<CloudAPIThreadControl> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the timestamp at which the sender issued the control change
     * notification.
     *
     * @return an {@link Optional} containing the timestamp, or
     *         {@link Optional#empty()} if the timestamp is not set
     */
    public Optional<Instant> senderNotificationTimestampMs() {
        return Optional.ofNullable(senderNotificationTimestampMs);
    }

    /**
     * Returns the Linked Identifier (LID) of the consumer whose conversation
     * control is being transferred.
     *
     * @return an {@link Optional} containing the consumer LID, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<String> consumerLid() {
        return Optional.ofNullable(consumerLid);
    }

    /**
     * Returns the phone number of the consumer whose conversation control is
     * being transferred.
     *
     * @return an {@link Optional} containing the phone number, or
     *         {@link Optional#empty()} if it is not set
     */
    public Optional<String> consumerPhoneNumber() {
        return Optional.ofNullable(consumerPhoneNumber);
    }

    /**
     * Returns the display content associated with the handoff.
     *
     * @return an {@link Optional} containing the notification content, or
     *         {@link Optional#empty()} if no content is set
     */
    public Optional<CloudAPIThreadControlNotificationContent> notificationContent() {
        return Optional.ofNullable(notificationContent);
    }

    /**
     * Indicates whether the receiving endpoint should suppress surfacing this
     * notification to the end user.
     *
     * @return {@code true} if the notification should be hidden from the user,
     *         {@code false} otherwise or when the flag is unset
     */
    public boolean shouldSuppressNotification() {
        return shouldSuppressNotification != null && shouldSuppressNotification;
    }

    /**
     * Sets the new control state for the conversation.
     *
     * @param status the new control state, or {@code null} to clear it
     */
    public void setStatus(CloudAPIThreadControl status) {
        this.status = status;
    }

    /**
     * Sets the timestamp at which the sender issued the control change
     * notification.
     *
     * @param senderNotificationTimestampMs the new timestamp, or {@code null} to clear it
     */
    public void setSenderNotificationTimestampMs(Instant senderNotificationTimestampMs) {
        this.senderNotificationTimestampMs = senderNotificationTimestampMs;
    }

    /**
     * Sets the Linked Identifier (LID) of the consumer.
     *
     * @param consumerLid the new consumer LID, or {@code null} to clear it
     */
    public void setConsumerLid(String consumerLid) {
        this.consumerLid = consumerLid;
    }

    /**
     * Sets the phone number of the consumer.
     *
     * @param consumerPhoneNumber the new phone number, or {@code null} to clear it
     */
    public void setConsumerPhoneNumber(String consumerPhoneNumber) {
        this.consumerPhoneNumber = consumerPhoneNumber;
    }

    /**
     * Sets the display content associated with the handoff.
     *
     * @param notificationContent the new notification content, or {@code null} to clear it
     */
    public void setNotificationContent(CloudAPIThreadControlNotificationContent notificationContent) {
        this.notificationContent = notificationContent;
    }

    /**
     * Sets whether the receiving endpoint should suppress surfacing this
     * notification to the end user.
     *
     * @param shouldSuppressNotification the new suppression flag, or {@code null} to clear it
     */
    public void setShouldSuppressNotification(Boolean shouldSuppressNotification) {
        this.shouldSuppressNotification = shouldSuppressNotification;
    }

    /**
     * Enumerates the possible control states for a WhatsApp Business Cloud API
     * conversation handoff.
     *
     * <p>These values describe the transition that occurred between the
     * business primary endpoint and a secondary integration.
     */
    @ProtobufEnum(name = "Message.CloudAPIThreadControlNotification.CloudAPIThreadControl")
    public static enum CloudAPIThreadControl {
        /**
         * The control state is unspecified or not recognised.
         */
        UNKNOWN(0),
        /**
         * Control has been passed from the current owner to another endpoint.
         */
        CONTROL_PASSED(1),
        /**
         * Control has been taken by the current owner from another endpoint.
         */
        CONTROL_TAKEN(2);

        /**
         * Constructs a new enum constant with the given protobuf index.
         *
         * @param index the protobuf wire index for this constant
         */
        CloudAPIThreadControl(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The textual and structured content that describes a Cloud API thread
     * control handoff.
     *
     * <p>The handoff text is intended for display in the conversation to
     * indicate that control changed. Additional machine readable data can be
     * attached as a free-form JSON string for downstream integrations.
     */
    @ProtobufMessage(name = "Message.CloudAPIThreadControlNotification.CloudAPIThreadControlNotificationContent")
    public static final class CloudAPIThreadControlNotificationContent {
        /**
         * The human readable text describing the handoff, intended for
         * display in the conversation.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String handoffNotificationText;

        /**
         * Additional free-form JSON metadata attached to the notification.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String extraJson;


        /**
         * Constructs a new notification content payload.
         *
         * @param handoffNotificationText the display text for the handoff, may be {@code null}
         * @param extraJson               the extra JSON metadata, may be {@code null}
         */
        CloudAPIThreadControlNotificationContent(String handoffNotificationText, String extraJson) {
            this.handoffNotificationText = handoffNotificationText;
            this.extraJson = extraJson;
        }

        /**
         * Returns the human readable text describing the handoff.
         *
         * @return an {@link Optional} containing the handoff text, or
         *         {@link Optional#empty()} if no text is set
         */
        public Optional<String> handoffNotificationText() {
            return Optional.ofNullable(handoffNotificationText);
        }

        /**
         * Returns the additional free-form JSON metadata.
         *
         * @return an {@link Optional} containing the JSON string, or
         *         {@link Optional#empty()} if no extra metadata is set
         */
        public Optional<String> extraJson() {
            return Optional.ofNullable(extraJson);
        }

        /**
         * Sets the human readable text describing the handoff.
         *
         * @param handoffNotificationText the new handoff text, or {@code null} to clear it
         */
        public void setHandoffNotificationText(String handoffNotificationText) {
            this.handoffNotificationText = handoffNotificationText;
    }

        /**
         * Sets the additional free-form JSON metadata.
         *
         * @param extraJson the new JSON string, or {@code null} to clear it
         */
        public void setExtraJson(String extraJson) {
            this.extraJson = extraJson;
    }
    }
}
