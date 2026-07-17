package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWaCallingHistoryGroupCallRecordSaveConditionCheckStatus")
@WamEnum
public enum WaCallingHistoryGroupCallRecordSaveConditionCheckStatus {
    @WamEnumConstant(0) INVALID,
    @WamEnumConstant(1) NOT_GROUP_CALL,
    @WamEnumConstant(2) NOT_START_FROM_VIDEO_CALL,
    @WamEnumConstant(3) HAS_AV_SWITCH,
    @WamEnumConstant(4) SELF_CAMERA_OFF,
    @WamEnumConstant(5) PEER_CAMERA_OFF,
    @WamEnumConstant(6) SELF_HAS_NETWORK_TRANSITION,
    @WamEnumConstant(7) PEER_HAS_NETWORK_TRANSITION,
    @WamEnumConstant(8) CALL_TOO_SHORT,
    @WamEnumConstant(100) PASS
}
