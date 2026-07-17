package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbListFeatureNameType")
@WamEnum
public enum SmbListFeatureNameType {
    @WamEnumConstant(1) LISTS_CREATION,
    @WamEnumConstant(2) LIST_APPLICATION
}
