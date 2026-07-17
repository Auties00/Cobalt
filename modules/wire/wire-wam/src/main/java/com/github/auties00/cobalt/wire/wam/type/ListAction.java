package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumListAction")
@WamEnum
public enum ListAction {
    @WamEnumConstant(1) CREATE,
    @WamEnumConstant(2) DELETE,
    @WamEnumConstant(3) UNDELETE,
    @WamEnumConstant(4) RENAME,
    @WamEnumConstant(5) UPDATE_MEMBERS,
    @WamEnumConstant(6) MUTE,
    @WamEnumConstant(7) UNMUTE,
    @WamEnumConstant(8) HIDE,
    @WamEnumConstant(9) UNHIDE
}
