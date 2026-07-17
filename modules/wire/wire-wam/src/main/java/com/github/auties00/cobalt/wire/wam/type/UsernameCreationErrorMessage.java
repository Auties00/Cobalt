package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUsernameCreationErrorMessage")
@WamEnum
public enum UsernameCreationErrorMessage {
    @WamEnumConstant(1) REQUIRES_FB_LINKING,
    @WamEnumConstant(2) REQUIRES_IG_LINKING,
    @WamEnumConstant(3) REQUIRES_FB_IG_LINKING
}
