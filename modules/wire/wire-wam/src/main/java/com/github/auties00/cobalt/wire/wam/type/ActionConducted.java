package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumActionConducted")
@WamEnum
public enum ActionConducted {
    @WamEnumConstant(1) MUTE,
    @WamEnumConstant(2) UNMUTE,
    @WamEnumConstant(3) EXPIRE
}
