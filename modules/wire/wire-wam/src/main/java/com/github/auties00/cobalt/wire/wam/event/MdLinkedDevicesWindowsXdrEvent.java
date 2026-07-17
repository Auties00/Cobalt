package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MdLinkedDevicesWindowsXdrStage;
import com.github.auties00.cobalt.wire.wam.type.MdXdrTransportType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdLinkedDevicesWindowsXdrWamEvent")
@WamEvent(id = 7804)
public interface MdLinkedDevicesWindowsXdrEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MdLinkedDevicesWindowsXdrStage> mdLinkedDevicesWindowsXdrStage();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong mdXdrDebounceTimeoutInMs();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> mdXdrErrorReason();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> mdXdrPayload();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> mdXdrSessionUuid();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MdXdrTransportType> mdXdrTransportType();
}
