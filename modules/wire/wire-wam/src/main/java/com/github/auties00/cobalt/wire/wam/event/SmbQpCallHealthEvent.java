package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.FetchMechanismEnum;
import com.github.auties00.cobalt.wire.wam.type.FetchResultEnum;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSmbQpCallHealthWamEvent")
@WamEvent(id = 6746)
public interface SmbQpCallHealthEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> fetchExceptionMessage();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<FetchMechanismEnum> fetchMechanism();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<FetchResultEnum> fetchResult();
}
