package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.OffboardSources;
import com.github.auties00.cobalt.wire.wam.type.OnboardSources;
import com.github.auties00.cobalt.wire.wam.type.PushNotificationInteractions;
import com.github.auties00.cobalt.wire.wam.type.WebNotificationSettingType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcBackgroundSyncAdoptionWamEvent")
@WamEvent(id = 5302)
public interface WebcBackgroundSyncAdoptionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<OffboardSources> offboardSource();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<OnboardSources> onboardSource();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PushNotificationInteractions> pushNotificationInteraction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<WebNotificationSettingType> webOsNotificationSetting();
}
