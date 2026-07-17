package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupEncryptionType")
@WamEnum
public enum GroupEncryptionType {
    @WamEnumConstant(1) E2EE,
    @WamEnumConstant(2) HOSTED
}
