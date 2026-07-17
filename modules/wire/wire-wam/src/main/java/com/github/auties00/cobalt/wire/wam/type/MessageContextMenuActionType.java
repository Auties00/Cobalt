package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageContextMenuActionType")
@WamEnum
public enum MessageContextMenuActionType {
    @WamEnumConstant(1) OPEN,
    @WamEnumConstant(2) CLICK,
    @WamEnumConstant(3) COMPLETE,
    @WamEnumConstant(4) CANCEL
}
