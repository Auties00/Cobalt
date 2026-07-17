package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AiChatOriginsType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebThreadInteractionDataAiWamEvent")
@WamEvent(id = 6410)
public interface ThreadInteractionDataAiEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<AiChatOriginsType> aiChatOrigins();

    @WamProperty(index = 29, type = WamType.STRING)
    Optional<String> aiDiscoveryTab();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong bottomSheetAnimatedSent();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong bottomSheetEditedAnimatedSent();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong bottomSheetEditedSent();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong bottomSheetImagesGenerated();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong bottomSheetMemuInitiated();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong bottomSheetMemuMessagesSent();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong bottomSheetMessagesSent();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong bottomSheetPromptsInitiated();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong bottomSheetRegeneratedSent();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong commandSheetShow();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong imagineCommandClick();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong imagineMeMessagesSent();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong imagineMePromptsInitiatedCount();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong metaAiMentionClick();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong metaAiMentionShow();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong suggestionPromptsClick();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong suggestionPromptsShow();

    @WamProperty(index = 28, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> threadIdByLid();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong totalMessageFromAgentCnt();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong totalMessageToAgentCnt();
}
