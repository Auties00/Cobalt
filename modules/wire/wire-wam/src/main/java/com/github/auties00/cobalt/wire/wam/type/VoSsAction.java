package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumVoSsAction")
@WamEnum
public enum VoSsAction {
    @WamEnumConstant(1) SENDER_NUX_TYPE_C_IMPRESSION,
    @WamEnumConstant(2) SENDER_NUX_TYPE_D_IMPRESSION,
    @WamEnumConstant(3) SCREENSHOT_SENDER_NUX_TYPE_D_IMPRESSION,
    @WamEnumConstant(4) RECIPIENT_OPEN_NUX_TYPE_C_IMPRESSION,
    @WamEnumConstant(5) RECIPIENT_OPEN_NUX_TYPE_D_IMPRESSION,
    @WamEnumConstant(6) SENDER_NUX_TYPE_C_LEARN_MORE_TAP,
    @WamEnumConstant(7) SENDER_NUX_TYPE_D_LEARN_MORE_TAP,
    @WamEnumConstant(8) SCREENSHOT_SENDER_NUX_TYPE_D_LEARN_MORE_TAP,
    @WamEnumConstant(9) RECIPIENT_OPEN_NUX_TYPE_C_LEARN_MORE_TAP,
    @WamEnumConstant(10) RECIPIENT_OPEN_NUX_TYPE_D_LEARN_MORE_TAP,
    @WamEnumConstant(11) SCREENSHOT_BLOCKED,
    @WamEnumConstant(12) SCREENSHOT_TAKEN,
    @WamEnumConstant(13) SCREEN_RECORDING_BLOCKED,
    @WamEnumConstant(14) SCREEN_RECORDING_STARTED,
    @WamEnumConstant(15) PLACEHOLDER_MESSAGE_LEARN_MORE_TAP
}
