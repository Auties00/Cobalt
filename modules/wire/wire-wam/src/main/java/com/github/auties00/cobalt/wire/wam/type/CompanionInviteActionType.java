package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumCompanionInviteActionType")
@WamEnum
public enum CompanionInviteActionType {
    @WamEnumConstant(0) IMPRESSION,
    @WamEnumConstant(1) INVITE_SEND
}
