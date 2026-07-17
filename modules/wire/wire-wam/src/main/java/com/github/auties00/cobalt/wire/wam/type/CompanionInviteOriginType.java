package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionInviteOriginType")
@WamEnum
public enum CompanionInviteOriginType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) CHATLIST_SEARCH,
    @WamEnumConstant(2) CONTACT_PICKER_LIST,
    @WamEnumConstant(3) CONTACT_PICKER_SEARCH,
    @WamEnumConstant(4) GROUPS_CREATE_PARTICIPANT_SELECTOR,
    @WamEnumConstant(5) GROUPS_ADD_PARTICIPANT_SELECTOR,
    @WamEnumConstant(6) CONTACT_EDIT_DRAWER
}
