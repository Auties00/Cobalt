package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PsTestEnumField;

import java.util.Optional;
import java.util.OptionalDouble;

@WhatsAppWebModule(moduleName = "WAWebTestAnonymousDailyIdWamEvent")
@WamEvent(id = 2958, channel = WamChannel.PRIVATE, privateStatsId = 248614979)
public interface TestAnonymousDailyIdEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PsTestEnumField> psTestEnumField();

    @WamProperty(index = 2, type = WamType.FLOAT)
    OptionalDouble psTestFloatField();
}
