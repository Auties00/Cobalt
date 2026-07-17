package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebContactListStartNewChatType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebContactListStartNewChatWamEvent")
@WamEvent(id = 4560)
public interface WebContactListStartNewChatEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> webContactListStartNewChatSearch();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebContactListStartNewChatType> webContactListStartNewChatType();
}
