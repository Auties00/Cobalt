package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbUserActionTypeEnum")
@WamEnum
public enum SmbUserActionTypeEnum {
    @WamEnumConstant(0) VIEW,
    @WamEnumConstant(1) CLICK,
    @WamEnumConstant(2) ENTER,
    @WamEnumConstant(3) SCROLL,
    @WamEnumConstant(4) SCAN,
    @WamEnumConstant(5) API,
    @WamEnumConstant(6) EDIT,
    @WamEnumConstant(7) DELETE,
    @WamEnumConstant(8) SEARCH,
    @WamEnumConstant(9) DISMISS,
    @WamEnumConstant(10) VALIDATION,
    @WamEnumConstant(11) READ,
    @WamEnumConstant(12) WRITE,
    @WamEnumConstant(13) SUPPRESS
}
