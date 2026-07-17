package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumHarmfulFileWarningClickthroughAction")
@WamEnum
public enum HarmfulFileWarningClickthroughAction {
    @WamEnumConstant(0) CANCEL,
    @WamEnumConstant(1) OPEN,
    @WamEnumConstant(2) DIALOG_DISMISSED,
    @WamEnumConstant(3) LEARN_MORE,
    @WamEnumConstant(4) DIALOG_OPEN
}
