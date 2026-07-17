package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAboutConsumptionSurfaceType")
@WamEnum
public enum AboutConsumptionSurfaceType {
    @WamEnumConstant(1) ONE_ON_ONE_CHAT,
    @WamEnumConstant(2) NEW_MESSAGE_CONTACTS,
    @WamEnumConstant(3) GROUP_MEMBERS_LIST,
    @WamEnumConstant(4) COMMUNITY_MEMBERS_LIST,
    @WamEnumConstant(5) FORWARD_CONTACTS,
    @WamEnumConstant(6) PROFILE_INFO_BOTTOM_SHEET,
    @WamEnumConstant(7) PROFILE_INFO
}
