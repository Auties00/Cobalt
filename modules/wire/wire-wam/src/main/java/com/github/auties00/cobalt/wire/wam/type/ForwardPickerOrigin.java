package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumForwardPickerOrigin")
@WamEnum
public enum ForwardPickerOrigin {
    @WamEnumConstant(1) STATUS_VIEWER,
    @WamEnumConstant(2) PTT_WIDGET
}
