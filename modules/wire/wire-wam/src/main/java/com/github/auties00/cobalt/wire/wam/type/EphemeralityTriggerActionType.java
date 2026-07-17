package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEphemeralityTriggerActionType")
@WamEnum
public enum EphemeralityTriggerActionType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) CHAT_SETTINGS,
    @WamEnumConstant(2) ACCOUNT_SETTINGS,
    @WamEnumConstant(3) BULK_CHANGE,
    @WamEnumConstant(4) BIZ_SUPPORTS_FB_HOSTING,
    @WamEnumConstant(5) UNKNOWN_GROUP
}
