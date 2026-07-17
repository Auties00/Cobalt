package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGroupTypeClient")
@WamEnum
public enum GroupTypeClient {
    @WamEnumConstant(1) REGULAR_GROUP,
    @WamEnumConstant(2) SUB_GROUP,
    @WamEnumConstant(3) DEFAULT_SUB_GROUP,
    @WamEnumConstant(4) PARENT_GROUP
}
