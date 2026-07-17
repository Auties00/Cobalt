package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWefrGroupClientExposureWamEvent")
@WamEvent(id = 6640, channel = WamChannel.REALTIME)
public interface WefrGroupClientExposureEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> exposureKey();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> groupJid();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> sentWithDaily();
}
