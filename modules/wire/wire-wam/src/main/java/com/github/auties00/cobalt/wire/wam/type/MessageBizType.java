package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageBizType")
@WamEnum
public enum MessageBizType {
    @WamEnumConstant(0) NOT_BIZ_MSG,
    @WamEnumConstant(1) API_MARKETING,
    @WamEnumConstant(2) API_UTILITY,
    @WamEnumConstant(3) OTHER_API_BIZ_MSG,
    @WamEnumConstant(4) SMB_BIZ_MSG
}
