package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumInputType")
@WamEnum
public enum InputType {
    @WamEnumConstant(1) SUGGESTION,
    @WamEnumConstant(2) USER_INPUT,
    @WamEnumConstant(3) USER_INPUT_AND_SUGGESTION
}
