package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSignalType")
@WamEnum
public enum SignalType {
    @WamEnumConstant(0) MM_CLICK,
    @WamEnumConstant(1) USER_BLOCK,
    @WamEnumConstant(2) USER_REPORT,
    @WamEnumConstant(3) FIRST_CUSTOMER_MESSAGE,
    @WamEnumConstant(4) FIRST_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(5) FIRST_BIZ_REPLY,
    @WamEnumConstant(6) FIRST_BIZ_REPLY_CONTINUATION,
    @WamEnumConstant(7) SECOND_CUSTOMER_MESSAGE,
    @WamEnumConstant(8) SECOND_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(9) SECOND_BIZ_REPLY,
    @WamEnumConstant(10) SECOND_BIZ_REPLY_CONTINUATION,
    @WamEnumConstant(11) THIRD_CUSTOMER_MESSAGE,
    @WamEnumConstant(12) THIRD_CUSTOMER_MESSAGE_CONTINUATION,
    @WamEnumConstant(13) THIRD_BIZ_REPLY,
    @WamEnumConstant(14) USER_INTERESTED,
    @WamEnumConstant(15) USER_NOT_INTERESTED,
    @WamEnumConstant(16) USER_STOP_OFFERS,
    @WamEnumConstant(17) USER_BLOCK_REASON_NO_LONGER_NEEDED,
    @WamEnumConstant(18) USER_BLOCK_REASON_NO_SIGN_UP,
    @WamEnumConstant(19) USER_BLOCK_REASON_SPAM,
    @WamEnumConstant(20) USER_BLOCK_REASON_OFFENSIVE_MESSAGES,
    @WamEnumConstant(21) USER_BLOCK_REASON_OTP_DID_NOT_REQUEST,
    @WamEnumConstant(22) USER_BLOCK_REASON_OTHER,
    @WamEnumConstant(23) USER_MUTE,
    @WamEnumConstant(24) USER_BLOCK_REASON_SCAM_OR_FRAUD,
    @WamEnumConstant(25) MESSAGE,
    @WamEnumConstant(26) USER_ARCHIVE,
    @WamEnumConstant(27) USER_BLOCK_REASON_DONT_RECOGNIZE,
    @WamEnumConstant(28) IAB_LPV,
    @WamEnumConstant(29) IAB_LPV_BODY
}
