package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AdminFlowType;
import com.github.auties00.cobalt.wire.wam.type.ChannelAdminAction;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelAdminWamEvent")
@WamEvent(id = 4556)
public interface ChannelAdminEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong adminFlowActionSequenceNumber();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<AdminFlowType> adminFlowType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ChannelAdminAction> channelAdminAction();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong channelAdminSessionId();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
