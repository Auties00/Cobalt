package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumImplementationType")
@WamEnum
public enum ImplementationType {
    @WamEnumConstant(0) NATIVE,
    @WamEnumConstant(1) GENAI
}
