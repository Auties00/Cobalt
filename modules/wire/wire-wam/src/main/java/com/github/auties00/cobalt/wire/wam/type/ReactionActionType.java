package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumReactionActionType")
@WamEnum
public enum ReactionActionType {
    @WamEnumConstant(1) OPEN_TRAY,
    @WamEnumConstant(2) DELETE,
    @WamEnumConstant(3) UPDATE
}
