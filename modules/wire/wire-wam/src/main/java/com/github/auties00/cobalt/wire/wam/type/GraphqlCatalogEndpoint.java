package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGraphqlCatalogEndpoint")
@WamEnum
public enum GraphqlCatalogEndpoint {
    @WamEnumConstant(1) GET_CATALOG,
    @WamEnumConstant(2) GET_PRODUCT,
    @WamEnumConstant(3) GET_PRODUCT_LIST,
    @WamEnumConstant(4) GET_COLLECTIONS,
    @WamEnumConstant(5) GET_SINGLE_COLLECTION,
    @WamEnumConstant(6) GET_CATEGORIES,
    @WamEnumConstant(7) GET_VARIANTS,
    @WamEnumConstant(8) GET_PROMOTIONS,
    @WamEnumConstant(9) CREATE_COLLECTION,
    @WamEnumConstant(10) GET_PUBLIC_KEY,
    @WamEnumConstant(11) VERIFY_POSTCODE
}
