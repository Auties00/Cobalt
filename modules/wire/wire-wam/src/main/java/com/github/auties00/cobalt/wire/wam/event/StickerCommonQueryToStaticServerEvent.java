package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.QueryType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStickerCommonQueryToStaticServerWamEvent")
@WamEvent(id = 2740)
public interface StickerCommonQueryToStaticServerEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong httpResponseCode();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> params();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> queryLatencyMs();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<QueryType> queryType();
}
