package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChannelEventType")
@WamEnum
public enum ChannelEventType {
    @WamEnumConstant(1) FOLLOW,
    @WamEnumConstant(2) UNFOLLOW,
    @WamEnumConstant(3) MUTE,
    @WamEnumConstant(4) UNMUTE,
    @WamEnumConstant(5) PREMIUM_SUBSCRIBE,
    @WamEnumConstant(6) PREMIUM_UNSUBSCRIBE,
    @WamEnumConstant(7) CHANNEL_PREMIUM_SETUP,
    @WamEnumConstant(8) CHANNEL_PREMIUM_CANCEL,
    @WamEnumConstant(9) HIDE,
    @WamEnumConstant(10) UNHIDE,
    @WamEnumConstant(11) CREATE,
    @WamEnumConstant(12) DELETE,
    @WamEnumConstant(13) ADMIN_PROFILE_CREATE,
    @WamEnumConstant(14) ADMIN_PROFILE_UPDATE,
    @WamEnumConstant(15) ADMIN_PROFILE_DELETE,
    @WamEnumConstant(16) ADMIN_PROFILE_SETTING_ENABLE,
    @WamEnumConstant(17) ADMIN_PROFILE_SETTING_DISABLE
}
