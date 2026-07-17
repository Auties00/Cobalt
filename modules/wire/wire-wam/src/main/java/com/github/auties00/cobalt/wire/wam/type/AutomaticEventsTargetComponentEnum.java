package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAutomaticEventsTargetComponentEnum")
@WamEnum
public enum AutomaticEventsTargetComponentEnum {
    @WamEnumConstant(0) SYSTEM_MESSAGE,
    @WamEnumConstant(1) LEARN_MORE,
    @WamEnumConstant(2) NUX_SCREEN,
    @WamEnumConstant(3) NUX_SCREEN_OK,
    @WamEnumConstant(4) NUX_SCREEN_META_BUSINESS_SUITE,
    @WamEnumConstant(5) AE_ONBOARDING_NUX_SCREEN,
    @WamEnumConstant(6) AE_ONBOARDING_NUX_TURN_ON,
    @WamEnumConstant(7) AE_ONBOARDING_NUX_NOT_NOW,
    @WamEnumConstant(8) AE_ONBOARDING_NUX_META_TERMS_FOR_AUTOMATIC_EVENTS,
    @WamEnumConstant(9) AE_ONBOARDING_NUX_PRIVACY_POLICY,
    @WamEnumConstant(10) AE_ONBOARDING_NUX_LEARN_MORE,
    @WamEnumConstant(11) AE_ONBOARDING_START,
    @WamEnumConstant(12) AE_ONBOARDING_FINISH,
    @WamEnumConstant(13) AE_OFFBOARDING_NUX_SCREEN,
    @WamEnumConstant(14) AE_OFFBOARDING_NUX_TURN_OFF,
    @WamEnumConstant(15) AE_OFFBOARDING_NUX_NOT_NOW,
    @WamEnumConstant(16) AE_OFFBOARDING_CONFIRMATION_DIALOG,
    @WamEnumConstant(17) AE_OFFBOARDING_CONFIRMATION_DIALOG_TURN_OFF,
    @WamEnumConstant(18) AE_OFFBOARDING_CONFIRMATION_DIALOG_NOT_NOW,
    @WamEnumConstant(19) AE_OFFBOARDING_START,
    @WamEnumConstant(20) AE_OFFBOARDING_FINISH,
    @WamEnumConstant(21) AE_ONBOARDING_BLOCKED_FOR_MULTI1P,
    @WamEnumConstant(22) LIST_SETTINGS_AE_ROW_OFFBOARDED,
    @WamEnumConstant(23) LIST_SETTINGS_AE_ROW_ONBOARDED,
    @WamEnumConstant(24) LIST_SETTINGS_AE_ROW_OFFBOARDING,
    @WamEnumConstant(25) LIST_SETTINGS_AE_ROW_ONBOARDING,
    @WamEnumConstant(26) LIST_SETTINGS_AE_ROW_OFFBOARDED_CLICK,
    @WamEnumConstant(27) LIST_SETTINGS_AE_ROW_ONBOARDED_CLICK,
    @WamEnumConstant(28) AE_ONBOARDING_BIOMETRIC_SCREEN,
    @WamEnumConstant(29) AE_ONBOARDING_BIOMETRIC_SKIPPED,
    @WamEnumConstant(30) AE_ONBOARDING_BIOMETRIC_BYPASS,
    @WamEnumConstant(31) AE_ONBOARDING_BIOMETRIC_SUCCESS,
    @WamEnumConstant(32) AE_ONBOARDING_BIOMETRIC_CANCELLED,
    @WamEnumConstant(33) AE_ONBOARDING_BIOMETRIC_ERROR,
    @WamEnumConstant(34) AE_PRECHECK_FINISH
}
