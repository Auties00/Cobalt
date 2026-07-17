package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOfflineResumeResultType")
@WamEnum
public enum OfflineResumeResultType {
    @WamEnumConstant(1) COMPLETE,
    @WamEnumConstant(2) INCOMPLETE_UNKNOWN_ERROR,
    @WamEnumConstant(3) INCOMPLETE_DISCONNECT,
    @WamEnumConstant(4) INCOMPLETE_APP_RESTART
}
