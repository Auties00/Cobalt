package com.github.auties00.cobalt.model.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "Message.CloudAPIThreadControlNotification")
public final class CloudAPIThreadControlNotification {
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final CloudAPIThreadControl status;

    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    final long senderNotificationTimestampMs;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String consumerLid;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String consumerPhoneNumber;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final CloudAPIThreadControlNotificationContent notificationContent;

    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean shouldSuppressNotification;

    public CloudAPIThreadControlNotification(CloudAPIThreadControl status, long senderNotificationTimestampMs, String consumerLid, String consumerPhoneNumber, CloudAPIThreadControlNotificationContent notificationContent, boolean shouldSuppressNotification) {
        this.status = status;
        this.senderNotificationTimestampMs = senderNotificationTimestampMs;
        this.consumerLid = consumerLid;
        this.consumerPhoneNumber = consumerPhoneNumber;
        this.notificationContent = notificationContent;
        this.shouldSuppressNotification = shouldSuppressNotification;
    }

    public Optional<CloudAPIThreadControl> status() {
        return Optional.ofNullable(status);
    }

    public long senderNotificationTimestampMs() {
        return senderNotificationTimestampMs;
    }

    public Optional<String> consumerLid() {
        return Optional.ofNullable(consumerLid);
    }

    public Optional<String> consumerPhoneNumber() {
        return Optional.ofNullable(consumerPhoneNumber);
    }

    public Optional<CloudAPIThreadControlNotificationContent> notificationContent() {
        return Optional.ofNullable(notificationContent);
    }

    public boolean shouldSuppressNotification() {
        return shouldSuppressNotification;
    }

    @ProtobufEnum(name = "Message.CloudAPIThreadControlNotification.CloudAPIThreadControl")
    public enum CloudAPIThreadControl {
        UNKNOWN(0),
        CONTROL_PASSED(1),
        CONTROL_TAKEN(2);

        final int index;

        CloudAPIThreadControl(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    @ProtobufMessage(name = "Message.CloudAPIThreadControlNotification.CloudAPIThreadControlNotificationContent")
    public static final class CloudAPIThreadControlNotificationContent {
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String handoffNotificationText;

        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String extraJson;

        public CloudAPIThreadControlNotificationContent(String handoffNotificationText, String extraJson) {
            this.handoffNotificationText = handoffNotificationText;
            this.extraJson = extraJson;
        }

        public Optional<String> handoffNotificationText() {
            return Optional.ofNullable(handoffNotificationText);
        }

        public Optional<String> extraJson() {
            return Optional.ofNullable(extraJson);
        }
    }
}
