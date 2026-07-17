package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStickerAddToFavoriteOriginType")
@WamEnum
public enum StickerAddToFavoriteOriginType {
    @WamEnumConstant(1) STICKER_RECEIVED,
    @WamEnumConstant(2) STICKER_SENT,
    @WamEnumConstant(3) STICKER_PICKER,
    @WamEnumConstant(4) STICKER_STORE,
    @WamEnumConstant(5) STICKER_SEARCH,
    @WamEnumConstant(6) STICKER_AI_CREATE,
    @WamEnumConstant(7) DISCOVERY_PACK,
    @WamEnumConstant(8) STICKER_MAKER,
    @WamEnumConstant(9) MEDIA_HUB
}
