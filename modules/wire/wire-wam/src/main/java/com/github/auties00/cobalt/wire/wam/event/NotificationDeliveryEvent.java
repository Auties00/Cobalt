package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.NotificationActionType;
import com.github.auties00.cobalt.wire.wam.type.NotificationDestinationType;
import com.github.auties00.cobalt.wire.wam.type.NotificationSourceType;
import com.github.auties00.cobalt.wire.wam.type.NotificationTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.PeripheralDeviceType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebNotificationDeliveryWamEvent")
@WamEvent(id = 3748)
public interface NotificationDeliveryEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong channelMilestoneValue();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> clientMessageId();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isSilenced();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<NotificationActionType> notificationAction();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong notificationDeliveryT();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<NotificationDestinationType> notificationDestination();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> notificationId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<NotificationSourceType> notificationSource();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<PeripheralDeviceType> peripheralDeviceOrigin();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> postId();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> pushToNotifT();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> triggeredByOfflineMessage();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<NotificationTypeEnum> uiNotificationType();
}
