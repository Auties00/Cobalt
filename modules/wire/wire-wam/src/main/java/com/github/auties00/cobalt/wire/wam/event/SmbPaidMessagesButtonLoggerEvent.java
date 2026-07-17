package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PmButtonEventType;
import com.github.auties00.cobalt.wire.wam.type.PmButtonType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSmbPaidMessagesButtonLoggerWamEvent")
@WamEvent(id = 4508, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface SmbPaidMessagesButtonLoggerEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong businessPhoneNumber();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong pmButtonCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PmButtonEventType> pmButtonEventType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong pmButtonIndex();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PmButtonType> pmButtonType();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> pmIsTrackableLink();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> pmServerCampaignId();
}
