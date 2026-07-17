package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PreferredLinkType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebCtwaActionBannerUnderstandWamEvent")
@WamEvent(id = 3586, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface CtwaActionBannerUnderstandEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> bannerIdentifier();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> bannerLocale();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> clientLocale();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> hasLocalLink();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> hasUniversalLink();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> invalidLink();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> lwiFlowIdentifier();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> notificationLogId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<PreferredLinkType> preferredLink();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> validLocale();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> validNotification();
}
