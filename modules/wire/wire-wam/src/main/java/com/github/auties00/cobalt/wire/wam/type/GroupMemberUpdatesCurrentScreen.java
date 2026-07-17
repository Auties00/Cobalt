package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupMemberUpdatesCurrentScreen")
@WamEnum
public enum GroupMemberUpdatesCurrentScreen {
    @WamEnumConstant(0) GROUP_MEMBER_UPDATES_SCREEN,
    @WamEnumConstant(1) GROUP_MEMBER_CONTACT_INFO
}
