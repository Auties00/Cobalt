package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumChatWallpaperType")
@WamEnum
public enum ChatWallpaperType {
    @WamEnumConstant(1) LIGHT,
    @WamEnumConstant(2) DARK,
    @WamEnumConstant(3) SOLID,
    @WamEnumConstant(4) CUSTOM,
    @WamEnumConstant(5) DEFAULT
}
