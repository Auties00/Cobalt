package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeleteActionType")
@WamEnum
public enum DeleteActionType {
    @WamEnumConstant(0) DELETE_FOR_ME,
    @WamEnumConstant(1) DELETE_FOR_EVERYONE
}
