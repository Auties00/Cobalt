package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOtpEventType")
@WamEnum
public enum OtpEventType {
    @WamEnumConstant(0) CLICK,
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) OTP_CODE_REQUESTED,
    @WamEnumConstant(3) OTP_CODE_SENT,
    @WamEnumConstant(4) MESSAGE_RECEIVED,
    @WamEnumConstant(5) MESSAGE_READ,
    @WamEnumConstant(6) MESSAGE_DELETED,
    @WamEnumConstant(7) ZERO_TAP_ENABLED,
    @WamEnumConstant(8) ZERO_TAP_DISABLED,
    @WamEnumConstant(9) ZERO_TAP_NOTICE_VIEWED,
    @WamEnumConstant(10) CLEAR_CHAT,
    @WamEnumConstant(11) ZERO_TAP_SEND_CODE_STARTED,
    @WamEnumConstant(12) ZERO_TAP_SEND_CODE_COMPLETED,
    @WamEnumConstant(13) ZERO_TAP_SEND_CODE_FAILED,
    @WamEnumConstant(14) OTP_CONF_OPT_ZERO_TAP_FLAG_ENABLED,
    @WamEnumConstant(15) OTP_CONF_OPT_ZERO_TAP_FLAG_DISABLED,
    @WamEnumConstant(16) HANDSHAKE_CONFIRMATION_SENT,
    @WamEnumConstant(17) FEEDBACK_BUTTON_CLICK
}
