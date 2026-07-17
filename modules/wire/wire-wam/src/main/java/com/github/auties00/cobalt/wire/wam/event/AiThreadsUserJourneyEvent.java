package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MetaAiActionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ThreadActionTypes;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAiThreadsUserJourneyWamEvent")
@WamEvent(id = 7224)
public interface AiThreadsUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> aiSessionId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> conversationThreadCreationTs();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> conversationThreadId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong eventTsMs();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isCanonicalThread();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isIncognitoMode();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<MetaAiActionEntryPoint> metaAiActionEntryPoint();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> rawBotEntryPoint();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ThreadActionTypes> threadActionType();
}
