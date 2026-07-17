package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumHatchActionType")
@WamEnum
public enum HatchActionType {
    @WamEnumConstant(1) REQUEST_WELCOME_MSG_SENT,
    @WamEnumConstant(2) TAP_UNLINK_BUTTON,
    @WamEnumConstant(3) UNLINK_SUCCESS,
    @WamEnumConstant(4) WA_READ_WRITE_ACCESS_IMPRESSION,
    @WamEnumConstant(5) TAP_WA_READ_WRITE_ACCESS,
    @WamEnumConstant(6) WA_READ_WRITE_ACCESS_LINK_SUCCESS
}
