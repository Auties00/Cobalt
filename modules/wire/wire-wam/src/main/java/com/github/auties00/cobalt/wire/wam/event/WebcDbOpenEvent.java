package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcDbOpenWamEvent")
@WamEvent(id = 1940, betaWeight = 1000, releaseWeight = 2000)
public interface WebcDbOpenEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> webcDbName();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong webcDbOpenNumAttempts();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> webcDbOpenWasSuccess();
}
