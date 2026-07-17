package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyTipActionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPrivacyTipActionWamEvent")
@WamEvent(id = 4794)
public interface PrivacyTipActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PrivacyTipActionType> privacyTipActionType();
}
