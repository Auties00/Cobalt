package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPttPlayerType")
@WamEnum
public enum PttPlayerType {
    @WamEnumConstant(0) AUDIO_QUEUE,
    @WamEnumConstant(1) AVPLAYER,
    @WamEnumConstant(2) AVAUDIOPLAYER,
    @WamEnumConstant(3) OPUSPLAYER,
    @WamEnumConstant(4) ANDROIDPLAYER,
    @WamEnumConstant(5) EXOPLAYER,
    @WamEnumConstant(6) UWPPLAYER,
    @WamEnumConstant(7) VOIPPLAYER
}
