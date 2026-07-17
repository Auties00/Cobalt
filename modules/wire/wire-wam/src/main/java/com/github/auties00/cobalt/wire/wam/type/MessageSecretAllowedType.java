package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMessageSecretAllowedType")
@WamEnum
public enum MessageSecretAllowedType {
    @WamEnumConstant(0) MESSAGE_POLL,
    @WamEnumConstant(1) MESSAGE_EDIT,
    @WamEnumConstant(2) REACTION,
    @WamEnumConstant(3) COMMENT,
    @WamEnumConstant(4) EVENT_RESPONSE,
    @WamEnumConstant(5) MESSAGE_EVENT_EDIT,
    @WamEnumConstant(6) MESSAGE_REPORTING_TOKEN
}
