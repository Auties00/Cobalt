package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatMuteNotificationChoice")
@WamEnum
public enum ChatMuteNotificationChoice {
    @WamEnumConstant(1) NO_NOTIFICATIONS_WHEN_MUTED,
    @WamEnumConstant(2) YES_NOTIFICATIONS_WHEN_MUTED
}
