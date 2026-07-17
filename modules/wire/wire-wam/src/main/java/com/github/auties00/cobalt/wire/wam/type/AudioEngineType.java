package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudioEngineType")
@WamEnum
public enum AudioEngineType {
    @WamEnumConstant(1) JNI,
    @WamEnumConstant(2) OPENSLES,
    @WamEnumConstant(3) OBOE_OPENSL,
    @WamEnumConstant(4) OBOE_UNSPECIFIED,
    @WamEnumConstant(5) JNI_IN_OBOE_OPENSL_OUT,
    @WamEnumConstant(6) JNI_IN_OBOE_UNSPECIFIED_OUT
}
