package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUnreadBadgeSettingType")
@WamEnum
public enum UnreadBadgeSettingType {
    @WamEnumConstant(0) CURRENT_UNREAD_COUNT,
    @WamEnumConstant(1) CLEAR_ON_APP_OPEN
}
