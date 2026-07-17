package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupHistoryReceiverUserJourneyActionType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebGroupHistoryReceiverUserJourneyWamEvent")
@WamEvent(id = 7064)
public interface GroupHistoryReceiverUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> groupHistoryDbIgnoredOlderMessages();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong groupHistoryMessagesCount();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong groupHistoryOutWindowPinsCount();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong groupHistoryPinsCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupHistoryReceiverUserJourneyActionType> groupHistoryReceiverActionType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> groupHistoryReceiverGroupId();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong groupHistoryUncountedMessagesCount();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isAutoProcess();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> messageKeyHash();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong messageReceivedTs();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> receiverFailureReason();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<TsSurface> uiSurface();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong userJourneyMs();
}
