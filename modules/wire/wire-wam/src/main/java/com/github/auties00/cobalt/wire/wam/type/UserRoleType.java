package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUserRoleType")
@WamEnum
public enum UserRoleType {
    @WamEnumConstant(0) MEMBER,
    @WamEnumConstant(1) ADMIN,
    @WamEnumConstant(2) CADMIN
}
