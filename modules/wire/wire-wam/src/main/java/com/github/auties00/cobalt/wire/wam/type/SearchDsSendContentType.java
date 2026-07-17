package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSearchDsSendContentType")
@WamEnum
public enum SearchDsSendContentType {
    @WamEnumConstant(1) TEXT,
    @WamEnumConstant(2) PHOTO,
    @WamEnumConstant(3) VIDEO,
    @WamEnumConstant(4) PTT,
    @WamEnumConstant(5) DOCUMENT,
    @WamEnumConstant(6) STICKER,
    @WamEnumConstant(7) GIF,
    @WamEnumConstant(8) CONTACT_CARD,
    @WamEnumConstant(9) LOCATION
}
