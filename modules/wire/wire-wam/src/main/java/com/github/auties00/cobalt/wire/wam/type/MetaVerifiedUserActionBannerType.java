package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedUserActionBannerType")
@WamEnum
public enum MetaVerifiedUserActionBannerType {
    @WamEnumConstant(1) BB_APPROACHING_LIMIT,
    @WamEnumConstant(2) BB_LIMIT_REACHED,
    @WamEnumConstant(3) BB_CREDIT_EXPIRED,
    @WamEnumConstant(4) BB_MV_PENDING,
    @WamEnumConstant(5) BB_MV_BENEFIT,
    @WamEnumConstant(6) BB_NEED_MORE_CREDITS,
    @WamEnumConstant(7) BB_MV_UPSELL,
    @WamEnumConstant(8) BB_SEND_LIMIT_EDUCATIONAL
}
