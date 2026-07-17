package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.InteractionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.StructuredMessageClass;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsStructuredMessageInteractionWamEvent")
@WamEvent(id = 3052, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface PsStructuredMessageInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BizPlatform> bizPlatform();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> businessOwnerJid();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StructuredMessageClass> messageClass();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> messageClassAttributes();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<InteractionType> messageInteraction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> templateId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
