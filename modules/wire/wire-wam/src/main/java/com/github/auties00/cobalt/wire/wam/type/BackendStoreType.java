package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBackendStoreType")
@WamEnum
public enum BackendStoreType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) NON_DIRECT_PATH,
    @WamEnumConstant(2) EVERSTORE,
    @WamEnumConstant(3) OIL,
    @WamEnumConstant(4) EXPRESS_PATH,
    @WamEnumConstant(5) STATIC,
    @WamEnumConstant(6) MANIFOLD
}
