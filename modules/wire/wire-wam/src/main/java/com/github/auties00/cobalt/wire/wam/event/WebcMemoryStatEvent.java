package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcRuntimeEnvCode;
import com.github.auties00.cobalt.wire.wam.type.WebcScenarioType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMemoryStatWamEvent")
@WamEvent(id = 1188, releaseWeight = 10)
public interface WebcMemoryStatEvent extends WamEventSpec {
    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong chatCollectionSize();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong chatDbSize();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong contactCollectionSize();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong contactDbSize();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isForeground();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong jsHeapSizeLimit();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong messageCollectionSize();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong messageDbSize();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong peakUsedJsHeapSize();

    @WamProperty(index = 15, type = WamType.ENUM)
    Optional<WebcScenarioType> scenario();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong totalJsHeapSize();

    @WamProperty(index = 6, type = WamType.FLOAT)
    OptionalDouble uptime();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong usedJsHeapSize();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong usedJsHeapSizeDelta();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<WebcRuntimeEnvCode> webcRuntimeEnv();
}
