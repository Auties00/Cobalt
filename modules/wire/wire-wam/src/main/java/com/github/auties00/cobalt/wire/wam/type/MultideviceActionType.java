package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMultideviceActionType")
@WamEnum
public enum MultideviceActionType {
    @WamEnumConstant(0) LOGIN,
    @WamEnumConstant(1) LOGOUT,
    @WamEnumConstant(2) CUSTOM_AGENT_NAME,
    @WamEnumConstant(3) MESSAGE_INFO,
    @WamEnumConstant(4) ACTIVE,
    @WamEnumConstant(5) DELETE
}
