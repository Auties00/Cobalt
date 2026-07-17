package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareDirection;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelLinkShareScreen;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelLinkShareWamEvent")
@WamEvent(id = 4728)
public interface ChannelLinkShareEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChannelLinkShareDirection> channelLinkShareDirection();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChannelLinkShareEntryPoint> channelLinkShareEntryPoint();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChannelLinkShareScreen> channelLinkShareScreen();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
