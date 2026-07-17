package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CertVerificationResultType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCertificateValidationEventWamEvent")
@WamEvent(id = 7120)
public interface CertificateValidationEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong certChainLength();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CertVerificationResultType> certVerificationResult();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> leafCertCommonName();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> leafCertId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong leafCertTtlDays();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> rawErrorCode();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> signatureVersion();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> verificationLatency();
}
