package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedUserActionErrorDetails")
@WamEnum
public enum MetaVerifiedUserActionErrorDetails {
    @WamEnumConstant(1) IQ_REQUEST_FAILED,
    @WamEnumConstant(2) INELIGIBLE,
    @WamEnumConstant(3) USER_CANCELLED
}
