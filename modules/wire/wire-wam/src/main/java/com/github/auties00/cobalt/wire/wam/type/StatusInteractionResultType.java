package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusInteractionResultType")
@WamEnum
public enum StatusInteractionResultType {
    @WamEnumConstant(1) OK,
    @WamEnumConstant(2) REVOKED,
    @WamEnumConstant(3) ERROR_UNKNOWN
}
