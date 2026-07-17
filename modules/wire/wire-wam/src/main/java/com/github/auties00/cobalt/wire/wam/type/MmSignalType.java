package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMmSignalType")
@WamEnum
public enum MmSignalType {
    @WamEnumConstant(0) USER_BLOCK,
    @WamEnumConstant(1) USER_REPORT,
    @WamEnumConstant(2) FIRST_CUSTOMER_MESSAGE,
    @WamEnumConstant(3) FIRST_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(4) FIRST_BIZ_REPLY,
    @WamEnumConstant(5) FIRST_BIZ_REPLY_CONTINUATION,
    @WamEnumConstant(6) SECOND_CUSTOMER_MESSAGE,
    @WamEnumConstant(7) SECOND_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(8) SECOND_BIZ_REPLY,
    @WamEnumConstant(9) SECOND_BIZ_REPLY_CONTINUATION,
    @WamEnumConstant(10) THIRD_CUSTOMER_MESSAGE,
    @WamEnumConstant(11) THIRD_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(12) THIRD_BIZ_REPLY,
    @WamEnumConstant(13) USER_INTERESTED,
    @WamEnumConstant(14) USER_NOT_INTERESTED,
    @WamEnumConstant(15) USER_STOP_OFFERS,
    @WamEnumConstant(16) USER_BLOCK_REASON_NO_LONGER_NEEDED,
    @WamEnumConstant(17) USER_BLOCK_REASON_NO_SIGN_UP,
    @WamEnumConstant(18) USER_BLOCK_REASON_SPAM,
    @WamEnumConstant(19) USER_BLOCK_REASON_OFFENSIVE_MESSAGES,
    @WamEnumConstant(20) USER_BLOCK_REASON_OTP_DID_NOT_REQUEST,
    @WamEnumConstant(21) USER_BLOCK_REASON_OTHER,
    @WamEnumConstant(22) USER_MUTE,
    @WamEnumConstant(23) URL_CTA_CLICK,
    @WamEnumConstant(24) APP_CTA_CLICK,
    @WamEnumConstant(25) BODY_URL_CLICK,
    @WamEnumConstant(26) USER_BLOCK_REASON_SCAM_OR_FRAUD,
    @WamEnumConstant(27) MESSAGE,
    @WamEnumConstant(28) USER_ARCHIVE,
    @WamEnumConstant(29) BODY_URL_LONG_PRESS,
    @WamEnumConstant(30) USER_BLOCK_REASON_DONT_RECOGNIZE,
    @WamEnumConstant(31) IAB_LPV,
    @WamEnumConstant(32) IAB_LPV_BODY
}
