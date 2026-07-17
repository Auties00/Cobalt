package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BannerStatus;
import com.github.auties00.cobalt.wire.wam.type.BannerStatusReason;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventSurface;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelSimilarChannelsWamEvent")
@WamEvent(id = 5202)
public interface ChannelSimilarChannelsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BannerStatus> bannerStatus();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BannerStatusReason> bannerStatusReason();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong similarChannelDisplayRank();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ChannelEventSurface> similarChannelEventSurface();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> similarChannelId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong similarChannelRank();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ChannelUserType> similarChannelUserType();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong similarChannelsSessionId();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
