package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessInteractionActionType;
import com.github.auties00.cobalt.wire.wam.type.BusinessInteractionEntryPointApp;
import com.github.auties00.cobalt.wire.wam.type.BusinessInteractionEntryPointSource;
import com.github.auties00.cobalt.wire.wam.type.BusinessInteractionInternalEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.BusinessInteractionTargetScreenType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBusinessInteractionWamEvent")
@WamEvent(id = 3450, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface BusinessInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BusinessInteractionActionType> businessInteractionAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BusinessInteractionTargetScreenType> businessInteractionTargetScreen();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> businessJid();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<BusinessInteractionEntryPointApp> entryPointApp();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<BusinessInteractionEntryPointSource> entryPointSource();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<BusinessInteractionInternalEntryPoint> internalEntryPoint();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong sequenceNumber();
}
