package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupRoleType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PinInChatType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPinInChatMessageSendWamEvent")
@WamEvent(id = 4438)
public interface PinInChatMessageSendEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GroupRoleType> groupRole();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupTypeClient> groupTypeClient();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfParentMessage();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfPin();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong pinInChatExpirySecs();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<PinInChatType> pinInChatType();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong timeRemainingToExpirySecs();
}
