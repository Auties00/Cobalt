package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsaMessageRemoveAction")
@WamEnum
public enum PsaMessageRemoveAction {
    @WamEnumConstant(1) BLOCK,
    @WamEnumConstant(2) UNBLOCK,
    @WamEnumConstant(3) ARCHIVE,
    @WamEnumConstant(4) UNARCHIVE,
    @WamEnumConstant(5) CLEAR,
    @WamEnumConstant(6) DELETE_ALL
}
