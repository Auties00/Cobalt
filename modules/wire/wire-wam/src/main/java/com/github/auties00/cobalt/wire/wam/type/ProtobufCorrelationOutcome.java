package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumProtobufCorrelationOutcome")
@WamEnum
public enum ProtobufCorrelationOutcome {
    @WamEnumConstant(0) LEGACY_NO_INTERACTION,
    @WamEnumConstant(1) LEGACY_REJECTED,
    @WamEnumConstant(2) LEGACY_ACCEPTED
}
