package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQueryType")
@WamEnum
public enum QueryType {
    @WamEnumConstant(0) STICKER_STORE_DATA,
    @WamEnumConstant(1) PREVIEW_IMAGE_DOWNLOAD,
    @WamEnumConstant(2) STICKER_PACK_DATA,
    @WamEnumConstant(3) STICKER_SEARCH,
    @WamEnumConstant(4) DISCOVERY_PACK
}
