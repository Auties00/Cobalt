package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.InAppNotificationAlertStyle;
import com.github.auties00.cobalt.wire.wam.type.NotificationSoundTone;
import com.github.auties00.cobalt.wire.wam.type.UnreadBadgeSettingType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebNotificationSettingWamEvent")
@WamEvent(id = 3684)
public interface NotificationSettingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> groupReactionNotification();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> groupShowNotification();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<NotificationSoundTone> groupSoundTone();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<InAppNotificationAlertStyle> inAppNotificationAlertStyle();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> inAppNotificationSound();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> inAppNotificationVibrate();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> messageReactionNotification();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> messageShowNotification();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<NotificationSoundTone> messageSoundTone();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> offlineNotification();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> recommendedChannelsNotificationSetting();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> showPreview();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> statusNotificationPriority();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> statusNotificationVibration();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> statusReactionNotification();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> statusReminderNotification();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> statusShowNotification();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<NotificationSoundTone> statusSoundTone();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<UnreadBadgeSettingType> unreadBadgeSetting();
}
