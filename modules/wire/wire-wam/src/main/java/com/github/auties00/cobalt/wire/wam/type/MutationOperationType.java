package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMutationOperationType")
@WamEnum
public enum MutationOperationType {
    @WamEnumConstant(0) SET,
    @WamEnumConstant(1) REMOVE
}
