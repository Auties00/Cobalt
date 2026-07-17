package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPeerDataResponseResultType")
@WamEnum
public enum PeerDataResponseResultType {
    @WamEnumConstant(1) SUCCESS,
    @WamEnumConstant(2) OTHER_ERROR,
    @WamEnumConstant(3) REQUEST_TARGET_NOT_FOUND,
    @WamEnumConstant(4) REQUEST_INVALID,
    @WamEnumConstant(5) FAIL_TO_UPLOAD,
    @WamEnumConstant(6) FAIL_TO_SEND_RESPONSE,
    @WamEnumConstant(7) REQUEST_TOO_OLD
}
