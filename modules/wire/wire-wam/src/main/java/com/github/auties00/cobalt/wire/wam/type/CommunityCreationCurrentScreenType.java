package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCommunityCreationCurrentScreenType")
@WamEnum
public enum CommunityCreationCurrentScreenType {
    @WamEnumConstant(1) CHATS_TAB,
    @WamEnumConstant(2) COMMUNITIES_TAB,
    @WamEnumConstant(3) COMMUNITY_NUX,
    @WamEnumConstant(4) COMMUNITY_INFO,
    @WamEnumConstant(5) COMMUNITY_GROUPS_SUMMARY,
    @WamEnumConstant(6) DEEP_LINK,
    @WamEnumConstant(7) BANNER,
    @WamEnumConstant(8) GROUP_INFO,
    @WamEnumConstant(9) LINK_GROUP_CONFIRMATION,
    @WamEnumConstant(10) GROUP_INFO_NEW_OR_EXISTING_COMMUNITY,
    @WamEnumConstant(11) ADD_GROUP_TO_EXISTING_COMMUNITY,
    @WamEnumConstant(12) ADD_GROUP_TO_EXISTING_COMMUNITY_AS_CADMIN_CONFIRMATION,
    @WamEnumConstant(14) SUGGEST_GROUP_TO_EXISTING_COMMUNITY_CONFIRMATION
}
