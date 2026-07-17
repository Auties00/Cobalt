package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PhoneNumHyperlinkActionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsPhoneNumberHyperlinkWamEvent")
@WamEvent(id = 3266, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface PsPhoneNumberHyperlinkEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isPhoneNumHyperlinkOwner();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PhoneNumHyperlinkActionType> phoneNumHyperlinkAction();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> phoneNumberStatusOnWa();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong sequenceNumber();
}
