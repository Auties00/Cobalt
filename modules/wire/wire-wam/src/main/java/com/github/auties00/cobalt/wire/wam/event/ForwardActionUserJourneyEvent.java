package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ForwardActionUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebForwardActionUserJourneyWamEvent")
@WamEvent(id = 6506)
public interface ForwardActionUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ForwardActionUserJourneyAction> forwardActionUserJourneyAction();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> forwardUserJourneyFunnelId();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFromMe();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<UserJourneyChatType> userJourneyChatType();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
