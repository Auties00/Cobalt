package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DisclosureAction;
import com.github.auties00.cobalt.wire.wam.type.DisclosureContextType;
import com.github.auties00.cobalt.wire.wam.type.DisclosureEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.DisclosureType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCtwaConsumerDisclosureWamEvent")
@WamEvent(id = 4406, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface CtwaConsumerDisclosureEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong ctwaConsumerDisclosureVersion();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DisclosureAction> disclosureAction();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<DisclosureContextType> disclosureContext();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<DisclosureEntryPointType> disclosureEntryPoint();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DisclosureType> disclosureType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
