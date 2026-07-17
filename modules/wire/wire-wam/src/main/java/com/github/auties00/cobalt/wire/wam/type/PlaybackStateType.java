package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPlaybackStateType")
@WamEnum
public enum PlaybackStateType {
    @WamEnumConstant(1) IDLE,
    @WamEnumConstant(2) READY_PLAY,
    @WamEnumConstant(3) READY_PAUSE,
    @WamEnumConstant(4) BUFFERING,
    @WamEnumConstant(5) OUTSIDE,
    @WamEnumConstant(6) ENDED,
    @WamEnumConstant(7) ERROR
}
