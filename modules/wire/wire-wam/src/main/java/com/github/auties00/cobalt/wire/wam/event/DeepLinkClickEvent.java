package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDeepLinkClickWamEvent")
@WamEvent(id = 1156)
public interface DeepLinkClickEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> deepLinkHasPhoneNumber();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> deepLinkHasText();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> deepLinkHasUsername();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> deepLinkHasUsernamePin();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> deepLinkRequirePinEntry();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> deepLinkSessionId();
}
