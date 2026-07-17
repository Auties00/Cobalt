package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DmChatPickerEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.DmChatPickerEventNameType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDisappearingMessageChatPickerWamEvent")
@WamEvent(id = 3398)
public interface DisappearingMessageChatPickerEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatsSelected();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DmChatPickerEntryPointType> dmChatPickerEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<DmChatPickerEventNameType> dmChatPickerEventName();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong ephemeralityDuration();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong groupChatsSelected();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> groupSizeDistributionJson();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong newlyEphemeralChats();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong totalChatsInChatPicker();
}
