package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMismatchOriginType")
@WamEnum
public enum MismatchOriginType {
    @WamEnumConstant(1) INCOMING_GROUP_MESSAGE,
    @WamEnumConstant(2) ACK_OUTGOING_MESSAGE,
    @WamEnumConstant(3) GROUP_NOTIFICATION,
    @WamEnumConstant(4) GROUP_PROFILE_PICTURE_NOTIFICATION,
    @WamEnumConstant(5) IQ_RESPONSES
}
