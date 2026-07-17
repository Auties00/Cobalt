package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCtwaAdAccountType")
@WamEnum
public enum CtwaAdAccountType {
    @WamEnumConstant(0) CTWA_FB_PAGE_LINKED_ACCOUNT,
    @WamEnumConstant(1) CTWA_FB_PAGELESS_ACCOUNT,
    @WamEnumConstant(2) CTWA_WA_AD_ACCOUNT
}
