package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AdditionalCategoryType;
import com.github.auties00.cobalt.wire.wam.type.BotDiscoveryPathType;
import com.github.auties00.cobalt.wire.wam.type.BotEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.BotPromptType;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterActionTypes;
import com.github.auties00.cobalt.wire.wam.type.DiscoveryOriginType;
import com.github.auties00.cobalt.wire.wam.type.InlineTosStatus;
import com.github.auties00.cobalt.wire.wam.type.InputType;
import com.github.auties00.cobalt.wire.wam.type.PromptTriggerPoint;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBotJourneyWamEvent")
@WamEvent(id = 4630)
public interface BotJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatFilterActionTypes> actionType();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<AdditionalCategoryType> additionalCategory();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> aiCreationAvatarCropChanges();

    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<InputType> aiCreationInputType();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> aiCreationPersonalityCategoryInsert();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> aiCreationPersonalityCategorySelect();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> aiDiscoveryTab();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> aiSessionId();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> aiVoiceOnSelectionDefault();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> aiVoiceSelectionEnum();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<BotDiscoveryPathType> botDiscoveryPath();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<BotEntryPointType> botEntryPoint();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> botPersonaId();

    @WamProperty(index = 49, type = WamType.ENUM)
    Optional<BotPromptType> botPromptType();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> callRandomId();

    @WamProperty(index = 40, type = WamType.STRING)
    Optional<String> categoryType();

    @WamProperty(index = 52, type = WamType.STRING)
    Optional<String> commandName();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> conversationStarterCategory();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong conversationStarterIndex();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> conversationStarterLabel();

    @WamProperty(index = 51, type = WamType.STRING)
    Optional<String> conversationStarterName();

    @WamProperty(index = 28, type = WamType.STRING)
    Optional<String> conversationStarterPromptMode();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> deviceLanguage();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<DiscoveryOriginType> discoveryOrigin();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong eventTsMs();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> hasContinueChatting();

    @WamProperty(index = 42, type = WamType.BOOLEAN)
    Optional<Boolean> hasYourAiCategory();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> inlineTosNoticeId();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<InlineTosStatus> inlineTosStatus();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong interestCategoriesSelected();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> isCache();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaAiAssistant();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaAiCharacterBotChat();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isUserCreatedAgent();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong metricCount();

    @WamProperty(index = 50, type = WamType.ENUM)
    Optional<PromptTriggerPoint> promptTriggerPoint();

    @WamProperty(index = 48, type = WamType.STRING)
    Optional<String> rawBotEntryPoint();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong scrollDepth();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong scrollFetchLatency();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong xmaReelIndex();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong xmaReelMaxIndex();
}
