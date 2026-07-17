package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsUploadReason")
@WamEnum
public enum PsUploadReason {
    @WamEnumConstant(0) REASON_PS_PINGER,
    @WamEnumConstant(1) REASON_PS_OFFCYCLE
}
