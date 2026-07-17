package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumFetchResultEnum")
@WamEnum
public enum FetchResultEnum {
    @WamEnumConstant(0) SUCCESS,
    @WamEnumConstant(1) EXCEPTION
}
