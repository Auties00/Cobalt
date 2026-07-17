package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupRoleType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PinInChatInteractionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPinInChatInteractionWamEvent")
@WamEvent(id = 4436)
public interface PinInChatInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GroupRoleType> groupRole();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<GroupTypeClient> groupTypeClient();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfPin();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong pinCount();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<PinInChatInteractionType> pinInChatInteractionType();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong pinIndex();
}
