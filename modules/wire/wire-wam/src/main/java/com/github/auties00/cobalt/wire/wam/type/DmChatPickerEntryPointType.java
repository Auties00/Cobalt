package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDmChatPickerEntryPointType")
@WamEnum
public enum DmChatPickerEntryPointType {
    @WamEnumConstant(0) DEFAULT_MODE_SETTING,
    @WamEnumConstant(1) STORAGE_SETTING,
    @WamEnumConstant(2) PRIVACY_SETTING
}
