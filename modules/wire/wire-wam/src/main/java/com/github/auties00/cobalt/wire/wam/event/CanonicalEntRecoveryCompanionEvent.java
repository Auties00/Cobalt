package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CanonicalEntEventMarker;
import com.github.auties00.cobalt.wire.wam.type.CanonicalEntRecoveryCompanionEventType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCanonicalEntRecoveryCompanionWamEvent")
@WamEvent(id = 7434)
public interface CanonicalEntRecoveryCompanionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CanonicalEntEventMarker> canonicalEntEventCompanionMarker();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> canonicalEntFeatureName();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CanonicalEntRecoveryCompanionEventType> canonicalEntRecoveryCompanionEvent();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> canonicalEntRecoveryEventMetadata();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong canonicalEntRecoveryTimeoutSeconds();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> canonicalEntRegistrationTraceId();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> canonicalEntRequestId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong canonicalEntSequenceNumberSinceLastRegistration();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> canonicalEntStorageSource();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> deviceId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> familyDeviceId();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong traceIdInt();
}
