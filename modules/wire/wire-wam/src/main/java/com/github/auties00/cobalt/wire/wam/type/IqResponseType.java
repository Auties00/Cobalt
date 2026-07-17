package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumIqResponseType")
@WamEnum
public enum IqResponseType {
    @WamEnumConstant(1) ADD_PARTICIPANT,
    @WamEnumConstant(2) REMOVE_PARTICIPANT,
    @WamEnumConstant(3) PROMOTE_PARTICIPANT,
    @WamEnumConstant(4) DEMOTE_PARTICIPANT
}
