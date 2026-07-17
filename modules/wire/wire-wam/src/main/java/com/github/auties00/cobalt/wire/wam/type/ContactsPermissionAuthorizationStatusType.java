package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumContactsPermissionAuthorizationStatusType")
@WamEnum
public enum ContactsPermissionAuthorizationStatusType {
    @WamEnumConstant(1) NOT_DETERMINED,
    @WamEnumConstant(2) RESTRICTED,
    @WamEnumConstant(3) DENIED,
    @WamEnumConstant(4) AUTHORIZED,
    @WamEnumConstant(5) LIMITED
}
