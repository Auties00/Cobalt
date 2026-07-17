package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeleteSuspendedGroupBtn")
@WamEnum
public enum DeleteSuspendedGroupBtn {
    @WamEnumConstant(1) BOTTOM_SHEET_BTN,
    @WamEnumConstant(2) BLOCKED_COMPOSER_BTN
}
