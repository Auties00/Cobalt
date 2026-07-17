package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCommunityCreationActionTakenType")
@WamEnum
public enum CommunityCreationActionTakenType {
    @WamEnumConstant(1) ENTER,
    @WamEnumConstant(2) GET_STARTED,
    @WamEnumConstant(3) DISMISS,
    @WamEnumConstant(4) NEXT,
    @WamEnumConstant(5) CREATE_GROUP,
    @WamEnumConstant(6) LINK_GROUP,
    @WamEnumConstant(7) CREATE_COMMUNITY,
    @WamEnumConstant(8) EXIT,
    @WamEnumConstant(9) UNLINK_GROUP,
    @WamEnumConstant(10) CREATE_COMMUNITY_SUCCESS,
    @WamEnumConstant(11) CREATE_COMMUNITY_FAIL,
    @WamEnumConstant(12) HELP_ICON_CLICK,
    @WamEnumConstant(13) LINK_GROUP_CONFIRMATION_OK,
    @WamEnumConstant(14) LINK_GROUP_CONFIRMATION_CANCEL,
    @WamEnumConstant(15) NEW_COMMUNITY,
    @WamEnumConstant(16) EXISTING_COMMUNITY,
    @WamEnumConstant(17) ADD_SUBGROUP_AS_CADMIN,
    @WamEnumConstant(19) SUGGEST_SUBGROUP_AS_MEMBER,
    @WamEnumConstant(20) CANCEL,
    @WamEnumConstant(21) ADD_GROUP_TO_EXISTING_COMMUNITY_FAIL,
    @WamEnumConstant(22) ADD_GROUP_TO_EXISTING_COMMUNITY_SUCCESS
}
