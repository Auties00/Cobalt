package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupBulkRemovalAction")
@WamEnum
public enum GroupBulkRemovalAction {
    @WamEnumConstant(1) TAP_SYSTEM_MESSAGE,
    @WamEnumConstant(2) TAP_REMOVE_BUTTON,
    @WamEnumConstant(3) TAP_CONFIRMATION_BUTTON
}
