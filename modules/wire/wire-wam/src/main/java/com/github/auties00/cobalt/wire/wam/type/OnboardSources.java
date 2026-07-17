package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOnboardSources")
@WamEnum
public enum OnboardSources {
    @WamEnumConstant(1) DEFAULT,
    @WamEnumConstant(2) BACKGROUND_SYNC_BUTTERBAR,
    @WamEnumConstant(3) WEB_NOTIFICATION_BUTTERBAR,
    @WamEnumConstant(4) IN_APP_SETTING,
    @WamEnumConstant(5) AUTO_ENABLE
}
