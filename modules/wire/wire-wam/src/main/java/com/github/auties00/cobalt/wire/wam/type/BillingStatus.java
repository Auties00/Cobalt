package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBillingStatus")
@WamEnum
public enum BillingStatus {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) NO_ACTION_REQUIRED,
    @WamEnumConstant(3) HAS_PENDING_ACTIONS
}
