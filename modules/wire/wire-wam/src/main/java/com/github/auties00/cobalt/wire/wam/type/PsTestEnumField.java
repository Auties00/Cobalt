package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsTestEnumField")
@WamEnum
public enum PsTestEnumField {
    @WamEnumConstant(1) TEST_VALUE1,
    @WamEnumConstant(2) TEST_VALUE2
}
