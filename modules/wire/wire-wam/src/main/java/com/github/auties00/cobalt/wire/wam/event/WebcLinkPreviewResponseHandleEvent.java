package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcLinkPreviewResponseHandleWamEvent")
@WamEvent(id = 3860)
public interface WebcLinkPreviewResponseHandleEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> didRespondHqPreview();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isPreviewSuccess();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong previewDurationMs();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> previewSessionId();
}
