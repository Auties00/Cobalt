package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVoipSettingReleaseType")
@WamEnum
public enum VoipSettingReleaseType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) PROD,
    @WamEnumConstant(2) FALLBACK,
    @WamEnumConstant(3) CANARY
}
