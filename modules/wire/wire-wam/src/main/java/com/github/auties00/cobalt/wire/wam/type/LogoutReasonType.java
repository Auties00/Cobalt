package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLogoutReasonType")
@WamEnum
public enum LogoutReasonType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) NETWORK_BLOCKED,
    @WamEnumConstant(2) WRITING_STANZA_ERROR,
    @WamEnumConstant(3) READING_STANZA_ERROR_IO,
    @WamEnumConstant(4) READING_STANZA_READING_IS_OVER,
    @WamEnumConstant(5) READING_STANZA_ERROR_CORRUPT_STREAM,
    @WamEnumConstant(6) PUSH_WITH_SAME_SESSION_ID,
    @WamEnumConstant(7) SCHEDULED_LOGOUT,
    @WamEnumConstant(8) CLIENT_PING_TIMEOUT,
    @WamEnumConstant(9) RECEIPT_TIMEOUT,
    @WamEnumConstant(10) ACTIVE_CONNECTION_TIMEOUT,
    @WamEnumConstant(11) NETWORK_CHANGED,
    @WamEnumConstant(12) SWITCH_ACCOUNT,
    @WamEnumConstant(13) DELETE_ACCOUNT,
    @WamEnumConstant(14) CHANGE_NUMBER,
    @WamEnumConstant(15) MBS_MIGRATION,
    @WamEnumConstant(16) REGISTRATION_RELATED,
    @WamEnumConstant(17) ROADBLOCKED_DETAIL,
    @WamEnumConstant(18) SERVER_ACK_KICK,
    @WamEnumConstant(19) SERVER_PING_KICK,
    @WamEnumConstant(20) CONNECTION_RESET
}
