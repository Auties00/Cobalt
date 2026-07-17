package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGenaiExitPoint")
@WamEnum
public enum GenaiExitPoint {
    @WamEnumConstant(1) CANCEL_FULL_SHEET,
    @WamEnumConstant(2) CANCEL_HALF_SHEET,
    @WamEnumConstant(3) DISMISS_FULL_SHEET,
    @WamEnumConstant(4) DISMISS_HALF_SHEET,
    @WamEnumConstant(5) INACTIVITY_FULL_SHEET,
    @WamEnumConstant(6) INACTIVITY_HALF_SHEET,
    @WamEnumConstant(7) KEYBOARD_FULL_SHEET,
    @WamEnumConstant(8) KEYBOARD_HALF_SHEET,
    @WamEnumConstant(9) CAMERA_FULL_SHEET,
    @WamEnumConstant(10) CAMERA_HALF_SHEET,
    @WamEnumConstant(11) BACKGROUNDED_FULL_SHEET,
    @WamEnumConstant(12) BACKGROUNDED_HALF_SHEET,
    @WamEnumConstant(13) HALF_SHEET_DIRECT_TO_SETTING,
    @WamEnumConstant(14) FULL_SHEET_DIRECT_TO_SETTING,
    @WamEnumConstant(15) EXIT_MM_SURFACE,
    @WamEnumConstant(16) COMPOSER_TRANSITION
}
