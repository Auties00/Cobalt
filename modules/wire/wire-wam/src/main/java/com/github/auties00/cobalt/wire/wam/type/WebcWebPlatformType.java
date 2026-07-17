package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcWebPlatformType")
@WamEnum
public enum WebcWebPlatformType {
    @WamEnumConstant(1) WEB,
    @WamEnumConstant(2) WIN32,
    @WamEnumConstant(3) DARWIN,
    @WamEnumConstant(4) IOS_TABLET,
    @WamEnumConstant(5) ANDROID_TABLET,
    @WamEnumConstant(6) WINSTORE,
    @WamEnumConstant(7) MACSTORE,
    @WamEnumConstant(8) DARWIN_BETA,
    @WamEnumConstant(9) WIN32_BETA,
    @WamEnumConstant(10) PWA,
    @WamEnumConstant(11) WIN_HYBRID
}
