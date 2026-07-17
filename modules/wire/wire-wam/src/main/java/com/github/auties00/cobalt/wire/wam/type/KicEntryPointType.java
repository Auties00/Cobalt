package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKicEntryPointType")
@WamEnum
public enum KicEntryPointType {
    @WamEnumConstant(1) CHAT_INFO,
    @WamEnumConstant(2) SEARCH,
    @WamEnumConstant(3) CHAT,
    @WamEnumConstant(4) MEDIA,
    @WamEnumConstant(5) DOCS,
    @WamEnumConstant(6) LINKS,
    @WamEnumConstant(7) PHOTOS,
    @WamEnumConstant(8) VIDEOS,
    @WamEnumConstant(9) STICKERS,
    @WamEnumConstant(10) ALL_MEDIA
}
