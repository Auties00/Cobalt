package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChannelOpenFromInviteWamEvent")
@WamEvent(id = 7134, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface ChannelOpenFromInviteEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChannelEntryPoint> channelEntryPoint();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChannelUserType> channelUserType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<TsSurface> discoverySurface();
}
