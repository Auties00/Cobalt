package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumOnboardingEntryPoint")
@WamEnum
public enum OnboardingEntryPoint {
    @WamEnumConstant(1) ONBOARDING_ENTRY_POINT_FAST_TRACK,
    @WamEnumConstant(2) ONBOARDING_ENTRY_POINT_AD_REVIEW_SCREEN,
    @WamEnumConstant(3) ONBOARDING_ENTRY_POINT_CONSENT_HOST
}
