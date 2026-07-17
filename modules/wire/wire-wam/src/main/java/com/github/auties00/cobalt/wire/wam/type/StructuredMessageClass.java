package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStructuredMessageClass")
@WamEnum
public enum StructuredMessageClass {
    @WamEnumConstant(0) HSM,
    @WamEnumConstant(1) BUTTON,
    @WamEnumConstant(2) LIST,
    @WamEnumConstant(3) PRODUCT_LIST,
    @WamEnumConstant(4) BUTTON_NFM,
    @WamEnumConstant(5) PRODUCT_ITEM,
    @WamEnumConstant(6) SHOP_STOREFRONT
}
