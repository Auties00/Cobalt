package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.KicActionType;
import com.github.auties00.cobalt.wire.wam.type.KicErrorCodeType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebKeepInChatErrorsWamEvent")
@WamEvent(id = 3698)
public interface KeepInChatErrorsEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> canEditDmSettings();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<KicActionType> kicAction();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<KicErrorCodeType> kicErrorCode();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong kicMessageEphemeralityDuration();
}
