package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.IqResponseType;
import com.github.auties00.cobalt.wire.wam.type.MismatchOriginType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebAddressingModeMismatchWamEvent")
@WamEvent(id = 4750)
public interface AddressingModeMismatchEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<IqResponseType> iqResponse();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MismatchOriginType> mismatchOrigin();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> notificationTag();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<AddressingMode> serverAddressingMode();
}
