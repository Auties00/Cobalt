package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeleteActionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessageDeleteActionsWamEvent")
@WamEvent(id = 3626)
public interface MessageDeleteActionsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeleteActionType> deleteActionType();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong messagesDeleted();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> threadId();
}
