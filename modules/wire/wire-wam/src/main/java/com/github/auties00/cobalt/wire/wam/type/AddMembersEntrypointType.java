package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAddMembersEntrypointType")
@WamEnum
public enum AddMembersEntrypointType {
    @WamEnumConstant(0) GROUP_INFO_ACTION_BUTTON,
    @WamEnumConstant(1) GROUP_INFO_CONTEXT_MENU,
    @WamEnumConstant(2) GROUP_INFO_MEMBERS_LIST_ITEM,
    @WamEnumConstant(3) GROUP_CREATED_CONTEXT_CARD,
    @WamEnumConstant(4) GROUP_ADDED_CONTEXT_CARD,
    @WamEnumConstant(5) GROUP_USER_JOINED_BY_LINK_CONTEXT_CARD,
    @WamEnumConstant(6) GROUP_ADD_MEMBER_ENTRY_POINT_OTHER,
    @WamEnumConstant(7) GROUP_CONVERSATION_OVERFLOW_MENU,
    @WamEnumConstant(8) GROUP_ADD_MEMBER_FROM_SHARED_CONTACT,
    @WamEnumConstant(9) GROUP_ADD_MEMBER_FROM_EMPTY_GROUP_BANNER,
    @WamEnumConstant(10) GROUP_ADD_MEMBER_FROM_SHARED_NON_CONTACT,
    @WamEnumConstant(11) GROUP_SAVE_CONTACT_FROM_SHARED_NON_CONTACT,
    @WamEnumConstant(12) GROUP_ADD_MEMBER_FROM_CHAT_HEADER,
    @WamEnumConstant(13) GROUP_MENTION_PICKER,
    @WamEnumConstant(14) COMMUNITY_CONTEXT_CARD,
    @WamEnumConstant(15) COMMUNITY_EMPTY_SUBGROUP,
    @WamEnumConstant(16) GROUP_MEMBERS_LIST_ADD_BUTTON
}
