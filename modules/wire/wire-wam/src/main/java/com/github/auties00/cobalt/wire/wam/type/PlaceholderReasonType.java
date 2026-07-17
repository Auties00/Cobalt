package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPlaceholderReasonType")
@WamEnum
public enum PlaceholderReasonType {
    @WamEnumConstant(0) OTHER,
    @WamEnumConstant(1) SIGNAL_NO_SESSION,
    @WamEnumConstant(2) DEVICE_VERIFICATION_FAILURE,
    @WamEnumConstant(3) UNKNOWN_SELF_DEVICE,
    @WamEnumConstant(4) SIGNAL_INVALID_KEY,
    @WamEnumConstant(5) SIGNAL_INVALID_KEY_ID,
    @WamEnumConstant(6) SIGNAL_INVALID_MESSAGE,
    @WamEnumConstant(7) DEVICE_VERIFICATION_FAILURE_SELF_PEER,
    @WamEnumConstant(8) UNKNOWN_COMPANION_NO_PREKEY,
    @WamEnumConstant(9) BAD_EPHEMERAL_SETTING,
    @WamEnumConstant(10) SIGNAL_FUTURE_MESSAGE,
    @WamEnumConstant(11) SIGNAL_INVALID_SIGNATURE,
    @WamEnumConstant(12) SIGNAL_BAD_MAC,
    @WamEnumConstant(13) SIGNAL_INVALID_SESSION
}
