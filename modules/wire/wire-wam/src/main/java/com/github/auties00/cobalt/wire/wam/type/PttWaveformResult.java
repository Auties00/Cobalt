package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttWaveformResult")
@WamEnum
public enum PttWaveformResult {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) ALL_ZEROES,
    @WamEnumConstant(3) ALL_ONES,
    @WamEnumConstant(4) MISSING
}
