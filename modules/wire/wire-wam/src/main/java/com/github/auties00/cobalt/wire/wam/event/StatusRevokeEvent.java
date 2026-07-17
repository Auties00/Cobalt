package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusRevokeWamEvent")
@WamEvent(id = 1250)
public interface StatusRevokeEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong statusLifeT();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
