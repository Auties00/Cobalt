package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUserNoticeErrorEvent")
@WamEnum
public enum UserNoticeErrorEventType {
    @WamEnumConstant(1) INVALID_STANZA,
    @WamEnumConstant(2) JSON_FETCH,
    @WamEnumConstant(3) JSON_PARSE,
    @WamEnumConstant(4) IMAGE_FETCH,
    @WamEnumConstant(5) NO_ELIGIBLE_DISCLOSURE,
    @WamEnumConstant(6) DISCLOSURE_STAGE_FETCH,
    @WamEnumConstant(7) UI_TEMPLATE_MISMATCHED,
    @WamEnumConstant(8) ERROR_UNKNOWN,
    @WamEnumConstant(9) SYNC_STAGE_MISMATCH,
    @WamEnumConstant(10) IMAGE_FETCH_400,
    @WamEnumConstant(11) JSON_FETCH_400,
    @WamEnumConstant(12) JSON_FETCH_REDIRECT,
    @WamEnumConstant(13) IMAGE_FETCH_REDIRECT,
    @WamEnumConstant(14) IMAGE_FETCH_FORBIDDEN,
    @WamEnumConstant(15) JSON_FETCH_FORBIDDEN,
    @WamEnumConstant(16) FAIL_TO_SEND_STAGE_TO_SERVER
}
