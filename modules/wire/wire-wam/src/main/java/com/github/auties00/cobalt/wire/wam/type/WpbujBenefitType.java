package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWpbujBenefitType")
@WamEnum
public enum WpbujBenefitType {
    @WamEnumConstant(1) APP_THEMES,
    @WamEnumConstant(2) APP_ICONS,
    @WamEnumConstant(3) RINGTONES,
    @WamEnumConstant(4) PINNED_CHATS,
    @WamEnumConstant(5) STICKERS,
    @WamEnumConstant(6) LISTS,
    @WamEnumConstant(7) NOT_APPLICABLE,
    @WamEnumConstant(8) CLOUD_STORAGE
}
