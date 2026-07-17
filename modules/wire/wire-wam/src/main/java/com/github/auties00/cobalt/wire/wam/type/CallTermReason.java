package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallTermReason")
@WamEnum
public enum CallTermReason {
    @WamEnumConstant(1) ENDED_BY_USER,
    @WamEnumConstant(2) TIMEOUT,
    @WamEnumConstant(3) RECONNECTING,
    @WamEnumConstant(4) ENDED_BY_SELF,
    @WamEnumConstant(5) ENDED_BY_PEER,
    @WamEnumConstant(6) VIDEO_ENCODER_FATAL_ERROR,
    @WamEnumConstant(7) VIDEO_DECODER_FATAL_ERROR,
    @WamEnumConstant(8) AUDIO_RECORD_FATAL_ERROR,
    @WamEnumConstant(9) CRASH,
    @WamEnumConstant(10) DETECT_IDENTITY_CHANGE,
    @WamEnumConstant(11) USER_INVISIBLE,
    @WamEnumConstant(12) RELAY_BIND_FAILED,
    @WamEnumConstant(13) SETUP_FAILED,
    @WamEnumConstant(14) ACCEPTED_ELSEWHERE,
    @WamEnumConstant(15) REJECTED_ELSEWHERE,
    @WamEnumConstant(16) GROUP_CALL_ENDED,
    @WamEnumConstant(17) CALL_IS_FULL,
    @WamEnumConstant(18) PHONE_PERMISSION_DENIED,
    @WamEnumConstant(19) USER_REMOVED,
    @WamEnumConstant(20) HEARTBEAT_TERMINATE,
    @WamEnumConstant(21) REMOTE_DISCONNECT,
    @WamEnumConstant(22) CALLER_BLOCKED,
    @WamEnumConstant(23) DND_MODE,
    @WamEnumConstant(24) MICROPHONE_PERMISSION_DENIED,
    @WamEnumConstant(25) CAMERA_PERMISSION_DENIED,
    @WamEnumConstant(26) MEDIA_TX_RX_TIMEOUT,
    @WamEnumConstant(27) RINGING_TIMEOUT,
    @WamEnumConstant(28) CALL_IS_PROTECTED,
    @WamEnumConstant(29) MEDIA_RX_TIMEOUT,
    @WamEnumConstant(30) MEDIA_TX_TIMEOUT,
    @WamEnumConstant(31) VC_LONELY_STATE_TIMEOUT,
    @WamEnumConstant(32) WAITING_ROOM_DENIED,
    @WamEnumConstant(33) WAITING_ROOM_TIMEOUT,
    @WamEnumConstant(34) DEVICE_SWITCH,
    @WamEnumConstant(35) VIDEO_PREVIEW_ERROR,
    @WamEnumConstant(36) VIDEO_STREAM_CREATE_ERROR,
    @WamEnumConstant(37) VIDEO_PORT_CREATE_ERROR,
    @WamEnumConstant(38) FAILED_TO_SET_VIDEO_DISPLAY_SURFACE,
    @WamEnumConstant(39) FATAL_NACK
}
