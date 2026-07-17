package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatbarInitialState;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyNavigateResult;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebQuotedMessageUserJourneyWamEvent")
@WamEvent(id = 6444)
public interface QuotedMessageUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ChatbarInitialState> chatbarInitialState();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MediaType> quotedMediaType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MessageType> quotedMessageTypeEnum();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<QuotedMessageUserJourneyAction> quotedMessageUserJourneyAction();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<QuotedMessageUserJourneyEntryPoint> quotedMessageUserJourneyEntryPoint();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<QuotedMessageUserJourneyNavigateResult> quotedMessageUserJourneyNavigateResult();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<UserJourneyChatType> userJourneyChatType();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
