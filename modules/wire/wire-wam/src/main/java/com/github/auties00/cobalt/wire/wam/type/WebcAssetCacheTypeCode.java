package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcAssetCacheTypeCode")
@WamEnum
public enum WebcAssetCacheTypeCode {
    @WamEnumConstant(0) UNCACHED,
    @WamEnumConstant(1) IDB,
    @WamEnumConstant(2) SW
}
