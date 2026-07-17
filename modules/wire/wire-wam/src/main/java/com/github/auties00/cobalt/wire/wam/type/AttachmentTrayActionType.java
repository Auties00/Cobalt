package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumAttachmentTrayActionType")
@WamEnum
public enum AttachmentTrayActionType {
    @WamEnumConstant(1) CLICK,
    @WamEnumConstant(2) SEND,
    @WamEnumConstant(3) CANCEL
}
