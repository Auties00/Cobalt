package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAudioStreamType")
@WamEnum
public enum AudioStreamType {
    @WamEnumConstant(0) STREAM_MUSIC,
    @WamEnumConstant(1) STREAM_VOICE_CALL
}
