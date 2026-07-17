package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVideoPlayResult")
@WamEnum
public enum VideoPlayResult {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) ERROR_PLAYER,
    @WamEnumConstant(3) ERROR_VIDEO_TRACK,
    @WamEnumConstant(4) ERROR_AUDIO_TRACK,
    @WamEnumConstant(5) ERROR_DOWNLOAD_FAILED
}
