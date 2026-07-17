package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsIdAction")
@WamEnum
public enum PsIdAction {
    @WamEnumConstant(1) CREATED,
    @WamEnumConstant(2) ROTATED,
    @WamEnumConstant(3) DELETED
}
