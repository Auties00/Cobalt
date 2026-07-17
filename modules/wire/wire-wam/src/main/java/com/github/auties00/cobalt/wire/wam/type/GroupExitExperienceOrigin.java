package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupExitExperienceOrigin")
@WamEnum
public enum GroupExitExperienceOrigin {
    @WamEnumConstant(0) CHAT_LIST_LONG_PRESS,
    @WamEnumConstant(1) CHAT_OVERFLOW_MENU,
    @WamEnumConstant(2) GROUP_INFO,
    @WamEnumConstant(3) FGX_CARD,
    @WamEnumConstant(4) SUSPICIOUS_CHAT_BANNER,
    @WamEnumConstant(5) IOS_SWIPE_MENU,
    @WamEnumConstant(6) WEB_CONTEXT_MENU,
    @WamEnumConstant(7) ANDROID_MULTIPLE_CHAT_LONG_PRESS,
    @WamEnumConstant(8) GROUP_SAFETY_CHECK
}
