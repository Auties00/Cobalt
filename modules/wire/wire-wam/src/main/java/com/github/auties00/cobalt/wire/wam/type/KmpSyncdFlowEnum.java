package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKmpSyncdFlowEnum")
@WamEnum
public enum KmpSyncdFlowEnum {
    @WamEnumConstant(0) KMP_ENCRYPTION,
    @WamEnumConstant(1) KMP_DECRYPTION,
    @WamEnumConstant(2) KMP_OUTGOING_PROCESSOR,
    @WamEnumConstant(3) KMP_INCOMING_PROCESSOR,
    @WamEnumConstant(4) NONE
}
