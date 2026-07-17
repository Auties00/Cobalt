package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActionTarget;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsActionType;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ChannelProducerInsightsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelProducerInsightsNavigationWamEvent")
@WamEvent(id = 5626)
public interface ChannelProducerInsightsNavigationEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ActionTarget> channelProducerInsightsActionTarget();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChannelProducerInsightsActionType> channelProducerInsightsActionType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ChannelProducerInsightsEntryPoint> channelProducerInsightsEntryPoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong channelProducerInsightsSequenceNumber();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ChannelProducerInsightsSurface> channelProducerInsightsSurface();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong producerInsightsSessionId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
