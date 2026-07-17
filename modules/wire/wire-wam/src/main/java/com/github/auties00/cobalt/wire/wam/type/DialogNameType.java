package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDialogNameType")
@WamEnum
public enum DialogNameType {
    @WamEnumConstant(1) LOGOUT,
    @WamEnumConstant(2) APP_LOCK_ENABLE,
    @WamEnumConstant(3) APP_LOCK_DISABLE,
    @WamEnumConstant(4) APP_LOCK_ENABLED_CONFIRM,
    @WamEnumConstant(5) HARD_REFRESH
}
