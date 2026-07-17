package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDefaultAudienceLocationType")
@WamEnum
public enum DefaultAudienceLocationType {
    @WamEnumConstant(1) CITY_LEVEL,
    @WamEnumConstant(2) COUNTRY_LEVEL
}
