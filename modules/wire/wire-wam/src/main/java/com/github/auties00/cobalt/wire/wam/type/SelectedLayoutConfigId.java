package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSelectedLayoutConfigId")
@WamEnum
public enum SelectedLayoutConfigId {
    @WamEnumConstant(1) ONE_BY_TWO,
    @WamEnumConstant(2) ONE_AND_TWO,
    @WamEnumConstant(3) ONE_BY_THREE,
    @WamEnumConstant(4) TWO_BY_TWO,
    @WamEnumConstant(5) TWO_ONE_TWO,
    @WamEnumConstant(6) TWO_BY_THREE
}
