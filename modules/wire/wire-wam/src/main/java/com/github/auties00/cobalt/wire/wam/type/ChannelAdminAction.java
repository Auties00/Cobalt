package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelAdminAction")
@WamEnum
public enum ChannelAdminAction {
    @WamEnumConstant(1) CHANNEL_CREATION_TAP,
    @WamEnumConstant(2) CHANNEL_CREATE_LAUNCH_SUCCESS,
    @WamEnumConstant(3) CHANNEL_CREATE_LAUNCH_ERROR,
    @WamEnumConstant(4) CHANNEL_CREATE_LAUNCH_BLOCKED,
    @WamEnumConstant(5) EDIT_CHANNEL_TAP,
    @WamEnumConstant(6) CHANNEL_NAME_SET,
    @WamEnumConstant(7) CHANNEL_ICON_SET_CAMERA,
    @WamEnumConstant(8) CHANNEL_ICON_SET_GALLERY,
    @WamEnumConstant(9) CHANNEL_ICON_SET_EMOJI_STICKER,
    @WamEnumConstant(10) CHANNEL_ICON_SET_WEB,
    @WamEnumConstant(11) CHANNEL_DESCRIPTION_SET,
    @WamEnumConstant(12) CHANNEL_ADMIN_FLOW_CONFIRMATION_TAP,
    @WamEnumConstant(13) CHANNEL_ADMIN_FLOW_SUCCESS,
    @WamEnumConstant(14) CHANNEL_ADMIN_FLOW_FAILURE,
    @WamEnumConstant(15) SEARCH_FOLLOWER,
    @WamEnumConstant(16) REACTIONS_SET_TO_ANY_EMOJI,
    @WamEnumConstant(17) REACTIONS_SET_TO_DEFAULT_EMOJI,
    @WamEnumConstant(18) REACTIONS_SET_TO_NONE_EMOJI,
    @WamEnumConstant(19) INVITE_CONTACT_TO_FOLLOW_LAUNCH_SUCCESS
}
