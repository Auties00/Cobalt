package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCanonicalEntRecoveryCriticalEventWamEvent")
@WamEvent(id = 7442)
public interface CanonicalEntRecoveryCriticalEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> canonicalEntRecoveryCriticalEventMetadata();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> canonicalEntRecoveryCriticalEventName();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> canonicalEntRegistrationTraceId();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> canonicalEntRequestId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong canonicalEntSequenceNumberSinceLastRegistration();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> deviceId();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> familyDeviceId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong traceIdInt();
}
