package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StickerLatencyAction;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStickerLatencyWamEvent")
@WamEvent(id = 5026)
public interface StickerLatencyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong size();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<StickerLatencyAction> stickerLatencyAction();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong stickerLatencyTtAction();
}
