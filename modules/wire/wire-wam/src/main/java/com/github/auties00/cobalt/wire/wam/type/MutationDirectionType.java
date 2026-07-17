package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMutationDirectionType")
@WamEnum
public enum MutationDirectionType {
    @WamEnumConstant(0) INCOMING,
    @WamEnumConstant(1) OUTGOING
}
