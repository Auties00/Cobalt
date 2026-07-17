package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttPlaybackSpeedType")
@WamEnum
public enum PttPlaybackSpeedType {
    @WamEnumConstant(0) SPEED_1,
    @WamEnumConstant(1) SPEED_1_5,
    @WamEnumConstant(2) SPEED_2
}
