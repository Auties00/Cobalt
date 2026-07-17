package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebKeepInChatNotifWamEvent")
@WamEvent(id = 3484)
public interface KeepInChatNotifEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong kicGroupNotificationTaps();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong kicGroupNotifications();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong kicNotificationTaps();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong kicNotifications();
}
