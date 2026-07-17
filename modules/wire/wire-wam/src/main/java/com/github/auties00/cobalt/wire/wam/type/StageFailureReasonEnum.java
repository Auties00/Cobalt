package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStageFailureReasonEnum")
@WamEnum
public enum StageFailureReasonEnum {
    @WamEnumConstant(1) MALFORMED_PEER_MESSAGE,
    @WamEnumConstant(2) INITIATED_LOGOUT_BASED_ON_MAPPING,
    @WamEnumConstant(3) DID_NOT_GET_PEER_MESSAGE_ON_TIME,
    @WamEnumConstant(4) DID_NOT_COMPLETE_MIGRATION_ON_TIME,
    @WamEnumConstant(5) COMPANION_UNSUPPORTED_VERSION,
    @WamEnumConstant(6) INTERNAL_ERROR,
    @WamEnumConstant(7) COMPANION_TIMEOUT_BASED_ON_DEVICE_CAPABILITY
}
