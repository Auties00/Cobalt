package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcMediaOperationCode;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMediaErrorUnknownDetailsWamEvent")
@WamEvent(id = 2352)
public interface WebcMediaErrorUnknownDetailsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong mediaId();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> webcMediaErrorMessage();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> webcMediaErrorName();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcMediaOperationCode> webcMediaOperation();
}
