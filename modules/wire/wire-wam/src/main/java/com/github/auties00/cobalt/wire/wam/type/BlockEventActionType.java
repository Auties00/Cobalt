package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBlockEventActionType")
@WamEnum
public enum BlockEventActionType {
    @WamEnumConstant(0) BLOCK,
    @WamEnumConstant(1) UNBLOCK
}
