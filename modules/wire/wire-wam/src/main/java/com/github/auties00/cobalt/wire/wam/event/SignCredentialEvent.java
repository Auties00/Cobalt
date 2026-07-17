package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.ProjectCode;
import com.github.auties00.cobalt.wire.wam.type.SignCredentialResult;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSignCredentialWamEvent")
@WamEvent(id = 2242)
public interface SignCredentialEvent extends WamEventSpec {
    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ApplicationState> applicationState();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWameta();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> overallT();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ProjectCode> projectCode();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SignCredentialResult> signCredentialResult();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> signCredentialT();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> waConnectedToChatd();
}
