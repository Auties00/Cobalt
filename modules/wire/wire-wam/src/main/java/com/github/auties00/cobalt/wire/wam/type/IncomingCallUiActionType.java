package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumIncomingCallUiActionType")
@WamEnum
public enum IncomingCallUiActionType {
    @WamEnumConstant(1) INCOMING_CALL_SCREEN_ANSWER,
    @WamEnumConstant(2) INCOMING_CALL_SCREEN_REJECT,
    @WamEnumConstant(3) INCOMING_CALL_NOTIFICATION_ANSWER,
    @WamEnumConstant(4) INCOMING_CALL_NOTIFICATION_REJECT,
    @WamEnumConstant(5) PENDING_CALL_SCREEN_END_AND_ANSWER,
    @WamEnumConstant(6) PENDING_CALL_SCREEN_HOLD_AND_ANSWER,
    @WamEnumConstant(7) PENDING_CALL_SCREEN_REJECT,
    @WamEnumConstant(8) INCOMING_CALL_REMOTE_ANSWER,
    @WamEnumConstant(9) INCOMING_CALL_REMOTE_REJECT,
    @WamEnumConstant(10) PENDING_CALL_NOTIFICATION_END_AND_ANSWER,
    @WamEnumConstant(11) PENDING_CALL_NOTIFICATION_REJECT,
    @WamEnumConstant(12) MINIMIZED_BANNER_ANSWER,
    @WamEnumConstant(13) MINIMIZED_BANNER_REJECT,
    @WamEnumConstant(14) INCOMING_CALL_SCREEN_TURN_OFF_VIDEO,
    @WamEnumConstant(15) INCOMING_CALL_SCREEN_TURN_ON_VIDEO
}
