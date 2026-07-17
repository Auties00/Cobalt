package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkAction;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDeepLinkMsgSentWamEvent")
@WamEvent(id = 3198)
public interface DeepLinkMsgSentEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeepLinkAction> deepLinkAction();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> deepLinkSessionId();
}
