package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.ThreadType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserRoleType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessagingUserJourneyWamEvent")
@WamEvent(id = 5134)
public interface MessagingUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfPin();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ActionType> messagingActionType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong pinInChatExpirySecs();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ThreadType> threadType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<UserRoleType> userRole();
}
