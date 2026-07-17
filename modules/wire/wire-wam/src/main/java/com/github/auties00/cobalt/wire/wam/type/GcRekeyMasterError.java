package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGcRekeyMasterError")
@WamEnum
public enum GcRekeyMasterError {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) FAILED_TO_GENERATE_NEW_E2EE_KEYS,
    @WamEnumConstant(2) FAILED_TO_UPDATE_PARTICIPANT_KEYS
}
