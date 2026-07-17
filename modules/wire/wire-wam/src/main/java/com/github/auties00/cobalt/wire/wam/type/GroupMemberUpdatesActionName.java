package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupMemberUpdatesActionName")
@WamEnum
public enum GroupMemberUpdatesActionName {
    @WamEnumConstant(0) VIEW,
    @WamEnumConstant(1) CLICK_PAST_MEMBER_UPDATE,
    @WamEnumConstant(2) CLICK_USERNAME_UPDATE,
    @WamEnumConstant(3) CLICK_INFO_OPTION,
    @WamEnumConstant(4) CLICK_MESSAGE_OPTION,
    @WamEnumConstant(5) CLICK_AUDIO_OPTION,
    @WamEnumConstant(6) CLICK_VIDEO_OPTION,
    @WamEnumConstant(7) CLICK_USERNAME_UPSELL_OPTION,
    @WamEnumConstant(8) CLICK_EDIT_CONTACT_INFO,
    @WamEnumConstant(9) FETCH_MEMBER_UPDATES_SUCCESS,
    @WamEnumConstant(10) FETCH_MEMBER_UPDATES_FAILURE,
    @WamEnumConstant(11) FETCH_MEMBER_UPDATES_EMPTY,
    @WamEnumConstant(12) VIEW_USERNAME_UPSELL_BUTTON,
    @WamEnumConstant(13) CLICK_ADD_TO_CONTACTS
}
