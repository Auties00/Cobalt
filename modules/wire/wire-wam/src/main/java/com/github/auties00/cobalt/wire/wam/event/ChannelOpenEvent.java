package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPointMetadata;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;
import com.github.auties00.cobalt.wire.wam.type.DeeplinkSource;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelOpenWamEvent")
@WamEvent(id = 4316)
public interface ChannelOpenEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong channelDirectorySessionId();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> channelDiscoveryQueryId();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> channelDiscoverySearchId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChannelEntryPoint> channelEntryPoint();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ChannelEntryPointMetadata> channelEntryPointMetadata();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong channelSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChannelUserType> channelUserType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<DeeplinkSource> deeplinkSource();

    @WamProperty(index = 15, type = WamType.ENUM)
    Optional<TsSurface> discoverySurface();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> entryPointMetadata();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> hasNetworkConnection();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isPremium();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong similarChannelsSessionId();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong unreadMessages();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong unreadPremiumMessages();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
