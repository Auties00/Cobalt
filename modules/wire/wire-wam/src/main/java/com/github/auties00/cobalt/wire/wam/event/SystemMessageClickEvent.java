package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SystemMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.SystemMessageTypeType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSystemMessageClickWamEvent")
@WamEvent(id = 5082)
public interface SystemMessageClickEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isANewThread();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<SystemMessageCategoryType> systemMessageCategory();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<SystemMessageTypeType> systemMessageType();
}
