package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdSyncdFatalErrorSource")
@WamEnum
public enum MdSyncdFatalErrorSource {
    @WamEnumConstant(1) SNAPSHOT,
    @WamEnumConstant(2) EXTERNAL_PATCH,
    @WamEnumConstant(3) INLINE_PATCH
}
