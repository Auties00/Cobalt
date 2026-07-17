package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoTranscoderAlgorithmType")
@WamEnum
public enum VideoTranscoderAlgorithmType {
    @WamEnumConstant(0) WA_IPHONE,
    @WamEnumConstant(1) FB_IPHONE,
    @WamEnumConstant(2) WASM_MP4_CHECK_AND_REPAIR,
    @WamEnumConstant(3) WEB_MEDIA_WORKER,
    @WamEnumConstant(4) HYBRID_BRIDGE
}
