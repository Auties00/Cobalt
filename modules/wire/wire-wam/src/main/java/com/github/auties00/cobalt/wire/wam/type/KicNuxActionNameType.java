package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKicNuxActionNameType")
@WamEnum
public enum KicNuxActionNameType {
    @WamEnumConstant(1) FIRST_DM_NUX_IMPRESSION,
    @WamEnumConstant(2) KIC_NUX_IMPRESSION,
    @WamEnumConstant(3) KIC_NUX_LEARN_MORE_TAP,
    @WamEnumConstant(4) KIC_SYSTEM_MESSAGE_GENERATE
}
