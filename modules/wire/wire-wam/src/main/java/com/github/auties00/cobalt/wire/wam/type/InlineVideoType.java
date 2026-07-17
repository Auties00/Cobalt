package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumInlineVideoType")
@WamEnum
public enum InlineVideoType {
    @WamEnumConstant(1) FACEBOOK,
    @WamEnumConstant(2) FACEBOOK_WATCH,
    @WamEnumConstant(3) INSTAGRAM,
    @WamEnumConstant(4) YOUTUBE,
    @WamEnumConstant(5) STREAMABLE,
    @WamEnumConstant(6) NETFLIX,
    @WamEnumConstant(7) LASSO,
    @WamEnumConstant(8) SHARECHAT,
    @WamEnumConstant(9) SPOTIFY,
    @WamEnumConstant(10) APPLEMUSIC,
    @WamEnumConstant(11) TIDAL,
    @WamEnumConstant(12) DEEZER,
    @WamEnumConstant(13) SOUNDCLOUD,
    @WamEnumConstant(14) QOBUZ,
    @WamEnumConstant(15) PANDORA,
    @WamEnumConstant(16) TIKTOK,
    @WamEnumConstant(17) FACEBOOK_MESSENGER
}
