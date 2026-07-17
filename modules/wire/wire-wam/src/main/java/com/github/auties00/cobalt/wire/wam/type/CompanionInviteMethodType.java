package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionInviteMethodType")
@WamEnum
public enum CompanionInviteMethodType {
    @WamEnumConstant(0) UNKNOWN,
    @WamEnumConstant(1) NATIVE_SMS,
    @WamEnumConstant(2) SERVER_SMS
}
