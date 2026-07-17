package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupMemberTagEntryPointType")
@WamEnum
public enum GroupMemberTagEntryPointType {
    @WamEnumConstant(1) MEMBER_LIST,
    @WamEnumConstant(2) NEW_MEMBER_PROMPT,
    @WamEnumConstant(3) CONTACT_CARD,
    @WamEnumConstant(4) SNACKBAR_EDIT,
    @WamEnumConstant(5) OTHER
}
