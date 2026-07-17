package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiAdCreationAccountConsentFlow")
@WamEnum
public enum LwiAdCreationAccountConsentFlow {
    @WamEnumConstant(1) SKIP_CONSENT_WITH_EXISTING_FB_CONSENT,
    @WamEnumConstant(2) SKIP_CONSENT_WITHOUT_FB_WEBLOGIN_OPTION,
    @WamEnumConstant(3) SKIP_CONSENT_WITH_FB_WEBLOGIN_OPTION,
    @WamEnumConstant(4) SKIP_CONSENT_WITH_SUGGEST_FB_CONSENT,
    @WamEnumConstant(5) ACCEPT_CONSENT_WITH_SUGGEST_FB_CONSENT,
    @WamEnumConstant(6) ACCEPT_CONSENT_WITH_REQUIRE_FB_CONSENT,
    @WamEnumConstant(7) RELOAD_BLOKS_SCREEN_ON_SUCCESS_FB_WEB_SSO
}
