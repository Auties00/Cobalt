package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOtpEventSource")
@WamEnum
public enum OtpEventSource {
    @WamEnumConstant(0) NOTIFICATION_CTA,
    @WamEnumConstant(1) NOTIFICATION_BODY,
    @WamEnumConstant(2) CHAT_CTA,
    @WamEnumConstant(3) OTHER,
    @WamEnumConstant(4) OTP_MESSAGE,
    @WamEnumConstant(5) OTP_MESSAGE_INFO,
    @WamEnumConstant(6) OTP_CONFIGURATION,
    @WamEnumConstant(7) OTP_REQUEST_SENDER,
    @WamEnumConstant(8) OTP_ZERO_TAP_SENDER,
    @WamEnumConstant(9) OTP_CONF_OPTION
}
