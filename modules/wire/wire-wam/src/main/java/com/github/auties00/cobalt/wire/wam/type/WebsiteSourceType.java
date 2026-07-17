package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebsiteSourceType")
@WamEnum
public enum WebsiteSourceType {
    @WamEnumConstant(1) SOURCE_OTHER,
    @WamEnumConstant(2) SOURCE_INSTAGRAM
}
