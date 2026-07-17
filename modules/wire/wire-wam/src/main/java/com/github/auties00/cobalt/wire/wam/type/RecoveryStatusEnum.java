package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRecoveryStatusEnum")
@WamEnum
public enum RecoveryStatusEnum {
    @WamEnumConstant(1) PRIMARY_UNSUPPORTED,
    @WamEnumConstant(2) ABPROP_OFF,
    @WamEnumConstant(3) NOT_FATAL,
    @WamEnumConstant(4) COLLECTION_UNSUPPORTED,
    @WamEnumConstant(5) MUTATION_COUNT_TOO_HIGH,
    @WamEnumConstant(6) PRIMARY_DID_NOT_RESPOND
}
