package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPointApp;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPointMetadata;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventSurface;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventType;
import com.github.auties00.cobalt.wire.wam.type.ChannelEventUnit;
import com.github.auties00.cobalt.wire.wam.type.CoreEventTriggerType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelCoreEventWamEvent")
@WamEvent(id = 4692)
public interface ChannelCoreEventEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong channelCoreEventSequenceNumber();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChannelEventType> channelCoreEventType();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong channelDirectorySessionId();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> channelDiscoveryQueryId();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> channelDiscoverySearchId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChannelEntryPoint> channelEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ChannelEntryPointApp> channelEntryPointApp();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<ChannelEntryPointMetadata> channelEntryPointMetadata();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ChannelEventUnit> channelEventUnit();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> channelRequestMetadata();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<CoreEventTriggerType> coreEventTrigger();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong directoryChannelIndex();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<TsSurface> discoverySurface();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> entryPointMetadata();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ChannelEventSurface> eventSurface();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong similarChannelsSessionId();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
