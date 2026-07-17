package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDisappearingChatInitiatorType")
@WamEnum
public enum DisappearingChatInitiatorType {
    @WamEnumConstant(1) CHAT,
    @WamEnumConstant(2) INITIATED_BY_ME,
    @WamEnumConstant(3) INITIATED_BY_OTHER,
    @WamEnumConstant(4) CHAT_PICKER,
    @WamEnumConstant(5) BIZ_UPGRADE_FB_HOSTING
}
