package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumIsPureSyncdSessionEnum")
@WamEnum
public enum IsPureSyncdSessionEnum {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) YES,
    @WamEnumConstant(3) NO,
    @WamEnumConstant(4) PROBABLY_YES,
    @WamEnumConstant(5) PROBABLY_NO
}
