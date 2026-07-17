package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatbarInitialState;
import com.github.auties00.cobalt.wire.wam.type.PttMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.PttMessageUserJourneyFailureReason;
import com.github.auties00.cobalt.wire.wam.type.PttMessageUserJourneyStage;
import com.github.auties00.cobalt.wire.wam.type.PttWaveformResult;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPttMessageUserJourneyWamEvent")
@WamEvent(id = 5402)
public interface PttMessageUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ChatbarInitialState> chatbarInitialState();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaAiThread();

    @WamProperty(index = 14, type = WamType.FLOAT)
    OptionalDouble pttIntensityAggregateValue();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PttMessageUserJourneyAction> pttMessageUserJourneyAction();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> pttMessageUserJourneyContainsQuotedItem();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<PttMessageUserJourneyFailureReason> pttMessageUserJourneyFailureReason();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PttMessageUserJourneyStage> pttMessageUserJourneyStage();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<PttWaveformResult> pttWaveformResult();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<UserJourneyChatType> userJourneyChatType();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
