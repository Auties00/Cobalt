package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MdAppStateKeyRotationReasonCode;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdAppStateKeyRotationWamEvent")
@WamEvent(id = 2518, betaWeight = 20, releaseWeight = 1000)
public interface MdAppStateKeyRotationEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> companionSessionIds();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MdAppStateKeyRotationReasonCode> mdAppStateKeyRotationReason();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> oldSyncdKeyhash();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> oldSyncdKeyid();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong seqNumber();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> syncdKeyhashAfterRotation();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> syncdKeyidAfterRotation();
}
