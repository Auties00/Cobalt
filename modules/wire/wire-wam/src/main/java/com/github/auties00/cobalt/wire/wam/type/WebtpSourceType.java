package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebtpSourceType")
@WamEnum
public enum WebtpSourceType {
    @WamEnumConstant(1) CHAT,
    @WamEnumConstant(2) MEDIA_HUB,
    @WamEnumConstant(3) CHAT_MEDIA,
    @WamEnumConstant(4) THUMBNAIL,
    @WamEnumConstant(5) MEDIA_VIEWER_HEADER,
    @WamEnumConstant(6) MESSAGE_BUBBLE,
    @WamEnumConstant(7) PDF_VIEWER_ERROR_SCREEN,
    @WamEnumConstant(8) PDF_VIEWER,
    @WamEnumConstant(9) PDF_SHARER,
    @WamEnumConstant(10) PDF_RECEIVER
}
