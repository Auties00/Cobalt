package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchUjFilterType")
@WamEnum
public enum SearchUjFilterType {
    @WamEnumConstant(1) PHOTO,
    @WamEnumConstant(2) VIDEO,
    @WamEnumConstant(3) LINK,
    @WamEnumConstant(4) GIF,
    @WamEnumConstant(5) AUDIO,
    @WamEnumConstant(6) DOCUMENT,
    @WamEnumConstant(7) STICKER,
    @WamEnumConstant(8) POLL,
    @WamEnumConstant(9) OTHER
}
