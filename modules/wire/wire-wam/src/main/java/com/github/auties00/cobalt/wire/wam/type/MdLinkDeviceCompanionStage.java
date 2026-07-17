package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMdLinkDeviceCompanionStage")
@WamEnum
public enum MdLinkDeviceCompanionStage {
    @WamEnumConstant(1) PAIR_SUCCESS_RECEIVED,
    @WamEnumConstant(2) PAIR_DEVICE_SIGN_SENT,
    @WamEnumConstant(3) FIRST_CONNECT,
    @WamEnumConstant(4) UPLOAD_PREKEYS,
    @WamEnumConstant(5) COMPLETE,
    @WamEnumConstant(6) GENERATE_PREKEYS,
    @WamEnumConstant(7) SENT_PREKEYS
}
