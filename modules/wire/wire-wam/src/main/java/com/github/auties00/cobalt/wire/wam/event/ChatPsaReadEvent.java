package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.ReadEntryPoint;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebChatPsaReadWamEvent")
@WamEvent(id = 3574)
public interface ChatPsaReadEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> psaCampaignId();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> psaMsgId();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ReadEntryPoint> readEntryPoint();
}
