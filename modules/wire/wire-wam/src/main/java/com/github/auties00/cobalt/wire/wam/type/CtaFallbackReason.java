package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtaFallbackReason")
@WamEnum
public enum CtaFallbackReason {
    @WamEnumConstant(0) NO_OTP_REQUEST_RECEIVED,
    @WamEnumConstant(1) OTP_REQUEST_EXPIRED,
    @WamEnumConstant(2) HASH_MISMATCH,
    @WamEnumConstant(3) NO_ACTIVITY_LISTENING_ON_THIRD_PARTY_APP,
    @WamEnumConstant(4) OTHER,
    @WamEnumConstant(5) NO_PACKAGE_NAME_ON_MESSAGE,
    @WamEnumConstant(6) NO_CTA_DISPLAY_NAME,
    @WamEnumConstant(7) INCOMPATIBLE_OS_VERSION,
    @WamEnumConstant(8) NO_RETRIEVER_BUTTON,
    @WamEnumConstant(9) FEATURE_DISABLED,
    @WamEnumConstant(10) AMBIGUOUS_DELIVERY_DESTINATION
}
