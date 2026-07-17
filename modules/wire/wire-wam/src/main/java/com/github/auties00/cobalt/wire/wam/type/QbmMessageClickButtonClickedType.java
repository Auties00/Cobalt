package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQbmMessageClickButtonClickedType")
@WamEnum
public enum QbmMessageClickButtonClickedType {
    @WamEnumConstant(0) URL,
    @WamEnumConstant(1) QUICK_REPLY,
    @WamEnumConstant(2) PHONE_NUMBER,
    @WamEnumConstant(3) COPY_CODE,
    @WamEnumConstant(4) CATALOG,
    @WamEnumConstant(5) MPM,
    @WamEnumConstant(6) FLOW,
    @WamEnumConstant(7) OTHER,
    @WamEnumConstant(8) HEADER_URL,
    @WamEnumConstant(9) BODY_URL,
    @WamEnumConstant(10) FOOTER_URL,
    @WamEnumConstant(11) APP,
    @WamEnumConstant(12) IMAGE_HEADER_URL,
    @WamEnumConstant(13) FULLSCREEN_IMAGE_CTA_URL,
    @WamEnumConstant(14) FULLSCREEN_VIDEO_CTA_URL,
    @WamEnumConstant(15) VIEW_PRODUCT,
    @WamEnumConstant(16) TEXT_HEADER_URL,
    @WamEnumConstant(17) FULLSCREEN_ALBUM_CTA_URL,
    @WamEnumConstant(18) WATCH_AND_BROWSE_VIDEO,
    @WamEnumConstant(19) CTA_REMIND_ME,
    @WamEnumConstant(20) CTA_CANCEL_REMINDER,
    @WamEnumConstant(21) SUGGESTED_QUICK_REPLY,
    @WamEnumConstant(22) QUICK_REPLY_PILL
}
