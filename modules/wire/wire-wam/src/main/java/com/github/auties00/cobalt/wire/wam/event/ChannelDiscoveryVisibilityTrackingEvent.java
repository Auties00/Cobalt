package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelDirectoryPillSelected;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventUnit;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelDiscoveryVisibilityTrackingWamEvent")
@WamEvent(id = 5766)
public interface ChannelDiscoveryVisibilityTrackingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong channelCategoryIndex();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> channelCategoryName();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong channelDirectorySessionId();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> channelDiscoveryQueryId();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> channelDiscoverySearchId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChannelEventUnit> channelEventUnit();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong channelIndex();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> countrySelector();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<TsSurface> discoverySurface();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isSubImpression();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<ChannelDirectoryPillSelected> pillSelected();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong similarChannelsSessionId();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
