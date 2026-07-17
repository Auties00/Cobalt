package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWefrClientExposureWamEvent")
@WamEvent(id = 5504, channel = WamChannel.REALTIME)
public interface WefrClientExposureEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong canonicalEntLastValidationTsMs();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> deviceExpId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> exposureKey();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> fromMetaconfig();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> guestId();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isCanonicalEntPresent();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> sentWithDaily();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong userLid();
}
