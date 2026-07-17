package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedInteractionSurface")
@WamEnum
public enum MetaVerifiedInteractionSurface {
    @WamEnumConstant(1) BUSINESS_PROFILE,
    @WamEnumConstant(2) MV_EDUCATION_BOTTOM_SHEET,
    @WamEnumConstant(3) CROSS_SELL_PROFILE_INTERSTITIAL,
    @WamEnumConstant(4) META_VERIFIED_HOME,
    @WamEnumConstant(5) SETTINGS,
    @WamEnumConstant(6) BUSINESS_TOOLS,
    @WamEnumConstant(7) BUSINESS_CARD,
    @WamEnumConstant(8) FMX_NOT_META_VERFIED_BOTTOM_SHEET,
    @WamEnumConstant(9) FMX_CONTACT_CARD,
    @WamEnumConstant(10) INCOMING_CALL_NOTIFICATION,
    @WamEnumConstant(11) INCOMING_CALL_SCREEN,
    @WamEnumConstant(12) INCOMING_CALL_NOT_MV_BOTTOMSHEET
}
