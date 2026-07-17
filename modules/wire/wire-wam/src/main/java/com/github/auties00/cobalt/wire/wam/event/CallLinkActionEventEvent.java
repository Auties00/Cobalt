package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CallLinkAction;
import com.github.auties00.cobalt.wire.wam.type.CallLinkActionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.CallLinkMedia;
import com.github.auties00.cobalt.wire.wam.type.CallLinkShareChatType;
import com.github.auties00.cobalt.wire.wam.type.CallLinkType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCallLinkActionEventWamEvent")
@WamEvent(id = 3852)
public interface CallLinkActionEventEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CallLinkAction> callLinkAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CallLinkActionEntryPoint> callLinkActionEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<CallLinkMedia> callLinkMedia();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<CallLinkShareChatType> callLinkShareChatType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> callLinkSharedApp();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<CallLinkType> callLinkType();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isWaitingRoomEnabled();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
