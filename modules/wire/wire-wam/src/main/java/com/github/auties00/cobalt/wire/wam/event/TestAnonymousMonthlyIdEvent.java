package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;


@WhatsAppWebModule(moduleName = "WAWebTestAnonymousMonthlyIdWamEvent")
@WamEvent(id = 2960, channel = WamChannel.PRIVATE, betaWeight = 20, releaseWeight = 1000, privateStatsId = 191000728)
public interface TestAnonymousMonthlyIdEvent extends WamEventSpec {
}
