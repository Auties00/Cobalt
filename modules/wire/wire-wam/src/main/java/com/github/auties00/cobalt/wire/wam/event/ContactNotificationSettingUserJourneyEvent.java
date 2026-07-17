package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactNotificationSettingActionType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebContactNotificationSettingUserJourneyWamEvent")
@WamEvent(id = 5304)
public interface ContactNotificationSettingUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ContactNotificationSettingActionType> contactNotificationSettingActionType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();
}
