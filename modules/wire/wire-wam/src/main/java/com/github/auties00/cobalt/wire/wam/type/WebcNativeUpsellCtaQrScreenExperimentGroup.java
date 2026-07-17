package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcNativeUpsellCtaQrScreenExperimentGroup")
@WamEnum
public enum WebcNativeUpsellCtaQrScreenExperimentGroup {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) TEST_BANNER_SLIM,
    @WamEnumConstant(3) TEST_BANNER_LARGE,
    @WamEnumConstant(4) CONTROL
}
