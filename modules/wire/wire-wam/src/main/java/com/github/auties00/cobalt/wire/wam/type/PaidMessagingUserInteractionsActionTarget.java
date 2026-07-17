package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPaidMessagingUserInteractionsActionTarget")
@WamEnum
public enum PaidMessagingUserInteractionsActionTarget {
    @WamEnumConstant(0) NONE,
    @WamEnumConstant(1) HEADER,
    @WamEnumConstant(2) CTA_COPY_CODE,
    @WamEnumConstant(3) CTA_URL,
    @WamEnumConstant(4) QUICK_REPLY,
    @WamEnumConstant(5) CTA_CALL,
    @WamEnumConstant(6) CTA_REMINDER,
    @WamEnumConstant(7) SEE_ALL,
    @WamEnumConstant(8) THUMBNAIL,
    @WamEnumConstant(9) VIEW_PRODUCT,
    @WamEnumConstant(10) URL,
    @WamEnumConstant(11) CTA_APP,
    @WamEnumConstant(12) IMAGE_HEADER_URL,
    @WamEnumConstant(13) FULLSCREEN_IMAGE_CTA_URL,
    @WamEnumConstant(14) FULLSCREEN_VIDEO_CTA_URL,
    @WamEnumConstant(15) TEXT_HEADER_URL,
    @WamEnumConstant(16) FULLSCREEN_ALBUM_CTA_URL,
    @WamEnumConstant(17) WATCH_AND_BROWSE_VIDEO,
    @WamEnumConstant(18) SUGGESTED_QUICK_REPLY,
    @WamEnumConstant(19) QUICK_REPLY_PILL
}
