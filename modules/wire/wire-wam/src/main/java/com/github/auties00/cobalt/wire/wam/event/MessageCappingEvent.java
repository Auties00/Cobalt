package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MessageCappingActionType;
import com.github.auties00.cobalt.wire.wam.type.SurfaceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessageCappingWamEvent")
@WamEvent(id = 6854)
public interface MessageCappingEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MessageCappingActionType> messageCappingActionType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<SurfaceType> messageCappingEntryPoint();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong messageCappingSequence();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> messageCappingSession();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<SurfaceType> surface();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> userActionTarget();
}
