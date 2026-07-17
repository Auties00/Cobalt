package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWhatsappQuickPromotionClientEligibilityWaterfallWamEvent")
@WamEvent(id = 4360)
public interface WhatsappQuickPromotionClientEligibilityWaterfallEvent extends WamEventSpec {
    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> clientExtraData();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> eligibilityStatus();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> instanceLogData();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> promotionId();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> qpFailureReason();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> step();
}
