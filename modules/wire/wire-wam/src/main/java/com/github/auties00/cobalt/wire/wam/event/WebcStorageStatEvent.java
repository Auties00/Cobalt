package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcStorageStatWamEvent")
@WamEvent(id = 1504)
public interface WebcStorageStatEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong webcAgeOfStorage();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> webcPackingEnabled();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong webcStorageQuota();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong webcStorageUsage();
}
