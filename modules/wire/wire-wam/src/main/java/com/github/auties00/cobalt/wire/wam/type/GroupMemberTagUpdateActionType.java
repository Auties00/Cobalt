package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupMemberTagUpdateActionType")
@WamEnum
public enum GroupMemberTagUpdateActionType {
    @WamEnumConstant(1) UPDATE,
    @WamEnumConstant(2) DELETE_CONFIRM,
    @WamEnumConstant(3) ERROR
}
