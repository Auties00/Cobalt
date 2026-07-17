package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtwaLabelTarget")
@WamEnum
public enum CtwaLabelTarget {
    @WamEnumConstant(0) CHAT,
    @WamEnumConstant(1) MESSAGE
}
