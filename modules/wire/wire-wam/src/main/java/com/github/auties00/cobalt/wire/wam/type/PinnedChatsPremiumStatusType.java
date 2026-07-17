package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPinnedChatsPremiumStatusType")
@WamEnum
public enum PinnedChatsPremiumStatusType {
    @WamEnumConstant(0) DISABLED,
    @WamEnumConstant(1) ENABLED,
    @WamEnumConstant(2) ACTIVE
}
