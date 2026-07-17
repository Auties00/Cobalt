package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcDisplayStatusType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcLinkPreviewDisplayWamEvent")
@WamEvent(id = 3864)
public interface WebcLinkPreviewDisplayEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> didFallbackNonHq();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> didRequestHq();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> didRespondHqPreview();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebcDisplayStatusType> webcDisplayStatus();
}
