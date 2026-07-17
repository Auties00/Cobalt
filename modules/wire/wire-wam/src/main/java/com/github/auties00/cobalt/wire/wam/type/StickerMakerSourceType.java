package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStickerMakerSourceType")
@WamEnum
public enum StickerMakerSourceType {
    @WamEnumConstant(1) CUTOUT_IMAGE,
    @WamEnumConstant(2) WEB_STICKER_MAKER,
    @WamEnumConstant(3) IOS_STICKER_MAKER,
    @WamEnumConstant(4) ANDROID_STICKER_MAKER,
    @WamEnumConstant(5) TRANSPARENT_IMAGE,
    @WamEnumConstant(6) GIF
}
