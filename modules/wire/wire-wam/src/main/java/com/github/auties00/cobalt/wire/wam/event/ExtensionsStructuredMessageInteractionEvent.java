package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.EntryPointConversationInitiated;
import com.github.auties00.cobalt.wire.wam.type.FlowEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.InteractionType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.StructuredMessageClass;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebExtensionsStructuredMessageInteractionWamEvent")
@WamEvent(id = 4114, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface ExtensionsStructuredMessageInteractionEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> adContext();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BizPlatform> bizPlatform();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> businessOwnerJid();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<EntryPointConversationInitiated> entryPointConversationInitiated();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> entryPointConversionApp();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<FlowEntryPoint> flowEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StructuredMessageClass> messageClass();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> messageClassAttributes();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<InteractionType> messageInteraction();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
