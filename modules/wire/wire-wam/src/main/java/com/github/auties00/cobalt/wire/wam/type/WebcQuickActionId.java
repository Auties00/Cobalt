package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcQuickActionId")
@WamEnum
public enum WebcQuickActionId {
    @WamEnumConstant(1) TEXT_STATUS,
    @WamEnumConstant(2) PHOTO_VIDEO,
    @WamEnumConstant(3) CREATE_CHANNEL,
    @WamEnumConstant(4) FIND_CHANNELS,
    @WamEnumConstant(5) COMMUNITY_INFO,
    @WamEnumConstant(6) COMMUNITY_MEMBERS,
    @WamEnumConstant(7) COMMUNITY_SETTINGS,
    @WamEnumConstant(8) COMMUNITY_NEW_GROUP,
    @WamEnumConstant(9) COMMUNITY_EXISTING_GROUP,
    @WamEnumConstant(10) SEND_DOCUMENT,
    @WamEnumConstant(11) ADD_CONTACT,
    @WamEnumConstant(12) ASK_META_AI
}
