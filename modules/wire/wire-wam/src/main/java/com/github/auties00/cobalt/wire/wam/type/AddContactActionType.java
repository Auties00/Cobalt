package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAddContactActionType")
@WamEnum
public enum AddContactActionType {
    @WamEnumConstant(0) OPEN,
    @WamEnumConstant(1) SAVE,
    @WamEnumConstant(2) CANCEL,
    @WamEnumConstant(3) DELETE,
    @WamEnumConstant(4) CREATE_DUPLICATE,
    @WamEnumConstant(5) PIN_SUBMIT
}
