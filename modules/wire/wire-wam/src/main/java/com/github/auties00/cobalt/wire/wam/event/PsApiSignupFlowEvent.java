package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SignupEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SignupUserJourneyOperation;
import com.github.auties00.cobalt.wire.wam.type.ThreadCreationTime;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsApiSignupFlowWamEvent")
@WamEvent(id = 7628, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface PsApiSignupFlowEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong businessLid();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong businessPhoneNumber();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> signupDeepLinkId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<SignupEntryPoint> signupEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<SignupUserJourneyOperation> signupUserJourneyOperation();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ThreadCreationTime> threadCreationTime();
}
