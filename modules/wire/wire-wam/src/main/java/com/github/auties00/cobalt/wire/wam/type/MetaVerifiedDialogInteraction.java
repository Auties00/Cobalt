package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedDialogInteraction")
@WamEnum
public enum MetaVerifiedDialogInteraction {
    @WamEnumConstant(1) IMPRESSION,
    @WamEnumConstant(2) CONFIRM,
    @WamEnumConstant(3) LEARN_MORE,
    @WamEnumConstant(4) CANCEL
}
