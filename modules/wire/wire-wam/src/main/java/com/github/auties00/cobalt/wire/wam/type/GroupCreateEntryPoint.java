package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupCreateEntryPoint")
@WamEnum
public enum GroupCreateEntryPoint {
    @WamEnumConstant(1) BANNER,
    @WamEnumConstant(2) MENU,
    @WamEnumConstant(3) CONTACTS,
    @WamEnumConstant(4) MULTISELECT,
    @WamEnumConstant(5) CHAT_UI_MENU,
    @WamEnumConstant(6) CONTACT_INFO_OVERFLOW,
    @WamEnumConstant(7) CONTACT_INFO_GROUPS_IN_COMMON,
    @WamEnumConstant(8) CHATS,
    @WamEnumConstant(9) COMMUNITY_HOME,
    @WamEnumConstant(10) COMMUNITY_SUBGROUP_SWITCHER,
    @WamEnumConstant(11) XFAMILY_GROUP_PICKER,
    @WamEnumConstant(12) NEW_CHAT,
    @WamEnumConstant(13) LINK_EXISTING_GROUP,
    @WamEnumConstant(14) COMMUNITY_GROUP_MANAGEMENT,
    @WamEnumConstant(15) GROUP_MEMBER_SELECT,
    @WamEnumConstant(16) FORWARD,
    @WamEnumConstant(17) SHARE,
    @WamEnumConstant(18) CONVERSATION_OVERFLOW,
    @WamEnumConstant(19) GROUPS_FILTER_EMPTY_CTA,
    @WamEnumConstant(20) SEARCH,
    @WamEnumConstant(21) NON_CONTACT_INFO_GROUPS_IN_COMMON,
    @WamEnumConstant(22) META_AI_NULL_STATE_MENU,
    @WamEnumConstant(23) META_AI_NULL_STATE_CAPABILITY,
    @WamEnumConstant(24) GROUP_AI_DEEPLINK,
    @WamEnumConstant(25) GROUP_FROM_GROUP_ROW,
    @WamEnumConstant(26) GROUP_FROM_GROUP_OVERFLOW_MENU,
    @WamEnumConstant(27) GROUP_MENTION_PICKER,
    @WamEnumConstant(28) THREE_P_DEEPLINK,
    @WamEnumConstant(29) CHAT_MENTION_PICKER,
    @WamEnumConstant(30) POLLS,
    @WamEnumConstant(31) LINKED_GROUP_CALL
}
