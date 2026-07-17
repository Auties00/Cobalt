package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SignupEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SignupUserJourneyOperation;
import com.github.auties00.cobalt.wire.wam.type.ThreadCreationTime;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebFsApiSignupFlowWamEvent")
@WamEvent(id = 7952)
public interface FsApiSignupFlowEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SignupEntryPoint> signupEntryPoint();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<SignupUserJourneyOperation> signupUserJourneyOperation();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ThreadCreationTime> threadCreationTime();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
