package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtaType")
@WamEnum
public enum CtaType {
    @WamEnumConstant(0) COPY_CODE,
    @WamEnumConstant(1) AUTOFILL
}
