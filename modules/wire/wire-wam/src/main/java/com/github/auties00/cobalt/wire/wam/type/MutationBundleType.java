package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMutationBundleType")
@WamEnum
public enum MutationBundleType {
    @WamEnumConstant(0) SNAPSHOT,
    @WamEnumConstant(1) PATCH
}
