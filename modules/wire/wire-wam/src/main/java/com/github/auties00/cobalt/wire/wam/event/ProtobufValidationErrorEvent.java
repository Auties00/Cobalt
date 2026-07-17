package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ProtobufCorrelationOutcome;
import com.github.auties00.cobalt.wire.wam.type.ProtobufValidationFlow;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebProtobufValidationErrorWamEvent")
@WamEvent(id = 6110)
public interface ProtobufValidationErrorEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ProtobufCorrelationOutcome> protobufCorrelationOutcome();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> protobufLegacyValidationDropped();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong protobufValidationContext();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> protobufValidationDropped();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> protobufValidationErrorMessage();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> protobufValidationExpression();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ProtobufValidationFlow> protobufValidationFlow();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> protobufValidationPath();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> protobufValidationRuleId();
}
