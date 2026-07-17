package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GuestUpsellActionType;
import com.github.auties00.cobalt.wire.wam.type.GuestUpsellEntryPointType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebGuestUpsellInteractionWamEvent")
@WamEvent(id = 7146)
public interface GuestUpsellInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GuestUpsellActionType> guestUpsellAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GuestUpsellEntryPointType> guestUpsellEntryPoint();
}
