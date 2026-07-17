package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterActionTypes;
import com.github.auties00.cobalt.wire.wam.type.SurfaceType;
import com.github.auties00.cobalt.wire.wam.type.ThreadType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserRoleType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebGroupJourneyWamEvent")
@WamEvent(id = 4512)
public interface GroupJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatFilterActionTypes> actionType();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<SurfaceType> surface();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ThreadType> threadType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<UserRoleType> userRole();
}
