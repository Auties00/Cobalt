package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAfterReadScreenEntryPointType")
@WamEnum
public enum AfterReadScreenEntryPointType {
    @WamEnumConstant(0) STORAGE_SETTING,
    @WamEnumConstant(1) PRIVACY,
    @WamEnumConstant(2) DM_TIMER_SCREEN,
    @WamEnumConstant(3) CHAT_PICKER_SCREEN,
    @WamEnumConstant(4) CHAT_ENTRY,
    @WamEnumConstant(5) GROUP_CREATION
}
