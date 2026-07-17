package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLabelSyncDirectionType")
@WamEnum
public enum LabelSyncDirectionType {
    @WamEnumConstant(1) SENDER,
    @WamEnumConstant(2) RECEIVER,
    @WamEnumConstant(3) RETRY,
    @WamEnumConstant(4) BOOTSTRAP_SENDER
}
