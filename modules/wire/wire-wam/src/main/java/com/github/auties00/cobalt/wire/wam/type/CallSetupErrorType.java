package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCallSetupErrorType")
@WamEnum
public enum CallSetupErrorType {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) CALL_ACCEPT_FAILED,
    @WamEnumConstant(3) INIT_MEDIA_STREAM_FAILED,
    @WamEnumConstant(4) START_MEDIA_STREAM_FAILED,
    @WamEnumConstant(5) AUDIO_INIT_ERROR,
    @WamEnumConstant(6) HANDLE_OFFER_FAILED,
    @WamEnumConstant(7) HANDLE_ACCEPT_FAILED,
    @WamEnumConstant(8) SOUND_PORT_CREATE_FAILED,
    @WamEnumConstant(9) P2P_TRANSPORT_CREATE_FAILED,
    @WamEnumConstant(10) P2P_TRANSPORT_MEDIA_CREATE_FAILED,
    @WamEnumConstant(11) INCOMPATIBLE_SRTP_KEY_EXCHANGE,
    @WamEnumConstant(12) SRTP_KEY_GENERATION_ERROR,
    @WamEnumConstant(13) UNSUPPORTED_AUDIO_CAPS,
    @WamEnumConstant(14) P2P_TRANSPORT_START_FAILED,
    @WamEnumConstant(15) RELAY_BIND_FAILED,
    @WamEnumConstant(16) CANNOT_INITIALIZE_AUDIO_RECORD_OBJECT,
    @WamEnumConstant(17) PEER_RELAY_BIND_FAILED,
    @WamEnumConstant(18) VIDEO_CAPTURE_INIT_FAILED,
    @WamEnumConstant(19) VIDEO_CAPTURE_START_FAILED,
    @WamEnumConstant(20) VIDEO_RENDER_INIT_FAILED,
    @WamEnumConstant(21) VIDEO_RENDER_START_FAILED,
    @WamEnumConstant(22) VIDEO_ENCODER_OPEN_FAILED,
    @WamEnumConstant(23) VIDEO_DECODER_OPEN_FAILED,
    @WamEnumConstant(24) VIDEO_STREAM_CREATE_FAILED,
    @WamEnumConstant(25) VIDEO_STREAM_SETUP_FAILED,
    @WamEnumConstant(26) PEER_SETUP_FAILED,
    @WamEnumConstant(27) HANDLE_PREACCEPT_FAILED
}
