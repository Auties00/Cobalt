package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StatusInteractionActors;
import com.github.auties00.cobalt.wire.wam.type.StatusInteractionMessageType;
import com.github.auties00.cobalt.wire.wam.type.StatusInteractionResultType;
import com.github.auties00.cobalt.wire.wam.type.StatusInteractionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusInteractionReceivedWamEvent")
@WamEvent(id = 6810)
public interface StatusInteractionReceivedEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong channelStatusId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<StatusInteractionActors> statusInteractionActors();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<StatusInteractionMessageType> statusInteractionMessageType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<StatusInteractionResultType> statusInteractionResultType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<StatusInteractionType> statusInteractionType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
