package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttMessageUserJourneyStage")
@WamEnum
public enum PttMessageUserJourneyStage {
    @WamEnumConstant(1) NORMAL,
    @WamEnumConstant(2) LOCKED
}
