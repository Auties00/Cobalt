package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumEditType")
@WamEnum
public enum EditType {
    @WamEnumConstant(0) NOT_EDITED,
    @WamEnumConstant(1) EDITED,
    @WamEnumConstant(2) SENDER_REVOKE,
    @WamEnumConstant(3) ADMIN_REVOKE,
    @WamEnumConstant(4) ADMIN_EDIT,
    @WamEnumConstant(5) PIN
}
