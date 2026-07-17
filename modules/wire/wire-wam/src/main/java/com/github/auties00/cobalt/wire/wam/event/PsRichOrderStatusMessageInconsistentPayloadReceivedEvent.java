package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsRichOrderStatusMessageInconsistentPayloadReceivedWamEvent")
@WamEvent(id = 6938, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface PsRichOrderStatusMessageInconsistentPayloadReceivedEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessJid();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> hasCurrencyChanged();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> hasHeaderImageChanged();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemImageChanged();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemNameChanged();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemNumberChanged();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemPriceChanged();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemQuantityChanged();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> hasItemVariantChanged();
}
