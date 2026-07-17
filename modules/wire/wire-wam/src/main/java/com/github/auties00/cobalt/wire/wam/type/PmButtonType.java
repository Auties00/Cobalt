package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPmButtonType")
@WamEnum
public enum PmButtonType {
    @WamEnumConstant(0) QUICK_REPLY,
    @WamEnumConstant(1) CTA_URL,
    @WamEnumConstant(2) CTA_CALL,
    @WamEnumConstant(3) CTA_CATALOG,
    @WamEnumConstant(4) CTA_CATALOG_ITEM
}
