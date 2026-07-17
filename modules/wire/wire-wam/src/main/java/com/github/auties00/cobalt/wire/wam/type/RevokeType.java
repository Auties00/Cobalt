package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRevokeType")
@WamEnum
public enum RevokeType {
    @WamEnumConstant(0) SENDER,
    @WamEnumConstant(1) ADMIN
}
