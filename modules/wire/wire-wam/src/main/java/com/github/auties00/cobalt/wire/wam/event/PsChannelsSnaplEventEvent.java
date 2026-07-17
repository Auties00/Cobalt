package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsChannelsSnaplEventWamEvent")
@WamEvent(id = 6254, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface PsChannelsSnaplEventEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong appId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> videoEventJson();
}
