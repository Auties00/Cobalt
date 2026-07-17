package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyControlEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyControlItemType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPrivacySettingsClickWamEvent")
@WamEvent(id = 3726)
public interface PrivacySettingsClickEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PrivacyControlEntryPointType> privacyControlEntryPoint();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PrivacyControlItemType> privacyControlItem();
}
