package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeviceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMdRetryFromUnknownDeviceWamEvent")
@WamEvent(id = 2178)
public interface MdRetryFromUnknownDeviceEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> offline();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeviceType> senderType();
}
