package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRingtoneEntryType")
@WamEnum
public enum RingtoneEntryType {
    @WamEnumConstant(0) APP_WIDE,
    @WamEnumConstant(1) ONE_TO_ONE,
    @WamEnumConstant(2) GROUP,
    @WamEnumConstant(3) LIST
}
