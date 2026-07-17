package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaAiUpsellCtaOperationType")
@WamEnum
public enum MetaAiUpsellCtaOperationType {
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) BUTTON_CLICK,
    @WamEnumConstant(3) DISMISS
}
