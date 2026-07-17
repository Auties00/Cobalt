package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumErrorType")
@WamEnum
public enum ErrorType {
    @WamEnumConstant(0) ERROR_FETCHING_AGENT_NAME,
    @WamEnumConstant(1) ERROR_FETCHING_CHAT,
    @WamEnumConstant(2) ERROR_OTHER
}
