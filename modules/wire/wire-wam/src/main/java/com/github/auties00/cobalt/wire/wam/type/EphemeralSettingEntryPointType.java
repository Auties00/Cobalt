package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEphemeralSettingEntryPointType")
@WamEnum
public enum EphemeralSettingEntryPointType {
    @WamEnumConstant(1) CHAT_INFO,
    @WamEnumConstant(2) SYSTEM_MESSAGE,
    @WamEnumConstant(3) CHAT_OVERFLOW,
    @WamEnumConstant(4) CHAT_PICKER,
    @WamEnumConstant(5) EPHEMERAL_NUX,
    @WamEnumConstant(6) CHAT_PICKER_DISAPPEARING_MODE_TIMER,
    @WamEnumConstant(7) CHAT_PICKER_STORAGE_SETTING
}
