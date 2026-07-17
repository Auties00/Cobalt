package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.KicErrorCodeType;
import com.github.auties00.cobalt.wire.wam.type.KicRequestTypeType;
import com.github.auties00.cobalt.wire.wam.type.ResponseType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebKeepInChatPerfWamEvent")
@WamEvent(id = 3488)
public interface KeepInChatPerfEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<KicErrorCodeType> kicErrorCode();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong kicMessageEphemeralityDuration();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<KicRequestTypeType> kicRequestType();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong requestSendTime();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ResponseType> response();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> threadId();
}
