package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFetchMechanismEnum")
@WamEnum
public enum FetchMechanismEnum {
    @WamEnumConstant(0) GRAPHQL,
    @WamEnumConstant(1) IQ
}
