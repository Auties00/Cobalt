package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.ChatbarInitialState;
import com.github.auties00.cobalt.wire.wam.type.TextMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebTextMessageUserJourneyWamEvent")
@WamEvent(id = 5404)
public interface TextMessageUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ChatbarInitialState> chatbarInitialState();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<TextMessageUserJourneyAction> textMessageUserJourneyAction();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> textMessageUserJourneyContainsQuotedItem();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<UserJourneyChatType> userJourneyChatType();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
