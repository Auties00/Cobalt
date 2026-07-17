package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusType")
@WamEnum
public enum StatusType {
    @WamEnumConstant(1) IMAGE,
    @WamEnumConstant(2) VIDEO,
    @WamEnumConstant(3) GIF,
    @WamEnumConstant(4) AUDIO,
    @WamEnumConstant(5) TEXT,
    @WamEnumConstant(6) MUSIC_STANDALONE,
    @WamEnumConstant(7) FUTURE,
    @WamEnumConstant(8) PLACEHOLDER,
    @WamEnumConstant(9) INLINE_VIDEO
}
